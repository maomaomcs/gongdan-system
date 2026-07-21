<template>
  <div>
    <div class="page-title">账号管理</div>
    <p class="page-desc">管理管理员账号、老师报修账号,以及注册邀请码</p>

    <el-tabs v-model="tab">
      <!-- ============ 管理员账号 ============ -->
      <el-tab-pane label="管理员账号" name="admin">
        <el-card style="margin-bottom:16px">
          <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
            <span style="color:var(--ink-2)">共 {{ admins.length }} 个管理员</span>
            <div>
              <el-button @click="pwdDialog = true">修改我的密码</el-button>
              <el-button type="primary" :icon="Plus" @click="openCreate">新增管理员</el-button>
            </div>
          </div>
        </el-card>
        <el-card>
          <el-table :data="admins" v-loading="loadingAdmin" style="width:100%">
            <el-table-column prop="username" label="用户名" min-width="130" />
            <el-table-column prop="displayName" label="显示名" min-width="120" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="!isMobile" prop="createdAt" label="创建时间" width="170" />
            <el-table-column label="操作" width="190">
              <template #default="{ row }">
                <el-switch :model-value="row.enabled" @change="(v) => toggleAdmin(row, v)"
                  inline-prompt active-text="启用" inactive-text="停用" style="margin-right:10px" />
                <el-button size="small" type="danger" plain @click="removeAdmin(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 老师账号 ============ -->
      <el-tab-pane label="老师账号" name="teacher">
        <el-card style="margin-bottom:16px">
          <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
            <span style="color:var(--ink-2)">共 {{ teachers.length }} 个老师账号</span>
            <el-button :icon="Refresh" @click="loadTeachers">刷新</el-button>
          </div>
        </el-card>
        <el-card>
          <el-table :data="teachers" v-loading="loadingTeacher" style="width:100%">
            <el-table-column prop="displayName" label="姓名" min-width="110" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column v-if="!isMobile" prop="phone" label="联系方式" min-width="120" />
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">{{ row.enabled ? '启用' : '停用' }}</el-tag>
                <el-tag v-if="row.isAdmin" type="warning" effect="light" style="margin-left:6px">管理员</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" :width="isMobile ? 150 : 320">
              <template #default="{ row }">
                <div style="display:flex;flex-wrap:wrap;gap:6px;align-items:center">
                  <el-switch :model-value="row.enabled" @change="(v) => toggleTeacher(row, v)"
                    inline-prompt active-text="启用" inactive-text="停用" />
                  <el-button size="small" @click="openReset(row)">重置密码</el-button>
                  <el-button size="small" type="warning" plain :disabled="row.isAdmin" @click="promote(row)">
                    {{ row.isAdmin ? '已是管理员' : '设为管理员' }}
                  </el-button>
                  <el-button size="small" type="danger" plain @click="removeTeacher(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingTeacher && !teachers.length" description="还没有老师注册" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 邀请码 ============ -->
      <el-tab-pane label="注册邀请码" name="invite">
        <el-card style="margin-bottom:16px">
          <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
            <span style="color:var(--ink-2)">老师注册时需填写有效邀请码</span>
            <el-button type="primary" :icon="Plus" @click="genDialog = true">生成邀请码</el-button>
          </div>
        </el-card>
        <el-card>
          <el-table :data="codes" v-loading="loadingCode" style="width:100%">
            <el-table-column label="邀请码" min-width="130">
              <template #default="{ row }">
                <span style="font-family:ui-monospace,Menlo,monospace;font-weight:700;letter-spacing:1px;color:var(--brand)">{{ row.code }}</span>
                <el-button link :icon="CopyDocument" @click="copyCode(row.code)" style="margin-left:4px" />
              </template>
            </el-table-column>
            <el-table-column v-if="!isMobile" prop="note" label="备注" min-width="140">
              <template #default="{ row }">{{ row.note || '—' }}</template>
            </el-table-column>
            <el-table-column label="使用情况" width="120">
              <template #default="{ row }">
                {{ row.usedCount }} / {{ row.maxUses > 0 ? row.maxUses : '不限' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170">
              <template #default="{ row }">
                <el-switch :model-value="row.enabled" @change="(v) => toggleCode(row, v)"
                  inline-prompt active-text="启用" inactive-text="停用" style="margin-right:10px" />
                <el-button size="small" type="danger" plain @click="removeCode(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingCode && !codes.length" description="还没有邀请码,点右上角生成" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增管理员 -->
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

    <!-- 重置老师密码 -->
    <el-dialog v-model="resetDialog" :title="'重置密码 · ' + (resetTarget?.displayName || '')" :width="isMobile ? '92%' : '440px'">
      <el-form label-position="top">
        <el-form-item label="新密码" required>
          <el-input v-model="resetPwd" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <div class="u-tip">重置后该老师需用新密码重新登录。</div>
      </el-form>
      <template #footer>
        <el-button @click="resetDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doReset">确定</el-button>
      </template>
    </el-dialog>

    <!-- 生成邀请码 -->
    <el-dialog v-model="genDialog" title="生成邀请码" :width="isMobile ? '92%' : '440px'">
      <el-form :model="genForm" label-position="top">
        <el-form-item label="备注(选填)">
          <el-input v-model="genForm.note" placeholder="如:2026级新老师" />
        </el-form-item>
        <el-form-item label="可用次数">
          <el-input-number v-model="genForm.maxUses" :min="0" :max="9999" />
          <span class="u-tip" style="margin-left:10px">0 表示不限次数</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doGen">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, CopyDocument } from '@element-plus/icons-vue'
import {
  listUsers, createUser, setUserEnabled, deleteUser, changePassword,
  listAppUsers, setAppUserEnabled, resetAppUserPassword, promoteAppUser, deleteAppUser,
  listInviteCodes, createInviteCode, setInviteCodeEnabled, deleteInviteCode,
} from '../api'
import { useMobile } from '../composables/useMobile'

const { isMobile } = useMobile()

const tab = ref('admin')
const saving = ref(false)

/* ---- 管理员 ---- */
const admins = ref([])
const loadingAdmin = ref(false)
const createDialog = ref(false)
const pwdDialog = ref(false)
const form = reactive({ username: '', displayName: '', password: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

async function loadAdmins() {
  loadingAdmin.value = true
  try { admins.value = await listUsers() }
  catch (e) { ElMessage.error(e.message) }
  finally { loadingAdmin.value = false }
}
function openCreate() { form.username = ''; form.displayName = ''; form.password = ''; createDialog.value = true }
async function doCreate() {
  if (!form.username || !form.password) return ElMessage.warning('用户名和密码必填')
  saving.value = true
  try {
    await createUser({ username: form.username.trim(), displayName: form.displayName.trim(), password: form.password })
    ElMessage.success('账号已创建'); createDialog.value = false; loadAdmins()
  } catch (e) { ElMessage.error(e.message) } finally { saving.value = false }
}
async function toggleAdmin(row, v) {
  try { await setUserEnabled(row.id, v); row.enabled = v; ElMessage.success('已' + (v ? '启用' : '停用')) }
  catch (e) { ElMessage.error(e.message); loadAdmins() }
}
async function removeAdmin(row) {
  try {
    await ElMessageBox.confirm(`确定删除管理员「${row.username}」?`, '提示', { type: 'warning' })
    await deleteUser(row.id); ElMessage.success('已删除'); loadAdmins()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.message) }
}
async function doChangePwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) return ElMessage.warning('请填写完整')
  saving.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改'); pwdDialog.value = false
    pwdForm.oldPassword = ''; pwdForm.newPassword = ''
  } catch (e) { ElMessage.error(e.message) } finally { saving.value = false }
}

