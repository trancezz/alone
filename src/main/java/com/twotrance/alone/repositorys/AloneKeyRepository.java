package com.twotrance.alone.repositorys;

import com.twotrance.alone.model.key.AloneKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * AloneKeyRepository
 *
 * @author trance
 * @description App秘钥仓库
 * @date 2021/3/20
 */
@Repository
public interface AloneKeyRepository extends JpaRepository<AloneKey, String> {
    AloneKey findAloneKeyByPhoneAndKey(@Param("phone") String phone, @Param("key") String key);
}
