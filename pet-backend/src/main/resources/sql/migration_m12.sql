-- ============================================================
-- 增量升级脚本:M12(测试账号/宠物上架字段/按物种分级食物)
-- 默认值(可改):成熟期=出生 3 天;测试账号密码统一 12345678
-- 用法:mysql -u root -p pet_ai < migration_m12.sql
-- ============================================================

USE pet_ai;

-- 1. 字段
ALTER TABLE shop_item
    ADD COLUMN species   VARCHAR(30) DEFAULT NULL COMMENT '适用种类 type_code,空=通用' AFTER item_type,
    ADD COLUMN min_level INT         NOT NULL DEFAULT 1 COMMENT '解锁等级' AFTER mood_restore;
ALTER TABLE pet
    ADD COLUMN listed TINYINT NOT NULL DEFAULT 0 COMMENT '1 已上架配种市场(需雄/活着/成熟3天/非冷却)' AFTER status;

-- 2. 测试账号(密码统一 12345678)
INSERT IGNORE INTO sys_user (username, password, nickname, email, role, status, created_at, updated_at, deleted) VALUES
('admin1', '$2a$10$bbb5zpigxlVYCiz/Tt7BkeHAm.FHGPfP7YNkOtKDi4dLPZtUehzza', '管理员一', 'admin1@test.local', 'ADMIN', 1, NOW(), NOW(), 0),
('admin2', '$2a$10$bbb5zpigxlVYCiz/Tt7BkeHAm.FHGPfP7YNkOtKDi4dLPZtUehzza', '管理员二', 'admin2@test.local', 'ADMIN', 1, NOW(), NOW(), 0),
('tester1', '$2a$10$bbb5zpigxlVYCiz/Tt7BkeHAm.FHGPfP7YNkOtKDi4dLPZtUehzza', '测试员1', 'tester1@test.local', 'USER', 1, NOW(), NOW(), 0),
('tester2', '$2a$10$bbb5zpigxlVYCiz/Tt7BkeHAm.FHGPfP7YNkOtKDi4dLPZtUehzza', '测试员2', 'tester2@test.local', 'USER', 1, NOW(), NOW(), 0),
('tester3', '$2a$10$bbb5zpigxlVYCiz/Tt7BkeHAm.FHGPfP7YNkOtKDi4dLPZtUehzza', '测试员3', 'tester3@test.local', 'USER', 1, NOW(), NOW(), 0);

-- 3. 测试宠物(出生 4 天前 → 已成熟;管理员宠物币 99999)
INSERT INTO pet (user_id, pet_type_id, pet_name, subtype_name, gender, personality, level, exp, hunger, mood, coins, status, hatched_at, hunger_calc_at, listed, created_at, updated_at, deleted)
SELECT u.id, t.id, '大虎', '东北虎', 'MALE', '高冷', 11, 12000, 100, 100, 99999, 'ALIVE', NOW() - INTERVAL 4 DAY, NOW(), 0, NOW(), NOW(), 0
FROM sys_user u, pet_type t WHERE u.username = 'admin1' AND t.type_code = 'TIGER';
INSERT INTO pet (user_id, pet_type_id, pet_name, subtype_name, gender, personality, level, exp, hunger, mood, coins, status, hatched_at, hunger_calc_at, listed, created_at, updated_at, deleted)
SELECT u.id, t.id, '小云', '东方龙', 'FEMALE', '傲娇', 6, 6000, 100, 100, 99999, 'ALIVE', NOW() - INTERVAL 4 DAY, NOW(), 0, NOW(), NOW(), 0
FROM sys_user u, pet_type t WHERE u.username = 'admin2' AND t.type_code = 'DRAGON';
INSERT INTO pet (user_id, pet_type_id, pet_name, subtype_name, gender, personality, level, exp, hunger, mood, coins, status, hatched_at, hunger_calc_at, listed, created_at, updated_at, deleted)
SELECT u.id, t.id, '旺财', '金毛', 'MALE', '活泼', 4, 3600, 100, 100, 200, 'ALIVE', NOW() - INTERVAL 4 DAY, NOW(), 1, NOW(), NOW(), 0
FROM sys_user u, pet_type t WHERE u.username = 'tester1' AND t.type_code = 'DOG';
INSERT INTO pet (user_id, pet_type_id, pet_name, subtype_name, gender, personality, level, exp, hunger, mood, coins, status, hatched_at, hunger_calc_at, listed, created_at, updated_at, deleted)
SELECT u.id, t.id, '咪咪', '布偶猫', 'FEMALE', '粘人', 3, 2000, 100, 100, 200, 'ALIVE', NOW() - INTERVAL 4 DAY, NOW(), 0, NOW(), NOW(), 0
FROM sys_user u, pet_type t WHERE u.username = 'tester2' AND t.type_code = 'CAT';
INSERT INTO pet (user_id, pet_type_id, pet_name, subtype_name, gender, personality, level, exp, hunger, mood, coins, status, hatched_at, hunger_calc_at, listed, created_at, updated_at, deleted)
SELECT u.id, t.id, '雪球', '垂耳兔', 'MALE', '贪吃', 2, 1000, 100, 100, 200, 'ALIVE', NOW() - INTERVAL 4 DAY, NOW(), 0, NOW(), NOW(), 0
FROM sys_user u, pet_type t WHERE u.username = 'tester3' AND t.type_code = 'RABBIT';

