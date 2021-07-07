package com.twotrance.alone.model.segment;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 * Paragraph
 *
 * @author trance
 * @description 区间
 * @date 2021/3/20
 */
@Getter
@Setter
@Entity
@Table(name = "paragraph")
public class Paragraph {
    // 主键ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 手机号码
    @Column(name = "phone", length = 11, nullable = false)
    private String phone;
    // 模块名称
    @Column(name = "model", unique = true, length = 128, nullable = false)
    private String model;
    // 当前此区间模块产生的最大ID
    @Column(name = "max", length = 20, nullable = false)
    private Long max;
    // 区间长度
    @Column(name = "length", length = 20, nullable = false)
    private Long length;
    // 备注
    @Column(name = "desc", length = 500)
    private String desc;
    // 是否删除
    @Column(name = "is_delete", nullable = false, length = 1)
    private Boolean delete;
    // 创建时间
    @Column(name = "create_time", nullable = false)
    private Date createTime;
    // 更新时间
    @Column(name = "update_time")
    private Date updateTime;
    // 删除时间
    @Column(name = "delete_time")
    private Date deleteTime;

    // 非映射字段, 秘钥
    @Transient
    private String appKey;
}
