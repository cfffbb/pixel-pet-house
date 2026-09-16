<template>
  <div>
    <!-- 拍照卡片 -->
    <div class="card cam-card">
      <h3>📷 与宠物合影</h3>
      <p class="muted">左半屏是你的摄像头画面，右半屏是宠物参考框。把脸和宠物对齐后拍照，存入宠物相册。</p>

      <div class="cam-stage" v-if="running">
        <div class="cam-left">
          <video ref="videoEl" autoplay muted playsinline class="cam-video"></video>
          <div class="cam-overlay">
            <span class="cam-timestamp">{{ currentTime }}</span>
          </div>
        </div>
        <div class="cam-right">
          <div class="ref-frame">
            <img v-if="petImg" :src="petImg" class="ref-pet-img" alt="宠物" />
            <span v-else class="ref-pet-emoji">{{ petEmoji() }}</span>
            <div class="ref-corner tl"></div>
            <div class="ref-corner tr"></div>
            <div class="ref-corner bl"></div>
            <div class="ref-corner br"></div>
          </div>
          <p class="ref-label">{{ petName }} · {{ stageText() }}</p>
          <p class="ref-hint">把脸放进框里对齐</p>
        </div>
      </div>

      <div class="cam-placeholder" v-else>
        <span class="cam-placeholder-icon">📸</span>
        <p class="muted">点击下方按钮开启摄像头</p>
      </div>

      <div class="cam-actions">
        <button v-if="!running" class="pix-btn btn-primary" @click="start">📷 开启摄像头</button>
        <button v-if="running" class="pix-btn btn-ghost" @click="stop">关闭摄像头</button>
        <button v-if="running" class="pix-btn btn-primary cam-shoot-btn" @click="shoot">📸 拍照</button>
      </div>
      <p v-if="statusText" :class="ok ? 'ok' : 'muted'">{{ statusText }}</p>
      <p v-if="err" class="error">{{ err }}</p>
    </div>

    <!-- 相册卡片 -->
    <div class="card">
      <h3>🖼️ 宠物相册</h3>
      <div class="album-search-row">
        <input v-model="searchKey" placeholder="按宠物名搜索相册" @input="loadAlbums" class="album-search" />
      </div>
      <div v-for="album in albums" :key="album.petId" class="album">
        <div class="album-head">
          <PetFigure :type-code="album.typeCode" :gender="album.gender" :size="44" />
          <b>{{ album.petName }}</b>
          <span class="muted small">{{ album.photos.length }} 张</span>
        </div>
        <div class="photos">
          <div v-for="p in album.photos" :key="p.id" class="photo-item">
            <img :src="fileUrl(p.path)" class="photo" alt="" />
            <button class="del" title="删除" @click="delPhoto(p)">✕</button>
          </div>
        </div>
      </div>
      <p v-if="!albums.length" class="muted">还没有照片，开摄像头拍一张吧。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue';
import http, { fileUrl } from '../api/http';
import PetFigure from './PetFigure.vue';
import { PET_EMOJI } from '../constants';
import { petSampleUrl } from '../petSamples';
import { useSound } from '../soundStore';

const { playSound, playCamera } = useSound();

const running = ref(false);
const statusText = ref('');
const ok = ref(false);
const err = ref('');
const videoEl = ref(null);
const pet = ref(null);
const albums = ref([]);
const searchKey = ref('');

let stream = null;

const petImg = computed(() => {
  if (!pet.value || !pet.value.hasPet) return '';
  return petSampleUrl(pet.value.typeCode, pet.value.gender);
});
const petName = computed(() => {
  if (!pet.value || !pet.value.hasPet) return '宠物';
  return pet.value.petName || '宠物';
});
const currentTime = ref('');

function petEmoji() {
  if (!pet.value || !pet.value.hasPet) return '🐾';
  return (PET_EMOJI[pet.value.typeCode] || '🐾') + (pet.value.gender === 'MALE' ? '♂' : '♀');
}
function stageText() {
  if (!pet.value || !pet.value.hasPet) return '';
  return { BABY: '幼年', YOUNG: '少年', ADULT: '成年', ELDER: '老年' }[pet.value.stage] || '';
}

