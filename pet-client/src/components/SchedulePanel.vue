<template>
  <div>
    <!-- 新增/编辑 -->
    <div class="card">
      <h3>{{ editing ? '✏️ 编辑日程' : '📝 新建日程' }}</h3>
      <div class="field">
        <input v-model="form.title" placeholder="标题,如:完成数据库大作业" />
      </div>
      <div class="sched-form-row">
        <div class="field sched-field-time">
          <label>提醒时间</label>
          <input type="datetime-local" v-model="form.remindAt" />
        </div>
        <div class="field sched-field-repeat">
          <label>重复</label>
          <select v-model="form.repeatType">
            <option value="ONCE">一次</option>
            <option value="DAILY">每天</option>
            <option value="WEEKLY">每周</option>
          </select>
        </div>
        <div class="field sched-field-prio">
          <label>优先级</label>
          <div class="prio-grid">
            <button type="button" v-for="p in prioOptions" :key="p.value" class="prio-pick" :class="{ active: form.priority === p.value, ['prio-pick-' + p.value]: true }" @click="form.priority = p.value">
              <span class="prio-dot"></span>{{ p.label }}
            </button>
          </div>
        </div>
      </div>
      <div class="field">
        <label>标签</label>
        <input v-model="form.tag" list="tag-list" placeholder="学习/工作/生活/健康/其他(可自定义)" />
        <datalist id="tag-list">
          <option v-for="t in tags" :key="t" :value="t" />
        </datalist>
      </div>
      <div class="sched-form-btns">
        <button class="pix-btn btn-primary" @click="save">{{ editing ? '保存修改' : '添加' }}</button>
        <button v-if="editing" class="pix-btn btn-ghost" @click="cancelEdit">取消</button>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </div>

    <!-- 列表:按优先级分区 -->
    <p v-if="!schedules.length" class="muted center empty-sched">还没有日程,先加一条吧。</p>

    <template v-for="sec in prioSections" :key="sec.level">
      <div v-if="pendingByPrio[sec.level].length" class="prio-section">
        <h4 class="prio-section-title" :class="sec.cls">{{ sec.label }}<span class="muted small">（{{ pendingByPrio[sec.level].length }}）</span></h4>
        <div
          v-for="s in pendingByPrio[sec.level]"
          :key="s.id"
          class="card sched-item"
          :class="{ overdue: isOverdue(s) }"
        >
          <div class="sched-item-main">
            <label class="sched-check" :class="{ checked: s.status === 'DONE' }">
              <input type="checkbox" :checked="s.status === 'DONE'" @change="toggleDone(s)" />
              <span class="sched-check-box"></span>
            </label>
            <div class="sched-item-info">
              <div class="sched-item-title-row">
                <b>{{ s.title }}</b>
              </div>
              <span class="muted sched-meta">
                #{{ s.tag || '未分类' }} · {{ repeatText(s.repeatType) }} · {{ fmt(s.remindAt) }}
              </span>
              <p v-if="isOverdue(s)" class="error sched-overdue-hint">再接再厉!这条过期了,现在补上也来得及</p>
            </div>
            <div class="sched-item-actions">
              <button class="pix-btn btn-ghost btn-sm" @click="startEdit(s)">✏️</button>
              <button class="pix-btn btn-ghost btn-sm sched-del-btn" @click="remove(s)">🗑</button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 已完成列表 -->
    <div v-if="completedSchedules.length" class="prio-section">
      <h4 class="prio-section-title prio-done">✅ 已完成<span class="muted small">（{{ completedSchedules.length }}）</span></h4>
      <div
        v-for="s in completedSchedules"
        :key="s.id"
        class="card sched-item done"
      >
        <div class="sched-item-main">
          <label class="sched-check checked">
            <input type="checkbox" checked @change="toggleDone(s)" />
            <span class="sched-check-box"></span>
          </label>
          <div class="sched-item-info">
            <div class="sched-item-title-row">
              <b class="text-done">{{ s.title }}</b>
            </div>
            <span class="muted sched-meta">
              #{{ s.tag || '未分类' }} · {{ repeatText(s.repeatType) }} · {{ fmt(s.remindAt) }}
            </span>
            <p class="ok sched-done-hint">已完成 {{ fmtTime(s.completedAt) }}</p>
          </div>
          <div class="sched-item-actions">
            <button class="pix-btn btn-ghost btn-sm sched-del-btn" @click="remove(s)">🗑</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 完成时的亲亲动画 -->
    <transition name="pop">
      <div v-if="kiss" class="kiss">{{ kiss }}</div>
    </transition>

    <!-- 统计 -->
    <div class="card">
      <h3>📊 统计</h3>
      <div class="grid-summary" v-if="summary">
        <div class="sum">完成日程 <b>{{ summary.totalDone }}</b></div>
        <div class="sum">专注轮数 <b>{{ summary.totalSessions }}</b></div>
        <div class="sum">累计专注 <b>{{ summary.totalMinutes }} 分钟</b></div>
        <div class="sum">累计获得 <b>{{ summary.totalCoins }} 币</b></div>
        <div class="sum">平均每轮 <b>{{ summary.avgMinutes }} 分钟</b></div>
      </div>
      <div class="chart-tabs">
        <button class="chip" :class="{ active: chart === 'pie' }" @click="switchChart('pie')">🍅 番茄钟项目占比(饼状图)</button>
        <button class="chip" :class="{ active: chart === 'trend' }" @click="switchChart('trend')">近 7 天(柱状+折线)</button>
        <button class="chip" :class="{ active: chart === 'line' }" @click="switchChart('line')">启动时间分布(折线图)</button>
      </div>
      <!-- 日期导航:箭头切换,不可超过今天 -->
      <div class="date-nav">
        <button class="pix-btn btn-ghost btn-sm date-arrow" @click="shiftDate(-1)">◀ {{ shiftLabel }}</button>
        <span class="date-range">{{ dateFrom }} ~ {{ dateTo }}</span>
        <button class="pix-btn btn-ghost btn-sm date-arrow" @click="shiftDate(1)" :disabled="!canShiftForward">{{ shiftLabel }} ▶</button>
        <input type="date" v-model="dateTo" :max="todayStr" @change="onDatePick" class="date-picker" />
      </div>
      <div ref="chartEl" class="chart"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue';
