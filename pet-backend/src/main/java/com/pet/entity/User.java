package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户(含管理员)
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名(唯一) */
    private String username;

    /** BCrypt 哈希,固定 60 位,明文绝不落库 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 玩家ID(如P10001,唯一,用于搜索) */
    private String playerId;

    /** 邮箱(注册验证/找回用) */
    private String email;

    /** 角色:USER 普通用户 / ADMIN 管理员 */
    private String role;

    /** 密保问题(忘记密码用) */
    private String securityQuestion;

    /** 密保答案(BCrypt 哈希) */
    private String securityAnswer;

    /** 状态:1 启用 / 0 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除:0 未删 / 1 已删 */
    @TableLogic
    private Integer deleted;
}