async function start() {
  err.value = '';
  playSound('button');
  if (!localStorage.getItem('cam_consented')) {
    if (!window.confirm('开启摄像头:画面仅在本地内存处理,照片只存进你自己的相册,绝不上传别处。是否开启?')) return;
    localStorage.setItem('cam_consented', '1');
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { width: 960, height: 540 }, audio: false });
    videoEl.value.srcObject = stream;
    await videoEl.value.play().catch(() => {});
    running.value = true;
    statusText.value = '摄像头已开启';
    pet.value = await http.get('/api/pet/my').catch(() => null);
    currentTime.value = new Date().toLocaleString();
    setInterval(() => { currentTime.value = new Date().toLocaleString(); }, 1000);
  } catch (e) {
    err.value = '无法访问摄像头:' + e.message;
  }
}

function shoot() {
  playCamera();
  const video = videoEl.value;
  if (!video) return;
  const canvas = document.createElement('canvas');
  canvas.width = 960;
  canvas.height = 540;
  const ctx = canvas.getContext('2d');

  // 左半屏:摄像头画面
  ctx.drawImage(video, 0, 0, 480, 540);

  // 右半屏:暖色背景
  const grad = ctx.createLinearGradient(480, 0, 960, 540);
  grad.addColorStop(0, '#fdf6ec');
  grad.addColorStop(1, '#f5e6cc');
  ctx.fillStyle = grad;
  ctx.fillRect(480, 0, 480, 540);

  // 参考框边角装饰
  ctx.strokeStyle = '#ff9f45';
  ctx.lineWidth = 3;
  const fx = 520, fy = 50, fw = 400, fh = 360;
  const cl = 30;
  ctx.beginPath();
  ctx.moveTo(fx, fy + cl); ctx.lineTo(fx, fy); ctx.lineTo(fx + cl, fy);
  ctx.moveTo(fx + fw - cl, fy); ctx.lineTo(fx + fw, fy); ctx.lineTo(fx + fw, fy + cl);
  ctx.moveTo(fx, fy + fh - cl); ctx.lineTo(fx, fy + fh); ctx.lineTo(fx + cl, fy + fh);
  ctx.moveTo(fx + fw - cl, fy + fh); ctx.lineTo(fx + fw, fy + fh); ctx.lineTo(fx + fw, fy + fh - cl);
  ctx.stroke();

  // 尝试加载宠物图片
  const petImgEl = new Image();
  petImgEl.crossOrigin = 'anonymous';
  petImgEl.onload = () => {
    ctx.drawImage(petImgEl, fx + 60, fy + 40, fw - 120, fh - 100);
    finalizePhoto(ctx, canvas, fx, fy, fw, fh);
  };
  petImgEl.onerror = () => {
    ctx.font = '100px serif';
    ctx.textAlign = 'center';
    ctx.fillStyle = '#e8a93c';
    ctx.fillText(petEmoji(), fx + fw / 2, fy + fh / 2 + 20);
    finalizePhoto(ctx, canvas, fx, fy, fw, fh);
  };
  petImgEl.src = petImg.value || '';
}

function finalizePhoto(ctx, canvas, fx, fy, fw, fh) {
  // 宠物名 + 阶段
  ctx.font = 'bold 22px "Microsoft YaHei"';
  ctx.fillStyle = '#5a4a35';
  ctx.textAlign = 'center';
  ctx.fillText(petName.value + ' · ' + stageText(), 720, 440);
  ctx.font = '16px "Microsoft YaHei"';
  ctx.fillStyle = '#9a8a75';
  ctx.fillText('把脸放进框里', 720, 470);
  // 时间戳
  ctx.textAlign = 'left';
  ctx.font = '13px "Microsoft YaHei"';
  ctx.fillStyle = 'rgba(255,255,255,0.85)';
  ctx.fillText(new Date().toLocaleString(), 12, 24);

  canvas.toBlob(async (blob) => {
    if (!blob) return;
    const fd = new FormData();
    fd.append('file', blob, 'photo.png');
    try {
      await http.post('/api/photo/upload', fd);
      ok.value = true;
      statusText.value = '照片已存入相册';
      await loadAlbums();
    } catch (e) {
      err.value = e.message;
    }
  }, 'image/png');
}

