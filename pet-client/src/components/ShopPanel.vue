<template>
  <div>
    <div class="card">
      <h3>🪙 我的游戏币:<b>{{ coins }}</b> <span class="muted small">(管理员购买不扣币)</span></h3>
    </div>

    <div class="card">
      <h3>🏪 商店(按宠物物种与等级)</h3>
      <p class="muted small">只有你宠物能吃的食物才显示;高级食物要等级解锁。</p>
      <div class="grid">
        <div v-for="it in available" :key="it.id" class="item" :class="{ new: newly.has(it.id) }">
          <img v-if="it.image" :src="fileUrl(it.image)" class="item-img" alt="" />
          <div v-else class="name-emoji">{{ emojiOf(it.itemName) }}</div>
          <div class="name">{{ it.itemName }}</div>
          <div class="muted small">
            {{ it.price }} 币
            <span v-if="it.hungerRestore > 0"> · 饥饿+{{ it.hungerRestore }}</span>
            <span v-if="it.moodRestore > 0"> · 心情+{{ it.moodRestore }}</span>
          </div>
          <button @click="buy(it)">购买</button>
        </div>
      </div>
      <h4 class="lock-title">🔒 等级解锁</h4>
      <div class="grid">
        <div v-for="it in locked" :key="it.id" class="item locked">
          <img v-if="it.image" :src="fileUrl(it.image)" class="item-img" alt="" />
          <div v-else class="name-emoji">🔒</div>
          <div class="name">🔒 {{ it.itemName }}</div>
          <div class="muted small">Lv{{ it.unlockLevel }} 解锁 · {{ it.price }} 币
            <span v-if="it.hungerRestore > 0"> · 饥饿+{{ it.hungerRestore }}</span>
          </div>
        </div>
      </div>
      <p v-if="!available.length && !locked.length" class="muted">商店暂时缺货。</p>
      <p v-if="msg" class="ok">{{ msg }}</p>
      <p v-if="err" class="error">{{ err }}</p>
    </div>

    <div class="card">
      <h3>🎒 宠物背包</h3>
      <div v-for="inv in inventory" :key="inv.itemId" class="row inv-row">
        <span>{{ emojiOf(inv.itemName) }} {{ inv.itemName }} × {{ inv.quantity }}</span>
        <button v-if="inv.itemType === 'FOOD' && inv.quantity > 0" @click="feed(inv)">🍖 喂食</button>
      </div>
      <p v-if="!inventory.length" class="muted">背包空空,先去商店买点东西吧。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import http, { fileUrl } from '../api/http';
import { useSound } from '../soundStore';

const { playSound } = useSound();

const coins = ref(0);
const available = ref([]);
const locked = ref([]);
const inventory = ref([]);
const newly = ref(new Set());
const msg = ref('');
const err = ref('');
let prevLockedIds = new Set();

function emojiOf(name) {
  return {
    小鱼干: '🐟', 肉骨头: '🍖', 豪华大餐: '🍱', 玩具球: '🎾',
    牛排: '🥩', 三文鱼: '🍣', 龙晶果: '🔮', 瓜子: '🌻', 胡萝卜: '🥕',
    牧草团: '🌿', 鲜肉大餐: '🥘', 龙果: '🍎', 小老鼠: '🐁', 苹果: '🍎',
    青草饼: '🥞', 香蕉: '🍌', 虫子串: '🐛', 大棒骨: '🍖', 南瓜: '🎃', 猫罐头: '🥫',
  }[name] || '📦';
}

async function load() {
  try {
    const pet = await http.get('/api/pet/my');
    coins.value = pet.hasPet ? pet.coins : 0;
    const data = await http.get('/api/shop/items');
    // 解锁动画:之前锁定、现在可用的物品
    const newUnlocked = new Set();
    for (const it of data.available) {
      if (prevLockedIds.has(it.id)) newUnlocked.add(it.id);
    }
    if (newUnlocked.size) {
      newly.value = newUnlocked;
      setTimeout(() => (newly.value = new Set()), 2500);
    }
    prevLockedIds = new Set(data.locked.map((i) => i.id));
    available.value = data.available;
    locked.value = data.locked;
    inventory.value = await http.get('/api/shop/inventory');
  } catch (e) {
    err.value = e.message;
  }
}

async function buy(it) {
  playSound('button');
  err.value = '';
  try {
    await http.post('/api/shop/buy', { itemId: it.id, quantity: 1 });
    msg.value = '已购买 ' + it.itemName;
    await load();
  } catch (e) {
    err.value = e.message;
  }
}

async function feed(inv) {
  playSound('button');
  msg.value = '';
  err.value = '';
  try {
    const data = await http.post('/api/shop/feed', { itemId: inv.itemId });
    msg.value = data.message;
    await load();
  } catch (e) {
    err.value = e.message;
  }
}

onMounted(load);
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 10px;
}
.item {
  border: 1px solid #f0e0c8;
  border-radius: 10px;
  padding: 10px;
  text-align: center;
  background: #fffdf8;
}
.item.new {
  animation: unlock 2.5s ease-out;
  border-color: #ff9f45;
}
.item.locked {
  opacity: 0.55;
  background: #f5f0e8;
}
@keyframes unlock {
  0% { transform: scale(0.7); background: #ffe9b8; }
  30% { transform: scale(1.08); background: #ffe9b8; }
  100% { transform: scale(1); }
}
.name { font-weight: bold; margin-bottom: 4px; }
.name-emoji { font-size: 30px; }
.item-img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 10px;
  margin-bottom: 4px;
}
.lock-title { margin: 12px 0 6px; }
.inv-row { justify-content: space-between; padding: 6px 0; border-bottom: 1px dashed #f0e0c8; }
.small { font-size: 12px; }
</style>
