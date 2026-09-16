package com.pet.dto;

import lombok.Data;

/**
 * 管理员修改商店物品(价格/上下架)
 */
@Data
public class ShopItemUpdateDTO {

    /** 价格(游戏币),留空不改 */
    private Integer price;

    /** 1 上架 / 0 下架,留空不改 */
    private Integer enabled;
}
