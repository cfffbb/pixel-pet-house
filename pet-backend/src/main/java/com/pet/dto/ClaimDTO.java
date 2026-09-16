package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 领取幼崽请求(强制起名)
 */
@Data
public class ClaimDTO {

    @NotNull(message = "幼崽不能为空")
    private Long hatchlingId;

    @NotBlank(message = "必须给幼崽起名")
    private String petName;
}
