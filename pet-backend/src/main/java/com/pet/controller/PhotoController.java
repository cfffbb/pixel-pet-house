package com.pet.controller;

import com.pet.common.Result;
import com.pet.security.JwtInterceptor;
import com.pet.service.PhotoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 拍照相册接口(需登录;每宠物一个相册,支持搜索/删除)
 */
@RestController
@RequestMapping("/api/photo")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    /** 拍照上传(存入当前宠物相册) */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return Result.ok(photoService.upload((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), file));
    }

    /** 相册(按宠物分组,相册名=宠物名),keyword 按宠物名搜索 */
    @GetMapping("/albums")
    public Result<List<Map<String, Object>>> albums(@RequestParam(required = false) String keyword,
                                                    HttpServletRequest request) {
        return Result.ok(photoService.albums((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), keyword));
    }

    /** 删除照片(仅本人) */
    @DeleteMapping("/{photoId}")
    public Result<Void> delete(@PathVariable Long photoId, HttpServletRequest request) {
        photoService.delete((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), photoId);
        return Result.ok();
    }
}
