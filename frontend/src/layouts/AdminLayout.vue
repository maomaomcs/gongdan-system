<template>
  <el-container class="admin-layout">
    <!-- 桌面侧边栏 -->
    <el-aside v-if="!isMobile" width="210px" class="admin-aside">
      <div class="admin-logo"><span class="dot">石</span> 石室联中 · 报修后台</div>
      <el-menu :default-active="active" background-color="#2a2320" text-color="#cabbac" active-text-color="#fdf6e8" router>
        <el-menu-item index="/admin/tickets"><el-icon><Tickets /></el-icon><span>工单管理</span></el-menu-item>
        <el-menu-item index="/admin/stats"><el-icon><DataLine /></el-icon><span>数据统计</span></el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><UserFilled /></el-icon><span>账号管理</span></el-menu-item>
        <el-menu-item index="/admin/settings"><el-icon><Setting /></el-icon><span>系统设置</span></el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 移动端抽屉菜单 -->
    <el-drawer v-model="drawer" direction="ltr" size="210px" :with-header="false" class="admin-drawer">
      <div class="admin-logo"><span class="dot">石</span> 石室联中 · 报修后台</div>
      <el-menu :default-active="active" background-color="#2a2320" text-color="#cabbac" active-text-color="#fdf6e8" router @select="drawer = false">
        <el-menu-item index="/admin/tickets"><el-icon><Tickets /></el-icon><span>工单管理</span></el-menu-item>
        <el-menu-item index="/admin/stats"><el-icon><DataLine /></el-icon><span>数据统计</span></el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><UserFilled /></el-icon><span>账号管理</span></el-menu-item>
        <el-menu-item index="/admin/settings"><el-icon><Setting /></el-icon><span>系统设置</span></el-menu-item>
      </el-menu>
    </el-drawer>

    <el-container>
      <el-header class="admin-header">
        <div style="display:flex;align-items:center;gap:10px">
          <el-icon v-if="isMobile" style="font-size:22px" @click="drawer = true"><Menu /></el-icon>
          <span style="font-weight:700;color:#7a1519;font-family:'Noto Serif SC','STSong',serif">石室联合中学 · 后勤报修管理</span>
        </div>
        <div style="display:flex;align-items:center;gap:4px">
          <span v-if="!isMobile" style="color:#94a3b8;font-size:14px;margin-right:6px">{{ name }}</span>
          <el-button text size="small" @click="$router.push('/login')">报修入口</el-button>
          <el-button text type="danger" size="small" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="admin-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logout as apiLogout } from '../api'

const route = useRoute()
const router = useRouter()
const active = computed(() => route.path)
const name = localStorage.getItem('admin_name') || '管理员'

const isMobile = ref(window.innerWidth < 768)
const drawer = ref(false)
function onResize() { isMobile.value = window.innerWidth < 768 }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

async function logout() {
  try { await apiLogout() } catch (e) { /* ignore */ }
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_name')
  router.push('/admin/login')
}
</script>
