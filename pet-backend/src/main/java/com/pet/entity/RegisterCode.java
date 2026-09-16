package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 注册邮箱验证码(M10)
 */
@Data
@TableName("register_code")
public class RegisterCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标邮箱 */
    private String email;

    /** 验证码(6 位数字) */
    private String code;

    /** 过期时间(10 分钟) */
    private LocalDateTime expiresAt;

    /** 1 已使用 */
    private Integer used;

    private LocalDateTime createdAt;
}
