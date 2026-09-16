package com.pet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置项修改请求(如 配种授权 allow_breeding)
 */
@Data
public class SettingDTO {

    @NotNull(message = "值不能为空")
    private Boolean value;
}
