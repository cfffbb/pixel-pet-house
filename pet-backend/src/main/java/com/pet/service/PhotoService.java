package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.exception.BusinessException;
import com.pet.entity.PhotoAlbum;
import com.pet.mapper.PhotoAlbumMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拍照相册(每宠物一个相册,可删除)
 */
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoAlbumMapper photoMapper;
    private final UploadService uploadService;
    private final PetService petService;

    /** 拍照并存入当前宠物相册 */
    public Map<String, Object> upload(Long userId, MultipartFile file) {
        var pet = petService.getMyPet(userId);
        if (pet == null) {
            throw new BusinessException("你还没有宠物,先抽蛋领养一只");
        }
        String path = uploadService.save(file, "photos");
        PhotoAlbum p = new PhotoAlbum();
        p.setUserId(userId);
        p.setPetId(pet.getId());
        p.setFilePath(path);
        photoMapper.insert(p);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("path", path);
        data.put("petId", pet.getId());
        return data;
    }

    /** 相册(按宠物分组,相册名=宠物名),支持按宠物名搜索 */
    public List<Map<String, Object>> albums(Long userId, String keyword) {
        List<PhotoAlbum> photos = photoMapper.selectList(new LambdaQueryWrapper<PhotoAlbum>()
                .eq(PhotoAlbum::getUserId, userId).orderByDesc(PhotoAlbum::getId));
        Map<Long, Map<String, Object>> byPet = new LinkedHashMap<>();
        for (PhotoAlbum ph : photos) {
            Map<String, Object> album = byPet.computeIfAbsent(ph.getPetId(), k -> {
                var pet = petService.findPetById(k);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("petId", k);
                m.put("petName", pet != null ? pet.getPetName() : "未知宠物");
                m.put("typeCode", pet != null ? petService.typeCodeOf(pet) : null);
                m.put("gender", pet != null ? pet.getGender() : null);
                m.put("photos", new ArrayList<Map<String, Object>>());
                return m;
            });
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ph.getId());
            item.put("path", ph.getFilePath());
            item.put("createdAt", ph.getCreatedAt());
            ((List<Map<String, Object>>) album.get("photos")).add(item);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> album : byPet.values()) {
            if (keyword != null && !keyword.isBlank()
                    && !String.valueOf(album.get("petName")).contains(keyword)) {
                continue;
            }
            result.add(album);
        }
        return result;
    }

    /** 删除照片(仅本人) */
    public void delete(Long userId, Long photoId) {
        PhotoAlbum ph = photoMapper.selectById(photoId);
        if (ph == null || !ph.getUserId().equals(userId)) {
            throw new BusinessException("照片不存在");
        }
        photoMapper.deleteById(photoId);
    }
}
