package com.pet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回
 */
@Data
@AllArgsConstructor
public class LoginVO {

    /** JWT,前端保存,请求时放请求头 Authorization: Bearer <token> */
    private String token;

    private Long userId;
    private String username;
    private String nickname;
    private String role;
    private String playerId;
}
