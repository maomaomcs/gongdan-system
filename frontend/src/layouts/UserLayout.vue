<template>
  <div class="user-shell">
    <header class="user-top">
      <div class="user-top-inner">
        <span class="u-logo">🛠️ 设备报修</span>
        <span class="u-name" @click="logout">{{ name }} · 退出</span>
      </div>
    </header>

    <main class="user-main">
      <router-view />
    </main>

    <nav class="user-tabbar">
      <router-link to="/report" class="tab" :class="{ on: active === 'report' }">
        <span class="ic">📝</span><span>我要报修</span>
      </router-link>
      <router-link to="/my" class="tab" :class="{ on: active === 'my' }">
        <span class="ic">📋</span><span>我的报修</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userLogout } from '../api'

const route = useRoute()
const router = useRouter()
const active = computed(() => route.path.includes('/my') ? 'my' : 'report')
const name = localStorage.getItem('user_name') || '我'

async function logout() {
  try { await userLogout() } catch (e) { /* ignore */ }
  localStorage.removeItem('user_token')
  localStorage.removeItem('user_name')
  ElMessage.success('已退出')
  router.push('/login')
}
</script>
