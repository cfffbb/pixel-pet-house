<template>
  <div class="feed-wrap">
    <!-- 左栏 -->
    <div class="feed-left">
      <!-- 宠物状态 + 连击 -->
      <div class="card feed-pet-card" v-if="pet">
        <div class="feed-pet-top">
          <div class="feed-pet-avatar" :class="petExpressionClass">
            <span class="feed-pet-emoji">{{ petEmoji }}</span>
          </div>
          <div class="feed-pet-info">
            <h3>🐾 {{ pet.name }}</h3>
            <span class="muted">{{ pet.typeName }} · {{ pet.personality }} · Lv{{ pet.level }}</span>
          </div>
          <div class="feed-streak-badge" v-if="stats && stats.streak > 0">
            <span class="streak-flame">🔥</span>
            <span class="streak-num">{{ stats.streak }}</span>
            <span class="streak-label">天</span>
          </div>
        </div>

        <div class="feed-bars">
          <div class="feed-bar-row">
            <span class="feed-bar-label">饱食度</span>
            <div class="feed-bar-track">
              <div class="feed-bar-fill hunger" :style="{ width: (pet.hunger || 0) + '%' }" :class="hungerClass"></div>
            </div>
            <span class="feed-bar-val">{{ pet.hunger || 0 }} · {{ hungerDesc }}</span>
          </div>
          <div class="feed-bar-row">
            <span class="feed-bar-label">心情</span>
            <div class="feed-bar-track">
              <div class="feed-bar-fill mood" :style="{ width: (pet.mood || 0) + '%' }"></div>
            </div>
            <span class="feed-bar-val">{{ pet.mood || 0 }} · {{ moodDesc }}</span>
          </div>
        </div>

        <!-- 统计行 -->
        <div class="feed-stats-row" v-if="stats">
          <div class="feed-stat-chip">
            <span class="fs-icon">🔥</span>
            <span class="fs-val">{{ stats.streak || 0 }}天连击</span>
          </div>
          <div class="feed-stat-chip">
            <span class="fs-icon">🍽️</span>
            <span class="fs-val">{{ stats.totalFeeds || 0 }}次喂食</span>
          </div>
          <div class="feed-stat-chip">
            <span class="fs-icon">✨</span>
            <span class="fs-val">{{ stats.luckyCount || 0 }}次幸运</span>
          </div>
          <div class="feed-stat-chip" v-if="stats.nextMilestone > 0">
            <span class="fs-icon">🎯</span>
            <span class="fs-val">距{{ stats.nextMilestone }}天</span>
          </div>
        </div>
      </div>
      <div class="card" v-else>
        <p class="muted">请先领取宠物</p>
      </div>

      <!-- 喂食动画区 -->
      <div class="card feed-anim-card" :class="{ active: feedAnim, lucky: luckyPopup }">
        <div class="feed-anim-stage">
          <!-- 宠物大表情 -->
          <div class="feed-anim-pet" :class="animPetClass">
            <span class="feed-anim-emoji">{{ animPetEmoji }}</span>
          </div>

          <!-- 食物飞入 -->
          <transition name="food-fly">
            <div v-if="feedAnim" class="feed-anim-food" :key="animKey">
              <span class="food-fly-emoji">{{ feedEmoji }}</span>
            </div>
          </transition>

          <!-- 粒子效果 -->
          <div class="feed-particles" v-if="feedAnim">
            <span v-for="i in 8" :key="'p'+i" class="particle" :style="particleStyle(i)">✨</span>
            <span v-for="i in 4" :key="'h'+i" class="particle heart" :style="particleStyle(i+8)">💖</span>
          </div>

          <!-- 反应文字 -->
          <div class="feed-reaction-text" v-if="feedAnim">
            {{ feedReaction }}
          </div>
          <div class="feed-reaction-text idle" v-else>
            选一个食物喂给它吧!
          </div>
        </div>

        <!-- 数值变化 -->
        <div class="feed-deltas" v-if="feedDeltas">
          <span class="delta hunger-delta" v-if="feedDeltas.hunger">🍖 饱食度 +{{ feedDeltas.hunger }}</span>
          <span class="delta mood-delta" v-if="feedDeltas.mood !== undefined">
            😊 心情 {{ feedDeltas.mood >= 0 ? '+' : '' }}{{ feedDeltas.mood }}
          </span>
          <span class="delta exp-delta" v-if="feedDeltas.exp">⭐ 经验 +{{ feedDeltas.exp }}</span>
          <span class="delta coin-delta" v-if="feedDeltas.coins">💰 金币 +{{ feedDeltas.coins }}</span>
        </div>

        <!-- 多样性提示 -->
        <div class="feed-variety-tip" v-if="feedDeltas && feedDeltas.varietyNote">
          <span :class="feedDeltas.varietyBonus >= 0 ? 'pos' : 'neg'">{{ feedDeltas.varietyNote }}</span>
        </div>
      </div>

      <!-- 幸运事件弹窗 -->
      <transition name="lucky-pop">
        <div class="lucky-overlay" v-if="luckyPopup" @click="luckyPopup = null">
          <div class="lucky-card" @click.stop>
            <div class="lucky-burst">
              <span v-for="i in 12" :key="i" class="lucky-spark" :style="sparkStyle(i)">✨</span>
            </div>
            <div class="lucky-emoji">🎉</div>
            <div class="lucky-title">{{ luckyTitle }}</div>
            <div class="lucky-desc">{{ luckyPopup }}</div>
            <button class="pix-btn btn-primary" @click="luckyPopup = null">好耶!</button>
          </div>
        </div>
      </transition>

      <!-- 连击里程碑弹窗 -->
      <transition name="lucky-pop">
        <div class="lucky-overlay" v-if="streakReward" @click="streakReward = ''">
          <div class="lucky-card streak" @click.stop>
            <div class="lucky-emoji">🔥</div>
            <div class="lucky-title">连击奖励!</div>
            <div class="lucky-desc">{{ streakReward }}</div>
            <button class="pix-btn btn-primary" @click="streakReward = ''">继续加油!</button>
          </div>
        </div>
      </transition>
    </div>

    <!-- 右栏 -->
    <div class="feed-right">
      <!-- 食物背包 -->
      <div class="card">
        <h3>🍱 食物背包</h3>
        <div class="food-category-tabs" v-if="inventory.length">
          <button class="cat-tab" :class="{ active: catFilter === 'ALL' }" @click="catFilter = 'ALL'">全部</button>
          <button class="cat-tab" :class="{ active: catFilter === 'STAPLE' }" @click="catFilter = 'STAPLE'">主食</button>
          <button class="cat-tab" :class="{ active: catFilter === 'SNACK' }" @click="catFilter = 'SNACK'">零食</button>
          <button class="cat-tab" :class="{ active: catFilter === 'SPECIAL' }" @click="catFilter = 'SPECIAL'">✨特殊</button>
          <button class="cat-tab" :class="{ active: catFilter === 'MEDICINE' }" @click="catFilter = 'MEDICINE'">药品</button>
        </div>

        <div class="food-grid" v-if="filteredFood.length">
          <div
            v-for="f in filteredFood"
            :key="f.itemId"
            class="food-card"
            :class="[f.foodCategory, { disabled: feeding || f.quantity <= 0, pref: prefType(f) }]"
            @click="feed(f)"
          >
            <div class="food-emoji">{{ foodEmoji(f) }}</div>
            <div class="food-name">{{ f.itemName }}</div>
            <div class="food-stats">
              <span title="恢复饱食度">🍖{{ f.hungerRestore }}</span>
              <span title="恢复心情">😊{{ f.moodRestore }}</span>
              <span class="food-exp" title="获得经验">⭐{{ expForCat(f.foodCategory) }}</span>
            </div>
            <div class="food-bottom">
              <span class="food-qty" :class="{ zero: f.quantity <= 0 }">x{{ f.quantity }}</span>
              <span class="food-cat-tag" :class="f.foodCategory">{{ catText(f.foodCategory) }}</span>
            </div>
            <div class="food-pref-hint" v-if="prefType(f) === 'LIKE'">❤️</div>
            <div class="food-pref-hint" v-if="prefType(f) === 'DISLIKE'">💔</div>
          </div>
        </div>
        <p v-else class="muted">背包空空如也,去商店买点食物吧!</p>
      </div>

      <!-- 最近喂食记录 -->
      <div class="card" v-if="recentMeals.length">
        <h3>📋 最近喂食</h3>
        <div class="meal-timeline">
          <div v-for="(m, i) in recentMeals" :key="i" class="meal-item">
            <span class="meal-emoji">{{ mealEmoji(m) }}</span>
            <div class="meal-info">
              <span class="meal-name">{{ m.itemName || '未知' }}</span>
              <span class="meal-meta">
                <span :class="'pref-' + (m.preference || 'NORMAL')">{{ prefText(m.preference) }}</span>
                <span v-if="m.expGain">+{{ m.expGain }}EXP</span>
                <span v-if="m.luckyEvent" class="lucky-tag">✨幸运</span>
                <span class="meal-time">{{ m.fedAt }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 喂养小贴士 -->
      <div class="card" v-if="pet">
        <h3>💡 喂养指南</h3>
        <div class="feed-tips">
          <div class="tip-row" v-for="t in tipList" :key="t.icon">
            <span class="tip-icon">{{ t.icon }}</span>
            <span class="tip-text">{{ t.text }}</span>
          </div>
        </div>
      </div>
    </div>

    <p v-if="err" class="error">{{ err }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import http from '../api/http';
import { useSound } from '../soundStore';

const { playSound } = useSound();

const pet = ref(null);
const inventory = ref([]);
const stats = ref(null);
const recentMeals = ref([]);
const catFilter = ref('ALL');
const feeding = ref(false);
const err = ref('');
const feedAnim = ref(false);
const feedEmoji = ref('');
const feedReaction = ref('');
const feedDeltas = ref(null);
const animKey = ref(0);
const luckyPopup = ref(null);
const luckyTitle = ref('');
const streakReward = ref('');
const animPetEmoji = ref('😊');
const animPetClass = ref('');

const EMOJI_MAP = {
  FISH: '🐟', BONE: '🦴', FEAST: '🍱', STEAK: '🥩', SALMON: '🍣',
  SEED: '🌻', CARROT: '🥕', GRASS: '🌿', MEAT: '🍖', MOUSE: '🐭',
  APPLE: '🍎', CAKE2: '🥧', BANANA: '🍌', BUG: '🐛', BONE2: '🦴',
  PUMPKIN: '🎃', CANTEEN: '🥫', FRUIT2: '🍇', FRUIT3: '🐉',
  COOKIE: '🍪', CAKE: '🎂', CANDY: '🍬', JERKY: '🥓', CHEESE: '🧀',
  RICE: '🍚', BREAD: '🍞', POTION: '💊', HERB: '🌿', VITAMIN: '💊',
  GOLDEN_FISH: '✨🐟', CRYSTAL_FRUIT: '🔮', RAINBOW_CAKE: '🌈', LUCKY_CLOVER: '🍀',
};

const LUCKY_TITLES = {
  COIN_BURST: '金币大爆发!',
  MOOD_BURST: '心情大爆发!',
  EXP_RAIN: '经验雨!',
  DOUBLE_HUNGER: '饱食度翻倍!',
  GIFT: '神秘礼物!',
};

const filteredFood = computed(() => {
  if (catFilter.value === 'ALL') return inventory.value;
  return inventory.value.filter(f => f.foodCategory === catFilter.value);
});

const hungerDesc = computed(() => {
  const h = pet.value?.hunger || 0;
  if (h >= 80) return '饱饱的';
  if (h >= 50) return '还不太饿';
  if (h >= 20) return '有点饿了';
  return '快饿坏了!';
});

const moodDesc = computed(() => {
  const m = pet.value?.mood || 0;
  if (m >= 80) return '超级开心';
  if (m >= 50) return '心情不错';
  if (m >= 20) return '有点低落';
  return '心情很差';
});

const hungerClass = computed(() => {
  const h = pet.value?.hunger || 0;
  if (h >= 80) return 'high';
  if (h >= 50) return 'mid';
  return 'low';
});

const petEmoji = computed(() => {
  const p = pet.value;
  if (!p) return '🐾';
  const mood = p.mood || 50;
  if (mood >= 80) return '😊';
  if (mood >= 50) return '🙂';
  if (mood >= 20) return '😟';
  return '😢';
});

const petExpressionClass = computed(() => {
  const p = pet.value;
  if (!p) return '';
  const mood = p.mood || 50;
  if (mood >= 80) return 'happy';
  if (mood >= 50) return 'ok';
  if (mood >= 20) return 'sad';
  return 'cry';
});

const tipList = computed(() => {
  const p = pet.value?.personality || '活泼';
  const tips = [
    { icon: '🔥', text: '每天喂食可获得连击,3/7/15/30天有金币奖励!' },
    { icon: '✨', text: '喂食有15%概率触发幸运事件(金币/经验/礼物等)' },
    { icon: '🍱', text: '喂3种以上不同食物有多样性奖励,总吃一样的会腻' },
    { icon: '⭐', text: '不同食物给不同经验:主食+1,零食+2,特殊+5' },
    { icon: '🐾', text: `${p}性格的宠物有特定食物偏好,喂对了额外加心情` },
  ];
  return tips;
});

function foodEmoji(f) {
  return EMOJI_MAP[f.itemCode] || '🍽️';
}

function catText(cat) {
  return { STAPLE: '主食', SNACK: '零食', MEDICINE: '药品', SPECIAL: '特殊' }[cat] || '食物';
}

function expForCat(cat) {
  return { STAPLE: 1, SNACK: 2, MEDICINE: 1, SPECIAL: 5 }[cat] || 1;
}

function prefType(f) {
  const p = pet.value?.personality || '活泼';
  const cat = f.foodCategory;
  if (p === '活泼') {
    if (cat === 'SNACK') return 'LIKE';
    if (cat === 'MEDICINE') return 'DISLIKE';
  }
  if (p === '高冷') {
    if (cat === 'STAPLE') return 'LIKE';
    if (cat === 'SNACK') return 'DISLIKE';
  }
  if (p === '贪吃') return 'LIKE';
  if (p === '粘人') {
    if (cat === 'SNACK') return 'LIKE';
    if (cat === 'MEDICINE') return 'DISLIKE';
  }
  if (p === '傲娇') {
    if (cat === 'SPECIAL') return 'LIKE';
    if (cat === 'STAPLE') return 'DISLIKE';
  }
  if (p === '胆小' && cat === 'SPECIAL') return 'LIKE';
  return 'NORMAL';
}

function prefText(pref) {
  return { LIKE: '❤️喜欢', DISLIKE: '💔不爱', NORMAL: '😐普通' }[pref] || '😐普通';
}

function mealEmoji(m) {
  const invItem = inventory.value.find(f => f.itemName === m.itemName);
  if (invItem) return EMOJI_MAP[invItem.itemCode] || '🍽️';
  return m.luckyEvent ? '✨🍽️' : '🍽️';
}

function particleStyle(i) {
  const angle = (i * 45) % 360;
  const dist = 40 + (i % 3) * 20;
  const delay = (i * 0.05) + 's';
  return {
    '--angle': angle + 'deg',
    '--dist': dist + 'px',
    'animation-delay': delay,
  };
}

function sparkStyle(i) {
  const angle = (i * 30) % 360;
  const dist = 30 + (i % 4) * 15;
  return {
    '--angle': angle + 'deg',
    '--dist': dist + 'px',
    'animation-delay': (i * 0.04) + 's',
  };
}

async function loadAll() {
  try {
    const [p, inv, st] = await Promise.all([
      http.get('/api/pet/my'),
      http.get('/api/shop/inventory'),
      http.get('/api/shop/feeding-stats'),
    ]);
    if (p && p.hasPet) pet.value = p;
    inventory.value = inv || [];
    stats.value = st || null;
    recentMeals.value = st?.recentMeals || [];
  } catch (e) {
    err.value = e.message;
  }
}

async function feed(f) {
  if (feeding.value) return;
  if (f.quantity <= 0) { err.value = '这个食物没有了'; return; }
  feeding.value = true;
  err.value = '';
  playSound('button');
  try {
    const data = await http.post('/api/shop/feed', { itemId: f.itemId });
    feedEmoji.value = foodEmoji(f);
    feedReaction.value = data.reaction || '吃完了!';
    feedDeltas.value = {
      hunger: data.hungerDelta,
      mood: data.moodDelta,
      exp: data.expGain,
      coins: data.coinGain,
      varietyBonus: data.varietyBonus,
      varietyNote: data.varietyNote,
    };
    animPetEmoji.value = data.petExpression || '😊';
    animPetClass.value = data.luckyEvent ? 'lucky' : (data.preference === 'LIKE' ? 'love' : data.preference === 'DISLIKE' ? 'disgust' : 'happy');
    animKey.value++;
    feedAnim.value = true;
    setTimeout(() => { feedAnim.value = false; animPetClass.value = ''; }, 2500);

    if (data.luckyEvent) {
      luckyTitle.value = LUCKY_TITLES[data.luckyEvent] || '幸运事件!';
      luckyPopup.value = data.luckyDesc || '触发了幸运事件!';
      setTimeout(() => { if (luckyPopup.value) luckyPopup.value = null; }, 4000);
    }
    if (data.streakReward) {
      streakReward.value = data.streakReward;
      setTimeout(() => { if (streakReward.value) streakReward.value = ''; }, 4000);
    }
    await loadAll();
  } catch (e) {
    err.value = e.message;
  } finally {
    feeding.value = false;
  }
}

onMounted(() => { loadAll(); });
</script>

<style scoped>
.feed-wrap { display: flex; gap: 16px; }
.feed-left { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.feed-right { width: 360px; display: flex; flex-direction: column; gap: 12px; }
@media (max-width: 860px) { .feed-wrap { flex-direction: column; } .feed-right { width: 100%; } }

/* 宠物状态卡 */
.feed-pet-top { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.feed-pet-avatar { width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 32px; border: 3px solid var(--border); background: var(--surface); transition: all 0.3s; }
.feed-pet-avatar.happy { border-color: #6bcf7f; background: #e8f5e9; }
.feed-pet-avatar.ok { border-color: var(--accent); background: #fff3e0; }
.feed-pet-avatar.sad { border-color: #ff9f43; background: #fff8e1; }
.feed-pet-avatar.cry { border-color: #e91e63; background: #fce4ec; }
.feed-pet-info { flex: 1; }
.feed-pet-info h3 { margin: 0 0 2px; }

/* 连击徽章 */
.feed-streak-badge { display: flex; align-items: center; gap: 2px; padding: 4px 12px; background: linear-gradient(135deg, #ff6b35, #ff9f43); border-radius: 20px; color: white; font-weight: 700; box-shadow: 0 2px 8px rgba(255,107,53,0.3); }
.streak-flame { font-size: 18px; animation: flameFlicker 0.5s ease-in-out infinite alternate; }
@keyframes flameFlicker { 0% { transform: scale(1); } 100% { transform: scale(1.15) rotate(-3deg); } }
.streak-num { font-size: 20px; }
.streak-label { font-size: 11px; opacity: 0.9; }

/* 进度条 */
.feed-bars { margin-bottom: 8px; }
.feed-bar-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.feed-bar-label { width: 50px; font-size: 12px; color: var(--muted); }
.feed-bar-track { flex: 1; height: 14px; background: var(--surface); border-radius: 7px; border: 2px solid var(--border); overflow: hidden; }
.feed-bar-fill { height: 100%; border-radius: 5px; transition: width 0.5s ease; }
.feed-bar-fill.hunger { background: linear-gradient(90deg, var(--accent), #ffd93d); }
.feed-bar-fill.hunger.low { background: linear-gradient(90deg, #d1453b, #ff6b6b); }
.feed-bar-fill.hunger.mid { background: linear-gradient(90deg, #ff9f43, #ffd93d); }
.feed-bar-fill.hunger.high { background: linear-gradient(90deg, #6bcf7f, #4d96ff); }
.feed-bar-fill.mood { background: linear-gradient(90deg, #c780ff, #ff9ff3); }
.feed-bar-val { font-size: 11px; color: var(--muted); min-width: 100px; }

/* 统计行 */
.feed-stats-row { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
.feed-stat-chip { display: flex; align-items: center; gap: 4px; padding: 3px 8px; background: var(--surface); border: 1px solid var(--border); border-radius: 12px; font-size: 11px; }
.fs-icon { font-size: 14px; }
.fs-val { color: var(--muted); }

/* 喂食动画区 */
.feed-anim-card { min-height: 200px; display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative; overflow: hidden; }
.feed-anim-card.active { background: linear-gradient(135deg, #fff3e0, #fff8e1); }
.feed-anim-card.lucky { background: linear-gradient(135deg, #fffde7, #fce4ec); box-shadow: 0 0 20px rgba(255,193,7,0.4); }
.feed-anim-stage { position: relative; width: 100%; text-align: center; padding: 20px 0; min-height: 120px; }

.feed-anim-pet { font-size: 64px; display: inline-block; transition: transform 0.3s; }
.feed-anim-pet.happy { animation: bounce 0.5s ease infinite alternate; }
.feed-anim-pet.love { animation: loveShake 0.4s ease; }
.feed-anim-pet.disgust { animation: disgust 0.3s ease; }
.feed-anim-pet.lucky { animation: luckyBounce 0.5s ease infinite alternate; }
@keyframes bounce { 0% { transform: translateY(0); } 100% { transform: translateY(-10px); } }
@keyframes loveShake { 0%,100% { transform: rotate(0); } 25% { transform: rotate(-8deg) scale(1.1); } 75% { transform: rotate(8deg) scale(1.1); } }
@keyframes disgust { 0%,100% { transform: translateX(0); } 25% { transform: translateX(-5px); } 75% { transform: translateX(5px); } }
@keyframes luckyBounce { 0% { transform: scale(1) rotate(0); } 100% { transform: scale(1.2) rotate(5deg); } }

.food-fly-emoji { font-size: 36px; position: absolute; top: 10px; right: 10%; animation: foodFly 0.8s ease forwards; }
@keyframes foodFly { 0% { transform: translate(80px, -20px) scale(0.5); opacity: 0; } 50% { transform: translate(20px, 10px) scale(1.3); opacity: 1; } 100% { transform: translate(0, 30px) scale(0.8); opacity: 0; } }

.feed-particles { position: absolute; top: 50%; left: 50%; pointer-events: none; }
.particle { position: absolute; font-size: 16px; animation: particleBurst 1.2s ease-out forwards; }
.particle.heart { font-size: 18px; }
@keyframes particleBurst { 0% { transform: translate(-50%,-50%) rotate(var(--angle)) translateX(0) scale(0); opacity: 1; } 100% { transform: translate(-50%,-50%) rotate(var(--angle)) translateX(var(--dist)) scale(0.5) translateY(-30px); opacity: 0; } }

.feed-reaction-text { font-size: 14px; color: var(--accent); font-weight: 600; margin-top: 12px; }
.feed-reaction-text.idle { color: var(--muted); font-weight: 400; }

.feed-deltas { margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
.feed-deltas .delta { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; animation: deltaPop 0.4s ease; }
@keyframes deltaPop { 0% { transform: scale(0.7); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
.hunger-delta { background: #fff3e0; color: #ff9f43; }
.mood-delta { background: #f3e5f5; color: #c780ff; }
.exp-delta { background: #e8f5e9; color: #2e7d32; }
.coin-delta { background: #fffde7; color: #f9a825; }

.feed-variety-tip { margin-top: 6px; font-size: 12px; }
.feed-variety-tip .pos { color: #2e7d32; }
.feed-variety-tip .neg { color: #c62828; }

/* 幸运弹窗 */
.lucky-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 9999; }
.lucky-card { background: white; border-radius: 20px; padding: 32px 40px; text-align: center; box-shadow: 0 10px 40px rgba(0,0,0,0.2); max-width: 320px; position: relative; overflow: hidden; }
.lucky-card.streak { border: 3px solid #ff9f43; }
.lucky-emoji { font-size: 48px; margin-bottom: 8px; animation: luckyBounce 0.6s ease infinite alternate; }
.lucky-title { font-size: 20px; font-weight: 700; color: var(--accent); margin-bottom: 8px; }
.lucky-desc { font-size: 14px; color: var(--fg); margin-bottom: 16px; }
.lucky-burst { position: absolute; top: 50%; left: 50%; pointer-events: none; }
.lucky-spark { position: absolute; font-size: 14px; animation: particleBurst 1s ease-out forwards; }

.lucky-pop-enter-active, .lucky-pop-leave-active { transition: all 0.3s ease; }
.lucky-pop-enter-from { opacity: 0; transform: scale(0.8); }
.lucky-pop-leave-to { opacity: 0; transform: scale(0.9); }

.food-fly-enter-active { transition: all 0.8s ease; }

/* 食物网格 */
.food-category-tabs { display: flex; gap: 4px; margin-bottom: 12px; flex-wrap: wrap; }
.cat-tab { padding: 4px 12px; border: 2px solid var(--border); border-radius: 16px; background: var(--surface); color: var(--muted); font-size: 12px; cursor: pointer; transition: all 0.2s; }
.cat-tab.active { background: var(--accent); color: white; border-color: var(--accent); }
.food-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.food-card { text-align: center; padding: 10px 6px; background: var(--surface); border: 2px solid var(--border); border-radius: 12px; cursor: pointer; transition: all 0.2s; position: relative; }
.food-card:hover { border-color: var(--accent); transform: translateY(-3px); box-shadow: 0 4px 12px rgba(255,159,67,0.15); }
.food-card.disabled { opacity: 0.4; pointer-events: none; }
.food-card.STAPLE { border-top: 3px solid #ff9f43; }
.food-card.SNACK { border-top: 3px solid #6bcf7f; }
.food-card.MEDICINE { border-top: 3px solid #e91e63; }
.food-card.SPECIAL { border-top: 3px solid #9c27b0; background: linear-gradient(135deg, var(--surface), #fffde7); }
.food-card.pref.LIKE { box-shadow: 0 0 8px rgba(107,207,127,0.2); }
.food-emoji { font-size: 28px; }
.food-name { font-size: 12px; color: var(--fg); margin: 4px 0; }
.food-stats { font-size: 10px; color: var(--muted); display: flex; justify-content: center; gap: 4px; }
.food-exp { color: #2e7d32; }
.food-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.food-qty { font-size: 12px; color: var(--accent); font-weight: 700; }
.food-qty.zero { color: #ccc; }
.food-cat-tag { font-size: 9px; padding: 1px 6px; border-radius: 4px; }
.food-cat-tag.STAPLE { background: #fff3e0; color: #ff9f43; }
.food-cat-tag.SNACK { background: #e8f5e9; color: #6bcf7f; }
.food-cat-tag.MEDICINE { background: #fce4ec; color: #e91e63; }
.food-cat-tag.SPECIAL { background: #f3e5f5; color: #9c27b0; }
.food-pref-hint { position: absolute; top: 4px; right: 6px; font-size: 14px; }

/* 喂食记录 */
.meal-timeline { display: flex; flex-direction: column; gap: 6px; }
.meal-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 8px; background: var(--surface); transition: all 0.2s; }
.meal-item:hover { background: #fff3e0; }
.meal-emoji { font-size: 20px; }
.meal-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.meal-name { font-size: 13px; font-weight: 500; }
.meal-meta { font-size: 10px; color: var(--muted); display: flex; gap: 8px; flex-wrap: wrap; }
.pref-LIKE { color: #e91e63; }
.pref-DISLIKE { color: #696969; }
.pref-NORMAL { color: var(--muted); }
.lucky-tag { color: #ff9f43; font-weight: 600; }
.meal-time { margin-left: auto; }

/* 小贴士 */
.feed-tips { display: flex; flex-direction: column; gap: 8px; }
.tip-row { display: flex; align-items: flex-start; gap: 8px; padding: 6px 8px; border-radius: 8px; background: var(--surface); }
.tip-icon { font-size: 16px; flex-shrink: 0; }
.tip-text { font-size: 12px; color: var(--fg); line-height: 1.5; }
</style>
