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
     * 日志记录器
     */
    Log log = LogFactory.get(this.getClass());

    /**
     * 分配服务
     */
    private AllocService allocService;

    /**
     * 异常信息配置文件
     */
    private ExceptionMsgProperties ex;

    /**
     * 构造函数
     *
     * @param allocService 分配号段服务
     * @explain init() 初始化号段缓冲区
     */
    public SegmentService(AllocService allocService, ExceptionMsgProperties exceptionMsgProperties) {
        this.allocService = allocService;
        this.ex = exceptionMsgProperties;
        init();
    }

    /**
     * 利用高性能且线程安全的Map缓存不同业务的号段缓冲区
     */
    private Map<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    /**
     * 缓存是否成功初始化标识
     */
    private volatile boolean cacheInit = false;

    /**
     * (初始化) 缓存
     *
     * @return boolean
     * @explain updateCache() 更新缓存(当前每个存在的号段缓冲区)
     * @explain cacheInit 是否完成初始化缓存标识
     * @explain loopUpdateCacheByOneMinute() 重复更新缓存 1/m
     */
    @Override
    public void init() {
        updateCache();
        cacheInit = true;
        loopUpdateCacheByOneMinute();
    }

    /**
     * (更新) 缓存
     * DB -> Cache
     *
     * @explain bizKeys 获取数据库中存在的所有业务键
     * @explain checkCache() 检查是否有新的业务键或过期的业务键
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
     * 检查缓存的更新
     *
     * @param add       新增消费者
     * @param remove    删除消费者
     * @param bizKeys   业务键集合
     * @param cacheKeys 缓存键集合
     */
    private void checkCache(BiConsumer<Set<String>, Set<String>> add, BiConsumer<Set<String>, Set<String>> remove, Set<String> bizKeys, Set<String> cacheKeys) {
        add.andThen(remove).accept(bizKeys, cacheKeys);
    }

    /**
     * 时间调度线程池
     */
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> new EnThread(r).rename("update-cache-thread").daemon(true));

    /**
     * 一分钟更新一次缓存
     *
     * @explain 轮询更新缓存
     */
    private void loopUpdateCacheByOneMinute() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> updateCache(), 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 获取生成的ID, 并初始化未被初始化的号段缓冲区
     *
     * @param bizKey 业务键
     * @return long
     * @explain produceID() 生成ID
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
        log.error(ex.exception(1015));
        throw ex.exception(1015);
    }

    /**
     * 专门用于切换号段的线程池
     */
    private ExecutorService switchThreadPool = Executors.newSingleThreadExecutor();

    /**
     * 生成ID
     *
     * @param segmentBuffer 号段缓冲区
     * @return long
     * @expain validID() 验证当前的ID是否达到当前号段最大值
     * @expain 当ID达到最大值则开启切换号段线程, 并且所有生成ID的请求等待
     * @expain 当前切换号段线程唤醒所有生成ID的请求后, 重新进入生成ID的方法请求生成ID
     */
    private long produceID(SegmentBuffer segmentBuffer) {
        try {
            segmentBuffer.getLock().lock();
            updateNextSegment(segmentBuffer);
            Segment segment = segmentBuffer.current();
            long id = segment.getValue().get();
            if (!validID(id, segment.getMaxId())) {
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
            if (validID(id, segment.getMaxId())) {
                id = segment.getValue().getAndIncrement();
                if (log.isInfoEnabled()) log.info(Constants.LOG_PREFIX_PLACEHOLDER_MODE, "自增 ID = " + id);
                return id;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            segmentBuffer.getLock().unlock();
        }
        return produceID(segmentBuffer);
    }

    /**
     * 更新号段
     *
     * @param bizKey  业务键
     * @param segment 号段
     * @explain 初始化未准备好的号段
     * @explain 通过上一次消耗号段时间, 动态更新下一号段
     * 在DB不可用的情况下, 理论上如果 QPS 处于恒定值, 那么消耗时间也会处于恒定值
     * 假如如果每分钟可处理500个 QPS, 在DB不可用的情况下, 假设可坚持10分钟
     * 但是如果 QPS 每分钟突然猛增N个QPS, 那么在DB不可用的情况下就只能坚持 < 10分钟
     * 结论就是 QPS 越大, 那么时间就坚持得越短, 为了解决这个问题, 我们需要使时间趋于恒定
     * 即 QPS 与 号段长度成正比
     * 按照消耗速度15分钟为界, 当前消耗速度小于15分钟, 那么下一号段的步长为当前步长的2倍, 如果大于15分钟且小于30分钟, 那么下一号段的步长为当前步长, 如果大于30分钟, 那么下一号段的步长减半
     * 此处个人规定了步长跨度不能大于10万
     */
    @Transactional // 事务支持
    public void updateSegment(String bizKey, SegmentBuffer segmentBuffer, Segment segment, int lastStep) {
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
    }

    /**
     * 更新下一号段
     *
     * @param segmentBuffer 号段缓存区
     * @explain 更新时机为当前号段消耗超百分之10, 并且下一号段没有被更新
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
     * 验证ID合法性
     *
     * @param id    ID
     * @param maxId 最大值
     * @return boolean
     */
    private boolean validID(long id, long maxId) {
        return id >= maxId ? false : true;
    }

}
