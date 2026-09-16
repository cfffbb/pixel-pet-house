<template>
  <div class="floating" @mouseenter="hovered = true" @mouseleave="hovered = false">
    <button v-show="hovered" class="close" title="隐藏悬浮宠物" @click.stop="hide">✕</button>

    <div class="drag-zone">
      <!-- 撒花庆祝特效 -->
      <div v-if="celebrating" class="confetti-layer">
        <span 
          v-for="p in confettiPieces" 
          :key="p.id" 
          class="confetti-piece"
          :style="{ left: p.left + '%', animationDelay: p.delay + 's', backgroundColor: p.color, width: p.size + 'px', height: p.size + 'px' }"
        ></span>
      </div>
      
      <!-- 番茄钟倒计时(顶部) -->
      <div v-if="countdown" class="timer-badge">
        <span class="tm-icon">🍅</span>
        <span class="tm-text">{{ countdown }}</span>
      </div>

      <!-- 宠物主体 -->
      <div class="pet" :class="{ spin }" @click="onPetClick" @contextmenu.prevent="openMenu">
        <img v-if="pet && pet.hasPet" :src="pixelSrc" class="figure" alt="" />
        <span v-else class="egg">🐣</span>
        <div v-if="hungry" class="hungry-mark">🍖?</div>
      </div>

      <!-- 气泡(宠物上方) -->
      <div v-if="bubble" class="bubble" :class="{ warn: hungry && bubble === '好饿…' }">
        {{ bubble }}
        <div class="bubble-tail"></div>
      </div>

      <!-- 底部操作栏:语音按钮 + 通话按钮 -->
      <div class="action-bar" @click.stop>
        <button class="action-btn voice-btn" :class="{ listening: isListening }" @click="toggleVoice" :title="isListening ? '正在录音…点击停止' : '点击说话'">
          <span v-if="isListening" class="voice-wave">
            <span></span><span></span><span></span>
          </span>
          <span v-else>🎤</span>
        </button>
        <button class="action-btn call-btn" :class="{ active: callMode }" @click="toggleCallMode" :title="callMode ? '结束通话' : '语音通话(轮次对话)'">
          <span v-if="callMode" class="call-pulse"></span>
          <span>📞</span>
        </button>
      </div>

      <!-- 点击弹出菜单 -->
      <div v-if="menuOpen" class="pet-menu" @click.stop>
        <button @click="doAction('feed')">🍖 喂食</button>
        <button @click="doAction('play')">🎮 玩耍</button>
        <button @click="toggleVoice">🎤 语音对话</button>
        <button @click="toggleCallMode">📞 语音通话</button>
        <button @click="doAction('study')">📚 学习陪我</button>
        <button @click="doAction('showMain')">🏠 打开主窗口</button>
        <button @click="doAction('hide')">🙈 隐藏一下</button>
      </div>

      <!-- 语音模式:AI回复气泡(显示在宠物上方) -->
      <div v-if="voiceReply" class="voice-reply-bubble">
        <span class="vr-text">{{ voiceReply }}</span>
        <button class="vr-close" @click="voiceReply = ''">✕</button>
      </div>

      <!-- 语音状态提示 -->
      <div v-if="voiceStatus" class="voice-status" :class="voiceStatusType">
        {{ voiceStatus }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import http from '../api/http';
import { petImgFrom } from '../petSamples';
import { useSound } from '../soundStore';

const { playSound, playPomodoroEnd } = useSound();

const pet = ref(null);
const hungry = ref(false);
const bubble = ref('');
const countdown = ref('');
const spin = ref(false);
const hovered = ref(false);
const menuOpen = ref(false);
const pixelSrc = computed(() => {
  if (!pet.value || !pet.value.hasPet) return '';
  return petImgFrom(pet.value);
});
let timer = null;
let blinkTimer = null;
let bubbleTimer = null;

// 语音交互(MediaRecorder + 后端 STT/TTS)
const isListening = ref(false);
const voiceReply = ref('');
const voiceStatus = ref('');
const voiceStatusType = ref('');

// 语音通话模式(轮次式录音对话)
const callMode = ref(false);
let callIdleTimer = null;

// WAV 编码:AudioBuffer → 16kHz 16-bit 单声道 WAV Blob
function audioBufferToWav(audioBuffer) {
  const targetRate = 16000;
  const src = audioBuffer.getChannelData(0);
  const ratio = audioBuffer.sampleRate / targetRate;
  const newLen = Math.floor(src.length / ratio);
  const pcm = new Int16Array(newLen);
  for (let i = 0; i < newLen; i++) {
    const idx = Math.floor(i * ratio);
    let s = src[idx] || 0;
    s = Math.max(-1, Math.min(1, s));
    pcm[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
  }
  const buf = new ArrayBuffer(44 + pcm.length * 2);
  const view = new DataView(buf);
  const ws = (o, s) => { for (let i = 0; i < s.length; i++) view.setUint8(o + i, s.charCodeAt(i)); };
  ws(0, 'RIFF'); view.setUint32(4, 36 + pcm.length * 2, true); ws(8, 'WAVE'); ws(12, 'fmt ');
  view.setUint32(16, 16, true); view.setUint16(20, 1, true); view.setUint16(22, 1, true);
  view.setUint32(24, targetRate, true); view.setUint32(28, targetRate * 2, true);
  view.setUint16(32, 2, true); view.setUint16(34, 16, true); ws(36, 'data');
  view.setUint32(40, pcm.length * 2, true);
  let off = 44;
  for (let i = 0; i < pcm.length; i++) { view.setInt16(off, pcm[i], true); off += 2; }
  return new Blob([buf], { type: 'audio/wav' });
}

// 后端 TTS:调用 /api/chat/tts 获取 MP3 并播放
let ttsAudioEl = null;
function speakText(text) {
  if (!text) return false;
  const token = localStorage.getItem('pet_token') || '';
  fetch('http://localhost:8080/api/chat/tts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
    body: JSON.stringify({ text }),
  })
    .then(r => { if (!r.ok) throw new Error('TTS'); return r.blob(); })
    .then(blob => {
      if (ttsAudioEl) URL.revokeObjectURL(ttsAudioEl.src);
      ttsAudioEl = new Audio(URL.createObjectURL(blob));
      ttsAudioEl.volume = 0.85;
      ttsAudioEl.onended = () => { if (ttsAudioEl) URL.revokeObjectURL(ttsAudioEl.src); };
      ttsAudioEl.play().catch(() => {});
      // 通话模式:TTS 结束后恢复监听
      if (callMode.value && ttsAudioEl) {
        ttsAudioEl.onended = () => {
          URL.revokeObjectURL(ttsAudioEl.src);
          voiceReply.value = '';
          if (callMode.value) {
            voiceStatus.value = '📞 通话中…点击⏹说完';
            voiceStatusType.value = 'listening';
            startCallRecording();
          }
        };
      }
    })
    .catch(() => {});
  return true;
}

function stopSpeaking() {
  if (ttsAudioEl) { ttsAudioEl.pause(); URL.revokeObjectURL(ttsAudioEl.src); ttsAudioEl = null; }
}

// 录音+识别:MediaRecorder → WAV → 后端 STT → AI 回复 → TTS
let mediaRecorder = null;
let audioChunks = [];
let callMediaRecorder = null;
let callAudioChunks = [];

async function startMicRecording() {
  const stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, sampleRate: 16000 } });
  audioChunks = [];
  mediaRecorder = new MediaRecorder(stream);
  mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) audioChunks.push(e.data); };
  mediaRecorder.onstop = async () => {
    stream.getTracks().forEach(t => t.stop());
    const webmBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType || 'audio/webm' });
    voiceStatus.value = '正在识别…';
    voiceStatusType.value = 'processing';
    try {
      const arrayBuf = await webmBlob.arrayBuffer();
      const ctx = new (window.AudioContext || window.webkitAudioContext)();
      const audioBuf = await ctx.decodeAudioData(arrayBuf);
      const wavBlob = audioBufferToWav(audioBuf);
      const formData = new FormData();
      formData.append('file', wavBlob, 'voice.wav');
      const token = localStorage.getItem('pet_token') || '';
      const resp = await fetch('http://localhost:8080/api/chat/voice', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token },
        body: formData,
      });
      const json = await resp.json();
      if (json.code !== 200) throw new Error(json.msg || '识别失败');
      const reply = json.data?.reply || '嗯…';
      voiceReply.value = reply;
      voiceStatus.value = '';
      speakText(reply);
    } catch (e) {
      voiceStatus.value = '识别失败:' + e.message;
      voiceStatusType.value = 'error';
      setTimeout(() => { voiceStatus.value = ''; }, 3000);
    }
  };
  mediaRecorder.start();
}

