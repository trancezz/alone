package com.twotrance.alone.config;

import cn.hutool.core.util.StrUtil;
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
 * ExceptionHandler
 *
 * @author trance
 * @description exception handler
 * @date 2021/2/2
 */
@Component
@ConfigurationProperties("exception")
@PropertySource(value = "classpath:exception.properties", encoding = "utf-8")
public class ExceptionHandler {

    @Getter
    @Setter
    private Map<Integer, String> codes = new HashMap<>();


    public ServerCommonException exception(Integer code) {
        return exception(code, null);
    }

    public ServerCommonException exception(Integer code, String... params) {
        String message = codes.get(code);
        if (StrUtil.isBlank(message)) {
            code = 1001;
            message = codes.get(code);
        }
        return new ServerCommonException(code, MessageUtil.format(message, params));
    }

}
