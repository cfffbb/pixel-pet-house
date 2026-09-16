package com.pet.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.dto.BreedingRequestDTO;
import com.pet.entity.BreedingRecord;
import com.pet.entity.InboxMessage;
import com.pet.entity.Pet;
import com.pet.entity.PetHatchling;
import com.pet.entity.PetType;
import com.pet.entity.User;
import com.pet.entity.UserSetting;
import com.pet.mapper.BreedingRecordMapper;
import com.pet.mapper.InboxMessageMapper;
import com.pet.mapper.PetHatchlingMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PetTypeMapper;
import com.pet.mapper.UserMapper;
import com.pet.mapper.UserSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配种市场与托管业务(M3.5):申请→收件箱→双方同意→怀孕 3 天→产 3–5 崽
 */
@Service
@RequiredArgsConstructor
public class BreedingService {

    private final PetMapper petMapper;
    private final PetTypeMapper petTypeMapper;
    private final BreedingRecordMapper breedingRecordMapper;
    private final PetHatchlingMapper hatchlingMapper;
    private final InboxMessageMapper inboxMessageMapper;
    private final UserSettingMapper userSettingMapper;
    private final UserMapper userMapper;
    private final PetService petService;

    /**
     * 市场:只看"已上架"的宠物(M12),支持种类筛选/排序/分页
     */
    public Map<String, Object> market(Long myUserId, String species, String sort, long page, long size) {
        List<Pet> candidates = petMapper.selectList(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getListed, 1)
                .eq(Pet::getStatus, PetConstants.PET_ALIVE));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Pet p : candidates) {
            if (p.getBreedingCoolUntil() != null && p.getBreedingCoolUntil().isAfter(LocalDateTime.now())) {
                continue;
            }
            PetType type = petTypeMapper.selectById(p.getPetTypeId());
            if (species != null && !species.isBlank() && (type == null || !species.equals(type.getTypeCode()))) {
                continue;
            }
            User owner = userMapper.selectById(p.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("petId", p.getId());
            m.put("petName", p.getPetName());
            m.put("ownerNickname", owner != null ? owner.getNickname() : null);
            m.put("ownerUsername", owner != null ? owner.getUsername() : null);
            m.put("ownerUserId", owner != null ? owner.getId() : null);
            m.put("typeCode", type != null ? type.getTypeCode() : null);
            m.put("typeName", type != null ? type.getTypeName() : null);
            m.put("subtypeName", p.getSubtypeName());
            m.put("rarity", type != null ? type.getRarity() : null);
            m.put("gender", p.getGender());
            m.put("personality", p.getPersonality());
            m.put("level", p.getLevel());
            m.put("hunger", p.getHunger());
            m.put("mood", p.getMood());
            m.put("coins", p.getCoins());
            m.put("ageDays", p.getHatchedAt() == null ? 0
                    : java.time.Duration.between(p.getHatchedAt(), LocalDateTime.now()).toDays());
            m.put("createdAt", p.getCreatedAt());
            m.put("listed", 1);
            rows.add(m);
        }
        // 排序:newest 最新上架 / level 等级 / rarity 稀有度
        if ("level".equals(sort)) {
            rows.sort((a, b) -> Integer.compare((Integer) b.get("level"), (Integer) a.get("level")));
        } else if ("rarity".equals(sort)) {
            rows.sort((a, b) -> Integer.compare(rarityRank((String) b.get("rarity")), rarityRank((String) a.get("rarity"))));
        } else {
            rows.sort((a, b) -> Long.compare((Long) b.get("petId"), (Long) a.get("petId")));
        }
        long total = rows.size();
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 10 : size;
        long from = (p - 1) * s;
        long to = Math.min(from + s, total);
        List<Map<String, Object>> pageRows = from >= total ? new ArrayList<>() : rows.subList((int) from, (int) to);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", pageRows);
        data.put("total", total);
        data.put("page", p);
        data.put("size", s);
        return data;
    }

    private int rarityRank(String r) {
        return switch (r == null ? "" : r) {
            case "LEGEND" -> 3;
            case "RARE" -> 2;
            default -> 1;
        };
    }

    /** 配种授权(M3.5 拍板:默认开;用户可在设置里关闭) */
    public boolean allowBreeding(Long ownerId) {
        UserSetting s = userSettingMapper.selectOne(new LambdaQueryWrapper<UserSetting>()
                .eq(UserSetting::getUserId, ownerId)
                .eq(UserSetting::getSettingKey, PetConstants.SETTING_ALLOW_BREEDING)
                .last("LIMIT 1"));
        return s == null || !"false".equalsIgnoreCase(s.getSettingValue());
    }

    /** 是否管理员用户 */
    private boolean isAdminUser(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null && "ADMIN".equals(u.getRole());
    }

    /** 发起配种申请(双方需成熟 ≥3 天;管理员免费用;雌雄均可发起,对方须为异性) */
    public void request(Long userId, String role, BreedingRequestDTO dto) {
        Pet myPet = petService.getMyPet(userId);
        if (myPet == null) {
            throw new BusinessException("你还没有宠物");
        }
        if (!petService.isMature(myPet)) {
            throw new BusinessException("你的宠物还未成熟,出生满 " + PetConstants.MATURE_DAYS + " 天后才能配种");
        }
        Pet target = petMapper.selectById(dto.getFatherPetId());
        if (target == null) {
            throw new BusinessException("对方宠物不存在");
        }
        if (target.getUserId().equals(userId)) {
            throw new BusinessException("不能用自己的宠物配种");
        }
        if (target.getListed() == null || target.getListed() != 1) {
            throw new BusinessException("对方宠物未上架,不能申请配种");
        }
        if (!PetConstants.PET_ALIVE.equals(target.getStatus())) {
            throw new BusinessException("对方宠物不在可配种状态");
        }
        if (!petService.isMature(target)) {
            throw new BusinessException("对方宠物还未成熟");
        }
        // 确定母方和父方:雌为母,雄为父
        Pet mother, father;
        if ("FEMALE".equals(myPet.getGender()) && "MALE".equals(target.getGender())) {
            mother = myPet;
            father = target;
        } else if ("MALE".equals(myPet.getGender()) && "FEMALE".equals(target.getGender())) {
            mother = target;
            father = myPet;
        } else {
            throw new BusinessException("配种双方必须是一公一母");
        }
        if (mother.getPregnantUntil() != null && mother.getPregnantUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("母方正在怀孕,不能配种");
        }
        if (mother.getBreedingCoolUntil() != null && mother.getBreedingCoolUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("母方还在配种冷却中");
        }
        if (father.getBreedingCoolUntil() != null && father.getBreedingCoolUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("父方还在配种冷却中");
        }
        // 双方各扣 10 币(管理员免)
        boolean admin = "ADMIN".equals(role);
        if (!admin && myPet.getCoins() < PetConstants.BREEDING_FEE) {
            throw new BusinessException("你的游戏币不足(配种需 10 币)");
        }
        boolean targetAdmin = isAdminUser(target.getUserId());
        if (!targetAdmin && target.getCoins() < PetConstants.BREEDING_FEE) {
            throw new BusinessException("对方游戏币不足,无法配种");
        }
        if (!admin) {
            myPet.setCoins(myPet.getCoins() - PetConstants.BREEDING_FEE);
        }
        if (!targetAdmin) {
            target.setCoins(target.getCoins() - PetConstants.BREEDING_FEE);
        }
        petMapper.updateById(myPet);
        petMapper.updateById(target);
        petMapper.updateById(mother);
        petMapper.updateById(father);

        BreedingRecord rec = new BreedingRecord();
        rec.setMotherPetId(mother.getId());
        rec.setFatherPetId(father.getId());
        rec.setRequestedBy(userId);
        rec.setStatus(PetConstants.BREED_PENDING);
        breedingRecordMapper.insert(rec);

        InboxMessage msg = new InboxMessage();
        msg.setSenderId(userId);
        msg.setReceiverId(target.getUserId());
        msg.setType(PetConstants.MSG_BREED_REQUEST);
        msg.setRefId(rec.getId());
        msg.setContent("想用我的宠物与你的「" + target.getPetName() + "」配种,请处理");
        msg.setStatus(PetConstants.MSG_UNREAD);
        msg.setRequestStatus(PetConstants.REQ_PENDING);
        inboxMessageMapper.insert(msg);
    }

    /** 父方主人同意 → 怀孕 3 天,双方冷却 3 天(M3.5 拍板) */
    public void accept(Long recordId, Long ownerUserId) {
        BreedingRecord rec = getRecord(recordId);
        Pet father = petMapper.selectById(rec.getFatherPetId());
        if (father == null || !father.getUserId().equals(ownerUserId)) {
            throw new BusinessException("无权处理该申请");
        }
        if (!PetConstants.BREED_PENDING.equals(rec.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        Pet mother = petMapper.selectById(rec.getMotherPetId());
        if (mother == null || !PetConstants.PET_ALIVE.equals(mother.getStatus())) {
            throw new BusinessException("母方宠物已不在,配种取消");
        }
        rec.setStatus(PetConstants.BREED_PREGNANT);
        breedingRecordMapper.updateById(rec);

        mother.setPregnantUntil(LocalDateTime.now().plusDays(PetConstants.PREGNANT_DAYS));
        mother.setBreedingCoolUntil(LocalDateTime.now().plusDays(PetConstants.BREEDING_COOL_DAYS));
        petMapper.updateById(mother);
        father.setBreedingCoolUntil(LocalDateTime.now().plusDays(PetConstants.BREEDING_COOL_DAYS));
        petMapper.updateById(father);
    }

    /** 父方主人拒绝 → 双方退 10 币 */
    public void reject(Long recordId, Long ownerUserId) {
        BreedingRecord rec = getRecord(recordId);
        Pet father = petMapper.selectById(rec.getFatherPetId());
        if (father == null || !father.getUserId().equals(ownerUserId)) {
            throw new BusinessException("无权处理该申请");
        }
        if (!PetConstants.BREED_PENDING.equals(rec.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        rec.setStatus(PetConstants.BREED_REJECTED);
        breedingRecordMapper.updateById(rec);

        Pet mother = petMapper.selectById(rec.getMotherPetId());
        if (mother != null) {
            mother.setCoins(mother.getCoins() + PetConstants.BREEDING_FEE);
            petMapper.updateById(mother);
        }
        father.setCoins(father.getCoins() + PetConstants.BREEDING_FEE);
        petMapper.updateById(father);
    }

    /** 到期产崽:3–5 只,每只 10% 夭折,种类 50/50 取父母,品质受父母影响 */
    public void maybeGiveBirth(Pet mother) {
        if (mother.getPregnantUntil() == null || LocalDateTime.now().isBefore(mother.getPregnantUntil())) {
            return;
        }
        if (!PetConstants.PET_ALIVE.equals(mother.getStatus()) && !PetConstants.PET_DANGER.equals(mother.getStatus())) {
            return;
        }
        BreedingRecord rec = breedingRecordMapper.selectOne(new LambdaQueryWrapper<BreedingRecord>()
                .eq(BreedingRecord::getMotherPetId, mother.getId())
                .eq(BreedingRecord::getStatus, PetConstants.BREED_PREGNANT)
                .last("LIMIT 1"));
        if (rec == null) {
            mother.setPregnantUntil(null);
            petMapper.updateById(mother);
            return;
        }
        Pet father = petMapper.selectById(rec.getFatherPetId());
        PetType motherType = petTypeMapper.selectById(mother.getPetTypeId());
        PetType fatherType = father != null ? petTypeMapper.selectById(father.getPetTypeId()) : null;

        int litter = RandomUtil.randomInt(PetConstants.LITTER_MIN, PetConstants.LITTER_MAX + 1);
        for (int i = 0; i < litter; i++) {
            if (RandomUtil.randomInt(1, 101) <= PetConstants.HATCHLING_MORTALITY_PERCENT) {
                continue; // 10% 夭折
            }
            PetType childType = RandomUtil.randomBoolean() ? motherType : fatherType;
            if (childType == null) {
                childType = motherType != null ? motherType : fatherType;
            }
            if (childType == null) {
                continue;
            }
            PetHatchling h = new PetHatchling();
            h.setUserId(mother.getUserId());
            h.setMotherPetId(mother.getId());
            h.setFatherPetId(father != null ? father.getId() : null);
            h.setPetTypeId(childType.getId());
            h.setSubtypeName(petService.randomSubtype(childType.getId()));
            h.setGender(RandomUtil.randomBoolean() ? "MALE" : "FEMALE");
            h.setPersonality(RandomUtil.randomEle(PetConstants.PERSONALITIES));
            h.setRarity(randomChildRarity(motherType != null ? motherType.getRarity() : null,
                    fatherType != null ? fatherType.getRarity() : null));
            h.setStatus(PetConstants.HATCHLING_STORED);
            h.setStoredAt(LocalDateTime.now());
            hatchlingMapper.insert(h);
        }
        rec.setStatus(PetConstants.BREED_BORN);
        breedingRecordMapper.updateById(rec);
        mother.setPregnantUntil(null);
        petMapper.updateById(mother);
    }

    /**
     * 品质继承(M3.5 拍板,公示规则):
     * 父母含传说 → 传说 30% / 稀有 40% / 普通 30%
     * 父母含稀有 → 传说 5% / 稀有 40% / 普通 55%
     * 双方普通   → 传说 2% / 稀有 18% / 普通 80%
     */
    private String randomChildRarity(String motherRarity, String fatherRarity) {
        boolean hasLegend = "LEGEND".equals(motherRarity) || "LEGEND".equals(fatherRarity);
        boolean hasRare = "RARE".equals(motherRarity) || "RARE".equals(fatherRarity);
        int roll = RandomUtil.randomInt(1, 101);
        if (hasLegend) {
            if (roll <= 30) {
                return "LEGEND";
            }
            if (roll <= 70) {
                return "RARE";
            }
            return "NORMAL";
        }
        if (hasRare) {
            if (roll <= 5) {
                return "LEGEND";
            }
            if (roll <= 45) {
                return "RARE";
            }
            return "NORMAL";
        }
        if (roll <= 2) {
            return "LEGEND";
        }
        if (roll <= 20) {
            return "RARE";
        }
        return "NORMAL";
    }

    private BreedingRecord getRecord(Long recordId) {
        BreedingRecord rec = breedingRecordMapper.selectById(recordId);
        if (rec == null) {
            throw new BusinessException("配种记录不存在");
        }
        return rec;
    }

    /** 我的配种记录(我发起或我方为父方),顺带推进产崽 */
    public List<Map<String, Object>> myRecords(Long userId) {
        List<Pet> myPets = petMapper.selectList(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .in(Pet::getStatus, PetConstants.PET_ALIVE, PetConstants.PET_DANGER));
        for (Pet p : myPets) {
            if (p.getPregnantUntil() != null) {
                maybeGiveBirth(p);
            }
        }
        List<Long> myPetIds = myPets.stream().map(Pet::getId).toList();
        LambdaQueryWrapper<BreedingRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(BreedingRecord::getRequestedBy, userId);
        if (!myPetIds.isEmpty()) {
            qw.or().in(BreedingRecord::getFatherPetId, myPetIds);
        }
        qw.orderByDesc(BreedingRecord::getCreatedAt);
        List<BreedingRecord> records = breedingRecordMapper.selectList(qw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (BreedingRecord r : records) {
            Pet mother = petMapper.selectById(r.getMotherPetId());
            Pet father = petMapper.selectById(r.getFatherPetId());
            User requester = userMapper.selectById(r.getRequestedBy());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordId", r.getId());
            m.put("status", r.getStatus());
            m.put("motherPetName", mother != null ? mother.getPetName() : null);
            m.put("fatherPetName", father != null ? father.getPetName() : null);
            m.put("requestedByNickname", requester != null ? requester.getNickname() : null);
            m.put("createdAt", r.getCreatedAt());
            // 怀孕进度
            if (PetConstants.BREED_PREGNANT.equals(r.getStatus()) && mother != null && mother.getPregnantUntil() != null) {
                m.put("pregnancyProgress", calcPregnancyProgress(mother));
            }
            // 产崽数量
            if (PetConstants.BREED_BORN.equals(r.getStatus())) {
                long cubCount = hatchlingMapper.selectCount(new LambdaQueryWrapper<PetHatchling>()
                        .eq(PetHatchling::getMotherPetId, r.getMotherPetId())
                        .eq(PetHatchling::getFatherPetId, r.getFatherPetId()));
                m.put("cubCount", cubCount);
            }
            result.add(m);
        }
        return result;
    }

    /** 繁育详情:当前宠物怀孕状态 + 幼崽列表 + 统计 */
    public Map<String, Object> breedingDetail(Long userId) {
        Pet pet = petService.getMyPet(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        if (pet == null) return data;

        // 宠物基本信息
        data.put("petName", pet.getPetName());
        data.put("gender", pet.getGender());
        data.put("personality", pet.getPersonality());
        data.put("level", pet.getLevel());
        PetType pt = petTypeMapper.selectById(pet.getPetTypeId());
        data.put("typeName", pt != null ? pt.getTypeName() : null);
        data.put("typeCode", pt != null ? pt.getTypeCode() : null);
        data.put("rarity", pt != null ? pt.getRarity() : null);

        // 成长阶段
        data.put("growthStage", petService.stageOf(pet));
        data.put("isMature", petService.isMature(pet));

        // 怀孕进度
        if (pet.getPregnantUntil() != null && pet.getPregnantUntil().isAfter(LocalDateTime.now())) {
            data.put("pregnancy", calcPregnancyProgress(pet));
        } else {
            data.put("pregnancy", null);
        }

        // 配种冷却
        if (pet.getBreedingCoolUntil() != null && pet.getBreedingCoolUntil().isAfter(LocalDateTime.now())) {
            long coolRemaining = java.time.Duration.between(LocalDateTime.now(), pet.getBreedingCoolUntil()).getSeconds();
            data.put("coolRemainingSeconds", coolRemaining);
            data.put("coolRemainingDesc", formatDuration(coolRemaining));
        } else {
            data.put("coolRemainingSeconds", 0);
            data.put("coolRemainingDesc", "无冷却");
        }

        // 配种就绪检查
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(checkItem("活着", PetConstants.PET_ALIVE.equals(pet.getStatus())));
        checks.add(checkItem("已成熟(3天)", petService.isMature(pet)));
        checks.add(checkItem("不在怀孕期", pet.getPregnantUntil() == null || !pet.getPregnantUntil().isAfter(LocalDateTime.now())));
        checks.add(checkItem("不在冷却期", pet.getBreedingCoolUntil() == null || !pet.getBreedingCoolUntil().isAfter(LocalDateTime.now())));
        checks.add(checkItem("游戏币≥" + PetConstants.BREEDING_FEE, pet.getCoins() >= PetConstants.BREEDING_FEE));
        data.put("readinessChecks", checks);
        data.put("canBreed", checks.stream().allMatch(c -> Boolean.TRUE.equals(c.get("ok"))));

        // 我的幼崽统计
        long totalCubs = hatchlingMapper.selectCount(new LambdaQueryWrapper<PetHatchling>()
                .eq(PetHatchling::getUserId, userId));
        long storedCubs = hatchlingMapper.selectCount(new LambdaQueryWrapper<PetHatchling>()
                .eq(PetHatchling::getUserId, userId)
                .eq(PetHatchling::getStatus, PetConstants.HATCHLING_STORED));
        long frozenCubs = hatchlingMapper.selectCount(new LambdaQueryWrapper<PetHatchling>()
                .eq(PetHatchling::getUserId, userId)
                .eq(PetHatchling::getStatus, PetConstants.HATCHLING_FROZEN));
        long claimedCubs = hatchlingMapper.selectCount(new LambdaQueryWrapper<PetHatchling>()
                .eq(PetHatchling::getUserId, userId)
                .eq(PetHatchling::getStatus, PetConstants.HATCHLING_CLAIMED));
        data.put("totalCubs", totalCubs);
        data.put("storedCubs", storedCubs);
        data.put("frozenCubs", frozenCubs);
        data.put("claimedCubs", claimedCubs);

        return data;
    }

    /** 计算怀孕进度 */
    private Map<String, Object> calcPregnancyProgress(Pet mother) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = mother.getPregnantUntil();
        // 从配种记录找到怀孕开始时间(状态变更为PREGNANT的 updatedAt)
        BreedingRecord rec = breedingRecordMapper.selectOne(new LambdaQueryWrapper<BreedingRecord>()
                .eq(BreedingRecord::getMotherPetId, mother.getId())
                .eq(BreedingRecord::getStatus, PetConstants.BREED_PREGNANT)
                .last("LIMIT 1"));
        LocalDateTime start = rec != null && rec.getUpdatedAt() != null ? rec.getUpdatedAt() : now.minusDays(PetConstants.PREGNANT_DAYS);

        long totalSeconds = java.time.Duration.between(start, until).getSeconds();
        long elapsedSeconds = java.time.Duration.between(start, now).getSeconds();
        int progress = totalSeconds > 0 ? (int) Math.min(100, (elapsedSeconds * 100 / totalSeconds)) : 100;
        long remainingSeconds = Math.max(0, java.time.Duration.between(now, until).getSeconds());

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("progress", progress);
        p.put("remainingSeconds", remainingSeconds);
        p.put("remainingDesc", formatDuration(remainingSeconds));
        p.put("totalDays", PetConstants.PREGNANT_DAYS);
        p.put("stage", progress < 33 ? "初期" : progress < 66 ? "中期" : progress < 100 ? "后期" : "待产");
        p.put("stageEmoji", progress < 33 ? "🥚" : progress < 66 ? "🐣" : progress < 100 ? "🐤" : "🎉");
        return p;
    }

    /** 格式化时间描述 */
    private String formatDuration(long seconds) {
        if (seconds <= 0) return "已结束";
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        if (days > 0) return days + "天" + hours + "小时";
        if (hours > 0) return hours + "小时" + mins + "分";
        return mins + "分钟";
    }

    private Map<String, Object> checkItem(String label, boolean ok) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("ok", ok);
        return m;
    }

    // ==================== 单机版后院繁育 ====================

    /** NPC候选宠物市场（单机版，不依赖其他玩家） */
    public List<Map<String, Object>> npcMarket(String species) {
        List<Pet> npcs = petMapper.selectList(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getIsNpc, 1)
                .eq(Pet::getListed, 1)
                .eq(Pet::getStatus, PetConstants.PET_ALIVE));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Pet p : npcs) {
            if (p.getBreedingCoolUntil() != null && p.getBreedingCoolUntil().isAfter(LocalDateTime.now())) {
                continue;
            }
            PetType type = petTypeMapper.selectById(p.getPetTypeId());
            if (species != null && !species.isBlank() && (type == null || !species.equals(type.getTypeCode()))) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("petId", p.getId());
            m.put("petName", p.getPetName());
            m.put("typeCode", type != null ? type.getTypeCode() : null);
            m.put("typeName", type != null ? type.getTypeName() : null);
            m.put("subtypeName", p.getSubtypeName());
            m.put("rarity", type != null ? type.getRarity() : null);
            m.put("gender", p.getGender());
            m.put("personality", p.getPersonality());
            m.put("level", p.getLevel());
            m.put("isNpc", 1);
            rows.add(m);
        }
        return rows;
    }

    /**
     * 单机配种：用户宠物 + NPC宠物 → 直接怀孕（跳过收件箱同意流程）
     * 用户宠物为母方，NPC为父方（或反过来）；NPC不扣币、不冷却
     */
    public Map<String, Object> singleBreed(Long userId, Long npcPetId) {
        Pet myPet = petService.getMyPet(userId);
        if (myPet == null) {
            throw new BusinessException("你还没有宠物");
        }
        if (!petService.isMature(myPet)) {
            throw new BusinessException("你的宠物还未成熟，出生满 " + PetConstants.MATURE_DAYS + " 天后才能配种");
        }
        Pet npc = petMapper.selectById(npcPetId);
        if (npc == null || npc.getIsNpc() == null || npc.getIsNpc() != 1) {
            throw new BusinessException("NPC宠物不存在");
        }
        if (!PetConstants.PET_ALIVE.equals(npc.getStatus())) {
            throw new BusinessException("NPC宠物不在可配种状态");
        }

        Pet mother, father;
        if ("FEMALE".equals(myPet.getGender()) && "MALE".equals(npc.getGender())) {
            mother = myPet;
            father = npc;
        } else if ("MALE".equals(myPet.getGender()) && "FEMALE".equals(npc.getGender())) {
            mother = npc;
            father = myPet;
        } else {
            throw new BusinessException("配种双方必须是一公一母");
        }

        if (myPet.getPregnantUntil() != null && myPet.getPregnantUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("你的宠物正在怀孕，不能配种");
        }
        if (myPet.getBreedingCoolUntil() != null && myPet.getBreedingCoolUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("你的宠物还在配种冷却中");
        }
        if (myPet.getCoins() < PetConstants.BREEDING_FEE) {
            throw new BusinessException("游戏币不足（配种需 " + PetConstants.BREEDING_FEE + " 币）");
        }

        // 扣币（只扣用户方）
        myPet.setCoins(myPet.getCoins() - PetConstants.BREEDING_FEE);
        petMapper.updateById(myPet);

        // 直接怀孕（用户方为母方时，设怀孕；用户方为父方时，NPC直接"怀孕"产崽给用户）
        BreedingRecord rec = new BreedingRecord();
        rec.setMotherPetId(mother.getId());
        rec.setFatherPetId(father.getId());
        rec.setRequestedBy(userId);
        rec.setStatus(PetConstants.BREED_PREGNANT);
        breedingRecordMapper.insert(rec);

        // 用户方为母方 → 直接怀孕
        if (mother.getUserId().equals(userId)) {
            mother.setPregnantUntil(LocalDateTime.now().plusDays(PetConstants.PREGNANT_DAYS));
            mother.setBreedingCoolUntil(LocalDateTime.now().plusDays(PetConstants.BREEDING_COOL_DAYS));
            petMapper.updateById(mother);
        } else {
            // 用户方为父方，NPC为母方 → NPC立即产崽（幼崽归用户）
            maybeGiveBirth(mother);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", rec.getId());
        data.put("message", "配种成功！" + (mother.getUserId().equals(userId)
                ? "你的宠物怀孕了，" + PetConstants.PREGNANT_DAYS + " 天后产崽"
                : "NPC已产崽，去托管所查看幼崽"));
        return data;
    }

    /** 管理员调试:强制完成当前用户的繁育流程(怀孕→立即出生) */
    public Map<String, Object> debugFinishBreeding(Long userId) {
        Pet pet = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .eq(Pet::getStatus, PetConstants.PET_ALIVE)
                .last("LIMIT 1"));
        if (pet == null) {
            throw new BusinessException("没有活着的宠物");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (pet.getPregnantUntil() != null) {
            // 正在怀孕 → 强制到到时间并执行产崽
            pet.setPregnantUntil(LocalDateTime.now().minusMinutes(1));
            petMapper.updateById(pet);
            maybeGiveBirth(pet);
            result.put("action", "forced_birth");
            result.put("message", "已强制完成怀孕,幼崽已出生!去后院查看");
        } else if (pet.getBreedingCoolUntil() != null && pet.getBreedingCoolUntil().isAfter(LocalDateTime.now())) {
            // 冷却中 → 清除冷却
            pet.setBreedingCoolUntil(null);
            petMapper.updateById(pet);
            result.put("action", "cleared_cooldown");
            result.put("message", "已清除配种冷却,可以再次配种");
        } else {
            result.put("action", "nothing");
            result.put("message", "当前没有正在进行的繁育流程(先去后院配种)");
        }
        return result;
    }
}
