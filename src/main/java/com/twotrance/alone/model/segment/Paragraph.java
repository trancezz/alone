package com.twotrance.alone.model.segment;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
@ToString
@Entity
@Table(name = "paragraph")
public class Paragraph {
    // 主键ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 手机号码
    @NotNull(message = "{Phone.NotNull}")
    @Pattern(message = "{Phone.Pattern}", regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$")
    @Column(name = "phone", length = 11, nullable = false)
    private String phone;
    // 模块名称
    @NotNull(message = "{Model.NotNull}")
    @Column(name = "model", unique = true, nullable = false, length = 128)
    private String model;
    // 当前此区间模块产生的最大ID
    @Column(name = "max", length = 20)
    private Long max;
    // 区间长度
    @NotNull(message = "{Length.NotNull}")
    @Min(value = 1, message = "{Length.Min}")
    @Max(value = 10000, message = "{Length.Max}")
    @Column(name = "length", nullable = false, length = 20)
    private Long length;
    // 备注
    @Column(name = "remark", length = 500)
    private String remark;
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
    @NotNull(message = "{AppKey.NotNull}")
    @Transient
    private String appKey;
}
