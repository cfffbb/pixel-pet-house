package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Friend;
import com.pet.entity.InboxMessage;
import com.pet.entity.Pet;
import com.pet.entity.PetType;
import com.pet.entity.User;
import com.pet.mapper.FriendMapper;
import com.pet.mapper.InboxMessageMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PetTypeMapper;
import com.pet.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 好友业务(M11):申请(收件箱)→ 同意 → 双向好友
 */
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final InboxMessageMapper inboxMessageMapper;
    private final PetMapper petMapper;
    private final PetTypeMapper petTypeMapper;

    /** 发好友申请(到对方收件箱,对方同意后成为好友) */
    public void request(Long userId, String targetUsername, String targetPlayerId, Long targetUserId) {
        User target = null;
        if (targetUserId != null) {
            target = userMapper.selectById(targetUserId);
        } else if (targetPlayerId != null && !targetPlayerId.isBlank()) {
            target = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getPlayerId, targetPlayerId).last("LIMIT 1"));
        } else if (targetUsername != null && !targetUsername.isBlank()) {
            target = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, targetUsername).last("LIMIT 1"));
        }
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if (target.getId().equals(userId)) {
            throw new BusinessException("不能添加自己为好友");
        }
        if (isFriend(userId, target.getId())) {
            throw new BusinessException("已经是好友了");
        }
        Long pending = inboxMessageMapper.selectCount(new LambdaQueryWrapper<InboxMessage>()
                .eq(InboxMessage::getSenderId, userId)
                .eq(InboxMessage::getReceiverId, target.getId())
                .eq(InboxMessage::getType, PetConstants.MSG_FRIEND_REQUEST)
                .eq(InboxMessage::getRequestStatus, PetConstants.REQ_PENDING));
        if (pending > 0) {
            throw new BusinessException("已发送过好友申请,等对方处理");
        }
        InboxMessage msg = new InboxMessage();
        msg.setSenderId(userId);
        msg.setReceiverId(target.getId());
        msg.setType(PetConstants.MSG_FRIEND_REQUEST);
        msg.setContent("想加你为好友,一起养宠物吧");
        msg.setStatus(PetConstants.MSG_UNREAD);
        msg.setRequestStatus(PetConstants.REQ_PENDING);
        inboxMessageMapper.insert(msg);
    }

    /** 对方同意:双向建立好友(收件箱 accept 调用) */
    public void accept(InboxMessage msg) {
        addPair(msg.getSenderId(), msg.getReceiverId());
    }

    private void addPair(Long a, Long b) {
        Friend f1 = new Friend();
        f1.setUserId(a);
        f1.setFriendId(b);
        friendMapper.insert(f1);
        Friend f2 = new Friend();
        f2.setUserId(b);
        f2.setFriendId(a);
        friendMapper.insert(f2);
    }

    private boolean isFriend(Long a, Long b) {
        return friendMapper.selectCount(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, a).eq(Friend::getFriendId, b)) > 0;
    }

    /** 我的好友列表(含好友宠物形象,做头像用) */
    public List<Map<String, Object>> list(Long userId) {
        List<Friend> rows = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).orderByDesc(Friend::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Friend f : rows) {
            User u = userMapper.selectById(f.getFriendId());
            if (u == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("friendId", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("playerId", u.getPlayerId());
            m.put("since", f.getCreatedAt());
            m.put("muted", f.getMuted() != null && f.getMuted() == 1);
            m.put("isOfficial", f.getIsOfficial() != null && f.getIsOfficial() == 1);
            // 好友当前宠物形象(默认头像)
            Pet pet = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                    .eq(Pet::getUserId, u.getId())
                    .in(Pet::getStatus, "ALIVE", "DANGER")
                    .last("LIMIT 1"));
            if (pet != null) {
                PetType t = petTypeMapper.selectById(pet.getPetTypeId());
                m.put("petTypeCode", t != null ? t.getTypeCode() : null);
                m.put("petGender", pet.getGender());
            }
            result.add(m);
        }
        return result;
    }

    /** 删除好友(双向;官方好友不可删除) */
    public void remove(Long userId, Long friendId) {
        Friend f = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId).last("LIMIT 1"));
        if (f != null && f.getIsOfficial() != null && f.getIsOfficial() == 1) {
            throw new BusinessException("官方助手不可删除");
        }
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId));
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId));
    }

    /** 免打扰开关(Iter-06) */
    public void toggleMute(Long userId, Long friendId, int muted) {
        Friend f = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId).last("LIMIT 1"));
        if (f == null) {
            throw new com.pet.common.exception.BusinessException("不是好友关系");
        }
        f.setMuted(muted);
        friendMapper.updateById(f);
    }

    /** 检查是否被对方免打扰(发消息时用) */
    public boolean isMuted(Long senderId, Long receiverId) {
        Friend f = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, receiverId).eq(Friend::getFriendId, senderId).last("LIMIT 1"));
        return f != null && f.getMuted() != null && f.getMuted() == 1;
    }
}
