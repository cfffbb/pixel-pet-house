<template>
  <div class="card">
    <h3>🏠 托管所</h3>
    <p class="muted">
      幼崽出生后寄养在这里,每只每天 5 币托管费(欠费冻结,补交后解冻);老宠死亡并完成反思答题后才能领取(保持同时只养一只);可赠送他人。
    </p>
    <div v-for="h in list" :key="h.id" class="row item">
      <span>
        {{ h.typeName }}({{ h.subtypeName }}) · {{ rarityText(h.rarity) }} · {{ h.gender === 'MALE' ? '公' : '母' }} ·
        {{ h.personality }}
        <span :class="h.status === 'STORED' ? 'ok' : 'error'">
          {{ h.status === 'STORED' ? '托管中' : h.status === 'FROZEN' ? '欠费冻结' : '已领取' }}
        </span>
      </span>
      <div class="row">
        <button v-if="h.status === 'STORED'" @click="claim(h)">领取</button>
        <button v-if="h.status === 'STORED'" class="ghost" @click="gift(h)">赠送</button>
      </div>
    </div>
    <p v-if="!list.length" class="muted">托管所空空如也。</p>
    <p v-if="msg" class="ok">{{ msg }}</p>
    <p v-if="err" class="error">{{ err }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import http from '../api/http';
import { rarityText } from '../constants';
import { useSound } from '../soundStore';

const { playSound } = useSound();

const list = ref([]);
const msg = ref('');
const err = ref('');

async function load() {
  try {
    list.value = await http.get('/api/hatchling/my');
  } catch (e) {
    err.value = e.message;
  }
}

async function claim(h) {
  playSound('button');
  msg.value = '';
  err.value = '';
  const petName = window.prompt('给幼崽起个名字(必填,最多 12 字):');
  if (!petName) return;
  try {
    const data = await http.post('/api/hatchling/claim', { hatchlingId: h.id, petName });
    msg.value = '领养成功:' + data.typeName + ' ' + data.rarity;
    await load();
  } catch (e) {
    err.value = e.message;
  }
}

async function gift(h) {
  playSound('button');
  msg.value = '';
  err.value = '';
  const username = window.prompt('要送给谁?(对方用户名)');
  if (!username) return;
  try {
    await http.post('/api/hatchling/gift', { hatchlingId: h.id, targetUsername: username });
    msg.value = '已发送赠送申请,等对方在收件箱同意';
  } catch (e) {
    err.value = e.message;
  }
}

onMounted(load);
</script>

<style scoped>
.item {
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px dashed #f0e0c8;
}
button.ghost {
  background: transparent;
  border: 1px solid #e5d5c0;
  color: #9a8a75;
}
</style>
