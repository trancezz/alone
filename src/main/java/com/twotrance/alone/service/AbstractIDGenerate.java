package com.twotrance.alone.service;

/**
 * IIDGenerate
 *
 * @author trance
 * @description abstract id generate
 */
public abstract class AbstractIDGenerate {

    public void init() {};

    public abstract long id(String bizKey);
}
