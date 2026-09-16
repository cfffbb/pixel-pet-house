package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.InboxMessageDTO;
import com.pet.security.JwtInterceptor;
import com.pet.service.InboxService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 收件箱接口(需登录):配种/赠送申请 + 双方聊天
 */
@RestController
@RequestMapping("/api/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    /** 我的收件箱 */
    @GetMapping
    public Result<List<Map<String, Object>>> list(HttpServletRequest request) {
        return Result.ok(inboxService.listMine((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 聊天列表(微信式) */
    @GetMapping("/chat-list")
    public Result<List<Map<String, Object>>> chatList(HttpServletRequest request) {
        return Result.ok(inboxService.chatList((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID)));
    }

    /** 与某用户的对话线程 */
    @GetMapping("/conversation")
    public Result<List<Map<String, Object>>> conversation(@RequestParam Long with, HttpServletRequest request) {
        return Result.ok(inboxService.conversation((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), with));
    }

    /** 发聊天消息 */
    @PostMapping("/message")
    public Result<Void> send(@Valid @RequestBody InboxMessageDTO dto, HttpServletRequest request) {
        inboxService.sendChat((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), dto);
        return Result.ok();
    }

    /** 标记已读 */
    @PostMapping("/{messageId}/read")
    public Result<Void> read(@PathVariable Long messageId, HttpServletRequest request) {
        inboxService.markRead(messageId, (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID));
        return Result.ok();
    }

    /** 同意申请(配种→怀孕;赠送→幼崽转移) */
    @PostMapping("/{messageId}/accept")
    public Result<Void> accept(@PathVariable Long messageId, HttpServletRequest request) {
        inboxService.accept(messageId, (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID));
        return Result.ok();
    }

    /** 拒绝申请(配种退币) */
    @PostMapping("/{messageId}/reject")
    public Result<Void> reject(@PathVariable Long messageId, HttpServletRequest request) {
        inboxService.reject(messageId, (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID));
        return Result.ok();
    }

    /** 删除与某用户的对话(双向消息) */
    @DeleteMapping("/conversation")
    public Result<Void> deleteConversation(@RequestParam Long with, HttpServletRequest request) {
        inboxService.deleteConversation((Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID), with);
        return Result.ok();
    }
}
