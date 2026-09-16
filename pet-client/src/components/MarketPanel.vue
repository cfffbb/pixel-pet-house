<template>
  <div>
    <!-- 我的宠物 · 市场状态 -->
    <div class="card">
      <h3>📋 我的宠物 · 市场状态</h3>
      <div v-if="myPet && myPet.hasPet">
        <div class="row">
          <PetFigure :type-code="myPet.typeCode" :gender="myPet.gender" :stage="myPet.stage" :size="56" />
          <div>
            <b>{{ myPet.petName }}</b>
            <span class="muted">({{ myPet.typeName }} · {{ myPet.gender === 'MALE' ? '雄' : '雌' }} · {{ stageText(myPet.stage) }})</span>
            <span :class="myPet.listed === 1 ? 'ok' : 'error'"> {{ myPet.listed === 1 ? '● 已上架' : '○ 未上架' }}</span>
          </div>
          <button v-if="myPet.gender === 'MALE'" @click="toggleListed">{{ myPet.listed === 1 ? '下架' : '上架到市场' }}</button>
        </div>
        <ul class="checks">
          <li v-for="c in checks" :key="c.label" :class="c.ok ? 'ok' : 'error'">
            {{ c.ok ? '✓' : '✗' }} {{ c.label }}{{ c.ok ? '' : ':' + c.reason }}
          </li>
        </ul>
        <p v-if="myMsg" class="ok">{{ myMsg }}</p>
        <p v-if="myErr" class="error">{{ myErr }}</p>
      </div>
      <p v-else class="muted">你还没有宠物,不能上架或配种。</p>
    </div>

    <!-- 市场卡片 -->
    <div class="card">
      <h3>🧬 配种市场(已上架的宠物)</h3>
      <div class="row filter">
        <select v-model="species" @change="loadMarket">
          <option value="">全部种类</option>
          <option v-for="t in types" :key="t.typeCode" :value="t.typeCode">{{ t.typeName }}</option>
        </select>
        <select v-model="sort" @change="loadMarket">
          <option value="newest">最新上架</option>
          <option value="level">等级从高到低</option>
          <option value="rarity">稀有度从高到低</option>
        </select>
        <span class="muted small">共 {{ total }} 只上架</span>
      </div>
      <div v-if="!records.length" class="muted">市场暂无上架宠物(雄宠成熟 3 天并点上架后才会出现)。</div>
      <div class="grid">
        <div v-for="p in records" :key="p.petId" class="card pet-card">
          <div class="badge" :class="p.rarity === 'LEGEND' ? 'legend' : p.rarity === 'RARE' ? 'rare' : 'normal'">
            {{ rarityText(p.rarity) }}
          </div>
          <div class="listed-tag">● 已上架</div>
          <PetFigure :type-code="p.typeCode" :gender="p.gender" :stage="stageOf(p.ageDays)" :size="88" />
          <div class="name"><b>{{ p.petName }}</b></div>
          <div class="owner">主人:{{ p.ownerNickname || p.ownerUsername }}</div>
          <div class="muted intro">{{ p.personality }} · {{ p.typeName }}({{ p.subtypeName }}) · Lv{{ p.level }}</div>
          <div v-if="p.expanded" class="detail">
            <div class="muted small">饥饿 {{ p.hunger }} · 心情 {{ p.mood }} · 金币 {{ p.coins }}</div>
            <div class="muted small">年龄 {{ p.ageDays }} 天 · {{ stageText(stageOf(p.ageDays)) }} · {{ p.gender === 'MALE' ? '雄' : '雌' }}</div>
          </div>
          <button class="link" @click="playSound('button'); p.expanded = !p.expanded">{{ p.expanded ? '收起属性' : '展开属性' }}</button>
          <div class="row actions">
            <button @click="request(p)">💌 申请配种</button>
            <button class="ghost" @click="addFriend(p)">➕ 加好友</button>
            <button class="ghost" @click="chat(p)">💬 私聊</button>
          </div>
        </div>
      </div>
      <div class="row" v-if="total > size">
        <button class="ghost" @click="playSound('button'); page > 1 && (page--, loadMarket())" :disabled="page <= 1">上一页</button>
        <span class="muted">第 {{ page }} 页</span>
        <button class="ghost" @click="playSound('button'); page++, loadMarket()" :disabled="page * size >= total">下一页</button>
      </div>
      <p v-if="reqMsg" class="ok">{{ reqMsg }}</p>
      <p v-if="reqErr" class="error">{{ reqErr }}</p>
    </div>

    <!-- 我的配种记录 -->
    <div class="card">
      <h3>📜 我的配种记录</h3>
      <div v-for="r in records2" :key="r.recordId" class="row item">
        <span>
          {{ r.motherPetName }} × {{ r.fatherPetName }}
          <span class="muted">{{ statusText(r.status) }} · {{ fmt(r.createdAt) }}</span>
        </span>
      </div>
      <p v-if="!records2.length" class="muted">还没有配种记录。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import http from '../api/http';
import PetFigure from './PetFigure.vue';
import { rarityText } from '../constants';
import { useSound } from '../soundStore';

const { playSound } = useSound();

const emit = defineEmits(['chat']);

