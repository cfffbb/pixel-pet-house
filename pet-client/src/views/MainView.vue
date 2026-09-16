<template>
  <div id="appView">
    <!-- 场景舞台(背景图 + 像素宠物) -->
    <div class="scene-stage" ref="stageRef">
      <div class="scene-bg" :style="{ backgroundImage: `url('${themeBg}')` }"></div>
      <img v-if="petInfo && petSampleSrc" ref="scenePetImgRef" class="scene-pet-img" :src="petSampleSrc" @click="onPetClick" alt="宠物" style="cursor:pointer">
      <canvas v-show="false" ref="scenePetRef" class="scene-pet-canvas"></canvas>
      <img v-if="showEgg" class="stage-egg" :src="eggGlyph" alt="宠物蛋" @click="goScene('home')">
      <div class="topbar">
        <span class="brand"><span class="dot"></span>像素宠物小屋</span>
        <div class="top-right">
          <span class="coin-pill"><span class="c"></span>{{ coins }}</span>
          <button class="pix-btn btn-soft btn-sm" @click="toggleFloatingWindow" title="显示/隐藏悬浮宠物">🐾 悬浮</button>
          <span class="user-chip">{{ user.nickname || user.username }}</span>
          <button class="pix-btn btn-ghost btn-sm" @click="doLogout">退出</button>
        </div>
      </div>
      <div class="nameplate" v-if="petInfo" :style="nameplateStyle">
        <span class="nm">{{ petInfo.name }}</span>
      </div>
      <div class="speech" :class="{ show: speechVisible }" :style="speechStyle">{{ speechText }}</div>
      <!-- 灯按钮 + 时间显示 -->
      <div class="scene-controls">
        <img class="lamp-btn" :src="lampImg" alt="开关灯" @click="toggleLight">
        <div class="time-display">
          <div class="game-time">{{ gameTimeText }}</div>
          <div class="real-time">{{ realTimeText }} · {{ seasonName }}</div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content-wrap" :class="{ 'page-in': pageIn }">
      <div class="page">
        <div class="sub-pills" v-if="currentPages.length > 1">
          <button
            v-for="[key, label] in currentPages"
            :key="key"
            class="chip"
            :class="{ active: page === key }"
            @click="playSound('button'); page = key"
          >
            {{ label }}
          </button>
        </div>

        <PetPanel v-if="page === 'pet'" :user="user" @go-shop="goScene('shop')" @pet-switched="onPetSwitched" />
        <FeedingPanel v-else-if="page === 'feed'" />
        <SchedulePanel v-else-if="page === 'sched'" />
        <PomodoroPanel v-else-if="page === 'pomo'" @complete="onPomodoroComplete" />
        <ShopPanel v-else-if="page === 'shop'" />
        <DeskPanel v-else-if="page === 'desk'" />
        <ChatPanel v-else-if="page === 'chat'" />
        <BackyardPanel v-else-if="page === 'backyard'" />
        <SettingPanel v-else-if="page === 'set'" :user="user" />
        <AdminPanel v-else :user="user" />
      </div>
    </div>

    <!-- 底部圆形导航坞(像素图标) -->
    <nav class="scene-dock">
      <button
        v-for="s in visibleScenes"
        :key="s.id"
        class="dock-btn"
        :class="{ active: scene === s.id }"
        @click="goScene(s.id)"
      >
        <span class="dk"><img :src="dockGlyph(s.glyph)" alt=""></span>
        <span class="dl">{{ s.n }}</span>
      </button>
    </nav>

    <!-- 悬浮宠物球 -->
    <div class="mini-pet" v-if="petInfo" @contextmenu.prevent="openFloatChat">
      <div class="mp-bubble" :class="{ show: bubbleVisible }">{{ bubbleText }}</div>
      <div class="mp-menu" :class="{ show: menuVisible }">
        <button class="ghost small" @click="quickAction('喂食')">🍖 喂食</button>
        <button class="ghost small" @click="quickAction('玩耍')">🎮 玩耍</button>
        <button class="ghost small" @click="openFloatChat">💬 说话</button>
      </div>
      <div class="mp-face" :class="mpAnim" @click="toggleMenu">
        <img v-if="miniPetSrc" :src="miniPetSrc" alt="">
        <span v-else>🐾</span>
      </div>
    </div>

    <!-- 右键/快捷对话输入框 -->
    <div class="float-chat-overlay" v-if="floatChatVisible" @click.self="playSound('button'); floatChatVisible = false">
      <div class="float-chat-box">
        <div class="float-chat-header">
          <span>💬 和宠物说话</span>
          <button class="float-chat-close" @click="playSound('button'); floatChatVisible = false">✕</button>
        </div>
        <div class="float-chat-body" ref="floatChatBodyRef">
          <div v-for="(m, i) in floatChatMessages" :key="i" class="float-chat-msg" :class="m.role">
            <span class="float-chat-bubble">{{ m.content }}</span>
          </div>
          <p v-if="floatChatThinking" class="muted center">宠物正在想…</p>
        </div>
        <div class="float-chat-input">
          <input v-model="floatChatText" placeholder="说点什么…(Enter 发送)" @keyup.enter="sendFloatChat" :disabled="floatChatThinking" />
          <button class="pix-btn btn-primary btn-sm" @click="sendFloatChat" :disabled="floatChatThinking || !floatChatText.trim()">发送</button>
        </div>
        <div class="float-chat-tts" v-if="ttsEnabled">
          <button class="pix-btn btn-ghost btn-sm" @click="playTts(lastAiReply)" :disabled="!lastAiReply">🔊 朗读回复</button>
        </div>
      </div>
    </div>

    <!-- TTS 音频播放器(隐藏) -->
    <audio ref="ttsAudioRef" v-show="false"></audio>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import PetPanel from '../components/PetPanel.vue';
