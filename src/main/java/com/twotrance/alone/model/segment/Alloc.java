package com.twotrance.alone.model.segment;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * Alloc
 *
 * @author trance
 * @description alloc
 */
@Setter
@Getter
@ToString
@TableName("alloc")
public class Alloc {
    @TableId
    private String bizKey;
    private long maxId;
    private int step;
    private String description;
    private Date updateTime;
}
