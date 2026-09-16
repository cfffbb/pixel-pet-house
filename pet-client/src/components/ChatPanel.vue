<template>
  <div class="chat" :style="{ backgroundImage: `url('${chatBg}')` }">
    <div class="chat-dim"></div>
    <div class="msg-list" ref="listEl">
      <div v-for="(m, idx) in messages" :key="m.id">
        <!-- 日期分隔(微信式) -->
        <div v-if="idx === 0 || dayOf(messages[idx - 1]) !== dayOf(m)" class="day">{{ dayText(m.createdAt) }}</div>
        <div class="msg" :class="m.role === 'USER' ? 'me' : 'pet'">
          <PetFigure v-if="m.role !== 'USER'" :type-code="petTypeCode" :gender="petGender" :stage="petStage" :size="38" class="avatar" />
          <div class="col">
            <div class="bubble-wrap">
              <span class="bubble">{{ m.content }}</span>
              <div class="bubble-tail" v-if="m.role !== 'USER'"></div>
              <div class="bubble-tail-r" v-if="m.role === 'USER'"></div>
            </div>
            <span class="msg-time">{{ m.role === 'USER' && m.isVoice === 1 ? '🎤 ' : '' }}{{ timeOf(m.createdAt) }}</span>
          </div>
        </div>
      </div>
      <p v-if="!messages.length" class="muted center empty-hint">和宠物说说话吧(打字或录音)</p>
      <p v-if="thinking" class="muted center thinking-hint">宠物正在想…</p>
    </div>

    <div class="input-bar">
      <input v-model="text" placeholder="说点什么…" @keyup.enter="sendText" :disabled="recording || thinking" />
      <button class="rec-btn" :class="{ on: recording }" @click="toggleRecord" :disabled="thinking">
        {{ recording ? '⏹' : '🎤' }}
      </button>
      <button class="send-btn" @click="sendText" :disabled="recording || thinking || !text.trim()">发送</button>
    </div>
    <p v-if="recording" class="muted small center rec-hint">🎙️ 正在录音…说完再点一次 ⏹ 结束</p>
    <p v-if="err" class="error center">{{ err }}</p>
    <p class="muted small center tips">点击 🎤 录音说话(自动转文字+AI回复),或直接打字</p>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue';
import http from '../api/http';
import PetFigure from './PetFigure.vue';
import { useSound } from '../soundStore';
import { useTheme } from '../themeStore';

const { playSound, getVoiceVolume } = useSound();
const { chatBgUrl } = useTheme();
const chatBg = computed(() => encodeURI(chatBgUrl()));

const messages = ref([]);
const text = ref('');
const thinking = ref(false);
const recording = ref(false);
const err = ref('');
const listEl = ref(null);
const petTypeCode = ref('CAT');
const petGender = ref('MALE');
const petStage = ref('ADULT');
const ttsEnabled = ref(true);

// ── WAV 编码:将 AudioBuffer 转为 16kHz 16-bit 单声道 WAV ──
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
  const writeStr = (off, str) => { for (let i = 0; i < str.length; i++) view.setUint8(off + i, str.charCodeAt(i)); };
  writeStr(0, 'RIFF');
  view.setUint32(4, 36 + pcm.length * 2, true);
  writeStr(8, 'WAVE');
  writeStr(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, targetRate, true);
  view.setUint32(28, targetRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeStr(36, 'data');
  view.setUint32(40, pcm.length * 2, true);
  let off = 44;
  for (let i = 0; i < pcm.length; i++) { view.setInt16(off, pcm[i], true); off += 2; }
  return new Blob([buf], { type: 'audio/wav' });
}

// 后端 TTS:调用 /api/chat/tts 获取 MP3 并播放
let ttsAudioEl = null;
function speakText(text) {
  if (!text) return;
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
      ttsAudioEl.volume = getVoiceVolume() || 0.85;
      ttsAudioEl.play().catch(() => {});
    })
    .catch(() => {});
}