import SchedulePanel from '../components/SchedulePanel.vue';
import PomodoroPanel from '../components/PomodoroPanel.vue';
import ShopPanel from '../components/ShopPanel.vue';
import DeskPanel from '../components/DeskPanel.vue';
import ChatPanel from '../components/ChatPanel.vue';
import SettingPanel from '../components/SettingPanel.vue';
import AdminPanel from '../components/AdminPanel.vue';
import BackyardPanel from '../components/BackyardPanel.vue';
import FeedingPanel from '../components/FeedingPanel.vue';
import { logout } from '../api/http';
import http from '../api/http';
import { glyphData, resolveColors } from '../pixelPet';
import { useTheme, THEMES, SEASONS } from '../themeStore';
import { petImgFrom } from '../petSamples';
import { useSound } from '../soundStore';

const { playSound, restoreBgMusic, getVoiceVolume, playLampSound } = useSound();

const {
  themeId, isNight, autoTime, currentSeason, effectiveNight, timeOfDay,
  gameTimeText, realTimeText,
  bgUrl, chatBgUrl, timerDialUrl, lampUrl, lampSoundUrl,
  setTheme, toggleLight: themeToggleLight, setAutoTime, initTheme,
  startGameTime, stopGameTime, startRealTime, stopRealTime,
} = useTheme();

const themeBg = computed(() => encodeURI(bgUrl()));
const lampImg = computed(() => lampUrl());
const seasonName = computed(() => {
  const s = SEASONS.find(x => x.id === currentSeason.value);
  return s ? s.name : '';
});

function toggleLight() {
  themeToggleLight();
  playLampSound(lampSoundUrl());
  showSpeech(effectiveNight.value ? '关灯啦,晚安～' : '开灯啦,天亮了～');
}

const props = defineProps({ user: Object });
const emit = defineEmits(['logout']);

