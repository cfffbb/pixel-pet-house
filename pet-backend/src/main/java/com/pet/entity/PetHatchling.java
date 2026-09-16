package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 幼崽托管(托管所;出生 3–5 只、10% 夭折、每只 5 币/天)
 */
@Data
@TableName("pet_hatchling")
public class PetHatchling {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户(出生在母方托管所) */
    private Long userId;

    /** 母方宠物 pet.id */
    private Long motherPetId;

    /** 父方宠物 pet.id */
    private Long fatherPetId;

    /** 种类 id(50/50 取父母) */
    private Long petTypeId;

    /** 亚种 */
    private String subtypeName;

    /** 性别 */
    private String gender;

    /** 性格 */
    private String personality;

    /** 品质(受父母影响) */
    private String rarity;

    /** STORED 托管中 / FROZEN 欠费冻结 / CLAIMED 已领取 */
    private String status;

    /** 入托时间(托管费计费起点) */
    private LocalDateTime storedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