// ── 语音录音:MediaRecorder + WAV 转换 + 后端 STT ──
let mediaRecorder = null;
let audioChunks = [];

function dayOf(t) {
  return t ? String(t).slice(0, 10) : '';
}
function dayText(t) {
  return t ? String(t).slice(5, 10) : '';
}
function timeOf(t) {
  return t ? String(t).slice(11, 16) : '';
}

async function loadHistory() {
  const list = await http.get('/api/chat/history');
  messages.value = [...list].reverse();
  await nextTick();
  if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight;
}

async function loadPet() {
  try {
    const pet = await http.get('/api/pet/my');
    if (pet && pet.hasPet) {
      petTypeCode.value = pet.typeCode;
      petGender.value = pet.gender;
      petStage.value = pet.stage;
    }
  } catch (e) {}
}

async function sendText() {
  if (!text.value.trim()) return;
  playSound('button');
  thinking.value = true;
  err.value = '';
  try {
    const data = await http.post('/api/chat/message', { content: text.value });
    text.value = '';
    await loadHistory();
    if (ttsEnabled.value && data.reply) {
      speakText(data.reply);
    }
  } catch (e) {
    err.value = e.message;
  }
  thinking.value = false;
}

async function toggleRecord() {
  if (recording.value) { stopRecord(); return; }
  err.value = '';
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, sampleRate: 16000 } });
    audioChunks = [];
    mediaRecorder = new MediaRecorder(stream);
    mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) audioChunks.push(e.data); };
    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach(t => t.stop());
      const webmBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType || 'audio/webm' });
      thinking.value = true;
      err.value = '正在识别语音…';
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
        if (json.code !== 200) throw new Error(json.msg || '语音识别失败');
        err.value = '';
        await loadHistory();
        if (ttsEnabled.value && json.data && json.data.reply) {
          speakText(json.data.reply);
        }
      } catch (e) {
        err.value = '语音识别失败:' + e.message;
      }
      thinking.value = false;
    };
    mediaRecorder.start();
    recording.value = true;
  } catch (e) {
    err.value = '无法访问麦克风:' + e.message;
  }
}

function stopRecord() {
  recording.value = false;
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop();
  }
}

onMounted(async () => {
  loadHistory();
  loadPet();
  // 加载 TTS 偏好
  try {
    const pref = await http.get('/api/chat/voice-pref');
    ttsEnabled.value = pref.ttsEnabled === 1;
  } catch (e) {}
});
</script>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - clamp(220px, 30vh, 300px) - 160px);
  min-height: 360px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* 背景遮罩层 */
.chat-dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.25);
  pointer-events: none;
  z-index: 0;
}

