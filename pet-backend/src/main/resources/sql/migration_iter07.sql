-- Iter-07: 扩展气泡提示库(12 生肖全种类覆盖) + 社交模块增强

-- ========== 1. 气泡提示库:为所有 12 生肖种类添加台词 ==========
INSERT INTO bubble_prompt (species, personality, gender, stage, category, content) VALUES
-- 鼠 (RAT)
('RAT', '活泼', NULL, NULL, 'greeting', '吱吱!主人来啦,我藏了好吃的要不要看?'),
('RAT', '贪吃', NULL, NULL, 'play', '吱~来捉迷藏吧,我钻洞可快了!'),
('RAT', '高冷', NULL, NULL, 'random', '别小看我,十二生肖我可是排第一的'),
('RAT', NULL, NULL, NULL, 'hungry', '吱吱…存粮吃完了,主人快补货!'),
('RAT', NULL, NULL, NULL, 'happy', '吱!今天也是元气满满的小老鼠!'),
-- 牛 (OX)
('OX', '粘人', NULL, NULL, 'greeting', '哞~主人好,我帮你看着家呢'),
('OX', '贪吃', NULL, NULL, 'play', '哞~虽然我慢,但陪你散步没问题!'),
('OX', NULL, NULL, NULL, 'random', '踏踏实实过日子,一步一个脚印哞'),
('OX', NULL, NULL, NULL, 'sleepy', '哞…困了,趴一会儿不打紧吧?'),
('OX', NULL, NULL, NULL, 'study', '主人加油,我默默在旁边给你加油哞'),
-- 虎 (TIGER)
('TIGER', '活泼', 'MALE', NULL, 'greeting', '嗷呜!本大王等你很久了!'),
('TIGER', '傲娇', NULL, NULL, 'play', '哼,才不是想陪你玩呢…嗷!'),
('TIGER', '高冷', NULL, NULL, 'random', '百兽之王也需要午睡,别打扰我嗷'),
('TIGER', NULL, NULL, NULL, 'happy', '嗷呜~心情大好,今天hunt必定成功!'),
('TIGER', NULL, NULL, NULL, 'hungry', '嗷…大王的肚子在叫了,快上肉!'),
-- 兔 (RABBIT)
('RABBIT', '胆小', NULL, NULL, 'greeting', '主人来了?我…我躲在角落等你哦'),
('RABBIT', '活泼', NULL, NULL, 'play', '蹦蹦跳跳!来追我呀~'),
('RABBIT', '粘人', 'FEMALE', NULL, 'random', '蹭蹭~主人身上暖暖的好舒服'),
('RABBIT', NULL, NULL, NULL, 'hungry', '咕咕…有胡萝卜吗?求你了!'),
('RABBIT', NULL, NULL, NULL, 'sleepy', '兔兔要睡了…主人晚安,不要吵我哦'),
-- 龙 (DRAGON)
('DRAGON', '傲娇', NULL, NULL, 'greeting', '哼,凡人,你终于来觐见了?'),
('DRAGON', '活泼', NULL, NULL, 'play', '嗷~翱翔天际!主人要一起来飞吗?'),
('DRAGON', NULL, NULL, NULL, 'random', '身为传说之龙,当然要有排面!'),
('DRAGON', NULL, NULL, NULL, 'happy', '龙颜大悦!今日赏你金币一枚!'),
('DRAGON', NULL, NULL, NULL, 'study', '主人专注的样子,龙龙也觉得很帅气!'),
-- 蛇 (SNAKE)
('SNAKE', '高冷', NULL, NULL, 'greeting', '嘶…你来了,我盘好了,坐吧'),
('SNAKE', '粘人', NULL, NULL, 'play', '嘶~缠绕着你,不要走开嘛'),
('SNAKE', NULL, NULL, NULL, 'random', '冷血动物也有热心肠,只是不太会表达嘶'),
('SNAKE', NULL, NULL, NULL, 'hungry', '嘶…肚子扁了,主人给我弄点吃的吧'),
('SNAKE', NULL, NULL, NULL, 'sleepy', '嘶…冬眠模式启动中…请勿打扰'),
-- 马 (HORSE)
('HORSE', '活泼', NULL, NULL, 'greeting', '咴咴!主人来了,一起去草原跑两圈!'),
('HORSE', '忠诚', NULL, NULL, 'play', '咴!骑着我兜风,天下第一帅!'),
('HORSE', NULL, NULL, NULL, 'random', '马不停蹄,主人也要加油咴!'),
('HORSE', NULL, NULL, NULL, 'happy', '咴咴~今天跑了个痛快,爽!'),
('HORSE', NULL, NULL, NULL, 'study', '主人专注学习,我安静站着陪你咴'),
-- 羊 (GOAT)
('GOAT', '温柔', NULL, NULL, 'greeting', '咩~主人来啦,羊毛暖暖的要不要摸?'),
('GOAT', '贪吃', NULL, NULL, 'play', '咩~来草地一起吃草聊天吧!'),
('GOAT', NULL, NULL, NULL, 'random', '咩~今天天气真好,适合发呆'),
('GOAT', NULL, NULL, NULL, 'hungry', '咩咩…草吃完了,主人买点草饼吧?'),
('GOAT', NULL, NULL, NULL, 'sleepy', '咩~数羊数着数着自己就困了…'),
-- 猴 (MONKEY)
('MONKEY', '活泼', NULL, NULL, 'greeting', '叽叽!主人来啦,看我翻跟头!'),
('MONKEY', '贪吃', NULL, NULL, 'play', '叽!来比爬树,你肯定输给我!'),
('MONKEY', NULL, NULL, NULL, 'random', '叽叽喳喳~今天又是上蹿下跳的一天!'),
('MONKEY', NULL, NULL, NULL, 'happy', '叽!开心到转圈圈,香蕉在哪?'),
('MONKEY', NULL, NULL, NULL, 'play', '丢香蕉皮?不不不,我来接!'),
-- 鸡 (ROOSTER)
('ROOSTER', '活泼', 'MALE', NULL, 'greeting', '喔喔喔!主人早安,我帮你打鸣了!'),
('ROOSTER', '傲娇', NULL, NULL, 'play', '咯咯~别碰我的鸡冠,帅着呢!'),
('ROOSTER', NULL, NULL, NULL, 'random', '咯咯哒~早起的小鸡有虫吃!'),
('ROOSTER', NULL, NULL, NULL, 'hungry', '咯咯…谷粒吃完了,主人快补!'),
('ROOSTER', NULL, NULL, NULL, 'study', '喔~主人学习,我帮你计时打鸣!'),
-- 狗 (DOG) - 补充更多
('DOG', '忠诚', NULL, NULL, 'greeting', '汪汪汪!主人回来啦!想死你了!'),
('DOG', '活泼', 'MALE', NULL, 'play', '汪!丢球丢球!我捡回来给你!'),
('DOG', '贪吃', NULL, NULL, 'hungry', '汪…骨头骨头骨头,给我骨头!'),
('DOG', '粘人', 'FEMALE', NULL, 'happy', '汪~趴在主人脚边最安心了'),
('DOG', NULL, NULL, NULL, 'sleepy', '汪…打了个哈欠,主人也休息吧'),
-- 猪 (PIG)
('PIG', '贪吃', NULL, NULL, 'greeting', '哼哼~主人来了,带吃的了吗?'),
('PIG', '贪吃', NULL, NULL, 'play', '哼哼!来比赛吃东西,我从来没输过!'),
('PIG', '活泼', NULL, NULL, 'random', '哼哼~吃饱了睡,睡醒了吃,猪生圆满!'),
('PIG', NULL, NULL, NULL, 'hungry', '哼哼哼…饿了饿了,快喂我!'),
('PIG', NULL, NULL, NULL, 'happy', '哼~今天吃了三顿,超开心!'),
-- 猫 (CAT) - 补充更多
('CAT', '高冷', NULL, NULL, 'greeting', '喵~你来了?我刚好不忙,勉强理你一下'),
('CAT', '活泼', NULL, NULL, 'play', '喵!逗猫棒呢?快来逗我!'),
('CAT', '贪吃', NULL, NULL, 'hungry', '喵呜…罐头呢?小鱼干呢?快上供!'),
('CAT', '傲娇', 'FEMALE', NULL, 'happy', '喵~今天心情不错,允许你摸一下'),
('CAT', '粘人', NULL, NULL, 'sleepy', '喵…趴在你腿上打呼噜,别动哦'),
-- 通用补充
(NULL, NULL, NULL, NULL, 'play', '好开心呀!再来一次!'),
(NULL, NULL, NULL, NULL, 'play', '和主人一起玩最棒了!'),
(NULL, NULL, NULL, NULL, 'play', '还不过瘾!继续继续!'),
(NULL, NULL, NULL, NULL, 'happy', '元气满满!今天也是快乐的一天!'),
(NULL, NULL, NULL, NULL, 'hungry', '咕…肚子抗议了,主人投喂一下吧?'),
(NULL, NULL, NULL, NULL, 'study', '番茄钟加油!我在旁边给你加油呢!'),
(NULL, NULL, NULL, NULL, 'study', '专注中的主人最帅气了!'),
(NULL, NULL, NULL, NULL, 'sad', '别难过啦,我一直在你身边呢'),
(NULL, NULL, NULL, NULL, 'greeting', '欢迎回来!我等你好久了~')
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- ========== 2. 社交模块增强 ==========

