package com.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 桌面整理分区新增/修改请求
 */
@Data
public class DesktopZoneDTO {

    /** 分区名,如 文档 / 图片 / 其他 */
    @NotBlank(message = "分区名不能为空")
    private String zoneName;

    /** 匹配扩展名,逗号分隔;空表示兜底"其他" */
    private String extensions;

    /** 目标文件夹名(相对桌面);默认等于分区名 */
    private String targetDir;

    /** 1 启用 / 0 停用 */
    private Integer enabled;
}
