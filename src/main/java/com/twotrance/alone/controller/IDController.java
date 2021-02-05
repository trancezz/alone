package com.twotrance.alone.controller;

import com.twotrance.alone.service.segment.SegmentService;
import com.twotrance.alone.service.snowflake.SnowFlakeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * IDController
 *
 * @author trance
 * @description id controller
 */
@RestController
public class IDController {

    @Resource
    private SegmentService segmentService;


    @Resource
    private SnowFlakeService snowFlakeService;

    /**
     * 获取号段ID
     *
     * @param bizKey
     * @return long
     */
    @GetMapping("/seg/{bizKey}")
    public long segId(@PathVariable("bizKey") String bizKey) {
        return segmentService.id(bizKey);
    }

    /**
     * 获取雪花ID
     *
     * @return long
     */
    @GetMapping("/snow")
    public long snowId() {
        return snowFlakeService.id(null);
    }
}