import * as echarts from 'echarts';
import http from '../api/http';
import { useSound } from '../soundStore';

const { playSound } = useSound();

const schedules = ref([]);
const tags = ref([]);
const err = ref('');
const editing = ref(false);
const editId = ref(null);
const kiss = ref('');
const chart = ref('pie');
const chartEl = ref(null);
const summary = ref(null);
let chartInst = null;

// 日期导航(使用本地时间,避免 UTC 偏差)
function localDateStr(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
const todayStr = localDateStr(new Date());
const dateTo = ref(todayStr);
const dateFrom = ref(todayStr);
const chartSpan = { pie: 30, trend: 7, line: 30 };
const shiftLabel = computed(() => (chartSpan[chart.value] || 30) + '天');
const canShiftForward = computed(() => dateTo.value < todayStr);

const form = ref({ title: '', remindAt: '', repeatType: 'ONCE', priority: 3, tag: '' });

const sortedSchedules = computed(() => {
  const prioOrder = { 1: 0, 2: 1, 3: 2, 4: 3 };
  return [...schedules.value].sort((a, b) => {
    if (a.status !== b.status) return a.status === 'PENDING' ? -1 : 1;
    const pa = prioOrder[a.priority] ?? 9;
    const pb = prioOrder[b.priority] ?? 9;
    if (pa !== pb) return pa - pb;
    return new Date(a.remindAt) - new Date(b.remindAt);
  });
});

const pendingByPrio = computed(() => {
  const groups = { 1: [], 2: [], 3: [], 4: [] };
  for (const s of schedules.value) {
    if (s.status === 'PENDING' && groups[s.priority]) {
      groups[s.priority].push(s);
    }
  }
  for (const k of Object.keys(groups)) {
    groups[k].sort((a, b) => new Date(a.remindAt) - new Date(b.remindAt));
  }
  return groups;
});

const completedSchedules = computed(() => {
  return schedules.value
    .filter(s => s.status === 'DONE')
    .sort((a, b) => new Date(b.completedAt || b.remindAt) - new Date(a.completedAt || a.remindAt));
});

const prioSections = [
  { level: 1, label: '🔴 紧急', cls: 'prio-1' },
  { level: 2, label: '🟠 高', cls: 'prio-2' },
  { level: 3, label: '🟡 中', cls: 'prio-3' },
  { level: 4, label: '🟢 低', cls: 'prio-4' },
];
const prioOptions = [
  { value: 1, label: '紧急' },
  { value: 2, label: '高' },
  { value: 3, label: '中' },
  { value: 4, label: '低' },
];

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '';
}
function fmtForInput(t) {
  return t ? String(t).slice(0, 16) : '';
}
function fmtTime(t) {
  return t ? fmt(t) : '';
}
function repeatText(r) {
  return { ONCE: '一次', DAILY: '每天', WEEKLY: '每周' }[r] || r;
}
function prioText(p) {
  return { 1: '紧急', 2: '高', 3: '中', 4: '低' }[p] || '';
}
function isOverdue(s) {
  return s.status === 'PENDING' && s.repeatType === 'ONCE' && new Date(s.remindAt) < new Date();
}

