package com.twotrance.alone.common.vo;

import lombok.Getter;

/**
 * R
 *
 * @author trance
 * @description 结果类
 * @date 2021/3/8
 */
@Getter
public class R {

    private Integer code;
    private String message;
    private Object data;

    public static R success() {
        R r = new R();
        r.code(200);
        r.message("ok");
        r.data(-1);
        return r;
    }

    public static R error() {
        R r = new R();
        r.code(500);
        r.message("server error");
        return r;
    }

    public R message(String message) {
        this.message = message;
        return this;
    }

    public R code(Integer code) {
        this.code = code;
        return this;
    }

    public R data(Object data) {
        this.data = data;
        return this;
    }
}
