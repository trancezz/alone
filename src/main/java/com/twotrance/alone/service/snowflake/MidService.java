package com.twotrance.alone.service.snowflake;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.twotrance.alone.common.constants.Constants;
import com.twotrance.alone.common.utils.EnThread;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.model.snowflake.MidInfo;
import lombok.Getter;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private ExceptionHandler ex;

    /**
     * distributed locking of machine ID information
     */
    @Getter
    private RLock lock;

    /**
     * redisson client
     */
    private RedissonClient redissonClient;

    /**
     * machine map
     */
    private RMap<String, MidInfo> machineMap;

    /**
     * constructor
     *
     * @param environment      environment variable
     * @param exceptionHandler exception information profile
     */
    public MidService(Environment environment, RedissonClient redissonClient, ExceptionHandler exceptionHandler) {
        this.redissonClient = redissonClient;
        this.ex = exceptionHandler;
        this.port = environment.getProperty("server.port", Integer.class);
        if (ObjectUtil.isEmpty(port)) {
            log.error(ex.exception(2006));
            throw ex.exception(1001);
        }
        init();
    }

    /**
     * the machine id is initialized
     */
    public void init() {
        ipAndPort = ip + "-" + port;
        // local cache map options
        LocalCachedMapOptions localCachedMapOptions = LocalCachedMapOptions.defaults()
                .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
                .cacheSize(500)
                .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.LOAD)
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.UPDATE)
                .timeToLive(3600000)
                .maxIdle(3600000);
        machineMap = redissonClient.getLocalCachedMap(Constants.HASH_MACHINE_ID, localCachedMapOptions);
        lock = machineMap.getLock(Constants.HASH_MACHINE_LOCK);
        if (MapUtil.isEmpty(machineMap)) {
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
            log.error(ex.exception(2007));
            throw ex.exception(1001);
        }
        machineID = midInfo.getMid();
        loopUpdateMidInfo();
    }

    /**
     * milliseconds per minute
     *
     * @param ms
     * @return int
     */
    private Integer ms2Mi(long ms) {
        return Long.valueOf(ms / 1000 / 60).intValue();
    }

    /**
     * gets the current timestamp
     *
     * @return long
     */
    private Long genTime() {
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
        Object object = machineMap.get(ipAndPort);
        if (ObjectUtil.isEmpty(object))
            return null;
        return (MidInfo) object;
    }

    /**
     * store the machine id information in redis and resets the current machine id state
     */
    private void putMidInfo2Redis() {
        machineMap.fastPut(ipAndPort, genNodeInfo());
    }

    /**
     * retrieve all machine id information in redis, and delete the invalid machine id information in redis, if the time difference is greater than 1 minute
     * generate machine IDs to prevent stack overflow so no recursion is used
     *
     * @return Long
     */
    private Long genMid() {
        Boolean locked;
        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (locked) {
                if (null == machineMap) {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                    return -1L;
                }
                machineMap.values().parallelStream()
                        .filter(midInfo -> ms2Mi(genTime() - midInfo.getTimestamp()) > 1)
                        .parallel()
                        .forEach(midInfo -> machineMap.fastRemove(midInfo.getIpAndPort()));
                List<Long> inUseMids = machineMap.values().parallelStream().map(MidInfo::getMid).collect(Collectors.toList());
                if (inUseMids.size() >= 1024) {
                    return -1L;
                }
                Long mid = RandomUtil.randomLong(1024);
                while (inUseMids.contains(mid))
                    mid = RandomUtil.randomLong(1024);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
                return mid.longValue();
            }
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            ex.exception(2008);
        }
        return genMid();
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
    public Boolean midIsValid() {
        return midInfo4Redis() != null ? true : false;
    }
}
