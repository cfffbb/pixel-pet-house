package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 赠送幼崽请求(发到对方收件箱,对方同意后转移)
 */
@Data
public class GiftDTO {

    @NotNull(message = "幼崽不能为空")
    private Long hatchlingId;

    @NotBlank(message = "接收人用户名不能为空")
    private String targetUsername;
}
