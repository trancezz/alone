package com.twotrance.alone.config;

import com.twotrance.alone.common.Constants;
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
    private Map<Long, String> commons = new HashMap<>();

    @Getter
    @Setter
    private Map<Long, String> snowflakes = new HashMap<>();

    @Getter
    @Setter
    private Map<Long, String> autos = new HashMap<>();

    public ServerCommonException exception(long code, int type) {
        return exception(code, type, null);
    }

    public ServerCommonException exception(long code, int type, String params) {
        if (Constants.EXCEPTION_TYPE_AUTO == type)
            return new ServerCommonException(code, MessageUtil.format(autos.get(code), params));
        if (Constants.EXCEPTION_TYPE_SNOWFLAKE == type)
            return new ServerCommonException(code, MessageUtil.format(snowflakes.get(code), params));
        if (Constants.EXCEPTION_TYPE_COMMON == type)
            return new ServerCommonException(code, MessageUtil.format(commons.get(code), params));
        return new ServerCommonException(1000, "unknown exception");
    }

}
