package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 玩耍模式新增/修改(管理员)
 */
@Data
public class PlayModeDTO {

    @NotBlank(message = "模式名不能为空")
    private String name;

    /** emoji 图标 */
    private String icon;

    /** 图片路径(可选) */
    private String image;

    private Integer mood;
    private Integer hunger;
    private Integer exp;
    private Integer enabled;
}
