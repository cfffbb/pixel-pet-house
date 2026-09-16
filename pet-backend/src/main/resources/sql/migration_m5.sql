-- ============================================================
-- 增量升级脚本:M5(日程标签/提醒去重/完成时间)
-- 用法:mysql -u root -p pet_ai < migration_m5.sql
-- ============================================================

USE pet_ai;

ALTER TABLE schedule
    ADD COLUMN tag              VARCHAR(30) DEFAULT '其他' COMMENT '标签(学习/工作/生活/健康/其他,可自定义)' AFTER status,
    ADD COLUMN last_notified_at DATETIME    DEFAULT NULL COMMENT '上次提醒时间(去重)' AFTER tag,
    ADD COLUMN completed_at     DATETIME    DEFAULT NULL COMMENT '完成时间(统计/工资单用)' AFTER last_notified_at;
