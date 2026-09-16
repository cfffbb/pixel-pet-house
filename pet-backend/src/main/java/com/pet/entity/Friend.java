package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系(M11;双向各一行)
 */
@Data
@TableName("friend")
public class Friend {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 本人 */
    private Long userId;

    /** 好友 */
    private Long friendId;

    /** 免打扰:0 否 / 1 是(Iter-06) */
    private Integer muted;

    /** 是否官方好友:0 普通 / 1 官方(不可删除) */
    private Integer isOfficial;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
