package com.twotrance.alone.controller;

import com.twotrance.alone.common.vo.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ErrorController
 *
 * @author trance
 * @description error controller
 * @date 2021/1/25
 */
@RestController
public class ErrorController {

    @GetMapping("/error/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result error404() {
        return new Result(404L, "not found", -1L);
    }

    @GetMapping("/error/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result error500() {
        return new Result(500L, "server error", -1L);
    }
}
