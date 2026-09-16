package com.pet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员:启用/禁用用户请求
 */
@Data
public class AdminUserStatusDTO {

    /** 1 启用 / 0 禁用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
