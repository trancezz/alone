package com.twotrance.alone.controller;

import com.twotrance.alone.common.vo.Result;
import com.twotrance.alone.model.segment.Paragraph;
import com.twotrance.alone.service.segment.ParagraphService;
import com.twotrance.alone.service.segment.SegmentService;
import com.twotrance.alone.service.snowflake.SnowFlakeService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    private SnowFlakeService snowFlakeService;

    @Resource
    private SegmentService segmentService;

    @Resource
    private ParagraphService paragraphService;

    /**
     * 获取号段ID
     */
    @PostMapping("/addModel")
    public Mono<Result> addModel(@RequestBody Paragraph paragraph) {
        return Mono.just(new Result(paragraphService.addModel(paragraph)));
    }

    /**
     * 获取号段ID
     */
    @GetMapping("/seg")
    public Mono<Result> segId(@RequestParam(value = "model", required = false) String model, @RequestParam(value = "phone", required = false) String phone, @RequestParam(value = "appKey", required = false)String appKey) {
        return Mono.just(new Result(segmentService.id(model, phone, appKey)));
    }

    /**
     * 获取雪花ID
     *
     * @return Mono<Result>
     */
    @GetMapping("/snow")
    public Mono<Result> snowId(@RequestParam(value = "phone", required = false) String phone, @RequestParam(value = "appKey", required = false) String appKey) {
        return Mono.just(new Result(snowFlakeService.id(null, phone, appKey)));
    }
}
