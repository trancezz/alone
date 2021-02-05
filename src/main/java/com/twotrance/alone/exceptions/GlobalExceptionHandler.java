package com.twotrance.alone.exceptions;

import cn.hutool.json.JSONUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler
 *
 * @author trance
 * @description global exception handler
 * @date 2021/1/25
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 未找到业务异常
     *
     * @param e 异常
     * @return ResponseEntity
     */
    @ExceptionHandler(ServerCommonException.class)
    public ResponseEntity serverCommonException(ServerCommonException e) {
        return new ResponseEntity(JSONUtil.createObj().putOnce("code", e.getCode()).putOnce("message", e.getMessage()).toStringPretty(), HttpStatus.valueOf(500));
    }

    /**
     * 未知异常
     *
     * @param e 异常
     * @return ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity exception(Exception e) {
        return new ResponseEntity(JSONUtil.createObj().putOnce("code", 500).putOnce("message", e.getMessage()).toStringPretty(), HttpStatus.valueOf(500));
    }

}
