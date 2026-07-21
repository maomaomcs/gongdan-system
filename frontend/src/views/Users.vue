<template>
  <div>
    <div class="page-title">账号管理</div>
    <p class="page-desc">管理登录后台的管理员账号</p>

    <el-card style="margin-bottom:16px">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="color:#64748b">共 {{ users.length }} 个账号</span>
        <div>
          <el-button @click="pwdDialog = true">修改我的密码</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增账号</el-button>
        </div>
      </div>
    </el-card>

    <el-card>
      <el-table :data="users" v-loading="loading" style="width:100%">
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="displayName" label="显示名" min-width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="!isMobile" prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="(v) => toggle(row, v)"
              inline-prompt active-text="启用" inactive-text="停用" style="margin-right:10px" />
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增账号 -->
    <el-dialog v-model="createDialog" title="新增管理员账号" :width="isMobile ? '92%' : '440px'">
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名(登录用)" required>
          <el-input v-model="form.username" placeholder="3~64位,字母/数字" />
        </el-form-item>
        <el-form-item label="显示名(记录处理人时显示)">
          <el-input v-model="form.displayName" placeholder="如:王工" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 修改自己的密码 -->
    <el-dialog v-model="pwdDialog" title="修改我的密码" :width="isMobile ? '92%' : '440px'">
      <el-form :model="pwdForm" label-position="top">
        <el-form-item label="原密码" required>
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doChangePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listUsers, createUser, setUserEnabled, deleteUser, changePassword } from '../api'
import { useMobile } from '../composables/useMobile'

const { isMobile } = useMobile()

const users = ref([])
const loading = ref(false)
const saving = ref(false)
const createDialog = ref(false)
const pwdDialog = ref(false)
const form = reactive({ username: '', displayName: '', password: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

async function load() {
  loading.value = true
  try { users.value = await listUsers() }
  catch (e) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function openCreate() {
  form.username = ''; form.displayName = ''; form.password = ''
  createDialog.value = true
}

async function doCreate() {
  if (!form.username || !form.password) return ElMessage.warning('用户名和密码必填')
  saving.value = true
  try {
    await createUser({ username: form.username.trim(), displayName: form.displayName.trim(), password: form.password })
    ElMessage.success('账号已创建')
    createDialog.value = false
    load()
  } catch (e) { ElMessage.error(e.message) }
  finally { saving.value = false }
}

async function toggle(row, v) {
  try { await setUserEnabled(row.id, v); row.enabled = v; ElMessage.success('已' + (v ? '启用' : '停用')) }
  catch (e) { ElMessage.error(e.message); load() }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除账号「${row.username}」?`, '提示', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.message) }
}

async function doChangePwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) return ElMessage.warning('请填写完整')
  saving.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改')
    pwdDialog.value = false
    pwdForm.oldPassword = ''; pwdForm.newPassword = ''
  } catch (e) { ElMessage.error(e.message) }
  finally { saving.value = false }
}

onMounted(load)
</script>
