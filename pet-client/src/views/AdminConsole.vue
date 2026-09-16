<template>
  <div>
    <header class="head">
      <h2>🛡️ 管理员控制面板</h2>
      <div class="row">
        <button :class="{ active: tab === 'stats' }" @click="tab = 'stats'">数据总览</button>
        <button :class="{ active: tab === 'users' }" @click="tab = 'users'">用户管理</button>
        <button :class="{ active: tab === 'pets' }" @click="tab = 'pets'">宠物管理</button>
        <button :class="{ active: tab === 'shop' }" @click="tab = 'shop'">商店管理</button>
        <button :class="{ active: tab === 'play' }" @click="tab = 'play'">玩耍管理</button>
        <button :class="{ active: tab === 'reports' }" @click="tab = 'reports'">举报中心</button>
        <button class="ghost" @click="emit('go-user')">返回用户端</button>
        <button class="ghost" @click="doLogout">退出登录</button>
      </div>
    </header>

    <!-- 数据总览 -->
    <div v-if="tab === 'stats'" class="grid">
      <div v-for="(v, k) in stats" :key="k" class="stat card">
        <div class="num">{{ v }}</div>
        <div class="muted">{{ label(k) }}</div>
      </div>
    </div>

    <!-- 用户管理 -->
    <div v-else-if="tab === 'users'" class="card">
      <div class="row">
        <input v-model="keyword" placeholder="按用户名/昵称搜索" @keyup.enter="loadUsers" style="max-width: 220px" />
        <button @click="loadUsers">搜索</button>
      </div>
      <table>
        <thead>
          <tr><th>用户名</th><th>昵称</th><th>邮箱</th><th>角色</th><th>状态</th><th>API Key</th><th>注册时间</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.username }}</td>
            <td>{{ u.nickname }}</td>
            <td class="muted">{{ u.email || '—' }}</td>
            <td>{{ u.role === 'ADMIN' ? '管理员' : '用户' }}</td>
            <td :class="u.status === 1 ? 'ok' : 'error'">{{ u.status === 1 ? '启用' : '禁用' }}</td>
            <td>{{ u.hasApiKey ? '✅' : '—' }}</td>
            <td class="muted">{{ fmt(u.createdAt) }}</td>
            <td>
              <button class="small" @click="toggleStatus(u)">{{ u.status === 1 ? '禁用' : '启用' }}</button>
              <button class="small ghost" @click="resetPwd(u)">重置密码</button>
              <button class="small danger" @click="delUser(u)">删除用户</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="row">
        <button class="ghost" @click="page > 1 && (page--, loadUsers())" :disabled="page <= 1">上一页</button>
        <span class="muted">第 {{ page }} 页</span>
        <button class="ghost" @click="page++, loadUsers()">下一页</button>
      </div>
    </div>

    <!-- 宠物管理 -->
    <div v-else-if="tab === 'pets'" class="card">
      <table>
        <thead>
          <tr><th>ID</th><th>主人</th><th>名字</th><th>亚种</th><th>性别</th><th>性格</th><th>等级</th><th>饥饿</th><th>心情</th><th>币</th><th>状态</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in pets" :key="p.petId">
            <td>{{ p.petId }}</td>
            <td>{{ p.owner }}</td>
            <td>{{ p.petName }}</td>
            <td>{{ p.subtypeName }}</td>
            <td>{{ p.gender === 'MALE' ? '公' : '母' }}</td>
            <td>{{ p.personality }}</td>
            <td>{{ p.level }}</td>
            <td>{{ p.hunger }}</td>
            <td>{{ p.mood }}</td>
            <td>{{ p.coins }}</td>
            <td>{{ statusText(p.status) }}</td>
          </tr>
        </tbody>
      </table>
      <div class="row">
        <button class="ghost" @click="petPage > 1 && (petPage--, loadPets())" :disabled="petPage <= 1">上一页</button>
        <span class="muted">第 {{ petPage }} 页</span>
        <button class="ghost" @click="petPage++, loadPets()">下一页</button>
      </div>
    </div>

    <!-- 商店管理 -->
    <div v-else-if="tab === 'shop'" class="card">
      <h3>➕ 新增物品(上架新品)</h3>
      <div class="row form">
        <input v-model="newItem.itemName" placeholder="物品名" style="max-width: 110px" />
        <select v-model="newItem.itemType" style="max-width: 90px">
          <option value="FOOD">食物</option>
          <option value="TOY">玩具</option>
        </select>
        <select v-model="newItem.species" style="max-width: 100px">
          <option value="">通用</option>
          <option v-for="t in allTypes" :key="t.typeCode" :value="t.typeCode">{{ t.typeName }}</option>
        </select>
        <input v-model.number="newItem.price" type="number" min="0" placeholder="价格" style="width: 70px" />
        <input v-model.number="newItem.hungerRestore" type="number" placeholder="饥饿" style="width: 60px" />
        <input v-model.number="newItem.moodRestore" type="number" placeholder="心情" style="width: 60px" />
        <input v-model.number="newItem.minLevel" type="number" min="1" placeholder="解锁等级" style="width: 90px" />
        <input v-model="newItem.image" placeholder="图片路径" style="width: 140px" />
        <button class="ghost small" @click="newItemFile.click()">传图</button>
        <input type="file" ref="newItemFile" accept="image/*" style="display: none" @change="onUploadImage('newItem')" />
        <button @click="addItem">添加</button>
      </div>
      <table>
        <thead>
          <tr><th>物品</th><th>价格</th><th>饥饿恢复</th><th>心情恢复</th><th>解锁等级</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="it in shopItems" :key="it.id">
            <td><img v-if="it.image" :src="it.image" class="thumb" alt="" /> {{ it.itemName }}</td>
            <td>
              <input v-model.number="editPrice[it.id]" type="number" min="0" style="width: 70px" />
            </td>
            <td>{{ it.hungerRestore }}</td>
            <td>{{ it.moodRestore }}</td>
            <td>{{ it.minLevel }}</td>
            <td :class="it.enabled === 1 ? 'ok' : 'error'">{{ it.enabled === 1 ? '上架' : '下架' }}</td>
            <td>
              <button class="small" @click="saveItem(it)">保存价格</button>
              <button class="small ghost" @click="toggleItem(it)">{{ it.enabled === 1 ? '下架' : '上架' }}</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 玩耍管理 -->
    <div v-else-if="tab === 'play'" class="card">
      <h3>🎮 玩耍模式管理(可增删改,配图)</h3>
      <div class="row form">
        <input v-model="pm.name" placeholder="模式名,如 跳舞" style="max-width: 110px" />
        <input v-model="pm.icon" placeholder="emoji" style="max-width: 70px" />
        <input v-model="pm.image" placeholder="图片路径" style="width: 140px" />
        <button class="ghost small" @click="pmFile.click()">传图</button>
        <input type="file" ref="pmFile" accept="image/*" style="display: none" @change="onUploadImage('pm')" />
        <input v-model.number="pm.mood" type="number" placeholder="心情" style="width: 60px" />
        <input v-model.number="pm.hunger" type="number" placeholder="饥饿" style="width: 60px" />
        <input v-model.number="pm.exp" type="number" placeholder="经验" style="width: 60px" />
        <button @click="addPlayMode">添加</button>
      </div>
      <table>
        <thead>
          <tr><th>模式</th><th>图标/图</th><th>心情</th><th>饥饿</th><th>经验</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="m in playModes" :key="m.id">
            <td>{{ m.name }}</td>
            <td><img v-if="m.image" :src="m.image" class="thumb" alt="" /><span v-else>{{ m.icon }}</span></td>
            <td>{{ m.mood }}</td>
            <td>{{ m.hunger }}</td>
            <td>{{ m.exp }}</td>
            <td :class="m.enabled === 1 ? 'ok' : 'error'">{{ m.enabled === 1 ? '启用' : '停用' }}</td>
            <td><button class="small danger" @click="delPlayMode(m)">删除</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 举报中心 -->
    <div v-else-if="tab === 'reports'" class="card">
      <h3>🚩 举报中心(审核聊天内容后可拉黑账户)</h3>
      <div v-for="r in reports" :key="r.id" class="report">
        <div class="row">
          <span>
            <b>{{ r.reporter }}</b> 举报 <b>{{ r.target }}</b>
            <span class="muted">{{ r.status === 'PENDING' ? '待审核' : r.status === 'BLOCKED' ? '已拉黑' : '已驳回' }} · {{ fmt(r.createdAt) }}</span>
          </span>
          <div class="row" v-if="r.status === 'PENDING'">
            <button class="small danger" @click="resolveReport(r, 'BLOCK')">拉黑该账户</button>
            <button class="small ghost" @click="resolveReport(r, 'DISMISS')">驳回</button>
          </div>
        </div>
        <p class="muted">举报说明:{{ r.content }}</p>
        <p v-if="r.messageContent" class="msg-content">相关聊天内容:「{{ r.messageContent }}」</p>
      </div>
      <p v-if="!reports.length" class="muted">暂无举报。</p>
    </div>

    <!-- 测试工具 -->
    <div class="card">
      <h3>🧪 测试工具 / 系统设置</h3>
      <p class="muted small">学习(番茄钟进行中)时宠物静音。</p>
      <div class="row">
        <button @click="testCompanion">💬 触发陪伴聊天泡</button>
        <button @click="testNaughty">😼 触发卖萌</button>
        <button @click="testSilent">🔕 学习中静音测试</button>
        <button @click="testInvite">🙋 触发主动邀请</button>
        <button class="ghost" @click="openPixel">🖼️ 打开像素版前端</button>
        <button class="ghost" @click="openSecond">🪟 新开测试窗口</button>
      </div>
      <div class="row set-row">
        <span class="muted">对话记忆条数(管理员可改):</span>
        <input v-model.number="chatMemory" type="number" min="1" max="100" style="width: 80px" />
        <button class="small" @click="saveChatMemory">保存</button>
      </div>
      <p v-if="testMsg" class="ok">{{ testMsg }}</p>
      <p v-if="testErr" class="error">{{ testErr }}</p>
    </div>

    <p v-if="msg" class="ok">{{ msg }}</p>
    <p v-if="err" class="error">{{ err }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import http from '../api/http';
import { logout } from '../api/http';

const emit = defineEmits(['go-user', 'logout']);
defineProps({ user: Object });

const tab = ref('stats');
const stats = ref({});
const users = ref([]);
const pets = ref([]);
const shopItems = ref([]);
const playModes = ref([]);
const reports = ref([]);
const allTypes = ref([]);
const newItem = ref({ itemName: '', itemType: 'FOOD', species: '', price: 1, hungerRestore: 0, moodRestore: 0, minLevel: 1, image: '' });
const pm = ref({ name: '', icon: '', image: '', mood: 0, hunger: 0, exp: 0 });
const chatMemory = ref(10);
const newItemFile = ref(null);
const pmFile = ref(null);
const keyword = ref('');
const page = ref(1);
const petPage = ref(1);
const editPrice = ref({});
const msg = ref('');
const err = ref('');
const testMsg = ref('');
const testErr = ref('');

async function testCompanion() {
  testErr.value = '';
  testMsg.value = '';
  try {
    const c = await http.get('/api/push/companion');
    if (window.petAPI) window.petAPI.sendBubble(c.text);
    testMsg.value = '已推送聊天泡:' + c.text;
  } catch (e) {
    testErr.value = e.message;
  }
}

async function testNaughty() {
  testErr.value = '';
  testMsg.value = '';
  try {
    const n = await http.get('/api/push/naughty');
    if (window.petAPI) window.petAPI.sendBubble(n.text);
    testMsg.value = '已推送卖萌:' + n.text;
  } catch (e) {
    testErr.value = e.message;
  }
}

function testSilent() {
  // 学习中静音由主进程判断(番茄钟进行中);发一条测试消息看是否被静音
  if (window.petAPI) window.petAPI.sendBubble('测试消息:主人,我能说话吗?');
  testMsg.value = '已发送测试消息(若番茄钟进行中会提示静音)';
}

function openSecond() {
  if (window.petAPI) window.petAPI.openSecondWindow();
  testMsg.value = '已打开测试窗口 2(登录另一个测试账号即可双账号交互)';
}

function openPixel() {
  if (window.petAPI) window.petAPI.openPixel();
  testMsg.value = '已打开像素版前端(与后端同源联调)';
}

function label(k) {
  return { userCount: '用户数', petCount: '宠物数', scheduleCount: '日程数', chatCount: '对话数', pomodoroCount: '番茄钟数', gravestoneCount: '墓碑数' }[k] || k;
}
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '';
}
function statusText(s) {
  return { ALIVE: '活着', DANGER: '病危', STARVED: '已死' }[s] || s;
}

