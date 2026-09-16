package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报(管理员审核后可拉黑账户)
 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人 */
    private Long reporterId;

    /** 被举报用户 */
    private Long targetId;

    /** 关联消息(收件箱消息 id) */
    private Long messageId;

    /** 举报说明 */
    private String content;

    /** PENDING 待审核 / BLOCKED 已拉黑 / DISMISSED 已驳回 */
    private String status;

    private LocalDateTime createdAt;
}
