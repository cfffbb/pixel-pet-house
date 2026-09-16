package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 喂食流水(审计用,方便排查饿死 / 恢复问题)
 */
@Data
@TableName("feeding_record")
public class FeedingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宠物 pet.id */
    private Long petId;

    /** 食物 shop_item.id */
    private Long itemId;

    /** 喂食前饥饿度 */
    private Integer hungerBefore;

    /** 喂食后饥饿度 */
    private Integer hungerAfter;

    /** 喂食前心情 */
    private Integer moodBefore;

    /** 喂食后心情 */
    private Integer moodAfter;

    /** 食物名称(冗余,方便前端展示) */
    private String itemName;

    /** 偏好:NORMAL/LIKE/DISLIKE */
    private String preference;

    /** 幸运事件类型(无则null) */
    private String luckyEvent;

    /** 获得经验 */
    private Integer expGain;

    /** 喂食时间 */
    private LocalDateTime fedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