const RANDOM_PLAYS = ['抚摸', '跑步', '玩球', '散步', '听音乐', '跳舞', '捉迷藏', '晒太阳', '扑蝶'];

function updatePixel() {
  clearInterval(blinkTimer);
  if (pet.value && pet.value.hasPet) {
    blinkTimer = setInterval(() => {
      if (!pet.value || !pet.value.hasPet) return;
      const img = document.querySelector('.floating .figure');
      if (img) {
        img.style.transform = img.style.transform + ' scaleY(0.1)';
        setTimeout(() => {
          if (img && img.style.transform) {
            img.style.transform = img.style.transform.replace(' scaleY(0.1)', '');
          }
        }, 160);
      }
    }, 3500);
  }
}

async function refresh() {
  if (!localStorage.getItem('pet_token')) {
    pet.value = null;
    hungry.value = false;
    updatePixel();
    return;
  }
  try {
    pet.value = await http.get('/api/pet/my');
    hungry.value = pet.value && pet.value.hasPet && (pet.value.hunger < 30 || pet.value.status === 'DANGER');
    updatePixel();
  } catch (e) {
    pet.value = null;
    hungry.value = false;
    updatePixel();
  }
}

function hide() {
  playSound('button');
  menuOpen.value = false;
  if (window.petAPI) window.petAPI.hideFloating();
}

