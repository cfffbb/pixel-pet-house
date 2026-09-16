-- ============================================================
-- 增量升级脚本:M6(API Key 增加模型名字段)
-- 用法:mysql -u root -p pet_ai < migration_m6.sql
-- ============================================================

USE pet_ai;

ALTER TABLE user_api_key
    ADD COLUMN model_name VARCHAR(100) DEFAULT NULL COMMENT '对话模型名,如 gpt-4o-mini / qwen-plus' AFTER base_url;