async function loadStats() {
  stats.value = await http.get('/api/admin/stats');
}

async function loadUsers() {
  try {
    const p = await http.get('/api/admin/users', { params: { page: page.value, size: 10, keyword: keyword.value } });
    users.value = p.records;
  } catch (e) {
    err.value = e.message;
  }
}

async function toggleStatus(u) {
  try {
    const target = u.status === 1 ? 0 : 1;
    await http.put('/api/admin/users/' + u.id + '/status', { status: target });
    await loadUsers();
  } catch (e) {
    err.value = e.message;
  }
}

async function delUser(u) {
  if (!window.confirm('确定删除用户 ' + u.username + '?(将禁用且不可登录)')) return;
  try {
    await http.delete('/api/admin/users/' + u.id);
    msg.value = '已删除用户 ' + u.username;
    await loadUsers();
  } catch (e) {
    err.value = e.message;
  }
}

async function addItem() {
  try {
    await http.post('/api/admin/shop-items', { ...newItem.value });
    msg.value = '已上架新物品';
    newItem.value = { itemName: '', itemType: 'FOOD', species: '', price: 1, hungerRestore: 0, moodRestore: 0, minLevel: 1, image: '' };
    await loadShop();
  } catch (e) {
    err.value = e.message;
  }
}

function onUploadImage(which) {
  const input = which === 'newItem' ? newItemFile.value : pmFile.value;
  const file = input && input.files && input.files[0];
  if (!file) return;
  const fd = new FormData();
  fd.append('file', file);
  http
    .post('/api/admin/upload', fd)
    .then((data) => {
      if (which === 'newItem') newItem.value.image = data.path;
      else pm.value.image = data.path;
      msg.value = '图片已上传:' + data.path;
    })
    .catch((e) => {
      err.value = e.message;
    });
}

