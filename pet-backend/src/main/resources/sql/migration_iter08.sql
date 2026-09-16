-- Iter-08: 气泡库扩充 - 增加学习陪伴、日常互动、人感化台词(50+条)

INSERT INTO bubble_prompt (species, personality, gender, stage, category, content, enabled) VALUES
-- ========== 学习陪伴类(重点扩充) ==========
(NULL, NULL, NULL, NULL, 'study', '专注半小时了,站起来活动一下筋骨吧~', 1),
(NULL, NULL, NULL, NULL, 'study', '喝口水,眼睛往远处看看,保护视力哦', 1),
(NULL, NULL, NULL, NULL, 'study', '你学习的样子好认真,我都不敢打扰你', 1),
(NULL, NULL, NULL, NULL, 'study', '番茄钟又完成一个!你超棒的!', 1),
(NULL, NULL, NULL, NULL, 'study', '今天已经学习很久了,适当休息一下吧', 1),
(NULL, NULL, NULL, NULL, 'study', '遇到难题了吗?慢慢来,我相信你可以的!', 1),
(NULL, NULL, NULL, NULL, 'study', '学习间隙要不要和我玩一会儿换换脑子?', 1),
(NULL, NULL, NULL, NULL, 'study', '加油加油!再坚持一下就到休息时间啦', 1),
(NULL, NULL, NULL, NULL, 'study', '哇,你今天学了好多东西,我都佩服你了', 1),
(NULL, NULL, NULL, NULL, 'study', '我就在旁边陪着你,不吵不闹~', 1),
(NULL, NULL, NULL, NULL, 'study', '你敲键盘的声音好好听,像在弹钢琴', 1),
(NULL, NULL, NULL, NULL, 'study', '累了就摸摸我,我给你充电!', 1),
(NULL, NULL, NULL, NULL, 'study', '学累了吗?我给你讲个笑话吧~', 1),
(NULL, NULL, NULL, NULL, 'study', '你努力的样子,是我见过最帅的!', 1),
(NULL, NULL, NULL, NULL, 'study', '今天也要元气满满地学习哦~', 1),

-- ========== 日常互动/问候类 ==========
(NULL, NULL, NULL, NULL, 'greeting', '你回来啦!我一直在等你呢~', 1),
(NULL, NULL, NULL, NULL, 'greeting', '嘿嘿,一看到你我就开心!', 1),
(NULL, NULL, NULL, NULL, 'greeting', '今天过得怎么样呀?想和我说说吗?', 1),
(NULL, NULL, NULL, NULL, 'greeting', '欢迎回家~家里有我在,不会孤单的', 1),
(NULL, NULL, NULL, NULL, 'greeting', '你来啦!我刚刚还在想你呢', 1),
(NULL, NULL, NULL, NULL, 'greeting', '又是新的一天,我们一起加油吧!', 1),
(NULL, NULL, NULL, NULL, 'greeting', '你终于来了,我一个人好无聊…', 1),
(NULL, NULL, NULL, NULL, 'greeting', '看到你就像看到了小太阳~', 1),

-- ========== 随机闲聊/人感化 ==========
(NULL, NULL, NULL, NULL, 'random', '你知道吗?我最喜欢你笑的样子了', 1),
(NULL, NULL, NULL, NULL, 'random', '今天天气好吗?我也想出去晒太阳~', 1),
(NULL, NULL, NULL, NULL, 'random', '嘿嘿,我偷偷学了个新动作,想看吗?', 1),
(NULL, NULL, NULL, NULL, 'random', '你最近好像很忙的样子,要注意身体哦', 1),
(NULL, NULL, NULL, NULL, 'random', '我虽然不会说话,但我一直都在听你说', 1),
(NULL, NULL, NULL, NULL, 'random', '你是我最重要的人,我会一直陪着你', 1),
(NULL, NULL, NULL, NULL, 'random', '哼,你不理我,我要生气了…才怪!', 1),
(NULL, NULL, NULL, NULL, 'random', '今天的你也很帅气/漂亮呢~', 1),
(NULL, NULL, NULL, NULL, 'random', '我做了个梦,梦到我们一起去冒险了', 1),
(NULL, NULL, NULL, NULL, 'random', '你身上有股好闻的味道,是什么呀?', 1),
(NULL, NULL, NULL, NULL, 'random', '我觉得和你在一起,时间过得好快', 1),
(NULL, NULL, NULL, NULL, 'random', '你有没有想我呀?我可想你了', 1),
(NULL, NULL, NULL, NULL, 'random', '发呆的时候最适合想你了', 1),
(NULL, NULL, NULL, NULL, 'random', '我不会说话,但我的尾巴会替我说', 1),
(NULL, NULL, NULL, NULL, 'random', '今天也要开开心心的哦!', 1),
(NULL, NULL, NULL, NULL, 'random', '你是世界上最好的主人!', 1),
(NULL, NULL, NULL, NULL, 'random', '我虽然小,但我会用我的方式保护你', 1),
(NULL, NULL, NULL, NULL, 'random', '偷偷告诉你,你打哈欠的时候好可爱', 1),
(NULL, NULL, NULL, NULL, 'random', '我在你屏幕上踩了个小脚印,嘿嘿', 1),

-- ========== 开心/情绪类 ==========
(NULL, NULL, NULL, NULL, 'happy', '嘿嘿嘿,今天超开心的!', 1),
(NULL, NULL, NULL, NULL, 'happy', '开心到转圈圈!', 1),
(NULL, NULL, NULL, NULL, 'happy', '你开心我就开心,我们是一伙的!', 1),
(NULL, NULL, NULL, NULL, 'happy', '笑一个嘛~你笑起来最好看了', 1),

-- ========== 困倦/晚安类 ==========
(NULL, NULL, NULL, NULL, 'sleepy', '哈欠~我有点困了,你也早点休息吧', 1),
(NULL, NULL, NULL, NULL, 'sleepy', '熬夜对身体不好哦,我陪你睡', 1),
(NULL, NULL, NULL, NULL, 'sleepy', '晚安~明天见,记得梦见我哦', 1),
(NULL, NULL, NULL, NULL, 'sleepy', '眼睛都睁不开了…让我靠一会儿', 1),

-- ========== 饥饿类 ==========
(NULL, NULL, NULL, NULL, 'hungry', '肚子在叫了…不是我,是肚子自己叫的!', 1),
(NULL, NULL, NULL, NULL, 'hungry', '有好吃的吗?什么口味都行!', 1),
(NULL, NULL, NULL, NULL, 'hungry', '再不给我吃的,我就要…我就要卖萌了!', 1),

-- ========== 玩耍类 ==========
(NULL, NULL, NULL, NULL, 'play', '来玩来玩!我超厉害的!', 1),
(NULL, NULL, NULL, NULL, 'play', '再来一次!这次我肯定赢!', 1),
(NULL, NULL, NULL, NULL, 'play', '嘿嘿,抓到你啦!', 1),
(NULL, NULL, NULL, NULL, 'play', '玩累了…不过还能再玩一会儿!', 1),

-- ========== 安慰/陪伴类 ==========
(NULL, NULL, NULL, NULL, 'sad', '别难过了,我会一直陪着你的', 1),
(NULL, NULL, NULL, NULL, 'sad', '哭出来也没关系,我帮你擦掉眼泪', 1),
(NULL, NULL, NULL, NULL, 'sad', '抱抱~一切都会好起来的', 1),
(NULL, NULL, NULL, NULL, 'sad', '你不是一个人,还有我呢', 1)

ON DUPLICATE KEY UPDATE content = VALUES(content), enabled = VALUES(enabled);
