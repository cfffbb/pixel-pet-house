-- Iter-12: 喂养系统大升级 - 连续喂食/幸运事件/多样性/经验

-- 1. pet 表增加连续喂食天数和上次喂食日期
SET @s1 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='pet' AND COLUMN_NAME='feeding_streak');
SET @sql1 = IF(@s1=0, 'ALTER TABLE pet ADD COLUMN feeding_streak INT DEFAULT 0 COMMENT ''连续喂食天数''', 'SELECT 1');
PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

SET @s2 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='pet' AND COLUMN_NAME='last_feed_date');
SET @sql2 = IF(@s2=0, 'ALTER TABLE pet ADD COLUMN last_feed_date VARCHAR(10) DEFAULT NULL COMMENT ''上次喂食日期YYYY-MM-DD''', 'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- 2. feeding_record 增加心情变化、幸运事件、经验等字段
SET @s3 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='feeding_record' AND COLUMN_NAME='mood_before');
SET @sql3 = IF(@s3=0, 'ALTER TABLE feeding_record ADD COLUMN mood_before INT DEFAULT NULL COMMENT ''喂食前心情''', 'SELECT 1');
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;

SET @s4 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='feeding_record' AND COLUMN_NAME='mood_after');
SET @sql4 = IF(@s4=0, 'ALTER TABLE feeding_record ADD COLUMN mood_after INT DEFAULT NULL COMMENT ''喂食后心情''', 'SELECT 1');
PREPARE stmt4 FROM @sql4; EXECUTE stmt4; DEALLOCATE PREPARE stmt4;

SET @s5 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='feeding_record' AND COLUMN_NAME='lucky_event');
SET @sql5 = IF(@s5=0, 'ALTER TABLE feeding_record ADD COLUMN lucky_event VARCHAR(50) DEFAULT NULL COMMENT ''幸运事件类型''', 'SELECT 1');
PREPARE stmt5 FROM @sql5; EXECUTE stmt5; DEALLOCATE PREPARE stmt5;

SET @s6 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='feeding_record' AND COLUMN_NAME='exp_gain');
SET @sql6 = IF(@s6=0, 'ALTER TABLE feeding_record ADD COLUMN exp_gain INT DEFAULT 0 COMMENT ''获得经验''', 'SELECT 1');
PREPARE stmt6 FROM @sql6; EXECUTE stmt6; DEALLOCATE PREPARE stmt6;

SET @s7 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='feeding_record' AND COLUMN_NAME='item_name');
SET @sql7 = IF(@s7=0, 'ALTER TABLE feeding_record ADD COLUMN item_name VARCHAR(100) DEFAULT NULL COMMENT ''食物名称(冗余)''', 'SELECT 1');
PREPARE stmt7 FROM @sql7; EXECUTE stmt7; DEALLOCATE PREPARE stmt7;

SET @s8 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='feeding_record' AND COLUMN_NAME='preference');
SET @sql8 = IF(@s8=0, 'ALTER TABLE feeding_record ADD COLUMN preference VARCHAR(20) DEFAULT NULL COMMENT ''NORMAL/LIKE/DISLIKE''', 'SELECT 1');
PREPARE stmt8 FROM @sql8; EXECUTE stmt8; DEALLOCATE PREPARE stmt8;
