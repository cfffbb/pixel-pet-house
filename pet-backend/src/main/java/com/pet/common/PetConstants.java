package com.pet.common;

/**
 * 宠物系统常量(M3/M3.5 拍板的固定池与状态)
 */
public final class PetConstants {

    private PetConstants() {
    }

    /** 性格池(5 种,M3 拍板) */
    public static final java.util.List<String> PERSONALITIES =
            java.util.List.of("活泼", "高冷", "粘人", "贪吃", "傲娇");

    /** 性格 → 饥饿下降速度系数(M3 拍板,保证饿死 ≥ 2 天) */
    public static final double COEFF_HIGH_COLD = 0.8;   // 高冷
    public static final double COEFF_STICKY = 1.1;      // 粘人
    public static final double COEFF_GREEDY = 1.5;      // 贪吃

    /** 保底饿死小时数 = 2 天 */
    public static final double MIN_STARVE_HOURS = 48.0;

    /** 病危缓冲(小时),M3 拍板 24 小时 */
    public static final long DANGER_GRACE_HOURS = 24;

    /** 配种冷却与怀孕时长(天),缩短为1天方便测试和快速体验 */
    public static final long BREEDING_COOL_DAYS = 1;
    public static final long PREGNANT_DAYS = 1;

    /** 配种费用(币/方),M3.5 拍板 10 */
    public static final int BREEDING_FEE = 10;

    /** 托管费(币/只/天),M3.5 拍板 5 */
    public static final int HATCHLING_FEE_PER_DAY = 5;

    /** 幼崽夭折概率 10% */
    public static final int HATCHLING_MORTALITY_PERCENT = 10;

    /** 每胎出生数量 3–5 只 */
    public static final int LITTER_MIN = 3;
    public static final int LITTER_MAX = 5;

    /** 宠物状态 */
    public static final String PET_ALIVE = "ALIVE";
    public static final String PET_DANGER = "DANGER";
    public static final String PET_STARVED = "STARVED";

    /** 幼崽状态 */
    public static final String HATCHLING_STORED = "STORED";
    public static final String HATCHLING_FROZEN = "FROZEN";
    public static final String HATCHLING_CLAIMED = "CLAIMED";

    /** 配种记录状态 */
    public static final String BREED_PENDING = "PENDING";
    public static final String BREED_PREGNANT = "PREGNANT";
    public static final String BREED_BORN = "BORN";
    public static final String BREED_REJECTED = "REJECTED";

    /** 收件箱类型与状态 */
    public static final String MSG_BREED_REQUEST = "BREED_REQUEST";
    public static final String MSG_GIFT_REQUEST = "GIFT_REQUEST";
    public static final String MSG_CHAT = "CHAT";
    public static final String MSG_REPORT = "REPORT"; // 工资单(系统消息,发送人=0)
    public static final String MSG_FRIEND_REQUEST = "FRIEND_REQUEST"; // 好友申请(M11)
    public static final String MSG_UNREAD = "UNREAD";
    public static final String MSG_READ = "READ";
    public static final String REQ_PENDING = "PENDING";
    public static final String REQ_ACCEPTED = "ACCEPTED";
    public static final String REQ_REJECTED = "REJECTED";

    /** 设置项键名 */
    public static final String SETTING_ALLOW_BREEDING = "allow_breeding";
    /** 应用感知授权(M9;默认关,隐私红线) */
    public static final String SETTING_APP_MONITOR = "app_monitor_enabled";

    /** 玩耍(M4 暂定值,列入 M7 统一校准):每次 +2 心情,每天最多 5 次 */
    public static final int PLAY_MOOD_GAIN = 2;
    public static final int PLAY_DAILY_LIMIT = 5;

    /** 行为事件(M3 拍板):每天最多 1 次、30% 概率;暖心 +5 心情 / 搞破坏 -5 心情 */
    public static final int BEHAVIOR_DAILY_PERCENT = 30;
    public static final int BEHAVIOR_MOOD_CHANGE = 5;

    /** 喂食基础心情加成(M7 暂定,校准):喂食 +3 心情 */
    public static final int FEED_MOOD_BONUS = 3;

    /** 喂食幸运事件概率(15%) */
    public static final int FEED_LUCKY_PERCENT = 15;

    /** 连续喂食每天心情加成上限 */
    public static final int FEED_STREAK_MOOD_CAP = 10;

    /** 食物多样性奖励:近5次中3种以上不同食物时心情加成 */
    public static final int FEED_VARIETY_BONUS = 5;

    /** 重复喂食惩罚:近5次全相同食物时心情扣减 */
    public static final int FEED_BOREDOM_PENALTY = 2;

    /** 喂食经验值:按食物分类 */
    public static final int FEED_EXP_STAPLE = 1;
    public static final int FEED_EXP_SNACK = 2;
    public static final int FEED_EXP_MEDICINE = 1;
    public static final int FEED_EXP_SPECIAL = 5;

    /** 连续喂食天数奖励金币(里程碑) */
    public static final int FEED_STREAK_3_COINS = 10;
    public static final int FEED_STREAK_7_COINS = 30;
    public static final int FEED_STREAK_15_COINS = 50;
    public static final int FEED_STREAK_30_COINS = 100;

    /** 番茄钟(M7 拍板):1 币/分钟;每轮最多暂停 2 次;放弃 0 币 */
    public static final int POMODORO_COIN_PER_MINUTE = 1;
    public static final int POMODORO_MAX_PAUSES = 2;
    public static final int POMODORO_MAX_MINUTES = 480;

    /** 等级:累计专注分钟每 1200 分钟(20 小时)升 1 级,最高 100 级 */
    public static final int LEVEL_MINUTES_PER_LEVEL = 1200;
    public static final int LEVEL_MAX = 100;

    /** 新宠物初始食物:10 条小鱼干(M7 拍板) */
    public static final int INITIAL_FOOD_QUANTITY = 10;
    public static final String INITIAL_FOOD_CODE = "FISH";

    /** 成熟期:出生 3 天后可上架配种市场(M12 默认,可改) */
    public static final int MATURE_DAYS = 3;

    /** 成长阶段(现实天数):幼年<1天,少年<3天,成年<60天,老年≥60天 */
    public static final int STAGE_BABY_DAYS = 1;
    public static final int STAGE_YOUNG_DAYS = 3;
    public static final int STAGE_OLD_DAYS = 60;

    /**
     * 玩耍模式(10 种,M12 默认):[名称, 图标, 心情, 饥饿变化, 经验]
     * 饥饿为负表示消耗(变小),0 不变
     */
    public static final java.util.List<String[]> PLAY_MODES = java.util.List.of(
            new String[]{"抚摸", "🤗", "4", "0", "0"},
            new String[]{"跑步", "🏃", "4", "-3", "0"},
            new String[]{"学习", "📚", "1", "0", "2"},
            new String[]{"玩球", "⚽", "5", "-2", "0"},
            new String[]{"散步", "🚶", "3", "-1", "1"},
            new String[]{"听音乐", "🎵", "4", "0", "0"},
            new String[]{"跳舞", "💃", "5", "-2", "0"},
            new String[]{"捉迷藏", "🙈", "5", "-2", "0"},
            new String[]{"晒太阳", "☀️", "2", "0", "0"},
            new String[]{"扑蝶", "🦋", "4", "-2", "0"}
    );
}
