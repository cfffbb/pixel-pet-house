-- Iter-10: 番茄钟任务管理 + pomodoro_record 增加字段

-- 1. 新建任务表
CREATE TABLE IF NOT EXISTS pomodoro_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    tag VARCHAR(50) DEFAULT NULL COMMENT '标签',
    estimate_count INT DEFAULT 1 COMMENT '预估番茄数',
    done_count INT DEFAULT 0 COMMENT '已完成番茄数',
    status INT DEFAULT 0 COMMENT '0待办 1已完成',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    plan_date VARCHAR(10) DEFAULT NULL COMMENT '计划日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_date (user_id, plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='番茄钟任务';

-- 2. pomodoro_record 增加缺失的字段
SET @c1 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='pomodoro_record' AND COLUMN_NAME='label');
SET @sql1 = IF(@c1=0, 'ALTER TABLE pomodoro_record ADD COLUMN label VARCHAR(200) DEFAULT NULL COMMENT ''番茄钟名称''', 'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @c2 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='pomodoro_record' AND COLUMN_NAME='task_id');
SET @sql2 = IF(@c2=0, 'ALTER TABLE pomodoro_record ADD COLUMN task_id BIGINT DEFAULT NULL COMMENT ''关联任务ID''', 'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- 3. 插入示例任务
INSERT INTO pomodoro_task (user_id, title, tag, estimate_count, done_count, status, sort_order, plan_date)
SELECT id, '复习高等数学第三章', '学习', 3, 0, 0, 1, CURDATE() FROM sys_user WHERE deleted=0 LIMIT 1;
INSERT INTO pomodoro_task (user_id, title, tag, estimate_count, done_count, status, sort_order, plan_date)
SELECT id, '写毕业设计论文', '学习', 4, 0, 0, 2, CURDATE() FROM sys_user WHERE deleted=0 LIMIT 1;
INSERT INTO pomodoro_task (user_id, title, tag, estimate_count, done_count, status, sort_order)
SELECT id, '跑步30分钟', '运动', 1, 0, 0, 3 FROM sys_user WHERE deleted=0 LIMIT 1;
