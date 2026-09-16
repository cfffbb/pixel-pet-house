package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配种记录(申请→同意→怀孕→产崽)
 */
@Data
@TableName("breeding_record")
public class BreedingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 母方宠物 pet.id */
    private Long motherPetId;

    /** 父方宠物 pet.id */
    private Long fatherPetId;

    /** 发起人(母方主人) */
    private Long requestedBy;

    /** PENDING 待对方同意 / PREGNANT 怀孕中 / BORN 已产崽 / REJECTED 已拒绝 */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
