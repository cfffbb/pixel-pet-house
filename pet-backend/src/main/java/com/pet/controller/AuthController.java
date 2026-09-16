package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.ForgotDTO;
import com.pet.dto.LoginDTO;
import com.pet.dto.LoginVO;
import com.pet.dto.RegisterCodeDTO;
import com.pet.dto.RegisterDTO;
import com.pet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 注册 / 登录 / 忘记密码
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 发送注册邮箱验证码(M10) */
    @PostMapping("/register-code")
    public Result<Void> registerCode(@Valid @RequestBody RegisterCodeDTO dto) {
        userService.sendRegisterCode(dto.getEmail());
        return Result.ok();
    }

    /**
     * 注册(邮箱验证码 + 密保设置)。
     * M2 拍板:表里还没有任何用户时,首个注册的自动成为 ADMIN
     */
    @PostMapping("/register")
    public Result<Map<String, Long>> register(@Valid @RequestBody RegisterDTO dto) {
        Long userId = userService.register(dto);
        return Result.ok(Map.of("userId", userId));
    }

    /**
     * 登录,返回 token。前端保存,后续请求放请求头 Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    /** 忘记密码:取密保问题 */
    @GetMapping("/forgot/question")
    public Result<Map<String, String>> forgotQuestion(@RequestParam String username) {
        return Result.ok(userService.forgotQuestion(username));
    }

    /** 忘记密码:密保答案正确则重置 */
    @PostMapping("/forgot")
    public Result<Void> forgot(@Valid @RequestBody ForgotDTO dto) {
        userService.forgotReset(dto.getUsername(), dto.getAnswer(), dto.getNewPassword());
        return Result.ok();
    }

    // 退出登录:M2 拍板"前端删 token 即退出",后端无需接口
}
