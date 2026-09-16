package com.pet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务(注册验证码)
 * 安全红线:SMTP 账号密码一律用 <你的密钥> 占位,配置时不提交 git。
 * 本地未配置 SMTP 时发送会失败,验证码会打进后端日志便于开发调试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    public void sendCode(String to, String code) {
        // 开发调试:未配置 SMTP 时验证码可从这里看到(上线前移除)
        log.info("注册验证码 {} 发送到 {}", code, to);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject("AI 宠物助手 - 注册验证码");
            msg.setText("你的注册验证码是:" + code + ",10 分钟内有效。");
            mailSender.send(msg);
            log.info("验证码邮件已发送:{}", to);
        } catch (Exception e) {
            // SMTP 未配置(占位符)时降级:验证码已在上面日志里,便于本地开发联调
            log.warn("邮件发送失败(SMTP 未配置?):{}", e.getMessage());
        }
    }
}
