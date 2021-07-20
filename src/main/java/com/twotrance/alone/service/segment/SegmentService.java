package com.twotrance.alone.service.segment;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.twotrance.alone.common.constants.Constants;
import com.twotrance.alone.common.utils.EnThread;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.model.segment.Paragraph;
import com.twotrance.alone.model.segment.Segment;
import com.twotrance.alone.model.segment.SegmentBuffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * SegmentService
 *
 * @author trance
 * @description segment service
 */
@Service
@Transactional
public class SegmentService {

    /**
     * logger
     */
    Log log = LogFactory.get(this.getClass());

    /**
     * paragraph service
     */
    private ParagraphService paragraphService;

    /**
     * exception information profile
     */
    private ExceptionHandler ex;

    /**
     * constructor
     *
     * @param paragraphService paragraph service
     * @param exceptionHandler exception information profile
     */
    public SegmentService(ParagraphService paragraphService, ExceptionHandler exceptionHandler) {
        this.paragraphService = paragraphService;
        this.ex = exceptionHandler;
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
    private void init() {
        updateCache();
        cacheInit = true;
        loopUpdateCacheByOneMinute();
    }

    /**
     * update the cache
     */
    public void updateCache() {
        Set<String> models = CollStreamUtil.toSet(paragraphService.models(), model -> model);
        if (CollUtil.isEmpty(models))
            return;
        checkCache(
                (bs, cs) -> CollUtil.subtractToList(bs, cs)
                        .parallelStream().forEach(model -> cache.put(model, new SegmentBuffer(model))),
                (bs, cs) -> CollUtil.subtractToList(cs, bs)
                        .parallelStream().forEach(model -> cache.remove(model)),
                models, cache.keySet());
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
     * @param model model
     * @return long
     */
    public Long id(String model, String phone) {
        if (StrUtil.isEmpty(model)) ex.exception(3001);
        if (!paragraphService.hasModel(phone, model)) ex.exception(3001);
        if (cacheInit && cache.containsKey(model)) {
            SegmentBuffer segmentBuffer = cache.get(model);
            segmentBuffer.getLock().lock();
            if (!segmentBuffer.getInit()) {
                updateSegment(phone, model, segmentBuffer, segmentBuffer.current());
            }
            segmentBuffer.getLock().unlock();
            return produceID(phone, segmentBuffer);
        }
        log.error(ex.exception(3001));
        throw ex.exception(3001);
    }

    /**
     * segment update
     *
     * @param model
     * @param segmentBuffer
     * @param segment
     */
    private void updateSegment(String phone, String model, SegmentBuffer segmentBuffer, Segment segment) {
        try {
            Paragraph paragraph = paragraphService.model(phone, model);
            if (!segmentBuffer.getInit()) {
                segment.setMax(paragraph.getMax());
                segment.setLength(paragraph.getLength());
                segment.getValue().set(paragraph.getMax() - paragraph.getLength());
                segmentBuffer.setUpdateTimestamp(System.currentTimeMillis());
                paragraphService.updateOfMax(paragraph.getLength(), model, paragraph.getMax());
            } else {
                paragraphService.updateOfMax(calcNextLength(segmentBuffer.getUpdateTimestamp(), paragraph.getLength()), model, paragraph.getMax());
                paragraph = paragraphService.model(phone, model);
                segment.getValue().set(paragraph.getMax() - paragraph.getLength());
                segment.setLength(paragraph.getLength());
                segment.setMax(paragraph.getMax());
                segmentBuffer.setUpdateTimestamp(System.currentTimeMillis());
            }
            segmentBuffer.setInit(true);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(ex.exception(3003));
            throw ex.exception(1001);
        }
    }

    /**
     * calc next length
     *
     * @param updateTimestamp
     * @param length
     * @return
     */
    private Long calcNextLength(Long updateTimestamp, Long length) {
        long accordingTime = 15 * 60 * 1000L;
        long timeDiff = System.currentTimeMillis() - updateTimestamp;
        long nextLength = 0;
        if (timeDiff < accordingTime) {
            if (length < 100000) {
                nextLength = length * 2;
            }
        } else if (timeDiff < accordingTime * 2) {
            nextLength = length;
        } else {
            nextLength = length / 2;
        }
        return nextLength;
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
    private Long produceID(String phone, SegmentBuffer segmentBuffer) {
        try {
            segmentBuffer.getLock().lock();
            updateNextSegment(phone, segmentBuffer);
            Segment segment = segmentBuffer.current();
            long id = segment.getValue().get();
            if (!verifyMaxIdIsValid(id, segment.getMax())) {
                if (!segmentBuffer.getSwitched()) {
                    segmentBuffer.setSwitched(true);
                    switchThreadPool.execute(() -> {
                        try {
                            segmentBuffer.getLock().lock();
                            segmentBuffer.switchCurrent();
                            segmentBuffer.setNextReady(false);
                            segmentBuffer.getCondition().signalAll();
                            segmentBuffer.setSwitched(false);
                        } finally {
                            segmentBuffer.getLock().unlock();
                        }
                    });
                }
                segmentBuffer.getCondition().await();
            }
            segment = segmentBuffer.current();
            id = segment.getValue().get();
            if (verifyMaxIdIsValid(id, segment.getMax())) {
                id = segment.getValue().getAndIncrement();
                if (log.isInfoEnabled()) log.info(Constants.LOG_PREFIX_PLACEHOLDER_MODE, "auto id = " + id);
                return id;
            }
        } catch (InterruptedException e) {
            log.error(ex.exception(3002));
            throw ex.exception(1001);
        } finally {
            segmentBuffer.getLock().unlock();
        }
        return produceID(phone, segmentBuffer);
    }


    /**
     * update the next segment
     *
     * @param segmentBuffer segment buffer
     */
    private void updateNextSegment(String phone, SegmentBuffer segmentBuffer) {
        Segment segment = segmentBuffer.current();
        boolean surpass = segment.getMax() - segment.getValue().get() < 0.9 * segment.getLength();
        if (!segmentBuffer.getNextReady() && surpass) {
            Segment nextSegment = segmentBuffer.nextSegment();
            updateSegment(phone, segmentBuffer.getModel(), segmentBuffer, nextSegment);
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
