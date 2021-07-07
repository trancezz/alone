package com.twotrance.alone.exceptions;

import com.twotrance.alone.common.vo.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
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
     * 服务通用异常
     *
     * @param e 异常
     * @return Result
     */
    @ExceptionHandler(ServerCommonException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result serverCommonException(ServerCommonException e) {
        return new Result(e.getCode(), e.getMessage(), -1L);
    }

    /**
     * 未知异常
     *
     * @return Result
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result exception(Exception e) {
        e.printStackTrace();
        return new Result(1000L, "unknown exception", -1L);
    }


}
