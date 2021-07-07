package com.twotrance.alone.service.common;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.twotrance.alone.config.ExceptionHandler;
import com.twotrance.alone.exceptions.ServerCommonException;
import com.twotrance.alone.model.key.AloneKey;
import com.twotrance.alone.service.key.AloneKeyService;

import javax.annotation.Resource;

/**
 * BaseAbstractService
 *
 * @author trance
 * @description base abstract service
 * @date 2021/7/7
 */
public abstract class BaseAbstractService {

    @Resource
    private AloneKeyService aloneKeyService;

    @Resource
    private ExceptionHandler ex;

    public AloneKey validAppKey(String phone, String appKey) {
        AloneKey aloneKey = aloneKeyService.byPhoneAndKey(phone, appKey);
        if (ObjectUtil.isEmpty(aloneKey))
            throw ex.exception(1002);
        return aloneKey;
    }

    public void throwException(Integer code) {
        throw ex.exception(code);
    }

    public ServerCommonException getException(Integer code) {
        throw ex.exception(code);
    }

}
