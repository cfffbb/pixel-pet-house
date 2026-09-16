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
 * 宠物种类字典(开局抽蛋的候选池)
 */
@Data
@TableName("pet_type")
public class PetType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 种类编码,如 CAT / DOG / DRAGON */
    private String typeCode;

    /** 展示名,如 布偶猫 */
    private String typeName;

    /** 稀有度:NORMAL 普通 / RARE 稀有 / LEGEND 传说 */
    private String rarity;

    /** 基准饿死天数(3–7,按体型代谢,M3 拍板) */
    private Integer baseDays;

    /** 描述 */
    private String description;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
