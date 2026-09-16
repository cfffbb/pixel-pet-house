<template>
  <div>
    <!-- 配种授权开关 -->
    <div class="card">
      <h3>🐾 配种授权</h3>
      <p class="muted">开启后,其他玩家可在配种市场看到你的公宠物(只显示昵称/种类/亚种/性别/性格)</p>
      <label class="switch">
        <input type="checkbox" v-model="allowBreeding" @change="saveAllowBreeding" />
        <span>允许我的宠物参与配种</span>
      </label>
      <p v-if="setMsg" class="ok">{{ setMsg }}</p>
      <p v-if="setErr" class="error">{{ setErr }}</p>
    </div>

    <!-- 应用感知 -->
    <div class="card">
      <h3>👀 应用感知(陪伴聊天泡)</h3>
      <p class="muted">开启后,每 5 分钟采样一次当前使用的应用(仅进程名/窗口标题),宠物随机发聊天泡;记录保留 7 天。默认关。</p>
      <label class="switch">
        <input type="checkbox" v-model="appMonitor" @change="saveAppMonitor" />
        <span>允许应用感知</span>
      </label>
      <p v-if="monitorMsg" class="ok">{{ monitorMsg }}</p>
      <p v-if="monitorErr" class="error">{{ monitorErr }}</p>
    </div>

    <!-- API Key 管理(极简版) -->
    <div class="card">
      <h3>🔑 我的 AI Key(对话 / 语音)</h3>
      <p class="muted">只需两步:选服务商 → 粘贴 Key。接口地址和模型名会自动填好,不用管。</p>

      <!-- 已添加的 Key 列表 -->
      <div v-if="keys.length" class="key-list">
        <div v-for="k in keys" :key="k.id" class="key-row">
          <div class="key-row-info">
            <span class="key-provider-badge" :class="'p-' + k.provider">{{ labelOf(k.provider) }}</span>
            <span class="key-masked">{{ k.keyMasked }}</span>
            <span v-if="k.stt" class="key-voice-tag">🎤 支持语音</span>
          </div>
          <button class="small danger" @click="removeKey(k.id)">删除</button>
        </div>
      </div>

      <!-- 快速添加:只需选服务商+粘贴Key -->
      <div class="quick-add-section">
        <div class="quick-add-title">添加新 Key</div>
        <div class="provider-grid">
          <button
            v-for="p in providers"
            :key="p.name"
            class="provider-card"
            :class="{ active: form.provider === p.name }"
            @click="selectProvider(p.name)"
          >
            <span class="pc-icon">{{ p.icon }}</span>
            <span class="pc-name">{{ p.label }}</span>
            <span v-if="p.stt" class="pc-badge">🎤语音</span>
          </button>
        </div>

        <div class="key-input-row">
          <input
            v-model="form.key"
            :placeholder="keyHint ? '在此粘贴 ' + keyHint : '在此粘贴 API Key'"
            class="key-input"
            @keyup.enter="addKey"
          />
          <button class="pix-btn btn-primary" @click="addKey" :disabled="!form.key.trim()">添加</button>
        </div>

        <!-- 高级选项(默认折叠) -->
        <details class="advanced-toggle">
          <summary>高级选项(接口地址 / 模型名)</summary>
          <div class="advanced-fields">
            <input v-model="form.baseUrl" placeholder="接口地址(已自动填好,一般不用改)" />
            <input v-model="form.modelName" placeholder="模型名(已自动填好,可改)" />
          </div>
        </details>
      </div>

      <!-- 加密说明 -->
      <details class="encrypt-info">
        <summary>🔒 Key 是怎么加密存储的?</summary>
        <div class="encrypt-detail">
          <p>• 你的 Key 通过 <b>AES-256</b> 加密后存入数据库,任何人(包括管理员)都无法看到完整 Key。</p>
          <p>• 列表里只显示 Key 的前4位和后4位(如 <code>sk-x...mzly</code>),中间用 * 代替。</p>
          <p>• 调用 AI 时系统会在内存中临时解密,用完即销毁,不会写入日志。</p>
          <p>• 删除 Key 后数据库中的密文也会一并删除,不会保留。</p>
        </div>
      </details>

      <!-- 使用提示 -->
      <div class="api-tip-box">
        <p class="muted small">💡 想用语音对话?选带 🎤 标记的服务商(如 OpenAI、硅基流动)。讯飞语音需在管理后台配置。</p>
        <p class="muted small">💡 只想打字聊天?随便选一个都行,推荐 硅基流动(免费额度多)。</p>
      </div>

      <p v-if="keyMsg" class="ok">{{ keyMsg }}</p>
      <p v-if="keyErr" class="error">{{ keyErr }}</p>
    </div>

    <!-- 语音 / TTS 设置 -->
    <div class="card">
      <h3>🔊 语音与朗读设置</h3>
      <p class="muted">配置 AI 回复是否语音朗读、选择音色和语速。需管理员已配置语音 API 或你自己配置了 OpenAI/硅基流动 Key。</p>

      <label class="switch">
        <input type="checkbox" v-model="voicePref.useAdminKey" :true-value="1" :false-value="0" @change="saveVoicePref" />
        <span>使用管理员配置的 API(关闭则使用自己的 Key)</span>
      </label>

      <label class="switch">
        <input type="checkbox" v-model="voicePref.ttsEnabled" :true-value="1" :false-value="0" @change="saveVoicePref" />
        <span>开启 AI 回复语音朗读</span>
      </label>

      <label class="switch">
        <input type="checkbox" v-model="voicePref.bubbleRead" :true-value="1" :false-value="0" @change="saveVoicePref" />
        <span>气泡提示也朗读(本地随机触发的气泡)</span>
      </label>

      <div class="voice-field">
        <label>TTS 音色</label>
        <select v-model="voicePref.voice" @change="saveVoicePref" class="voice-select">
          <option v-for="v in availableVoices" :key="v" :value="v">{{ voiceLabel(v) }}</option>
        </select>
      </div>

      <div class="voice-field">
        <label>语速: {{ voicePref.ttsSpeed }}x</label>
        <input type="range" v-model.number="voicePref.ttsSpeed" min="0.5" max="2.0" step="0.1" class="voice-slider" @change="saveVoicePref" />
      </div>

      <p v-if="voiceMsg" class="ok">{{ voiceMsg }}</p>
      <p v-if="voiceErr" class="error">{{ voiceErr }}</p>
    </div>

    <!-- 音效与背景音乐 -->
    <div class="card">
      <h3>🎵 音效与背景音乐</h3>
      <p class="muted">调节各类音量,设置会自动保存。管理员可在后台导入背景音乐。</p>

      <!-- 播放器主体 -->
      <div class="music-player">
        <div class="player-info">
          <div class="player-icon">🎵</div>
          <div class="player-track-info">
            <div class="track-name">{{ currentTrack ? currentTrack.name : '暂无音乐' }}</div>
            <div class="track-time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</div>
          </div>
        </div>

        <!-- 进度条 -->
        <div class="progress-bar" @click="onProgressClick">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          <div class="progress-thumb" :style="{ left: progressPercent + '%' }"></div>
        </div>

        <!-- 控制按钮 -->
        <div class="player-controls">
          <button class="ctrl-btn" :class="{ active: isShuffle }" @click="onShuffle" title="随机播放">🔀</button>
          <button class="ctrl-btn" @click="onPrev" title="上一首">⏮</button>
          <button class="ctrl-btn play-btn" @click="toggleBg" :title="bgMusicEnabled ? '暂停' : '播放'">
            {{ isPlaying ? '⏸' : '▶️' }}
          </button>
          <button class="ctrl-btn" @click="onNext" title="下一首">⏭</button>
          <button class="ctrl-btn" @click="onRepeat" :title="repeatLabel">
            {{ repeatIcon }}
          </button>
        </div>
      </div>

      <!-- 播放列表 -->
      <div class="playlist-section">
        <div class="playlist-header">
          <span class="playlist-title">📋 播放列表 ({{ playlist.length }})</span>
          <button class="pix-btn btn-soft btn-sm" @click="importMusic">📁 导入音乐</button>
          <button v-if="playlist.length" class="pix-btn btn-ghost btn-sm" @click="clearPlaylist">清空</button>
        </div>
        <div class="playlist-items">
          <div
            v-for="(track, idx) in playlist"
            :key="track.id"
            class="playlist-item"
            :class="{ active: idx === currentTrackIndex }"
            @click="onPlayTrack(idx)"
          >
            <span class="track-index">{{ idx + 1 }}</span>
            <span class="track-name">{{ track.name }}</span>
            <span v-if="idx === currentTrackIndex && isPlaying" class="playing-indicator">♪</span>
          </div>
          <p v-if="!playlist.length" class="muted" style="text-align:center;padding:16px">
            播放列表为空,点击上方"导入音乐"按钮选择本地音频文件
          </p>
        </div>
      </div>

      <!-- 音量控制 -->
      <div class="volume-section">
        <div class="sound-field">
          <label><span>🔊 背景音乐音量</span></label>
          <input type="range" v-model.number="bgVol" min="0" max="1" step="0.05" class="sound-slider" @input="onBgVol" />
          <span class="vol-text">{{ Math.round(bgVol * 100) }}%</span>
        </div>

        <div class="sound-field">
          <label><span>🔔 UI 音效(按钮/操作)</span></label>
          <input type="range" v-model.number="uiVol" min="0" max="1" step="0.05" class="sound-slider" @input="onUiVol" />
          <span class="vol-text">{{ Math.round(uiVol * 100) }}%</span>
          <button class="pix-btn btn-ghost btn-sm" @click="testUiSound">试听</button>
        </div>

        <div class="sound-field">
          <label><span>🎙️ 语音音量(TTS 朗读)</span></label>
          <input type="range" v-model.number="voiceVol" min="0" max="1" step="0.05" class="sound-slider" @input="onVoiceVol" />
          <span class="vol-text">{{ Math.round(voiceVol * 100) }}%</span>
        </div>
      </div>

      <p v-if="soundMsg" class="ok">{{ soundMsg }}</p>
    </div>

    <!-- 悬浮窗设置 -->
    <div class="card">
      <h3>🐾 桌面悬浮宠物</h3>
      <p class="muted">开启后,宠物会以小窗口形式常驻桌面,陪你学习和工作。</p>
      
      <label class="switch">
        <input type="checkbox" v-model="floatingEnabled" @change="saveFloatingSetting" />
        <span>开启桌面悬浮宠物</span>
      </label>
      
      <div class="floating-actions" style="margin-top: 10px">
        <button class="pix-btn btn-soft btn-sm" @click="showFloatingWindow">📌 显示悬浮窗</button>
        <button class="pix-btn btn-ghost btn-sm" @click="hideFloatingWindow">🙈 隐藏悬浮窗</button>
      </div>
      
      <p v-if="floatMsg" class="ok">{{ floatMsg }}</p>
      <p class="muted small" style="margin-top: 8px">
        💡 小提示:悬浮窗可拖拽移动,右键弹出菜单,点击宠物会说话
      </p>
    </div>

    <!-- 小屋主题风格 -->
    <div class="card">
      <h3>🏠 小屋主题风格</h3>
      <p class="muted">选择你喜欢的房间风格,三种风格严格封装,不会杂交。</p>
      <div class="theme-grid">
        <button
          v-for="t in themes"
          :key="t.id"
          class="theme-card"
          :class="{ active: currentTheme === t.id }"
          @click="switchTheme(t.id)"
        >
          <span class="theme-icon">{{ t.icon }}</span>
          <span class="theme-name">{{ t.name }}</span>
        </button>
      </div>
      <label class="switch" style="margin-top:10px">
        <input type="checkbox" v-model="autoTime" @change="toggleAutoTime" />
        <span>自动昼夜(根据现实时间)</span>
      </label>
      <p v-if="themeMsg" class="ok">{{ themeMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import http from '../api/http';
import { useTheme, THEMES } from '../themeStore';
import { useSound } from '../soundStore';

defineProps({ user: Object });

const { themeId, setTheme, autoTime: themeAutoTime, setAutoTime } = useTheme();

// 音效系统
const {
  bgVolume, voiceVolume, uiVolume, bgMusicEnabled,
  playlist, currentTrackIndex, currentTrack, currentTime, duration,
  isPlaying, isShuffle, repeatMode,
  setBgVolume, setVoiceVolume, setUiVolume,
  toggleBgMusic, playSound, playNext, playPrev, playTrack,
  seekBgMusic, toggleShuffle, setRepeatMode, formatTime,
  addTrack, removeTrack, stopBgMusic,
} = useSound();
const bgVol = ref(bgVolume.value);
const uiVol = ref(uiVolume.value);
const voiceVol = ref(voiceVolume.value);
const soundMsg = ref('');

// 进度百分比
const progressPercent = computed(() => {
  if (!duration.value) return 0;
  return (currentTime.value / duration.value) * 100;
});

// 循环模式图标和标签
const repeatIcon = computed(() => {
  switch (repeatMode.value) {
    case 'one': return '🔂';
    case 'all': return '🔁';
    default: return '➡️';
  }
});
const repeatLabel = computed(() => {
  switch (repeatMode.value) {
    case 'one': return '单曲循环';
    case 'all': return '列表循环';
    default: return '顺序播放';
  }
});

function onBgVol() { setBgVolume(bgVol.value); }
function onUiVol() { setUiVolume(uiVol.value); }
function onVoiceVol() { setVoiceVolume(voiceVol.value); }
function toggleBg() {
  toggleBgMusic();
  soundMsg.value = bgMusicEnabled.value ? '已开始播放' : '已暂停';
  setTimeout(() => { soundMsg.value = ''; }, 1500);
}
function testUiSound() {
  playSound('button');
  soundMsg.value = '试听完毕';
  setTimeout(() => { soundMsg.value = ''; }, 1500);
}
function onNext() {
  playNext();
  playSound('button');
}
function onPrev() {
  playPrev();
  playSound('button');
}
function onPlayTrack(idx) {
  playTrack(idx);
  playSound('button');
}
function onShuffle() {
  toggleShuffle();
  playSound('button');
  soundMsg.value = isShuffle.value ? '已开启随机播放' : '已关闭随机播放';
  setTimeout(() => { soundMsg.value = ''; }, 1500);
}
function onRepeat() {
  const modes = ['all', 'one', 'none'];
  const currentIdx = modes.indexOf(repeatMode.value);
  const nextMode = modes[(currentIdx + 1) % modes.length];
  setRepeatMode(nextMode);
  playSound('button');
  const labels = { all: '列表循环', one: '单曲循环', none: '顺序播放' };
  soundMsg.value = '已切换为 ' + labels[nextMode];
  setTimeout(() => { soundMsg.value = ''; }, 1500);
}

// 导入本地音乐文件
function importMusic() {
  playSound('button');
  if (window.petAPI && window.petAPI.importMusic) {
    window.petAPI.importMusic().then((result) => {
      if (result && result.tracks && result.tracks.length) {
        for (const t of result.tracks) {
          addTrack(t);
        }
        soundMsg.value = `已导入 ${result.tracks.length} 首音乐`;
        setTimeout(() => { soundMsg.value = ''; }, 2000);
      }
    }).catch(() => {
      soundMsg.value = '导入失败';
      setTimeout(() => { soundMsg.value = ''; }, 2000);
    });
  } else {
    // 非Electron环境:用文件选择器
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'audio/*';
    input.multiple = true;
    input.onchange = (e) => {
      const files = Array.from(e.target.files);
      for (const f of files) {
        const url = URL.createObjectURL(f);
        addTrack({ name: f.name.replace(/\.[^/.]+$/, ''), url, source: 'local' });
      }
      if (files.length) {
        soundMsg.value = `已导入 ${files.length} 首音乐`;
        setTimeout(() => { soundMsg.value = ''; }, 2000);
      }
    };
    input.click();
  }
}

function clearPlaylist() {
  playSound('button');
  stopBgMusic();
  playlist.value = [];
  currentTrackIndex.value = -1;
  try { localStorage.removeItem('pet_playlist'); } catch (_) {}
  soundMsg.value = '播放列表已清空';
  setTimeout(() => { soundMsg.value = ''; }, 1500);
}
function onProgressClick(e) {
  if (!duration.value) return;
  const rect = e.currentTarget.getBoundingClientRect();
  const percent = (e.clientX - rect.left) / rect.width;
  const time = percent * duration.value;
  seekBgMusic(time);
}
// 悬浮窗设置
const floatingEnabled = ref(localStorage.getItem('pet_floating') !== '0'); // 默认开启
const floatMsg = ref('');

function saveFloatingSetting() {
  playSound('button');
  localStorage.setItem('pet_floating', floatingEnabled.value ? '1' : '0');
  if (floatingEnabled.value) {
    showFloatingWindow();
    floatMsg.value = '已开启桌面悬浮宠物';
  } else {
    hideFloatingWindow();
    floatMsg.value = '已关闭桌面悬浮宠物';
  }
  setTimeout(() => { floatMsg.value = ''; }, 2000);
}

function showFloatingWindow() {
  playSound('button');
  if (window.petAPI && window.petAPI.showFloating) {
    window.petAPI.showFloating();
    floatMsg.value = '悬浮窗已显示';
  } else {
    floatMsg.value = '当前环境不支持悬浮窗';
  }
  setTimeout(() => { floatMsg.value = ''; }, 2000);
}

function hideFloatingWindow() {
  playSound('button');
  if (window.petAPI && window.petAPI.hideFloating) {
    window.petAPI.hideFloating();
    floatMsg.value = '悬浮窗已隐藏';
  }
  setTimeout(() => { floatMsg.value = ''; }, 2000);
}

const currentTheme = ref(themeId.value);
const themes = THEMES;
const autoTime = ref(themeAutoTime.value);
const themeMsg = ref('');

function switchTheme(id) {
  setTheme(id);
  currentTheme.value = id;
  themeMsg.value = '已切换为 ' + THEMES.find(t => t.id === id).name;
  setTimeout(() => { themeMsg.value = ''; }, 2000);
}
function toggleAutoTime() {
  setAutoTime(autoTime.value);
}

const allowBreeding = ref(true);
const appMonitor = ref(false);
const setMsg = ref('');
const setErr = ref('');
const monitorMsg = ref('');
const monitorErr = ref('');
const keys = ref([]);
const providers = ref([]);
const keyHint = ref('');
const keyMsg = ref('');
const keyErr = ref('');
const form = ref({ provider: '', key: '', baseUrl: '', modelName: '' });

// 语音偏好
const voicePref = ref({ useAdminKey: 1, ttsEnabled: 0, voice: 'alloy', ttsSpeed: 1.0, bubbleRead: 0 });
const availableVoices = ref(['alloy', 'echo', 'fable', 'onyx', 'nova', 'shimmer']);
const voiceMsg = ref('');
const voiceErr = ref('');

function voiceLabel(v) {
  return { alloy: 'Alloy(中性)', echo: 'Echo(男声)', fable: 'Fable(叙事)', onyx: 'Onyx(深沉)', nova: 'Nova(女声)', shimmer: 'Shimmer(清亮)' }[v] || v;
}

async function loadVoicePref() {
  try {
    const data = await http.get('/api/chat/voice-pref');
    voicePref.value = {
      useAdminKey: data.useAdminKey ?? 1,
      ttsEnabled: data.ttsEnabled ?? 0,
      voice: data.voice || 'alloy',
      ttsSpeed: data.ttsSpeed || 1.0,
      bubbleRead: data.bubbleRead ?? 0,
    };
    if (data.availableVoices) availableVoices.value = data.availableVoices;
  } catch (e) {}
}

async function saveVoicePref() {
  voiceMsg.value = ''; voiceErr.value = '';
  try {
    await http.put('/api/chat/voice-pref', { ...voicePref.value });
    voiceMsg.value = '已保存';
    setTimeout(() => { voiceMsg.value = ''; }, 2000);
  } catch (e) { voiceErr.value = e.message; }
}

function labelOf(name) {
  const p = providers.value.find((x) => x.name === name);
  return p ? p.label : name;
}

async function load() {
  try {
    const s = await http.get('/api/settings');
    allowBreeding.value = s.allowBreeding;
    appMonitor.value = s.appMonitorEnabled;
    keys.value = await http.get('/api/keys');
    providers.value = await http.get('/api/keys/providers');
    // 补全 icon 字段
    for (const p of providers.value) {
      p.icon = providerIcon(p.name);
    }
    if (providers.value.length) {
      form.value.provider = providers.value[0].name;
      onProviderChange();
    }
    await loadVoicePref();
  } catch (e) {
    setErr.value = e.message;
  }
}

function onProviderChange() {
  const p = providers.value.find((x) => x.name === form.value.provider);
  if (!p) return;
  form.value.baseUrl = p.baseUrl || '';
  form.value.modelName = p.models && p.models.length ? p.models[0] : '';
  keyHint.value = p.keyHint;
}

// 服务商图标映射
const PROVIDER_ICONS = {
  openai: '🟢',
  siliconflow: '🔥',
  qwen: '☁️',
  zhipu: '✨',
  deepseek: '🌊',
  xfyun: '🧠',
  custom: '⚙️',
};

function selectProvider(name) {
  form.value.provider = name;
  onProviderChange();
}

// 补全 provider 的 icon 字段
function providerIcon(name) {
  return PROVIDER_ICONS[name] || '🔑';
}

async function saveAllowBreeding() {
  setMsg.value = '';
  setErr.value = '';
  try {
    await http.put('/api/settings/allow-breeding', { value: allowBreeding.value });
    setMsg.value = '已保存';
  } catch (e) {
    setErr.value = e.message;
  }
}

async function saveAppMonitor() {
  monitorMsg.value = '';
  monitorErr.value = '';
  if (appMonitor.value && !window.confirm('开启应用感知:每 5 分钟采样当前应用(仅进程名/窗口标题,不读取内容、不上传明文),记录保留 7 天。是否开启?')) {
    appMonitor.value = false;
    return;
  }
  try {
    await http.put('/api/settings/app-monitor', { value: appMonitor.value });
    if (window.petAPI) window.petAPI.setAppMonitor(appMonitor.value);
    monitorMsg.value = appMonitor.value ? '已开启,宠物会偶尔找你聊聊天' : '已关闭';
  } catch (e) {
    monitorErr.value = e.message;
  }
}

async function addKey() {
  keyMsg.value = '';
  keyErr.value = '';
  if (!form.value.key.trim()) {
    keyErr.value = '请先粘贴 Key';
    return;
  }
  if (form.value.provider === 'custom' && (!form.value.baseUrl.trim() || !form.value.modelName.trim())) {
    keyErr.value = '自定义服务商必须填写接口地址和模型名';
    return;
  }
  try {
    await http.post('/api/keys', {
      provider: form.value.provider,
      key: form.value.key,
      baseUrl: form.value.baseUrl,
      modelName: form.value.modelName,
    });
    form.value.key = '';
    keys.value = await http.get('/api/keys');
    keyMsg.value = '已添加(加密存储)';
  } catch (e) {
    keyErr.value = e.message;
  }
}

async function removeKey(id) {
  keyErr.value = '';
  try {
    await http.delete('/api/keys/' + id);
    keys.value = await http.get('/api/keys');
  } catch (e) {
    keyErr.value = e.message;
  }
}

onMounted(load);
</script>

<style scoped>
.key-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px dashed #f0e0c8;
}
.add {
  margin-top: 10px;
}
select {
  padding: 8px 10px;
  border: 1px solid #e5d5c0;
  border-radius: 8px;
  font-size: 14px;
  background: #fffdf8;
  flex: 1;
}
input {
  flex: 1;
}
/* 复选框:不要 flex:1,保持原始大小和对齐 */
.switch input[type="checkbox"] {
  flex: none !important;
  width: 18px !important;
  height: 18px !important;
  margin: 0 !important;
  accent-color: var(--accent);
  cursor: pointer;
}
.switch {
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
  margin: 6px 0 !important;
  padding-left: 0 !important;
  cursor: pointer;
}
.switch span {
  flex: 1;
  line-height: 1.5;
}
button.small {
  padding: 4px 10px;
  font-size: 12px;
}
button.danger {
  background: #f4b8b0;
  color: #7c2d23;
}
.small {
  font-size: 12px;
}
.voice-field {
  margin-top: 10px;
}
.voice-field label {
  font-size: 12px;
  color: var(--muted);
  font-weight: 600;
  display: block;
  margin-bottom: 4px;
}
.voice-select {
  width: 100% !important;
  padding: 8px 12px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 10px !important;
  font-size: 14px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  box-shadow: none !important;
}
.voice-slider {
  width: 100% !important;
  accent-color: var(--accent);
}
.small {
  font-size: 12px;
}
.sound-field {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.sound-field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--fg);
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin-bottom: 2px;
}
.sound-slider {
  flex: 1;
  min-width: 120px;
  accent-color: var(--accent);
  height: 6px;
}
.vol-text {
  font-size: 12px;
  color: var(--muted);
  min-width: 36px;
  text-align: right;
  font-family: var(--font-pix);
}
.small {
  font-size: 12px;
}

