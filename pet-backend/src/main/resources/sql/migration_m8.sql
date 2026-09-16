-- ============================================================
-- 增量升级脚本:M8(整理记录加明细/撤销标记)
-- 用法:mysql -u root -p pet_ai < migration_m8.sql
-- ============================================================

USE pet_ai;

ALTER TABLE desktop_organize_record
    ADD COLUMN details_json TEXT      DEFAULT NULL COMMENT '移动明细 JSON:[{fileName,from,to}]' AFTER moved_count,
    ADD COLUMN undone       TINYINT   NOT NULL DEFAULT 0 COMMENT '1 已撤销' AFTER details_json;