/* ---- 老师账号 ---- */
const teachers = ref([])
const loadingTeacher = ref(false)
const resetDialog = ref(false)
const resetTarget = ref(null)
const resetPwd = ref('')

async function loadTeachers() {
  loadingTeacher.value = true
  try { teachers.value = await listAppUsers() }
  catch (e) { ElMessage.error(e.message) }
  finally { loadingTeacher.value = false }
}
async function toggleTeacher(row, v) {
  try { await setAppUserEnabled(row.id, v); row.enabled = v; ElMessage.success('已' + (v ? '启用' : '停用')) }
  catch (e) { ElMessage.error(e.message); loadTeachers() }
}
function openReset(row) { resetTarget.value = row; resetPwd.value = ''; resetDialog.value = true }
async function doReset() {
  if (!resetPwd.value || resetPwd.value.length < 6) return ElMessage.warning('新密码至少6位')
  saving.value = true
  try {
    await resetAppUserPassword(resetTarget.value.id, resetPwd.value)
    ElMessage.success('密码已重置'); resetDialog.value = false
  } catch (e) { ElMessage.error(e.message) } finally { saving.value = false }
}
async function promote(row) {
  try {
    await ElMessageBox.confirm(`将「${row.displayName || row.username}」设为管理员?\n对方将可用当前用户名和密码登录管理后台。`, '设为管理员', { type: 'warning' })
    await promoteAppUser(row.id); ElMessage.success('已设为管理员'); loadTeachers(); loadAdmins()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.message) }
}
async function removeTeacher(row) {
  try {
    await ElMessageBox.confirm(`确定删除老师账号「${row.displayName || row.username}」?其报修记录会保留。`, '提示', { type: 'warning' })
    await deleteAppUser(row.id); ElMessage.success('已删除'); loadTeachers()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.message) }
}

/* ---- 邀请码 ---- */
const codes = ref([])
const loadingCode = ref(false)
const genDialog = ref(false)
const genForm = reactive({ note: '', maxUses: 0 })

async function loadCodes() {
  loadingCode.value = true
  try { codes.value = await listInviteCodes() }
  catch (e) { ElMessage.error(e.message) }
  finally { loadingCode.value = false }
}
async function doGen() {
  saving.value = true
  try {
    await createInviteCode({ note: genForm.note.trim(), maxUses: genForm.maxUses })
    ElMessage.success('邀请码已生成'); genDialog.value = false
    genForm.note = ''; genForm.maxUses = 0; loadCodes()
  } catch (e) { ElMessage.error(e.message) } finally { saving.value = false }
}
async function toggleCode(row, v) {
  try { await setInviteCodeEnabled(row.id, v); row.enabled = v; ElMessage.success('已' + (v ? '启用' : '停用')) }
  catch (e) { ElMessage.error(e.message); loadCodes() }
}
async function removeCode(row) {
  try {
    await ElMessageBox.confirm(`确定删除邀请码「${row.code}」?`, '提示', { type: 'warning' })
    await deleteInviteCode(row.id); ElMessage.success('已删除'); loadCodes()
  } catch (e) { if (e !== 'cancel') ElMessage.error(e.message) }
}
async function copyCode(code) {
  try { await navigator.clipboard.writeText(code); ElMessage.success('已复制:' + code) }
  catch (e) { ElMessage.warning('复制失败,请手动复制') }
}

onMounted(() => { loadAdmins(); loadTeachers(); loadCodes() })
</script>
