<template>
  <div class="pomo-wrap">
    <!-- 左栏：任务列表 -->
    <div class="pomo-left">
      <div class="card pomo-tasks-card">
        <div class="pomo-tasks-header">
          <h3>📋 我的任务</h3>
          <button class="pix-btn btn-soft btn-sm" @click="showAddTask = !showAddTask">+ 新建</button>
        </div>

        <!-- 新建任务 -->
        <div v-if="showAddTask" class="task-add-form">
          <input v-model="newTask.title" placeholder="任务名称..." maxlength="50" @keyup.enter="createTask" class="task-input" />
          <div class="task-add-row">
            <select v-model="newTask.tag" class="task-tag-select">
              <option value="">无标签</option>
              <option value="学习">学习</option>
              <option value="工作">工作</option>
              <option value="运动">运动</option>
              <option value="阅读">阅读</option>
              <option value="其他">其他</option>
            </select>
            <input v-model.number="newTask.estimateCount" type="number" min="1" max="20" class="task-est-input" title="预估番茄数" />
            <span class="muted">🍅</span>
            <input v-model="newTask.planDate" type="date" class="task-date-input" />
            <button class="pix-btn btn-primary btn-sm" @click="createTask">添加</button>
          </div>
        </div>

        <!-- 视图切换 -->
        <div class="task-tabs">
          <button class="task-tab" :class="{ active: taskView === 'today' }" @click="taskView = 'today'; loadTasks()">今日</button>
          <button class="task-tab" :class="{ active: taskView === 'todo' }" @click="taskView = 'todo'; loadTasks()">待办</button>
          <button class="task-tab" :class="{ active: taskView === 'done' }" @click="taskView = 'done'; loadTasks()">已完成</button>
          <button class="task-tab" :class="{ active: taskView === 'all' }" @click="taskView = 'all'; loadTasks()">全部</button>
        </div>

        <!-- 任务列表 -->
        <div class="task-list">
          <div v-for="t in tasks" :key="t.id" class="task-item" :class="{ done: t.status === 1, selected: selectedTaskId === t.id }" @click="selectTask(t)">
            <button class="task-check" @click.stop="toggleTask(t)" :class="{ checked: t.status === 1 }">
              <span v-if="t.status === 1">✓</span>
            </button>
            <div class="task-info">
              <div class="task-title">{{ t.title }}</div>
              <div class="task-meta">
                <span v-if="t.tag" class="task-tag">{{ t.tag }}</span>
                <span class="task-tomato">🍅 {{ t.doneCount || 0 }}/{{ t.estimateCount || 1 }}</span>
                <span v-if="t.planDate" class="task-date">{{ t.planDate }}</span>
              </div>
            </div>
            <button class="task-del" @click.stop="deleteTask(t)">×</button>
          </div>
          <p v-if="!tasks.length" class="muted task-empty">暂无任务，点击右上角新建</p>
        </div>
      </div>
    </div>

    <!-- 右栏：计时器 + 统计 -->
    <div class="pomo-right">
      <!-- 计时器 -->
      <div class="card pomo-timer-card">
        <div v-if="state === 'idle'" class="pomo-setup">
          <div class="timer-ring idle">
            <div class="timer-inner">
              <div class="timer-num">{{ selectedTask ? selectedTask.title.slice(0, 6) : '准备' }}</div>
              <div class="timer-label">{{ selectedTask ? '已选任务' : '选择任务或直接开始' }}</div>
            </div>
          </div>
          <div class="pomo-setup-row">
            <div class="field">
              <label>专注时长</label>
              <div class="duration-pills">
                <button v-for="d in [15, 25, 30, 45, 60]" :key="d" class="duration-pill" :class="{ active: minutes === d }" @click="minutes = d">{{ d }}分</button>
              </div>
            </div>
          </div>
          <div class="field">
            <label>备注(可选)</label>
            <input v-model="label" placeholder="给这轮专注起个名字..." maxlength="20" class="task-input" />
          </div>
          <button class="pix-btn btn-primary btn-block pomo-start-btn" @click="start">▶️ 开始专注</button>
        </div>

        <div v-else class="pomo-active">
          <div class="timer-ring" :class="{ warn: remainSeconds <= 60, paused: state === 'paused' }">
            <svg class="timer-svg" viewBox="0 0 200 200">
              <circle class="timer-track" cx="100" cy="100" r="88" />
              <circle class="timer-progress" cx="100" cy="100" r="88" :stroke-dasharray="circumference" :stroke-dashoffset="progressOffset" />
            </svg>
            <div class="timer-inner">
              <div class="timer-num">{{ remainText }}</div>
              <div class="timer-label" v-if="label">{{ label }}</div>
            </div>
          </div>
          <p class="muted center">目标 {{ planMinutes }} 分钟 · {{ state === 'paused' ? '已暂停' : '专注中' }} · 暂停 {{ pauseCount }} 次</p>
          <div class="pomo-controls">
            <button v-if="state === 'running'" class="pix-btn btn-soft" @click="doPause">⏸ 暂停</button>
            <button v-if="state === 'paused'" class="pix-btn btn-primary" @click="doResume">▶ 继续</button>
            <button class="pix-btn btn-ghost" @click="doFinish">⏹ 完成</button>
            <button class="pix-btn btn-danger" @click="doAbandon">🚫 放弃</button>
          </div>
        </div>

        <p v-if="msg" class="ok">{{ msg }}</p>
        <p v-if="err" class="error">{{ err }}</p>
      </div>

      <!-- 统计 -->
      <div class="card pomo-stats-card">
        <h3>📊 专注统计</h3>
        <div class="stats-row">
          <div class="stat-item"><div class="stat-num">{{ stats.todayMinutes || 0 }}</div><div class="stat-label">今日分钟</div></div>
          <div class="stat-item"><div class="stat-num">{{ stats.todayCount || 0 }}</div><div class="stat-label">今日番茄</div></div>
          <div class="stat-item"><div class="stat-num">{{ stats.weekMinutes || 0 }}</div><div class="stat-label">本周分钟</div></div>
          <div class="stat-item"><div class="stat-num">{{ stats.totalCount || 0 }}</div><div class="stat-label">累计番茄</div></div>
        </div>
        <div class="chart-wrap">
          <div class="chart-title">最近 7 天</div>
          <div class="bar-chart">
            <div v-for="(m, i) in stats.dailyMinutes || []" :key="i" class="bar-col">
              <div class="bar-wrap"><div class="bar-fill" :style="{ height: barHeight(m) + '%' }"></div></div>
              <div class="bar-label">{{ stats.dailyLabels ? stats.dailyLabels[i] : '' }}</div>
              <div class="bar-val">{{ m }}分</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import http from '../api/http';
