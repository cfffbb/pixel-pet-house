<template>
  <div id="authView" class="auth-view">
    <div class="auth-scene" :style="{ backgroundImage: 'url(./register-bg.jpg)' }"></div>
    <div class="auth-dim"></div>
    <div class="auth-wrap">
      <div class="auth-card">
        <div class="auth-brand">
          <span class="dot"></span>
          <h1>像素宠物小屋</h1>
        </div>

        <!-- 认证标签栏 -->
        <div class="auth-tabs">
          <button
            v-for="t in tabs"
            :key="t.id"
            class="chip"
            :class="{ active: mode === t.id }"
            @click="playSound('button'); mode = t.id; err = ''; ok = ''"
          >
            {{ t.label }}
          </button>
        </div>

        <!-- 登录 -->
        <div v-if="mode === 'login'" class="stack">
          <div class="field">
            <label>用户名</label>
            <input v-model="username" placeholder="请输入用户名" @keyup.enter="doLogin">
          </div>
          <div class="field">
            <label>密码</label>
            <input v-model="password" type="password" placeholder="请输入密码" @keyup.enter="doLogin">
          </div>
          <button class="pix-btn btn-primary btn-block star" @click="doLogin">登 录</button>
        </div>

        <!-- 注册 -->
        <div v-else-if="mode === 'register'" class="stack">
          <div class="field">
            <label>用户名</label>
            <input v-model="r.username" placeholder="2-20 位字符">
          </div>
          <div class="field">
            <label>昵称</label>
            <input v-model="r.nickname" placeholder="怎么称呼你">
          </div>
          <div class="field">
            <label>邮箱</label>
            <div class="row">
              <input v-model="r.email" placeholder="用于接收验证码" type="email" style="flex:1">
              <button class="pix-btn btn-ghost btn-sm" @click="sendCode" :disabled="sending">
                {{ sending ? '发送中…' : '发送验证码' }}
              </button>
            </div>
          </div>
          <div class="field">
            <label>邮箱验证码</label>
            <input v-model="r.code" placeholder="6 位数字">
          </div>
          <div class="field">
            <label>密码</label>
            <input v-model="r.password" type="password" placeholder="至少 8 位">
          </div>
          <div class="field">
            <label>确认密码</label>
            <input v-model="r.confirm" type="password" placeholder="再次输入密码">
          </div>
          <div class="field">
            <label>密保问题</label>
            <select v-model="r.securityQuestion">
              <option v-for="q in questions" :key="q" :value="q">{{ q }}</option>
            </select>
          </div>
          <div class="field">
            <label>密保答案</label>
            <input v-model="r.securityAnswer" placeholder="用于找回密码">
          </div>
          <button class="pix-btn btn-primary btn-block star" @click="doRegister">注 册</button>
        </div>

        <!-- 忘记密码 -->
        <div v-else class="stack">
          <div class="field">
            <label>用户名</label>
            <div class="row">
              <input v-model="f.username" placeholder="请输入用户名" style="flex:1">
              <button class="pix-btn btn-soft btn-sm" @click="loadQuestion" :disabled="!f.username">获取问题</button>
            </div>
          </div>
          <p v-if="f.question" class="ok">密保问题: {{ f.question }}</p>
          <div class="field">
            <label>密保答案</label>
            <input v-model="f.answer" placeholder="请输入密保答案">
          </div>
          <div class="field">
            <label>新密码</label>
            <input v-model="f.newPassword" type="password" placeholder="请输入新密码">
          </div>
          <button class="pix-btn btn-primary btn-block star" @click="doForgot" :disabled="!f.question">重置密码</button>
        </div>

        <p v-if="err" class="error">{{ err }}</p>
        <p v-if="ok" class="ok">{{ ok }}</p>

        <!-- 封禁提醒弹窗 -->
        <transition name="ban-fade">
          <div v-if="banVisible" class="ban-overlay" @click.self="playSound('button'); banVisible = false">
            <div class="ban-modal">
              <div class="ban-icon">🚫</div>
              <h3>账号已被封禁</h3>
              <p class="ban-desc">{{ banReason }}</p>
              <p class="muted small">如有疑问,请联系管理员处理。</p>
              <button class="pix-btn btn-primary btn-block" @click="confirmBan">我已知晓</button>
            </div>
          </div>
        </transition>

        <!-- 管理员测试快捷登录 -->
        <details class="quick">
          <summary>🛠️ 管理员测试账号快捷登录</summary>
          <div class="quick-btns">
            <button
              v-for="t in testAccounts"
              :key="t.username"
              class="pix-btn btn-ghost btn-sm"
              @click="quickLogin(t)"
            >
              {{ t.username }}({{ t.label }})
            </button>
          </div>
        </details>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import http from '../api/http';
