package com.twotrance.alone.service.snowflake;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.twotrance.alone.common.Constants;
import com.twotrance.alone.common.utils.EnThread;
import com.twotrance.alone.config.ExceptionMsgProperties;
import com.twotrance.alone.model.snowflake.MidInfo;
import lombok.Getter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MidService
 *
 * @author trance
 * @description machine id service
 * @date 2021/1/29
 */
@Service
public class MidService {

    /**
     * logger
     */
    private static final Log log = LogFactory.get();

    /**
     * redis template
     */
    private RedisTemplate redisTemplate;

    /**
     * local ip address
     */
    private final String ip = NetUtil.getLocalhostStr();

    /**
     * machine id
     */
    @Getter
    private Long machineID;

    /**
     * application port
     */
    private Integer port;

    /**
     * local ip address and application port
     */
    private String ipAndPort;

    /**
     * exception information profile
     */
    private ExceptionMsgProperties ex;

    /**
     * distributed locking of machine ID information
     */
    @Getter
    private RLock lock;

    /**
     * constructor
     *
     * @param redisTemplate          redis template
     * @param environment            environment variable
     * @param exceptionMsgProperties exception information profile
     */
    public MidService(RedisTemplate redisTemplate, Environment environment, RedissonClient redissonClient, ExceptionMsgProperties exceptionMsgProperties) {
        this.redisTemplate = redisTemplate;
        this.lock = redissonClient.getLock(Constants.HASH_MACHINE_LOCK);
        this.ex = exceptionMsgProperties;
        this.port = environment.getProperty("server.port", Integer.class);
        if (ObjectUtil.isEmpty(port)) {
            log.error(ex.exception(2006, Constants.EXCEPTION_TYPE_SNOWFLAKE));
            throw ex.exception(1001, Constants.EXCEPTION_TYPE_COMMON);
        }
        init();
    }

    /**
     * the machine id is initialized
     */
    public void init() {
        ipAndPort = ip + "-" + port;
        if (!hasMidInfosKey()) {
            machineID = genMid();
            if (-1L == machineID)
                return;
            putMidInfo2Redis();
            loopUpdateMidInfo();
            return;
        }
        MidInfo midInfo = midInfo4Redis();
        if (ObjectUtil.isEmpty(midInfo)) {
            machineID = genMid();
            if (-1L == machineID)
                return;
            putMidInfo2Redis();
            loopUpdateMidInfo();
            return;
        }
        if (genTime() < midInfo.getTimestamp()) {
            machineID = -1L;
            log.error(ex.exception(2007, Constants.EXCEPTION_TYPE_SNOWFLAKE));
            throw ex.exception(1001, Constants.EXCEPTION_TYPE_COMMON);
        }
        machineID = midInfo.getMid();
        loopUpdateMidInfo();
    }

    /**
     * whether the machine id information key exists
     *
     * @return boolean
     */
    private boolean hasMidInfosKey() {
        return redisTemplate.hasKey(Constants.HASH_MACHINE_ID);
    }

    /**
     * milliseconds per minute
     *
     * @param ms
     * @return int
     */
    private int ms2Mi(long ms) {
        return Long.valueOf(ms / 1000 / 60).intValue();
    }

    /**
     * gets the current timestamp
     *
     * @return long
     */
    private long genTime() {
        return System.currentTimeMillis();
    }

    /**
     * generate machine id information
     *
     * @return MidInfo
     */
    private MidInfo genNodeInfo() {
        return new MidInfo(ipAndPort, machineID, genTime());
    }

    /**
     * get the machine id information from redis
     *
     * @return MidInfo
     */
    private MidInfo midInfo4Redis() {
        Object object = redisTemplate.opsForHash().get(Constants.HASH_MACHINE_ID, ipAndPort);
        if (ObjectUtil.isEmpty(object))
            return null;
        return (MidInfo) object;
    }

    /**
     * store the machine id information in redis and resets the current machine id state
     */
    private void putMidInfo2Redis() {
        redisTemplate.opsForHash().put(Constants.HASH_MACHINE_ID, ipAndPort, genNodeInfo());
    }

    /**
     * retrieve all machine id information in redis, and delete the invalid machine id information in redis, if the time difference is greater than 1 minute
     *
     * @return List<MidInfo>
     */
    private List<MidInfo> midInfos4Redis() {
        lock.lock();
        Map<String, MidInfo> midInfoMap = redisTemplate.opsForHash().entries(Constants.HASH_MACHINE_ID);
        if (MapUtil.isEmpty(midInfoMap)) {
            lock.unlock();
            return ListUtil.list(false);
        }
        List<MidInfo> invalidMidInfos = midInfoMap.values().parallelStream()
                .filter(midInfo -> ms2Mi(genTime() - midInfo.getTimestamp()) > 1)
                .collect(Collectors.toList());
        invalidMidInfos.parallelStream().forEach(midInfo -> {
            midInfoMap.remove(midInfo.getIpAndPort());
            redisTemplate.opsForHash().delete(Constants.HASH_MACHINE_ID, midInfo.getIpAndPort());
        });
        lock.unlock();
        return midInfoMap.values().parallelStream().collect(Collectors.toList());
    }

    /**
     * gets the machine id that is already in use
     *
     * @return List<Long>
     */
    private List<Long> inUseMids() {
        return midInfos4Redis().parallelStream().map(MidInfo::getMid).collect(Collectors.toList());
    }

    /**
     * generate machine IDs to prevent stack overflow so no recursion is used
     *
     * @return long
     */
    private long genMid() {
        lock.lock();
        List<Long> inUseMids = inUseMids();
        if (inUseMids.size() >= 1024) {
            lock.unlock();
            return -1;
        }
        Long mid = RandomUtil.randomLong(1024);
        while (inUseMids.contains(mid))
            mid = RandomUtil.randomLong(1024);
        lock.unlock();
        return mid.longValue();
    }

    /**
     * update machine id information single thread pool
     */
    private ScheduledExecutorService updateMidInfoThreadPool = Executors.newSingleThreadScheduledExecutor(r -> new EnThread(r).daemon(true));

    /**
     * the machine id information is updated every 3 seconds
     */
    private void loopUpdateMidInfo() {
        updateMidInfoThreadPool.scheduleAtFixedRate(this::putMidInfo2Redis, 3L, 3L, TimeUnit.SECONDS);
    }

    /**
     * rebuild the machine when the machine id expires
     */
    public boolean midIsValid() {
        return midInfo4Redis() != null ? true : false;
    }
}
