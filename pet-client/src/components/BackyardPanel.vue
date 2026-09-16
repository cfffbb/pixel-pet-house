<template>
  <div class="yard-wrap">
    <!-- 左栏 -->
    <div class="yard-left">
      <!-- 宠物状态 + 怀孕进度 -->
      <div class="card yard-pet-card" v-if="detail">
        <div class="yard-pet-top">
          <div class="yard-pet-avatar" :class="detail.gender">
            <span class="yard-pet-gender">{{ detail.gender === 'MALE' ? '♂' : '♀' }}</span>
          </div>
          <div class="yard-pet-info">
            <h3>🐾 {{ detail.petName }}</h3>
            <span class="muted">{{ detail.typeName }} · {{ detail.personality }} · Lv{{ detail.level }}</span>
            <span class="rarity-tag" :class="rarityClass(detail.rarity)">{{ rarityText(detail.rarity) }}</span>
          </div>
          <div class="yard-pet-stage">
            <span class="stage-emoji">{{ stageEmoji(detail.growthStage) }}</span>
            <span class="stage-text">{{ stageText(detail.growthStage) }}</span>
          </div>
        </div>

        <!-- 怀孕进度条 -->
        <div class="pregnancy-box" v-if="detail.pregnancy">
          <div class="preg-header">
            <span class="preg-emoji">{{ detail.pregnancy.stageEmoji }}</span>
            <span class="preg-title">怀孕{{ detail.pregnancy.stage }}</span>
            <span class="preg-remaining">剩余 {{ detail.pregnancy.remainingDesc }}</span>
          </div>
          <div class="preg-bar-track">
            <div class="preg-bar-fill" :style="{ width: detail.pregnancy.progress + '%' }" :class="pregBarClass(detail.pregnancy.progress)">
              <span class="preg-bar-text" v-if="detail.pregnancy.progress > 15">{{ detail.pregnancy.progress }}%</span>
            </div>
          </div>
          <div class="preg-stages">
            <span :class="{ active: detail.pregnancy.progress < 33 }">🥚初期</span>
            <span :class="{ active: detail.pregnancy.progress >= 33 && detail.pregnancy.progress < 66 }">🐣中期</span>
            <span :class="{ active: detail.pregnancy.progress >= 66 }">🐤后期</span>
            <span :class="{ active: detail.pregnancy.progress >= 100 }">🎉待产</span>
          </div>
        </div>

        <!-- 冷却/就绪状态 -->
        <div class="yard-status-box" v-else>
          <div class="yard-cool" v-if="detail.coolRemainingSeconds > 0">
            <span class="cool-emoji">❄️</span>
            <span>配种冷却中 · 剩余 {{ detail.coolRemainingDesc }}</span>
          </div>
          <div class="yard-ready" v-else-if="detail.canBreed">
            <span class="ready-emoji">✅</span>
            <span>可以配种了!</span>
          </div>
          <div class="yard-not-ready" v-else>
            <span class="not-ready-emoji">⚠️</span>
            <span>暂不满足配种条件</span>
          </div>

          <!-- 就绪检查清单 -->
          <div class="readiness-checks">
            <div v-for="c in detail.readinessChecks" :key="c.label" class="check-item" :class="{ ok: c.ok, fail: !c.ok }">
              <span class="check-icon">{{ c.ok ? '✓' : '✗' }}</span>
              <span>{{ c.label }}</span>
            </div>
          </div>
        </div>

        <!-- 幼崽统计 -->
        <div class="cub-stats">
          <div class="cub-stat-chip"><span class="cs-icon">📦</span><span class="cs-val">{{ detail.totalCubs }}</span><span class="cs-label">总数</span></div>
          <div class="cub-stat-chip"><span class="cs-icon">🏠</span><span class="cs-val">{{ detail.storedCubs }}</span><span class="cs-label">托管</span></div>
          <div class="cub-stat-chip" v-if="detail.frozenCubs > 0"><span class="cs-icon">🧊</span><span class="cs-val">{{ detail.frozenCubs }}</span><span class="cs-label">冻结</span></div>
          <div class="cub-stat-chip"><span class="cs-icon">✅</span><span class="cs-val">{{ detail.claimedCubs }}</span><span class="cs-label">已领</span></div>
        </div>
      </div>
      <div class="card" v-else>
        <p class="muted">加载中...</p>
      </div>

      <!-- 配种记录时间线 -->
      <div class="card">
        <h3>📜 繁育记录</h3>
        <div class="breed-timeline" v-if="records.length">
          <div v-for="r in records" :key="r.recordId" class="timeline-item">
            <div class="timeline-dot" :class="r.status"></div>
            <div class="timeline-content">
              <div class="timeline-header">
                <span class="timeline-pair">{{ r.motherPetName }} ♀ × {{ r.fatherPetName }} ♂</span>
                <span class="timeline-status" :class="r.status">{{ statusText(r.status) }}</span>
              </div>
              <!-- 怀孕进度(记录中) -->
              <div class="timeline-preg" v-if="r.pregnancyProgress">
                <div class="mini-preg-bar">
                  <div class="mini-preg-fill" :style="{ width: r.pregnancyProgress.progress + '%' }"></div>
                </div>
                <span class="mini-preg-text">{{ r.pregnancyProgress.stageEmoji }} {{ r.pregnancyProgress.remainingDesc }}</span>
              </div>
              <!-- 产崽数量 -->
              <div class="timeline-born" v-if="r.status === 'BORN'">
                <span class="born-icon">🐣</span>
                <span>产下 {{ r.cubCount || 0 }} 只幼崽</span>
              </div>
              <span class="timeline-time">{{ fmt(r.createdAt) }}</span>
            </div>
          </div>
        </div>
        <p v-else class="muted">还没有配种记录。</p>
      </div>
    </div>

    <!-- 右栏 -->
    <div class="yard-right">
      <!-- NPC配种市场 -->
      <div class="card">
        <h3>🌿 配种市场</h3>
        <div class="market-filter">
          <select v-model="speciesFilter" @change="loadNpcMarket" class="filter-select">
            <option value="">全部种类</option>
            <option v-for="t in petTypes" :key="t.typeCode" :value="t.typeCode">{{ t.typeName }}</option>
          </select>
          <span class="muted small">{{ npcList.length }} 只候选</span>
        </div>

        <div class="npc-list" v-if="npcList.length">
          <div v-for="p in npcList" :key="p.petId" class="npc-card" :class="{ disabled: !canBreed }" @click="canBreed && doBreed(p)">
            <div class="npc-card-left">
              <div class="npc-avatar" :class="p.gender">
                <img v-if="petImg(p)" :src="petImg(p)" class="npc-pet-img" alt="" />
                <span v-else class="npc-emoji">{{ petEmoji(p.typeCode) }}</span>
                <span class="npc-gender-icon">{{ p.gender === 'MALE' ? '♂' : '♀' }}</span>
              </div>
            </div>
            <div class="npc-card-info">
              <div class="npc-name-row">
                <b>{{ p.petName }}</b>
                <span class="rarity-tag sm" :class="rarityClass(p.rarity)">{{ rarityText(p.rarity) }}</span>
              </div>
              <div class="muted small">{{ p.typeName }} · {{ p.personality }} · Lv{{ p.level }}</div>
              <button class="pix-btn btn-primary btn-sm breed-btn" @click.stop="doBreed(p)" :disabled="!canBreed">
                🧬 配种
              </button>
            </div>
          </div>
        </div>
        <p v-else class="muted">暂无可配种的NPC宠物。</p>
        <p v-if="breedMsg" class="ok">{{ breedMsg }}</p>
        <p v-if="breedErr" class="error">{{ breedErr }}</p>
      </div>

      <!-- 托管所 -->
      <div class="card">
        <h3>🏠 幼崽托管所</h3>
        <p class="muted small">每只每天5币托管费。老宠死亡并完成反思后可领取。</p>
        <div class="hatchling-grid" v-if="hatchlings.length">
          <div v-for="h in hatchlings" :key="h.id" class="hatchling-card" :class="[h.status.toLowerCase(), rarityClass(h.rarity)]">
            <div class="hatch-avatar">
              <img v-if="hatchlingImg(h)" :src="hatchlingImg(h)" class="hatchling-img" alt="" />
              <span v-else class="npc-emoji">{{ petEmoji(h.typeCode) }}</span>
            </div>
            <div class="hatch-info">
              <div class="hatch-name-row">
                <b>{{ h.typeName }}</b>
                <span class="rarity-tag sm" :class="rarityClass(h.rarity)">{{ rarityText(h.rarity) }}</span>
              </div>
              <div class="hatch-meta">
                <span>{{ h.gender === 'MALE' ? '♂公' : '♀母' }}</span>
                <span>{{ h.personality }}</span>
              </div>
              <div class="hatch-status-row">
                <span class="hatch-status" :class="h.status.toLowerCase()">
                  {{ h.status === 'STORED' ? '🏠托管中' : h.status === 'FROZEN' ? '🧊已冻结' : '✅已领取' }}
                </span>
              </div>
              <div class="hatch-actions" v-if="h.status === 'STORED'">
                <button class="pix-btn btn-primary btn-sm" @click="claimHatchling(h)">领取</button>
                <button class="pix-btn btn-ghost btn-sm" @click="giftHatchling(h)">赠送</button>
              </div>
              <div class="hatch-actions" v-else-if="h.status === 'FROZEN'">
                <span class="muted small">余额不足,充值后自动解冻</span>
              </div>
            </div>
          </div>
        </div>
        <p v-else class="muted">托管所空空如也。</p>
        <p v-if="hatchlingMsg" class="ok">{{ hatchlingMsg }}</p>
        <p v-if="hatchlingErr" class="error">{{ hatchlingErr }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import http from '../api/http';
import { rarityText, PET_EMOJI } from '../constants';
import { useSound } from '../soundStore';
import { petImgFrom } from '../petSamples';

const { playSound } = useSound();

const detail = ref(null);
const petTypes = ref([]);
const npcList = ref([]);
const records = ref([]);
const hatchlings = ref([]);
const speciesFilter = ref('');
const breedMsg = ref('');
const breedErr = ref('');
const hatchlingMsg = ref('');
const hatchlingErr = ref('');

const canBreed = computed(() => detail.value?.canBreed || false);

function fmt(t) { return t ? String(t).replace('T', ' ').slice(5, 16) : ''; }
function statusText(s) { return { PENDING: '待同意', PREGNANT: '怀孕中', BORN: '已产崽', REJECTED: '已拒绝' }[s] || s; }
function rarityClass(r) { return { LEGEND: 'legend', RARE: 'rare', NORMAL: 'normal' }[r] || 'normal'; }
function petEmoji(code) { return PET_EMOJI[code] || '🐾'; }
function petImg(p) { return petImgFrom(p); }
function hatchlingImg(h) {
  const code = petTypes.value.find(t => t.typeName === h.typeName)?.typeCode || h.typeCode;
  return code ? petImgFrom({ typeCode: code, gender: h.gender }) : '';
}
function stageText(s) { return { BABY: '幼年', YOUNG: '少年', ADULT: '成年', ELDER: '老年' }[s] || '成年'; }
function stageEmoji(s) { return { BABY: '🐣', YOUNG: '🐥', ADULT: '🐾', ELDER: '👴' }[s] || '🐾'; }
function pregBarClass(p) { return p < 33 ? 'early' : p < 66 ? 'mid' : 'late'; }

async function loadAll() {
  try {
    const [types] = await Promise.all([
      http.get('/api/pet/types'),
    ]);
    petTypes.value = types;
  } catch (e) { /* ignore */ }
  await Promise.all([loadDetail(), loadNpcMarket(), loadRecords(), loadHatchlings()]);
}

async function loadDetail() {
  try {
    detail.value = await http.get('/api/breeding/detail');
  } catch (e) { /* ignore */ }
}

async function loadNpcMarket() {
  try {
    npcList.value = await http.get('/api/breeding/npc-market', { params: { species: speciesFilter.value } });
  } catch (e) { breedErr.value = e.message; }
}

async function loadRecords() {
  try { records.value = await http.get('/api/breeding/my'); } catch (e) { /* ignore */ }
}

async function loadHatchlings() {
  try { hatchlings.value = await http.get('/api/hatchling/my'); } catch (e) { /* ignore */ }
}

async function doBreed(p) {
  playSound('button');
  breedMsg.value = '';
  breedErr.value = '';
  if (!window.confirm(`确认用 ${p.petName}（${p.typeName} ${p.gender === 'MALE' ? '公' : '母'}）配种？消耗10游戏币`)) return;
  try {
    const data = await http.post('/api/breeding/single', { npcPetId: p.petId });
    breedMsg.value = data.message;
    await loadAll();
  } catch (e) { breedErr.value = e.message; }
}

async function claimHatchling(h) {
  playSound('button');
  hatchlingMsg.value = '';
  hatchlingErr.value = '';
  const petName = window.prompt('给幼崽起个名字（必填，最多12字）：');
  if (!petName) return;
  try {
    const data = await http.post('/api/hatchling/claim', { hatchlingId: h.id, petName });
    hatchlingMsg.value = '领养成功：' + data.typeName + ' ' + rarityText(data.rarity);
    await loadAll();
  } catch (e) { hatchlingErr.value = e.message; }
}

async function giftHatchling(h) {
  playSound('button');
  hatchlingMsg.value = '';
  hatchlingErr.value = '';
  const username = window.prompt('要送给谁？（对方用户名）');
  if (!username) return;
  try {
    await http.post('/api/hatchling/gift', { hatchlingId: h.id, targetUsername: username });
    hatchlingMsg.value = '已发送赠送申请，等对方在收件箱同意';
  } catch (e) { hatchlingErr.value = e.message; }
}

onMounted(loadAll);
</script>

<style scoped>
.yard-wrap { display: flex; gap: 16px; }
.yard-left { flex: 1; display: flex; flex-direction: column; gap: 12px; }
.yard-right { width: 380px; display: flex; flex-direction: column; gap: 12px; }
@media (max-width: 860px) { .yard-wrap { flex-direction: column; } .yard-right { width: 100%; } }

/* 宠物状态卡 */
.yard-pet-top { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.yard-pet-avatar { width: 52px; height: 52px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; border: 3px solid; }
.yard-pet-avatar.MALE { border-color: #42a5f5; background: #e3f2fd; }
.yard-pet-avatar.FEMALE { border-color: #ec407a; background: #fce4ec; }
.yard-pet-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.yard-pet-info h3 { margin: 0; }
.yard-pet-stage { text-align: center; }
.stage-emoji { font-size: 28px; display: block; }
.stage-text { font-size: 12px; color: var(--muted); }

.rarity-tag { font-size: 10px; padding: 1px 6px; border-radius: 4px; display: inline-block; }
.rarity-tag.legend { background: #fff3e0; color: #e8a33d; }
.rarity-tag.rare { background: #e8f5e9; color: #2e7d32; }
.rarity-tag.normal { background: #f5f5f5; color: #9e9e9e; }
.rarity-tag.sm { font-size: 9px; }

/* 怀孕进度 */
.pregnancy-box { margin: 12px 0; padding: 12px; background: linear-gradient(135deg, #fff8e1, #fce4ec); border-radius: 12px; border: 2px solid #ffcc80; }
.preg-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.preg-emoji { font-size: 24px; }
.preg-title { font-weight: 700; color: #e65100; }
.preg-remaining { margin-left: auto; font-size: 12px; color: #bf360c; }
.preg-bar-track { height: 20px; background: #fff3e0; border-radius: 10px; overflow: hidden; border: 2px solid #ffcc80; }
.preg-bar-fill { height: 100%; border-radius: 8px; transition: width 0.5s ease; display: flex; align-items: center; justify-content: center; }
.preg-bar-fill.early { background: linear-gradient(90deg, #ffe0b2, #ffcc80); }
.preg-bar-fill.mid { background: linear-gradient(90deg, #ffcc80, #ffab40); }
.preg-bar-fill.late { background: linear-gradient(90deg, #ffab40, #ff6f00); }
.preg-bar-text { font-size: 11px; font-weight: 700; color: #fff; text-shadow: 0 1px 2px rgba(0,0,0,0.3); }
.preg-stages { display: flex; justify-content: space-between; margin-top: 8px; font-size: 11px; color: #8d6e63; }
.preg-stages span { padding: 2px 6px; border-radius: 6px; }
.preg-stages span.active { background: #ff6f00; color: white; font-weight: 600; }

/* 冷却/就绪 */
.yard-status-box { margin: 12px 0; }
.yard-cool, .yard-ready, .yard-not-ready { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 8px; margin-bottom: 8px; }
.yard-cool { background: #e3f2fd; color: #1565c0; }
.yard-ready { background: #e8f5e9; color: #2e7d32; }
.yard-not-ready { background: #fff3e0; color: #e65100; }

.readiness-checks { display: flex; flex-wrap: wrap; gap: 6px; }
.check-item { display: flex; align-items: center; gap: 4px; padding: 3px 8px; border-radius: 8px; font-size: 11px; }
.check-item.ok { background: #e8f5e9; color: #2e7d32; }
.check-item.fail { background: #fce4ec; color: #c62828; }
.check-icon { font-weight: 700; }

/* 幼崽统计 */
.cub-stats { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 12px; padding-top: 12px; border-top: 1px dashed var(--border); }
.cub-stat-chip { display: flex; align-items: center; gap: 3px; padding: 4px 10px; background: var(--surface); border: 1px solid var(--border); border-radius: 12px; }
.cs-icon { font-size: 14px; }
.cs-val { font-weight: 700; font-size: 14px; }
.cs-label { font-size: 10px; color: var(--muted); }

/* 配种记录时间线 */
.breed-timeline { position: relative; padding-left: 20px; }
.breed-timeline::before { content: ''; position: absolute; left: 6px; top: 0; bottom: 0; width: 2px; background: var(--border); }
.timeline-item { position: relative; margin-bottom: 16px; }
.timeline-dot { position: absolute; left: -20px; top: 4px; width: 12px; height: 12px; border-radius: 50%; border: 2px solid var(--border); background: var(--surface); }
.timeline-dot.PREGNANT { background: #ff6f00; border-color: #ff6f00; }
.timeline-dot.BORN { background: #2e7d32; border-color: #2e7d32; }
.timeline-dot.PENDING { background: #ff9800; border-color: #ff9800; }
.timeline-dot.REJECTED { background: #c62828; border-color: #c62828; }
.timeline-content { padding: 8px 12px; background: var(--surface); border-radius: 8px; border: 1px solid var(--border); }
.timeline-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.timeline-pair { font-size: 13px; font-weight: 600; }
.timeline-status { font-size: 11px; padding: 2px 8px; border-radius: 6px; }
.timeline-status.PREGNANT { background: #ffe0b2; color: #e65100; }
.timeline-status.BORN { background: #e8f5e9; color: #2e7d32; }
.timeline-status.PENDING { background: #fff3e0; color: #ef6c00; }
.timeline-status.REJECTED { background: #fce4ec; color: #c62828; }
.timeline-preg { display: flex; align-items: center; gap: 8px; margin: 6px 0; }
.mini-preg-bar { flex: 1; height: 8px; background: #fff3e0; border-radius: 4px; overflow: hidden; }
.mini-preg-fill { height: 100%; background: linear-gradient(90deg, #ffcc80, #ff6f00); border-radius: 4px; transition: width 0.3s; }
.mini-preg-text { font-size: 11px; color: #e65100; white-space: nowrap; }
.timeline-born { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #2e7d32; margin: 4px 0; }
.timeline-time { font-size: 10px; color: var(--muted); }

/* NPC市场 */
.market-filter { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.filter-select { padding: 6px 10px; border: 2px solid var(--border); border-radius: 8px; font-size: 13px; background: var(--surface); color: var(--fg); }
.npc-list { display: flex; flex-direction: column; gap: 8px; max-height: 500px; overflow-y: auto; }
.npc-card { display: flex; gap: 10px; padding: 10px; border: 2px solid var(--border); border-radius: 12px; background: var(--surface); cursor: pointer; transition: all 0.2s; }
.npc-card:hover { border-color: var(--accent); transform: translateX(2px); }
.npc-card.disabled { opacity: 0.5; pointer-events: none; }
.npc-card-left .npc-avatar { position: relative; width: 56px; height: 56px; }
.npc-pet-img { width: 56px; height: 56px; object-fit: contain; }
.npc-emoji { font-size: 36px; }
.npc-gender-icon { position: absolute; bottom: 0; right: 0; font-size: 14px; background: white; border-radius: 50%; padding: 0 2px; }
.npc-card-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.npc-name-row { display: flex; align-items: center; gap: 6px; }
.breed-btn { margin-top: 4px; align-self: flex-start; }

/* 托管所 */
.hatchling-grid { display: flex; flex-direction: column; gap: 8px; max-height: 400px; overflow-y: auto; }
.hatchling-card { display: flex; gap: 10px; padding: 10px; border: 2px solid var(--border); border-radius: 12px; background: var(--surface); }
.hatchling-card.legend { border-color: #e8a33d; background: linear-gradient(135deg, var(--surface), #fff8e1); }
.hatchling-card.rare { border-color: #7fbf7f; }
.hatchling-card.frozen { opacity: 0.6; }
.hatch-avatar { width: 48px; height: 48px; flex-shrink: 0; }
.hatchling-img { width: 48px; height: 48px; object-fit: contain; }
.hatch-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.hatch-name-row { display: flex; align-items: center; gap: 6px; }
.hatch-meta { font-size: 11px; color: var(--muted); display: flex; gap: 8px; }
.hatch-status-row { margin: 2px 0; }
.hatch-status { font-size: 11px; padding: 1px 8px; border-radius: 6px; }
.hatch-status.stored { background: #e8f5e9; color: #2e7d32; }
.hatch-status.frozen { background: #e3f2fd; color: #1565c0; }
.hatch-status.claimed { background: #f5f5f5; color: #9e9e9e; }
.hatch-actions { display: flex; gap: 4px; }

.small { font-size: 12px; }
</style>
