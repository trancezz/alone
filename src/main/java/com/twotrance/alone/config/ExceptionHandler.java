package com.twotrance.alone.config;

import cn.hutool.core.util.StrUtil;
import com.twotrance.alone.common.utils.MessageUtil;
import com.twotrance.alone.exceptions.ServerCommonException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.MessageFormat;

/**
 * ExceptionHandler
 *
 * @author trance
 * @description exception handler
 * @date 2021/2/2
 */
@Component
public class ExceptionHandler {

    @Resource(name = "messageSource")
    private MessageSource messageSource;

    public String getMessage(Integer code) {
        return messageSource.getMessage(MessageFormat.format("exception.codes[{0}]", code.toString()), null, LocaleContextHolder.getLocale());
    }

    public ServerCommonException exception(Integer code) {
        return exception(code, null);
    }

    public ServerCommonException exception(Integer code, String... params) {
        String message = messageSource.getMessage(MessageFormat.format("exception.codes[{0}]", code.toString()), null, LocaleContextHolder.getLocale());
        if (StrUtil.isBlank(message)) {
            code = 1001;
            message = messageSource.getMessage(MessageFormat.format("exception.codes[{0}]", code.toString()), null, LocaleContextHolder.getLocale());
        }
        return new ServerCommonException(code, MessageUtil.format(message, params));
    }

}
