package com.twotrance.alone.controller;

import com.twotrance.alone.common.vo.R;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @RequestMapping("/error/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R error404() {
        return R.error().code(404).message("not found").data(-1);
    }

    @RequestMapping("/error/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R error500() {
        return R.error().code(500).data(-1);
    }
}
