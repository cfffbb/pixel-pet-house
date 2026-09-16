package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收件箱消息(配种/赠送申请 + 双方聊天,双方同意制)
 */
@Data
@TableName("inbox_message")
public class InboxMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送人 */
    private Long senderId;

    /** 接收人 */
    private Long receiverId;

    /** BREED_REQUEST 配种申请 / GIFT_REQUEST 赠送申请 / CHAT 聊天 */
    private String type;

    /** 关联:配种记录 id 或幼崽 id */
    private Long refId;

    /** 内容(申请备注或聊天文本) */
    private String content;

    /** UNREAD 未读 / READ 已读 */
    private String status;

    /** 申请类消息:PENDING 待处理 / ACCEPTED 已同意 / REJECTED 已拒绝 */
    private String requestStatus;

    /** 是否官方推送:0 普通 / 1 官方 */
    private Integer isOfficial;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
