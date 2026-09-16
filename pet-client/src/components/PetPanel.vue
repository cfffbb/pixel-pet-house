<template>
  <div>
    <div v-if="loading" class="card muted">加载中…</div>

    <!-- 有宠物 -->
    <div v-else-if="pet && pet.hasPet" class="card">
      <div class="pet-head">
        <PetFigure :type-code="pet.typeCode" :gender="pet.gender" :stage="pet.stage" :size="96" />
        <div>
          <h3>
            {{ pet.petName }}
            <span class="muted">
              ({{ pet.typeName }} · {{ pet.subtypeName }} · {{ pet.gender === 'MALE' ? '雄' : '雌' }} ·
              {{ pet.personality }} · {{ rarityText(pet.rarity) }})
            </span>
          </h3>
          <p class="muted">
            {{ stageText(pet.stage) }} · 年龄 {{ pet.ageDays }} 天 · 等级 {{ pet.level }} · 经验 {{ pet.exp }}
            <span :class="pet.mature ? 'ok' : 'error'">{{ pet.mature ? '已成熟(可上架/配种)' : '未成熟(出生满 3 天可上架)' }}</span>
            <span v-if="pet.listed === 1" class="ok"> · ● 已上架市场</span>
          </p>
        </div>
      </div>
      <p v-if="pet.behavior" class="ok">🎭 {{ pet.behavior }}</p>
      <div class="pet-stats-bars">
        <div class="stat-bar-item">
          <span class="stat-bar-label">🍖 饥饿</span>
          <div class="stat-bar-track"><div class="stat-bar-fill hunger" :style="{ width: pet.hunger + '%' }"></div></div>
          <span class="stat-bar-val">{{ pet.hunger }}</span>
        </div>
        <div class="stat-bar-item">
          <span class="stat-bar-label">💗 心情</span>
          <div class="stat-bar-track"><div class="stat-bar-fill mood" :style="{ width: pet.mood + '%' }"></div></div>
          <span class="stat-bar-val">{{ pet.mood }}</span>
        </div>
        <div class="stat-bar-item">
          <span class="stat-bar-label">⭐ 经验</span>
          <div class="stat-bar-track"><div class="stat-bar-fill exp" :style="{ width: expPercent + '%' }"></div></div>
          <span class="stat-bar-val">Lv.{{ pet.level }} · {{ pet.exp }}/{{ expMax }}</span>
        </div>
        <div class="stat-bar-item">
          <span class="stat-bar-label">🪙 游戏币</span>
          <span class="stat-bar-val coins">{{ pet.coins }}</span>
        </div>
      </div>
      <p v-if="pet.status === 'DANGER'" class="error">
        ⚠️ 病危中!还剩约 {{ pet.dangerRemainHours }} 小时,快去商店喂食救它!
      </p>
      <p v-else-if="pet.hunger < 30" class="error">🍽️ 它好饿,去商店喂点吃的吧</p>

      <!-- 台词气泡 -->
      <transition name="bubble-fade">
        <div v-if="bubbleVisible" class="pet-speech-bubble">{{ bubbleText }}</div>
      </transition>

      <!-- 上架开关 -->
      <div class="row">
        <button @click="toggleListed">{{ pet.listed === 1 ? '下架市场' : '上架到市场' }}</button>
        <span class="muted small">上架条件:活着 · 成熟 3 天 · 非冷却(不满足会提示原因)</span>
      </div>

      <!-- 🎒 宠物背包(小屋内直接投喂) -->
      <h4>🎒 宠物背包</h4>
      <div class="inv-grid" v-if="inventory.length">
        <div v-for="inv in inventory" :key="inv.itemId" class="inv-card">
          <span class="inv-emoji">{{ emojiOf(inv.itemName) }}</span>
          <span class="inv-name">{{ inv.itemName }}</span>
          <span class="inv-qty">×{{ inv.quantity }}</span>
          <button v-if="inv.itemType === 'FOOD' && inv.quantity > 0" class="feed-btn" @click="feed(inv)">🍖 喂食</button>
        </div>
      </div>
      <p v-else class="muted small">背包空空,先去商店买点东西吧。</p>
      <p v-if="feedMsg" class="ok">{{ feedMsg }}</p>
      <p v-if="feedErr" class="error">{{ feedErr }}</p>

      <!-- 玩耍:模式来自数据库(管理员可加),不限次数 -->
      <h4>🎮 玩耍(不限次数,管理员可在后台添加模式)</h4>
      <div class="modes">
        <button v-for="m in modes" :key="m.name" class="mode" @click="play(m.name)">
          <img v-if="m.image" :src="m.image" class="mode-img" alt="" />
          <span v-else class="icon">{{ m.icon }}</span>
          <span>{{ m.name }}</span>
          <span class="muted small">心情{{ m.mood > 0 ? '+' : '' }}{{ m.mood }}{{ m.hunger ? ' · 饿' + m.hunger : '' }}{{ m.exp ? ' · 经验+' + m.exp : '' }}</span>
        </button>
      </div>
      <p v-if="playMsg" class="ok">{{ playMsg }}</p>
      <p v-if="playErr" class="error">{{ playErr }}</p>

      <!-- 管理员测试:换宠 -->
      <div v-if="isAdmin" class="admin-box">
        <h4>🛡️ 管理员测试 · 直接换宠物(检查 UI)</h4>
        <div class="row">
          <select v-model="sw.typeCode">
            <option v-for="t in types" :key="t.typeCode" :value="t.typeCode">{{ t.typeName }}</option>
          </select>
          <select v-model="sw.gender">
            <option value="MALE">雄</option>
            <option value="FEMALE">雌</option>
          </select>
          <select v-model="sw.personality">
            <option v-for="p in personalities" :key="p" :value="p">{{ p }}</option>
          </select>
          <button @click="switchPet">切换</button>
        </div>
        <p class="muted small">切换后旧宠物标为已死,新宠物金币 99999,方便你测试任意界面。</p>
      </div>
    </div>

    <!-- 没有宠物 -->
    <div v-else class="card">
      <h3>🐣 还没有宠物</h3>
      <div v-if="needReflection" class="card reflect">
        <p><b>它走了。先认真回答两个问题,再迎接新伙伴:</b></p>
        <p class="muted">{{ q1 }}</p>
        <textarea v-model="r1" rows="2" placeholder="至少 10 个字"></textarea>
        <p class="muted">{{ q2 }}</p>
        <textarea v-model="r2" rows="2" placeholder="至少 10 个字"></textarea>
        <button @click="submitReflection">提交反思</button>
        <p v-if="refErr" class="error">{{ refErr }}</p>
        <p v-if="refOk" class="ok">{{ refOk }}</p>
      </div>
      <div class="row">
        <input v-model="drawName" placeholder="给新宠物起个名字(只能起一次,不能改)" />
        <button @click="draw">🎁 抽蛋</button>
      </div>
      <p v-if="drawMsg" class="ok">{{ drawMsg }}</p>
      <p v-if="drawErr" class="error">{{ drawErr }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import http from '../api/http';
import PetFigure from './PetFigure.vue';
import { rarityText } from '../constants';
import { useSound } from '../soundStore';

const { playSound } = useSound();
const emit = defineEmits(['go-shop', 'pet-switched']);
const props = defineProps({ user: Object });
const isAdmin = () => props.user && props.user.role === 'ADMIN';

const loading = ref(true);
const pet = ref(null);
const expMax = computed(() => {
  if (!pet.value) return 100;
  return (pet.value.level || 1) * 100;
});
const expPercent = computed(() => {
  if (!pet.value) return 0;
  return Math.min(100, Math.round((pet.value.exp / expMax.value) * 100));
});
const modes = ref([]);
const types = ref([]);
const personalities = ['活泼', '高冷', '粘人', '贪吃', '傲娇'];
const sw = ref({ typeCode: 'CAT', gender: 'MALE', personality: '活泼' });
const playMsg = ref('');
const playErr = ref('');
const drawName = ref('');
const drawMsg = ref('');
const drawErr = ref('');
const needReflection = ref(false);
const q1 = ref('');
const q2 = ref('');
const r1 = ref('');
const r2 = ref('');
const refErr = ref('');
const refOk = ref('');

const inventory = ref([]);
const feedMsg = ref('');
const feedErr = ref('');
const bubbleVisible = ref(false);
const bubbleText = ref('');
let bubbleTimer = null;

const EMOJI_MAP = {
  '猫粮': '🐱', '狗粮': '🐶', '鱼': '🐟', '肉': '🍖', '骨头': '🦴',
  '胡萝卜': '🥕', '蛋糕': '🎂', '面包': '🍞', '牛奶': '🥛', '苹果': '🍎',
  '饼干': '🍪', '糖果': '🍬', '饭团': '🍙', '草莓': '🍓', '汉堡': '🍔',
};
function emojiOf(name) {
  for (const [k, v] of Object.entries(EMOJI_MAP)) {
    if (name && name.includes(k)) return v;
  }
  return '🍴';
}

function showBubble(text) {
  bubbleText.value = text;
  bubbleVisible.value = true;
  if (bubbleTimer) clearTimeout(bubbleTimer);
  bubbleTimer = setTimeout(() => { bubbleVisible.value = false; }, 3000);
}

function stageText(s) {
  return { BABY: '幼年', YOUNG: '少年', ADULT: '成年', ELDER: '老年' }[s] || s;
}

async function loadInventory() {
  try {
    inventory.value = await http.get('/api/shop/inventory');
  } catch (e) {
    inventory.value = [];
  }
}

async function load() {
  loading.value = true;
  try {
    pet.value = await http.get('/api/pet/my');
    modes.value = await http.get('/api/pet/play-modes');
    await loadInventory();
    if (isAdmin()) {
      types.value = await http.get('/api/pet/types');
      if (types.value.length) sw.value.typeCode = types.value[0].typeCode;
    }
    if (pet.value && !pet.value.hasPet) {
      const graves = await http.get('/api/gravestones');
      needReflection.value = graves.some((g) => g.answered === 0);
      if (needReflection.value) {
        const qs = await http.get('/api/reflection/questions');
        q1.value = qs.question1;
        q2.value = qs.question2;
      }
    }
  } catch (e) {
    pet.value = null;
  }
  loading.value = false;
}

async function play(mode) {
  playSound('play');
  playMsg.value = '';
  playErr.value = '';
  try {
    const data = await http.post('/api/pet/play', { mode });
    playMsg.value = data.message;
    // 直接用 API 返回的值更新状态,不做全量刷新
    if (data.mood != null) pet.value.mood = data.mood;
    if (data.hunger != null) pet.value.hunger = data.hunger;
    // 经验和等级可能变化,轻量更新
    pet.value.exp = (pet.value.exp || 0) + (modes.value.find(m => m.name === mode)?.exp || 0);
    while (pet.value.exp >= expMax.value) {
      pet.value.exp -= expMax.value;
      pet.value.level = (pet.value.level || 1) + 1;
    }
    // 触发台词气泡
    try {
      const bubble = await http.get('/api/chat/bubble?category=play');
      if (bubble && bubble.text) showBubble(bubble.text);
    } catch (e) {}
  } catch (e) {
    playErr.value = e.message;
  }
}

async function feed(inv) {
  playSound('button');
  feedMsg.value = '';
  feedErr.value = '';
  try {
    const data = await http.post('/api/shop/feed', { itemId: inv.itemId });
    feedMsg.value = data.message;
    // 更新背包数量和宠物状态
    inv.quantity--;
    if (inv.quantity <= 0) {
      inventory.value = inventory.value.filter(i => i.itemId !== inv.itemId);
    }
    // 轻量更新饥饿值
    if (pet.value) {
      try { pet.value = await http.get('/api/pet/my'); } catch (e) {}
    }
    // 触发吃东西台词
    try {
      const bubble = await http.get('/api/chat/bubble?category=happy');
      if (bubble && bubble.text) showBubble(bubble.text);
    } catch (e) {}
  } catch (e) {
    feedErr.value = e.message;
  }
}

async function toggleListed() {
  playSound('button');
  playErr.value = '';
  playMsg.value = '';
  try {
    const data = await http.post('/api/pet/listed', { listed: pet.value.listed === 1 ? false : true });
    playMsg.value = data.message;
    pet.value.listed = pet.value.listed === 1 ? 0 : 1;
  } catch (e) {
    playErr.value = e.message;
  }
}

async function switchPet() {
  playSound('button');
  playErr.value = '';
  playMsg.value = '';
  try {
    const data = await http.post('/api/admin/switch-pet', sw.value);
    playMsg.value = '已切换为 ' + data.typeName + '(' + data.subtypeName + ') ' + (data.gender === 'MALE' ? '雄' : '雌') + ' ' + data.personality;
    await load();
    emit('pet-switched');
  } catch (e) {
    playErr.value = e.message;
  }
}

async function draw() {
  playSound('button');
  drawMsg.value = '';
  drawErr.value = '';
  try {
    const data = await http.post('/api/pet/draw', { petName: drawName.value });
    drawMsg.value = `🎉 抽到 ${data.typeName}(${data.subtypeName}) · ${data.gender === 'MALE' ? '雄' : '雌'} · ${data.personality} · ${rarityText(data.rarity)}(名字只能起一次)`;
    drawName.value = '';
    await load();
    emit('pet-switched');
  } catch (e) {
    drawErr.value = e.message;
  }
}

async function submitReflection() {
  playSound('button');
  refErr.value = '';
  refOk.value = '';
  try {
    await http.post('/api/reflection/submit', { reflection1: r1.value, reflection2: r2.value });
    refOk.value = '反思已铭记于心。现在可以迎接新的伙伴了。';
    needReflection.value = false;
  } catch (e) {
    refErr.value = e.message;
  }
}

onMounted(load);
</script>

<style scoped>
.pet-head {
  display: flex;
  gap: 14px;
  align-items: center;
}
.stats {
  display: flex;
  gap: 24px;
  margin: 12px 0;
}
.stat {
  background: #fff3e0;
  border-radius: 10px;
  padding: 6px 14px;
}
.modes {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
  margin: 8px 0;
}
.mode {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 4px;
  font-size: 13px;
}
.mode .icon {
  font-size: 22px;
}
.mode-img {
  width: 34px;
  height: 34px;
  object-fit: cover;
  border-radius: 8px;
}
.admin-box {
  margin-top: 12px;
  padding: 10px;
  background: #fdf1e3;
  border-radius: 10px;
}
select {
  padding: 6px 8px;
  border: 1px solid #e5d5c0;
  border-radius: 8px;
  font-size: 13px;
  background: #fffdf8;
  flex: 1;
}
.reflect {
  background: #f7efe3;
  margin: 10px 0;
}
.small {
  font-size: 12px;
}

/* 进度条样式 */
.pet-stats-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 12px 0;
}
.stat-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.stat-bar-label {
  width: 64px;
  flex-shrink: 0;
  text-align: right;
}
.stat-bar-track {
  flex: 1;
  height: 16px;
  background: #f0e6d6;
  border-radius: 8px;
  overflow: hidden;
  border: 1.5px solid #e5d5c0;
}
.stat-bar-fill {
  height: 100%;
  border-radius: 7px;
  transition: width 0.4s ease;
}
.stat-bar-fill.hunger {
  background: linear-gradient(90deg, #ff9f45, #ffb86c);
}
.stat-bar-fill.mood {
  background: linear-gradient(90deg, #ff6b9d, #ff9ec7);
}
.stat-bar-fill.exp {
  background: linear-gradient(90deg, #5b9a8b, #7dc4b3);
}
.stat-bar-val {
  width: auto;
  min-width: 70px;
  flex-shrink: 0;
  font-weight: bold;
  font-size: 12px;
}
.stat-bar-val.coins {
  color: #e8a93c;
}

/* 台词气泡 */
.pet-speech-bubble {
  background: #fff;
  border: 2px solid #ffb86c;
  border-radius: 16px;
  padding: 10px 18px;
  margin: 8px 0;
  font-size: 14px;
  color: #6b4f3a;
  position: relative;
  animation: bubble-pop 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 159, 69, 0.15);
}
.pet-speech-bubble::before {
  content: '💬';
  margin-right: 6px;
}
@keyframes bubble-pop {
  0% { transform: scale(0.8); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
.bubble-fade-enter-active, .bubble-fade-leave-active {
  transition: opacity 0.3s ease;
}
.bubble-fade-enter-from, .bubble-fade-leave-to {
  opacity: 0;
}

/* 背包网格 */
.inv-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 8px;
  margin: 8px 0;
}
.inv-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px;
  background: #fff8ef;
  border: 1.5px solid #f0e0cc;
  border-radius: 12px;
  font-size: 13px;
}
.inv-emoji {
  font-size: 28px;
}
.inv-name {
  font-weight: 600;
  color: #6b4f3a;
}
.inv-qty {
  color: #e8a93c;
  font-weight: bold;
}
.feed-btn {
  padding: 4px 12px;
  font-size: 12px;
  background: #ff9f45;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 4px;
}
.feed-btn:hover {
  background: #ff8c1a;
}
</style>
