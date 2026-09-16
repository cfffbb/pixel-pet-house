<template>
  <FloatingPet v-if="isFloating" />
  <div v-else class="shell">
    <LoginView v-if="!token" @logged-in="onLoggedIn" />
    <template v-else>
      <!-- 管理员默认进独立控制面板(可切回用户端) -->
      <AdminConsole
        v-if="isAdmin && adminView"
        :user="user"
        @go-user="adminView = false"
        @logout="onLogout"
      />
      <MainView v-else :user="user" @logout="onLogout" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import LoginView from './views/LoginView.vue';
import MainView from './views/MainView.vue';
import AdminConsole from './views/AdminConsole.vue';
import FloatingPet from './components/FloatingPet.vue';

const isFloating = computed(
  () => new URLSearchParams(window.location.search).get('floating') === 'true'
);

const token = ref(localStorage.getItem('pet_token'));
const user = ref(JSON.parse(localStorage.getItem('pet_user') || 'null'));
const adminView = ref(true);

const isAdmin = computed(() => user.value && user.value.role === 'ADMIN');

function onLoggedIn(u) {
  token.value = u.token;
  user.value = u;
  adminView.value = true;
}

function onLogout() {
  token.value = null;
  user.value = null;
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  padding: 0;
}
</style>
