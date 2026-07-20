<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <div class="auth-header">
        <div class="logo">🛠️</div>
        <h1>注册账号</h1>
        <p>注册后报修记录会保存在你的账号下</p>
      </div>
      <el-card>
        <el-form :model="form" label-position="top">
          <el-form-item label="用户名(登录用)" required>
            <el-input v-model="form.username" placeholder="3~64位,建议用工号或拼音" />
          </el-form-item>
          <el-form-item label="姓名" required>
            <el-input v-model="form.displayName" placeholder="如:张老师(报修时显示)" />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model="form.phone" placeholder="手机号(选填,方便联系)" />
          </el-form-item>
          <el-form-item label="密码" required>
            <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doRegister">注册并登录</el-button>
          <div style="text-align:center;margin-top:14px">
            已有账号?<el-link type="primary" @click="$router.push('/login')">去登录</el-link>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userRegister } from '../../api'

const router = useRouter()
const form = reactive({ username: '', displayName: '', phone: '', password: '' })
const loading = ref(false)

async function doRegister() {
  if (!form.username || !form.password || !form.displayName) return ElMessage.warning('用户名、姓名、密码必填')
  loading.value = true
  try {
    const r = await userRegister({
      username: form.username.trim(), password: form.password,
      displayName: form.displayName.trim(), phone: form.phone.trim(),
    })
    localStorage.setItem('user_token', r.token)
    localStorage.setItem('user_name', r.displayName || r.username)
    ElMessage.success('注册成功')
    router.push('/report')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>