import { useSound } from '../soundStore';
import { useTheme } from '../themeStore';

const emit = defineEmits(['complete']);
const { playSound, playPomodoroEnd } = useSound();
const { timerDialUrl } = useTheme();

const state = ref('idle');
const minutes = ref(25);
const label = ref('');
const planMinutes = ref(0);
const pauseCount = ref(0);
const recordId = ref(null);
const remainText = ref('25:00');
const remainSeconds = ref(0);
const msg = ref('');
const err = ref('');
const stats = ref({});
let timer = null;
let endTime = 0;
let pausedRemain = 0;

const circumference = 2 * Math.PI * 88;
const progressOffset = computed(() => {
  if (!planMinutes.value) return circumference;
  const totalSeconds = planMinutes.value * 60;
  return circumference * (1 - remainSeconds.value / totalSeconds);
});

// 任务管理
const tasks = ref([]);
const showAddTask = ref(false);
const taskView = ref('today');
const selectedTaskId = ref(null);
const selectedTask = ref(null);
const newTask = ref({ title: '', tag: '', estimateCount: 1, planDate: new Date().toISOString().slice(0, 10) });

function mmss(sec) {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
}

function barHeight(m) {
  const max = Math.max(...(stats.value.dailyMinutes || [1]), 1);
  return Math.max(4, Math.round((m / max) * 100));
}

function tick() {
  if (state.value !== 'running') return;
  const remain = Math.max(0, Math.floor((endTime - Date.now()) / 1000));
  remainSeconds.value = remain;
  remainText.value = mmss(remain);
  if (remain <= 0) {
    clearInterval(timer);
    timer = null;
    autoComplete();
  }
}

