package com.twotrance.alone.controller;

import cn.hutool.core.util.ObjectUtil;
import com.twotrance.alone.common.vo.R;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.model.segment.Paragraph;
import com.twotrance.alone.service.segment.ParagraphService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * ModelController
 *
 * @author trance
 * @description model controller
 */
@RestController
@RequestMapping("/model")
public class ModelController {

    @Resource
    private ParagraphService paragraphService;

    @Resource
    private ExceptionHandler ex;

    @PostMapping
    public Mono<R> addModel(@RequestBody @Validated Paragraph paragraph) {
        Paragraph resultParagraph = paragraphService.addModel(paragraph);
        if (ObjectUtil.isEmpty(resultParagraph))
            return Mono.just(R.error().code(3006).message(ex.getMessage(3006)));
        return Mono.just(R.success().data(resultParagraph));
    }

    @PutMapping
    public Mono<R> editModel(@RequestParam("model") String model, @RequestParam("len") Long len, HttpServletRequest request) {
        paragraphService.updateOfLen(len, model, request.getAttribute("phone").toString());
        return Mono.just(R.success());
    }

    @DeleteMapping("/{model}")
    public Mono<R> delModel(@PathVariable("model") String model, HttpServletRequest request) {
        Paragraph resultParagraph = paragraphService.delModel(model, request.getAttribute("phone").toString());
        if (ObjectUtil.isEmpty(resultParagraph))
            return Mono.just(R.error().code(3007).message(ex.getMessage(3007)));
        return Mono.just(R.success().data(resultParagraph));
    }

}
