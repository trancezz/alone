package com.twotrance.alone.repositorys;

import com.twotrance.alone.model.segment.Paragraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * ParagraphRepository
 *
 * @author trance
 * @description 区间仓库
 * @date 2021/3/20
 */
@Repository
public interface ParagraphRepository extends JpaRepository<Paragraph, Long> {

    Paragraph findByPhoneAndModel(@Param("phone") String phone, @Param("model") String model);

    @Query(value = "select p.model from Paragraph as p")
    Set<String> models();

    @Modifying
    @Query(value = "update Paragraph as p set p.max = :len + p.max, p.length = :len, p.updateTime = current_date where p.model = :model and p.max = :oldMax")
    void updateOfMax(@Param("len") Long len, @Param("model") String model, @Param("oldMax") Long oldMax);

    @Modifying
    @Query(value = "update Paragraph as p set p.length = :len, p.updateTime = current_date where p.model = :model and p.phone = :phone and p.length = :oldLen")
    void updateOfLen(@Param("len") Long len, @Param("model") String model, @Param("phone") String phone, @Param("oldLen") Long oldLen);

}
