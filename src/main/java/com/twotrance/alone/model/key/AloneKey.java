package com.twotrance.alone.model.key;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * AloneKey
 *
 * @author trance
 * @description App秘钥
 * @date 2021/6/30
 */
@Getter
@Setter
@Entity
@Table(name = "alone_key")
public class AloneKey {

    @Id
    @Column(name = "phone", unique = true, length = 11, nullable = false)
    private String phone;

    @Column(name = "key", unique = true, length = 36, nullable = false)
    private String key;

    @Column(name = "admin", length = 1, nullable = false)
    private Boolean admin;
}
