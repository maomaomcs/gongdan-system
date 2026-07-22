<template>
  <div class="u-page">
    <h2 class="u-title">我的报修</h2>

    <div class="my-filter">
      <el-select v-model="status" placeholder="全部状态" clearable size="large" style="flex:1" @change="search">
        <el-option label="待处理" value="待处理" />
        <el-option label="处理中" value="处理中" />
        <el-option label="已解决" value="已解决" />
        <el-option label="已关闭" value="已关闭" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索标题/位置/工单号" clearable size="large" style="flex:2"
        @keyup.enter="search" @clear="search" />
    </div>

    <el-empty v-if="!loading && !list.length" description="没有符合条件的报修" />

    <div v-loading="loading">
      <el-card v-for="t in list" :key="t.id" class="ticket-item" shadow="never" @click="open(t)">
        <div class="ti-head">
          <span class="ti-title">
            <span v-if="t.userUnread" class="unread-dot" title="有更新"></span>{{ t.title }}
          </span>
          <div style="display:flex;align-items:center;gap:6px">
            <el-tag v-if="t.userUnread" type="danger" size="small" effect="dark">有更新</el-tag>
            <el-tag :type="statusType(t.status)" size="small" effect="light">{{ t.status }}</el-tag>
          </div>
        </div>
        <div class="ti-meta">
          <span>{{ t.category }}</span>
          <span v-if="t.urgency === '紧急'" class="urgent">🔴 紧急</span>
        </div>
        <div class="ti-sub">
          <span>{{ t.location }}</span>
          <span class="ti-code">{{ t.code }}</span>
        </div>
        <div style="display:flex;align-items:center;justify-content:space-between;margin-top:6px;gap:8px">
          <span class="ti-time">{{ t.createdAt }}</span>
          <div style="display:flex;gap:8px">
            <el-button v-if="t.status === '待处理'" size="small" plain :loading="canceling === t.id"
              @click.stop="cancel(t)">取消报修</el-button>
            <el-button v-if="canUrge(t)" size="small" type="warning" plain :loading="urging === t.id"
              @click.stop="urge(t)">催一下{{ t.urgeCount > 0 ? '(已催' + t.urgeCount + ')' : '' }}</el-button>
            <span v-else-if="t.urgeCount > 0" style="font-size:12px;color:#b8863b;align-self:center">已催 {{ t.urgeCount }} 次</span>
          </div>
        </div>
      </el-card>

      <div v-if="total > size" style="display:flex;justify-content:center;margin-top:12px">
        <el-pagination background layout="prev, pager, next" :total="total"
          :current-page="page" :page-size="size" @current-change="onPage" />
      </div>
    </div>

    <!-- 详情 -->
    <el-dialog v-model="show" :title="cur?.title" width="92%" style="max-width:520px">
      <template v-if="cur">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="工单号">{{ cur.code }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusType(cur.status)" size="small">{{ cur.status }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="位置">{{ cur.location }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ cur.category }}</el-descriptions-item>
          <el-descriptions-item label="紧急度">{{ cur.urgency }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ cur.createdAt }}</el-descriptions-item>
          <el-descriptions-item v-if="cur.description" label="详情">{{ cur.description }}</el-descriptions-item>
          <el-descriptions-item v-if="cur.handler" label="处理人">{{ cur.handler }}</el-descriptions-item>
          <el-descriptions-item v-if="cur.resolution" label="解决方案">{{ cur.resolution }}</el-descriptions-item>
          <el-descriptions-item v-if="cur.images && cur.images.length" label="照片">
            <el-image v-for="n in cur.images" :key="n" :src="'/api/files/' + n"
              :preview-src-list="cur.images.map(x => '/api/files/' + x)" fit="cover"
              style="width:66px;height:66px;border-radius:8px;margin:2px" />
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="cur.logs && cur.logs.length" style="margin-top:14px">
          <h4 style="margin:0 0 8px">处理进度</h4>
          <el-timeline>
            <el-timeline-item v-for="l in cur.logs" :key="l.id" :timestamp="l.createdAt" placement="top">
              {{ l.content }} <span v-if="l.author" style="color:#94a3b8">— {{ l.author }}</span>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ElMessageBox } from 'element-plus'
import { getMyTickets, getMyTicket, urgeTicket, cancelTicket } from '../../api'
import { useUnread } from '../../composables/useUnread'

const { refresh: refreshUnread } = useUnread()

const list = ref([])
const loading = ref(false)
const urging = ref(null)
const canceling = ref(null)
const show = ref(false)
const cur = ref(null)
const status = ref('')
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

function statusType(s) {
  return { 待处理: 'warning', 处理中: 'primary', 已解决: 'success', 已关闭: 'info', 已取消: 'info' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value }
    if (status.value) params.status = status.value
    if (keyword.value.trim()) params.q = keyword.value.trim()
    const res = await getMyTickets(params)
    list.value = res.list
    total.value = res.total
  }
  catch (e) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function search() { page.value = 1; load() }
function onPage(p) { page.value = p; load() }
function reload() { load(); refreshUnread() }

async function open(t) {
  try {
    cur.value = await getMyTicket(t.id)
    show.value = true
    if (t.userUnread) { t.userUnread = false; refreshUnread() } // 查看后清除红点
  }
  catch (e) { ElMessage.error(e.message) }
}

function canUrge(t) {
  return t.status === '待处理' || t.status === '处理中'
}
async function urge(t) {
  urging.value = t.id
  try {
    const updated = await urgeTicket(t.id)
    t.urgeCount = updated.urgeCount
    ElMessage.success('已催单,后勤会尽快处理')
  } catch (e) { ElMessage.error(e.message) }
  finally { urging.value = null }
}

async function cancel(t) {
  try {
    await ElMessageBox.confirm('确定取消这条报修吗?(仅"待处理"的报修可取消)', '取消报修', {
      type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想',
    })
  } catch (e) { return }
  canceling.value = t.id
  try {
    const updated = await cancelTicket(t.id)
    t.status = updated.status
    ElMessage.success('已取消报修')
  } catch (e) { ElMessage.error(e.message) }
  finally { canceling.value = null }
}

onMounted(reload)
</script>

<style scoped>
.unread-dot {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%;
  background: #f5222d; margin-right: 6px; vertical-align: middle;
}
</style>
