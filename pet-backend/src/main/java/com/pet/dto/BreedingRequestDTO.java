package com.pet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 配种申请请求(我的是母方,对方宠物为父方)
 */
@Data
public class BreedingRequestDTO {

    @NotNull(message = "请选择父方宠物")
    private Long fatherPetId;
}
