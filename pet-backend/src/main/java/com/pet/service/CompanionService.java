package com.pet.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.entity.Pet;
import com.pet.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 陪伴/卖萌推送文案(M5 拍板:宠物语气,按性格出句;10% 卖萌替代)
 */
@Service
@RequiredArgsConstructor
public class CompanionService {

    private static final Map<String, List<String>> PERSONALITY_LINES = Map.of(
            "活泼", List.of("主人!我刚刚打滚了三圈,你看到没!", "今天也要元气满满哦!", "嘿!看我,快看我!"),
            "高冷", List.of("……看什么看,我只是一只猫。", "哼,本座今天心情尚可。", "你忙你的,不用管我。"),
            "粘人", List.of("你回来啦?我数着秒等你呢。", "能抱抱吗?就一下。", "别走嘛,再陪我一分钟。"),
            "贪吃", List.of("闻到饭香了,是给我留的吗?", "我好像听到开饭的声音了……", "饿饿,饭饭。"),
            "傲娇", List.of("才不是特意等你,只是刚好路过。", "哼,今天的你勉强合格。", "你以为我在意你?才没有。")
    );
    private static final List<String> GENERIC_LINES =
            List.of("想你了,来看看你。", "今天过得怎么样呀?", "我在桌角蹲了好久啦。");

    private static final List<String> NAUGHTY_LINES = List.of(
            "本次提醒被一只猫按掉了喵~",
            "日程?不存在的,今天只想晒太阳。",
            "主人,我帮你把日程拖到了明天!",
            "这条提醒被我吞了,嗝。"
    );

    private final PetMapper petMapper;

    /** 随机陪伴推送文案(按当前宠物性格) */
    public Map<String, String> companion(Long userId) {
        Pet pet = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .in(Pet::getStatus, PetConstants.PET_ALIVE, PetConstants.PET_DANGER)
                .last("LIMIT 1"));
        List<String> pool = GENERIC_LINES;
        if (pet != null && StrUtil.isNotBlank(pet.getPersonality())) {
            pool = PERSONALITY_LINES.getOrDefault(pet.getPersonality(), GENERIC_LINES);
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("text", RandomUtil.randomEle(pool));
        return data;
    }

    /** 10% 卖萌替代文案 */
    public Map<String, String> naughty() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("text", RandomUtil.randomEle(NAUGHTY_LINES));
        return data;
    }
}
