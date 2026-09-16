-- ============================================================
-- 增量升级脚本:M7(番茄钟暂停字段 + 商店物品初始数据)
-- 用法:mysql -u root -p pet_ai < migration_m7.sql
-- ============================================================

USE pet_ai;

ALTER TABLE pomodoro_record
    ADD COLUMN paused_at            DATETIME DEFAULT NULL COMMENT '暂停时间(暂停中非空)' AFTER started_at,
    ADD COLUMN paused_count         INT      NOT NULL DEFAULT 0 COMMENT '已暂停次数(每轮最多 2 次)' AFTER paused_at,
    ADD COLUMN total_paused_minutes INT      NOT NULL DEFAULT 0 COMMENT '累计暂停分钟数' AFTER paused_count;

-- 商店初始数据(M7 拍板)
INSERT IGNORE INTO shop_item (item_code, item_name, item_type, price, hunger_restore, mood_restore, enabled) VALUES
('FISH',  '小鱼干',   'FOOD', 5,  20, 0,  1),
('BONE',  '肉骨头',   'FOOD', 10, 40, 0,  1),
('FEAST', '豪华大餐', 'FOOD', 20, 80, 10, 1),
('BALL',  '玩具球',   'TOY',  15, 0,  20, 1);
