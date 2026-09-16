package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物拍照相册(每宠物一个相册)
 */
@Data
@TableName("photo_album")
public class PhotoAlbum {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 */
    private Long userId;

    /** 宠物 pet.id */
    private Long petId;

    /** 图片路径(/uploads/photos/...) */
    private String filePath;

    private LocalDateTime createdAt;
}
