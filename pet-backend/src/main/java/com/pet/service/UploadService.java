package com.pet.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.pet.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件上传(图片/照片),存到 uploads/ 目录,返回 /uploads/... 访问路径
 */
@Service
@RequiredArgsConstructor
public class UploadService {

    public String save(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        String original = file.getOriginalFilename() == null ? "file.png" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase() : "png";
        if (!isImageExt(ext)) {
            throw new BusinessException("只支持图片文件(png/jpg/jpeg/gif/webp)");
        }
        String name = RandomUtil.randomString(16) + "." + ext;
        File dir = new File("uploads", subDir == null ? "" : subDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("无法创建上传目录");
        }
        File target = new File(dir, name);
        try {
            file.transferTo(target.getAbsoluteFile());
        } catch (Exception e) {
            throw new BusinessException("上传失败:" + e.getMessage());
        }
        return "/uploads/" + (StrUtil.isBlank(subDir) ? "" : subDir + "/") + name;
    }

    private boolean isImageExt(String ext) {
        return "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)
                || "gif".equals(ext) || "webp".equals(ext) || "bmp".equals(ext);
    }
}