async function load() {
  schedules.value = await http.get('/api/schedule');
  tags.value = await http.get('/api/schedule/tags');
}

async function save() {
  playSound('button');
  err.value = '';
  const payload = { ...form.value };
  if (!payload.title.trim()) {
    err.value = '标题不能为空';
    return;
  }
  try {
    if (editing.value) {
      await http.put('/api/schedule/' + editId.value, payload);
    } else {
      await http.post('/api/schedule', payload);
    }
    cancelEdit();
    await load();
  } catch (e) {
    err.value = e.message;
  }
}

function startEdit(s) {
  editing.value = true;
  editId.value = s.id;
  form.value = {
    title: s.title,
    remindAt: fmtForInput(s.remindAt),
    repeatType: s.repeatType,
    priority: s.priority,
    tag: s.tag,
  };
}

function cancelEdit() {
  playSound('button');
  editing.value = false;
  editId.value = null;
  form.value = { title: '', remindAt: '', repeatType: 'ONCE', priority: 3, tag: '' };
}

async function toggleDone(s) {
  playSound('button');
  try {
    if (s.status === 'DONE') {
      await http.post('/api/schedule/' + s.id + '/undone');
    } else {
      const data = await http.post('/api/schedule/' + s.id + '/done');
      kiss.value = data.kiss ? '❤️' : '🎉';
      setTimeout(() => (kiss.value = ''), 2200);
    }
    await load();
    await renderChart();
  } catch (e) {
    err.value = e.message;
  }
}

async function remove(s) {
  playSound('button');
  if (!window.confirm('删除日程「' + s.title + '」?')) return;
  try {
    await http.delete('/api/schedule/' + s.id);
    await load();
  } catch (e) {
    err.value = e.message;
  }
}

function resetDateRange() {
  const span = chartSpan[chart.value] || 30;
  const today = new Date();
  const to = localDateStr(today);
  const from = localDateStr(new Date(today.getTime() - (span - 1) * 86400000));
  dateTo.value = to;
  dateFrom.value = from;
}

function shiftDate(direction) {
  playSound('button');
  const span = chartSpan[chart.value] || 30;
  const currentTo = new Date(dateTo.value + 'T00:00:00');
  const newTo = new Date(currentTo.getTime() + direction * span * 86400000);
  const today = new Date(todayStr + 'T00:00:00');
  if (newTo > today) newTo.setTime(today.getTime());
  if (newTo < new Date('2020-01-01')) return;
  const newFrom = new Date(newTo.getTime() - (span - 1) * 86400000);
  dateTo.value = newTo.toISOString().slice(0, 10);
  dateFrom.value = newFrom.toISOString().slice(0, 10);
  renderChart();
}

function onDatePick() {
  const span = chartSpan[chart.value] || 30;
  if (dateTo.value > todayStr) dateTo.value = todayStr;
  const to = new Date(dateTo.value + 'T00:00:00');
  const from = new Date(to.getTime() - (span - 1) * 86400000);
  dateFrom.value = from.toISOString().slice(0, 10);
  renderChart();
}

async function switchChart(kind) {
  playSound('button');
  chart.value = kind;
  resetDateRange();
  await renderChart();
}

