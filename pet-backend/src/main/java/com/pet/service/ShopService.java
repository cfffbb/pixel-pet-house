package com.pet.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.entity.FeedingRecord;
import com.pet.entity.Pet;
import com.pet.entity.PetInventory;
import com.pet.entity.PetType;
import com.pet.entity.ShopItem;
import com.pet.mapper.FeedingRecordMapper;
import com.pet.mapper.PetInventoryMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PetTypeMapper;
import com.pet.mapper.ShopItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商店与喂食业务(M7 拍板:清单/价格/效果;喂食 +3 心情暂定,校准)
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemMapper shopItemMapper;
    private final PetInventoryMapper inventoryMapper;
    private final PetMapper petMapper;
    private final FeedingRecordMapper feedingRecordMapper;
    private final PetTypeMapper petTypeMapper;
    private final PetService petService;

    /** 商店物品(按当前宠物:仅本物种适用;等级不足的标为锁定) */
    public Map<String, Object> items(Long userId) {
        Pet pet = petService.getMyPet(userId);
        PetType petType = pet != null ? petTypeMapper.selectById(pet.getPetTypeId()) : null;
        List<ShopItem> all = shopItemMapper.selectList(new LambdaQueryWrapper<ShopItem>()
                .eq(ShopItem::getEnabled, 1).orderByAsc(ShopItem::getPrice));
        List<Map<String, Object>> available = new ArrayList<>();
        List<Map<String, Object>> locked = new ArrayList<>();
        for (ShopItem it : all) {
            // 物种限定:空=通用
            if (StrUtil.isNotBlank(it.getSpecies())
                    && (petType == null || !it.getSpecies().equals(petType.getTypeCode()))) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", it.getId());
            m.put("itemCode", it.getItemCode());
            m.put("itemName", it.getItemName());
            m.put("image", it.getImage());
            m.put("itemType", it.getItemType());
            m.put("foodCategory", it.getFoodCategory());
            m.put("price", it.getPrice());
            m.put("hungerRestore", it.getHungerRestore());
            m.put("moodRestore", it.getMoodRestore());
            m.put("minLevel", it.getMinLevel());
            m.put("locked", false);
            if (pet != null && it.getMinLevel() != null && pet.getLevel() < it.getMinLevel()) {
                m.put("locked", true);
                m.put("unlockLevel", it.getMinLevel());
                locked.add(m);
            } else {
                available.add(m);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", available);
        data.put("locked", locked);
        return data;
    }

    /** 我的宠物背包 */
    public List<Map<String, Object>> inventory(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        Pet pet = petService.getMyPet(userId);
        if (pet == null) {
            return result;
        }
        List<PetInventory> invs = inventoryMapper.selectList(new LambdaQueryWrapper<PetInventory>()
                .eq(PetInventory::getPetId, pet.getId()));
        for (PetInventory inv : invs) {
            ShopItem item = shopItemMapper.selectById(inv.getItemId());
            if (item == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", item.getId());
            m.put("itemName", item.getItemName());
            m.put("image", item.getImage());
            m.put("itemType", item.getItemType());
            m.put("foodCategory", item.getFoodCategory());
            m.put("hungerRestore", item.getHungerRestore());
            m.put("moodRestore", item.getMoodRestore());
            m.put("quantity", inv.getQuantity());
            result.add(m);
        }
        return result;
    }

    /** 购买(扣游戏币,进背包;管理员无限币;等级/物种校验) */
    public void buy(Long userId, String role, Long itemId, Integer quantity) {
        int qty = quantity == null ? 1 : quantity;
        if (qty <= 0 || qty > 99) {
            throw new BusinessException("数量不合法");
        }
        Pet pet = petService.getMyPet(userId);
        if (pet == null) {
            throw new BusinessException("你还没有宠物");
        }
        ShopItem item = shopItemMapper.selectById(itemId);
        if (item == null || item.getEnabled() == null || item.getEnabled() != 1) {
            throw new BusinessException("商品不存在");
        }
        if (item.getMinLevel() != null && pet.getLevel() < item.getMinLevel()) {
            throw new BusinessException("等级不足,需要 Lv" + item.getMinLevel() + " 解锁");
        }
        if (StrUtil.isNotBlank(item.getSpecies())) {
            PetType pt = petTypeMapper.selectById(pet.getPetTypeId());
            if (pt == null || !item.getSpecies().equals(pt.getTypeCode())) {
                throw new BusinessException("这不是你宠物适用的食物");
            }
        }
        int total = item.getPrice() * qty;
        if (!"ADMIN".equals(role)) {
            if (pet.getCoins() < total) {
                throw new BusinessException("游戏币不足");
            }
            pet.setCoins(pet.getCoins() - total);
            petMapper.updateById(pet);
        }
        // 管理员:无限币,不扣

        PetInventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<PetInventory>()
                .eq(PetInventory::getPetId, pet.getId())
                .eq(PetInventory::getItemId, itemId)
                .last("LIMIT 1"));
        if (inv == null) {
            inv = new PetInventory();
            inv.setPetId(pet.getId());
            inv.setItemId(itemId);
            inv.setQuantity(qty);
            inventoryMapper.insert(inv);
        } else {
            inv.setQuantity(inv.getQuantity() + qty);
            inventoryMapper.updateById(inv);
        }
    }

    /** 喂食:连续喂食奖励+幸运事件+食物多样性+经验获取+挑食机制 */
    public Map<String, Object> feed(Long userId, Long itemId) {
        Pet pet = petService.getMyPet(userId);
        if (pet == null) {
            throw new BusinessException("你还没有宠物");
        }
        if (pet.getLastFedAt() != null && pet.getLastFedAt().isAfter(LocalDateTime.now().minusSeconds(10))) {
            throw new BusinessException("宠物刚刚吃过,等几秒再喂吧");
        }
        ShopItem item = shopItemMapper.selectById(itemId);
        if (item == null || !"FOOD".equals(item.getItemType())) {
            throw new BusinessException("这不是食物");
        }
        PetInventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<PetInventory>()
                .eq(PetInventory::getPetId, pet.getId())
                .eq(PetInventory::getItemId, itemId)
                .last("LIMIT 1"));
        if (inv == null || inv.getQuantity() <= 0) {
            throw new BusinessException("背包里没有这个食物");
        }
        inv.setQuantity(inv.getQuantity() - 1);
        inventoryMapper.updateById(inv);

        String personality = pet.getPersonality() == null ? "活泼" : pet.getPersonality();
        String category = item.getFoodCategory() == null ? "STAPLE" : item.getFoodCategory();

        // === 1. 性格挑食 ===
        int moodBonus = PetConstants.FEED_MOOD_BONUS;
        String preference = "NORMAL";
        String reaction = "吃得挺香的";

        if ("活泼".equals(personality) && "SNACK".equals(category)) {
            moodBonus += 5; preference = "LIKE"; reaction = "超级开心!最爱零食!";
        } else if ("活泼".equals(personality) && "MEDICINE".equals(category)) {
            moodBonus -= 3; preference = "DISLIKE"; reaction = "苦着脸吃下去了...";
        } else if ("高冷".equals(personality) && "STAPLE".equals(category)) {
            moodBonus += 3; preference = "LIKE"; reaction = "优雅地享用主食";
        } else if ("高冷".equals(personality) && "SNACK".equals(category)) {
            moodBonus -= 2; preference = "DISLIKE"; reaction = "嫌弃地看了一眼零食...";
        } else if ("贪吃".equals(personality)) {
            moodBonus += 2; preference = "LIKE"; reaction = "狼吞虎咽地吃完了!";
        } else if ("粘人".equals(personality) && "SNACK".equals(category)) {
            moodBonus += 3; preference = "LIKE"; reaction = "蹭着你开心地吃掉了~";
        } else if ("粘人".equals(personality) && "MEDICINE".equals(category)) {
            moodBonus -= 1; preference = "DISLIKE"; reaction = "委屈巴巴地吃下去了...";
        } else if ("傲娇".equals(personality) && "SPECIAL".equals(category)) {
            moodBonus += 4; preference = "LIKE"; reaction = "哼,勉为其难地...真香!";
        } else if ("傲娇".equals(personality) && "STAPLE".equals(category)) {
            moodBonus -= 1; preference = "DISLIKE"; reaction = "就这?不太满意...";
        } else if ("胆小".equals(personality) && "SPECIAL".equals(category)) {
            moodBonus += 4; preference = "LIKE"; reaction = "好奇地尝了尝,很喜欢!";
        }

        // === 2. 连续喂食天数 ===
        String today = LocalDate.now().toString();
        int streak = pet.getFeedingStreak() == null ? 0 : pet.getFeedingStreak();
        String lastDate = pet.getLastFeedDate();
        boolean streakChanged = false;

        if (lastDate == null || !lastDate.equals(today)) {
            if (lastDate != null) {
                LocalDate last = LocalDate.parse(lastDate);
                LocalDate yesterday = LocalDate.now().minusDays(1);
                if (last.equals(yesterday)) {
                    streak++;
                } else {
                    streak = 1;
                }
            } else {
                streak = 1;
            }
            streakChanged = true;
        }
        int streakBonus = Math.min(streak, PetConstants.FEED_STREAK_MOOD_CAP);
        if (streak >= 3) {
            reaction += " (连续" + streak + "天!)";
        }

        // === 3. 食物多样性 ===
        List<FeedingRecord> recent = feedingRecordMapper.selectList(
                new LambdaQueryWrapper<FeedingRecord>()
                        .eq(FeedingRecord::getPetId, pet.getId())
                        .orderByDesc(FeedingRecord::getFedAt)
                        .last("LIMIT 5"));
        int varietyBonus = 0;
        String varietyNote = "";
        long distinctItems = recent.stream().map(FeedingRecord::getItemId).distinct().count();
        if (distinctItems >= 3) {
            varietyBonus = PetConstants.FEED_VARIETY_BONUS;
            varietyNote = "饮食多样,营养均衡!";
        } else if (distinctItems == 1 && recent.size() >= 3) {
            varietyBonus = -PetConstants.FEED_BOREDOM_PENALTY;
            varietyNote = "总吃一样的,有点腻了...";
        }

        // === 4. 幸运事件(15%) ===
        String luckyEvent = null;
        int luckyMood = 0;
        int luckyCoins = 0;
        int luckyExp = 0;
        boolean luckyDouble = false;
        String luckyDesc = "";

        int luckyRoll = ThreadLocalRandom.current().nextInt(100);
        if (luckyRoll < PetConstants.FEED_LUCKY_PERCENT) {
            int eventType = ThreadLocalRandom.current().nextInt(5);
            switch (eventType) {
                case 0:
                    luckyEvent = "COIN_BURST";
                    luckyCoins = 50;
                    luckyDesc = "金币大爆发! +50";
                    reaction += " 食物里藏着金币!";
                    break;
                case 1:
                    luckyEvent = "MOOD_BURST";
                    luckyMood = 15;
                    luckyDesc = "心情大爆发! +15";
                    reaction += " 心情大爆发!";
                    break;
                case 2:
                    luckyEvent = "EXP_RAIN";
                    luckyExp = 20;
                    luckyDesc = "经验雨! +20EXP";
                    reaction += " 获得了额外经验!";
                    break;
                case 3:
                    luckyEvent = "DOUBLE_HUNGER";
                    luckyDouble = true;
                    luckyDesc = "饱食度翻倍!";
                    reaction += " 大口吃完,饱食度翻倍!";
                    break;
                case 4:
                    luckyEvent = "GIFT";
                    ShopItem gift = shopItemMapper.selectOne(new LambdaQueryWrapper<ShopItem>()
                            .eq(ShopItem::getFoodCategory, "SNACK")
                            .eq(ShopItem::getEnabled, 1)
                            .last("LIMIT 1"));
                    if (gift != null) {
                        PetInventory giftInv = inventoryMapper.selectOne(new LambdaQueryWrapper<PetInventory>()
                                .eq(PetInventory::getPetId, pet.getId())
                                .eq(PetInventory::getItemId, gift.getId())
                                .last("LIMIT 1"));
                        if (giftInv == null) {
                            giftInv = new PetInventory();
                            giftInv.setPetId(pet.getId());
                            giftInv.setItemId(gift.getId());
                            giftInv.setQuantity(1);
                            inventoryMapper.insert(giftInv);
                        } else {
                            giftInv.setQuantity(giftInv.getQuantity() + 1);
                            inventoryMapper.updateById(giftInv);
                        }
                        luckyDesc = "神秘礼物! 获得" + gift.getItemName();
                    } else {
                        luckyCoins = 30;
                        luckyDesc = "幸运金币! +30";
                    }
                    reaction += " 食物里蹦出了神秘礼物!";
                    break;
            }
        }

        // === 5. 经验获取 ===
        int expGain = 0;
        switch (category) {
            case "STAPLE": expGain = PetConstants.FEED_EXP_STAPLE; break;
            case "SNACK": expGain = PetConstants.FEED_EXP_SNACK; break;
            case "MEDICINE": expGain = PetConstants.FEED_EXP_MEDICINE; break;
            case "SPECIAL": expGain = PetConstants.FEED_EXP_SPECIAL; break;
        }
        expGain += luckyExp;

        // === 6. 计算最终数值 ===
        int hungerBefore = pet.getHunger();
        int moodBefore = pet.getMood();

        int hungerGain = luckyDouble ? item.getHungerRestore() * 2 : item.getHungerRestore();
        pet.setHunger(Math.min(100, pet.getHunger() + hungerGain));

        int totalMoodGain = item.getMoodRestore() + moodBonus + streakBonus + varietyBonus + luckyMood;
        pet.setMood(Math.min(100, Math.max(0, pet.getMood() + totalMoodGain)));

        // 金币(连击里程碑+幸运)
        int coinGain = luckyCoins;
        String streakReward = "";
        if (streakChanged) {
            if (streak == 3) {
                coinGain += PetConstants.FEED_STREAK_3_COINS;
                streakReward = "连续喂食3天! +" + PetConstants.FEED_STREAK_3_COINS + "金币";
            } else if (streak == 7) {
                coinGain += PetConstants.FEED_STREAK_7_COINS;
                streakReward = "连续喂食7天! +" + PetConstants.FEED_STREAK_7_COINS + "金币";
            } else if (streak == 15) {
                coinGain += PetConstants.FEED_STREAK_15_COINS;
                streakReward = "连续喂食15天! +" + PetConstants.FEED_STREAK_15_COINS + "金币";
            } else if (streak == 30) {
                coinGain += PetConstants.FEED_STREAK_30_COINS;
                streakReward = "连续喂食30天! +" + PetConstants.FEED_STREAK_30_COINS + "金币";
            }
        }
        if (coinGain > 0) {
            pet.setCoins(pet.getCoins() + coinGain);
        }

        // 经验
        pet.setExp(pet.getExp() + expGain);

        // 病危救回
        if (PetConstants.PET_DANGER.equals(pet.getStatus()) && pet.getHunger() > 0) {
            pet.setStatus(PetConstants.PET_ALIVE);
            pet.setDangerSince(null);
        }

        pet.setFeedingStreak(streak);
        pet.setLastFeedDate(today);
        pet.setLastFedAt(LocalDateTime.now());
        petMapper.updateById(pet);

        // 记录流水
        FeedingRecord fr = new FeedingRecord();
        fr.setPetId(pet.getId());
        fr.setItemId(itemId);
        fr.setItemName(item.getItemName());
        fr.setHungerBefore(hungerBefore);
        fr.setHungerAfter(pet.getHunger());
        fr.setMoodBefore(moodBefore);
        fr.setMoodAfter(pet.getMood());
        fr.setPreference(preference);
        fr.setLuckyEvent(luckyEvent);
        fr.setExpGain(expGain);
        fr.setFedAt(LocalDateTime.now());
        feedingRecordMapper.insert(fr);

        String hungerDesc;
        if (pet.getHunger() >= 80) hungerDesc = "饱饱的";
        else if (pet.getHunger() >= 50) hungerDesc = "还不太饿";
        else if (pet.getHunger() >= 20) hungerDesc = "有点饿了";
        else hungerDesc = "快饿坏了!";

        String moodDesc;
        if (pet.getMood() >= 80) moodDesc = "超级开心";
        else if (pet.getMood() >= 50) moodDesc = "心情不错";
        else if (pet.getMood() >= 20) moodDesc = "有点低落";
        else moodDesc = "心情很差";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hunger", pet.getHunger());
        data.put("mood", pet.getMood());
        data.put("hungerDesc", hungerDesc);
        data.put("moodDesc", moodDesc);
        data.put("preference", preference);
        data.put("reaction", reaction);
        data.put("hungerDelta", hungerGain);
        data.put("moodDelta", totalMoodGain);
        data.put("streak", streak);
        data.put("streakBonus", streakBonus);
        data.put("streakReward", streakReward);
        data.put("varietyBonus", varietyBonus);
        data.put("varietyNote", varietyNote);
        data.put("luckyEvent", luckyEvent);
        data.put("luckyDesc", luckyDesc);
        data.put("expGain", expGain);
        data.put("coinGain", coinGain);
        data.put("petExpression", getExpression(pet, preference, luckyEvent));
        data.put("message", reaction);
        return data;
    }

    /** 喂食统计 */
    public Map<String, Object> feedingStats(Long userId) {
        Pet pet = petService.getMyPet(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        if (pet == null) return data;

        data.put("streak", pet.getFeedingStreak() == null ? 0 : pet.getFeedingStreak());

        long total = feedingRecordMapper.selectCount(
                new LambdaQueryWrapper<FeedingRecord>().eq(FeedingRecord::getPetId, pet.getId()));
        data.put("totalFeeds", total);

        long luckyCount = feedingRecordMapper.selectCount(
                new LambdaQueryWrapper<FeedingRecord>()
                        .eq(FeedingRecord::getPetId, pet.getId())
                        .isNotNull(FeedingRecord::getLuckyEvent));
        data.put("luckyCount", luckyCount);

        List<FeedingRecord> recent = feedingRecordMapper.selectList(
                new LambdaQueryWrapper<FeedingRecord>()
                        .eq(FeedingRecord::getPetId, pet.getId())
                        .orderByDesc(FeedingRecord::getFedAt)
                        .last("LIMIT 10"));
        List<Map<String, Object>> meals = new ArrayList<>();
        for (FeedingRecord r : recent) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemName", r.getItemName());
            m.put("preference", r.getPreference());
            m.put("luckyEvent", r.getLuckyEvent());
            m.put("expGain", r.getExpGain());
            m.put("fedAt", r.getFedAt() != null ? r.getFedAt().toString().replace("T", " ").substring(0, 16) : "");
            m.put("hungerBefore", r.getHungerBefore());
            m.put("hungerAfter", r.getHungerAfter());
            meals.add(m);
        }
        data.put("recentMeals", meals);

        // 下一个连击里程碑
        int streak = pet.getFeedingStreak() == null ? 0 : pet.getFeedingStreak();
        int nextMilestone = 3;
        if (streak >= 30) nextMilestone = 0;
        else if (streak >= 15) nextMilestone = 30;
        else if (streak >= 7) nextMilestone = 15;
        else if (streak >= 3) nextMilestone = 7;
        data.put("nextMilestone", nextMilestone);

        return data;
    }

    /** 根据宠物状态计算表情 */
    private String getExpression(Pet pet, String preference, String luckyEvent) {
        if (luckyEvent != null) return "🤩";
        if ("LIKE".equals(preference)) return "😍";
        if ("DISLIKE".equals(preference)) return "😒";
        int mood = pet.getMood() == null ? 50 : pet.getMood();
        if (mood >= 80) return "😊";
        if (mood >= 50) return "🙂";
        if (mood >= 20) return "😟";
        return "😢";
    }
}
