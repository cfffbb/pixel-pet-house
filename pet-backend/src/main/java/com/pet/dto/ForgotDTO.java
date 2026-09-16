package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码(密保找回):用户名 + 密保答案 + 新密码
 */
@Data
public class ForgotDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密保答案不能为空")
    private String answer;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码至少 8 位")
    private String newPassword;
}
