-- ============================================================
-- 增量升级脚本:M4(玩耍/行为事件字段)
-- 用法:mysql -u root -p pet_ai < migration_m4.sql
-- ============================================================

USE pet_ai;

ALTER TABLE pet
    ADD COLUMN play_date     DATE NULL COMMENT '玩耍日期(每日重置)' AFTER breeding_cool_until,
    ADD COLUMN play_count    INT  NOT NULL DEFAULT 0 COMMENT '今日玩耍次数' AFTER play_date,
    ADD COLUMN behavior_date DATE NULL COMMENT '行为事件发生日期(每天最多 1 次)' AFTER play_count;
