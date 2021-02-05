package com.twotrance.alone.common.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * RedisLuaUtil
 *
 * @author trance
 * @description redis lua util
 * @date 2021/1/30
 */
public class RedisLuaUtil {

    /**
     * 执行RedisLua脚本
     *
     * @param redisTemplate
     * @param filePath
     * @param resultType
     * @param keys
     * @param values
     * @param <T>
     * @return T
     */
    public static <T> T runLua(RedisTemplate redisTemplate, String filePath, Class<T> resultType, List<String> keys, Object... values) {
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(filePath)));
        redisScript.setResultType(resultType);
        return (T) redisTemplate.execute(redisScript, keys, values);
    }
}
