package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Blacklist;
import com.pet.entity.User;
import com.pet.mapper.BlacklistMapper;
import com.pet.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拉黑(单向):被拉黑者无法给你发消息
 */
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistMapper blacklistMapper;
    private final UserMapper userMapper;

    public void add(Long userId, Long blockedId) {
        if (blockedId.equals(userId)) {
            throw new BusinessException("不能拉黑自己");
        }
        User u = userMapper.selectById(blockedId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        Long exist = blacklistMapper.selectCount(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getUserId, userId).eq(Blacklist::getBlockedId, blockedId));
        if (exist > 0) {
            throw new BusinessException("已拉黑该用户");
        }
        Blacklist b = new Blacklist();
        b.setUserId(userId);
        b.setBlockedId(blockedId);
        blacklistMapper.insert(b);
    }

    public void remove(Long userId, Long blockedId) {
        blacklistMapper.delete(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getUserId, userId).eq(Blacklist::getBlockedId, blockedId));
    }

    public List<Map<String, Object>> list(Long userId) {
        List<Blacklist> rows = blacklistMapper.selectList(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getUserId, userId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Blacklist b : rows) {
            User u = userMapper.selectById(b.getBlockedId());
            if (u == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("blockedId", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            result.add(m);
        }
        return result;
    }

    /** b 是否拉黑了 a */
    public boolean isBlocked(Long a, Long b) {
        return blacklistMapper.selectCount(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getUserId, b).eq(Blacklist::getBlockedId, a)) > 0;
    }
}
