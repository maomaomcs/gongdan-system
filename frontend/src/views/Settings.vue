<template>
  <div>
    <div class="page-title">系统设置</div>
    <p class="page-desc">新工单钉钉通知</p>

    <el-card style="max-width:720px">
      <template #header>
        <div style="display:flex;align-items:center;gap:8px">
          <span style="font-weight:600">钉钉群机器人通知</span>
          <el-tag v-if="form.enabled" type="success" size="small">已开启</el-tag>
          <el-tag v-else type="info" size="small">未开启</el-tag>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom:18px">
        <div style="font-size:13px;line-height:1.9">
          开启后,每来一条新报修工单,系统会自动把工单内容推送到钉钉群。配置步骤见下方"如何获取"。
        </div>
      </el-alert>

      <el-form :model="form" :label-width="isMobile ? undefined : '130px'" :label-position="isMobile ? 'top' : 'left'">
        <el-form-item label="开启通知">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <el-form-item label="Webhook 地址" required>
          <el-input v-model="form.webhook" type="textarea" :rows="2"
            placeholder="https://oapi.dingtalk.com/robot/send?access_token=xxxxxx" />
        </el-form-item>

        <el-form-item label="安全设置">
          <el-radio-group v-model="secMode">
            <el-radio value="keyword">关键词</el-radio>
            <el-radio value="sign">加签(更安全)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="secMode === 'keyword'" label="关键词">
          <el-input v-model="form.keyword" placeholder="与钉钉机器人自定义关键词一致,如:工单" style="max-width:320px" />
          <div class="tip">机器人设置里填的关键词,消息里必须包含它才能发出。</div>
        </el-form-item>

        <el-form-item v-if="secMode === 'sign'" label="加签 Secret">
          <el-input v-model="form.secret" type="password" show-password
            :placeholder="form.secretSet ? '已设置(留空则不修改)' : '以 SEC 开头的密钥'" style="max-width:420px" />
          <div class="tip">
            钉钉机器人"加签"里的那串 Secret。
            <el-link v-if="form.secretSet" type="danger" :underline="false" @click="clearSecret">清空已存 Secret</el-link>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
          <el-button :loading="testing" @click="test">发送测试通知</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <div style="font-size:13px;color:#64748b;line-height:2">
        <b>如何获取 Webhook:</b><br>
        1. 打开钉钉,进入要接收通知的群 → 右上角"…" → 群设置 → 机器人 → 添加机器人 → 选"自定义"。<br>
        2. "安全设置"里勾选<b>关键词</b>(填个词,如"工单")或<b>加签</b>(复制那串 Secret)。<br>
        3. 复制生成的 <b>Webhook 地址</b>,粘到上面。<br>
        4. 按你选的安全方式填关键词或 Secret,保存后点"发送测试通知"验证。
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDingSettings, saveDingSettings, testDingNotify } from '../api'
import { useMobile } from '../composables/useMobile'

const { isMobile } = useMobile()

const form = reactive({ enabled: false, webhook: '', keyword: '', secret: '', secretSet: false })
const secMode = ref('keyword')
const saving = ref(false)
const testing = ref(false)
const clearSec = ref(false)

async function load() {
  try {
    const d = await getDingSettings()
    form.enabled = d.enabled
    form.webhook = d.webhook || ''
    form.keyword = d.keyword || ''
    form.secretSet = d.secretSet
    form.secret = ''
    secMode.value = d.secretSet ? 'sign' : 'keyword'
  } catch (e) { ElMessage.error(e.message) }
}

function clearSecret() {
  clearSec.value = true
  form.secret = ''
  form.secretSet = false
  ElMessage.info('保存后将清空 Secret')
}

function buildPayload() {
  const p = { enabled: form.enabled, webhook: form.webhook.trim() }
  if (secMode.value === 'keyword') {
    p.keyword = form.keyword.trim()
    if (clearSec.value) p.secret = '__CLEAR__'
  } else {
    p.keyword = ''
    if (clearSec.value) p.secret = '__CLEAR__'
    else if (form.secret) p.secret = form.secret.trim()
  }
  return p
}

async function save() {
  if (form.enabled && !form.webhook.trim()) return ElMessage.warning('请先填写 Webhook 地址')
  saving.value = true
  try {
    const d = await saveDingSettings(buildPayload())
    clearSec.value = false
    form.secretSet = d.secretSet
    form.secret = ''
    ElMessage.success('已保存')
  } catch (e) { ElMessage.error(e.message) }
  finally { saving.value = false }
}

async function test() {
  testing.value = true
  try {
    // 先保存当前配置,再测试,避免"改了没保存就测"
    await saveDingSettings(buildPayload())
    clearSec.value = false
    await testDingNotify()
    ElMessage.success('测试通知已发送,请查看钉钉群')
    load()
  } catch (e) { ElMessage.error(e.message) }
  finally { testing.value = false }
}

onMounted(load)
</script>

<style scoped>
.tip { font-size: 12px; color: #94a3b8; margin-top: 4px; }
</style>