async function start() {
  err.value = '';
  msg.value = '';
  playSound('button');
  if (!minutes.value || minutes.value < 1) { err.value = '请选择有效时长'; return; }
  try {
    const payload = { durationMinutes: minutes.value, label: label.value };
    if (selectedTaskId.value) payload.taskId = selectedTaskId.value;
    const data = await http.post('/api/pomodoro/start', payload);
    recordId.value = data.recordId;
    planMinutes.value = data.durationMinutes;
    pauseCount.value = 0;
    endTime = Date.now() + planMinutes.value * 60 * 1000;
    remainSeconds.value = planMinutes.value * 60;
    remainText.value = mmss(remainSeconds.value);
    state.value = 'running';
    timer = setInterval(tick, 500);
    if (window.petAPI) window.petAPI.setPomodoro(endTime);
  } catch (e) {
    err.value = e.message;
  }
}

async function doPause() {
  playSound('button');
  err.value = '';
  try {
    await http.post('/api/pomodoro/' + recordId.value + '/pause');
    pausedRemain = Math.max(0, Math.floor((endTime - Date.now()) / 1000));
    state.value = 'paused';
    pauseCount.value++;
    clearInterval(timer);
    timer = null;
    if (window.petAPI) window.petAPI.setPomodoro(null); // 暂停时停止悬浮窗倒计时,防止误触发完成通知
  } catch (e) { err.value = e.message === '已在暂停中' ? '时间已暂停,点继续即可' : '暂停失败: ' + e.message; }
}

async function doResume() {
  playSound('button');
  err.value = '';
  try {
    await http.post('/api/pomodoro/' + recordId.value + '/resume');
    endTime = Date.now() + pausedRemain * 1000;
    state.value = 'running';
    timer = setInterval(tick, 500);
    if (window.petAPI) window.petAPI.setPomodoro(endTime);
  } catch (e) { err.value = '继续失败: ' + e.message; }
}

async function doFinish() {
  playSound('button');
  err.value = '';
  try {
    const data = await http.post('/api/pomodoro/' + recordId.value + '/complete');
    msg.value = data.message || '专注完成!';
    if (window.petAPI) window.petAPI.setPomodoro(null); // 清除主进程计时器
    notify('🍅 专注完成', data.message || `获得 ${data.coins || 0} 游戏币`);
    playPomodoroEnd();
    emit('complete', { coins: data.coins || 0, label: label.value });
    stop();
    await loadTasks();
    await loadStats();
  } catch (e) { err.value = e.message; }
}

async function autoComplete() {
  state.value = 'finishing';
  try {
    const data = await http.post('/api/pomodoro/' + recordId.value + '/complete');
    msg.value = '🎉 ' + (data.message || '专注完成!');
    // 清除主进程倒计时,主进程会自动发通知
    if (window.petAPI) window.petAPI.setPomodoro(null);
    // 弹出系统通知(自动完成时也要提示)
    notify('🍅 专注完成', data.message || `获得 ${data.coins || 0} 游戏币`);
    playPomodoroEnd();
    emit('complete', { coins: data.coins || 0, label: label.value });
    stop();
    await loadTasks();
    await loadStats();
  } catch (e) {
    err.value = e.message;
    state.value = 'idle';
  }
}

async function doAbandon() {
  playSound('button');
  err.value = '';
  try {
    const data = await http.post('/api/pomodoro/' + recordId.value + '/abandon');
    msg.value = data.message || '已放弃,扣除部分游戏币';
    if (window.petAPI) window.petAPI.setPomodoro(null); // 清除主进程计时器
    stop();
    await loadTasks();
    await loadStats();
    emit('complete', { coins: data.coins || 0, label: label.value, abandoned: true });
  } catch (e) { err.value = e.message; }
}

function stop() {
  state.value = 'idle';
  clearInterval(timer);
  timer = null;
  label.value = '';
}