-- 2.1 sys_user 增加 player_id 字段(玩家唯一ID,便于搜索)
ALTER TABLE sys_user ADD COLUMN player_id VARCHAR(20) DEFAULT NULL COMMENT '玩家ID(如P10001)' AFTER nickname;
-- 为已有用户生成 player_id
UPDATE sys_user SET player_id = CONCAT('P', LPAD(id, 5, '0')) WHERE player_id IS NULL;
-- 添加唯一索引
ALTER TABLE sys_user ADD UNIQUE INDEX uk_player_id (player_id);

-- 2.2 friend 表增加 is_official 字段(官方好友标记,不可删除)
ALTER TABLE friend ADD COLUMN is_official TINYINT NOT NULL DEFAULT 0 COMMENT '0=普通好友 1=官方好友(不可删除)' AFTER muted;

-- 2.3 自动为所有已有用户添加官方好友(指向 id=1 的管理员账号)
INSERT IGNORE INTO friend (user_id, friend_id, muted, is_official)
SELECT u.id, 1, 0, 1 FROM sys_user u WHERE u.id != 1 AND u.deleted = 0;

-- 2.4 inbox_message 增加 is_official 标记(官方推送消息)
ALTER TABLE inbox_message ADD COLUMN is_official TINYINT NOT NULL DEFAULT 0 COMMENT '0=普通消息 1=官方推送' AFTER request_status;
