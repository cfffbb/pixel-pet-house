package com.pet.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.ResultCode;
import com.pet.common.exception.BusinessException;
import com.pet.dto.LoginDTO;
import com.pet.dto.LoginVO;
import com.pet.dto.RegisterDTO;
import com.pet.entity.RegisterCode;
import com.pet.entity.User;
import com.pet.mapper.RegisterCodeMapper;
import com.pet.mapper.UserMapper;
import com.pet.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 账号业务:注册(邮箱验证码 + 密保) / 登录 / 忘记密码(密保找回)
 * 安全红线:密码与密保答案一律 BCrypt 哈希存储,明文绝不落库
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String EMAIL_REGEX = "^[\\w.-]+@[\\w-]+(\\.[\\w-]+)+$";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final MailService mailService;
    private final RegisterCodeMapper registerCodeMapper;
    private final com.pet.mapper.FriendMapper friendMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 发送注册邮箱验证码(10 分钟有效) */
    public void sendRegisterCode(String email) {
        if (!ReUtil.isMatch(EMAIL_REGEX, email)) {
            throw new BusinessException("邮箱格式不正确");
        }
        Long emailExist = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (emailExist > 0) {
            throw new BusinessException("该邮箱已被注册");
        }
        String code = RandomUtil.randomNumbers(6);
        RegisterCode rc = new RegisterCode();
        rc.setEmail(email);
        rc.setCode(code);
        rc.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        rc.setUsed(0);
        registerCodeMapper.insert(rc);
        mailService.sendCode(email, code);
    }

    public Long register(RegisterDTO dto) {
        // 邮箱验证码校验
        if (!verifyCode(dto.getEmail(), dto.getCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        Long emailExist = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (emailExist > 0) {
            throw new BusinessException("该邮箱已被注册");
        }
        Long exist = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exist > 0) {
            throw new BusinessException("用户名已被占用");
        }
        if (StrUtil.isBlank(dto.getSecurityQuestion()) || StrUtil.isBlank(dto.getSecurityAnswer())) {
            throw new BusinessException("请设置密保问题与答案,用于忘记密码找回");
        }

        // M2 拍板:表里还没有任何用户时,首个注册的自动成为 ADMIN
        long userCount = userMapper.selectCount(null);
        String role = userCount == 0 ? "ADMIN" : "USER";

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StrUtil.isBlank(dto.getNickname()) ? dto.getUsername() : dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setRole(role);
        user.setSecurityQuestion(dto.getSecurityQuestion());
        user.setSecurityAnswer(passwordEncoder.encode(dto.getSecurityAnswer()));
        user.setStatus(1);
        userMapper.insert(user);

        // 生成玩家ID
        user.setPlayerId("P" + String.format("%05d", user.getId()));
        userMapper.updateById(user);

        // 自动添加官方好友(指向 id=1 的管理员,非首个用户时)
        if (user.getId() != 1) {
            com.pet.entity.Friend official = new com.pet.entity.Friend();
            official.setUserId(user.getId());
            official.setFriendId(1L);
            official.setMuted(0);
            official.setIsOfficial(1);
            friendMapper.insert(official);

            com.pet.entity.Friend reverse = new com.pet.entity.Friend();
            reverse.setUserId(1L);
            reverse.setFriendId(user.getId());
            reverse.setMuted(0);
            reverse.setIsOfficial(1);
            friendMapper.insert(reverse);
        }

        return user.getId();
    }

    /** 校验并消费验证码 */
    private boolean verifyCode(String email, String code) {
        RegisterCode rc = registerCodeMapper.selectOne(new LambdaQueryWrapper<RegisterCode>()
                .eq(RegisterCode::getEmail, email)
                .eq(RegisterCode::getCode, code)
                .eq(RegisterCode::getUsed, 0)
                .gt(RegisterCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(RegisterCode::getId)
                .last("LIMIT 1"));
        if (rc == null) {
            return false;
        }
        rc.setUsed(1);
        registerCodeMapper.updateById(rc);
        return true;
    }

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        // 用户不存在和密码错误统一提示,不泄露"哪个不对"
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getRole(), user.getPlayerId());
    }

    /** 忘记密码第一步:返回该用户的密保问题 */
    public Map<String, String> forgotQuestion(String username) {
        User u = findByUsername(username);
        return Map.of("securityQuestion", StrUtil.blankToDefault(u.getSecurityQuestion(), "未设置密保问题"));
    }

    /** 忘记密码第二步:密保答案正确则重置密码 */
    public void forgotReset(String username, String answer, String newPassword) {
        User u = findByUsername(username);
        if (StrUtil.isBlank(u.getSecurityAnswer())
                || !passwordEncoder.matches(StrUtil.blankToDefault(answer, ""), u.getSecurityAnswer())) {
            throw new BusinessException("密保答案不正确");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("新密码至少 8 位");
        }
        u.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(u);
    }

    private User findByUsername(String username) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        return u;
    }
}
