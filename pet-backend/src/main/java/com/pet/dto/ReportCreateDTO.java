package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 举报请求
 */
@Data
public class ReportCreateDTO {

    @NotNull(message = "被举报用户不能为空")
    private Long targetUserId;

    /** 关联收件箱消息 id(可选) */
    private Long messageId;

    @NotBlank(message = "举报说明不能为空")
    private String content;
}