const records = ref([]);
const records2 = ref([]);
const types = ref([]);
const myPet = ref(null);
const species = ref('');
const sort = ref('newest');
const page = ref(1);
const size = 9;
const total = ref(0);
const myMsg = ref('');
const myErr = ref('');
const reqMsg = ref('');
const reqErr = ref('');
const allowBreeding = ref(true);

function fmt(t) { return t ? String(t).replace('T', ' ').slice(5, 16) : ''; }
function statusText(s) { return { PENDING: '待对方同意', PREGNANT: '怀孕中', BORN: '已产崽(去托管所看)', REJECTED: '已拒绝' }[s] || s; }
function stageText(s) { return { BABY: '幼年', YOUNG: '少年', ADULT: '成年', ELDER: '老年' }[s] || s; }
function stageOf(days) {
  if (days < 1) return 'BABY';
  if (days < 3) return 'YOUNG';
  if (days < 60) return 'ADULT';
  return 'ELDER';
}

const checks = computed(() => {
  const list = [];
  if (!myPet.value || !myPet.value.hasPet) {
    list.push({ label: '有宠物', ok: false, reason: '先去抽蛋领养一只' });
    return list;
  }
  list.push({ label: '宠物活着', ok: myPet.value.status !== 'STARVED', reason: '已死亡' });
  list.push({ label: '已成熟(出生满 3 天)', ok: myPet.value.mature, reason: '未成熟,还差 ' + Math.max(0, 3 - myPet.value.ageDays) + ' 天' });
  if (myPet.value.gender === 'MALE') {
    list.push({ label: '是雄性(可上架)', ok: true, reason: '' });
    list.push({ label: '不在配种冷却', ok: !myPet.value.breedingCoolUntil, reason: '冷却中' });
  } else {
    list.push({ label: '是雌性(可发起配种)', ok: true, reason: '' });
    list.push({ label: '不在怀孕', ok: !myPet.value.pregnantUntil, reason: '怀孕中' });
  }
  return list;
});

async function load() {
  try {
    myPet.value = await http.get('/api/pet/my');
    types.value = await http.get('/api/pet/types');
    records2.value = await http.get('/api/breeding/my');
    const s = await http.get('/api/settings');
    allowBreeding.value = s.allowBreeding;
  } catch (e) {
    myErr.value = e.message;
  }
}

async function loadMarket() {
  try {
    const data = await http.get('/api/breeding/market', {
      params: { species: species.value, sort: sort.value, page: page.value, size },
    });
    records.value = data.records;
    total.value = data.total;
  } catch (e) {
    reqErr.value = e.message;
  }
}

async function toggleListed() {
  playSound('button');
  myErr.value = '';
  myMsg.value = '';
  try {
    const data = await http.post('/api/pet/listed', { listed: myPet.value.listed === 1 ? false : true });
    myMsg.value = data.message;
    await load();
  } catch (e) {
    myErr.value = e.message;
  }
}

async function request(p) {
  playSound('button');
  reqMsg.value = '';
  reqErr.value = '';
  try {
    await http.post('/api/breeding/request', { fatherPetId: p.petId });
    reqMsg.value = '已向 ' + (p.ownerNickname || p.ownerUsername) + ' 发送配种申请(已扣 10 币),等对方在收件箱同意';
    await load();
  } catch (e) {
    reqErr.value = e.message;
  }
}

async function addFriend(p) {
  playSound('button');
  reqErr.value = '';
  reqMsg.value = '';
  try {
    await http.post('/api/friends/request', { targetUsername: p.ownerUsername });
    reqMsg.value = '已向 ' + (p.ownerNickname || p.ownerUsername) + ' 发送好友申请';
  } catch (e) {
    reqErr.value = e.message;
  }
}

function chat(p) {
  playSound('button');
  emit('chat', { userId: p.ownerUserId, username: p.ownerUsername, nickname: p.ownerNickname });
}

onMounted(async () => {
  await load();
  await loadMarket();
});
</script>

<style scoped>
.filter { margin-bottom: 8px; }
select {
  padding: 6px 8px;
  border: 1px solid #e5d5c0;
  border-radius: 8px;
  font-size: 13px;
  background: #fffdf8;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
  margin-top: 10px;
}
.pet-card {
  position: relative;
  text-align: center;
  padding: 14px 10px;
}
.badge {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 8px;
  color: #fff;
}
.badge.legend { background: #e8a33d; }
.badge.rare { background: #7fbf7f; }
.badge.normal { background: #b8b0a8; }
.listed-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  color: #2e8b57;
  background: #e3f2e3;
  padding: 2px 8px;
  border-radius: 8px;
}
.name { margin-top: 4px; font-size: 15px; }
.owner { font-size: 13px; color: #8a6d3b; margin: 2px 0; }
.intro { font-size: 12px; }
.detail { margin: 4px 0; }
.actions { justify-content: center; margin-top: 6px; flex-wrap: wrap; }
button.ghost {
  background: transparent;
  border: 1px solid #e5d5c0;
  color: #9a8a75;
}
button.link {
  background: transparent;
  color: #ff9f45;
  font-size: 12px;
  padding: 2px;
}
.checks { list-style: none; padding: 0; margin: 8px 0; font-size: 13px; }
.item { justify-content: space-between; padding: 6px 0; border-bottom: 1px dashed #f0e0c8; }
.small { font-size: 12px; }
</style>
