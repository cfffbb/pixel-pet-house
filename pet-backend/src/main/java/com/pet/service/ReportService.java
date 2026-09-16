package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.dto.ReportCreateDTO;
import com.pet.entity.Report;
import com.pet.entity.User;
import com.pet.mapper.InboxMessageMapper;
import com.pet.mapper.ReportMapper;
import com.pet.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 举报(管理员审核后可拉黑账户)
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final InboxMessageMapper inboxMessageMapper;

    public void create(Long reporterId, ReportCreateDTO dto) {
        if (dto.getTargetUserId().equals(reporterId)) {
            throw new BusinessException("不能举报自己");
        }
        Report r = new Report();
        r.setReporterId(reporterId);
        r.setTargetId(dto.getTargetUserId());
        r.setMessageId(dto.getMessageId());
        r.setContent(dto.getContent());
        r.setStatus(PetConstants.REQ_PENDING);
        reportMapper.insert(r);
    }

    /** 管理员:举报列表(含相关聊天内容) */
    public List<Map<String, Object>> adminList() {
        List<Report> list = reportMapper.selectList(new LambdaQueryWrapper<Report>()
                .orderByDesc(Report::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Report r : list) {
            User reporter = userMapper.selectById(r.getReporterId());
            User target = userMapper.selectById(r.getTargetId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("reporter", reporter != null ? reporter.getUsername() : "?");
            m.put("target", target != null ? target.getUsername() : "?");
            m.put("targetId", r.getTargetId());
            m.put("content", r.getContent());
            m.put("status", r.getStatus());
            m.put("createdAt", r.getCreatedAt());
            // 相关聊天内容
            if (r.getMessageId() != null) {
                var msg = inboxMessageMapper.selectById(r.getMessageId());
                m.put("messageContent", msg != null ? msg.getContent() : null);
            }
            result.add(m);
        }
        return result;
    }

    /** 管理员处理:BLOCK 拉黑账户(禁用) / DISMISS 驳回 */
    public void resolve(Long reportId, String action) {
        Report r = reportMapper.selectById(reportId);
        if (r == null) {
            throw new BusinessException("举报不存在");
        }
        if ("BLOCK".equals(action)) {
            User u = userMapper.selectById(r.getTargetId());
            if (u != null) {
                u.setStatus(0);
                userMapper.updateById(u);
            }
            r.setStatus("BLOCKED");
        } else {
            r.setStatus("DISMISSED");
        }
        reportMapper.updateById(r);
    }
}
