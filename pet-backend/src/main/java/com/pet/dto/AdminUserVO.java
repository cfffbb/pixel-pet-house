package com.pet.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员视角的用户列表项
 */
@Data
public class AdminUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String role;
    private Integer status;

    /** 是否配置过 API Key(管理员只看是否配置,M2 拍板) */
    private Boolean hasApiKey;

    private LocalDateTime createdAt;
}
