/**
 * 全局音效管理器(Iter-07)
 * 统一管理所有音效播放 + 音量控制 + 背景音乐播放列表 + localStorage 持久化
 */
import { ref, computed } from 'vue';

const SOUND_BASE = './sounds';

// 音效文件映射
const SOUND_FILES = {
  button: `${SOUND_BASE}/除了规定外的声音.mp3`,
  play: `${SOUND_BASE}/播放按键的声音.mp3`,
  camera: `${SOUND_BASE}/点击拍照键的声音.mp3`,
  volumeDrag: `${SOUND_BASE}/拖动音量条的声音.mp3`,
  friendMessage: `${SOUND_BASE}/好友发送消息的声音.mp3`,
  pomodoroEnd: `${SOUND_BASE}/番茄钟记录时间结束音效.mp3`,
  pomodoroNotify: `${SOUND_BASE}/右下角番茄钟已经完成的跳出的提醒.mp3`,
};

// 音量(0-1),持久化到 localStorage
const bgVolume = ref(parseFloat(localStorage.getItem('pet_bg_volume') || '0.3'));
const voiceVolume = ref(parseFloat(localStorage.getItem('pet_voice_volume') || '0.7'));
const uiVolume = ref(parseFloat(localStorage.getItem('pet_ui_volume') || '0.5'));

// 背景音乐开关(持久化)
const bgMusicEnabled = ref(localStorage.getItem('pet_bg_music') === '1');

// 音频对象缓存
const audioCache = {};
let bgMusic = null;

// 播放列表
const playlist = ref([]);
const currentTrackIndex = ref(-1);
const currentTime = ref(0);
const duration = ref(0);
const isPlaying = ref(false);
const isShuffle = ref(localStorage.getItem('pet_bg_shuffle') === '1');
const repeatMode = ref(localStorage.getItem('pet_bg_repeat') || 'all'); // 'none', 'one', 'all'

// 从 localStorage 加载播放列表
function loadPlaylist() {
  try {
    const saved = localStorage.getItem('pet_playlist');
    if (saved) {
      playlist.value = JSON.parse(saved);
    }
  } catch (e) {
    playlist.value = [];
  }
  // 默认加一个 bg-music.mp3 占位（如果文件存在会正常播放）
  if (playlist.value.length === 0) {
    playlist.value = [{
      id: 'default',
      name: '背景音乐',
      url: `${SOUND_BASE}/bg-music.mp3`,
      source: 'default'
    }];
  }
}

// 保存播放列表到 localStorage
function savePlaylist() {
  try {
    // 只保存元数据，不保存 base64/admin 导入的内容（太大，localStorage 存不下）
    const toSave = playlist.value.filter(t => t.source !== 'base64' && t.source !== 'admin');
    localStorage.setItem('pet_playlist', JSON.stringify(toSave));
  } catch (e) {}
}

// 初始化
loadPlaylist();

function getAudio(name) {
  if (!audioCache[name]) {
    const file = SOUND_FILES[name];
    if (!file) return null;
    audioCache[name] = new Audio(file);
    audioCache[name].preload = 'auto';
  }
  return audioCache[name];
}

/**
 * 播放 UI 音效(按钮点击等)
 */
function playSound(name) {
  const audio = getAudio(name);
  if (!audio) return;
  audio.volume = uiVolume.value;
  audio.currentTime = 0;
  audio.play().catch(() => {});
}

/**
 * 播放好友消息提醒
 */
function playFriendMessage() {
  const audio = getAudio('friendMessage');
  if (!audio) return;
  audio.volume = uiVolume.value;
  audio.currentTime = 0;
  audio.play().catch(() => {});
}

/**
 * 播放番茄钟结束音效
 */
function playPomodoroEnd() {
  const audio = getAudio('pomodoroEnd');
  if (!audio) return;
  audio.volume = uiVolume.value;
  audio.currentTime = 0;
  audio.play().catch(() => {});
  // 延迟播放通知音
  setTimeout(() => {
    const notify = getAudio('pomodoroNotify');
    if (notify) {
      notify.volume = uiVolume.value;
      notify.currentTime = 0;
      notify.play().catch(() => {});
    }
  }, 800);
}

/**
 * 播放音量条拖动音效
 */
function playVolumeDrag() {
  const audio = getAudio('volumeDrag');
  if (!audio) return;
  audio.volume = uiVolume.value;
  audio.currentTime = 0;
  audio.play().catch(() => {});
}

/**
 * 播放拍照音效
 */