function notify(title, body) {
  // 优先用 Electron 主进程发系统通知(右下角弹窗)
  if (window.petAPI && window.petAPI.notifyPomodoro) {
    window.petAPI.notifyPomodoro(title, body);
  }
  // 同时用浏览器 Notification 作为后备
  try {
    if (window.Notification) {
      if (Notification.permission === 'granted') {
        new Notification(title, { body });
      } else if (Notification.permission !== 'denied') {
        Notification.requestPermission().then((perm) => {
          if (perm === 'granted') new Notification(title, { body });
        });
      }
    }
  } catch (e) {}
}

// 任务管理
async function loadTasks() {
  try {
    const today = new Date().toISOString().slice(0, 10);
    tasks.value = await http.get('/api/pomodoro/tasks', { params: { view: taskView.value, date: taskView.value === 'today' ? today : undefined } });
  } catch (e) { /* ignore */ }
}

async function createTask() {
  if (!newTask.value.title.trim()) return;
  playSound('button');
  try {
    await http.post('/api/pomodoro/tasks', newTask.value);
    newTask.value = { title: '', tag: '', estimateCount: 1, planDate: new Date().toISOString().slice(0, 10) };
    showAddTask.value = false;
    await loadTasks();
  } catch (e) { err.value = e.message; }
}

async function toggleTask(t) {
  playSound('button');
  try {
    await http.post('/api/pomodoro/tasks/' + t.id + '/toggle');
    await loadTasks();
  } catch (e) { err.value = e.message; }
}

async function deleteTask(t) {
  playSound('button');
  try {
    await http.delete('/api/pomodoro/tasks/' + t.id);
    if (selectedTaskId.value === t.id) { selectedTaskId.value = null; selectedTask.value = null; }
    await loadTasks();
  } catch (e) { err.value = e.message; }
}

function selectTask(t) {
  if (selectedTaskId.value === t.id) {
    selectedTaskId.value = null;
    selectedTask.value = null;
  } else {
    selectedTaskId.value = t.id;
    selectedTask.value = t;
  }
}

async function loadStats() {
  try { stats.value = await http.get('/api/pomodoro/stats'); } catch (e) {}
}

function onVisibilityChange() {
  if (document.hidden) return;
  if (state.value === 'running') {
    const remain = Math.max(0, Math.floor((endTime - Date.now()) / 1000));
    remainSeconds.value = remain;
    remainText.value = mmss(remain);
    if (window.petAPI) window.petAPI.setPomodoro(endTime);
  }
}

onMounted(async () => {
  await loadTasks();
  await loadStats();
  if (window.Notification && Notification.permission !== 'granted') Notification.requestPermission();
  document.addEventListener('visibilitychange', onVisibilityChange);
});

onBeforeUnmount(() => {
  clearInterval(timer);
  timer = null;
  document.removeEventListener('visibilitychange', onVisibilityChange);
});
</script>

<style scoped>
.pomo-wrap { display: flex; gap: 14px; }
.pomo-left { width: 45%; min-width: 280px; }
.pomo-right { flex: 1; }