function openMenu() {
  playSound('button');
  menuOpen.value = !menuOpen.value;
}

function onPetClick() {
  playSound('button');
  menuOpen.value = false;
  if (pet.value && pet.value.hasPet) {
    randomPetBubble();
  } else {
    showBubble('快去抽一只宠物吧!');
  }
}

function showBubble(text, ms = 3500) {
  if (bubbleTimer) clearTimeout(bubbleTimer);
  bubble.value = text;
  bubbleTimer = setTimeout(() => {
    bubble.value = '';
    bubbleTimer = null;
  }, ms);
}

async function randomPetBubble() {
  try {
    const data = await http.get('/api/chat/bubble', { params: { category: 'random' } });
    if (data && data.content) {
      showBubble(data.content);
    } else {
      const msgs = ['在呢~', '怎么啦?', '嘿嘿~', '主人好!', '想我了吗?'];
      showBubble(msgs[Math.floor(Math.random() * msgs.length)]);
    }
  } catch {
    const msgs = ['在呢~', '怎么啦?', '嘿嘿~', '主人好!', '想我了吗?'];
    showBubble(msgs[Math.floor(Math.random() * msgs.length)]);
  }
}

// 番茄钟完成庆祝特效
const celebrating = ref(false);
const confettiPieces = ref([]);
function celebratePomo() {
  celebrating.value = true;
  showBubble('🎉 太棒啦!专注完成!', 4000);
  spin.value = true;
  setTimeout(() => { spin.value = false; }, 2000);
  confettiPieces.value = [];
  const colors = ['#ff6b6b', '#ffd93d', '#6bcf7f', '#4d96ff', '#c780ff', '#ff9f43'];
  for (let i = 0; i < 20; i++) {
    confettiPieces.value.push({
      id: i,
      left: 10 + Math.random() * 80,
      delay: Math.random() * 0.8,
      color: colors[Math.floor(Math.random() * colors.length)],
      size: 4 + Math.random() * 6,
    });
  }
  setTimeout(() => {
    celebrating.value = false;
    confettiPieces.value = [];
  }, 3500);
}

