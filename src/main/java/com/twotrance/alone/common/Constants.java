package com.twotrance.alone.common;

import java.io.File;

/**
 * Constants
 *
 * @author trance
 * @description constants
 * @date 2021/1/29
 */
public class Constants {
    /**
     * 日志前缀占位模板
     */
    public final static String LOG_PREFIX_PLACEHOLDER_MODE = "=> {}";

    /**
     * 机器ID在Redis中的根键
     */
    public final static String HASH_MACHINE_ID = "ALONE:HASH_MACHINE_ID";

    /**
     * 机器ID使用情况位图键
     */
    public final static String BM_MACHINE_ID_USAGE = "ALONE:BM_MACHINE_ID_USAGE";

    /**
     * 机器ID本地存储路径
     */
    public final static String MACHINE_ID_LOCAL_STORE_PATH = System.getProperty("java.io.tmpdir") + File.separator + "/module_alone/machine_ids/machine-{-}.properties";

    /**
     * Redis机器ID信息锁
     */
    public final static String HASH_MACHINE_LOCK = "ALONE:HASH_MACHINE_LOCK";

}