function playCamera() {
  const audio = getAudio('camera');
  if (!audio) return;
  audio.volume = uiVolume.value;
  audio.currentTime = 0;
  audio.play().catch(() => {});
}

/**
 * 播放开关灯音效(根据主题动态传入URL)
 */
function playLampSound(url) {
  if (!url) return;
  const audio = new Audio(url);
  audio.volume = uiVolume.value;
  audio.play().catch(() => {});
}

/**
 * 设置背景音乐音量
 */
function setBgVolume(v) {
  bgVolume.value = v;
  localStorage.setItem('pet_bg_volume', String(v));
  if (bgMusic) bgMusic.volume = v;
}

/**
 * 设置语音音量(TTS 播放时使用)
 */
function setVoiceVolume(v) {
  voiceVolume.value = v;
  localStorage.setItem('pet_voice_volume', String(v));
}

/**
 * 设置 UI 音效音量
 */
function setUiVolume(v) {
  uiVolume.value = v;
  localStorage.setItem('pet_ui_volume', String(v));
}

/**
 * 获取语音音量(TTS 播放器调用)
 */
function getVoiceVolume() {
  return voiceVolume.value;
}

/**
 * 当前播放的曲目
 */
const currentTrack = computed(() => {
  if (currentTrackIndex.value >= 0 && currentTrackIndex.value < playlist.value.length) {
    return playlist.value[currentTrackIndex.value];
  }
  return null;
});

/**
 * 初始化 bgMusic 音频对象
 */
function initBgMusic(url) {
  if (bgMusic) {
    bgMusic.pause();
    bgMusic.src = '';
  }
  bgMusic = new Audio(url);
  bgMusic.volume = bgVolume.value;
  bgMusic.preload = 'auto';

  bgMusic.addEventListener('timeupdate', () => {
    currentTime.value = bgMusic.currentTime;
  });

  bgMusic.addEventListener('loadedmetadata', () => {
    let d = bgMusic.duration;
    if (d === Infinity || isNaN(d)) {
      duration.value = 0;
    } else {
      duration.value = d;
    }
  });

  bgMusic.addEventListener('durationchange', () => {
    let d = bgMusic.duration;
    if (d !== Infinity && !isNaN(d)) {
      duration.value = d;
    }
  });

  bgMusic.addEventListener('ended', () => {
    onTrackEnded();
  });

  bgMusic.addEventListener('play', () => {
    isPlaying.value = true;
  });

  bgMusic.addEventListener('pause', () => {
    isPlaying.value = false;
  });
}

/**
 * 曲目播放结束处理
 */
function onTrackEnded() {
  if (repeatMode.value === 'one') {
    // 单曲循环
    bgMusic.currentTime = 0;
    bgMusic.play().catch(() => {});
  } else if (repeatMode.value === 'none' && currentTrackIndex.value >= playlist.value.length - 1 && !isShuffle.value) {
    // 顺序播放到最后一首，停止
    isPlaying.value = false;
    bgMusicEnabled.value = false;
    localStorage.setItem('pet_bg_music', '0');
  } else {
    // 下一首
    playNext();
  }
}

/**
 * 播放指定索引的曲目
 */
function playTrack(index) {
  if (index < 0 || index >= playlist.value.length) return;
  
  const track = playlist.value[index];
  currentTrackIndex.value = index;
  currentTime.value = 0;
  duration.value = 0;
  
  initBgMusic(track.url);
  bgMusic.play().then(() => {
    bgMusicEnabled.value = true;
    localStorage.setItem('pet_bg_music', '1');
  }).catch(() => {
    // 播放失败（比如文件不存在），尝试下一首
    if (playlist.value.length > 1) {
      setTimeout(() => playNext(), 500);
    } else {
      bgMusicEnabled.value = false;
      localStorage.setItem('pet_bg_music', '0');
    }
  });
}

/**
 * 播放背景音乐(循环)
 */
function playBgMusic() {
  if (playlist.value.length === 0) return;
  
  if (currentTrackIndex.value < 0) {
    playTrack(0);
  } else if (bgMusic) {
    bgMusic.play().catch(() => {});
    bgMusicEnabled.value = true;
    localStorage.setItem('pet_bg_music', '1');
  }
}

/**
 * 停止背景音乐
 */
function stopBgMusic() {
  if (bgMusic) {
    bgMusic.pause();
  }
  isPlaying.value = false;
  bgMusicEnabled.value = false;
  localStorage.setItem('pet_bg_music', '0');
}

/**
 * 暂停背景音乐
 */
