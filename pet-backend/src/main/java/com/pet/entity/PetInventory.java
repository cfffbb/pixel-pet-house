package com.pet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物背包(持有物品及数量)
 */
@Data
@TableName("pet_inventory")
public class PetInventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宠物 pet.id */
    private Long petId;

    /** 物品 shop_item.id */
    private Long itemId;

    /** 持有数量 */
    private Integer quantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
