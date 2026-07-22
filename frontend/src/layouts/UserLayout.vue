<template>
  <div class="user-shell">
    <header class="user-top">
      <div class="user-top-inner">
        <span class="u-logo"><span class="seal">石</span>石室联中 · 后勤报修</span>
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
        <el-badge :value="unread" :hidden="!unread" :max="99" class="tab-badge">
          <span class="ic">📋</span>
        </el-badge>
        <span>我的报修</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userLogout } from '../api'
import { useUnread } from '../composables/useUnread'

const route = useRoute()
const router = useRouter()
const active = computed(() => route.path.includes('/my') ? 'my' : 'report')
const name = localStorage.getItem('user_name') || '我'

const { count: unread, refresh: refreshUnread } = useUnread()
let timer = null
onMounted(() => {
  refreshUnread()
  timer = setInterval(refreshUnread, 60000) // 每分钟刷新一次红点
  window.addEventListener('focus', refreshUnread)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('focus', refreshUnread)
})
watch(() => route.path, () => refreshUnread()) // 切页时刷新

async function logout() {
  try { await userLogout() } catch (e) { /* ignore */ }
  localStorage.removeItem('user_token')
  localStorage.removeItem('user_name')
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<style scoped>
.tab-badge :deep(.el-badge__content) { transform: translateY(-2px) translateX(6px); }
</style>
