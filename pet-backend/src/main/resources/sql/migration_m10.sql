-- ============================================================
-- 增量升级脚本:M10(邮箱验证码注册 + 密保找回)
-- 用法:mysql -u root -p pet_ai < migration_m10.sql
-- ============================================================

USE pet_ai;

ALTER TABLE sys_user
    ADD COLUMN email              VARCHAR(100) DEFAULT NULL COMMENT '邮箱(注册验证/找回用)' AFTER nickname,
    ADD COLUMN security_question  VARCHAR(100) DEFAULT NULL COMMENT '密保问题(忘记密码用)' AFTER role,
    ADD COLUMN security_answer    VARCHAR(60)  DEFAULT NULL COMMENT '密保答案(BCrypt 哈希)' AFTER security_question,
    ADD UNIQUE KEY uk_email (email);

CREATE TABLE IF NOT EXISTS register_code (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    email      VARCHAR(100) NOT NULL COMMENT '目标邮箱',
    code       VARCHAR(10)  NOT NULL COMMENT '验证码(6 位数字)',
    expires_at DATETIME     NOT NULL COMMENT '过期时间(10 分钟)',
    used       TINYINT      NOT NULL DEFAULT 0 COMMENT '1 已使用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_email (email)
) ENGINE = InnoDB COMMENT = '注册邮箱验证码';