import { useSound } from '../soundStore';

const { playSound } = useSound();

const emit = defineEmits(['logged-in']);

const mode = ref('login');
const err = ref('');
const ok = ref('');
const sending = ref(false);
const banVisible = ref(false);
const banReason = ref('');

const username = ref('');
const password = ref('');

const tabs = [
  { id: 'login', label: '登录' },
  { id: 'register', label: '注册' },
  { id: 'forgot', label: '忘记密码' },
];

const questions = [
  '你最喜欢的一道菜是什么?',
  '你的小学班主任姓什么?',
  '你第一次养的宠物叫什么?',
  '你的出生城市是哪里?',
];

const testAccounts = [
  { username: 'admin1', label: '管理员一' },
  { username: 'admin2', label: '管理员二' },
  { username: 'tester1', label: '测试员1' },
  { username: 'tester2', label: '测试员2' },
  { username: 'tester3', label: '测试员3' },
];

const r = ref({ username: '', nickname: '', email: '', code: '', password: '', confirm: '', securityQuestion: questions[0], securityAnswer: '' });
const f = ref({ username: '', question: '', answer: '', newPassword: '' });

async function quickLogin(t) {
  playSound('button');
  err.value = '';
  try {
    const data = await http.post('/api/auth/login', { username: t.username, password: 'admin123' });
    localStorage.setItem('pet_token', data.token);
    localStorage.setItem('pet_user', JSON.stringify(data));
    if (window.petAPI) window.petAPI.setToken(data.token);
    emit('logged-in', data);
  } catch (e) {
    err.value = e.message;
  }
}

async function doLogin() {
  playSound('button');
  err.value = '';
  try {
    const data = await http.post('/api/auth/login', { username: username.value, password: password.value });
    localStorage.setItem('pet_token', data.token);
    localStorage.setItem('pet_user', JSON.stringify(data));
    if (window.petAPI) window.petAPI.setToken(data.token);
    emit('logged-in', data);
  } catch (e) {
    if (e.message.includes('账号已被禁用') || e.message.includes('封禁')) {
      banReason.value = e.message;
      banVisible.value = true;
    } else {
      err.value = e.message;
    }
  }
}

function confirmBan() {
  playSound('button');
  banVisible.value = false;
  banReason.value = '';
  localStorage.removeItem('pet_token');
  localStorage.removeItem('pet_user');
  username.value = '';
  password.value = '';
}

async function sendCode() {
  playSound('button');
  err.value = '';
  ok.value = '';
  if (!r.value.email) {
    err.value = '请先填写邮箱';
    return;
  }
  sending.value = true;
  try {
    await http.post('/api/auth/register-code', { email: r.value.email });
    ok.value = '验证码已发送(未配置邮件服务时,验证码在控制台/后端日志里)';
  } catch (e) {
    err.value = e.message;
  }
  sending.value = false;
}

async function doRegister() {
  playSound('button');
  err.value = '';
  if (r.value.password !== r.value.confirm) {
    err.value = '两次密码不一致';
    return;
  }
  try {
    await http.post('/api/auth/register', {
      username: r.value.username,
      password: r.value.password,
      nickname: r.value.nickname,
      email: r.value.email,
      code: r.value.code,
      securityQuestion: r.value.securityQuestion,
      securityAnswer: r.value.securityAnswer,
    });
    const data = await http.post('/api/auth/login', { username: r.value.username, password: r.value.password });
    localStorage.setItem('pet_token', data.token);
    localStorage.setItem('pet_user', JSON.stringify(data));
    if (window.petAPI) window.petAPI.setToken(data.token);
    emit('logged-in', data);
  } catch (e) {
    err.value = e.message;
  }
}

async function loadQuestion() {
  playSound('button');
  err.value = '';
  f.value.question = '';
  try {
    const q = await http.get('/api/auth/forgot/question', { params: { username: f.value.username } });
    f.value.question = q.securityQuestion;
  } catch (e) {
    err.value = e.message;
  }
}

async function doForgot() {
  playSound('button');
  err.value = '';
  try {
    await http.post('/api/auth/forgot', {
      username: f.value.username,
      answer: f.value.answer,
      newPassword: f.value.newPassword,
    });
    ok.value = '密码已重置,去登录吧';
    f.value = { username: '', question: '', answer: '', newPassword: '' };
    mode.value = 'login';
  } catch (e) {
    err.value = e.message;
  }
}

onMounted(() => {
});
</script>
