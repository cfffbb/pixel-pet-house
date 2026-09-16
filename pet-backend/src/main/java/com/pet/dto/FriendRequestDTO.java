package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 好友申请请求
 */
@Data
public class FriendRequestDTO {

    /** 对方用户名(二选一) */
    private String targetUsername;

    /** 对方玩家ID(二选一) */
    private String targetPlayerId;

    /** 对方用户ID(二选一) */
    private Long targetUserId;
}