/* ─── 场景配置(glyph = 像素图标名, scene = drawScene 类型) ─── */
const SCENES = [
  { id: 'home', n: '小屋', glyph: 'home', scene: 'home' },
  { id: 'study', n: '书房', glyph: 'book', scene: 'study' },
  { id: 'shop', n: '商店', glyph: 'bag', scene: 'shop' },
  { id: 'garden', n: '花园', glyph: 'chat', scene: 'garden' },
  { id: 'backyard', n: '后院', glyph: 'chat', scene: 'backyard' },
  { id: 'settings', n: '设置', glyph: 'gear', scene: 'settings' },
  { id: 'admin', n: '管理', glyph: 'gear2', scene: 'home', adminOnly: true },
];

const SCENE_PAGES = {
  home: [['pet', '我的宠物'], ['feed', '喂养']],
  study: [['pomo', '番茄钟'], ['sched', '日程']],
  shop: [['shop', '商店']],
  garden: [['chat', '对话']],
  backyard: [['backyard', '后院繁育']],
  settings: [['set', '设置'], ['desk', '桌面整理']],
  admin: [['admin', '控制面板']],
};

/* ─── 响应式状态 ─── */
const isAdmin = computed(() => props.user && props.user.role === 'ADMIN');
const visibleScenes = computed(() => SCENES.filter(s => !s.adminOnly || isAdmin.value));

const scene = ref('home');
const page = ref('pet');
const pageIn = ref(false);
const coins = ref(0);
const petInfo = ref(null);

const speechVisible = ref(false);
const speechText = ref('你好呀～');
const speechStyle = ref({});
const nameplateStyle = ref({});
const bubbleVisible = ref(false);
const bubbleText = ref('你好呀～');
const menuVisible = ref(false);
const mpAnim = ref('');
const miniPetSrc = ref('');
const petSampleSrc = computed(() => {
  if (!petInfo.value) return '';
  return petImgFrom(petInfo.value);
});
const showEgg = ref(false);
const eggGlyph = ref('');

/* ─── 悬浮窗对话 + TTS ─── */
const floatChatVisible = ref(false);
const floatChatText = ref('');
const floatChatMessages = ref([]);
const floatChatThinking = ref(false);
const floatChatBodyRef = ref(null);
const ttsAudioRef = ref(null);
const ttsEnabled = ref(false);
const bubbleReadEnabled = ref(false);
const lastAiReply = ref('');
let bubbleTexts = []; // 气泡提示库缓存

/* ─── Canvas 引用 ─── */
const stageRef = ref(null);
const sceneCanvasRef = ref(null);
const scenePetRef = ref(null);

let speechTimer = null;
let bubbleTimer = null;
let animFrame = null;
let blinkTimer = 0;
let petScale = 8;
let petW = 0;
let petH = 0;
let resizeObserver = null;

const currentPages = computed(() => {
  if (scene.value === 'admin') return [['admin', '控制面板']];
  return SCENE_PAGES[scene.value] || SCENE_PAGES.home;
});

/* ─── 像素图标 dataURL ─── */
function dockGlyph(name) {
  return glyphData(name, '#4a3a2a');
}

/* ─── 场景切换 ─── */
function goScene(id, targetPage) {
  playSound('button');
  scene.value = id;
  const pages = id === 'admin' ? [['admin', '控制面板']] : (SCENE_PAGES[id] || SCENE_PAGES.home);
  if (targetPage) {
    page.value = targetPage;
  } else if (!pages.some(p => p[0] === page.value)) {
    page.value = pages[0][0];
  }
  showSpeech(getSceneGreeting(id));
  pageIn.value = false;
  nextTick(() => { pageIn.value = true; });
}

function onPetSwitched() {
  loadPetData();
}

function getSceneGreeting(id) {
  const greetings = {
    home: '欢迎回家～',
    study: '该学习啦！',
    shop: '想买点什么？',
    garden: '来聊天吧～',
    backyard: '后院的小动物们在等你～',
    settings: '调整设置中',
    admin: '管理员控制台',
  };
  return greetings[id] || '你好呀～';
}

