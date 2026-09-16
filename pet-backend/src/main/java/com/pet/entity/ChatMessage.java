package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天记录(与宠物的对话,含语音来源标记;只增不改)
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 宠物 pet.id */
    private Long petId;

    /** 角色:USER 用户 / ASSISTANT 宠物 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 1 语音来源 / 0 文字 */
    private Integer isVoice;

    /** 本次对话实际使用的服务商 */
    private String provider;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