/* 任务卡片 */
.pomo-tasks-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.pomo-tasks-header h3 { margin: 0; }
.task-add-form { background: var(--surface); border-radius: 10px; padding: 10px; margin-bottom: 10px; border: 2px solid var(--border); }
.task-add-row { display: flex; align-items: center; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
.task-input { width: 100%; padding: 8px 12px; border: 2px solid var(--border); border-radius: 10px; font-size: 14px; background: var(--bg); color: var(--fg); outline: none; }
.task-input:focus { border-color: var(--accent); }
.task-tag-select, .task-est-input, .task-date-input { padding: 5px 8px; border: 2px solid var(--border); border-radius: 8px; font-size: 12px; background: var(--bg); color: var(--fg); }
.task-est-input { width: 50px; }
.task-date-input { width: auto; }

.task-tabs { display: flex; gap: 4px; margin-bottom: 8px; }
.task-tab { padding: 5px 12px; border: 2px solid var(--border); border-radius: 8px; background: var(--surface); color: var(--muted); font-size: 13px; cursor: pointer; transition: all 0.2s; }
.task-tab.active { background: var(--accent); color: white; border-color: var(--accent); }

.task-list { max-height: 320px; overflow-y: auto; }
.task-item { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 10px; cursor: pointer; transition: background 0.2s; border: 2px solid transparent; }
.task-item:hover { background: var(--surface); }
.task-item.selected { border-color: var(--accent); background: var(--accent-soft); }
.task-item.done { opacity: 0.5; }
.task-check { width: 22px; height: 22px; border-radius: 50%; border: 2px solid var(--border); background: var(--bg); cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; color: white; font-size: 13px; }
.task-check.checked { background: var(--accent); border-color: var(--accent); }
.task-info { flex: 1; min-width: 0; }
.task-title { font-size: 14px; color: var(--fg); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-item.done .task-title { text-decoration: line-through; }
.task-meta { display: flex; gap: 8px; margin-top: 2px; flex-wrap: wrap; }
.task-tag { font-size: 10px; padding: 1px 6px; border-radius: 4px; background: var(--accent-soft); color: var(--accent); }
.task-tomato { font-size: 11px; color: var(--muted); }
.task-date { font-size: 11px; color: var(--muted); }
.task-del { width: 20px; height: 20px; border: none; background: none; color: var(--muted); font-size: 18px; cursor: pointer; opacity: 0; transition: opacity 0.2s; }
.task-item:hover .task-del { opacity: 0.6; }
.task-empty { text-align: center; padding: 20px; }

/* 计时器 */
.pomo-setup { display: flex; flex-direction: column; gap: 12px; }
.timer-ring { position: relative; width: 180px; height: 180px; margin: 8px auto; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.timer-ring.idle { border: 3px dashed var(--border); }
.timer-svg { position: absolute; inset: 0; width: 100%; height: 100%; transform: rotate(-90deg); }
.timer-track { fill: none; stroke: var(--border); stroke-width: 10; stroke-linecap: round; }
.timer-progress { fill: none; stroke: var(--accent); stroke-width: 8; stroke-linecap: round; transition: stroke-dashoffset 0.5s ease; }
.timer-ring.warn .timer-progress { stroke: #d1453b; }
.timer-ring.paused .timer-progress { stroke: var(--muted); }
.timer-inner { position: relative; z-index: 2; text-align: center; }
.timer-num { font-size: 28px; color: var(--fg); line-height: 1.2; }
.timer-ring.warn .timer-num { color: #d1453b; animation: pomoPulse 1s infinite; }
@keyframes pomoPulse { 0%,100% { opacity: 1; } 50% { opacity: 0.6; } }
.timer-label { font-size: 12px; color: var(--muted); margin-top: 4px; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.duration-pills { display: flex; gap: 6px; flex-wrap: wrap; }
.duration-pill { padding: 6px 14px; border: 2px solid var(--border); border-radius: 20px; background: var(--surface); color: var(--muted); font-size: 13px; cursor: pointer; transition: all 0.2s; }
.duration-pill.active { background: var(--accent); color: white; border-color: var(--accent); }
.pomo-start-btn { margin-top: 4px; }

.pomo-controls { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; }

/* 统计 */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 12px; }
.stat-item { text-align: center; padding: 10px 6px; background: var(--surface); border-radius: 10px; border: 2px solid var(--border); }
.stat-num { font-size: 20px; font-weight: 700; color: var(--accent); }
.stat-label { font-size: 11px; color: var(--muted); margin-top: 2px; }
.chart-wrap { margin-top: 4px; }
.chart-title { font-size: 12px; font-weight: 600; margin-bottom: 8px; }
.bar-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 80px; gap: 4px; }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 2px; height: 100%; justify-content: flex-end; }
.bar-wrap { width: 100%; height: 60px; display: flex; align-items: flex-end; justify-content: center; }
.bar-fill { width: 60%; min-height: 3px; background: linear-gradient(180deg, var(--accent), var(--accent-soft)); border-radius: 4px 4px 2px 2px; transition: height 0.6s ease; }
.bar-label { font-size: 10px; color: var(--muted); }
.bar-val { font-size: 9px; color: var(--muted); }

@media (max-width: 700px) {
  .pomo-wrap { flex-direction: column; }
  .pomo-left { width: 100%; }
}
</style>
