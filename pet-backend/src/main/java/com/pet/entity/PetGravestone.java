package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物墓碑(死亡纪念 + 反思答题记录)
 */
@Data
@TableName("pet_gravestone")
public class PetGravestone {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 */
    private Long userId;

    /** 原宠物 pet.id */
    private Long petId;

    /** 宠物名 */
    private String petName;

    /** 种类 id */
    private Long petTypeId;

    /** 种类名(快照) */
    private String typeName;

    /** 亚种(快照) */
    private String subtypeName;

    /** 性别(快照) */
    private String gender;

    /** 性格(快照) */
    private String personality;

    /** 出生时间 */
    private LocalDateTime bornAt;

    /** 死亡时间 */
    private LocalDateTime diedAt;

    /** 存活天数 */
    private Long lifespanDays;

    /** 1 已完成反思答题 / 0 未答 */
    private Integer answered;

    /** 反思题 1 回答(死因标注) */
    private String reflection1;

    /** 反思题 2 回答 */
    private String reflection2;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
