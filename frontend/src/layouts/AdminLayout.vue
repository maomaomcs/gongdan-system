<template>
  <el-container class="admin-layout">
    <el-aside width="210px" class="admin-aside">
      <div class="admin-logo">
        <span class="dot">📋</span> 工单管理系统
      </div>
      <el-menu :default-active="active" background-color="#1e2233" text-color="#cbd5e1"
        active-text-color="#fff" router>
        <el-menu-item index="/admin/tickets">
          <el-icon><Tickets /></el-icon><span>工单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/stats">
          <el-icon><DataLine /></el-icon><span>数据统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="background:#fff;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #eef0f4">
        <div style="font-weight:600;color:#475569">校园报障工单系统</div>
        <div>
          <el-button text @click="$router.push('/')">报修入口</el-button>
          <el-button text type="danger" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const active = computed(() => route.path)

function logout() {
  localStorage.removeItem('ticket_token')
  router.push('/login')
}
</script>
