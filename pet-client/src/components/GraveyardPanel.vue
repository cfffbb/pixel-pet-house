<template>
  <div class="card">
    <h3>🪦 墓地</h3>
    <p class="muted">每一只离开的伙伴都值得纪念。点击墓碑可查看它的反思记录。</p>
    <div v-for="g in graves" :key="g.id" class="grave" @click="playSound('button'); g.expanded = !g.expanded">
      <div class="row head">
        <span>
          🪦 <b>{{ g.petName }}</b>
          <span class="muted">
            {{ g.typeName }}({{ g.subtypeName }}) · {{ g.gender === 'MALE' ? '公' : '母' }} · {{ g.personality }} ·
            存活 {{ g.lifespanDays }} 天 · {{ fmt(g.diedAt) }}
          </span>
        </span>
        <span :class="g.answered === 1 ? 'ok' : 'error'">{{ g.answered === 1 ? '已反思' : '未反思' }}</span>
      </div>
      <div v-if="g.expanded && g.answered === 1" class="reflect">
        <p class="muted">反思 1:{{ g.reflection1 }}</p>
        <p class="muted">反思 2:{{ g.reflection2 }}</p>
      </div>
      <div v-else-if="g.expanded" class="reflect error">还没有反思记录。</div>
    </div>
    <p v-if="!graves.length" class="muted">还没有离开的伙伴。</p>
    <p v-if="err" class="error">{{ err }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import http from '../api/http';
import { useSound } from '../soundStore';

const { playSound } = useSound();

const graves = ref([]);
const err = ref('');

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '';
}

onMounted(async () => {
  try {
    graves.value = await http.get('/api/gravestones');
  } catch (e) {
    err.value = e.message;
  }
});
</script>

<style scoped>
.grave {
  padding: 10px 6px;
  border-bottom: 1px dashed #f0e0c8;
  cursor: pointer;
}
.grave:hover {
  background: #fbf3e6;
}
.reflect {
  margin-top: 6px;
  padding: 8px;
  background: #f7efe3;
  border-radius: 8px;
}
</style>
