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
 * 日程任务(右下角每日推送的数据来源)
 */
@Data
@TableName("schedule")
public class Schedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 提醒时间(右下角推送依据) */
    private LocalDateTime remindAt;

    /** 重复:ONCE 一次 / DAILY 每天 / WEEKLY 每周 */
    private String repeatType;

    /** 优先级:1 紧急 / 2 高 / 3 中 / 4 低 */
    private Integer priority;

    /** 状态:PENDING 待办 / DONE 已完成 */
    private String status;

    /** 标签(学习/工作/生活/健康/其他,可自定义) */
    private String tag;

    /** 上次提醒时间(去重用,ONCE 一次 / DAILY 每天 / WEEKLY 每周) */
    private LocalDateTime lastNotifiedAt;

    /** 完成时间(统计/工资单用) */
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
