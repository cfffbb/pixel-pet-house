/*
 * 主题管理模块 —— 三种小屋风格(传统木屋/赛博木屋/魔法小屋)
 * 管理: 主题切换 / 昼夜切换 / 季节判断 / 游戏内时间 / 背景图路径
 */
import { ref, computed, watch } from 'vue';

export const THEMES = [
  { id: 'wood', name: '传统木屋', icon: '🏠' },
  { id: 'cyber', name: '赛博木屋', icon: '🌃' },
  { id: 'magic', name: '魔法小屋', icon: '✨' },
];

export const SEASONS = [
  { id: 'spring', name: '春', months: [3, 4, 5] },
  { id: 'summer', name: '夏', months: [6, 7, 8] },
  { id: 'autumn', name: '秋', months: [9, 10, 11] },
  { id: 'winter', name: '冬', months: [12, 1, 2] },
];

// 响应式状态
const themeId = ref(localStorage.getItem('pet_theme') || 'wood');
const isNight = ref(false);
const gameHour = ref(8); // 游戏内小时(0-23)
const gameMinute = ref(0);
const autoTime = ref(true); // 自动根据现实时间判断昼夜
const seasonOverride = ref(null); // null=自动, 'spring'/'summer'/'autumn'/'winter'=手动覆盖

// 根据现实月份判断季节(可被管理员手动覆盖)
const currentSeason = computed(() => {
  if (seasonOverride.value) return seasonOverride.value;
  const month = new Date().getMonth() + 1;
  for (const s of SEASONS) {
    if (s.months.includes(month)) return s.id;
  }
  return 'spring';
});

// 根据现实时间判断昼夜
const realIsNight = computed(() => {
  const h = new Date().getHours();
  return h < 6 || h >= 18;
});

// 实际是否为夜晚(手动优先,否则自动)
const effectiveNight = computed(() => {
  if (autoTime.value) return realIsNight.value;
  return isNight.value;
});

// 当前时段(day/night)
const timeOfDay = computed(() => effectiveNight.value ? 'night' : 'day');

// 背景图路径
function bgUrl() {
  const seasonMap = { spring: '春', summer: '夏', autumn: '秋', winter: '冬' };
  const fileName = `${effectiveNight.value ? '晚上' : '白天'} ${seasonMap[currentSeason.value]}.jpg`;
  return `./scenes/themes/${themeId.value}/${timeOfDay.value}/${fileName}`;
}

// 聊天背景图
function chatBgUrl() {
  return `./scenes/themes/${themeId.value}/chat-bg.jpg`;
}

// 计时器表盘图
function timerDialUrl() {
  return `./scenes/themes/${themeId.value}/timer-dial.png`;
}

// 灯按钮图
function lampUrl() {
  return `./scenes/themes/${themeId.value}/lamp.png`;
}

// 开关灯声音
function lampSoundUrl() {
  return `./sounds/lamp-${themeId.value}.mp3`;
}

// 切换主题
function setTheme(id) {
  themeId.value = id;
  localStorage.setItem('pet_theme', id);
}

// 手动开关灯
function toggleLight() {
  autoTime.value = false;
  isNight.value = !isNight.value;
  localStorage.setItem('pet_auto_time', 'false');
  localStorage.setItem('pet_is_night', String(isNight.value));
}

// 设置自动时间模式
function setAutoTime(val) {
  autoTime.value = val;
  localStorage.setItem('pet_auto_time', String(val));
}

// 初始化(从localStorage恢复)
function initTheme() {
  const savedAuto = localStorage.getItem('pet_auto_time');
  if (savedAuto === 'false') {
    autoTime.value = false;
    isNight.value = localStorage.getItem('pet_is_night') === 'true';
  }
}

// 游戏内时间推进(每秒推进游戏内时间)
let gameTimeInterval = null;
function startGameTime() {
  if (gameTimeInterval) return;
  gameTimeInterval = setInterval(() => {
    gameMinute.value += 1;
    if (gameMinute.value >= 60) {
      gameMinute.value = 0;
      gameHour.value = (gameHour.value + 1) % 24;
    }
  }, 1000); // 1秒 = 1游戏分钟(可调)
}
function stopGameTime() {
  if (gameTimeInterval) { clearInterval(gameTimeInterval); gameTimeInterval = null; }
}

// 游戏内时间文本
const gameTimeText = computed(() => {
  return String(gameHour.value).padStart(2, '0') + ':' + String(gameMinute.value).padStart(2, '0');
});

// 现实时间
const realTimeText = ref('');
function updateRealTime() {
  const d = new Date();
  realTimeText.value = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
}
let realTimeInterval = null;
function startRealTime() {
  updateRealTime();
  if (realTimeInterval) return;
  realTimeInterval = setInterval(updateRealTime, 1000);
}
function stopRealTime() {
  if (realTimeInterval) { clearInterval(realTimeInterval); realTimeInterval = null; }
}

// 设置季节覆盖(null=自动)
function setSeasonOverride(season) {
  seasonOverride.value = season;
}

export function useTheme() {
  return {
    themeId, isNight, autoTime, currentSeason, effectiveNight, timeOfDay,
    seasonOverride,
    gameHour, gameMinute, gameTimeText, realTimeText,
    bgUrl, chatBgUrl, timerDialUrl, lampUrl, lampSoundUrl,
    setTheme, toggleLight, setAutoTime, initTheme, setSeasonOverride,
    startGameTime, stopGameTime, startRealTime, stopRealTime,
  };
}
