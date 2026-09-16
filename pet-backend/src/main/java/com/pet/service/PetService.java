package com.pet.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Pet;
import com.pet.entity.PetGravestone;
import com.pet.entity.PetInventory;
import com.pet.entity.PetSubtype;
import com.pet.entity.PetType;
import com.pet.entity.PlayMode;
import com.pet.entity.ShopItem;
import com.pet.mapper.PetGravestoneMapper;
import com.pet.mapper.PetInventoryMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PetSubtypeMapper;
import com.pet.mapper.PetTypeMapper;
import com.pet.mapper.PlayModeMapper;
import com.pet.mapper.ShopItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 宠物核心业务:抽蛋 / 状态推进(饥饿→病危→死亡) / 改名 / 种类展示
 */
@Service
@RequiredArgsConstructor
public class PetService {

    private final PetMapper petMapper;
    private final PetTypeMapper petTypeMapper;
    private final PetSubtypeMapper petSubtypeMapper;
    private final PetGravestoneMapper gravestoneMapper;
    private final ShopItemMapper shopItemMapper;
    private final PetInventoryMapper inventoryMapper;
    private final PlayModeMapper playModeMapper;

    /** 当前用户的活宠(ALIVE/DANGER),先推进状态再返回 */
    public Pet getMyPet(Long userId) {
        Pet pet = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .in(Pet::getStatus, PetConstants.PET_ALIVE, PetConstants.PET_DANGER)
                .last("LIMIT 1"));
        if (pet != null) {
            refreshState(pet);
        }
        return pet;
    }

    public boolean hasLivePet(Long userId) {
        return petMapper.selectCount(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .in(Pet::getStatus, PetConstants.PET_ALIVE, PetConstants.PET_DANGER)) > 0;
    }

    public boolean hasUnansweredGravestone(Long userId) {
        return gravestoneMapper.selectCount(new LambdaQueryWrapper<PetGravestone>()
                .eq(PetGravestone::getUserId, userId)
                .eq(PetGravestone::getAnswered, 0)) > 0;
    }

    /**
     * 状态推进(懒计算):饥饿按时间下降;降到 0 进病危;
     * 病危超过 24 小时未喂食 → 死亡并立墓碑(M3 拍板)
     */
    public void refreshState(Pet pet) {
        if (pet == null
                || (!PetConstants.PET_ALIVE.equals(pet.getStatus()) && !PetConstants.PET_DANGER.equals(pet.getStatus()))) {
            return;
        }
        PetType type = petTypeMapper.selectById(pet.getPetTypeId());
        if (type == null) {
            return;
        }
        // 每小时下降 = 100 / (基准天数×24×性格系数),保底饿死 ≥ 2 天
        double starveHours = type.getBaseDays() * 24.0 * personalityCoeff(pet.getPersonality());
        if (starveHours < PetConstants.MIN_STARVE_HOURS) {
            starveHours = PetConstants.MIN_STARVE_HOURS;
        }
        double ratePerHour = 100.0 / starveHours;

        LocalDateTime from = pet.getHungerCalcAt() != null ? pet.getHungerCalcAt() : pet.getHatchedAt();
        if (from == null) {
            from = LocalDateTime.now();
        }
        long elapsedMinutes = Duration.between(from, LocalDateTime.now()).toMinutes();
        if (elapsedMinutes <= 0) {
            return;
        }
        int drop = (int) Math.floor(ratePerHour * elapsedMinutes / 60.0);
        pet.setHunger(Math.max(0, pet.getHunger() - drop));
        pet.setHungerCalcAt(LocalDateTime.now());

        if (pet.getHunger() <= 0 && PetConstants.PET_ALIVE.equals(pet.getStatus())) {
            pet.setStatus(PetConstants.PET_DANGER);
            pet.setDangerSince(LocalDateTime.now());
        } else if (PetConstants.PET_DANGER.equals(pet.getStatus())) {
            LocalDateTime since = pet.getDangerSince() != null ? pet.getDangerSince() : LocalDateTime.now();
            if (Duration.between(since, LocalDateTime.now()).toHours() >= PetConstants.DANGER_GRACE_HOURS) {
                die(pet);
                return;
            }
        }
        petMapper.updateById(pet);
    }

    private void die(Pet pet) {
        pet.setStatus(PetConstants.PET_STARVED);
        petMapper.updateById(pet);
        PetType type = petTypeMapper.selectById(pet.getPetTypeId());
        PetGravestone g = new PetGravestone();
        g.setUserId(pet.getUserId());
        g.setPetId(pet.getId());
        g.setPetName(pet.getPetName());
        g.setPetTypeId(pet.getPetTypeId());
        g.setTypeName(type != null ? type.getTypeName() : null);
        g.setSubtypeName(pet.getSubtypeName());
        g.setGender(pet.getGender());
        g.setPersonality(pet.getPersonality());
        g.setBornAt(pet.getHatchedAt());
        g.setDiedAt(LocalDateTime.now());
        g.setLifespanDays(pet.getHatchedAt() != null
                ? Duration.between(pet.getHatchedAt(), LocalDateTime.now()).toDays() : 0);
        g.setAnswered(0);
        gravestoneMapper.insert(g);
    }

    private double personalityCoeff(String personality) {
        if (StrUtil.isBlank(personality)) {
            return 1.0;
        }
        return switch (personality) {
            case "高冷" -> PetConstants.COEFF_HIGH_COLD;
            case "粘人" -> PetConstants.COEFF_STICKY;
            case "贪吃" -> PetConstants.COEFF_GREEDY;
            default -> 1.0; // 活泼 / 傲娇 / 未知
        };
    }

    /** 抽蛋:一生一次;死过必须先反思答题;管理员可自由选种 */
    public Map<String, Object> draw(Long userId, String role, String petName, String typeCode) {
        if (StrUtil.isBlank(petName)) {
            throw new BusinessException("必须给宠物起名才能领养");
        }
        if (petName.trim().length() > 12) {
            throw new BusinessException("宠物名最多 12 个字");
        }
        if (hasLivePet(userId)) {
            throw new BusinessException("你已有一只宠物,不能再抽");
        }
        if (hasUnansweredGravestone(userId)) {
            throw new BusinessException("请先完成反思答题,才能迎接新的宠物");
        }

        PetType type;
        if ("ADMIN".equals(role) && StrUtil.isNotBlank(typeCode)) {
            type = petTypeMapper.selectOne(new LambdaQueryWrapper<PetType>()
                    .eq(PetType::getTypeCode, typeCode));
            if (type == null) {
                throw new BusinessException("种类不存在");
            }
        } else {
            type = randomTypeByRarity();
        }

        Pet pet = new Pet();
        pet.setUserId(userId);
        pet.setPetTypeId(type.getId());
        pet.setPetName(petName.trim());
        pet.setSubtypeName(randomSubtype(type.getId()));
        pet.setGender(RandomUtil.randomBoolean() ? "MALE" : "FEMALE");
        pet.setPersonality(RandomUtil.randomEle(PetConstants.PERSONALITIES));
        pet.setLevel(1);
        pet.setExp(0);
        pet.setHunger(100);
        pet.setMood(100);
        pet.setCoins(0);
        pet.setStatus(PetConstants.PET_ALIVE);
        pet.setHatchedAt(LocalDateTime.now());
        pet.setHungerCalcAt(LocalDateTime.now());
        petMapper.insert(pet);
        giveInitialFood(pet.getId()); // M7 拍板:新宠物送 10 条小鱼干

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("petId", pet.getId());
        data.put("typeName", type.getTypeName());
        data.put("subtypeName", pet.getSubtypeName());
        data.put("gender", pet.getGender());
        data.put("personality", pet.getPersonality());
        data.put("rarity", type.getRarity());
        return data;
    }

    /** 先按 70/25/5 抽稀有度,再在该档内随机选种(M3 拍板) */
    private PetType randomTypeByRarity() {
        int roll = RandomUtil.randomInt(1, 101);
        String rarity = roll <= 70 ? "NORMAL" : (roll <= 95 ? "RARE" : "LEGEND");
        List<PetType> pool = petTypeMapper.selectList(new LambdaQueryWrapper<PetType>()
                .eq(PetType::getRarity, rarity)
                .eq(PetType::getEnabled, 1));
        if (pool.isEmpty()) {
            pool = petTypeMapper.selectList(new LambdaQueryWrapper<PetType>().eq(PetType::getEnabled, 1));
        }
        return RandomUtil.randomEle(pool);
    }

    /** 新宠物初始食物:10 条小鱼干(M7 拍板) */
    public void giveInitialFood(Long petId) {
        ShopItem fish = shopItemMapper.selectOne(new LambdaQueryWrapper<ShopItem>()
                .eq(ShopItem::getItemCode, PetConstants.INITIAL_FOOD_CODE)
                .last("LIMIT 1"));
        if (fish == null) {
            return;
        }
        PetInventory inv = new PetInventory();
        inv.setPetId(petId);
        inv.setItemId(fish.getId());
        inv.setQuantity(PetConstants.INITIAL_FOOD_QUANTITY);
        inventoryMapper.insert(inv);
    }

    /** 相册用:按 id 查宠物 */
    public Pet findPetById(Long petId) {
        return petMapper.selectById(petId);
    }

    /** 相册用:宠物种类编码 */
    public String typeCodeOf(Pet pet) {
        if (pet == null) {
            return null;
        }
        PetType t = petTypeMapper.selectById(pet.getPetTypeId());
        return t != null ? t.getTypeCode() : null;
    }

    public String randomSubtype(Long typeId) {
        List<PetSubtype> subs = petSubtypeMapper.selectList(new LambdaQueryWrapper<PetSubtype>()
                .eq(PetSubtype::getPetTypeId, typeId)
                .eq(PetSubtype::getEnabled, 1));
        if (subs.isEmpty()) {
            return null;
        }
        return RandomUtil.randomEle(subs).getSubtypeName();
    }

    /** 我的宠物详情(状态已推进) */
    public Map<String, Object> my(Long userId) {
        Pet pet = getMyPet(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        if (pet == null) {
            data.put("hasPet", false);
            return data;
        }
        PetType type = petTypeMapper.selectById(pet.getPetTypeId());
        String behavior = applyDailyBehavior(pet);
        data.put("hasPet", true);
        data.put("petId", pet.getId());
        data.put("petName", pet.getPetName());
        data.put("typeCode", type != null ? type.getTypeCode() : null);
        data.put("typeName", type != null ? type.getTypeName() : null);
        data.put("subtypeName", pet.getSubtypeName());
        data.put("gender", pet.getGender());
        data.put("personality", pet.getPersonality());
        data.put("rarity", type != null ? type.getRarity() : null);
        data.put("level", pet.getLevel());
        data.put("exp", pet.getExp());
        data.put("hunger", pet.getHunger());
        data.put("mood", pet.getMood());
        data.put("coins", pet.getCoins());
        data.put("status", pet.getStatus());
        data.put("listed", pet.getListed());
        data.put("ageDays", ageDays(pet));
        data.put("stage", stageOf(pet));
        data.put("mature", isMature(pet));
        data.put("dangerRemainHours", PetConstants.PET_DANGER.equals(pet.getStatus())
                ? Math.max(0, PetConstants.DANGER_GRACE_HOURS - Duration.between(
                        pet.getDangerSince() != null ? pet.getDangerSince() : LocalDateTime.now(),
                        LocalDateTime.now()).toHours())
                : null);
        data.put("pregnantUntil", pet.getPregnantUntil());
        data.put("breedingCoolUntil", pet.getBreedingCoolUntil());
        data.put("behavior", behavior);
        return data;
    }

    /** 玩耍:M13 起模式存库(管理员可加),不限次数 */
    public Map<String, Object> play(Long userId, String mode) {
        Pet pet = getMyPet(userId);
        if (pet == null) {
            throw new BusinessException("你还没有宠物");
        }
        if (PetConstants.PET_DANGER.equals(pet.getStatus())) {
            throw new BusinessException("宠物病危中,先想办法救它吧");
        }
        PlayMode m = playModeMapper.selectOne(new LambdaQueryWrapper<PlayMode>()
                .eq(PlayMode::getName, mode)
                .eq(PlayMode::getEnabled, 1)
                .last("LIMIT 1"));
        if (m == null) {
            throw new BusinessException("未知的玩耍方式");
        }
        int mood = m.getMood() == null ? 0 : m.getMood();
        int hunger = m.getHunger() == null ? 0 : m.getHunger();
        int exp = m.getExp() == null ? 0 : m.getExp();
        pet.setMood(Math.max(0, Math.min(100, pet.getMood() + mood)));
        pet.setHunger(Math.max(0, Math.min(100, pet.getHunger() + hunger)));
        pet.setExp((pet.getExp() == null ? 0 : pet.getExp()) + exp);
        petMapper.updateById(pet);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", m.getName());
        data.put("icon", m.getIcon());
        data.put("mood", pet.getMood());
        data.put("hunger", pet.getHunger());
        data.put("message", pet.getPetName() + " " + m.getName() + "中!心情" + (mood >= 0 ? "+" : "") + mood
                + (hunger != 0 ? ",饥饿" + hunger : "") + (exp > 0 ? ",经验+" + exp : ""));
        return data;
    }

    /** 玩耍模式列表(来自数据库,管理员可维护) */
    public List<Map<String, Object>> playModes() {
        List<PlayMode> modes = playModeMapper.selectList(new LambdaQueryWrapper<PlayMode>()
                .eq(PlayMode::getEnabled, 1).orderByAsc(PlayMode::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlayMode m : modes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", m.getName());
            item.put("icon", m.getIcon());
            item.put("image", m.getImage());
            item.put("mood", m.getMood());
            item.put("hunger", m.getHunger());
            item.put("exp", m.getExp());
            result.add(item);
        }
        return result;
    }

    /** 年龄(现实天) */
    public long ageDays(Pet pet) {
        if (pet.getHatchedAt() == null) {
            return 0;
        }
        return Duration.between(pet.getHatchedAt(), LocalDateTime.now()).toDays();
    }

    /** 成长阶段:幼年/少年/成年/老年(M12 默认) */
    public String stageOf(Pet pet) {
        long days = ageDays(pet);
        if (days < PetConstants.STAGE_BABY_DAYS) {
            return "BABY";
        }
        if (days < PetConstants.STAGE_YOUNG_DAYS) {
            return "YOUNG";
        }
        if (days < PetConstants.STAGE_OLD_DAYS) {
            return "ADULT";
        }
        return "ELDER";
    }

    /** 是否成熟(可上架/可配种):出生 ≥ 3 天 */
    public boolean isMature(Pet pet) {
        return ageDays(pet) >= PetConstants.MATURE_DAYS;
    }

    /** 上架/下架配种市场:活着、成熟、非冷却才能上架;管理员可无视成熟要求(M13) */
    public Map<String, Object> setListed(Long userId, String role, boolean listed) {
        Pet pet = getMyPet(userId);
        if (pet == null) {
            throw new BusinessException("你还没有宠物");
        }
        boolean admin = "ADMIN".equals(role);
        if (listed) {
            if (!PetConstants.PET_ALIVE.equals(pet.getStatus())) {
                throw new BusinessException("宠物已死亡,不能上架");
            }
            if (!admin && !isMature(pet)) {
                throw new BusinessException("宠物还未成熟,出生满 " + PetConstants.MATURE_DAYS + " 天后才能上架(管理员可无视)");
            }
            if (pet.getBreedingCoolUntil() != null && pet.getBreedingCoolUntil().isAfter(LocalDateTime.now())) {
                throw new BusinessException("宠物还在配种冷却中,不能上架");
            }
        }
        pet.setListed(listed ? 1 : 0);
        petMapper.updateById(pet);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("listed", pet.getListed());
        data.put("message", listed ? "已上架,其他玩家可以在市场看到它" : "已下架");
        return data;
    }

    /** 管理员换宠(测试 UI 用):旧的标为已死,按指定种类/性别/性格新建 */
    public Map<String, Object> adminSwitch(Long adminId, String typeCode, String gender, String personality) {
        Pet old = getMyPet(adminId);
        if (old != null) {
            old.setStatus(PetConstants.PET_STARVED);
            petMapper.updateById(old);
        }
        PetType type = petTypeMapper.selectOne(new LambdaQueryWrapper<PetType>()
                .eq(PetType::getTypeCode, typeCode));
        if (type == null) {
            throw new BusinessException("种类不存在");
        }
        String g = "MALE".equals(gender) || "FEMALE".equals(gender) ? gender : "MALE";
        Pet pet = new Pet();
        pet.setUserId(adminId);
        pet.setPetTypeId(type.getId());
        pet.setPetName("测试-" + type.getTypeName());
        pet.setSubtypeName(randomSubtype(type.getId()));
        pet.setGender(g);
        pet.setPersonality(cn.hutool.core.util.StrUtil.isBlank(personality)
                ? cn.hutool.core.util.RandomUtil.randomEle(PetConstants.PERSONALITIES) : personality);
        pet.setLevel(1);
        pet.setExp(0);
        pet.setHunger(100);
        pet.setMood(100);
        pet.setCoins(99999);
        pet.setStatus(PetConstants.PET_ALIVE);
        pet.setHatchedAt(LocalDateTime.now());
        pet.setHungerCalcAt(LocalDateTime.now());
        pet.setListed(0);
        petMapper.insert(pet);
        giveInitialFood(pet.getId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("petId", pet.getId());
        data.put("typeName", type.getTypeName());
        data.put("subtypeName", pet.getSubtypeName());
        data.put("gender", pet.getGender());
        data.put("personality", pet.getPersonality());
        return data;
    }

    /** 每日行为事件(M3 拍板):30% 概率触发,暖心 +5 心情 / 搞破坏 -5 心情,每天最多 1 次 */
    private String applyDailyBehavior(Pet pet) {
        if (!PetConstants.PET_ALIVE.equals(pet.getStatus())) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (pet.getBehaviorDate() != null && pet.getBehaviorDate().equals(today)) {
            return null;
        }
        if (RandomUtil.randomInt(1, 101) > PetConstants.BEHAVIOR_DAILY_PERCENT) {
            return null;
        }
        boolean warm = RandomUtil.randomBoolean();
        int delta = warm ? PetConstants.BEHAVIOR_MOOD_CHANGE : -PetConstants.BEHAVIOR_MOOD_CHANGE;
        pet.setMood(Math.max(0, Math.min(100, pet.getMood() + delta)));
        pet.setBehaviorDate(today);
        petMapper.updateById(pet);
        return warm
                ? "暖心行为:它凑过来在你手心蹭了蹭(心情 +" + PetConstants.BEHAVIOR_MOOD_CHANGE + ")"
                : "搞破坏:它把你的文件推得东倒西歪(心情 -" + PetConstants.BEHAVIOR_MOOD_CHANGE + ")";
    }

    public void rename(Long userId, String newName) {
        // M12:名字只能起一次,不允许更改
        throw new BusinessException("宠物名字只能起一次,不能更改");
    }

    /** 13 种 + 亚种列表(展示用) */
    public List<Map<String, Object>> types() {
        List<PetType> types = petTypeMapper.selectList(new LambdaQueryWrapper<PetType>()
                .eq(PetType::getEnabled, 1).orderByAsc(PetType::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PetType t : types) {
            List<PetSubtype> subs = petSubtypeMapper.selectList(new LambdaQueryWrapper<PetSubtype>()
                    .eq(PetSubtype::getPetTypeId, t.getId()).eq(PetSubtype::getEnabled, 1));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("typeCode", t.getTypeCode());
            item.put("typeName", t.getTypeName());
            item.put("rarity", t.getRarity());
            item.put("baseDays", t.getBaseDays());
            item.put("subtypes", subs.stream().map(PetSubtype::getSubtypeName).toList());
            result.add(item);
        }
        return result;
    }
}
