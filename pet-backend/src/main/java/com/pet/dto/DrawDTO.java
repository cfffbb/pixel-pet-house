package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 抽蛋请求(强制起名,最多 12 字;管理员可选填 typeCode 自由选种)
 */
@Data
public class DrawDTO {

    @NotBlank(message = "必须给宠物起名才能领养")
    private String petName;

    /** 仅管理员可用:指定种类编码(如 DRAGON),跳过随机 */
    private String typeCode;
}
