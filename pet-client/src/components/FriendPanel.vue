<template>
  <div>
    <div class="card">
      <h3>👥 我的好友</h3>
      <div v-for="f in friends" :key="f.friendId" class="friend-item">
        <span class="avatar">
          <PetFigure v-if="f.petTypeCode" :type-code="f.petTypeCode" :gender="f.petGender" :size="44" />
          <span v-else class="egg">🐾</span>
        </span>
        <span class="who">
          {{ f.nickname || f.username }}
          <span class="muted">@{{ f.username }} · {{ fmt(f.since) }}</span>
          <span v-if="f.muted" class="mute-tag">🔇 免打扰</span>
        </span>
        <div class="friend-actions">
          <input v-model="msgText[f.friendId]" placeholder="发条消息…" @keyup.enter="chat(f)" class="friend-input" />
          <button class="pix-btn btn-soft btn-sm" @click="chat(f)">发消息</button>
          <button class="pix-btn btn-ghost btn-sm" @click="toggleMute(f)">{{ f.muted ? '🔔 取消免打扰' : '🔇 免打扰' }}</button>
          <button class="pix-btn btn-ghost btn-sm" @click="block(f)">🚫 拉黑</button>
          <button class="pix-btn btn-ghost btn-sm" @click="report(f)">🚩 举报</button>
          <button class="pix-btn btn-ghost btn-sm danger" @click="remove(f)">删好友</button>
        </div>
      </div>
      <p v-if="!friends.length" class="muted">还没有好友,去添加一个吧。</p>
      <p v-if="msg" class="ok">{{ msg }}</p>
      <p v-if="err" class="error">{{ err }}</p>
    </div>

    <div class="card">
      <h3>➕ 添加好友</h3>
      <div class="search-box">
        <input v-model="searchKey" placeholder="搜索用户(用户名/昵称)后点击添加" @input="onSearch" class="friend-input" />
        <div v-if="results.length" class="results">
          <div v-for="r in results" :key="r.userId" class="result" @click="add(r)">
            {{ r.nickname || r.username }} <span class="muted">@{{ r.username }}</span> → 发送申请
          </div>
        </div>
      </div>
      <p class="muted small">申请会发到对方收件箱,对方同意后成为好友。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import http from '../api/http';
import PetFigure from './PetFigure.vue';
import { useSound } from '../soundStore';

const { playSound, playFriendMessage } = useSound();

const friends = ref([]);
const searchKey = ref('');
const results = ref([]);
const msgText = ref({});
const msg = ref('');
const err = ref('');
let searchTimer = null;

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '';
}

async function load() {
  try {
    friends.value = await http.get('/api/friends');
  } catch (e) {
    err.value = e.message;
  }
}

async function onSearch() {
  clearTimeout(searchTimer);
  const kw = searchKey.value.trim();
  if (!kw) { results.value = []; return; }
  searchTimer = setTimeout(async () => {
    try {
      results.value = await http.get('/api/users/search', { params: { keyword: kw } });
    } catch (e) { results.value = []; }
  }, 300);
}

async function add(r) {
  msg.value = ''; err.value = '';
  playSound('button');
  try {
    await http.post('/api/friends/request', { targetUsername: r.username });
    msg.value = '已向 ' + (r.nickname || r.username) + ' 发送好友申请';
    results.value = [];
    searchKey.value = '';
  } catch (e) { err.value = e.message; }
}

async function chat(f) {
  err.value = '';
  const content = (msgText.value[f.friendId] || '').trim();
  if (!content) return;
  playSound('button');
  try {
    await http.post('/api/inbox/message', { receiverId: f.friendId, content });
    msgText.value[f.friendId] = '';
    msg.value = '消息已发送';
    playFriendMessage();
  } catch (e) { err.value = e.message; }
}

async function toggleMute(f) {
  playSound('button');
  try {
    await http.put('/api/friends/' + f.friendId + '/mute', { muted: f.muted ? 0 : 1 });
    await load();
  } catch (e) { err.value = e.message; }
}

async function block(f) {
  if (!window.confirm('拉黑 ' + (f.nickname || f.username) + '?拉黑后对方无法再给你发消息')) return;
  playSound('button');
  try {
    await http.post('/api/blacklist/' + f.friendId);
    msg.value = '已拉黑 ' + (f.nickname || f.username);
  } catch (e) { err.value = e.message; }
}

async function report(f) {
  const reason = window.prompt('举报 ' + (f.nickname || f.username) + ' 的原因(将被管理员审核):');
  if (!reason) return;
  playSound('button');
  try {
    await http.post('/api/reports', { targetUserId: f.friendId, content: reason });
    msg.value = '已举报,管理员审核后会处理';
  } catch (e) { err.value = e.message; }
}

async function remove(f) {
  if (!window.confirm('删除好友 ' + (f.nickname || f.username) + '?')) return;
  playSound('button');
  try {
    await http.delete('/api/friends/' + f.friendId);
    await load();
  } catch (e) { err.value = e.message; }
}

onMounted(load);
</script>

<style scoped>
.friend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
  flex-wrap: wrap;
}
.who { flex: 1; min-width: 120px; }
.avatar { width: 44px; height: 44px; flex-shrink: 0; }
.egg { font-size: 34px; }
.mute-tag {
  display: inline-block;
  padding: 1px 6px;
  background: color-mix(in oklch, var(--muted) 15%, var(--surface));
  border-radius: var(--radius-xl);
  font-size: 11px;
  color: var(--muted);
  margin-left: 4px;
}
.friend-actions {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
}
.friend-input {
  width: 140px !important;
  padding: 6px 10px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 10px !important;
  font-size: 13px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  box-shadow: none !important;
}
.friend-input:focus { border-color: var(--accent) !important; }
.search-box { position: relative; }
.results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--surface);
  border: 2px solid var(--border);
  border-radius: var(--radius);
  z-index: 20;
  max-height: 200px;
  overflow-y: auto;
}
.result { padding: 8px 10px; cursor: pointer; }
.result:hover { background: var(--accent-soft); }
.small { font-size: 12px; }
.danger { color: var(--st-mood, #c0392b) !important; }
</style>
