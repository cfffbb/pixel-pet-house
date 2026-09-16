package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码
 */
@Data
public class AdminResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码至少 8 位")
    private String newPassword;
}
