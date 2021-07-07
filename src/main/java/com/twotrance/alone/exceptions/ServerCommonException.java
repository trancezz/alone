package com.twotrance.alone.exceptions;

import lombok.Getter;

/**
 * ServerCommonException
 *
 * @author trance
 * @description server common exception
 */
public class ServerCommonException extends RuntimeException {

    @Getter
    private long code;

    @Getter
    private String message;

    public ServerCommonException(long code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