/* 音乐播放器 */
.music-player {
  background: linear-gradient(135deg, #fff4e0, #ffe8c8);
  border-radius: 12px;
  padding: 16px;
  margin: 12px 0;
  border: 2px solid #f0d8a0;
}
.player-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.player-icon {
  font-size: 32px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 50%;
  border: 2px solid #e8a050;
}
.player-track-info {
  flex: 1;
  min-width: 0;
}
.player-track-info .track-name {
  font-weight: 600;
  font-size: 14px;
  color: #8b5a2b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.player-track-info .track-time {
  font-size: 12px;
  color: #a08060;
  font-family: 'Courier New', monospace;
}

/* 进度条 */
.progress-bar {
  position: relative;
  height: 6px;
  background: #e8d4b0;
  border-radius: 3px;
  cursor: pointer;
  margin-bottom: 12px;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #e8954a, #f0b060);
  border-radius: 3px;
  transition: width 0.1s linear;
}
.progress-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 14px;
  height: 14px;
  background: white;
  border: 2px solid #e8954a;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0,0,0,0.15);
}

/* 控制按钮 */
.player-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.ctrl-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: white;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.ctrl-btn:hover {
  background: #fff8f0;
  transform: scale(1.05);
}
.ctrl-btn:active {
  transform: scale(0.95);
}
.ctrl-btn.active {
  background: #ffe8c8;
  color: #c2742e;
}
.ctrl-btn.play-btn {
  width: 44px;
  height: 44px;
  font-size: 18px;
  background: linear-gradient(135deg, #e8954a, #f0a050);
  color: white;
}
.ctrl-btn.play-btn:hover {
  background: linear-gradient(135deg, #f0a050, #f0b060);
}

/* 播放列表 */
.playlist-section {
  margin: 16px 0;
  border-top: 1px dashed #e0d0b8;
  padding-top: 12px;
}
.playlist-header {
  margin-bottom: 8px;
}
.playlist-title {
  font-size: 13px;
  font-weight: 600;
  color: #8b5a2b;
}
.playlist-items {
  max-height: 180px;
  overflow-y: auto;
  border-radius: 8px;
  background: #fffaf0;
  border: 1px solid #f0e0c8;
}
.playlist-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5ead8;
  transition: background 0.15s;
}
.playlist-item:last-child {
  border-bottom: none;
}
.playlist-item:hover {
  background: #fff4e0;
}
.playlist-item.active {
  background: #ffe8c8;
}
.playlist-item .track-index {
  width: 20px;
  font-size: 12px;
  color: #b09070;
  text-align: center;
  font-family: 'Courier New', monospace;
}
.playlist-item .track-name {
  flex: 1;
  font-size: 13px;
  color: #6b5030;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.playlist-item.active .track-name {
  color: #c2742e;
  font-weight: 600;
}
.playing-indicator {
  font-size: 14px;
  color: #e8954a;
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.volume-section {
  border-top: 1px dashed #e0d0b8;
  padding-top: 12px;
  margin-top: 12px;
}

/* ===== API Key 管理样式 ===== */
.key-list {
  margin: 10px 0;
}
.key-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  background: #fffaf0;
  border: 1px solid #f0e0c8;
  border-radius: 8px;
  margin-bottom: 6px;
}
.key-row-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.key-provider-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  white-space: nowrap;
  background: #f0e0c8;
  color: #8b5a2b;
}
.key-provider-badge.p-openai { background: #d4f4dc; color: #1a7a3a; }
.key-provider-badge.p-siliconflow { background: #ffdce0; color: #c23045; }
.key-provider-badge.p-qwen { background: #dce8ff; color: #2a5caa; }
.key-provider-badge.p-zhipu { background: #e8dcff; color: #6a3aaa; }
.key-provider-badge.p-deepseek { background: #dcefff; color: #1a6a9a; }
.key-provider-badge.p-xfyun { background: #fff0d0; color: #b8860b; }
.key-provider-badge.p-custom { background: #eee; color: #666; }
.key-masked {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #6b5030;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.key-voice-tag {
  font-size: 11px;
  background: #e8f5e9;
  color: #2e7d32;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

/* 快速添加区域 */
.quick-add-section {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #e0d0b8;
}
.quick-add-title {
  font-size: 13px;
  font-weight: 600;
  color: #8b5a2b;
  margin-bottom: 8px;
}

/* 服务商选择网格 */
.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}
.provider-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px;
  border: 2px solid #f0e0c8;
  border-radius: 10px;
  background: #fffaf0;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}
.provider-card:hover {
  border-color: #e8a050;
  background: #fff4e0;
}
.provider-card.active {
  border-color: #e8954a;
  background: linear-gradient(135deg, #fff4e0, #ffe8c8);
  box-shadow: 0 2px 8px rgba(232, 149, 74, 0.2);
}
.pc-icon {
  font-size: 20px;
}
.pc-name {
  font-size: 12px;
  font-weight: 600;
  color: #6b5030;
  text-align: center;
}
.pc-badge {
  font-size: 10px;
  background: #e8f5e9;
  color: #2e7d32;
  padding: 1px 6px;
  border-radius: 4px;
}

/* Key 输入行 */
.key-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.key-input {
  flex: 1;
  padding: 8px 12px !important;
  border: 2px solid #f0e0c8 !important;
  border-radius: 10px !important;
  font-size: 14px !important;
  font-family: 'Courier New', monospace;
  background: #fffdf8 !important;
}

/* 高级选项折叠 */
.advanced-toggle {
  margin-top: 6px;
}
.advanced-toggle summary {
  font-size: 12px;
  color: #a08060;
  cursor: pointer;
  padding: 4px 0;
}
.advanced-toggle summary:hover {
  color: #e8954a;
}
.advanced-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}
.advanced-fields input {
  padding: 8px 12px;
  border: 2px solid #f0e0c8;
  border-radius: 8px;
  font-size: 13px;
  font-family: 'Courier New', monospace;
  background: #fffdf8;
  width: 100%;
  box-sizing: border-box;
}

/* 加密说明 */
.encrypt-info {
  margin-top: 10px;
}
.encrypt-info summary {
  font-size: 13px;
  color: #8b5a2b;
  cursor: pointer;
  font-weight: 600;
  padding: 4px 0;
}
.encrypt-info summary:hover {
  color: #e8954a;
}
.encrypt-detail {
  background: #f8f4ed;
  border: 1px solid #f0e0c8;
  border-radius: 8px;
  padding: 10px 14px;
  margin-top: 6px;
}
.encrypt-detail p {
  margin: 4px 0;
  font-size: 12px;
  line-height: 1.6;
  color: #6b5030;
}
.encrypt-detail code {
  font-family: 'Courier New', monospace;
  background: #fff;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
}

/* API 使用提示 */
.api-tip-box {
  margin-top: 10px;
  padding: 8px 12px;
  background: #fff8ed;
  border-left: 3px solid #e8a050;
  border-radius: 4px;
}
.api-tip-box p {
  margin: 3px 0;
}
</style>
