-- ============================================================
-- 老库时间戳修复脚本(已按 M1→M9 迁移建库的用户执行一次)
-- 原因:种子字典数据未填 created_at/updated_at,非严格模式存了
--       0000-00-00,驱动读取报 "Zero date value prohibited"。
-- 新库直接跑 schema.sql 已含 DEFAULT CURRENT_TIMESTAMP,无需本文件。
-- 用法:mysql -u root -p pet_ai < fix_timestamps.sql
-- ============================================================

USE pet_ai;

ALTER TABLE pet_type    MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE pet_type    MODIFY updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE pet_subtype MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE pet_subtype MODIFY updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
ALTER TABLE shop_item   MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE shop_item   MODIFY updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

UPDATE pet_type    SET created_at = NOW(), updated_at = NOW() WHERE created_at < '2000-01-01';
UPDATE pet_subtype SET created_at = NOW(), updated_at = NOW() WHERE created_at < '2000-01-01';
UPDATE shop_item   SET created_at = NOW(), updated_at = NOW() WHERE created_at < '2000-01-01';
