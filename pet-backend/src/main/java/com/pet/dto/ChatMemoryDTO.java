package com.pet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员设置对话记忆条数
 */
@Data
public class ChatMemoryDTO {

    @NotNull(message = "条数不能为空")
    private Integer value;
}