-- 4. 测试宠物背包:小鱼干 ×10
INSERT INTO pet_inventory (pet_id, item_id, quantity, created_at, updated_at)
SELECT p.id, s.id, 10, NOW(), NOW() FROM pet p, shop_item s
WHERE s.item_code = 'FISH' AND p.user_id = (SELECT id FROM sys_user WHERE username = 'tester1');
INSERT INTO pet_inventory (pet_id, item_id, quantity, created_at, updated_at)
SELECT p.id, s.id, 10, NOW(), NOW() FROM pet p, shop_item s
WHERE s.item_code = 'FISH' AND p.user_id = (SELECT id FROM sys_user WHERE username = 'tester2');
INSERT INTO pet_inventory (pet_id, item_id, quantity, created_at, updated_at)
SELECT p.id, s.id, 10, NOW(), NOW() FROM pet p, shop_item s
WHERE s.item_code = 'FISH' AND p.user_id = (SELECT id FROM sys_user WHERE username = 'tester3');

-- 5. 商店扩展:通用分级食物 + 按物种专属食物
INSERT IGNORE INTO shop_item (item_code, item_name, item_type, species, price, hunger_restore, mood_restore, min_level, enabled, created_at, updated_at) VALUES
('STEAK',   '牛排',     'FOOD', NULL, 15, 50, 0,  8,  1, NOW(), NOW()),
('SALMON',  '三文鱼',   'FOOD', NULL, 10, 35, 0,  5,  1, NOW(), NOW()),
('FRUIT2',  '龙晶果',   'FOOD', NULL, 30, 90, 20, 15, 1, NOW(), NOW()),
('SEED',    '瓜子',     'FOOD', 'RAT',     5, 25, 0, 1, 1, NOW(), NOW()),
('CARROT',  '胡萝卜',   'FOOD', 'RABBIT',  5, 25, 0, 1, 1, NOW(), NOW()),
('GRASS',   '牧草团',   'FOOD', 'OX',      5, 30, 0, 1, 1, NOW(), NOW()),
('MEAT',    '鲜肉大餐', 'FOOD', 'TIGER',  20, 60, 5, 5, 1, NOW(), NOW()),
('FRUIT3',  '龙果',     'FOOD', 'DRAGON', 25, 70, 10, 10, 1, NOW(), NOW()),
('MOUSE',   '小老鼠',   'FOOD', 'SNAKE',   5, 25, 0, 1, 1, NOW(), NOW()),
('APPLE',   '苹果',     'FOOD', 'HORSE',   5, 25, 0, 1, 1, NOW(), NOW()),
('CAKE2',   '青草饼',   'FOOD', 'GOAT',    5, 25, 0, 1, 1, NOW(), NOW()),
('BANANA',  '香蕉',     'FOOD', 'MONKEY',  5, 25, 0, 1, 1, NOW(), NOW()),
('BUG',     '虫子串',   'FOOD', 'ROOSTER', 5, 25, 0, 1, 1, NOW(), NOW()),
('BONE2',   '大棒骨',   'FOOD', 'DOG',     8, 40, 0, 3, 1, NOW(), NOW()),
('PUMPKIN', '南瓜',     'FOOD', 'PIG',     5, 30, 0, 1, 1, NOW(), NOW()),
('CANTEEN', '猫罐头',   'FOOD', 'CAT',     8, 35, 0, 4, 1, NOW(), NOW());
