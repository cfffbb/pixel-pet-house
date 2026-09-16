package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拉黑(单向)
 */
@Data
@TableName("blacklist")
public class Blacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 本人 */
    private Long userId;

    /** 被拉黑用户 */
    private Long blockedId;

    private LocalDateTime createdAt;
}
