-- ============================================================
-- 桌面 AI 宠物助手 · 建库建表脚本(最终版,M1–M3.5 全部表)
-- MySQL 8.0,字符集 utf8mb4
-- 用法:mysql -u root -p < schema.sql(或 Navicat 直接运行整个文件)
-- 说明:本文件包含全部 17 张表结构 + 字典初始数据(宠物种类/亚种)。
--       已建过库的老用户请执行 sql/migration_m3.sql 增量升级。
-- ============================================================

CREATE DATABASE IF NOT EXISTS pet_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pet_ai;

-- ------------------------------------------------------------
-- 1. 用户表(普通用户 + 管理员)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(50) NOT NULL COMMENT '登录名(唯一)',
    password   VARCHAR(60) NOT NULL COMMENT 'BCrypt 哈希,固定 60 位,明文绝不落库',
    nickname   VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    email      VARCHAR(100) DEFAULT NULL COMMENT '邮箱(注册验证/找回用)',
    role       VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色:USER 普通用户 / ADMIN 管理员',
    security_question VARCHAR(100) DEFAULT NULL COMMENT '密保问题(忘记密码用)',
    security_answer   VARCHAR(60)  DEFAULT NULL COMMENT '密保答案(BCrypt 哈希)',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态:1 启用 / 0 禁用',
    created_at DATETIME    NOT NULL COMMENT '创建时间',
    updated_at DATETIME    NOT NULL COMMENT '更新时间',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除:0 未删 / 1 已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE = InnoDB COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 用户自填的 AI API Key(多端,加密存储)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_api_key (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '所属用户 sys_user.id',
    provider      VARCHAR(50)  NOT NULL COMMENT '服务商标识,如 openai / qwen / zhipu 等',
    key_encrypted VARCHAR(500) NOT NULL COMMENT '加密后的 Key(配置示例一律用 <你的密钥> 占位)',
    base_url      VARCHAR(200) DEFAULT NULL COMMENT '可选:自定义接口地址',
    model_name    VARCHAR(100) DEFAULT NULL COMMENT '对话模型名,如 gpt-4o-mini / qwen-plus',
    remark        VARCHAR(100) DEFAULT NULL COMMENT '备注',
    enabled       TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    created_at    DATETIME     NOT NULL COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '用户 AI API Key 表';

-- ------------------------------------------------------------
-- 3. 用户设置(kv 键值;allow_breeding 配种授权等)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_setting (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '所属用户',
    setting_key   VARCHAR(50)  NOT NULL COMMENT '设置项键名,如 allow_breeding / app_monitor_enabled',
    setting_value VARCHAR(200) DEFAULT NULL COMMENT '设置项值,如 true / false',
    created_at    DATETIME     NOT NULL COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_key (user_id, setting_key)
) ENGINE = InnoDB COMMENT = '用户设置表';

-- ------------------------------------------------------------
-- 4. 宠物种类字典(13 种;base_days = 基准饿死天数,M3 拍板)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pet_type (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    type_code   VARCHAR(30) NOT NULL COMMENT '种类编码,如 CAT / DOG / DRAGON',
    type_name   VARCHAR(30) NOT NULL COMMENT '展示名',
    rarity      VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '稀有度:NORMAL 普通 / RARE 稀有 / LEGEND 传说',
    base_days   INT         NOT NULL DEFAULT 5 COMMENT '基准饿死天数(3–7,按体型代谢,M3 拍板)',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    enabled     TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_code (type_code)
) ENGINE = InnoDB COMMENT = '宠物种类字典表';

-- ------------------------------------------------------------
-- 5. 宠物亚种字典(仅展示,不影响数值;猫狗各 5,其余各 2,M3 终审)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pet_subtype (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    pet_type_id  BIGINT      NOT NULL COMMENT '所属种类 pet_type.id',
    subtype_name VARCHAR(30) NOT NULL COMMENT '亚种名,如 布偶猫',
    enabled      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_pet_type_id (pet_type_id)
) ENGINE = InnoDB COMMENT = '宠物亚种字典表';

-- ------------------------------------------------------------
-- 6. 宠物档案(一只宠物一行;含性别/亚种/病危/怀孕/配种冷却)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pet (
    id                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id            BIGINT      NOT NULL COMMENT '归属用户',
    pet_type_id        BIGINT      NOT NULL COMMENT '种类 pet_type.id',
    pet_name           VARCHAR(30) DEFAULT NULL COMMENT '宠物昵称(强制起名)',
    subtype_name       VARCHAR(30) DEFAULT NULL COMMENT '亚种名(仅展示)',
    gender             VARCHAR(10) NOT NULL DEFAULT 'MALE' COMMENT '性别:MALE 公 / FEMALE 母(50/50 随机)',
    personality        VARCHAR(30) DEFAULT NULL COMMENT '性格:活泼/高冷/粘人/贪吃/傲娇',
    level              INT         NOT NULL DEFAULT 1 COMMENT '等级',
    exp                INT         NOT NULL DEFAULT 0 COMMENT '经验',
    hunger             INT         NOT NULL DEFAULT 100 COMMENT '饥饿度(0–100)',
    mood               INT         NOT NULL DEFAULT 100 COMMENT '心情值(0–100)',
    coins              INT         NOT NULL DEFAULT 0 COMMENT '游戏币(兑换规则 M7 拍板)',
    status             VARCHAR(20) NOT NULL DEFAULT 'ALIVE' COMMENT '状态:ALIVE 活着 / DANGER 病危 / STARVED 已死',
    hatched_at         DATETIME    DEFAULT NULL COMMENT '出生/孵化时间',
    last_fed_at        DATETIME    DEFAULT NULL COMMENT '上次喂食时间',
    hunger_calc_at     DATETIME    DEFAULT NULL COMMENT '上次结算饥饿的时间(懒计算用)',
    danger_since       DATETIME    DEFAULT NULL COMMENT '进入病危的时间(24 小时内喂食可救)',
    pregnant_until     DATETIME    DEFAULT NULL COMMENT '怀孕截止时间(到期产崽)',
    breeding_cool_until DATETIME   DEFAULT NULL COMMENT '配种冷却截止时间',
    play_date          DATE        DEFAULT NULL COMMENT '玩耍日期(每日重置)',
    play_count         INT         NOT NULL DEFAULT 0 COMMENT '今日玩耍次数',
    behavior_date      DATE        DEFAULT NULL COMMENT '行为事件发生日期(每天最多 1 次)',
    created_at         DATETIME    NOT NULL COMMENT '创建时间',
    updated_at         DATETIME    NOT NULL COMMENT '更新时间',
    deleted            TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_pet_type_id (pet_type_id)
) ENGINE = InnoDB COMMENT = '宠物档案表';

-- ------------------------------------------------------------
-- 7. 墓碑表(死亡纪念 + 反思答题记录,点击墓碑查看反思)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pet_gravestone (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT      NOT NULL COMMENT '所属用户',
    pet_id         BIGINT      DEFAULT NULL COMMENT '原宠物 pet.id',
    pet_name       VARCHAR(30) DEFAULT NULL COMMENT '宠物名',
    pet_type_id    BIGINT      DEFAULT NULL COMMENT '种类 id',
    type_name      VARCHAR(30) DEFAULT NULL COMMENT '种类名(快照)',
    subtype_name   VARCHAR(30) DEFAULT NULL COMMENT '亚种(快照)',
    gender         VARCHAR(10) DEFAULT NULL COMMENT '性别(快照)',
    personality    VARCHAR(30) DEFAULT NULL COMMENT '性格(快照)',
    born_at        DATETIME    DEFAULT NULL COMMENT '出生时间',
    died_at        DATETIME    NOT NULL COMMENT '死亡时间',
    lifespan_days  BIGINT      DEFAULT 0 COMMENT '存活天数',
    answered       TINYINT     NOT NULL DEFAULT 0 COMMENT '1 已完成反思答题 / 0 未答',
    reflection_1   VARCHAR(500) DEFAULT NULL COMMENT '反思题 1 回答(死因标注)',
    reflection_2   VARCHAR(500) DEFAULT NULL COMMENT '反思题 2 回答',
    created_at     DATETIME    NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '宠物墓碑表';

-- ------------------------------------------------------------
-- 8. 幼崽托管表(托管所;出生 3–5 只、10% 夭折、每只 5 币/天)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pet_hatchling (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT      NOT NULL COMMENT '归属用户(出生在母方托管所)',
    mother_pet_id BIGINT     NOT NULL COMMENT '母方宠物 pet.id',
    father_pet_id BIGINT     DEFAULT NULL COMMENT '父方宠物 pet.id',
    pet_type_id  BIGINT      NOT NULL COMMENT '种类 id(50/50 取父母)',
    subtype_name VARCHAR(30) DEFAULT NULL COMMENT '亚种',
    gender       VARCHAR(10) NOT NULL DEFAULT 'MALE' COMMENT '性别',
    personality  VARCHAR(30) DEFAULT NULL COMMENT '性格',
    rarity       VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '品质(受父母影响)',
    status       VARCHAR(20) NOT NULL DEFAULT 'STORED' COMMENT 'STORED 托管中 / FROZEN 欠费冻结 / CLAIMED 已领取',
    stored_at    DATETIME    NOT NULL COMMENT '入托时间(托管费计费起点)',
    created_at   DATETIME    NOT NULL COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '幼崽托管表';

-- ------------------------------------------------------------
-- 9. 配种记录表(申请→同意→怀孕→产崽)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS breeding_record (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    mother_pet_id BIGINT      NOT NULL COMMENT '母方宠物 pet.id',
    father_pet_id BIGINT      NOT NULL COMMENT '父方宠物 pet.id',
    requested_by  BIGINT      NOT NULL COMMENT '发起人(母方主人)',
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待对方同意 / PREGNANT 怀孕中 / BORN 已产崽 / REJECTED 已拒绝',
    created_at    DATETIME    NOT NULL COMMENT '创建时间',
    updated_at    DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_mother_pet_id (mother_pet_id)
) ENGINE = InnoDB COMMENT = '配种记录表';

-- ------------------------------------------------------------
-- 10. 收件箱消息表(配种/赠送申请 + 双方聊天,双方同意制)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inbox_message (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    sender_id      BIGINT       NOT NULL COMMENT '发送人',
    receiver_id    BIGINT       NOT NULL COMMENT '接收人',
    type           VARCHAR(20)  NOT NULL COMMENT 'BREED_REQUEST 配种申请 / GIFT_REQUEST 赠送申请 / CHAT 聊天',
    ref_id         BIGINT       DEFAULT NULL COMMENT '关联:配种记录 id 或幼崽 id',
    content        VARCHAR(500) DEFAULT NULL COMMENT '内容(申请备注或聊天文本)',
    status         VARCHAR(20)  NOT NULL DEFAULT 'UNREAD' COMMENT 'UNREAD 未读 / READ 已读',
    request_status VARCHAR(20)  DEFAULT NULL COMMENT '申请类消息:PENDING 待处理 / ACCEPTED 已同意 / REJECTED 已拒绝',
    created_at     DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_receiver_id (receiver_id)
) ENGINE = InnoDB COMMENT = '收件箱消息表';

-- ------------------------------------------------------------
-- 11–17. 以下表与 M1 定义一致
-- schedule / chat_message / pomodoro_record / shop_item /
-- pet_inventory / feeding_record / desktop_zone / desktop_organize_record
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schedule (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '归属用户',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    remind_at   DATETIME     DEFAULT NULL COMMENT '提醒时间(右下角推送依据)',
    repeat_type VARCHAR(20)  NOT NULL DEFAULT 'ONCE' COMMENT '重复:ONCE 一次 / DAILY 每天 / WEEKLY 每周',
    priority    TINYINT      NOT NULL DEFAULT 2 COMMENT '优先级:1 高 / 2 中 / 3 低',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING 待办 / DONE 已完成',
    tag         VARCHAR(30)  NOT NULL DEFAULT '其他' COMMENT '标签(学习/工作/生活/健康/其他,可自定义)',
    last_notified_at DATETIME DEFAULT NULL COMMENT '上次提醒时间(去重)',
    completed_at DATETIME    DEFAULT NULL COMMENT '完成时间(统计/工资单用)',
    created_at  DATETIME     NOT NULL COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_remind_at (remind_at)
) ENGINE = InnoDB COMMENT = '日程任务表';

CREATE TABLE IF NOT EXISTS chat_message (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT      NOT NULL COMMENT '归属用户',
    pet_id     BIGINT      DEFAULT NULL COMMENT '宠物 pet.id',
    role       VARCHAR(20) NOT NULL COMMENT '角色:USER 用户 / ASSISTANT 宠物',
    content    TEXT        NOT NULL COMMENT '消息内容',
    is_voice   TINYINT     NOT NULL DEFAULT 0 COMMENT '1 语音来源 / 0 文字',
    provider   VARCHAR(50) DEFAULT NULL COMMENT '本次对话实际使用的服务商',
    created_at DATETIME    NOT NULL COMMENT '创建时间(聊天记录只增不改)',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '聊天记录表';

CREATE TABLE IF NOT EXISTS pomodoro_record (
    id               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          BIGINT   NOT NULL COMMENT '归属用户',
    pet_id           BIGINT   DEFAULT NULL COMMENT '陪伴的宠物',
    started_at       DATETIME NOT NULL COMMENT '开始时间',
    paused_at        DATETIME DEFAULT NULL COMMENT '暂停时间(暂停中非空)',
    paused_count     INT      NOT NULL DEFAULT 0 COMMENT '已暂停次数(每轮最多 2 次)',
    total_paused_minutes INT  NOT NULL DEFAULT 0 COMMENT '累计暂停分钟数',
    ended_at         DATETIME DEFAULT NULL COMMENT '结束时间',
    duration_minutes INT      DEFAULT 0 COMMENT '实际专注分钟数',
    label            VARCHAR(50) DEFAULT NULL COMMENT '番茄钟名称(可选)',
    completed        TINYINT  NOT NULL DEFAULT 0 COMMENT '1 完成一轮 / 0 中途放弃',
    coins_earned     INT      NOT NULL DEFAULT 0 COMMENT '获得游戏币(兑换规则 M7 拍板)',
    created_at       DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '番茄钟记录表';

CREATE TABLE IF NOT EXISTS shop_item (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_code      VARCHAR(30) NOT NULL COMMENT '物品编码,如 FISH / BONE',
    item_name      VARCHAR(30) NOT NULL COMMENT '物品名',
    item_type      VARCHAR(20) NOT NULL DEFAULT 'FOOD' COMMENT '类型:FOOD 食物 / TOY 玩具',
    price          INT         NOT NULL DEFAULT 0 COMMENT '价格(游戏币)',
    hunger_restore INT         NOT NULL DEFAULT 0 COMMENT '恢复饥饿度',
    mood_restore   INT         NOT NULL DEFAULT 0 COMMENT '恢复心情值',
    enabled        TINYINT     NOT NULL DEFAULT 1 COMMENT '1 上架 / 0 下架',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_code (item_code)
) ENGINE = InnoDB COMMENT = '商店物品字典表';

CREATE TABLE IF NOT EXISTS pet_inventory (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    pet_id     BIGINT   NOT NULL COMMENT '宠物 pet.id',
    item_id    BIGINT   NOT NULL COMMENT '物品 shop_item.id',
    quantity   INT      NOT NULL DEFAULT 0 COMMENT '持有数量',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pet_item (pet_id, item_id)
) ENGINE = InnoDB COMMENT = '宠物背包表';

CREATE TABLE IF NOT EXISTS feeding_record (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    pet_id        BIGINT   NOT NULL COMMENT '宠物 pet.id',
    item_id       BIGINT   DEFAULT NULL COMMENT '食物 shop_item.id',
    hunger_before INT      NOT NULL COMMENT '喂食前饥饿度',
    hunger_after  INT      NOT NULL COMMENT '喂食后饥饿度',
    fed_at        DATETIME NOT NULL COMMENT '喂食时间',
    created_at    DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pet_id (pet_id)
) ENGINE = InnoDB COMMENT = '喂食流水表';

CREATE TABLE IF NOT EXISTS desktop_zone (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       NOT NULL COMMENT '归属用户',
    zone_name  VARCHAR(30)  NOT NULL COMMENT '分区名,如 文档 / 图片 / 视频 / 安装包',
    extensions VARCHAR(500) DEFAULT NULL COMMENT '匹配扩展名,逗号分隔;空表示按文件名关键词',
    target_dir VARCHAR(200) NOT NULL COMMENT '目标文件夹名(相对桌面)',
    enabled    TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    created_at DATETIME     NOT NULL COMMENT '创建时间',
    updated_at DATETIME     NOT NULL COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '桌面整理分区规则表';

CREATE TABLE IF NOT EXISTS desktop_organize_record (
    id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT   NOT NULL COMMENT '归属用户',
    zone_id      BIGINT   DEFAULT NULL COMMENT '分区 desktop_zone.id',
    moved_count  INT      NOT NULL DEFAULT 0 COMMENT '本次移动文件数',
    details_json TEXT     DEFAULT NULL COMMENT '移动明细 JSON:[{fileName,from,to}]',
    undone       TINYINT  NOT NULL DEFAULT 0 COMMENT '1 已撤销',
    organized_at DATETIME NOT NULL COMMENT '整理时间',
    created_at   DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '桌面整理流水表';

-- ------------------------------------------------------------
-- 18. 应用使用采样记录(M9;隐私红线:仅进程名/窗口标题,保留 7 天)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_usage_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '归属用户',
    app_name     VARCHAR(100) NOT NULL COMMENT '应用进程名,如 idea64 / chrome',
    window_title VARCHAR(300) DEFAULT NULL COMMENT '窗口标题(可选)',
    sampled_at   DATETIME     NOT NULL COMMENT '采样时间',
    created_at   DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_sampled_at (sampled_at)
) ENGINE = InnoDB COMMENT = '应用使用采样记录(隐私:仅进程名/窗口标题,保留 7 天)';

-- ------------------------------------------------------------
-- 19. 注册邮箱验证码(M10)
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 20. 好友关系(M11;双向各一行)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS friend (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT   NOT NULL COMMENT '本人',
    friend_id  BIGINT   NOT NULL COMMENT '好友',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pair (user_id, friend_id)
) ENGINE = InnoDB COMMENT = '好友关系(双向各一行)';

-- ============================================================
-- 字典初始数据(M3 拍板)
-- ============================================================

-- 13 种宠物:普通=猫狗,稀有=其余 10 生肖,传说=龙;基准饿死天数按体型代谢
INSERT IGNORE INTO pet_type (type_code, type_name, rarity, base_days, description) VALUES
('RAT',     '鼠',   'RARE',   3, '小体型代谢快,最不耐饿'),
('OX',      '牛',   'RARE',   6, '壮实耐饿'),
('TIGER',   '虎',   'RARE',   6, '猛兽,耐饿'),
('RABBIT',  '兔',   'RARE',   5, '温顺'),
('DRAGON',  '龙',   'LEGEND', 7, '传说,最耐饿'),
('SNAKE',   '蛇',   'RARE',   5, '冷血'),
('HORSE',   '马',   'RARE',   6, '耐饿'),
('GOAT',    '羊',   'RARE',   6, '耐饿'),
('MONKEY',  '猴',   'RARE',   5, '机灵'),
('ROOSTER', '鸡',   'RARE',   5, '活泼'),
('DOG',     '狗',   'NORMAL', 5, '普通,出现概率大'),
('PIG',     '猪',   'RARE',   4, '贪吃,不耐饿'),
('CAT',     '猫',   'NORMAL', 4, '普通,出现概率大');

-- 亚种:猫 5、狗 5,其余 11 种各 2
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '布偶猫'   FROM pet_type WHERE type_code='CAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '狸花猫'   FROM pet_type WHERE type_code='CAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '橘猫'     FROM pet_type WHERE type_code='CAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '英短'     FROM pet_type WHERE type_code='CAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '美短'     FROM pet_type WHERE type_code='CAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '金毛'     FROM pet_type WHERE type_code='DOG';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '柯基'     FROM pet_type WHERE type_code='DOG';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '哈士奇'   FROM pet_type WHERE type_code='DOG';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '萨摩耶'   FROM pet_type WHERE type_code='DOG';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '拉布拉多' FROM pet_type WHERE type_code='DOG';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '仓鼠'     FROM pet_type WHERE type_code='RAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '花枝鼠'   FROM pet_type WHERE type_code='RAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '奶牛'     FROM pet_type WHERE type_code='OX';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '水牛'     FROM pet_type WHERE type_code='OX';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '东北虎'   FROM pet_type WHERE type_code='TIGER';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '孟加拉虎' FROM pet_type WHERE type_code='TIGER';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '垂耳兔'   FROM pet_type WHERE type_code='RABBIT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '侏儒兔'   FROM pet_type WHERE type_code='RABBIT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '东方龙'   FROM pet_type WHERE type_code='DRAGON';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '西方龙'   FROM pet_type WHERE type_code='DRAGON';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '竹叶青'   FROM pet_type WHERE type_code='SNAKE';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '眼镜蛇'   FROM pet_type WHERE type_code='SNAKE';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '小马'     FROM pet_type WHERE type_code='HORSE';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '斑马'     FROM pet_type WHERE type_code='HORSE';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '小羊'     FROM pet_type WHERE type_code='GOAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '山羊'     FROM pet_type WHERE type_code='GOAT';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '金丝猴'   FROM pet_type WHERE type_code='MONKEY';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '猕猴'     FROM pet_type WHERE type_code='MONKEY';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '芦花鸡'   FROM pet_type WHERE type_code='ROOSTER';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '珍珠鸡'   FROM pet_type WHERE type_code='ROOSTER';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '粉猪'     FROM pet_type WHERE type_code='PIG';
INSERT IGNORE INTO pet_subtype (pet_type_id, subtype_name) SELECT id, '小香猪'   FROM pet_type WHERE type_code='PIG';

-- 商店初始数据(M7 拍板)
INSERT IGNORE INTO shop_item (item_code, item_name, item_type, price, hunger_restore, mood_restore, enabled) VALUES
('FISH',  '小鱼干',   'FOOD', 5,  20, 0,  1),
('BONE',  '肉骨头',   'FOOD', 10, 40, 0,  1),
('FEAST', '豪华大餐', 'FOOD', 20, 80, 10, 1),
('BALL',  '玩具球',   'TOY',  15, 0,  20, 1);

-- 修复:种子 INSERT 未填时间戳,老库可能存了 0000-00-00,补成当前时间(幂等,可安全重跑)
UPDATE pet_type   SET created_at = NOW(), updated_at = NOW() WHERE created_at < '2000-01-01';
UPDATE pet_subtype SET created_at = NOW(), updated_at = NOW() WHERE created_at < '2000-01-01';
UPDATE shop_item  SET created_at = NOW(), updated_at = NOW() WHERE created_at < '2000-01-01';
