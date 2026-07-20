<template>
  <div class="public-wrap" style="justify-content:center">
    <div class="public-card" style="max-width:400px;margin-top:60px">
      <div class="public-header">
        <div class="logo">🔐</div>
        <h1>管理员登录</h1>
        <p>输入密码进入工单后台</p>
      </div>
      <el-card>
        <el-input v-model="username" size="large" placeholder="用户名" :prefix-icon="User"
          @keyup.enter="doLogin" style="margin-bottom:14px" />
        <el-input v-model="password" type="password" size="large" placeholder="密码" :prefix-icon="Lock"
          show-password @keyup.enter="doLogin" />
        <el-button type="primary" size="large" style="width:100%;margin-top:16px" :loading="loading" @click="doLogin">
          登录
        </el-button>
      </el-card>
      <div style="text-align:center;margin-top:16px">
        <el-link type="primary" @click="$router.push('/')">← 返回报修入口</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function doLogin() {
  if (!username.value || !password.value) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const r = await login(username.value.trim(), password.value)
    localStorage.setItem('ticket_token', r.token)
    localStorage.setItem('ticket_user', r.displayName || r.username)
    ElMessage.success('登录成功')
    router.push('/admin/tickets')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>
