package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员测试换宠(检查 UI/执行情况用)
 */
@Data
public class AdminSwitchPetDTO {

    @NotBlank(message = "种类不能为空")
    private String typeCode;

    /** MALE / FEMALE,空默认雄 */
    private String gender;

    /** 性格,空随机 */
    private String personality;
}