async function loadPlayModes() {
  try {
    playModes.value = await http.get('/api/admin/play-modes');
  } catch (e) {
    err.value = e.message;
  }
}

async function addPlayMode() {
  try {
    await http.post('/api/admin/play-modes', { ...pm.value });
    msg.value = '已添加玩耍模式';
    pm.value = { name: '', icon: '', image: '', mood: 0, hunger: 0, exp: 0 };
    await loadPlayModes();
  } catch (e) {
    err.value = e.message;
  }
}

async function delPlayMode(m) {
  if (!window.confirm('删除玩耍模式「' + m.name + '」?')) return;
  try {
    await http.delete('/api/admin/play-modes/' + m.id);
    await loadPlayModes();
  } catch (e) {
    err.value = e.message;
  }
}

async function loadReports() {
  try {
    reports.value = await http.get('/api/admin/reports');
  } catch (e) {
    err.value = e.message;
  }
}

async function resolveReport(r, action) {
  try {
    await http.post('/api/admin/reports/' + r.id + '/resolve', { action });
    msg.value = action === 'BLOCK' ? '已拉黑该账户' : '已驳回';
    await loadReports();
  } catch (e) {
    err.value = e.message;
  }
}

async function saveChatMemory() {
  try {
    await http.put('/api/admin/settings/chat-memory', { value: chatMemory.value });
    msg.value = '对话记忆条数已更新为 ' + chatMemory.value;
  } catch (e) {
    err.value = e.message;
  }
}

