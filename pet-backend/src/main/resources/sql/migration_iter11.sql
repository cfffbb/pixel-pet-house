-- Iter-11: 喂养系统增强 - 食物分类 + 挑食机制

-- 1. shop_item 增加 food_category 字段
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='pet_ai' AND TABLE_NAME='shop_item' AND COLUMN_NAME='food_category');
SET @sql = IF(@c=0, 'ALTER TABLE shop_item ADD COLUMN food_category VARCHAR(20) DEFAULT ''STAPLE'' COMMENT ''食物分类:STAPLE主食/SNACK零食/MEDICINE药品/SPECIAL特殊''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 更新现有食物的分类
UPDATE shop_item SET food_category='STAPLE' WHERE item_code IN ('FISH','RICE','MEAT','BONE','CARROT','BREAD');
UPDATE shop_item SET food_category='SNACK' WHERE item_code IN ('COOKIE','CAKE','CANDY','JERKY','CHEESE');
UPDATE shop_item SET food_category='MEDICINE' WHERE item_code IN ('POTION','HERB','VITAMIN');
UPDATE shop_item SET food_category='SPECIAL' WHERE item_code IN ('GOLDEN_FISH','CRYSTAL_FRUIT','RAINBOW_CAKE','LUCKY_CLOVER');

-- 3. 确保所有食物都有分类(未命中的设为主食)
UPDATE shop_item SET food_category='STAPLE' WHERE item_type='FOOD' AND (food_category IS NULL OR food_category='');
