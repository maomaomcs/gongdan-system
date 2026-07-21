<template>
  <div>
    <div class="page-title">工单管理</div>
    <p class="page-desc">查看、筛选、跟进所有报修工单</p>

    <el-card style="margin-bottom:16px">
      <div style="display:flex;flex-wrap:wrap;gap:12px;align-items:center">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px" @change="search">
          <el-option v-for="s in cfg.statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="filters.category" placeholder="全部类型" clearable style="width:150px" @change="search">
          <el-option v-for="c in cfg.categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filters.urgency" placeholder="全部紧急度" clearable style="width:130px" @change="search">
          <el-option label="紧急" value="紧急" />
          <el-option label="普通" value="普通" />
        </el-select>
        <el-input v-model="filters.q" placeholder="搜索 标题/位置/报修人/工单号" clearable style="width:260px"
          @keyup.enter="search" @clear="search" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button type="success" :icon="Download" :loading="exporting" @click="doExport">导出 Excel</el-button>
      </div>
    </el-card>

    <el-card>
      <el-table :data="list" v-loading="loading" style="width:100%" :row-class-name="rowClass" @row-click="openDetail">
        <el-table-column prop="code" label="工单号" width="150">
          <template #default="{ row }"><span style="font-family:monospace;color:#64748b">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" effect="light">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="标记" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.urgency==='紧急'" type="danger" size="small" style="margin-right:4px">急</el-tag>
            <el-tag v-if="row.overdue" type="danger" effect="dark" size="small" style="margin-right:4px">超时</el-tag>
            <el-tag v-if="row.urgeCount > 0" type="warning" size="small">催{{ row.urgeCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="问题" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="!isMobile" prop="location" label="位置" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="!isMobile" prop="category" label="类型" width="130" />
        <el-table-column v-if="!isMobile" prop="reporter" label="报修人" width="90" />
        <el-table-column prop="createdAt" label="提交时间" :width="isMobile ? 120 : 160">
          <template #default="{ row }">{{ isMobile ? row.createdAt.slice(5, 16) : row.createdAt }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无工单" />
      <div v-if="total > 0" style="display:flex;justify-content:flex-end;margin-top:14px">
        <el-pagination
          background layout="total, sizes, prev, pager, next"
          :total="total" :current-page="page" :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="onPage" @size-change="onSize" />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawer" :title="current?.title" :size="isMobile ? '100%' : '480px'">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="工单号">{{ current.code }}</el-descriptions-item>
          <el-descriptions-item label="报修人">{{ current.reporter }} <span v-if="current.contact">· {{ current.contact }}</span></el-descriptions-item>
          <el-descriptions-item label="位置">{{ current.location }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ current.category }}</el-descriptions-item>
          <el-descriptions-item label="紧急度">
            <el-tag v-if="current.urgency==='紧急'" type="danger" size="small">紧急</el-tag><span v-else>普通</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ current.createdAt }}</el-descriptions-item>
          <el-descriptions-item v-if="current.description" label="详情">{{ current.description }}</el-descriptions-item>
          <el-descriptions-item v-if="current.images && current.images.length" label="照片">
            <el-image v-for="n in current.images" :key="n" :src="'/api/files/' + n"
              :preview-src-list="current.images.map(x => '/api/files/' + x)" fit="cover"
              style="width:76px;height:76px;border-radius:8px;margin:2px" />
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>处理</el-divider>
        <el-form label-position="top">
          <el-form-item label="状态">
            <el-select v-model="edit.status" style="width:100%">
              <el-option v-for="s in cfg.statuses" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item label="处理人">
            <el-input v-model="edit.handler" placeholder="谁在处理" />
          </el-form-item>
          <el-form-item label="解决方案 / 备注">
            <el-input v-model="edit.resolution" type="textarea" :rows="3" placeholder="最终如何解决的" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存处理结果</el-button>
        </el-form>

        <el-divider>跟进记录</el-divider>
        <el-timeline v-if="current.logs && current.logs.length">
          <el-timeline-item v-for="l in current.logs" :key="l.id" :timestamp="l.createdAt" placement="top">
            {{ l.content }} <span v-if="l.author" style="color:#94a3b8">— {{ l.author }}</span>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无跟进记录" :image-size="60" />
        <div style="display:flex;gap:8px;margin-top:10px">
          <el-input v-model="newLog" placeholder="添加一条跟进,如:已联系厂商" @keyup.enter="addLogItem" />
          <el-button type="primary" @click="addLogItem">添加</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getConfig, listTickets, getTicket, updateTicket, addLog, exportTicketsExcel } from '../api'
import { useMobile } from '../composables/useMobile'

const { isMobile } = useMobile()

const cfg = reactive({ statuses: [], categories: [] })
const filters = reactive({ status: '', category: '', urgency: '', q: '' })
const list = ref([])
const loading = ref(false)
const exporting = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const drawer = ref(false)
const current = ref(null)
const edit = reactive({ status: '', handler: '', resolution: '' })
const saving = ref(false)
const newLog = ref('')

function statusType(s) {
  return { 待处理: 'warning', 处理中: 'primary', 已解决: 'success', 已关闭: 'info' }[s] || 'info'
}
function rowClass({ row }) { return row.overdue ? 'overdue-row' : '' }

async function load() {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value }
    for (const k of ['status', 'category', 'urgency', 'q']) if (filters[k]) params[k] = filters[k]
    const res = await listTickets(params)
    list.value = res.list
    total.value = res.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function search() { page.value = 1; load() }
function onPage(p) { page.value = p; load() }
function onSize(s) { size.value = s; page.value = 1; load() }

async function doExport() {
  exporting.value = true
  try {
    const params = {}
    for (const k of ['status', 'category', 'urgency', 'q']) if (filters[k]) params[k] = filters[k]
    await exportTicketsExcel(params)
    ElMessage.success('已导出')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    exporting.value = false
  }
}

async function openDetail(row) {
  try {
    current.value = await getTicket(row.id)
    edit.status = current.value.status
    edit.handler = current.value.handler || ''
    edit.resolution = current.value.resolution || ''
    drawer.value = true
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function save() {
  saving.value = true
  try {
    current.value = await updateTicket(current.value.id, { ...edit })
    ElMessage.success('已保存')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}

async function addLogItem() {
  if (!newLog.value.trim()) return
  try {
    current.value = await addLog(current.value.id, { content: newLog.value.trim(), author: edit.handler })
    newLog.value = ''
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    const c = await getConfig()
    cfg.statuses = c.statuses
    cfg.categories = c.categories
  } catch (e) { /* ignore */ }
  load()
})
</script>

<style scoped>
:deep(.overdue-row td) { background: #fef2f2 !important; }
:deep(.overdue-row:hover td) { background: #fde8e8 !important; }
</style>
