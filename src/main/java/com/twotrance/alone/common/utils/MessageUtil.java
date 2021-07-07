package com.twotrance.alone.common.utils;

import java.text.MessageFormat;

/**
 * MessageUtil
 *
 * @author trance
 * @description 字符串消息工具类
 * @date 2021/2/2
 */
public class MessageUtil {

    public static String format(String data, String... values) {
        return MessageFormat.format(data, values);
    }
}