// === 语音交互:MediaRecorder + 后端 STT ===
function toggleVoice() {
  if (isListening.value) {
    stopListening();
  } else {
    startListening();
  }
}

async function startListening() {
  playSound('button');
  menuOpen.value = false;
  voiceReply.value = '';
  try {
    voiceStatus.value = '正在录音…说话吧';
    voiceStatusType.value = 'listening';
    await startMicRecording();
    isListening.value = true;
  } catch (e) {
    voiceStatus.value = '无法访问麦克风';
    voiceStatusType.value = 'error';
    setTimeout(() => { voiceStatus.value = ''; }, 3000);
  }
}

function stopListening() {
  isListening.value = false;
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop();
  }
  if (voiceStatus.value === '正在录音…说话吧') {
    voiceStatus.value = '';
  }
}

// === 语音通话模式:轮次式录音对话 ===
function toggleCallMode() {
  if (callMode.value) {
    // 通话中:如果在录音就停止录音→处理;如果不在录音就退出
    if (callMediaRecorder && callMediaRecorder.state === 'recording') {
      stopCallRecording();
    } else {
      endCallMode();
    }
  } else {
    startCallMode();
  }
}

async function startCallMode() {
  playSound('button');
  menuOpen.value = false;
  voiceReply.value = '';
  callMode.value = true;
  showBubble('📞 通话开始!点📞说完,再点一次结束', 4000);
  voiceStatus.value = '📞 通话中…点⏹说完';
  voiceStatusType.value = 'listening';
  // 3分钟无操作自动结束
  callIdleTimer = setTimeout(() => {
    if (callMode.value) {
      showBubble('通话超时自动结束~', 3000);
      endCallMode();
    }
  }, 180000);
  await startCallRecording();
}

async function startCallRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, sampleRate: 16000 } });
    callAudioChunks = [];
    callMediaRecorder = new MediaRecorder(stream);
    callMediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) callAudioChunks.push(e.data); };
    callMediaRecorder.onstop = async () => {
      stream.getTracks().forEach(t => t.stop());
      const webmBlob = new Blob(callAudioChunks, { type: callMediaRecorder.mimeType || 'audio/webm' });
      voiceStatus.value = '正在识别…';
      voiceStatusType.value = 'processing';
      try {
        const arrayBuf = await webmBlob.arrayBuffer();
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const audioBuf = await ctx.decodeAudioData(arrayBuf);
        const wavBlob = audioBufferToWav(audioBuf);
        const formData = new FormData();
        formData.append('file', wavBlob, 'voice.wav');
        const token = localStorage.getItem('pet_token') || '';
        const resp = await fetch('http://localhost:8080/api/chat/voice', {
          method: 'POST',
          headers: { 'Authorization': 'Bearer ' + token },
          body: formData,
        });
        const json = await resp.json();
        if (json.code !== 200) throw new Error(json.msg || '识别失败');
        const reply = json.data?.reply || '嗯…';
        voiceReply.value = reply;
        voiceStatus.value = '回复中…';
        voiceStatusType.value = 'processing';
        showBubble('💬 ' + (reply.length > 20 ? reply.slice(0, 20) + '…' : reply), 4000);
        // TTS 播放,onended 里自动恢复录音
        speakText(reply);
      } catch (e) {
        voiceStatus.value = '识别失败,重试中…';
        voiceStatusType.value = 'error';
        setTimeout(() => {
          if (callMode.value) {
            voiceStatus.value = '📞 通话中…点⏹说完';
            voiceStatusType.value = 'listening';
            startCallRecording();
          }
        }, 2000);
      }
    };
    callMediaRecorder.start();
  } catch (e) {
    voiceStatus.value = '无法访问麦克风';
    voiceStatusType.value = 'error';
    callMode.value = false;
    setTimeout(() => { voiceStatus.value = ''; }, 3000);
  }
}

