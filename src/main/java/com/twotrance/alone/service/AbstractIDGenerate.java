package com.twotrance.alone.service;

import com.twotrance.alone.service.common.BaseAbstractService;

/**
 * IIDGenerate
 *
 * @author trance
 * @description abstract id generate
 */
public abstract class AbstractIDGenerate extends BaseAbstractService {

    public void init() {};

    public abstract Long id(String bizKey, String phone, String appKey);
}
