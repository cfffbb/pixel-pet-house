-- ============================================================
-- 增量升级脚本:M9(应用使用采样记录表)
-- 隐私红线:仅记录进程名/窗口标题,不采集内容;记录保留 7 天
-- 用法:mysql -u root -p pet_ai < migration_m9.sql
-- ============================================================

USE pet_ai;

CREATE TABLE IF NOT EXISTS app_usage_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '归属用户',
    app_name     VARCHAR(100) NOT NULL COMMENT '应用进程名,如 idea64 / chrome',
    window_title VARCHAR(300) DEFAULT NULL COMMENT '窗口标题(可选,只存不展示内容分析)',
    sampled_at   DATETIME     NOT NULL COMMENT '采样时间',
    created_at   DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_sampled_at (sampled_at)
) ENGINE = InnoDB COMMENT = '应用使用采样记录(隐私:仅进程名/窗口标题,保留 7 天)';
