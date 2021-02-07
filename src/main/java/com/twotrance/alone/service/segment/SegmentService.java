package com.twotrance.alone.service.segment;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.twotrance.alone.common.Constants;
import com.twotrance.alone.service.AbstractIDGenerate;
import com.twotrance.alone.common.utils.EnThread;
import com.twotrance.alone.config.ExceptionMsgProperties;
import com.twotrance.alone.model.segment.Alloc;
import com.twotrance.alone.model.segment.Segment;
import com.twotrance.alone.model.segment.SegmentBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * SegmentService
 *
 * @author trance
 * @description segment service
 */
@Service
public class SegmentService extends AbstractIDGenerate {

    /**
     * logger
     */
    Log log = LogFactory.get(this.getClass());

    /**
     * alloc service
     */
    private AllocService allocService;

    /**
     * exception information profile
     */
    private ExceptionMsgProperties ex;

    /**
     * constructor
     *
     * @param allocService             alloc service
     * @param exceptionMsgProperties() exception information profile
     */
    public SegmentService(AllocService allocService, ExceptionMsgProperties exceptionMsgProperties) {
        this.allocService = allocService;
        this.ex = exceptionMsgProperties;
        init();
    }

    /**
     * biz cache
     */
    private Map<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    /**
     * identifies whether the cache is initialized
     */
    private volatile boolean cacheInit = false;

    /**
     * initialize the cache
     */
    @Override
    public void init() {
        updateCache();
        cacheInit = true;
        loopUpdateCacheByOneMinute();
    }

    /**
     * update the cache
     */
    private void updateCache() {
        Set<String> bizKeys = CollStreamUtil.toSet(allocService.bizKeys(), bizKey -> bizKey);
        if (CollUtil.isEmpty(bizKeys))
            return;
        checkCache((bs, cs) -> CollUtil.subtractToList(bs, cs).parallelStream().forEach(bizKey -> cache.put(bizKey, new SegmentBuffer(bizKey))),
                (bs, cs) -> CollUtil.subtractToList(cs, bs).parallelStream().forEach(bizKey -> cache.remove(bizKey)),
                bizKeys, cache.keySet());
    }

    /**
     * check the cache for updates
     *
     * @param add       new consumer
     * @param remove    delete consumer
     * @param bizKeys   business key set
     * @param cacheKeys cache key set
     */
    private void checkCache(BiConsumer<Set<String>, Set<String>> add, BiConsumer<Set<String>, Set<String>> remove, Set<String> bizKeys, Set<String> cacheKeys) {
        add.andThen(remove).accept(bizKeys, cacheKeys);
    }

