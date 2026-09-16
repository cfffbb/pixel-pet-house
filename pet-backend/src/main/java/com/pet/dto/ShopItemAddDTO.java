package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员新增商店物品
 */
@Data
public class ShopItemAddDTO {

    @NotBlank(message = "物品名不能为空")
    private String itemName;

    @NotBlank(message = "类型不能为空")
    private String itemType;

    /** 适用种类 type_code,空=通用 */
    private String species;

    /** 图片路径(可选) */
    private String image;

    @NotNull(message = "价格不能为空")
    private Integer price;

    private Integer hungerRestore;
    private Integer moodRestore;
    private Integer minLevel;
    private Integer enabled;
}
