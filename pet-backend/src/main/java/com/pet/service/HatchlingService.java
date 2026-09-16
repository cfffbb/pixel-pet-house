package com.pet.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.dto.ClaimDTO;
import com.pet.dto.GiftDTO;
import com.pet.entity.InboxMessage;
import com.pet.entity.Pet;
import com.pet.entity.PetHatchling;
import com.pet.entity.PetType;
import com.pet.entity.User;
import com.pet.mapper.InboxMessageMapper;
import com.pet.mapper.PetGravestoneMapper;
import com.pet.mapper.PetHatchlingMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PetTypeMapper;
import com.pet.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 幼崽托管业务:托管费懒结算 / 领取 / 赠送申请
 */
@Service
@RequiredArgsConstructor
public class HatchlingService {

    private final PetHatchlingMapper hatchlingMapper;
    private final PetMapper petMapper;
    private final PetTypeMapper petTypeMapper;
    private final PetGravestoneMapper gravestoneMapper;
    private final InboxMessageMapper inboxMessageMapper;
    private final UserMapper userMapper;
    private final PetService petService;

    /** 我的托管幼崽;顺带懒结算托管费(每只 5 币/天,不足则冻结) */
    public List<Map<String, Object>> listMine(Long userId) {
        List<PetHatchling> list = hatchlingMapper.selectList(new LambdaQueryWrapper<PetHatchling>()
                .eq(PetHatchling::getUserId, userId)
                .orderByDesc(PetHatchling::getStoredAt));
        Pet live = petService.getMyPet(userId);
        for (PetHatchling h : list) {
            if (!PetConstants.HATCHLING_STORED.equals(h.getStatus())) {
                continue;
            }
            long days = daysStored(h.getStoredAt());
            if (days <= 0) {
                continue;
            }
            long fee = days * PetConstants.HATCHLING_FEE_PER_DAY;
            if (live != null && live.getCoins() >= fee) {
                live.setCoins(live.getCoins() - (int) fee);
                petMapper.updateById(live);
                h.setStoredAt(LocalDateTime.now()); // 支付后重置计费起点
                hatchlingMapper.updateById(h);
            } else {
                h.setStatus(PetConstants.HATCHLING_FROZEN);
                hatchlingMapper.updateById(h);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (PetHatchling h : list) {
            result.add(toMap(h));
        }
        return result;
    }

    private long daysStored(LocalDateTime storedAt) {
        long minutes = Duration.between(storedAt, LocalDateTime.now()).toMinutes();
        long days = minutes / (24 * 60);
        if (minutes % (24 * 60) > 0) {
            days++;
        }
        return days;
    }

    private Map<String, Object> toMap(PetHatchling h) {
        PetType type = petTypeMapper.selectById(h.getPetTypeId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("typeName", type != null ? type.getTypeName() : null);
        m.put("subtypeName", h.getSubtypeName());
        m.put("gender", h.getGender());
        m.put("personality", h.getPersonality());
        m.put("rarity", h.getRarity());
        m.put("status", h.getStatus());
        m.put("storedAt", h.getStoredAt());
        return m;
    }

    /** 领取幼崽:无活宠 + 已通过反思答题 + 强制起名 */
    public Map<String, Object> claim(Long userId, ClaimDTO dto) {
        PetHatchling h = hatchlingMapper.selectById(dto.getHatchlingId());
        if (h == null || !h.getUserId().equals(userId)) {
            throw new BusinessException("幼崽不存在");
        }
        if (!PetConstants.HATCHLING_STORED.equals(h.getStatus())) {
            throw new BusinessException("该幼崽当前不可领取(欠费冻结或其他状态)");
        }
        if (petService.hasLivePet(userId)) {
            throw new BusinessException("你已有一只宠物,不能同时养两只");
        }
        if (petService.hasUnansweredGravestone(userId)) {
            throw new BusinessException("请先完成反思答题,才能迎接新的宠物");
        }
        if (StrUtil.isBlank(dto.getPetName()) || dto.getPetName().trim().length() > 12) {
            throw new BusinessException("宠物名 1–12 个字");
        }
        PetType type = petTypeMapper.selectById(h.getPetTypeId());
        Pet pet = new Pet();
        pet.setUserId(userId);
        pet.setPetTypeId(h.getPetTypeId());
        pet.setPetName(dto.getPetName().trim());
        pet.setSubtypeName(h.getSubtypeName());
        pet.setGender(h.getGender());
        pet.setPersonality(h.getPersonality());
        pet.setLevel(1);
        pet.setExp(0);
        pet.setHunger(100);
        pet.setMood(100);
        pet.setCoins(0);
        pet.setStatus(PetConstants.PET_ALIVE);
        pet.setHatchedAt(LocalDateTime.now());
        pet.setHungerCalcAt(LocalDateTime.now());
        petMapper.insert(pet);
        petService.giveInitialFood(pet.getId()); // 领养的幼崽同样送初始食物(M7)

        h.setStatus(PetConstants.HATCHLING_CLAIMED);
        hatchlingMapper.updateById(h);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("petId", pet.getId());
        data.put("typeName", type != null ? type.getTypeName() : null);
        data.put("rarity", h.getRarity());
        return data;
    }

    /** 赠送申请:发到对方收件箱,对方同意后转移 */
    public void giftRequest(Long userId, GiftDTO dto) {
        PetHatchling h = hatchlingMapper.selectById(dto.getHatchlingId());
        if (h == null || !h.getUserId().equals(userId)) {
            throw new BusinessException("幼崽不存在");
        }
        User target = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getTargetUsername()));
        if (target == null) {
            throw new BusinessException("对方用户不存在");
        }
        if (target.getId().equals(userId)) {
            throw new BusinessException("不能送给自己");
        }
        InboxMessage msg = new InboxMessage();
        msg.setSenderId(userId);
        msg.setReceiverId(target.getId());
        msg.setType(PetConstants.MSG_GIFT_REQUEST);
        msg.setRefId(h.getId());
        msg.setContent("想送你一只" + h.getRarity() + "品质的幼崽,请查收");
        msg.setStatus(PetConstants.MSG_UNREAD);
        msg.setRequestStatus(PetConstants.REQ_PENDING);
        inboxMessageMapper.insert(msg);
    }

    /** 对方同意赠送:幼崽归属转移给接收人 */
    public void acceptGift(InboxMessage msg, Long receiverId) {
        Long hatchlingId = msg.getRefId();
        PetHatchling h = hatchlingMapper.selectById(hatchlingId);
        if (h == null) {
            throw new BusinessException("幼崽已不存在");
        }
        h.setUserId(receiverId);
        h.setStatus(PetConstants.HATCHLING_STORED);
        h.setStoredAt(LocalDateTime.now());
        hatchlingMapper.updateById(h);
    }
}