function stopCallRecording() {
  if (callMediaRecorder && callMediaRecorder.state === 'recording') {
    callMediaRecorder.stop();
  }
}

function endCallMode() {
  callMode.value = false;
  voiceStatus.value = '';
  voiceReply.value = '';
  stopSpeaking();
  if (callIdleTimer) { clearTimeout(callIdleTimer); callIdleTimer = null; }
  if (callMediaRecorder && callMediaRecorder.state !== 'inactive') {
    try { callMediaRecorder.stop(); } catch (_) {}
  }
  callMediaRecorder = null;
  showBubble('👋 通话结束~', 2500);
}

async function doAction(action) {
  playSound('button');
  menuOpen.value = false;
  
  if (action === 'feed') {
    if (window.petAPI) window.petAPI.feedPet();
    return;
  }
  if (action === 'study') {
    showBubble('陪你学习,加油!');
    if (window.petAPI) window.petAPI.showMain();
    return;
  }
  if (action === 'showMain') {
    if (window.petAPI) window.petAPI.showMain();
    return;
  }
  if (action === 'hide') {
    hide();
    return;
  }
  const mode = action === '撒娇' ? '抚摸' : RANDOM_PLAYS[Math.floor(Math.random() * RANDOM_PLAYS.length)];
  if (!localStorage.getItem('pet_token')) {
    showBubble('先登录哦~');
    if (window.petAPI) window.petAPI.showMain();
    return;
  }
  try {
    const data = await http.post('/api/pet/play', { mode });
    showBubble(data.message || '好开心!');
  } catch (e) {
    showBubble(e.message);
  }
  refresh();
}

onMounted(() => {
  refresh();
  timer = setInterval(refresh, 60000);
  
  if (window.petAPI && window.petAPI.onBubble) {
    window.petAPI.onBubble((text) => { showBubble(text, 4000); });
  }
  if (window.petAPI && window.petAPI.onSpin) {
    window.petAPI.onSpin(() => {
      spin.value = true;
      showBubble('嘿嘿,转圈圈!', 2500);
      setTimeout(() => (spin.value = false), 1800);
    });
  }
  if (window.petAPI && window.petAPI.onCountdown) {
    window.petAPI.onCountdown((text) => { countdown.value = text; });
  }
  if (window.petAPI && window.petAPI.onPomodoroComplete) {
    window.petAPI.onPomodoroComplete(() => { celebratePomo(); });
  }
  if (window.petAPI && window.petAPI.onPlay) {
    window.petAPI.onPlay(() => doAction('play'));
  }

  const closeMenu = () => { menuOpen.value = false; };
  document.addEventListener('click', closeMenu);

  setTimeout(() => {
    if (pet.value && pet.value.hasPet && !bubble.value) {
      showBubble('主人好呀~', 3000);
    }
  }, 1500);
});

onBeforeUnmount(() => {
  clearInterval(timer);
  clearInterval(blinkTimer);
  if (bubbleTimer) clearTimeout(bubbleTimer);
  stopSpeaking();
  if (mediaRecorder && mediaRecorder.state !== 'inactive') { try { mediaRecorder.stop(); } catch (_) {} }
  endCallMode();
});
</script>

<style scoped>
.floating {
  width: 100vw;
  height: 100vh;
  background: transparent;
  overflow: hidden;
  position: relative;
}

