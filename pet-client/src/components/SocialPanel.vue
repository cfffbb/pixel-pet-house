<template>
  <div class="social-panel">
    <!-- 对话视图 -->
    <div v-if="activeChat" class="chat-view">
      <div class="chat-header">
        <button class="back-btn" @click="backToList">←</button>
        <span class="chat-title">
          <span v-if="activeChat.isOfficial" class="official-badge">官方</span>
          {{ activeChat.nickname }}
          <span class="muted small" v-if="activeChat.playerId">{{ activeChat.playerId }}</span>
        </span>
        <div v-if="!activeChat.isOfficial" class="chat-actions">
          <button class="act-btn" :title="activeChat.muted ? '取消免打扰' : '免打扰'" @click="toggleMute">{{ activeChat.muted ? '🔕' : '🔔' }}</button>
          <button class="act-btn" title="拉黑" @click="blockUser">🚫</button>
          <button class="act-btn" title="举报" @click="reportUser">🚩</button>
          <button class="act-btn" title="删除对话" @click="deleteConversation">🗑</button>
        </div>
      </div>
      <div class="msg-thread" ref="threadEl">
        <div v-for="(m, idx) in thread" :key="m.messageId" class="msg-row" :class="m.isMine ? 'mine' : 'other'">
          <span class="bubble">{{ m.content }}</span>
          <span class="muted small msg-time">{{ fmtTime(m.createdAt) }}</span>
        </div>
        <p v-if="!thread.length" class="muted center empty">还没有消息,发条消息开始聊天吧~</p>
      </div>
      <div class="chat-input-bar">
        <input v-model="chatText" placeholder="说点什么…" @keyup.enter="sendChat" />
        <button @click="sendChat" :disabled="!chatText.trim()">发送</button>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-view">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <input v-model="searchKey" placeholder="搜索玩家:昵称 / 用户名 / 玩家ID" @input="onSearch" />
      </div>
      <!-- 搜索结果 -->
      <div v-if="searchResults.length" class="search-results">
        <div v-for="r in searchResults" :key="r.userId" class="search-row" @click="addFriend(r)">
          <div class="avatar-placeholder">🐾</div>
          <div class="info">
            <span class="name">{{ r.nickname || r.username }}</span>
            <span class="muted small">@{{ r.username }} · {{ r.playerId || '—' }}</span>
          </div>
          <button class="add-btn">+ 加好友</button>
        </div>
        <p class="muted small center" style="padding:6px">点击搜索结果发送好友申请</p>
      </div>

      <!-- 聊天列表 -->
      <div v-else class="chat-list">
        <div v-if="!chatList.length" class="muted center empty">暂无聊天,先搜索添加好友吧~</div>
        <div v-for="c in chatList" :key="c.userId" class="chat-row" :class="{ official: c.isOfficial }" @click="openChat(c)">
          <div class="avatar-placeholder" :class="{ off: c.isOfficial }">
            {{ c.isOfficial ? '📢' : '🐾' }}
          </div>
          <div class="chat-info">
            <div class="row-1">
              <span class="name">{{ c.nickname }}</span>
              <span v-if="c.isOfficial" class="official-tag">官方助手</span>
              <span class="time">{{ fmtShort(c.lastTime) }}</span>
            </div>
            <div class="row-2">
              <span class="last-msg">{{ c.lastContent || (c.pendingCount ? `${c.pendingCount} 条待处理申请` : '点击开始聊天') }}</span>
              <span v-if="c.unreadCount" class="unread-badge">{{ c.unreadCount }}</span>
              <span v-if="c.pendingCount" class="pending-badge">{{ c.pendingCount }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 收件箱申请(待处理) -->
      <div v-if="pendingRequests.length" class="requests-section">
        <h4>📬 待处理申请 ({{ pendingRequests.length }})</h4>
        <div v-for="r in pendingRequests" :key="r.messageId" class="request-row">
          <div class="info">
            <span class="name">{{ r.senderNickname }}</span>
            <span class="muted small">{{ r.content }}</span>
          </div>
          <div class="actions">
            <button class="accept-btn" @click="acceptRequest(r)">✅ 同意</button>
            <button class="reject-btn" @click="rejectRequest(r)">❌ 拒绝</button>
          </div>
        </div>
      </div>

      <p v-if="socialErr" class="error center">{{ socialErr }}</p>
      <p v-if="socialMsg" class="ok center">{{ socialMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue';
import http from '../api/http';
import { useSound } from '../soundStore';

const { playSound, playFriendMessage } = useSound();

const chatList = ref([]);
const activeChat = ref(null);
const thread = ref([]);
const chatText = ref('');
const threadEl = ref(null);
const searchKey = ref('');
const searchResults = ref([]);
const pendingRequests = ref([]);
const socialErr = ref('');
const socialMsg = ref('');
let searchTimer = null;

function fmtTime(t) {
  if (!t) return '';
  const s = String(t);
  return s.slice(5, 16).replace('T', ' ');
}
function fmtShort(t) {
  if (!t) return '';
  const s = String(t);
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;
  const msgDay = s.slice(0, 10);
  if (msgDay === today) return s.slice(11, 16);
  return s.slice(5, 10);
}

async function loadChatList() {
  try {
    chatList.value = await http.get('/api/inbox/chat-list');
  } catch (e) {
    socialErr.value = e.message;
  }
}

async function loadPendingRequests() {
  try {
    const inbox = await http.get('/api/inbox');
    pendingRequests.value = (inbox || []).filter(m => m.requestStatus === 'PENDING' && m.type !== 'CHAT');
  } catch (e) {}
}

async function openChat(c) {
  playSound('button');
  activeChat.value = c;
  thread.value = [];
  chatText.value = '';
  try {
    thread.value = await http.get(`/api/inbox/conversation?with=${c.userId}`);
    await nextTick();
    if (threadEl.value) threadEl.value.scrollTop = threadEl.value.scrollHeight;
    await loadChatList();
    await loadPendingRequests();
  } catch (e) {
    socialErr.value = e.message;
  }
}

function backToList() {
  playSound('button');
  activeChat.value = null;
  thread.value = [];
  loadChatList();
}

async function sendChat() {
  if (!chatText.value.trim() || !activeChat.value) return;
  playSound('button');
  const text = chatText.value;
  chatText.value = '';
  try {
    await http.post('/api/inbox/message', { receiverId: activeChat.value.userId, content: text });
    thread.value.push({ messageId: Date.now(), content: text, isMine: true, createdAt: new Date().toISOString() });
    await nextTick();
    if (threadEl.value) threadEl.value.scrollTop = threadEl.value.scrollHeight;
    loadChatList();
  } catch (e) {
    socialErr.value = e.message;
    chatText.value = text;
  }
}

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(async () => {
    if (!searchKey.value.trim()) {
      searchResults.value = [];
      return;
    }
    try {
      searchResults.value = await http.get(`/api/users/search?keyword=${encodeURIComponent(searchKey.value.trim())}`);
    } catch (e) {
      searchResults.value = [];
    }
  }, 300);
}

async function addFriend(r) {
  playSound('button');
  socialErr.value = '';
  socialMsg.value = '';
  try {
    await http.post('/api/friends/request', { targetUserId: r.userId });
    socialMsg.value = `已向 ${r.nickname || r.username} 发送好友申请`;
    setTimeout(() => { socialMsg.value = ''; }, 3000);
  } catch (e) {
    socialErr.value = e.message;
  }
}

async function acceptRequest(r) {
  playSound('button');
  try {
    await http.post(`/api/inbox/${r.messageId}/accept`);
    socialMsg.value = '已同意申请';
    await loadPendingRequests();
    await loadChatList();
    setTimeout(() => { socialMsg.value = ''; }, 2000);
  } catch (e) {
    socialErr.value = e.message;
  }
}

async function rejectRequest(r) {
  playSound('button');
  try {
    await http.post(`/api/inbox/${r.messageId}/reject`);
    socialMsg.value = '已拒绝申请';
    await loadPendingRequests();
    setTimeout(() => { socialMsg.value = ''; }, 2000);
  } catch (e) {
    socialErr.value = e.message;
  }
}

async function deleteConversation() {
  if (!activeChat.value) return;
  playSound('button');
  try {
    await http.delete(`/api/inbox/conversation?with=${activeChat.value.userId}`);
    socialMsg.value = '对话已删除';
    backToList();
    setTimeout(() => { socialMsg.value = ''; }, 2000);
  } catch (e) {
    socialErr.value = e.message;
  }
}

async function toggleMute() {
  if (!activeChat.value) return;
  playSound('button');
  const newMuted = activeChat.value.muted ? 0 : 1;
  try {
    await http.put(`/api/friends/${activeChat.value.userId}/mute`, { muted: newMuted });
    activeChat.value.muted = newMuted;
    await loadChatList();
    socialMsg.value = newMuted ? '已开启免打扰' : '已关闭免打扰';
    setTimeout(() => { socialMsg.value = ''; }, 2000);
  } catch (e) {
    socialErr.value = e.message;
  }
}

async function blockUser() {
  if (!activeChat.value) return;
  if (!window.confirm(`确认拉黑 ${activeChat.value.nickname}?拉黑后对方将无法给你发消息`)) return;
  playSound('button');
  try {
    await http.post(`/api/blacklist/${activeChat.value.userId}`);
    socialMsg.value = '已拉黑该用户';
    backToList();
    setTimeout(() => { socialMsg.value = ''; }, 2000);
  } catch (e) {
    socialErr.value = e.message;
  }
}

async function reportUser() {
  if (!activeChat.value) return;
  const reason = window.prompt(`请输入举报 ${activeChat.value.nickname} 的理由:`);
  if (!reason || !reason.trim()) return;
  playSound('button');
  try {
    await http.post('/api/reports', { targetUserId: activeChat.value.userId, content: reason.trim() });
    socialMsg.value = '举报已提交,管理员会尽快处理';
    setTimeout(() => { socialMsg.value = ''; }, 3000);
  } catch (e) {
    socialErr.value = e.message;
  }
}

const props = defineProps({ presetTarget: Object });
watch(() => props.presetTarget, (t) => {
  if (t && t.userId) {
    activeChat.value = { userId: t.userId, nickname: t.nickname || t.username, isOfficial: false };
    openChat(activeChat.value);
  }
}, { immediate: true });

onMounted(() => {
  loadChatList();
  loadPendingRequests();
});
</script>

<style scoped>
.social-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
}

/* 列表视图 */
.search-bar {
  padding: 8px 0;
}
.search-bar input {
  width: 100%;
  padding: 10px 14px;
  border: 2px solid #f0e0cc;
  border-radius: 12px;
  font-size: 14px;
  background: #fffdf8;
  box-sizing: border-box;
}
.search-bar input:focus {
  border-color: #ff9f45;
  outline: none;
}

.search-results {
  margin: 4px 0;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.search-row:hover {
  background: #fff5eb;
}
.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #fff3e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.avatar-placeholder.off {
  background: #e3f2fd;
}
.info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.name {
  font-weight: 600;
  font-size: 14px;
  color: #6b4f3a;
}
.add-btn {
  padding: 4px 12px;
  font-size: 12px;
  background: #ff9f45;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
}

/* 聊天列表 */
.chat-list {
  flex: 1;
  overflow-y: auto;
}
.chat-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f5ede0;
}
.chat-row:hover {
  background: #fff5eb;
}
.chat-row.official {
  background: #f0f7ff;
}
.chat-info {
  flex: 1;
  min-width: 0;
}
.row-1 {
  display: flex;
  align-items: center;
  gap: 6px;
}
.row-1 .time {
  margin-left: auto;
  font-size: 11px;
  color: #b0a090;
}
.official-tag {
  font-size: 10px;
  background: #1976d2;
  color: #fff;
  padding: 1px 6px;
  border-radius: 4px;
}
.row-2 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}
.last-msg {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.unread-badge {
  background: #ff4444;
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}
.pending-badge {
  background: #ff9f45;
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

/* 待处理申请 */
.requests-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 2px solid #f0e0cc;
}
.requests-section h4 {
  margin: 0 0 6px;
  font-size: 14px;
}
.request-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #fff8ef;
  margin-bottom: 4px;
}
.request-row .info {
  flex: 1;
}
.request-row .actions {
  display: flex;
  gap: 6px;
}
.accept-btn, .reject-btn {
  padding: 4px 10px;
  font-size: 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.accept-btn {
  background: #4caf50;
  color: #fff;
}
.reject-btn {
  background: #f44336;
  color: #fff;
}

/* 对话视图 */
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 2px solid #f0e0cc;
}
.back-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #fff3e0;
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  color: #6b4f3a;
}
.chat-title {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
  color: #6b4f3a;
}
.official-badge {
  font-size: 10px;
  background: #1976d2;
  color: #fff;
  padding: 1px 6px;
  border-radius: 4px;
  margin-right: 4px;
}
.del-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
}
.chat-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}
.act-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  padding: 2px 4px;
  border-radius: 6px;
  transition: background 0.15s;
}
.act-btn:hover {
  background: rgba(0, 0, 0, 0.06);
}
.msg-thread {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.msg-row {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}
.msg-row.mine {
  align-self: flex-end;
  align-items: flex-end;
}
.msg-row.other {
  align-self: flex-start;
  align-items: flex-start;
}
.msg-row .bubble {
  padding: 8px 14px;
  border-radius: 14px;
  font-size: 14px;
  word-break: break-word;
}
.msg-row.mine .bubble {
  background: #ff9f45;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg-row.other .bubble {
  background: #fff;
  color: #6b4f3a;
  border: 1px solid #f0e0cc;
  border-bottom-left-radius: 4px;
}
.msg-time {
  font-size: 10px;
  margin-top: 2px;
}
.chat-input-bar {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  border-top: 2px solid #f0e0cc;
}
.chat-input-bar input {
  flex: 1;
  padding: 10px 14px;
  border: 2px solid #f0e0cc;
  border-radius: 12px;
  font-size: 14px;
  background: #fffdf8;
}
.chat-input-bar input:focus {
  border-color: #ff9f45;
  outline: none;
}
.chat-input-bar button {
  padding: 10px 20px;
  background: #ff9f45;
  color: #fff;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
}
.chat-input-bar button:disabled {
  opacity: 0.5;
}

.empty {
  padding: 40px 0;
  font-size: 14px;
}
.muted { color: #999; }
.small { font-size: 12px; }
.center { text-align: center; }
.ok { color: #4caf50; }
.error { color: #f44336; }
</style>
