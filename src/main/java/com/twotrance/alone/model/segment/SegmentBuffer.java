package com.twotrance.alone.model.segment;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private String model;
    private Segment[] segments;
    private volatile Integer currentIndex;
    private volatile Boolean nextReady;
    private volatile Boolean init;
    private volatile Boolean switched;
    private volatile Long updateTimestamp;
    @JsonIgnore
    private final ReentrantLock lock;
    @JsonIgnore
    private final Condition condition;

    public SegmentBuffer(String model) {
        this.model = model;
        this.segments = new Segment[]{new Segment(), new Segment()};
        this.currentIndex = 0;
        this.nextReady = false;
        this.init = false;
        this.switched = false;
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