.close {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1.5px solid #e8c890;
  background: rgba(255, 250, 240, 0.95);
  color: #b08050;
  font-size: 13px;
  line-height: 1;
  padding: 0;
  z-index: 30;
  -webkit-app-region: no-drag;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0,0,0,0.12);
  transition: all 0.15s;
}
.close:hover { background: #ffe8d0; color: #c2742e; transform: scale(1.08); }

.drag-zone {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  padding-bottom: 4px;
  -webkit-app-region: drag;
  box-sizing: border-box;
}

/* 番茄钟倒计时徽章 */
.timer-badge {
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 245, 224, 0.95);
  border: 2px solid #f0b870;
  border-radius: 20px;
  padding: 3px 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
  z-index: 15;
  -webkit-app-region: no-drag;
  backdrop-filter: blur(4px);
}
.tm-icon { font-size: 13px; }
.tm-text {
  font-size: 13px;
  font-weight: 700;
  color: #c2742e;
  font-family: 'Courier New', 'Consolas', monospace;
  letter-spacing: 0.5px;
}

/* 宠物 */
.pet {
  position: relative;
  width: 100%;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  -webkit-app-region: no-drag;
  cursor: pointer;
}
.pet.spin { animation: spin360 0.6s linear 3; }
@keyframes spin360 { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.figure {
  width: 72px;
  height: 72px;
  animation: bob 2.8s ease-in-out infinite;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
  filter: drop-shadow(0 4px 6px rgba(0,0,0,0.15));
}
.egg {
  font-size: 48px;
  animation: bob 2.8s ease-in-out infinite;
  filter: drop-shadow(0 4px 6px rgba(0,0,0,0.15));
}

.hungry-mark {
  position: absolute;
  top: -6px;
  right: 12px;
  font-size: 11px;
  background: rgba(255, 224, 224, 0.95);
  color: #d1453b;
  border: 1.5px solid #e87060;
  border-radius: 10px;
  padding: 1px 5px;
  animation: pulse 1.5s ease-in-out infinite;
  z-index: 5;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.08); }
}

/* 气泡 */
.bubble {
  position: absolute;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 253, 248, 0.98);
  border: 2px solid #ffb870;
  border-radius: 14px;
  padding: 5px 10px;
  font-size: 12px;
  color: #6b5030;
  white-space: nowrap;
  box-shadow: 0 3px 10px rgba(0,0,0,0.15);
  animation: bubbleIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 10;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}
.bubble.warn { border-color: #e87060; color: #d1453b; background: rgba(255, 245, 245, 0.98); }
.bubble-tail {
  position: absolute;
  bottom: -7px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 12px;
  height: 12px;
  background: rgba(255, 253, 248, 0.98);
  border-right: 2px solid #ffb870;
  border-bottom: 2px solid #ffb870;
}
.bubble.warn .bubble-tail { background: rgba(255, 245, 245, 0.98); border-right-color: #e87060; border-bottom-color: #e87060; }
@keyframes bubbleIn {
  from { transform: translateX(-50%) translateY(8px) scale(0.7); opacity: 0; }
  to { transform: translateX(-50%) translateY(0) scale(1); opacity: 1; }
}

/* 底部操作栏 */
.action-bar {
  display: flex;
  gap: 4px;
  -webkit-app-region: no-drag;
  z-index: 20;
}
.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid #e8c890;
  background: rgba(255, 250, 240, 0.9);
  color: #8b5a2b;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  backdrop-filter: blur(4px);
  position: relative;
}
.action-btn:hover { background: rgba(255, 232, 208, 0.95); transform: scale(1.08); }
.voice-btn.listening {
  background: rgba(255, 100, 100, 0.9);
  border-color: #e87060;
  color: white;
  animation: voicePulse 1s ease-in-out infinite;
}
.call-btn.active {
  background: rgba(76, 175, 80, 0.9);
  border-color: #4caf50;
  color: white;
  animation: callPulse 1.5s ease-in-out infinite;
}
.call-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid #4caf50;
  animation: callRing 1.5s ease-out infinite;
}
@keyframes voicePulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255,100,100,0.4); }
  50% { box-shadow: 0 0 0 8px rgba(255,100,100,0); }
}
@keyframes callPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(76,175,80,0.4); }
  50% { box-shadow: 0 0 0 6px rgba(76,175,80,0); }
}
@keyframes callRing {
  0% { transform: scale(1); opacity: 0.8; }
  100% { transform: scale(1.8); opacity: 0; }
}

