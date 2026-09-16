package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用使用采样记录(M9;隐私红线:仅进程名/窗口标题)
 */
@Data
@TableName("app_usage_log")
public class AppUsageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 应用进程名,如 idea64 / chrome */
    private String appName;

    /** 窗口标题(可选) */
    private String windowTitle;

    /** 采样时间 */
    private LocalDateTime sampledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
