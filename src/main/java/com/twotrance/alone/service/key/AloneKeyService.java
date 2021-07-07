package com.twotrance.alone.service.key;

import com.twotrance.alone.model.key.AloneKey;
import com.twotrance.alone.repositorys.AloneKeyRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * AloneKeyService
 *
 * @author trance
 * @description 秘钥服务
 * @date 2021/6/30
 */
@Service
public class AloneKeyService {

    @Resource
    private AloneKeyRepository aloneKeyRepository;

    public AloneKey byPhoneAndKey(String phone, String appKey) {
        return aloneKeyRepository.findAloneKeyByPhoneAndKey(phone, appKey);
    }

}
