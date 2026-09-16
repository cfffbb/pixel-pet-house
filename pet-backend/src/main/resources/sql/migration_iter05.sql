-- Iter-05: AI/语音集成 数据库迁移
-- 1. 管理员 API 配置表(全局 API,统一应用到所有用户)
-- 2. 气泡提示库表
-- 3. 用户语音偏好表(音色选择/是否朗读)

-- 1. 管理员 API 配置
CREATE TABLE IF NOT EXISTS admin_api_config (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_type    VARCHAR(20)  NOT NULL COMMENT 'API 类型:text / voice / image',
    provider    VARCHAR(30)  NOT NULL COMMENT '服务商标识:openai / qwen / siliconflow / zhipu / custom',
    base_url    VARCHAR(255)          COMMENT '接口地址',
    api_key_enc VARCHAR(512) NOT NULL COMMENT '加密后的 API Key',
    model_name  VARCHAR(100)          COMMENT '模型名',
    voice       VARCHAR(50)           COMMENT 'TTS 音色(仅 voice 类型)',
    enabled     TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(200),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员 API 配置(Iter-05)';

-- 2. 气泡提示库
CREATE TABLE IF NOT EXISTS bubble_prompt (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    species     VARCHAR(30)          COMMENT '宠物种类(留空=通用)',
    personality VARCHAR(30)          COMMENT '性格(留空=通用)',
    gender      VARCHAR(10)          COMMENT '性别(留空=通用)',
    stage       VARCHAR(20)          COMMENT '成长阶段(留空=通用)',
    category    VARCHAR(30) NOT NULL COMMENT '类别:greeting / sad / happy / hungry / sleepy / study / play / random',
    content     VARCHAR(300) NOT NULL COMMENT '气泡文本',
    enabled     TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='气泡提示库(Iter-05)';

-- 3. 用户语音偏好
CREATE TABLE IF NOT EXISTS user_voice_pref (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL UNIQUE,
    use_admin_key TINYINT      NOT NULL DEFAULT 1 COMMENT '1=用管理员配置的API 0=用自己的Key',
    tts_enabled   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否开启语音朗读',
    voice         VARCHAR(50)           DEFAULT 'alloy' COMMENT 'TTS音色',
    tts_speed     DECIMAL(3,1)          DEFAULT 1.0 COMMENT '语速 0.5-2.0',
    bubble_read   TINYINT      NOT NULL DEFAULT 0 COMMENT '气泡是否朗读',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户语音偏好(Iter-05)';

-- 4. 扩展 sys_config:管理员可配的默认音色列表
INSERT INTO sys_config (cfg_key, cfg_value) VALUES
    ('tts_default_voice', 'alloy'),
    ('tts_available_voices', 'alloy,echo,fable,onyx,nova,shimmer')
ON DUPLICATE KEY UPDATE cfg_key = cfg_key;

-- 5. 插入初始气泡数据(按种类/性格/性别/阶段组合)
INSERT INTO bubble_prompt (species, personality, gender, stage, category, content) VALUES
-- 通用问候
(NULL, NULL, NULL, NULL, 'greeting', '主人来啦!今天也要一起加油哦~'),
(NULL, NULL, NULL, NULL, 'greeting', '嘿嘿,终于等到你了!'),
(NULL, NULL, NULL, NULL, 'greeting', '想你想了好久,你终于来了!'),
(NULL, NULL, NULL, NULL, 'greeting', '欢迎回来!我帮你看着家呢~'),
-- 猫咪
('CAT', '活泼', 'MALE', 'BABY', 'random', '喵~我可是个小男子汉,快陪我玩!'),
('CAT', '温柔', 'FEMALE', 'BABY', 'random', '喵呜~主人抱抱我嘛'),
('CAT', '活泼', NULL, 'ADULT', 'random', '蹭蹭你!今天抓到虚拟虫子了吗?'),
('CAT', '温柔', NULL, 'ADULT', 'random', '咕噜咕噜~在主人身边最安心了'),
('CAT', '傲娇', NULL, NULL, 'random', '哼,才不是在等你呢...喵'),
-- 狗狗
('DOG', '活泼', 'MALE', 'BABY', 'random', '汪汪!我是小小护卫,谁也不许欺负你!'),
('DOG', '温柔', 'FEMALE', 'BABY', 'random', '汪~可以摸摸我的头吗?'),
('DOG', '活泼', NULL, 'ADULT', 'random', '摇尾巴!散步散步散步!'),
('DOG', '忠诚', NULL, NULL, 'random', '主人去哪我就去哪,永远陪着你!'),
-- 兔子
('RABBIT', '胆小', NULL, 'BABY', 'random', '缩成一团...你别突然出现嘛'),
('RABBIT', '活泼', NULL, 'ADULT', 'random', '蹦蹦跳跳!胡萝卜呢?'),
-- 学习相关
(NULL, NULL, NULL, NULL, 'study', '学习辛苦了!休息一下再继续吧~'),
(NULL, NULL, NULL, NULL, 'study', '番茄钟开始啦,我帮你看着时间!'),
(NULL, NULL, NULL, NULL, 'study', '专注中的主人最帅了!加油!'),
(NULL, NULL, NULL, NULL, 'study', '累了吗?喝口水,我等你~'),
-- 饥饿
(NULL, NULL, NULL, NULL, 'hungry', '肚子咕咕叫了...有零食吗?'),
(NULL, NULL, NULL, NULL, 'hungry', '好饿啊,去商店买点好吃的呗?'),
-- 开心
(NULL, NULL, NULL, NULL, 'happy', '今天超开心的!蹦蹦蹦!'),
(NULL, NULL, NULL, NULL, 'happy', '最喜欢和你在一起了!'),
-- 难过
(NULL, NULL, NULL, NULL, 'sad', '感觉你不太开心...我陪着你呢'),
(NULL, NULL, NULL, NULL, 'sad', '别难过啦,摸摸头,一切都会好的'),
-- 睡眠
(NULL, NULL, NULL, NULL, 'sleepy', '哈欠~好困,但我想等你一起睡'),
(NULL, NULL, NULL, NULL, 'sleepy', '主人也该休息了吧?晚安~'),
-- 玩耍
(NULL, NULL, NULL, NULL, 'play', '来玩呀来玩呀!'),
(NULL, NULL, NULL, NULL, 'play', '丢球!我去捡!再来一次!')
ON DUPLICATE KEY UPDATE content = VALUES(content);