async function renderChart() {
  await nextTick();
  if (!chartEl.value) return;
  if (!chartInst) {
    chartInst = echarts.init(chartEl.value);
  }
  const stats = await http.get('/api/schedule/stats', {
    params: { from: dateFrom.value, to: dateTo.value },
  });
  summary.value = stats.summary;
  let option;

  if (chart.value === 'pie') {
    // 番茄钟项目占比 - 饼状图(按 label 分组,显示专注分钟数)
    const pomoPieData = stats.pomodoroPie || [];
    const totalMin = stats.pomodoroTotalMinutes || 0;
    option = {
      title: {
        text: `番茄钟时间占比(共 ${totalMin} 分钟)`,
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c}分钟 ({d}%)',
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        top: 'middle',
        textStyle: { fontSize: 12 },
      },
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['62%', '55%'],
        data: pomoPieData.length ? pomoPieData : [{ name: '暂无数据', value: 1 }],
        label: {
          show: true,
          formatter: '{b}\n{c}分钟({d}%)',
          fontSize: 12,
        },
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2,
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      }],
    };
  } else if (chart.value === 'trend') {
    // 近 7 天:专注分钟(柱) + 完成数(折线)
    const maxCount = Math.max(...stats.last7Days.map((d) => d.count), 0);
    option = {
      title: { text: '近 7 天:专注分钟(柱) + 完成数(线)', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['专注分钟', '完成数'], bottom: 0 },
      xAxis: { type: 'category', data: stats.last7Days.map((d) => d.date.slice(5)) },
      yAxis: [
        { type: 'value', name: '分钟', minInterval: 1 },
        { type: 'value', name: '完成数', minInterval: 1, max: Math.max(maxCount + 2, 10) },
      ],
      series: [
        { name: '专注分钟', type: 'bar', data: stats.last7Days.map((d) => d.minutes), itemStyle: { color: '#5b9a8b', borderRadius: [6, 6, 0, 0] }, barWidth: '30%', label: { show: true, position: 'top', fontSize: 11 } },
        { name: '完成数', type: 'line', yAxisIndex: 1, data: stats.last7Days.map((d) => d.count), itemStyle: { color: '#ff9f45' }, lineStyle: { width: 3 }, symbol: 'circle', symbolSize: 8, smooth: true },
      ],
    };
  } else {
    // 启动时间分布 - 折线图(完成数)
    const hourData = stats.startHourDist || [];
    option = {
      title: { text: '番茄钟启动时间分布(折线图)', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: hourData.map((d) => d.hour + '时'),
        axisLabel: { fontSize: 11 },
      },
      yAxis: { type: 'value', minInterval: 1, name: '次数' },
      series: [{
        type: 'line',
        data: hourData.map((d) => d.count),
        itemStyle: { color: '#ff9f45' },
        lineStyle: { width: 3 },
        symbol: 'circle',
        symbolSize: 8,
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 159, 69, 0.3)' },
            { offset: 1, color: 'rgba(255, 159, 69, 0.02)' },
          ]),
        },
      }],
    };
  }
  chartInst.setOption(option, true);
}

function onResize() {
  if (chartInst) chartInst.resize();
}

