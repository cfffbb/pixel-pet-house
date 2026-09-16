package com.pet.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.exception.BusinessException;
import com.pet.dto.PlayModeDTO;
import com.pet.entity.PlayMode;
import com.pet.mapper.PlayModeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 玩耍模式业务(管理员可增删改,宠物玩时按名称查库)
 */
@Service
@RequiredArgsConstructor
public class PlayModeService {

    private final PlayModeMapper playModeMapper;

    public List<PlayMode> listEnabled() {
        return playModeMapper.selectList(new LambdaQueryWrapper<PlayMode>()
                .eq(PlayMode::getEnabled, 1).orderByAsc(PlayMode::getId));
    }

    public List<PlayMode> listAll() {
        return playModeMapper.selectList(new LambdaQueryWrapper<PlayMode>().orderByAsc(PlayMode::getId));
    }

    public PlayMode findByName(String name) {
        return playModeMapper.selectOne(new LambdaQueryWrapper<PlayMode>()
                .eq(PlayMode::getName, name)
                .eq(PlayMode::getEnabled, 1)
                .last("LIMIT 1"));
    }

    public void add(PlayModeDTO dto) {
        if (StrUtil.isBlank(dto.getName())) {
            throw new BusinessException("模式名不能为空");
        }
        Long exist = playModeMapper.selectCount(new LambdaQueryWrapper<PlayMode>()
                .eq(PlayMode::getName, dto.getName()));
        if (exist > 0) {
            throw new BusinessException("模式名已存在");
        }
        PlayMode m = new PlayMode();
        fill(m, dto);
        playModeMapper.insert(m);
    }

    public void update(Long id, PlayModeDTO dto) {
        PlayMode m = playModeMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("模式不存在");
        }
        fill(m, dto);
        playModeMapper.updateById(m);
    }

    public void delete(Long id) {
        playModeMapper.deleteById(id);
    }

    private void fill(PlayMode m, PlayModeDTO dto) {
        m.setName(dto.getName());
        m.setIcon(dto.getIcon());
        m.setImage(dto.getImage());
        m.setMood(dto.getMood() == null ? 0 : dto.getMood());
        m.setHunger(dto.getHunger() == null ? 0 : dto.getHunger());
        m.setExp(dto.getExp() == null ? 0 : dto.getExp());
        m.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
    }
}
