-- ============================================================
-- 增量升级脚本:M13(玩耍模式入库/图片/相册/黑名单/举报/系统配置)
-- 用法:mysql -u root -p pet_ai < migration_m13.sql
-- ============================================================

USE pet_ai;

-- 1. 商店物品加图片
ALTER TABLE shop_item
    ADD COLUMN image VARCHAR(200) DEFAULT NULL COMMENT '图片路径(/uploads/...)' AFTER item_name;

-- 2. 玩耍模式表(管理员可增删改,配图)
CREATE TABLE IF NOT EXISTS play_mode (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name       VARCHAR(30) NOT NULL COMMENT '模式名,如 抚摸/跑步/学习',
    icon       VARCHAR(20) DEFAULT NULL COMMENT 'emoji 图标',
    image      VARCHAR(200) DEFAULT NULL COMMENT '图片路径(可选)',
    mood       INT         NOT NULL DEFAULT 0 COMMENT '心情变化',
    hunger     INT         NOT NULL DEFAULT 0 COMMENT '饥饿变化(负=消耗)',
    exp        INT         NOT NULL DEFAULT 0 COMMENT '经验变化',
    enabled    TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB COMMENT = '玩耍模式(管理员可维护)';

INSERT IGNORE INTO play_mode (name, icon, mood, hunger, exp, enabled) VALUES
('抚摸', '🤗', 4, 0, 0, 1),
('跑步', '🏃', 4, -3, 0, 1),
('学习', '📚', 1, 0, 2, 1),
('玩球', '⚽', 5, -2, 0, 1),
('散步', '🚶', 3, -1, 1, 1),
('听音乐', '🎵', 4, 0, 0, 1),
('跳舞', '💃', 5, -2, 0, 1),
('捉迷藏', '🙈', 5, -2, 0, 1),
('晒太阳', '☀️', 2, 0, 0, 1),
('扑蝶', '🦋', 4, -2, 0, 1);

-- 3. 系统配置(如 对话记忆条数,管理员可改)
CREATE TABLE IF NOT EXISTS sys_config (
    id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    cfg_key   VARCHAR(50)  NOT NULL COMMENT '键,如 chat_memory',
    cfg_value VARCHAR(200) DEFAULT NULL COMMENT '值',
    PRIMARY KEY (id),
    UNIQUE KEY uk_key (cfg_key)
) ENGINE = InnoDB COMMENT = '系统配置(管理员维护)';

INSERT IGNORE INTO sys_config (cfg_key, cfg_value) VALUES ('chat_memory', '10');

-- 4. 拍照相册(每宠物一个相册)
CREATE TABLE IF NOT EXISTS photo_album (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       NOT NULL COMMENT '归属用户',
    pet_id     BIGINT       NOT NULL COMMENT '宠物 pet.id',
    file_path  VARCHAR(300) NOT NULL COMMENT '图片路径(/uploads/photos/...)',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_pet_id (pet_id)
) ENGINE = InnoDB COMMENT = '宠物拍照相册';

-- 5. 拉黑
CREATE TABLE IF NOT EXISTS blacklist (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT   NOT NULL COMMENT '本人',
    blocked_id BIGINT   NOT NULL COMMENT '被拉黑用户',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pair (user_id, blocked_id)
) ENGINE = InnoDB COMMENT = '拉黑(单向)';

-- 6. 举报(聊天内容等,管理员审核后可拉黑账户)
CREATE TABLE IF NOT EXISTS report (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    reporter_id BIGINT       NOT NULL COMMENT '举报人',
    target_id   BIGINT       NOT NULL COMMENT '被举报用户',
    message_id  BIGINT       DEFAULT NULL COMMENT '关联消息(收件箱消息 id)',
    content     VARCHAR(500) DEFAULT NULL COMMENT '举报说明',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待审核 / BLOCKED 已拉黑 / DISMISSED 已驳回',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '举报(管理员审核)';
