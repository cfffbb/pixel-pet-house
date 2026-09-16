-- ============================================================
-- Iter-09: 后院繁育系统（单机版）
-- 1) pet 表增加 is_npc 字段（标记系统NPC宠物）
-- 2) 插入一批NPC宠物作为配种候选（user_id=0）
-- ============================================================

USE pet_ai;

-- 1) 给 pet 表增加 is_npc 字段
ALTER TABLE pet ADD COLUMN is_npc TINYINT NOT NULL DEFAULT 0 COMMENT '0 普通宠物 / 1 系统NPC' AFTER listed;

-- 2) 插入NPC候选宠物（覆盖13种，公母各一只，共26只）
-- user_id=0 表示系统NPC；listed=1 上架；status=ALIVE；成熟度直接满足
INSERT INTO pet (user_id, pet_type_id, pet_name, subtype_name, gender, personality, level, exp, hunger, mood, coins, status, listed, is_npc, hatched_at, hunger_calc_at, created_at, updated_at)
SELECT 0, t.id, CONCAT(t.type_name, 'NPC'), '野生', 'MALE', '活泼', 5, 0, 100, 100, 999, 'ALIVE', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW(), NOW(), NOW()
FROM pet_type t WHERE t.enabled = 1 AND t.deleted = 0
AND NOT EXISTS (SELECT 1 FROM pet p WHERE p.is_npc = 1 AND p.pet_type_id = t.id AND p.gender = 'MALE');

INSERT INTO pet (user_id, pet_type_id, pet_name, subtype_name, gender, personality, level, exp, hunger, mood, coins, status, listed, is_npc, hatched_at, hunger_calc_at, created_at, updated_at)
SELECT 0, t.id, CONCAT(t.type_name, 'NPC'), '野生', 'FEMALE', '粘人', 5, 0, 100, 100, 999, 'ALIVE', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW(), NOW(), NOW()
FROM pet_type t WHERE t.enabled = 1 AND t.deleted = 0
AND NOT EXISTS (SELECT 1 FROM pet p WHERE p.is_npc = 1 AND p.pet_type_id = t.id AND p.gender = 'FEMALE');