function showSpeech(text) {
  speechText.value = text;
  speechVisible.value = true;
  clearTimeout(speechTimer);
  speechTimer = setTimeout(() => { speechVisible.value = false; }, 2600);
}

function showBubble(text) {
  bubbleText.value = text;
  bubbleVisible.value = true;
  clearTimeout(bubbleTimer);
  bubbleTimer = setTimeout(() => { bubbleVisible.value = false; }, 3000);
}

/* ─── 宠物点击交互 ─── */
const PET_CLICK_MSGS = [
  '你好呀～', '陪我玩嘛～', '今天也要加油！', '饿饿...', '想睡觉了...',
  '主人最好了～', '嘿嘿～', '抱抱我～', '去学习吧！', '专注赚币买零食！',
  '我是不是最可爱的？', '心情好好～', '想出去散步...', '陪我聊聊天嘛',
];
function onPetClick() {
  playSound('play');
  const msg = PET_CLICK_MSGS[Math.floor(Math.random() * PET_CLICK_MSGS.length)];
  showSpeech(msg);
  showBubble(msg);
  if (scenePetRef.value) {
    scenePetRef.value.style.transform = 'scale(1.08)';
    setTimeout(() => { if (scenePetRef.value) scenePetRef.value.style.transform = ''; }, 200);
  }
}

function toggleMenu() {
  playSound('button');
  menuVisible.value = !menuVisible.value;
  mpAnim.value = 'mpBounce';
  setTimeout(() => { mpAnim.value = ''; }, 500);
  if (menuVisible.value) showBubble('想做什么？');
}

function quickAction(action) {
  playSound('play');
  menuVisible.value = false;
  if (action === '说话') {
    openFloatChat();
    return;
  }
  const anims = { 喂食: 'mpSquish', 玩耍: 'mpSpin', 说话: 'mpBounce' };
  mpAnim.value = anims[action] || 'mpBounce';
  setTimeout(() => { mpAnim.value = ''; }, 600);
  showSpeech(`${action}功能请到小屋页面操作～`);
}

/* ─── 悬浮窗对话 ─── */
function openFloatChat() {
  playSound('button');
  menuVisible.value = false;
  floatChatVisible.value = true;
  floatChatText.value = '';
}

async function sendFloatChat() {
  playSound('button');
  if (!floatChatText.value.trim() || floatChatThinking.value) return;
  const userText = floatChatText.value.trim();
  floatChatMessages.value.push({ role: 'user', content: userText });
  floatChatText.value = '';
  floatChatThinking.value = true;
  await scrollFloatChat();

  try {
    const data = await http.post('/api/chat/message', { content: userText });
    const reply = data.reply || '...';
    floatChatMessages.value.push({ role: 'assistant', content: reply });
    lastAiReply.value = reply;
    showBubble(reply.length > 30 ? reply.slice(0, 30) + '...' : reply);

    // TTS 自动播放
    if (ttsEnabled.value && data.ttsAvailable) {
      playTts(reply);
    }
  } catch (e) {
    floatChatMessages.value.push({ role: 'assistant', content: '出错了:' + e.message });
  }
  floatChatThinking.value = false;
  await scrollFloatChat();
}

async function scrollFloatChat() {
  await nextTick();
  if (floatChatBodyRef.value) {
    floatChatBodyRef.value.scrollTop = floatChatBodyRef.value.scrollHeight;
  }
}

