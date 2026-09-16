-- ============================================================
-- 增量升级脚本:M1/M2 已建库的老用户,升级到 M3/M3.5 结构
-- 新用户直接执行 schema.sql 即可,不需要本文件。
-- 用法:mysql -u root -p pet_ai < migration_m3.sql
-- ============================================================

USE pet_ai;

-- 1. pet 表新增字段(性别/亚种/病危/怀孕/配种冷却)
ALTER TABLE pet
    ADD COLUMN gender              VARCHAR(10) DEFAULT 'MALE'  COMMENT '性别:MALE 公 / FEMALE 母' AFTER subtype_name,
    ADD COLUMN subtype_name        VARCHAR(30) DEFAULT NULL    COMMENT '亚种名(仅展示)' AFTER pet_name,
    ADD COLUMN hunger_calc_at      DATETIME    DEFAULT NULL    COMMENT '上次结算饥饿的时间' AFTER last_fed_at,
    ADD COLUMN danger_since        DATETIME    DEFAULT NULL    COMMENT '进入病危的时间' AFTER hunger_calc_at,
    ADD COLUMN pregnant_until      DATETIME    DEFAULT NULL    COMMENT '怀孕截止时间' AFTER danger_since,
    ADD COLUMN breeding_cool_until DATETIME    DEFAULT NULL    COMMENT '配种冷却截止时间' AFTER pregnant_until;

-- 2. pet_type 表新增基准饿死天数
ALTER TABLE pet_type
    ADD COLUMN base_days INT NOT NULL DEFAULT 5 COMMENT '基准饿死天数(3–7)' AFTER rarity;

-- 3. 新表:亚种字典 / 墓碑 / 幼崽托管 / 配种记录 / 收件箱
CREATE TABLE IF NOT EXISTS pet_subtype (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    pet_type_id  BIGINT      NOT NULL COMMENT '所属种类 pet_type.id',
    subtype_name VARCHAR(30) NOT NULL COMMENT '亚种名,如 布偶猫',
    enabled      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    created_at   DATETIME    NOT NULL COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_pet_type_id (pet_type_id)
) ENGINE = InnoDB COMMENT = '宠物亚种字典表';

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

CREATE TABLE IF NOT EXISTS pet_hatchling (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT      NOT NULL COMMENT '归属用户(出生在母方托管所)',
    mother_pet_id BIGINT      NOT NULL COMMENT '母方宠物 pet.id',
    father_pet_id BIGINT      DEFAULT NULL COMMENT '父方宠物 pet.id',
    pet_type_id   BIGINT      NOT NULL COMMENT '种类 id(50/50 取父母)',
    subtype_name  VARCHAR(30) DEFAULT NULL COMMENT '亚种',
    gender        VARCHAR(10) NOT NULL DEFAULT 'MALE' COMMENT '性别',
    personality   VARCHAR(30) DEFAULT NULL COMMENT '性格',
    rarity        VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '品质(受父母影响)',
    status        VARCHAR(20) NOT NULL DEFAULT 'STORED' COMMENT 'STORED 托管中 / FROZEN 欠费冻结 / CLAIMED 已领取',
    stored_at     DATETIME    NOT NULL COMMENT '入托时间(托管费计费起点)',
    created_at    DATETIME    NOT NULL COMMENT '创建时间',
    updated_at    DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB COMMENT = '幼崽托管表';

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

-- 4. 字典数据
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