/* ── 消息列表 ── */
.msg-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
  background: transparent;
  border-radius: var(--radius-lg);
  position: relative;
  z-index: 1;
}
.day {
  text-align: center;
  color: var(--muted);
  font-size: 11px;
  margin: 10px 0;
  background: var(--fg-soft);
  border-radius: var(--radius-xl);
  padding: 2px 10px;
  display: inline-block;
  width: 100%;
  box-sizing: border-box;
}
.msg {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
.msg.me {
  justify-content: flex-end;
}
.avatar {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
}
.col {
  max-width: 72%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.msg.me .col {
  align-items: flex-end;
}

/* 气泡容器:定位容器,包裹气泡和尾巴 */
.bubble-wrap {
  position: relative;
  max-width: 100%;
  display: block;
}
.bubble {
  display: block;
  padding: 10px 14px;
  border-radius: 16px;
  background: var(--surface);
  border: 2px solid var(--border);
  line-height: 1.6;
  font-size: 14px;
  word-break: break-word;
  overflow-wrap: break-word;
  box-shadow: 0 2px 0 var(--border);
  max-width: 100%;
  white-space: pre-wrap;
}
.msg.me .bubble {
  background: var(--accent);
  color: var(--surface);
  border-color: color-mix(in oklch, var(--accent) 60%, black);
  box-shadow: 0 2px 0 color-mix(in oklch, var(--accent) 60%, black);
  border-bottom-right-radius: 5px;
}
.msg.pet .bubble {
  border-bottom-left-radius: 5px;
}

/* 气泡小尾巴:纯 CSS 三角形,贴合气泡底部 */
.bubble-tail {
  position: absolute;
  left: -7px;
  bottom: 6px;
  width: 0;
  height: 0;
  border-top: 7px solid transparent;
  border-bottom: 7px solid transparent;
  border-right: 8px solid var(--surface);
  filter: drop-shadow(-1px 0 0 var(--border));
}
.bubble-tail-r {
  position: absolute;
  right: -7px;
  bottom: 6px;
  width: 0;
  height: 0;
  border-top: 7px solid transparent;
  border-bottom: 7px solid transparent;
  border-left: 8px solid var(--accent);
  filter: drop-shadow(1px 0 0 color-mix(in oklch, var(--accent) 60%, black));
}

/* 消息时间戳 */
.msg-time {
  font-size: 11px;
  color: var(--muted);
  padding: 0 6px;
  line-height: 1.2;
}
.small {
  font-size: 11px;
  margin-top: 3px;
}
.center {
  text-align: center;
}
.empty-hint, .thinking-hint {
  padding: 20px 0;
}

/* ── 输入栏(固定在底部,不需滚动即可看到) ── */
.input-bar {
  display: flex;
  gap: 8px;
  padding: 10px 0 6px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}
.input-bar input {
  flex: 1;
  width: auto !important;
  padding: 10px 14px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 14px !important;
  font-size: 14px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  font-family: var(--font-body) !important;
  outline: none !important;
  box-shadow: none !important;
  transition: border-color 0.15s !important;
}
.input-bar input:focus {
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 3px var(--accent-soft) !important;
}
.input-bar input:disabled {
  opacity: 0.5;
}

/* 录音按钮 */
.rec-btn {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  padding: 0 !important;
  border: 2.5px solid var(--p-line) !important;
  border-radius: 50% !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  font-size: 18px !important;
  box-shadow: 0 3px 0 color-mix(in oklch, var(--p-line) 45%, transparent) !important;
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.1s !important;
  display: grid;
  place-items: center;
}
.rec-btn:hover { transform: translateY(-1px); }
.rec-btn:active { transform: translateY(2px); box-shadow: 0 1px 0 color-mix(in oklch, var(--p-line) 45%, transparent) !important; }
.rec-btn.on {
  background: var(--st-mood, #d1453b) !important;
  color: var(--surface) !important;
  border-color: color-mix(in oklch, var(--st-mood) 60%, black) !important;
  box-shadow: 0 3px 0 color-mix(in oklch, var(--st-mood) 60%, black) !important;
  animation: recPulse 1s ease-in-out infinite;
}
.rec-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
@keyframes recPulse {
  0%, 100% { box-shadow: 0 3px 0 color-mix(in oklch, var(--st-mood) 60%, black) !important; }
  50% { box-shadow: 0 3px 0 color-mix(in oklch, var(--st-mood) 60%, black), 0 0 12px var(--st-mood) !important; }
}

/* 发送按钮 */
.send-btn {
  padding: 0 20px !important;
  height: 44px;
  flex-shrink: 0;
  border: 2.5px solid color-mix(in oklch, var(--accent) 55%, black) !important;
  border-radius: 14px !important;
  background: var(--accent) !important;
  color: var(--surface) !important;
  font-size: 15px !important;
  font-family: var(--font-display) !important;
  box-shadow: 0 3px 0 color-mix(in oklch, var(--accent) 55%, black) !important;
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.1s !important;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.send-btn:hover { transform: translateY(-1px); background: color-mix(in oklch, var(--accent) 85%, white) !important; }
.send-btn:active { transform: translateY(2px); box-shadow: 0 1px 0 color-mix(in oklch, var(--accent) 55%, black) !important; }
.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none !important;
}

.rec-hint {
  margin: 4px 0;
}
.tips {
  margin-top: 6px;
  font-size: 11px;
}
</style>
