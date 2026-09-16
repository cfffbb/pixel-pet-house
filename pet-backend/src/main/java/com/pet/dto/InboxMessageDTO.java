package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收件箱聊天消息
 */
@Data
public class InboxMessageDTO {

    @NotNull(message = "接收人不能为空")
    private Long receiverId;

    @NotBlank(message = "内容不能为空")
    private String content;
}
