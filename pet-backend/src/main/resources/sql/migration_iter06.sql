-- 迭代6:番茄钟记录表增加 label 字段(番茄钟名称)
ALTER TABLE pomodoro_record ADD COLUMN label VARCHAR(50) DEFAULT NULL COMMENT '番茄钟名称(可选)' AFTER duration_minutes;