function pauseBgMusic() {
  if (bgMusic) {
    bgMusic.pause();
  }
}

/**
 * 切换背景音乐开关
 */
function toggleBgMusic() {
  if (bgMusicEnabled.value) {
    stopBgMusic();
  } else {
    playBgMusic();
  }
}

/**
 * 播放下一首
 */
function playNext() {
  if (playlist.value.length === 0) return;
  
  let nextIndex;
  if (isShuffle.value) {
    // 随机播放
    nextIndex = Math.floor(Math.random() * playlist.value.length);
    if (playlist.value.length > 1 && nextIndex === currentTrackIndex.value) {
      nextIndex = (nextIndex + 1) % playlist.value.length;
    }
  } else {
    // 顺序播放
    nextIndex = (currentTrackIndex.value + 1) % playlist.value.length;
  }
  playTrack(nextIndex);
}

/**
 * 播放上一首
 */
function playPrev() {
  if (playlist.value.length === 0) return;
  
  let prevIndex;
  if (isShuffle.value) {
    prevIndex = Math.floor(Math.random() * playlist.value.length);
  } else {
    prevIndex = currentTrackIndex.value <= 0 ? playlist.value.length - 1 : currentTrackIndex.value - 1;
  }
  playTrack(prevIndex);
}

/**
 * 跳转到指定时间
 */
function seekBgMusic(time) {
  if (bgMusic && !isNaN(time)) {
    bgMusic.currentTime = time;
    currentTime.value = time;
  }
}

/**
 * 切换随机播放
 */
function toggleShuffle() {
  isShuffle.value = !isShuffle.value;
  localStorage.setItem('pet_bg_shuffle', isShuffle.value ? '1' : '0');
}

/**
 * 设置循环模式
 */
function setRepeatMode(mode) {
  repeatMode.value = mode;
  localStorage.setItem('pet_bg_repeat', mode);
}

/**
 * 添加音乐到播放列表
 */
function addTrack(track) {
  const newTrack = {
    id: track.id || 'track_' + Date.now(),
    name: track.name || '未知曲目',
    url: track.url,
    source: track.source || 'custom'
  };
  playlist.value.push(newTrack);
  savePlaylist();
  return newTrack;
}

/**
 * 从播放列表移除音乐
 */
function removeTrack(index) {
  if (index < 0 || index >= playlist.value.length) return;
  
  const wasCurrent = index === currentTrackIndex.value;
  playlist.value.splice(index, 1);
  
  if (wasCurrent) {
    // 移除的是当前播放的曲目
    if (playlist.value.length > 0) {
      const newIndex = Math.min(index, playlist.value.length - 1);
      playTrack(newIndex);
    } else {
      stopBgMusic();
      currentTrackIndex.value = -1;
    }
  } else if (index < currentTrackIndex.value) {
    // 移除的是前面的曲目，调整索引
    currentTrackIndex.value--;
  }
  
  savePlaylist();
}

/**
 * 清空播放列表
 */
function clearPlaylist() {
  stopBgMusic();
  playlist.value = [];
  currentTrackIndex.value = -1;
  savePlaylist();
}

/**
 * 尝试自动恢复背景音乐(页面加载后,如果之前是开着的)
 */
function restoreBgMusic() {
  if (bgMusicEnabled.value && playlist.value.length > 0) {
    // 延迟播放，等待用户交互解锁音频
    setTimeout(() => {
      if (currentTrackIndex.value < 0) {
        playTrack(0);
      } else {
        playBgMusic();
      }
    }, 1000);
  }
}

/**
 * 格式化时间
 */
function formatTime(seconds) {
  if (!seconds || isNaN(seconds)) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

export function useSound() {
  return {
    bgVolume,
    voiceVolume,
    uiVolume,
    bgMusicEnabled,
    playlist,
    currentTrackIndex,
    currentTrack,
    currentTime,
    duration,
    isPlaying,
    isShuffle,
    repeatMode,
    playSound,
    playFriendMessage,
    playPomodoroEnd,
    playVolumeDrag,
    playCamera,
    playLampSound,
    setBgVolume,
    setVoiceVolume,
    setUiVolume,
    getVoiceVolume,
    playBgMusic,
    stopBgMusic,
    pauseBgMusic,
    toggleBgMusic,
    playTrack,
    playNext,
    playPrev,
    seekBgMusic,
    toggleShuffle,
    setRepeatMode,
    addTrack,
    removeTrack,
    clearPlaylist,
    restoreBgMusic,
    formatTime,
  };
}
