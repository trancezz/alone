package com.twotrance.alone.service.snowflake;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.setting.dialect.Props;
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

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * MachineIDInitService
 *
 * @author trance
 * @description machineID init service
 * @date 2021/1/29
 */
@Service
public class MachineIDInitService {

    /**
     * 日志记录器
     */
    private static final Log log = LogFactory.get();

    /**
     * RedisTemplate
     */
    private RedisTemplate redisTemplate;

    /**
     * 本机IP地址
     */
    private final String ip = NetUtil.getLocalhostStr();

    /**
     * 当前初始化后的机器ID
     */
    @Getter
    private Long machineID;

    /**
     * 获取当前项目端口号
     */
    private Integer port;

    /**
     * 当前机器IP与端口字符串
     */
    private String ipAndPort;

    /**
     * 本地机场ID文件
     */
    private File machineIDLocalFile;

    /**
     * 异常信息配置文件
     */
    private ExceptionMsgProperties ex;
    /**
     * 机器ID信息分布式锁
     */
    private RLock lock;

    /**
     * 标识当前机器ID是否失效
     */
    @Getter
    private volatile boolean failure = false;


    /**
     * 构造函数
     *
     * @param redisTemplate          Redis模板
     * @param environment            SpringBoot环境变量
     * @param exceptionMsgProperties 异常信息配置
     * @explain 如果获取不到端口号则抛出异常, 因为端口号是必须的
     * @explain 这里的机器码我把它默认为App的端口号, 同样最多可部署1024个App
     * @explain init() 初始化机器ID
     */
    public MachineIDInitService(RedisTemplate redisTemplate, Environment environment, RedissonClient redissonClient, ExceptionMsgProperties exceptionMsgProperties) {
        this.redisTemplate = redisTemplate;
        lock = redissonClient.getLock(Constants.HASH_MACHINE_LOCK);
        this.ex = exceptionMsgProperties;
        String portString = environment.getProperty("server.port");
        if (StrUtil.isBlank(portString)) {
            log.error(ex.exception(1006));
            throw ex.exception(1001);
        }
        port = Integer.valueOf(portString);
        init();
    }

    /**
     * 机器ID初始化工作
     *
     * @explain initMachineIDUsageMap() 初始化机器ID使用情况列表
     * @explain ipAndPort IP地址与端口号组成当前App的标识键
     * @explain machineIDLocalFile 机器ID存储的本地文件
     * @expalin 如果当前Redis中不存在此Hash, 证明这是部署的第一个App, 所以应该去初始化它, 以IP地址和Port为键和生成的机器ID为值存入RedisHash中
     * 如果存在此Hash则通过IP地址和Port为键去获取Hash中存放的机器ID实例(这里为CurrentPoint), 这里分2种情况, 第一种是存在机器ID实例, 取出后先检查时间戳
     * 是否合理, 当前时间戳大于存储的时间戳, 如果当前时间戳小于取出的时间戳那么证明出现时间回拨的现象抛出异常, 如果时间戳合理, 那么就取出当前实例中的机器ID作为机器ID
     * 如果此Hash不存在相应的机器ID实例, 那么就用此IP地址和Port初始化此机器ID实例
     */
    public void init() {
        ipAndPort = ip + "-" + port;
        machineIDLocalFile = new File(Constants.MACHINE_ID_LOCAL_STORE_PATH.replace("{-}", ipAndPort));
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
            return;
        }
        machineID = midInfo.getMid();
        loopUpdateMidInfo();
    }

    /**
     * 判断Redis中是否存在机器ID信息键
     *
     * @return boolean
     */
    private boolean hasMidInfosKey() {
        return redisTemplate.hasKey(Constants.HASH_MACHINE_ID);
    }

    /**
     * 毫秒转分钟
     *
     * @param ms
     * @return int
     */
    private int ms2Mi(long ms) {
        return Long.valueOf(ms / 1000 / 60).intValue();
    }

    /**
     * 生成系统当前时间, 单位毫秒
     *
     * @return long
     */
    private long genTime() {
        return System.currentTimeMillis();
    }

    /**
     * 生成机器ID信息
     *
     * @return MidInfo
     */
    private MidInfo genNodeInfo() {
        return new MidInfo(ipAndPort, machineID, genTime());
    }

    /**
     * 将对应的机器ID信息取出
     */
    private MidInfo midInfo4Redis() {
        return (MidInfo) redisTemplate.opsForHash().get(Constants.HASH_MACHINE_ID, ipAndPort);
    }

    /**
     * 将机器ID数据存入Redis中
     */
    private void putMidInfo2Redis() {
        try {
            redisTemplate.opsForHash().put(Constants.HASH_MACHINE_ID, ipAndPort, genNodeInfo());
            failure = false;
        } catch (Exception e) {
            failure = true;
        }
    }

    /**
     * 获取Redis中所有的机器ID信息, 并且删除Redis中无效的机器ID信息, 条件是时间差大于1分钟
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
     * 获取已经使用的机器ID
     *
     * @return int[]
     */
    private List<Long> inUseMids() {
        return midInfos4Redis().parallelStream().map(MidInfo::getMid).collect(Collectors.toList());
    }

    /**
     * 生成机器ID, 防止栈溢出所以未用递归
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
     * 更新机器ID信息单线程池
     */
    private ScheduledExecutorService updateMidInfoThreadPool = Executors.newSingleThreadScheduledExecutor(r -> new EnThread(r).daemon(true));

    /**
     * 每3秒钟更新一次机器ID信息
     */
    private void loopUpdateMidInfo() {
        updateMidInfoThreadPool.scheduleAtFixedRate(this::putMidInfo2Redis, 3L, 3L, TimeUnit.SECONDS);
    }
}
