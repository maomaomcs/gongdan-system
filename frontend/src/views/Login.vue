<template>
  <div class="auth-wrap">
    <div class="auth-card" style="max-width:400px">
      <div class="auth-header">
        <div class="logo">石</div>
        <h1>石室联中132 · 报修后台</h1>
        <p>后勤报修管理 · 管理员登录</p>
        <span class="motto">爱国利民</span>
      </div>
      <el-card>
        <el-input v-model="username" size="large" placeholder="用户名" :prefix-icon="User" style="margin-bottom:14px" @keyup.enter="doLogin" />
        <el-input v-model="password" type="password" size="large" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="doLogin" />
        <el-checkbox v-model="remember" style="margin-top:12px">记住密码</el-checkbox>
        <el-button type="primary" size="large" style="width:100%;margin-top:12px" :loading="loading" @click="doLogin">登录</el-button>
      </el-card>
      <div style="text-align:center;margin-top:16px">
        <el-link @click="$router.push('/login')" style="color:#94a3b8;font-size:13px">← 返回老师报修入口</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { adminLogin } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const remember = ref(false)

const SAVE_KEY = 'saved_admin_login'

onMounted(() => {
  try {
    const raw = localStorage.getItem(SAVE_KEY)
    if (raw) {
      const { u, p } = JSON.parse(decodeURIComponent(atob(raw)))
      username.value = u || ''
      password.value = p || ''
      remember.value = true
    }
  } catch (e) { /* ignore */ }
})

async function doLogin() {
  if (!username.value || !password.value) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const r = await adminLogin(username.value.trim(), password.value)
    localStorage.setItem('admin_token', r.token)
    localStorage.setItem('admin_name', r.displayName || r.username)
    if (remember.value) {
      localStorage.setItem(SAVE_KEY, btoa(encodeURIComponent(JSON.stringify({ u: username.value.trim(), p: password.value }))))
    } else {
      localStorage.removeItem(SAVE_KEY)
    }
    ElMessage.success('登录成功')
    router.push('/admin/tickets')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>
