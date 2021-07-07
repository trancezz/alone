package com.twotrance.alone.common.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * Result
 *
 * @author trance
 * @description 结果类
 * @date 2021/3/8
 */
@Getter
@Setter
public class Result<T> {

    private Long code = 200L;
    private String message = "ok";
    private T data;

    public Result(){}

    public Result(Long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(T data) {
        this.data = data;
    }
}