    /**
     * a single scheduling thread pool
     */
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new EnThread(r).rename("update-cache-thread").daemon(true));

    /**
     * the cache is updated once a minute
     */
    private void loopUpdateCacheByOneMinute() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> updateCache(), 1, 1, TimeUnit.MINUTES);
    }

    /**
     * gets the generated ID and initializes the uninitialized segment buffer
     *
     * @param bizKey biz key
     * @return long
     */
    @Override
    public long id(String bizKey) {
        SegmentBuffer segmentBuffer;
        if (cacheInit && cache.containsKey(bizKey)) {
            segmentBuffer = cache.get(bizKey);
            segmentBuffer.getLock().lock();
            if (!segmentBuffer.isInit()) {
                updateSegment(bizKey, segmentBuffer, segmentBuffer.current(), 0);
                segmentBuffer.setInit(true);
            }
            segmentBuffer.getLock().unlock();
            return produceID(segmentBuffer);
        }
        log.error(ex.exception(3001, Constants.EXCEPTION_TYPE_AUTO));
        throw ex.exception(3001, Constants.EXCEPTION_TYPE_AUTO);
    }

    /**
     * single toggle segment thread pool
     */
    private ExecutorService switchThreadPool = Executors.newSingleThreadExecutor();

    /**
     * to generate the id
     *
     * @param segmentBuffer segmentBuffer
     * @return long
     */
    private long produceID(SegmentBuffer segmentBuffer) {
        try {
            segmentBuffer.getLock().lock();
            updateNextSegment(segmentBuffer);
            Segment segment = segmentBuffer.current();
            long id = segment.getValue().get();
            if (!verifyMaxIdIsValid(id, segment.getMaxId())) {
                if (!segmentBuffer.isSwitch()) {
                    segmentBuffer.setSwitch(true);
                    switchThreadPool.execute(() -> {
                        try {
                            segmentBuffer.getLock().lock();
                            segmentBuffer.switchCurrent();
                            segmentBuffer.setNextReady(false);
                            segmentBuffer.getCondition().signalAll();
                            segmentBuffer.setSwitch(false);
                        } finally {
                            segmentBuffer.getLock().unlock();
                        }
                    });
                }
                segmentBuffer.getCondition().await();
            }
            segment = segmentBuffer.current();
            id = segment.getValue().get();
            if (verifyMaxIdIsValid(id, segment.getMaxId())) {
                id = segment.getValue().getAndIncrement();
                if (log.isInfoEnabled()) log.info(Constants.LOG_PREFIX_PLACEHOLDER_MODE, "auto id = " + id);
                return id;
            }
        } catch (InterruptedException e) {
            log.error(ex.exception(3002, Constants.EXCEPTION_TYPE_AUTO));
            throw ex.exception(1001, Constants.EXCEPTION_TYPE_COMMON);
        } finally {
            segmentBuffer.getLock().unlock();
        }
        return produceID(segmentBuffer);
    }

    /**
     * update segment
     *
     * @param bizKey  biz key
     * @param segment segment
     */
    @Transactional
    public void updateSegment(String bizKey, SegmentBuffer segmentBuffer, Segment segment, int lastStep) {
        try {
            Alloc alloc;
            if (!segmentBuffer.isInit() || segmentBuffer.getUpdateTimestamp() == 0) {
                allocService.uMaxId(bizKey);
                alloc = allocService.alloc(bizKey);
                segment.setMaxId(alloc.getMaxId());
                segment.setStep(alloc.getStep());
                segment.getValue().set(alloc.getMaxId() - alloc.getStep());
                segmentBuffer.setUpdateTimestamp(System.currentTimeMillis());
            } else {
                long consumeTime = 15 * 60 * 1000L;
                long duration = System.currentTimeMillis() - segmentBuffer.getUpdateTimestamp();
                int nextStep = 0;
                if (duration < consumeTime) {
                    if (segment.getStep() * 2 < 100000) {
                        nextStep = lastStep * 2;
                    }
                } else if (duration < (15 * 60 * 1000L) * 2) {
                    nextStep = lastStep;
                } else {
                    nextStep = lastStep / 2;
                }
                Alloc nextAlloc = new Alloc();
                nextAlloc.setBizKey(bizKey);
                nextAlloc.setStep(nextStep);
                allocService.uMaxIdByCustom(bizKey, nextStep);
                Alloc updatedAlloc = allocService.alloc(bizKey);
                segment.getValue().set(updatedAlloc.getMaxId() - nextStep);
                segment.setStep(updatedAlloc.getStep());
                segment.setMaxId(updatedAlloc.getMaxId());
                segmentBuffer.setUpdateTimestamp(System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.error(ex.exception(3003, Constants.EXCEPTION_TYPE_AUTO));
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw ex.exception(1001, Constants.EXCEPTION_TYPE_COMMON);
        }
    }

    /**
     * update the next segment
     *
     * @param segmentBuffer segment buffer
     */
    private void updateNextSegment(SegmentBuffer segmentBuffer) {
        Segment segment = segmentBuffer.current();
        boolean surpass = segment.getMaxId() - segment.getValue().get() < 0.9 * segment.getStep();
        if (!segmentBuffer.isNextReady() && surpass) {
            Segment nextSegment = segmentBuffer.nextSegment();
            updateSegment(segmentBuffer.getBizKey(), segmentBuffer, nextSegment, segment.getStep());
            segmentBuffer.setNextReady(true);
        }
    }

    /**
     * verify max id validity
     *
     * @param id    id
     * @param maxId max id
     * @return boolean
     */
    private boolean verifyMaxIdIsValid(long id, long maxId) {
        return id >= maxId ? false : true;
    }

}
