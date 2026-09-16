package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩耍模式(管理员可增删改,配图)
 */
@Data
@TableName("play_mode")
public class PlayMode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模式名,如 抚摸/跑步/学习 */
    private String name;

    /** emoji 图标 */
    private String icon;

    /** 图片路径(可选) */
    private String image;

    /** 心情变化 */
    private Integer mood;

    /** 饥饿变化(负=消耗) */
    private Integer hunger;

    /** 经验变化 */
    private Integer exp;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
