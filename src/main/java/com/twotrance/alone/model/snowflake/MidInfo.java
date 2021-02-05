package com.twotrance.alone.model.snowflake;

import lombok.*;

import java.io.Serializable;


/**
 * MidInfo
 *
 * @author trance
 * @description mid info
 * @date 2021/1/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MidInfo implements Serializable {
    private String ipAndPort;
    private long mid;
    private long timestamp;
}