/* ─── TTS 语音播放 ─── */
async function playTts(text) {
  playSound('button');
  if (!text) return;
  try {
    const resp = await fetch('http://localhost:8080/api/chat/tts', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + (localStorage.getItem('pet_token') || ''),
      },
      body: JSON.stringify({ text }),
    });
    if (!resp.ok) throw new Error('TTS 请求失败:' + resp.status);
    const blob = await resp.blob();
    const url = URL.createObjectURL(blob);
    if (ttsAudioRef.value) {
      ttsAudioRef.value.src = url;
      ttsAudioRef.value.volume = getVoiceVolume();
      ttsAudioRef.value.play().catch(() => {});
    }
  } catch (e) {
    showBubble('语音播放失败');
  }
}

/* ─── 气泡提示库 ─── */
async function loadBubbleTexts() {
  try {
    const data = await http.get('/api/chat/bubbles');
    if (data.texts && data.texts.length) {
      bubbleTexts = data.texts;
    }
  } catch (e) {}
}

async function loadVoicePref() {
  try {
    const data = await http.get('/api/chat/voice-pref');
    ttsEnabled.value = data.ttsEnabled === 1;
    bubbleReadEnabled.value = data.bubbleRead === 1;
  } catch (e) {}
}

function randomBubbleFromLibrary() {
  if (bubbleTexts.length === 0) return null;
  return bubbleTexts[Math.floor(Math.random() * bubbleTexts.length)];
}

function doLogout() {
  playSound('button');
  logout();
  emit('logout');
}

// 悬浮窗显示/隐藏切换
const floatingVisible = ref(true);
function toggleFloatingWindow() {
  playSound('button');
  if (window.petAPI) {
    if (floatingVisible.value) {
      window.petAPI.hideFloating();
      floatingVisible.value = false;
    } else {
      window.petAPI.showFloating();
      floatingVisible.value = true;
    }
  }
}

/* ─── 番茄钟完成:宠物恭喜提示 ─── */
function onPomodoroComplete(data) {
  if (data && data.abandoned) {
    showSpeech('没关系,下次加油!');
    showBubble('没关系,下次加油!');
  } else {
    const congrats = [
      '太棒了!专注完成!',
      '你好厉害!又赚到游戏币啦～',
      '主人好专注!奖励自己一下～',
      '完美的番茄钟!我给你加油!',
    ];
    const msg = congrats[Math.floor(Math.random() * congrats.length)];
    const coinText = data && data.coins ? ` +${data.coins}币` : '';
    showSpeech(msg + coinText);
    showBubble(msg + coinText);
  }
  // 更新游戏币(放弃时为负数)
  if (data && data.coins) {
    coins.value += data.coins;
  }
}

/* ─── 场景宠物布局(CSS 控制图片尺寸,JS 只定位铭牌/气泡) ─── */
function layoutScenePet() {
  const stage = stageRef.value;
  if (!stage) return;
  const h = stage.clientHeight;

  if (!petInfo.value) {
    showEgg.value = true;
    eggGlyph.value = glyphData('egg', '#e8b25c');
    speechStyle.value = { top: (h / 2 - 92) + 'px' };
    nameplateStyle.value = { display: 'none' };
    return;
  }

  showEgg.value = false;
  // 宠物图由 CSS 定位(bottom:10px,居中),这里只算铭牌/气泡位置
  const petH = Math.round(h * 0.45);
  const y = h * 0.72 - petH;

  speechStyle.value = { top: (y - 42) + 'px' };
  nameplateStyle.value = { top: (y - 24) + 'px', display: 'inline-flex' };
}

/* 浮动动画(用CSS animation) */
function animLoop() {
  // CSS动画处理浮动,不需要requestAnimationFrame
}

/* ─── 加载宠物数据 ─── */
async function loadPetData() {
  try {
    const pet = await http.get('/api/pet/my');
    if (pet && (pet.id || pet.petId || pet.hasPet)) {
      petInfo.value = pet;
      coins.value = pet.coins || 0;
      miniPetSrc.value = petImgFrom(pet);
    } else {
      petInfo.value = null;
      showEgg.value = true;
      // 首次启动:自动跳转到宠物面板抽宠
      scene.value = 'home';
      page.value = 'pet';
      setTimeout(() => { showSpeech('快去抽一只宠物吧!'); }, 800);
    }
  } catch (e) {
    console.log('[MainView] 宠物数据加载失败', e.message);
    petInfo.value = null;
    showEgg.value = true;
  }
}