function testInvite() {
  if (window.petAPI) window.petAPI.triggerInvite();
  testMsg.value = '已触发宠物主动邀请(陪玩/学习)';
}

async function resetPwd(u) {
  const pwd = window.prompt('给 ' + u.username + ' 设置新密码(至少 8 位):');
  if (!pwd) return;
  try {
    await http.post('/api/admin/users/' + u.id + '/reset-password', { newPassword: pwd });
    msg.value = '已重置 ' + u.username + ' 的密码';
  } catch (e) {
    err.value = e.message;
  }
}

async function loadPets() {
  try {
    const p = await http.get('/api/admin/pets', { params: { page: petPage.value, size: 10 } });
    pets.value = p.records;
  } catch (e) {
    err.value = e.message;
  }
}

async function loadShop() {
  try {
    shopItems.value = await http.get('/api/admin/shop-items');
    for (const it of shopItems.value) editPrice.value[it.id] = it.price;
  } catch (e) {
    err.value = e.message;
  }
}

async function saveItem(it) {
  try {
    await http.put('/api/admin/shop-items/' + it.id, { price: editPrice.value[it.id] });
    msg.value = '已保存 ' + it.itemName + ' 的价格';
  } catch (e) {
    err.value = e.message;
  }
}

async function toggleItem(it) {
  try {
    await http.put('/api/admin/shop-items/' + it.id, { enabled: it.enabled === 1 ? 0 : 1 });
    await loadShop();
  } catch (e) {
    err.value = e.message;
  }
}