onMounted(async () => {
  await load();
  resetDateRange();
  await renderChart();
  window.addEventListener('resize', onResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  if (chartInst) {
    chartInst.dispose();
    chartInst = null;
  }
});
</script>

<style scoped>
/* ── 表单区 ── */
.sched-form-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.sched-field-time { flex: 1; min-width: 180px; }
.sched-field-repeat { width: 100px; flex: 0 0 100px; }
.sched-field-prio { flex: 0 0 auto; }
.prio-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
}
.prio-pick {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 8px;
  border: 2px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.prio-pick:hover { border-color: var(--accent); }
.prio-pick.active { box-shadow: 0 2px 0 var(--border); }
.prio-pick .prio-dot {
  width: 8px; height: 8px; border-radius: 50%; display: inline-block; flex-shrink: 0;
}
.prio-pick-1 .prio-dot { background: #e74c3c; }
.prio-pick-2 .prio-dot { background: #e67e22; }
.prio-pick-3 .prio-dot { background: #b8860b; }
.prio-pick-4 .prio-dot { background: #27ae60; }
.prio-pick-1.active { border-color: #e74c3c; background: color-mix(in oklch, #e74c3c 12%, var(--surface)); color: #c0392b; }
.prio-pick-2.active { border-color: #e67e22; background: color-mix(in oklch, #e67e22 12%, var(--surface)); color: #d35400; }
.prio-pick-3.active { border-color: #b8860b; background: color-mix(in oklch, #b8860b 12%, var(--surface)); color: #9a7400; }
.prio-pick-4.active { border-color: #27ae60; background: color-mix(in oklch, #27ae60 12%, var(--surface)); color: #1e8449; }
.sched-form-btns {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.sched-form-btns .pix-btn {
  padding: 9px 22px !important;
  font-size: 15px !important;
}

/* 覆盖全局 input/select 宽度 */
.sched-form-row input,
.sched-form-row select {
  width: 100% !important;
  padding: 8px 12px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 10px !important;
  font-size: 13px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  font-family: var(--font-body) !important;
  box-shadow: none !important;
}
.sched-form-row input:focus,
.sched-form-row select:focus {
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 3px var(--accent-soft) !important;
}
.field > input,
.field > select {
  width: 100% !important;
}
.field label {
  font-size: 12px;
  color: var(--muted);
  font-weight: 600;
  margin-bottom: 4px;
  display: block;
}

/* ── 日程列表项 ── */
.prio-section {
  margin-bottom: 16px;
}
.prio-section-title {
  margin: 10px 0 6px;
  font-size: 15px;
  font-weight: 700;
}
.prio-section-title.prio-1 { color: #e74c3c; }
.prio-section-title.prio-2 { color: #e67e22; }
.prio-section-title.prio-3 { color: #b8860b; }
.prio-section-title.prio-4 { color: #27ae60; }
.prio-section-title.prio-done { color: var(--muted, #888); }
.sched-item {
  padding: 14px !important;
  margin-bottom: 10px !important;
}
.sched-item.done {
  opacity: 0.55;
}
.sched-item.overdue {
  border-color: color-mix(in oklch, var(--st-mood) 40%, var(--border)) !important;
}
.sched-item-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

/* 自定义勾选框 */
.sched-check {
  position: relative;
  flex-shrink: 0;
  margin-top: 2px;
  cursor: pointer;
}
.sched-check input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  cursor: pointer;
}
.sched-check-box {
  display: block;
  width: 24px;
  height: 24px;
  border: 2.5px solid var(--border);
  border-radius: 7px;
  background: var(--surface);
  transition: all 0.15s ease;
  position: relative;
}
.sched-check:hover .sched-check-box {
  border-color: var(--accent);
}
.sched-check.checked .sched-check-box {
  background: var(--accent);
  border-color: var(--accent);
}
.sched-check.checked .sched-check-box::after {
  content: '';
  position: absolute;
  left: 7px;
  top: 3px;
  width: 7px;
  height: 12px;
  border: solid var(--surface);
  border-width: 0 2.5px 2.5px 0;
  transform: rotate(45deg);
}

/* 日程信息 */
.sched-item-info {
  flex: 1;
  min-width: 0;
}
.sched-item-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.sched-item-title-row b {
  font-size: 15px;
  color: var(--fg);
}
.text-done {
  text-decoration: line-through;
  color: var(--muted);
}
.prio-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: var(--radius-xl);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.prio-1 { background: color-mix(in oklch, var(--st-mood) 15%, var(--surface)); color: var(--st-mood); }
.prio-2 { background: color-mix(in oklch, var(--accent) 15%, var(--surface)); color: color-mix(in oklch, var(--accent) 60%, black); }
.prio-3 { background: color-mix(in oklch, #e8c84a 20%, var(--surface)); color: #8a6d00; }
.prio-4 { background: color-mix(in oklch, #5b9a8b 15%, var(--surface)); color: #3d6b60; }

.sched-meta {
  font-size: 12px;
  margin-top: 3px;
  display: block;
}
.sched-overdue-hint, .sched-done-hint {
  margin-top: 4px;
  font-size: 12px;
}

/* 操作按钮 */
.sched-item-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.sched-del-btn {
  font-size: 13px !important;
}

.empty-sched {
  padding: 20px 0;
  text-align: center;
}

/* ── 动画 ── */
.kiss {
  position: fixed;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 90px;
  z-index: 99;
  pointer-events: none;
  animation: kissPop 2.2s ease-out forwards;
}
@keyframes kissPop {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.3); }
  20% { opacity: 1; transform: translate(-50%, -50%) scale(1.2); }
  80% { opacity: 1; }
  100% { opacity: 0; transform: translate(-50%, -70%) scale(1); }
}

/* ── 图表 ── */
.chart-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin: 12px 0;
}
.date-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.date-arrow {
  font-size: 12px !important;
  padding: 4px 10px !important;
  white-space: nowrap;
}
.date-arrow:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.date-range {
  font-size: 13px;
  color: var(--muted);
  font-weight: 600;
  white-space: nowrap;
}
.date-picker {
  width: auto !important;
  padding: 4px 8px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 8px !important;
  font-size: 12px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  font-family: var(--font-body) !important;
  box-shadow: none !important;
}
.chart {
  width: 100%;
  height: 360px;
  margin-top: 6px;
}

/* ── 汇总卡片 ── */
.grid-summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}
.sum {
  background: var(--accent-soft);
  border-radius: var(--radius);
  padding: 8px 12px;
  font-size: 13px;
  color: var(--fg);
}
.sum b {
  color: color-mix(in oklch, var(--accent) 55%, black);
  font-size: 15px;
}
</style>