/* 声波动画 */
.voice-wave {
  display: flex;
  gap: 2px;
  align-items: center;
  height: 16px;
}
.voice-wave span {
  width: 3px;
  background: white;
  border-radius: 2px;
  animation: wave 0.6s ease-in-out infinite alternate;
}
.voice-wave span:nth-child(1) { height: 8px; animation-delay: 0s; }
.voice-wave span:nth-child(2) { height: 14px; animation-delay: 0.15s; }
.voice-wave span:nth-child(3) { height: 10px; animation-delay: 0.3s; }
@keyframes wave { 0% { height: 4px; } 100% { height: 16px; } }

/* 菜单 */
.pet-menu {
  position: absolute;
  bottom: 90px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: rgba(255, 253, 248, 0.98);
  border: 1.5px solid #e8d0a8;
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 4px 14px rgba(0,0,0,0.18);
  z-index: 25;
  -webkit-app-region: no-drag;
  min-width: 110px;
  animation: menuIn 0.2s ease-out;
  backdrop-filter: blur(4px);
}
@keyframes menuIn {
  from { transform: translateX(-50%) translateY(6px); opacity: 0; }
  to { transform: translateX(-50%) translateY(0); opacity: 1; }
}
.pet-menu button {
  border: none;
  background: transparent;
  padding: 6px 12px;
  font-size: 13px;
  text-align: left;
  border-radius: 8px;
  cursor: pointer;
  color: #5a4530;
  transition: background 0.12s;
  white-space: nowrap;
}
.pet-menu button:hover { background: #fff3e0; color: #c2742e; }

/* 浮动动画 */
@keyframes bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-7px); }
}

/* AI回复气泡(语音模式) */
.voice-reply-bubble {
  position: absolute;
  bottom: 85px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 253, 248, 0.98);
  border: 2px solid #c780ff;
  border-radius: 14px;
  padding: 8px 12px;
  font-size: 12px;
  color: #5a4530;
  box-shadow: 0 4px 14px rgba(0,0,0,0.18);
  z-index: 35;
  max-width: 200px;
  min-width: 80px;
  line-height: 1.5;
  word-break: break-word;
  animation: bubbleIn 0.3s ease-out;
}
.vr-close {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1.5px solid #c780ff;
  background: white;
  color: #c780ff;
  font-size: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 语音状态 */
.voice-status {
  position: absolute;
  bottom: 50px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 10px;
  z-index: 30;
  white-space: nowrap;
  animation: bubbleIn 0.3s ease-out;
}
.voice-status.listening { background: rgba(255, 100, 100, 0.9); color: white; }
.voice-status.processing { background: rgba(255, 159, 67, 0.9); color: white; }
.voice-status.error { background: rgba(200, 40, 40, 0.9); color: white; }

/* 撒花庆祝 */
.confetti-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
  z-index: 50;
}
.confetti-piece {
  position: absolute;
  top: -10px;
  border-radius: 2px;
  animation: confettiFall 2.5s ease-out forwards;
}
@keyframes confettiFall {
  0% { transform: translateY(0) rotate(0deg); opacity: 1; }
  100% { transform: translateY(180px) rotate(720deg); opacity: 0; }
}
</style>
