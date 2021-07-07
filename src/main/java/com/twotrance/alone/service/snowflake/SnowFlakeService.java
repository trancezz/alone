package com.twotrance.alone.service.snowflake;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.twotrance.alone.common.constants.Constants;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.service.AbstractIDGenerate;
import org.springframework.stereotype.Service;

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
     * logger
     */
    private static final Log log = LogFactory.get();

    /**
     * start timestamp
     */
    private final long startTime = 1611852459517L;

    /**
     * machine id
     */
    public static volatile Long machineID;

    /**
     * last timestamp
     */
    private long lastTimestamp = -1L;

    /**
     * sequence no
     */
    private long sequence = 0L;

    /**
     * the number of sequence no offsets
     */
    private long sequenceBits = 10L;

    /**
     * the number of machine id offsets
     */
    private final long machineIdOffset = 12L;

    /**
     * the number of timestamp offsets, the timestamp in java is converted to 41 bits in binary
     */
    private final long offsetTimestamp = sequenceBits + machineIdOffset;

    /**
     * maximum serial number
     */
    private final long maxSequence = ~(-1L << 12L);

    /**
     * exception information profile
     */
    private ExceptionHandler ex;

    /**
     * machine id service
     */
    private MidService midService;

    /**
     * constructor
     *
     * @param midService       machine id service
     * @param exceptionHandler exception message properties
     */
    public SnowFlakeService(MidService midService, ExceptionHandler exceptionHandler) {
        this.ex = exceptionHandler;
        this.midService = midService;
        machineID = midService.getMachineID();
        midInScope();
        if (log.isInfoEnabled())
            log.info(Constants.LOG_PREFIX_PLACEHOLDER_MODE, "the machine id has been initialized, machine id = " + machineID.toString());
    }

    /**
     * generate snowflake id
     *
     * @param bizKey useless arguments
     * @return long
     */
    @Override
    public synchronized Long id(String bizKey, String phone, String appKey) {
        validAppKey(phone, appKey);
        verifyMidIsValid();
        long timestamp = genTime();
        if (legalTime(timestamp)) {
            long callbackTimeMS = lastTimestamp - timestamp;
            if (callbackTimeMS <= 5) {
                try {
                    wait(callbackTimeMS << 1);
                    timestamp = genTime();
                    if (legalTime(timestamp)) {
                        log.error(getException(2003));
                        throwException(1001);
                    }
                } catch (InterruptedException e) {
                    log.error(getException(2004));
                    throwException(1001);
                }
            } else {
                log.error(getException(2005));
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
        Long id = ((timestamp - startTime) << offsetTimestamp) | (machineID << machineIdOffset) | sequence;
        if (log.isInfoEnabled()) log.info(Constants.LOG_PREFIX_PLACEHOLDER_MODE, "snowflake id = " + id);
        return id;
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
     * determines whether the current timestamp is valid
     *
     * @param timestamp
     * @return boolean
     */
    private boolean legalTime(long timestamp) {
        return timestamp < lastTimestamp;
    }

    /**
     * until the current timestamp is greater than the last timestamp
     *
     * @return long
     */
    private long nextTimestamp() {
        long timestamp = genTime();
        while (timestamp <= lastTimestamp) {
            timestamp = genTime();
        }
        return timestamp;
    }

    /**
     * verify that the machine id is valid
     */
    private void verifyMidIsValid() {
        if (!midService.midIsValid()) {
            log.error(getException(2002));
            midService.init();
            machineID = midService.getMachineID();
            if (log.isInfoEnabled())
                log.info(Constants.LOG_PREFIX_PLACEHOLDER_MODE, "rebuild machine id = " + machineID.toString());
            midInScope();
            throwException(1001);
        }
    }

    /**
     * check if the machine id is in range
     */
    private void midInScope() {
        if (-1 == machineID) {
            log.error(getException(2001));
            throwException(1001);
        }
    }
}
