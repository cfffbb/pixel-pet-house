-- ============================================================
-- 增量升级脚本:M11(好友系统)
-- 用法:mysql -u root -p pet_ai < migration_m11.sql
-- ============================================================

USE pet_ai;

CREATE TABLE IF NOT EXISTS friend (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT   NOT NULL COMMENT '本人',
    friend_id  BIGINT   NOT NULL COMMENT '好友',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pair (user_id, friend_id)
) ENGINE = InnoDB COMMENT = '好友关系(双向各一行)';
