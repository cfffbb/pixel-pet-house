package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * API Key 新增/修改请求
 */
@Data
public class ApiKeyDTO {

    /** 服务商,如 openai / qwen / zhipu */
    @NotBlank(message = "服务商不能为空")
    private String provider;

    /** 新增时必填;修改时留空表示不更换 Key */
    private String key;

    /** 可选:自定义接口地址 */
    private String baseUrl;

    /** 对话模型名,如 gpt-4o-mini / qwen-plus */
    private String modelName;

    private String remark;

    /** 1 启用 / 0 停用;为空时默认启用 */
    private Integer enabled;
}
