package com.twotrance.alone.model.segment;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Segment
 *
 * @author trance
 * @description segment
 * @explain 号段
 */
@Setter
@Getter
public class Segment {

    private AtomicLong value = new AtomicLong(1);
    private volatile Long max;
    private volatile Long length;

}