/* ─── 生命周期 ─── */
onMounted(async () => {
  resolveColors();
  initTheme();
  startGameTime();
  startRealTime();
  restoreBgMusic();
  await loadPetData();
  await loadVoicePref();
  await loadBubbleTexts();
  await nextTick();
  layoutScenePet();

  setTimeout(() => { showSpeech('你好呀～'); }, 500);

  // 随机气泡(优先使用气泡提示库)
  setInterval(() => {
    if (Math.random() < 0.3 && petInfo.value) {
      const libMsg = randomBubbleFromLibrary();
      if (libMsg) {
        showBubble(libMsg);
        // 如果开启气泡朗读,调用 TTS
        if (bubbleReadEnabled.value) {
          playTts(libMsg);
        }
      } else {
        const msgs = ['好饿啊～', '陪我玩嘛～', '今天也要加油！', 'ZZZ...'];
        showBubble(msgs[Math.floor(Math.random() * msgs.length)]);
      }
    }
  }, 15000);

  // 监听窗口大小变化
  if (window.ResizeObserver) {
    resizeObserver = new ResizeObserver(() => layoutScenePet());
    if (stageRef.value) resizeObserver.observe(stageRef.value);
  } else {
    window.addEventListener('resize', layoutScenePet);
  }
});

onUnmounted(() => {
  clearTimeout(speechTimer);
  clearTimeout(bubbleTimer);
  if (animFrame) cancelAnimationFrame(animFrame);
  if (resizeObserver) resizeObserver.disconnect();
  stopGameTime();
  stopRealTime();
  window.removeEventListener('resize', layoutScenePet);
});

// 场景变化时重新布局宠物
watch(scene, () => {
  nextTick(() => layoutScenePet());
});
// 主题/昼夜变化时背景图自动更新(通过computed自动响应)
</script>

<style scoped>
/* ─── 悬浮窗对话 ─── */
.float-chat-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}
.float-chat-box {
  width: min(420px, 90vw);
  max-height: 70vh;
  background: var(--bg);
  border: 3px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 32px rgba(60, 40, 20, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.float-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--accent-soft);
  border-bottom: 2px solid var(--border);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  color: var(--fg);
}
.float-chat-close {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--muted);
  padding: 4px 8px;
  border-radius: 6px;
}
.float-chat-close:hover { background: var(--border); color: var(--fg); }
.float-chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  min-height: 200px;
  max-height: 400px;
}
.float-chat-msg {
  margin-bottom: 10px;
  display: flex;
}
.float-chat-msg.user { justify-content: flex-end; }
.float-chat-msg.assistant { justify-content: flex-start; }
.float-chat-bubble {
  max-width: 80%;
  padding: 8px 13px;
  border-radius: 13px;
  font-size: 13.5px;
  line-height: 1.5;
  word-break: break-word;
}
.float-chat-msg.user .float-chat-bubble {
  background: var(--accent);
  color: var(--surface);
  border-bottom-right-radius: 4px;
}
.float-chat-msg.assistant .float-chat-bubble {
  background: var(--surface);
  border: 2px solid var(--border);
  border-bottom-left-radius: 4px;
}
.float-chat-input {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  border-top: 2px solid var(--border);
}
.float-chat-input input {
  flex: 1;
  width: auto !important;
  padding: 9px 13px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 12px !important;
  font-size: 14px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  box-shadow: none !important;
  outline: none !important;
}
.float-chat-input input:focus {
  border-color: var(--accent) !important;
}
.float-chat-tts {
  padding: 0 14px 10px;
  text-align: center;
}
</style>
