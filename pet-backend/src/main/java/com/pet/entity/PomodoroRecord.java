package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 番茄钟计时记录
 */
@Data
@TableName("pomodoro_record")
public class PomodoroRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 陪伴的宠物 */
    private Long petId;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 暂停时间(暂停中非空) */
    private LocalDateTime pausedAt;

    /** 已暂停次数(每轮最多 2 次,M7 拍板) */
    private Integer pausedCount;

    /** 累计暂停分钟数 */
    private Integer totalPausedMinutes;

    /** 结束时间 */
    private LocalDateTime endedAt;

    /** 实际专注分钟数 */
    private Integer durationMinutes;

    /** 番茄钟名称(可选,玩家自定义) */
    private String label;

    /** 关联任务ID(可选) */
    private Long taskId;

    /** 1 完成一轮 / 0 中途放弃 */
    private Integer completed;

    /** 获得游戏币(兑换规则 M7 拍板) */
    private Integer coinsEarned;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
