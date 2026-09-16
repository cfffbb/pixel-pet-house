package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 番茄钟任务(参考番茄Todo)
 */
@Data
@TableName("pomodoro_task")
public class PomodoroTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 任务名称 */
    private String title;

    /** 标签(如 学习/工作/运动/阅读) */
    private String tag;

    /** 预估番茄数 */
    private Integer estimateCount;

    /** 实际完成番茄数 */
    private Integer doneCount;

    /** 0 待办 / 1 已完成 */
    private Integer status;

    /** 排序权重(越小越靠前) */
    private Integer sortOrder;

    /** 关联日期(YYYY-MM-DD,空表示未指定) */
    private String planDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
