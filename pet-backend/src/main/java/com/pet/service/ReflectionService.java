package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.exception.BusinessException;
import com.pet.dto.ReflectionDTO;
import com.pet.entity.PetGravestone;
import com.pet.mapper.PetGravestoneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 反思答题(M3 拍板:2 道开放式问题,每题 ≥10 字,答完才可重新抽蛋/领幼崽)
 */
@Service
@RequiredArgsConstructor
public class ReflectionService {

    public static final String QUESTION_1 = "回忆一下:它饿倒前的最后 3 天,你的日程里排了什么?真正完成了多少?";
    public static final String QUESTION_2 = "如果时间倒回它出生的那天,你会把哪件事写进日程的第一行?";

    private final PetGravestoneMapper gravestoneMapper;

    public Map<String, String> questions() {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("question1", QUESTION_1);
        q.put("question2", QUESTION_2);
        return q;
    }

    /** 提交反思:填最近一座未答的墓碑 */
    public void submit(Long userId, ReflectionDTO dto) {
        PetGravestone g = latestUnanswered(userId);
        if (g == null) {
            throw new BusinessException("没有需要反思的宠物");
        }
        if (dto.getReflection1() == null || dto.getReflection1().trim().length() < 10
                || dto.getReflection2() == null || dto.getReflection2().trim().length() < 10) {
            throw new BusinessException("每题至少写 10 个字");
        }
        g.setReflection1(dto.getReflection1().trim());
        g.setReflection2(dto.getReflection2().trim());
        g.setAnswered(1);
        gravestoneMapper.updateById(g);
    }

    public PetGravestone latestUnanswered(Long userId) {
        return gravestoneMapper.selectOne(new LambdaQueryWrapper<PetGravestone>()
                .eq(PetGravestone::getUserId, userId)
                .eq(PetGravestone::getAnswered, 0)
                .orderByDesc(PetGravestone::getDiedAt)
                .last("LIMIT 1"));
    }

    /** 我的墓碑列表(新→旧) */
    public List<PetGravestone> listMine(Long userId) {
        return gravestoneMapper.selectList(new LambdaQueryWrapper<PetGravestone>()
                .eq(PetGravestone::getUserId, userId)
                .orderByDesc(PetGravestone::getDiedAt));
    }
}
