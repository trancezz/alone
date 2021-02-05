package com.twotrance.alone.common.utils;

/**
 * EnThread
 *
 * @author trance
 * @description enhance thread
 */
public class EnThread extends Thread {

    public EnThread(Runnable runnable) {
        super(runnable);
    }


    public EnThread rename(String name) {
        super.setName(name);
        return this;
    }

    public EnThread daemon(boolean flag) {
        super.setDaemon(flag);
        return this;
    }

}
