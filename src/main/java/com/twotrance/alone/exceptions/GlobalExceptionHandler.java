package com.twotrance.alone.exceptions;

import cn.hutool.core.util.ArrayUtil;
import com.twotrance.alone.common.vo.R;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

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
     * @return R
     */
    @ExceptionHandler(ServerCommonException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R serverCommonException(ServerCommonException e) {
        return R.error().code(e.getCode()).message(e.getMessage()).data(-1);
    }

    /**
     * 参数异常
     *
     * @param e 异常
     * @return R
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R serverCommonException(MethodArgumentNotValidException e) {
        return R.error().code(999).message(e.getBindingResult().getAllErrors().stream().map(error -> error.getDefaultMessage()).collect(Collectors.joining(","))).data(-1);
    }

    /**
     * 未知异常
     *
     * @return R
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R exception(Exception e) {
        e.printStackTrace();
        return R.error().code(1000).message("unknown exception").data(-1);
    }


}
