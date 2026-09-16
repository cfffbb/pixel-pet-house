<template>
  <div>
    <div class="card">
      <h3>📁 一键桌面整理</h3>
      <p class="muted">
        扫描桌面文件 → 按类型移动到桌面内的分区文件夹。只动文件、不动文件夹;执行前先给你看清单;可一键撤销。
      </p>
      <div class="row">
        <button @click="scan">🔍 扫描桌面</button>
        <button v-if="plan.length" @click="organize">✅ 执行整理({{ plan.length }} 个文件)</button>
        <button class="ghost" @click="undo">↩️ 撤销上次整理</button>
      </div>
      <p v-if="msg" class="ok">{{ msg }}</p>
      <p v-if="err" class="error">{{ err }}</p>
    </div>

    <div v-if="plan.length" class="card">
      <h3>将要移动的文件({{ plan.length }} 个)</h3>
      <div v-for="p in plan" :key="p.from" class="row plan-row">
        <span>{{ p.fileName }}</span>
        <span class="muted">→ {{ p.zone }}/</span>
      </div>
    </div>

    <div class="card">
      <h3>🗂 分区规则(可自定义)</h3>
      <div v-for="z in zones" :key="z.id" class="row zone-row">
        <span>
          <b>{{ z.zoneName }}</b>
          <span class="muted">{{ z.extensions || '(兜底:其他)' }}</span>
          <span :class="z.enabled === 1 ? 'ok' : 'error'">{{ z.enabled === 1 ? '启用' : '停用' }}</span>
        </span>
        <button class="ghost" @click="removeZone(z)">删</button>
      </div>
      <div class="row add">
        <input v-model="newZone.zoneName" placeholder="分区名,如 代码" />
        <input v-model="newZone.extensions" placeholder="扩展名,逗号分隔,如 java,py" />
        <button @click="addZone">添加分区</button>
      </div>
      <p v-if="zoneErr" class="error">{{ zoneErr }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import http from '../api/http';
import { useSound } from '../soundStore';

const { playSound } = useSound();

const zones = ref([]);
const plan = ref([]);
const msg = ref('');
const err = ref('');
const zoneErr = ref('');
const newZone = ref({ zoneName: '', extensions: '' });

async function loadZones() {
  try {
    zones.value = await http.get('/api/desktop/zones');
  } catch (e) {
    zoneErr.value = e.message;
  }
}

async function scan() {
  playSound('button');
  err.value = '';
  msg.value = '';
  if (!window.petAPI) {
    err.value = '请通过 Electron 桌面客户端使用本功能（浏览器打开不支持）。如果你已经在客户端中，请确保已登录。';
    return;
  }
  try {
    // 确保 token 已传给主进程
    const token = localStorage.getItem('pet_token');
    if (token) await window.petAPI.setToken(token);
    const result = await window.petAPI.scanDesktop();
    if (result.error) {
      err.value = result.error;
      return;
    }
    zones.value = result.zones || [];
    plan.value = result.plan || [];
    msg.value = plan.value.length ? `扫描完成，发现 ${plan.value.length} 个可整理文件` : '桌面已经很干净了';
  } catch (e) {
    err.value = '扫描失败：' + (e.message || e);
  }
}

async function organize() {
  playSound('button');
  err.value = '';
  if (!window.petAPI) {
    err.value = '请通过 Electron 桌面客户端使用本功能';
    return;
  }
  if (!window.confirm('确认把 ' + plan.value.length + ' 个文件移动到对应分区文件夹?')) return;
  try {
    const token = localStorage.getItem('pet_token');
    if (token) await window.petAPI.setToken(token);
    // 转成纯对象,避免响应式 Proxy 导致 Electron 结构化克隆失败
    const plainPlan = plan.value.map(p => ({
      fileName: p.fileName,
      ext: p.ext,
      zone: p.zone,
      from: p.from,
      to: p.to,
    }));
    const result = await window.petAPI.organizeDesktop(plainPlan);
    msg.value = `整理完成,移动了 ${result.moved} 个文件`;
    if (result.errors && result.errors.length) {
      err.value = '以下文件移动失败:\n' + result.errors.join('\n');
    }
    plan.value = [];
    await loadZones();
  } catch (e) {
    err.value = '整理失败:' + (e.message || e);
  }
}

async function undo() {
  playSound('button');
  err.value = '';
  msg.value = '';
  if (!window.petAPI) {
    err.value = '请通过 Electron 桌面客户端使用本功能';
    return;
  }
  try {
    const result = await window.petAPI.undoDesktop();
    msg.value = result.undone > 0 ? `已撤销,移回 ${result.undone} 个文件` : '没有可撤销的整理记录';
  } catch (e) {
    err.value = '撤销失败:' + (e.message || e);
  }
}

async function addZone() {
  playSound('button');
  zoneErr.value = '';
  try {
    await http.post('/api/desktop/zones', {
      zoneName: newZone.value.zoneName,
      extensions: newZone.value.extensions,
    });
    newZone.value = { zoneName: '', extensions: '' };
    await loadZones();
  } catch (e) {
    zoneErr.value = e.message;
  }
}

async function removeZone(z) {
  playSound('button');
  zoneErr.value = '';
  try {
    await http.delete('/api/desktop/zones/' + z.id);
    await loadZones();
  } catch (e) {
    zoneErr.value = e.message;
  }
}

onMounted(loadZones);
</script>

<style scoped>
.plan-row {
  justify-content: space-between;
  padding: 4px 0;
  border-bottom: 1px dashed var(--border, #f0e0c8);
  font-size: 14px;
}
.zone-row {
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px dashed var(--border, #f0e0c8);
}
.add {
  margin-top: 10px;
}
button.ghost {
  background: transparent;
  border: 1px solid var(--border, #e5d5c0);
  color: var(--muted, #9a8a75);
}
</style>
