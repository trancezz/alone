package com.twotrance.alone.service.snowflake;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.twotrance.alone.common.Constants;
import com.twotrance.alone.common.utils.EnThread;
import com.twotrance.alone.service.AbstractIDGenerate;
import com.twotrance.alone.config.ExceptionMsgProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SnowFlakeService
 *
 * @author trance
 * @description snow flake service
 * @date 2021/1/26
 */
@Service
public class SnowFlakeService extends AbstractIDGenerate {

    /**
     * 日志记录
     */
    private static final Log log = LogFactory.get();

    /**
     * 开始时间
     */
    private final long startTime = 1611852459517L;

    /**
     * 机器ID
     */
    public static volatile Long machineID;

    /**
     * 上一次获取ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 序列号位数
     */
    private long sequenceBits = 10L;

    /**
     * 机器ID偏移12位
     */
    private final long machineIdOffset = 12L;

    /**
     * 时间戳偏移的位数, java中的时间戳转换为二进制位为41bits
     */
    private final long offsetTimestamp = sequenceBits + machineIdOffset;

    /**
     * 最大的序列号
     */
    private final long maxSequence = ~(-1L << 12L);

    /**
     * 异常信息配置文件
     */
    private ExceptionMsgProperties ex;

    /**
     * 机器ID初始化服务
     */
    private MachineIDInitService machineIDInitService;

    /**
     * 构造函数
     *
     * @param machineIDInitService machineID初始化服务
     * @explain machineID 如果初始化成功那么就获取其机器ID, 并且判断机器ID是否超出最大值
     * @explain 如果machineID为-1证明无论从Redis还是本地文件获取机器ID均失败
     */
    public SnowFlakeService(MachineIDInitService machineIDInitService, ExceptionMsgProperties exceptionMsgProperties) {
        this.ex = exceptionMsgProperties;
        this.machineIDInitService = machineIDInitService;
        machineID = machineIDInitService.getMachineID();
        if (-1 == machineID) {
            log.error(ex.exception(1002));
            throw ex.exception(1001);
        }
        if (log.isInfoEnabled())
            log.info(Constants.LOG_PREFIX_PLACEHOLDER_MODE, "机器ID初始化完毕, machineID = " + machineID.toString());
    }

    /**
     * 生成雪花ID
     *
     * @param bizKey 无用参数
     * @return long
     * @explain 当发生时间回拨的情况下, 查看时间差是否<5ms, 如<5ms则尝试重新获取时间戳进行修复, 否则直接抛出时间回拨, 时间差过大异常
     * @explain 如果在同一时间戳类存在多个ID请求, 则重随机序列号来避免重复, 当然如果序列号达到最大值则也会重随
     */
    @Override
    public synchronized long id(String bizKey) {
        if (machineIDInitService.isFailure()) {
            log.error(ex.exception(1019));
            throw ex.exception(1001);
        }
        long timestamp = ct();
        if (legalTime(timestamp)) {
            long callbackTimeMS = lastTimestamp - timestamp;
            if (callbackTimeMS <= 5) {
                try {
                    wait(callbackTimeMS << 1);
                    timestamp = ct();
                    if (legalTime(timestamp)) {
                        log.error(ex.exception(1003));
                        throw ex.exception(1001);
                    }
                } catch (InterruptedException e) {
                    log.error(ex.exception(1004));
                    throw ex.exception(1001);
                }
            } else {
                log.error(ex.exception(1005));
                throw ex.exception(1001);
            }
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (0 == sequence) {
                sequence = RandomUtil.randomInt(101);
                timestamp = nextTimestamp();
            }
        } else {
            sequence = RandomUtil.randomInt(101);
        }
        lastTimestamp = timestamp;
        long id = ((timestamp - startTime) << offsetTimestamp) | (machineID << machineIdOffset) | sequence;
        if (log.isInfoEnabled()) log.info(Constants.LOG_PREFIX_PLACEHOLDER_MODE, "雪花 ID = " + id);
        return id;
    }

    /**
     * 获取当前时间戳
     *
     * @return long
     */
    private long ct() {
        return System.currentTimeMillis();
    }

    /**
     * 判断当前时间戳是否合法
     *
     * @param timestamp
     * @return boolean
     */
    private boolean legalTime(long timestamp) {
        return timestamp < lastTimestamp;
    }

    /**
     * 自旋直到当前时间戳>上一次时间戳并返回
     *
     * @return long
     */
    private long nextTimestamp() {
        long timestamp = ct();
        while (timestamp <= lastTimestamp) {
            timestamp = ct();
        }
        return timestamp;
    }
}