function doLogout() {
  logout();
  emit('logout');
}

onMounted(() => {
  loadStats();
  loadUsers();
  loadPets();
  loadShop();
  loadPlayModes();
  loadReports();
  http.get('/api/pet/types').then((t) => (allTypes.value = t)).catch(() => {});
  http.get('/api/admin/settings/chat-memory').then((d) => (chatMemory.value = d.value)).catch(() => {});
});
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.head h2 {
  margin: 0;
  font-size: 22px;
  white-space: nowrap;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}
.stat {
  text-align: center;
  padding: 20px 10px;
}
.num {
  font-size: 30px;
  font-weight: bold;
  color: #ff9f45;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
  font-size: 14px;
}
th, td {
  text-align: left;
  padding: 8px 6px;
  border-bottom: 1px solid #f0e0c8;
}
button.small {
  padding: 4px 10px;
  font-size: 12px;
}
button.ghost {
  background: transparent;
  border: 1px solid #e5d5c0;
  color: #9a8a75;
}
button.danger {
  background: #f4b8b0;
  color: #7c2d23;
}
.thumb {
  width: 30px;
  height: 30px;
  object-fit: cover;
  border-radius: 6px;
  vertical-align: middle;
}
.form {
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.form input, .form select {
  padding: 6px 8px;
  border: 1px solid #e5d5c0;
  border-radius: 8px;
  font-size: 13px;
  background: #fffdf8;
}
.set-row {
  margin-top: 10px;
}
.report {
  padding: 10px 0;
  border-bottom: 1px dashed #f0e0c8;
}
.msg-content {
  background: #f7efe3;
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
}
</style>