async function loadAlbums() {
  try {
    albums.value = await http.get('/api/photo/albums', { params: { keyword: searchKey.value } });
  } catch (e) {
    err.value = e.message;
  }
}

async function delPhoto(p) {
  if (!window.confirm('删除这张照片?')) return;
  try {
    await http.delete('/api/photo/' + p.id);
    await loadAlbums();
  } catch (e) {
    err.value = e.message;
  }
}

function stop() {
  if (stream) {
    stream.getTracks().forEach((t) => t.stop());
    stream = null;
  }
  if (videoEl.value) videoEl.value.srcObject = null;
  running.value = false;
}

onMounted(loadAlbums);
onBeforeUnmount(stop);
</script>

<style scoped>
.cam-card {
  overflow: hidden;
}
.cam-stage {
  display: flex;
  gap: 0;
  margin: 12px 0;
  border-radius: 14px;
  overflow: hidden;
  border: 2.5px solid #e5d5c0;
  box-shadow: 0 4px 16px -6px rgba(60, 40, 20, 0.2);
  aspect-ratio: 16/9;
  max-height: 360px;
}
.cam-left {
  position: relative;
  flex: 1;
  overflow: hidden;
  background: #1a1a1a;
}
.cam-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cam-overlay {
  position: absolute;
  top: 0; left: 0; right: 0;
  padding: 6px 12px;
  background: linear-gradient(180deg, rgba(0,0,0,0.5), transparent);
  pointer-events: none;
}
.cam-timestamp {
  font-size: 12px;
  color: rgba(255,255,255,0.85);
  font-family: monospace;
}
.cam-right {
  width: 42%;
  background: linear-gradient(135deg, #fdf6ec, #f5e6cc);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  position: relative;
}
.ref-frame {
  position: relative;
  width: 100%;
  max-width: 180px;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ref-pet-img {
  width: 85%;
  height: 85%;
  object-fit: contain;
  filter: drop-shadow(0 4px 8px rgba(60,40,20,0.15));
}
.ref-pet-emoji {
  font-size: 72px;
}
.ref-corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border: 3px solid #ff9f45;
}
.ref-corner.tl { top: 0; left: 0; border-right: none; border-bottom: none; border-radius: 6px 0 0 0; }
.ref-corner.tr { top: 0; right: 0; border-left: none; border-bottom: none; border-radius: 0 6px 0 0; }
.ref-corner.bl { bottom: 0; left: 0; border-right: none; border-top: none; border-radius: 0 0 0 6px; }
.ref-corner.br { bottom: 0; right: 0; border-left: none; border-top: none; border-radius: 0 0 6px 0; }
.ref-label {
  margin-top: 12px;
  font-weight: bold;
  font-size: 14px;
  color: #5a4a35;
}
.ref-hint {
  font-size: 12px;
  color: #9a8a75;
}
.cam-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 8px;
}
.cam-placeholder-icon {
  font-size: 48px;
  opacity: 0.5;
}
.cam-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.cam-shoot-btn {
  animation: camPulse 2s ease-in-out infinite;
}
@keyframes camPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 159, 69, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(255, 159, 69, 0); }
}
.album-search-row {
  margin-bottom: 12px;
}
.album-search {
  width: 100%;
  max-width: 300px;
  padding: 8px 14px;
  border: 1.5px solid #e5d5c0;
  border-radius: 10px;
  font-size: 13px;
  background: #fffdf8;
}
.album {
  margin-top: 12px;
  padding: 12px;
  background: #fffaf2;
  border: 1px solid #f0e0c8;
  border-radius: 12px;
}
.album-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.photos {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
}
.photo-item {
  position: relative;
}
.photo {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 10px;
  border: 1.5px solid #f0e0c8;
}
.del {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 11px;
  line-height: 1;
  padding: 0;
  cursor: pointer;
  transition: background 0.2s;
}
.del:hover { background: rgba(209, 69, 59, 0.85); }
.small {
  font-size: 12px;
}
</style>
