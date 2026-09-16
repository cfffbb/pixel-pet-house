package com.pet.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.exception.BusinessException;
import com.pet.dto.DesktopZoneDTO;
import com.pet.entity.DesktopOrganizeRecord;
import com.pet.entity.DesktopZone;
import com.pet.mapper.DesktopOrganizeRecordMapper;
import com.pet.mapper.DesktopZoneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 桌面整理分区与记录业务(M8)
 * 文件扫描/移动由 Electron 主进程做,后端只存规则与记录。
 */
@Service
@RequiredArgsConstructor
public class DesktopZoneService {

    /** 默认分区(M8 拍板):文档/图片/视频/音频/安装包 + 其他(兜底) */
    private static final List<String[]> DEFAULT_ZONES = List.of(
            new String[]{"文档", "doc,docx,pdf,txt,md,xlsx,pptx,csv"},
            new String[]{"图片", "jpg,jpeg,png,gif,webp,bmp,svg"},
            new String[]{"视频", "mp4,avi,mkv,mov,wmv,flv"},
            new String[]{"音频", "mp3,wav,flac,aac,ogg"},
            new String[]{"安装包", "exe,msi,zip,rar,7z,iso"},
            new String[]{"其他", ""}
    );

    private final DesktopZoneMapper zoneMapper;
    private final DesktopOrganizeRecordMapper recordMapper;

    /** 我的分区列表;首次访问自动建默认分区 */
    public List<DesktopZone> list(Long userId) {
        LambdaQueryWrapper<DesktopZone> qw = new LambdaQueryWrapper<DesktopZone>()
                .eq(DesktopZone::getUserId, userId).orderByAsc(DesktopZone::getId);
        List<DesktopZone> zones = zoneMapper.selectList(qw);
        if (zones.isEmpty()) {
            for (String[] d : DEFAULT_ZONES) {
                DesktopZone z = new DesktopZone();
                z.setUserId(userId);
                z.setZoneName(d[0]);
                z.setExtensions(d[1]);
                z.setTargetDir(d[0]);
                z.setEnabled(1);
                zoneMapper.insert(z);
            }
            zones = zoneMapper.selectList(qw);
        }
        return zones;
    }

    public void add(Long userId, DesktopZoneDTO dto) {
        DesktopZone z = new DesktopZone();
        z.setUserId(userId);
        fill(z, dto);
        zoneMapper.insert(z);
    }

    public void update(Long userId, Long id, DesktopZoneDTO dto) {
        DesktopZone z = requireOwn(userId, id);
        fill(z, dto);
        zoneMapper.updateById(z);
    }

    public void delete(Long userId, Long id) {
        requireOwn(userId, id);
        zoneMapper.deleteById(id);
    }

    private void fill(DesktopZone z, DesktopZoneDTO dto) {
        z.setZoneName(dto.getZoneName().trim());
        z.setExtensions(dto.getExtensions());
        z.setTargetDir(StrUtil.isBlank(dto.getTargetDir()) ? dto.getZoneName().trim() : dto.getTargetDir().trim());
        z.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
    }

    /** 保存一次整理记录(客户端移动完成后上报) */
    public void saveRecord(Long userId, int movedCount, String detailsJson) {
        DesktopOrganizeRecord r = new DesktopOrganizeRecord();
        r.setUserId(userId);
        r.setMovedCount(movedCount);
        r.setDetailsJson(detailsJson);
        r.setUndone(0);
        r.setOrganizedAt(LocalDateTime.now());
        recordMapper.insert(r);
    }

    /** 最近一次未撤销的整理记录(含明细,供撤销) */
    public Map<String, Object> latest(Long userId) {
        DesktopOrganizeRecord r = recordMapper.selectOne(new LambdaQueryWrapper<DesktopOrganizeRecord>()
                .eq(DesktopOrganizeRecord::getUserId, userId)
                .eq(DesktopOrganizeRecord::getUndone, 0)
                .orderByDesc(DesktopOrganizeRecord::getId)
                .last("LIMIT 1"));
        Map<String, Object> data = new LinkedHashMap<>();
        if (r == null) {
            data.put("id", null);
            data.put("details", null);
            return data;
        }
        data.put("id", r.getId());
        data.put("movedCount", r.getMovedCount());
        data.put("organizedAt", r.getOrganizedAt());
        data.put("details", StrUtil.isBlank(r.getDetailsJson()) ? null : cn.hutool.json.JSONUtil.parseArray(r.getDetailsJson()));
        return data;
    }

    public void markUndone(Long userId, Long id) {
        DesktopOrganizeRecord r = recordMapper.selectById(id);
        if (r == null || !r.getUserId().equals(userId)) {
            throw new BusinessException("记录不存在");
        }
        r.setUndone(1);
        recordMapper.updateById(r);
    }

    private DesktopZone requireOwn(Long userId, Long id) {
        DesktopZone z = zoneMapper.selectById(id);
        if (z == null || !z.getUserId().equals(userId)) {
            throw new BusinessException("分区不存在");
        }
        return z;
    }
}
