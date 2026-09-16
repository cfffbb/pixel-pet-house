<template>
  <div>
    <div class="card">
      <h3>📬 收件箱</h3>
      <div v-for="m in messages" :key="m.messageId" class="msg" :class="m.status === 'UNREAD' ? 'unread' : ''">
        <div class="msg-head">
          <span>
            <b>{{ m.senderNickname }}</b>
            <span class="muted">{{ typeText(m.type) }} · {{ fmt(m.createdAt) }}</span>
          </span>
          <div class="msg-actions">
            <template v-if="m.type !== 'CHAT' && m.requestStatus === 'PENDING'">
              <button class="pix-btn btn-soft btn-sm" @click="accept(m)">✅ 同意</button>
              <button class="pix-btn btn-ghost btn-sm" @click="reject(m)">❌ 拒绝</button>
            </template>
            <button v-if="m.status === 'UNREAD'" class="pix-btn btn-ghost btn-sm" @click="read(m)">已读</button>
          </div>
        </div>
        <p>{{ m.content }}</p>
      </div>
      <p v-if="!messages.length" class="muted">收件箱是空的。</p>
      <p v-if="msg" class="ok">{{ msg }}</p>
      <p v-if="err" class="error">{{ err }}</p>
    </div>

    <div class="card">
      <h3>💬 私聊</h3>
      <div class="chat-search-row">
        <div class="search">
          <input v-model="searchKey" placeholder="搜索用户(用户名/昵称)" @input="onSearch" class="chat-input" />
          <div v-if="results.length" class="results">
            <div v-for="r in results" :key="r.userId" class="result" @click="pick(r)">
              {{ r.nickname || r.username }} <span class="muted">@{{ r.username }}</span>
            </div>
          </div>
        </div>
        <input v-model="content" placeholder="说点什么…" @keyup.enter="send" class="chat-input" />
        <button class="pix-btn btn-primary btn-sm" @click="send">发送</button>
      </div>
      <div class="target-bar" v-if="target">
        <span>发给:{{ target.nickname || target.username }}</span>
        <button class="pix-btn btn-ghost btn-sm" @click="toggleMute(target)">{{ target.muted ? '🔔 取消免打扰' : '🔇 免打扰' }}</button>
        <button class="pix-btn btn-ghost btn-sm" @click="block(target)">🚫 拉黑</button>
        <button class="pix-btn btn-ghost btn-sm" @click="report(target)">🚩 举报</button>
        <button class="pix-btn btn-ghost btn-sm" @click="clearConv(target)">🗑 删除对话</button>
      </div>
      <p v-if="chatMsg" class="ok">{{ chatMsg }}</p>
      <p v-if="chatErr" class="error">{{ chatErr }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import http from '../api/http';
import { useSound } from '../soundStore';

const { playSound, playFriendMessage } = useSound();

const props = defineProps({ presetTarget: { type: Object, default: null } });

const messages = ref([]);
const msg = ref('');
const err = ref('');
const searchKey = ref('');
const results = ref([]);
const target = ref(null);
const content = ref('');
const chatMsg = ref('');
const chatErr = ref('');
let searchTimer = null;

function fmt(t) { return t ? String(t).replace('T', ' ').slice(5, 16) : ''; }
function typeText(t) {
  return { BREED_REQUEST: '配种申请', GIFT_REQUEST: '赠送申请', FRIEND_REQUEST: '好友申请', CHAT: '聊天', REPORT: '工资单' }[t] || t;
}

async function load() {
  try {
    messages.value = await http.get('/api/inbox');
  } catch (e) { err.value = e.message; }
}

async function accept(m) {
  playSound('button');
  try { await http.post('/api/inbox/' + m.messageId + '/accept'); await load(); } catch (e) { err.value = e.message; }
}
async function reject(m) {
  playSound('button');
  try { await http.post('/api/inbox/' + m.messageId + '/reject'); await load(); } catch (e) { err.value = e.message; }
}
async function read(m) {
  try { await http.post('/api/inbox/' + m.messageId + '/read'); await load(); } catch (e) { err.value = e.message; }
}

async function onSearch() {
  clearTimeout(searchTimer);
  const kw = searchKey.value.trim();
  if (!kw) { results.value = []; return; }
  searchTimer = setTimeout(async () => {
    try { results.value = await http.get('/api/users/search', { params: { keyword: kw } }); } catch (e) { results.value = []; }
  }, 300);
}

function pick(r) {
  target.value = r;
  results.value = [];
  searchKey.value = '';
  playSound('button');
}

async function send() {
  chatMsg.value = ''; chatErr.value = '';
  if (!target.value) { chatErr.value = '请先搜索并选择一个用户'; return; }
  playSound('button');
  try {
    await http.post('/api/inbox/message', { receiverId: target.value.userId, content: content.value });
    chatMsg.value = '已发送给 ' + (target.value.nickname || target.value.username);
    content.value = '';
    playFriendMessage();
  } catch (e) { chatErr.value = e.message; }
}

async function toggleMute(u) {
  playSound('button');
  try {
    await http.put('/api/friends/' + u.userId + '/mute', { muted: u.muted ? 0 : 1 });
    u.muted = !u.muted;
    chatMsg.value = u.muted ? '已开启免打扰' : '已取消免打扰';
  } catch (e) { chatErr.value = e.message; }
}

async function block(u) {
  if (!window.confirm('拉黑 ' + (u.nickname || u.username) + '?')) return;
  playSound('button');
  try {
    await http.post('/api/blacklist/' + u.userId);
    chatMsg.value = '已拉黑 ' + (u.nickname || u.username);
  } catch (e) { chatErr.value = e.message; }
}

async function report(u) {
  const reason = window.prompt('举报 ' + (u.nickname || u.username) + ' 的原因:');
  if (!reason) return;
  playSound('button');
  try {
    await http.post('/api/reports', { targetUserId: u.userId, content: reason });
    chatMsg.value = '已举报,管理员审核后会处理';
  } catch (e) { chatErr.value = e.message; }
}

async function clearConv(u) {
  if (!window.confirm('删除与 ' + (u.nickname || u.username) + ' 的对话?')) return;
  playSound('button');
  try {
    await http.delete('/api/inbox/conversation', { params: { with: u.userId } });
    chatMsg.value = '对话已删除';
    target.value = null;
    await load();
  } catch (e) { chatErr.value = e.message; }
}

onMounted(load);

watch(() => props.presetTarget, (t) => {
  if (t) { target.value = t; }
});
</script>

<style scoped>
.msg { padding: 8px 0; border-bottom: 1px dashed var(--border); }
.msg.unread { background: var(--accent-soft); border-radius: var(--radius); padding: 8px 10px; }
.msg-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.msg-actions { display: flex; gap: 4px; }
.chat-search-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.search { position: relative; flex: 1; min-width: 150px; }
.chat-input {
  width: auto !important;
  padding: 8px 12px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 10px !important;
  font-size: 13px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  box-shadow: none !important;
  flex: 1;
}
.chat-input:focus { border-color: var(--accent) !important; }
.results {
  position: absolute;
  top: 100%; left: 0; right: 0;
  background: var(--surface);
  border: 2px solid var(--border);
  border-radius: var(--radius);
  z-index: 20;
  max-height: 200px;
  overflow-y: auto;
}
.result { padding: 8px 10px; cursor: pointer; }
.result:hover { background: var(--accent-soft); }
.target-bar {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: 10px;
  flex-wrap: wrap;
  font-size: 13px;
}
.small { font-size: 12px; }
</style>
