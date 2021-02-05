package com.twotrance.alone.model.segment;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SegmentBuffer
 *
 * @author trance
 * @description segment buffer
 * @explain 采用双号段缓冲区
 */
@Setter
@Getter
public class SegmentBuffer {

    private String bizKey;
    private Segment[] segments;
    private volatile int currentIndex;
    private volatile boolean nextReady;
    private volatile boolean init;
    private volatile boolean isSwitch;
    private final ReentrantLock lock;
    private final Condition condition;
    private volatile long updateTimestamp;

    public SegmentBuffer(String bizKey) {
        this.bizKey = bizKey;
        this.segments = new Segment[]{new Segment(), new Segment()};
        this.currentIndex = 0;
        this.nextReady = false;
        this.init = false;
        this.isSwitch = false;
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }

    public int nextIndex() {
        return (currentIndex + 1) % 2;
    }

    public Segment current() {
        return segments[currentIndex];
    }

    public void switchCurrent() {
        currentIndex = nextIndex();
    }

    public Segment nextSegment() {
        return this.segments[nextIndex()];
    }
}
