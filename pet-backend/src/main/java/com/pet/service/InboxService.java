package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.dto.InboxMessageDTO;
import com.pet.entity.InboxMessage;
import com.pet.entity.User;
import com.pet.mapper.InboxMessageMapper;
import com.pet.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 收件箱业务(M3.5 拍板:申请 + 聊天,双方同意制,类似招聘平台私聊)
 */
@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxMessageMapper inboxMessageMapper;
    private final UserMapper userMapper;
    private final BreedingService breedingService;
    private final HatchlingService hatchlingService;
    private final FriendService friendService;
    private final BlacklistService blacklistService;

    /** 我的收件箱(收到的消息,新→旧) */
    public List<Map<String, Object>> listMine(Long userId) {
        List<InboxMessage> msgs = inboxMessageMapper.selectList(new LambdaQueryWrapper<InboxMessage>()
                .eq(InboxMessage::getReceiverId, userId)
                .orderByDesc(InboxMessage::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (InboxMessage m : msgs) {
            User sender = m.getSenderId() != null && m.getSenderId() > 0 ? userMapper.selectById(m.getSenderId()) : null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("messageId", m.getId());
            item.put("senderId", m.getSenderId());
            item.put("senderNickname", sender != null ? sender.getNickname() : "官方助手");
            item.put("senderPlayerId", sender != null ? sender.getPlayerId() : null);
            item.put("type", m.getType());
            item.put("content", m.getContent());
            item.put("status", m.getStatus());
            item.put("requestStatus", m.getRequestStatus());
            item.put("isOfficial", m.getIsOfficial());
            item.put("refId", m.getRefId());
            item.put("createdAt", m.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    /** 获取与某用户的对话线程(双向消息,按时间正序) */
    public List<Map<String, Object>> conversation(Long userId, Long withUserId) {
        List<InboxMessage> msgs = inboxMessageMapper.selectList(new LambdaQueryWrapper<InboxMessage>()
                .and(w -> w
                        .and(w1 -> w1.eq(InboxMessage::getSenderId, userId).eq(InboxMessage::getReceiverId, withUserId))
                        .or(w2 -> w2.eq(InboxMessage::getSenderId, withUserId).eq(InboxMessage::getReceiverId, userId)))
                .orderByAsc(InboxMessage::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (InboxMessage m : msgs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("messageId", m.getId());
            item.put("senderId", m.getSenderId());
            item.put("receiverId", m.getReceiverId());
            item.put("content", m.getContent());
            item.put("type", m.getType());
            item.put("status", m.getStatus());
            item.put("isOfficial", m.getIsOfficial());
            item.put("createdAt", m.getCreatedAt());
            item.put("isMine", m.getSenderId() != null && m.getSenderId().equals(userId));
            result.add(item);
        }
        // 标记对方发来的未读消息为已读
        for (InboxMessage m : msgs) {
            if (m.getReceiverId().equals(userId) && PetConstants.MSG_UNREAD.equals(m.getStatus())) {
                m.setStatus(PetConstants.MSG_READ);
                inboxMessageMapper.updateById(m);
            }
        }
        return result;
    }

    /** 聊天列表(微信式:按最后消息时间分组) */
    public List<Map<String, Object>> chatList(Long userId) {
        // 获取所有涉及我的消息
        List<InboxMessage> allMsgs = inboxMessageMapper.selectList(new LambdaQueryWrapper<InboxMessage>()
                .and(w -> w.eq(InboxMessage::getSenderId, userId).or().eq(InboxMessage::getReceiverId, userId))
                .orderByDesc(InboxMessage::getCreatedAt));
        // 按对话对象分组
        Map<Long, Map<String, Object>> threads = new LinkedHashMap<>();
        int totalUnread = 0;
        for (InboxMessage m : allMsgs) {
            Long otherId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
            if (!threads.containsKey(otherId)) {
                User other = otherId != null && otherId > 0 ? userMapper.selectById(otherId) : null;
                Map<String, Object> thread = new LinkedHashMap<>();
                thread.put("userId", otherId);
                thread.put("nickname", other != null ? other.getNickname() : "官方助手");
                thread.put("playerId", other != null ? other.getPlayerId() : null);
                thread.put("username", other != null ? other.getUsername() : null);
                thread.put("lastContent", m.getContent());
                thread.put("lastTime", m.getCreatedAt());
                thread.put("isOfficial", m.getIsOfficial() != null && m.getIsOfficial() == 1);
                // 统计未读
                int unread = (int) allMsgs.stream()
                        .filter(msg -> msg.getSenderId().equals(otherId) && msg.getReceiverId().equals(userId)
                                && PetConstants.MSG_UNREAD.equals(msg.getStatus()))
                        .count();
                thread.put("unreadCount", unread);
                totalUnread += unread;
                // 统计待处理申请数
                long pending = allMsgs.stream()
                        .filter(msg -> msg.getSenderId().equals(otherId) && msg.getReceiverId().equals(userId)
                                && PetConstants.REQ_PENDING.equals(msg.getRequestStatus()))
                        .count();
                thread.put("pendingCount", pending);
                thread.put("muted", false);
                threads.put(otherId, thread);
            }
        }
        // 加入好友列表中没有消息记录的好友
        List<Map<String, Object>> friends = friendService.list(userId);
        for (Map<String, Object> f : friends) {
            Long fid = (Long) f.get("friendId");
            if (!threads.containsKey(fid)) {
                Map<String, Object> thread = new LinkedHashMap<>();
                thread.put("userId", fid);
                thread.put("nickname", f.get("nickname"));
                thread.put("playerId", f.get("playerId"));
                thread.put("username", f.get("username"));
                thread.put("lastContent", "");
                thread.put("lastTime", f.get("since"));
                thread.put("isOfficial", Boolean.TRUE.equals(f.get("isOfficial")));
                thread.put("unreadCount", 0);
                thread.put("pendingCount", 0);
                thread.put("muted", Boolean.TRUE.equals(f.get("muted")));
                threads.put(fid, thread);
            } else {
                // 更新已有线程的 isOfficial 和 muted
                threads.get(fid).put("isOfficial", Boolean.TRUE.equals(f.get("isOfficial")));
                threads.get(fid).put("muted", Boolean.TRUE.equals(f.get("muted")));
            }
        }
        List<Map<String, Object>> result = new ArrayList<>(threads.values());
        // 排序:官方好友优先,然后按最后消息时间
        result.sort((a, b) -> {
            boolean aOff = (Boolean) a.getOrDefault("isOfficial", false);
            boolean bOff = (Boolean) b.getOrDefault("isOfficial", false);
            if (aOff != bOff) return aOff ? -1 : 1;
            return b.get("lastTime").toString().compareTo(a.get("lastTime").toString());
        });
        return result;
    }

    /** 发聊天消息 */
    public void sendChat(Long senderId, InboxMessageDTO dto) {
        User receiver = userMapper.selectById(dto.getReceiverId());
        if (receiver == null) {
            throw new BusinessException("接收人不存在");
        }
        if (receiver.getId().equals(senderId)) {
            throw new BusinessException("不能给自己发消息");
        }
        if (blacklistService.isBlocked(senderId, receiver.getId())) {
            throw new BusinessException("对方已拉黑你,无法发送消息");
        }
        InboxMessage msg = new InboxMessage();
        msg.setSenderId(senderId);
        msg.setReceiverId(dto.getReceiverId());
        msg.setType(PetConstants.MSG_CHAT);
        msg.setContent(dto.getContent());
        msg.setStatus(PetConstants.MSG_UNREAD);
        inboxMessageMapper.insert(msg);
    }

    /** 删除与某用户的对话(双向消息) */
    public void deleteConversation(Long userId, Long withUserId) {
        inboxMessageMapper.delete(new LambdaQueryWrapper<InboxMessage>()
                .eq(InboxMessage::getSenderId, userId)
                .eq(InboxMessage::getReceiverId, withUserId));
        inboxMessageMapper.delete(new LambdaQueryWrapper<InboxMessage>()
                .eq(InboxMessage::getSenderId, withUserId)
                .eq(InboxMessage::getReceiverId, userId));
    }

    /** 标记已读 */
    public void markRead(Long messageId, Long userId) {
        InboxMessage msg = requireMine(messageId, userId);
        msg.setStatus(PetConstants.MSG_READ);
        inboxMessageMapper.updateById(msg);
    }

    /** 同意申请:配种→怀孕;赠送→幼崽转移 */
    public void accept(Long messageId, Long userId) {
        InboxMessage msg = requireMine(messageId, userId);
        if (!PetConstants.REQ_PENDING.equals(msg.getRequestStatus())) {
            throw new BusinessException("该申请已处理");
        }
        if (PetConstants.MSG_BREED_REQUEST.equals(msg.getType())) {
            breedingService.accept(msg.getRefId(), userId);
        } else if (PetConstants.MSG_GIFT_REQUEST.equals(msg.getType())) {
            hatchlingService.acceptGift(msg, userId);
        } else if (PetConstants.MSG_FRIEND_REQUEST.equals(msg.getType())) {
            friendService.accept(msg);
        } else {
            throw new BusinessException("该消息不是申请");
        }
        msg.setRequestStatus(PetConstants.REQ_ACCEPTED);
        msg.setStatus(PetConstants.MSG_READ);
        inboxMessageMapper.updateById(msg);
    }

    /** 拒绝申请:配种退币;赠送无变化 */
    public void reject(Long messageId, Long userId) {
        InboxMessage msg = requireMine(messageId, userId);
        if (!PetConstants.REQ_PENDING.equals(msg.getRequestStatus())) {
            throw new BusinessException("该申请已处理");
        }
        if (PetConstants.MSG_BREED_REQUEST.equals(msg.getType())) {
            breedingService.reject(msg.getRefId(), userId);
        }
        msg.setRequestStatus(PetConstants.REQ_REJECTED);
        msg.setStatus(PetConstants.MSG_READ);
        inboxMessageMapper.updateById(msg);
    }

    private InboxMessage requireMine(Long messageId, Long userId) {
        InboxMessage msg = inboxMessageMapper.selectById(messageId);
        if (msg == null || !msg.getReceiverId().equals(userId)) {
            throw new BusinessException("消息不存在");
        }
        return msg;
    }
}
