package com.twotrance.alone.config;

import com.twotrance.alone.common.utils.MessageUtil;
import com.twotrance.alone.exceptions.ServerCommonException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ExceptionMsgProperties
 *
 * @author trance
 * @description
 * @date 2021/2/2
 */
@Component
@ConfigurationProperties("exceptions")
@PropertySource(value = "classpath:exception.properties", encoding = "utf-8")
public class ExceptionMsgProperties {

    @Getter
    @Setter
    private Map<Long, String> errors = new HashMap<>();

    public ServerCommonException exception(long code) {
        return new ServerCommonException(code, errors.get(code));
    }

    public ServerCommonException exception(long code, String... params) {
        return new ServerCommonException(code, MessageUtil.format(errors.get(code), params));
    }

}
