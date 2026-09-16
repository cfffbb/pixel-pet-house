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
 * 商店物品字典(食物/道具;价格与效果数值 M7 拍板后填充)
 */
@Data
@TableName("shop_item")
public class ShopItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物品编码,如 FISH / BONE */
    private String itemCode;

    /** 物品名 */
    private String itemName;

    /** 图片路径(/uploads/...,可选) */
    private String image;

    /** 类型:FOOD 食物 / TOY 玩具 */
    private String itemType;

    /** 适用种类 type_code,空=通用(M12) */
    private String species;

    /** 价格(游戏币) */
    private Integer price;

    /** 恢复饥饿度 */
    private Integer hungerRestore;

    /** 恢复心情值 */
    private Integer moodRestore;

    /** 食物分类:STAPLE 主食 / SNACK 零食 / MEDICINE 药品 / SPECIAL 特殊 */
    private String foodCategory;

    /** 解锁等级(M12) */
    private Integer minLevel;

    /** 1 上架 / 0 下架 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
