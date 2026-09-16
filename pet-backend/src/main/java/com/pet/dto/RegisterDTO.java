package com.pet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求(M10 更新:邮箱验证码 + 密保设置)
 * M2 拍板:用户名 3–20 位字母/数字/下划线,密码至少 8 位
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "用户名须为 3–20 位字母/数字/下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少 8 位")
    private String password;

    /** 可选昵称,不填则默认等于用户名 */
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    private String code;

    @NotBlank(message = "密保问题不能为空")
    private String securityQuestion;

    @NotBlank(message = "密保答案不能为空")
    private String securityAnswer;
}
