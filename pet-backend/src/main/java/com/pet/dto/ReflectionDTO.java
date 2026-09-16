package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 反思答题提交(2 道开放式问题,每题 ≥10 字)
 */
@Data
public class ReflectionDTO {

    @NotBlank(message = "反思题 1 不能为空")
    private String reflection1;

    @NotBlank(message = "反思题 2 不能为空")
    private String reflection2;
}
