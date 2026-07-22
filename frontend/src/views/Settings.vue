<template>
  <div>
    <div class="page-title">系统设置</div>
    <p class="page-desc">报修选项 · 新工单钉钉通知</p>

    <!-- 报修选项:故障类型 / 常用位置 -->
    <el-card style="max-width:720px;margin-bottom:16px">
      <template #header>
        <span style="font-weight:600">报修选项</span>
      </template>
      <el-form :label-width="isMobile ? undefined : '130px'" :label-position="isMobile ? 'top' : 'left'">
        <el-form-item label="故障类型">
          <div class="tag-box">
            <el-tag v-for="(c, i) in categories" :key="'c' + i" closable @close="categories.splice(i, 1)" class="opt-tag">{{ c }}</el-tag>
            <el-input v-if="catShow" v-model="catVal" size="small" style="width:140px"
              @keyup.enter="addCat" @blur="addCat" placeholder="回车添加" />
            <el-button v-else size="small" :icon="Plus" @click="catShow = true">新增类型</el-button>
          </div>
          <div class="tip">老师报修时的"故障类型"下拉选项。</div>
        </el-form-item>
        <el-form-item label="常用位置">
          <div class="tag-box">
            <el-tag v-for="(l, i) in locations" :key="'l' + i" type="info" closable @close="locations.splice(i, 1)" class="opt-tag">{{ l }}</el-tag>
            <el-input v-if="locShow" v-model="locVal" size="small" style="width:160px"
              @keyup.enter="addLoc" @blur="addLoc" placeholder="回车添加" />
            <el-button v-else size="small" :icon="Plus" @click="locShow = true">新增位置</el-button>
          </div>
          <div class="tip">老师报修时可从这里快速选择位置,也允许自行输入其它位置。</div>
        </el-form-item>

        <el-form-item label="超时预警(小时)">
          <el-input-number v-model="overdueHours" :min="1" :max="720" />
          <span class="tip" style="margin-left:10px">待处理/处理中的工单超过此时长,在后台工单列表会标红提示。</span>
        </el-form-item>

        <el-form-item label="常见问题自助">
          <div style="width:100%">
            <div v-for="(f, i) in faqs" :key="i" class="faq-row">
              <el-input v-model="f.q" placeholder="问题,如:投影仪没信号怎么办?" style="margin-bottom:6px" />
              <div style="display:flex;gap:8px;align-items:flex-start">
                <el-input v-model="f.a" type="textarea" :rows="2" placeholder="处理办法,如:先检查 HDMI 线是否插好、投影仪信号源是否选对" />
                <el-button type="danger" plain :icon="Delete" @click="faqs.splice(i, 1)" />
              </div>
            </div>
            <el-button size="small" :icon="Plus" @click="faqs.push({ q: '', a: '' })" style="margin-top:6px">新增一条</el-button>
            <div class="tip">老师报修前会先看到这些问答,能自己解决就不用报修了。</div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="savingOpt" @click="saveOpt">保存选项</el-button>
        </el-form-item>
      </el-form>
    </el-card>

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

        <el-form-item label="群内按钮地址">
          <el-input v-model="form.actionBase" placeholder="如:http://43.136.56.131:8082(留空则群消息不带按钮)" />
          <div class="tip">填公网可访问的服务器地址后,新工单钉钉卡片会带【认领/已解决/取消】按钮,群里点一下即可同步平台。</div>
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
import { Plus, Delete } from '@element-plus/icons-vue'
import { getDingSettings, saveDingSettings, testDingNotify, getOptions, saveOptions } from '../api'
import { useMobile } from '../composables/useMobile'

const { isMobile } = useMobile()

/* ---- 报修选项 ---- */
const categories = ref([])
const locations = ref([])
const faqs = ref([])
const overdueHours = ref(24)
const catShow = ref(false); const catVal = ref('')
const locShow = ref(false); const locVal = ref('')
const savingOpt = ref(false)

function addCat() {
  const v = catVal.value.trim()
  if (v && !categories.value.includes(v)) categories.value.push(v)
  catVal.value = ''; catShow.value = false
}
function addLoc() {
  const v = locVal.value.trim()
  if (v && !locations.value.includes(v)) locations.value.push(v)
  locVal.value = ''; locShow.value = false
}
async function loadOptions() {
  try {
    const d = await getOptions()
    categories.value = d.categories || []
    locations.value = d.locations || []
    faqs.value = d.faqs || []
    overdueHours.value = d.overdueHours || 24
  } catch (e) { ElMessage.error(e.message) }
}
async function saveOpt() {
  if (!categories.value.length) return ElMessage.warning('至少保留一个故障类型')
  const cleanFaqs = faqs.value.filter(f => (f.q || '').trim())
  savingOpt.value = true
  try {
    const d = await saveOptions({
      categories: categories.value,
      locations: locations.value,
      faqs: cleanFaqs,
      overdueHours: overdueHours.value,
    })
    categories.value = d.categories || []
    locations.value = d.locations || []
    faqs.value = d.faqs || []
    overdueHours.value = d.overdueHours || 24
    ElMessage.success('报修选项已保存')
  } catch (e) { ElMessage.error(e.message) }
  finally { savingOpt.value = false }
}

const form = reactive({ enabled: false, webhook: '', keyword: '', secret: '', secretSet: false, actionBase: '' })
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
    form.actionBase = d.actionBase || ''
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
  const p = { enabled: form.enabled, webhook: form.webhook.trim(), actionBase: form.actionBase.trim() }
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

onMounted(() => { load(); loadOptions() })
</script>

<style scoped>
.tip { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.tag-box { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.opt-tag { }
.faq-row { padding: 10px; border: 1px solid var(--line, #e3d8c3); border-radius: 8px; margin-bottom: 10px; }
</style>
