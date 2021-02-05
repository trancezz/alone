package com.twotrance.alone.controller;

import cn.hutool.json.JSONUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity error404() {
        return new ResponseEntity(JSONUtil.createObj().putOnce("code", 404).putOnce("message", "未找到相关内容").toStringPretty(), HttpStatus.valueOf(404));
    }

    @GetMapping("/error/500")
    public ResponseEntity error500() {
        return new ResponseEntity(JSONUtil.createObj().putOnce("code", 500).putOnce("message", "服务器异常").toStringPretty(), HttpStatus.valueOf(500));
    }
}
