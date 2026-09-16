package com.pet.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.entity.*;
import com.pet.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 气泡提示库服务(Iter-05)
 * 按宠物种类/性格/性别/阶段组合,提供多样化气泡文本
 */
@Service
@RequiredArgsConstructor
public class BubblePromptService {

    private final BubblePromptMapper bubblePromptMapper;
    private final PetMapper petMapper;
    private final PetTypeMapper petTypeMapper;

    /**
     * 获取一条随机气泡(按宠物属性匹配)
     */
    public String randomBubble(Long userId, String category) {
        Pet pet = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .in(Pet::getStatus, PetConstants.PET_ALIVE, PetConstants.PET_DANGER)
                .last("LIMIT 1"));

        String species = null;
        String personality = null;
        String gender = null;
        String stage = null;

        if (pet != null) {
            PetType type = petTypeMapper.selectById(pet.getPetTypeId());
            if (type != null) {
                species = type.getTypeCode();
            }
            personality = pet.getPersonality();
            gender = pet.getGender();
            // 从 level 推导成长阶段
            int lv = pet.getLevel() != null ? pet.getLevel() : 1;
            if (lv < 10) stage = "BABY";
            else if (lv < 30) stage = "JUVENILE";
            else stage = "ADULT";
        }

        return pickBubble(species, personality, gender, stage, category);
    }

    /**
     * 多级匹配:精确匹配 → 种类匹配 → 通用匹配
     */
    private String pickBubble(String species, String personality, String gender, String stage, String category) {
        // 第一优先:种类+性格+性别+阶段 全匹配
        List<BubblePrompt> pool = query(species, personality, gender, stage, category);
        if (pool.isEmpty()) {
            // 第二优先:种类+性格
            pool = query(species, personality, null, null, category);
        }
        if (pool.isEmpty()) {
            // 第三优先:种类
            pool = query(species, null, null, null, category);
        }
        if (pool.isEmpty()) {
            // 第四优先:通用
            pool = query(null, null, null, null, category);
        }
        if (pool.isEmpty()) {
            // 兜底
            pool = query(null, null, null, null, "random");
        }
        if (pool.isEmpty()) {
            return "主人好呀~";
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size())).getContent();
    }

    private List<BubblePrompt> query(String species, String personality, String gender, String stage, String category) {
        LambdaQueryWrapper<BubblePrompt> qw = new LambdaQueryWrapper<BubblePrompt>()
                .eq(BubblePrompt::getEnabled, 1)
                .eq(BubblePrompt::getCategory, category);
        if (StrUtil.isNotBlank(species)) {
            qw.eq(BubblePrompt::getSpecies, species);
        } else {
            qw.isNull(BubblePrompt::getSpecies);
        }
        if (StrUtil.isNotBlank(personality)) {
            qw.eq(BubblePrompt::getPersonality, personality);
        } else {
            qw.and(w -> w.isNull(BubblePrompt::getPersonality).or().eq(BubblePrompt::getPersonality, ""));
        }
        if (StrUtil.isNotBlank(gender)) {
            qw.eq(BubblePrompt::getGender, gender);
        } else {
            qw.and(w -> w.isNull(BubblePrompt::getGender).or().eq(BubblePrompt::getGender, ""));
        }
        if (StrUtil.isNotBlank(stage)) {
            qw.eq(BubblePrompt::getStage, stage);
        } else {
            qw.and(w -> w.isNull(BubblePrompt::getStage).or().eq(BubblePrompt::getStage, ""));
        }
        return bubblePromptMapper.selectList(qw);
    }

    /**
     * 获取多条随机气泡(悬浮窗轮播用)
     */
    public List<String> randomBubbles(Long userId, int count) {
        List<String> categories = List.of("greeting", "happy", "play", "random", "study");
        Set<String> result = new LinkedHashSet<>();
        for (int i = 0; i < count * 3 && result.size() < count; i++) {
            String cat = categories.get(ThreadLocalRandom.current().nextInt(categories.size()));
            result.add(randomBubble(userId, cat));
        }
        return new ArrayList<>(result);
    }

    /** 管理员:获取全部气泡 */
    public List<BubblePrompt> listAll() {
        return bubblePromptMapper.selectList(
                new LambdaQueryWrapper<BubblePrompt>().orderByAsc(BubblePrompt::getCategory));
    }

    /** 管理员:添加气泡 */
    public void add(BubblePrompt prompt) {
        prompt.setEnabled(1);
        bubblePromptMapper.insert(prompt);
    }

    /** 管理员:删除气泡 */
    public void delete(Long id) {
        bubblePromptMapper.deleteById(id);
    }
}
