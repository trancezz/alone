package com.twotrance.alone.controller;

import com.twotrance.alone.common.vo.R;
import com.twotrance.alone.service.segment.SegmentService;
import com.twotrance.alone.service.snowflake.SnowFlakeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * IDController
 *
 * @author trance
 * @description id controller
 */
@RestController
@RequestMapping("/id")
public class IDController {

    @Resource
    private SnowFlakeService snowFlakeService;

    @Resource
    private SegmentService segmentService;

    /**
     * 获取号段ID
     */
    @GetMapping("/seg")
    public Mono<R> segId(@RequestParam(value = "model", required = false) String model, @RequestParam(value = "phone", required = false) String phone) {
        return Mono.just(R.success().data(segmentService.id(model, phone)));
    }

    /**
     * 获取雪花ID
     *
     * @return Mono<Result>
     */
    @GetMapping("/snow")
    public Mono<R> snowId() {
        return Mono.just(R.success().data(snowFlakeService.id()));
    }
}
