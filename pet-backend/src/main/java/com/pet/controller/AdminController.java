package com.pet.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.Result;
import com.pet.common.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.AdminResetPasswordDTO;
import com.pet.dto.AdminSwitchPetDTO;
import com.pet.dto.AdminUserStatusDTO;
import com.pet.dto.AdminUserVO;
import com.pet.dto.ShopItemUpdateDTO;
import com.pet.entity.ChatMessage;
import com.pet.entity.Pet;
import com.pet.entity.PetGravestone;
import com.pet.entity.PomodoroRecord;
import com.pet.entity.Schedule;
import com.pet.entity.ShopItem;
import com.pet.entity.User;
import com.pet.entity.UserApiKey;
import com.pet.mapper.ChatMessageMapper;
import com.pet.mapper.PetGravestoneMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PomodoroRecordMapper;
import com.pet.mapper.ScheduleMapper;
import com.pet.mapper.ShopItemMapper;
import com.pet.mapper.UserApiKeyMapper;
import com.pet.mapper.UserMapper;
import com.pet.security.JwtInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员接口(仅 ADMIN 可用;独立控制面板数据源)
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final UserApiKeyMapper userApiKeyMapper;
    private final PetMapper petMapper;
    private final ScheduleMapper scheduleMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final PomodoroRecordMapper pomodoroRecordMapper;
    private final PetGravestoneMapper gravestoneMapper;
    private final ShopItemMapper shopItemMapper;
    private final com.pet.service.PetService petService;
    private final com.pet.service.PlayModeService playModeService;
    private final com.pet.service.SysConfigService sysConfigService;
    private final com.pet.service.ReportService reportService;
    private final com.pet.service.UploadService uploadService;
    private final com.pet.service.AdminApiConfigService adminApiConfigService;
    private final com.pet.service.BubblePromptService bubblePromptService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 删除用户(逻辑删除 + 禁用) */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setStatus(0);
        u.setDeleted(1);
        userMapper.updateById(u);
        return Result.ok();
    }

    /** 对话记忆条数(管理员可改,M13) */
    @GetMapping("/settings/chat-memory")
    public Result<Map<String, Object>> chatMemory(HttpServletRequest request) {
        checkAdmin(request);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", sysConfigService.chatMemory());
        return Result.ok(m);
    }

    @PutMapping("/settings/chat-memory")
    public Result<Void> setChatMemory(@Valid @RequestBody com.pet.dto.ChatMemoryDTO dto, HttpServletRequest request) {
        checkAdmin(request);
        sysConfigService.setChatMemory(dto.getValue());
        return Result.ok();
    }

    /** 玩耍模式管理 */
    @GetMapping("/play-modes")
    public Result<List<com.pet.entity.PlayMode>> playModes(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(playModeService.listAll());
    }

    @PostMapping("/play-modes")
    public Result<Void> addPlayMode(@Valid @RequestBody com.pet.dto.PlayModeDTO dto, HttpServletRequest request) {
        checkAdmin(request);
        playModeService.add(dto);
        return Result.ok();
    }

    @PutMapping("/play-modes/{id}")
    public Result<Void> updatePlayMode(@PathVariable Long id, @Valid @RequestBody com.pet.dto.PlayModeDTO dto,
                                       HttpServletRequest request) {
        checkAdmin(request);
        playModeService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/play-modes/{id}")
    public Result<Void> deletePlayMode(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        playModeService.delete(id);
        return Result.ok();
    }

    /** 新增商店物品(管理员上架新品,M13) */
    @PostMapping("/shop-items")
    public Result<Void> addShopItem(@Valid @RequestBody com.pet.dto.ShopItemAddDTO dto, HttpServletRequest request) {
        checkAdmin(request);
        ShopItem item = new ShopItem();
        item.setItemCode("ITM" + System.currentTimeMillis() % 1000000);
        item.setItemName(dto.getItemName());
        item.setItemType(dto.getItemType());
        item.setSpecies(dto.getSpecies());
        item.setImage(dto.getImage());
        item.setPrice(dto.getPrice());
        item.setHungerRestore(dto.getHungerRestore() == null ? 0 : dto.getHungerRestore());
        item.setMoodRestore(dto.getMoodRestore() == null ? 0 : dto.getMoodRestore());
        item.setMinLevel(dto.getMinLevel() == null ? 1 : dto.getMinLevel());
        item.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        shopItemMapper.insert(item);
        return Result.ok();
    }

    /** 图片上传(玩耍模式图标/商店物品图),返回 /uploads/... 路径 */
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> upload(@org.springframework.web.bind.annotation.RequestPart("file")
                                              org.springframework.web.multipart.MultipartFile file,
                                              HttpServletRequest request) {
        checkAdmin(request);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("path", uploadService.save(file, "admin"));
        return Result.ok(m);
    }

    /** 举报审核列表(含聊天内容) */
    @GetMapping("/reports")
    public Result<List<Map<String, Object>>> reports(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(reportService.adminList());
    }

    /** 处理举报:BLOCK 拉黑账户 / DISMISS 驳回 */
    @PostMapping("/reports/{id}/resolve")
    public Result<Void> resolveReport(@PathVariable Long id, @RequestBody Map<String, String> body,
                                      HttpServletRequest request) {
        checkAdmin(request);
        reportService.resolve(id, "BLOCK".equals(body.get("action")) ? "BLOCK" : "DISMISS");
        return Result.ok();
    }

    /** 数据总览:各类数据量 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(HttpServletRequest request) {
        checkAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("activeUserCount", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)));
        data.put("disabledUserCount", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 0)));
        data.put("petCount", petMapper.selectCount(null));
        data.put("alivePetCount", petMapper.selectCount(
                new LambdaQueryWrapper<Pet>().in(Pet::getStatus, "ALIVE", "DANGER")));
        data.put("deadPetCount", petMapper.selectCount(
                new LambdaQueryWrapper<Pet>().eq(Pet::getStatus, "DEAD")));
        data.put("scheduleCount", scheduleMapper.selectCount(null));
        data.put("chatCount", chatMessageMapper.selectCount(null));
        data.put("pomodoroCount", pomodoroRecordMapper.selectCount(null));
        data.put("completedPomodoroCount", pomodoroRecordMapper.selectCount(
                new LambdaQueryWrapper<PomodoroRecord>().eq(PomodoroRecord::getCompleted, 1)));
        data.put("gravestoneCount", gravestoneMapper.selectCount(null));
        // 举报统计
        data.put("pendingReports", reportService.adminList().stream()
                .filter(m -> "PENDING".equals(m.get("status"))).count());
        // API 配置数
        data.put("apiConfigCount", adminApiConfigService.listAll().size());
        // 气泡数
        data.put("bubbleCount", bubblePromptService.listAll().size());
        return Result.ok(data);
    }

    /** 用户分页列表(含"是否配置了 API Key") */
    @GetMapping("/users")
    public Result<Page<AdminUserVO>> listUsers(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               HttpServletRequest request) {
        checkAdmin(request);
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        qw.orderByDesc(User::getCreatedAt);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), qw);

        List<AdminUserVO> vos = new ArrayList<>();
        for (User u : p.getRecords()) {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setRole(u.getRole());
            vo.setStatus(u.getStatus());
            vo.setCreatedAt(u.getCreatedAt());
            Long keyCount = userApiKeyMapper.selectCount(
                    new LambdaQueryWrapper<UserApiKey>().eq(UserApiKey::getUserId, u.getId()));
            vo.setHasApiKey(keyCount > 0);
            vos.add(vo);
        }
        Page<AdminUserVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(vos);
        return Result.ok(result);
    }

    /** 启用 / 禁用用户 */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody AdminUserStatusDTO dto,
                                     HttpServletRequest request) {
        checkAdmin(request);
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setStatus(dto.getStatus());
        userMapper.updateById(u);
        return Result.ok();
    }

    /** 管理员重置用户密码(独立面板:后台数据管理) */
    @PostMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody AdminResetPasswordDTO dto,
                                      HttpServletRequest request) {
        checkAdmin(request);
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(u);
        return Result.ok();
    }

    /** 查看某用户的 Key 配置情况(M2 拍板:只显示是否配置,不展示内容) */
    @GetMapping("/users/{id}/api-keys")
    public Result<List<Map<String, Object>>> listKeys(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        List<UserApiKey> keys = userApiKeyMapper.selectList(
                new LambdaQueryWrapper<UserApiKey>().eq(UserApiKey::getUserId, id));
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserApiKey k : keys) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("provider", k.getProvider());
            item.put("enabled", k.getEnabled());
            result.add(item);
        }
        return Result.ok(result);
    }

    /** 全部宠物列表(含主人用户名) */
    @GetMapping("/pets")
    public Result<Page<Map<String, Object>>> listPets(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      HttpServletRequest request) {
        checkAdmin(request);
        Page<Pet> p = petMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Pet>().orderByDesc(Pet::getId));
        Map<Long, String> nickMap = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Pet pet : p.getRecords()) {
            String owner = nickMap.computeIfAbsent(pet.getUserId(), uid -> {
                User u = userMapper.selectById(uid);
                return u == null ? "?" : u.getUsername();
            });
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("petId", pet.getId());
            m.put("owner", owner);
            m.put("petName", pet.getPetName());
            m.put("subtypeName", pet.getSubtypeName());
            m.put("gender", pet.getGender());
            m.put("personality", pet.getPersonality());
            m.put("level", pet.getLevel());
            m.put("hunger", pet.getHunger());
            m.put("mood", pet.getMood());
            m.put("coins", pet.getCoins());
            m.put("status", pet.getStatus());
            m.put("createdAt", pet.getCreatedAt());
            rows.add(m);
        }
        Page<Map<String, Object>> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(rows);
        return Result.ok(result);
    }

    /** 商店物品列表(管理用,含下架商品) */
    @GetMapping("/shop-items")
    public Result<List<ShopItem>> shopItems(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(shopItemMapper.selectList(
                new LambdaQueryWrapper<ShopItem>().orderByAsc(ShopItem::getId)));
    }

    /** 修改商店物品(价格/上下架) */
    @PutMapping("/shop-items/{id}")
    public Result<Void> updateShopItem(@PathVariable Long id,
                                       @RequestBody ShopItemUpdateDTO dto,
                                       HttpServletRequest request) {
        checkAdmin(request);
        ShopItem item = shopItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("物品不存在");
        }
        if (dto.getPrice() != null) {
            if (dto.getPrice() < 0) {
                throw new BusinessException("价格不能为负");
            }
            item.setPrice(dto.getPrice());
        }
        if (dto.getEnabled() != null) {
            item.setEnabled(dto.getEnabled());
        }
        shopItemMapper.updateById(item);
        return Result.ok();
    }

    /** 管理员测试换宠(检查 UI):旧的标为已死,按指定种类/性别/性格新建(管理员无限币) */
    @PostMapping("/switch-pet")
    public Result<Map<String, Object>> switchPet(@Valid @RequestBody AdminSwitchPetDTO dto, HttpServletRequest request) {
        checkAdmin(request);
        Long adminId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.ok(petService.adminSwitch(adminId, dto.getTypeCode(), dto.getGender(), dto.getPersonality()));
    }

    // ─── Iter-05: 管理员 API 配置 ───

    /** 获取全部管理员 API 配置(Key 脱敏) */
    @GetMapping("/api-config")
    public Result<List<Map<String, Object>>> listApiConfig(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(adminApiConfigService.listAll());
    }

    /** 保存/更新管理员 API 配置 */
    @PostMapping("/api-config")
    public Result<Void> saveApiConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        checkAdmin(request);
        adminApiConfigService.save(body);
        return Result.ok();
    }

    /** 删除管理员 API 配置 */
    @DeleteMapping("/api-config/{id}")
    public Result<Void> deleteApiConfig(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        adminApiConfigService.delete(id);
        return Result.ok();
    }

    // ─── Iter-05: 气泡提示库管理 ───

    /** 获取全部气泡 */
    @GetMapping("/bubbles")
    public Result<List<com.pet.entity.BubblePrompt>> listBubbles(HttpServletRequest request) {
        checkAdmin(request);
        return Result.ok(bubblePromptService.listAll());
    }

    /** 添加气泡 */
    @PostMapping("/bubbles")
    public Result<Void> addBubble(@RequestBody com.pet.entity.BubblePrompt prompt, HttpServletRequest request) {
        checkAdmin(request);
        bubblePromptService.add(prompt);
        return Result.ok();
    }

    /** 删除气泡 */
    @DeleteMapping("/bubbles/{id}")
    public Result<Void> deleteBubble(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        bubblePromptService.delete(id);
        return Result.ok();
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute(JwtInterceptor.ATTR_ROLE);
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    // ─── Iter-07: 管理员群发通知 ───

    private final com.pet.mapper.InboxMessageMapper inboxMessageMapper;

    /** 群发通知(给所有活跃用户发送官方消息) */
    @PostMapping("/broadcast")
    public Result<Map<String, Object>> broadcast(@RequestBody Map<String, String> body, HttpServletRequest request) {
        checkAdmin(request);
        Long adminId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new BusinessException("通知内容不能为空");
        }
        List<User> activeUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .eq(User::getDeleted, 0)
                .ne(User::getId, adminId));
        int count = 0;
        for (User u : activeUsers) {
            com.pet.entity.InboxMessage msg = new com.pet.entity.InboxMessage();
            msg.setSenderId(adminId);
            msg.setReceiverId(u.getId());
            msg.setType("CHAT");
            msg.setContent(content);
            msg.setStatus("UNREAD");
            msg.setIsOfficial(1);
            inboxMessageMapper.insert(msg);
            count++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sentCount", count);
        return Result.ok(result);
    }

    /** 测试通知(仅发送给自己) */
    @PostMapping("/broadcast/test")
    public Result<Void> broadcastTest(@RequestBody Map<String, String> body, HttpServletRequest request) {
        checkAdmin(request);
        Long adminId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            content = "【测试通知】这是一条测试消息,如果你看到了说明通知功能正常。";
        }
        com.pet.entity.InboxMessage msg = new com.pet.entity.InboxMessage();
        msg.setSenderId(adminId);
        msg.setReceiverId(adminId);
        msg.setType("CHAT");
        msg.setContent("【测试】" + content);
        msg.setStatus("UNREAD");
        msg.setIsOfficial(1);
        inboxMessageMapper.insert(msg);
        return Result.ok();
    }
}
