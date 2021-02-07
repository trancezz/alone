package com.twotrance.alone.common;

/**
 * Constants
 *
 * @author trance
 * @description constants
 * @date 2021/1/29
 */
public class Constants {
    /**
     * log prefix placeholder template
     */
    public final static String LOG_PREFIX_PLACEHOLDER_MODE = "=> {}";

    /**
     * machine id information key
     */
    public final static String HASH_MACHINE_ID = "ALONE:HASH_MACHINE_ID";

    /**
     * machine id information lock
     */
    public final static String HASH_MACHINE_LOCK = "ALONE:HASH_MACHINE_LOCK";

    /**
     * common exception type
     */
    public final static int EXCEPTION_TYPE_COMMON = 1;

    /**
     * self-incrementing id exception type
     */
    public final static int EXCEPTION_TYPE_AUTO = 2;

    /**
     * snowflake ID exception type
     */
    public final static int EXCEPTION_TYPE_SNOWFLAKE = 3;

}