package com.twotrance.alone.controller;

import cn.hutool.core.util.ObjectUtil;
import com.twotrance.alone.common.vo.R;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.model.segment.Paragraph;
import com.twotrance.alone.service.segment.ParagraphService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

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
            return Mono.just(R.error().code(3006).message(ex.getMessage(3006)).data(-1));
        return Mono.just(R.success().data(resultParagraph));
    }

}
