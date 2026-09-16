package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物亚种字典(仅展示,不影响数值)
 */
@Data
@TableName("pet_subtype")
public class PetSubtype {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属种类 pet_type.id */
    private Long petTypeId;

    /** 亚种名,如 布偶猫 */
    private String subtypeName;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
