<template>
  <div>
    <div class="page-title">工单管理</div>
    <p class="page-desc">查看、筛选、跟进所有报修工单</p>

    <el-card style="margin-bottom:16px">
      <div style="display:flex;flex-wrap:wrap;gap:12px;align-items:center">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px" @change="load">
          <el-option v-for="s in cfg.statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="filters.category" placeholder="全部类型" clearable style="width:150px" @change="load">
          <el-option v-for="c in cfg.categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filters.urgency" placeholder="全部紧急度" clearable style="width:130px" @change="load">
          <el-option label="紧急" value="紧急" />
          <el-option label="普通" value="普通" />
        </el-select>
        <el-input v-model="filters.q" placeholder="搜索 标题/位置/报修人/工单号" clearable style="width:260px"
          @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
    </el-card>

    <el-card>
      <el-table :data="list" v-loading="loading" style="width:100%" @row-click="openDetail">
        <el-table-column prop="code" label="工单号" width="150">
          <template #default="{ row }"><span style="font-family:monospace;color:#64748b">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" effect="light">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="紧急" width="70">
          <template #default="{ row }"><el-tag v-if="row.urgency==='紧急'" type="danger" size="small">急</el-tag></template>
        </el-table-column>
        <el-table-column prop="title" label="问题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="location" label="位置" min-width="140" show-overflow-tooltip />
        <el-table-column prop="category" label="类型" width="130" />
        <el-table-column prop="reporter" label="报修人" width="90" />
        <el-table-column prop="createdAt" label="提交时间" width="160" />
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无工单" />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawer" :title="current?.title" size="480px">
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
import { getConfig, listTickets, getTicket, updateTicket, addLog } from '../api'

const cfg = reactive({ statuses: [], categories: [] })
const filters = reactive({ status: '', category: '', urgency: '', q: '' })
const list = ref([])
const loading = ref(false)

const drawer = ref(false)
const current = ref(null)
const edit = reactive({ status: '', handler: '', resolution: '' })
const saving = ref(false)
const newLog = ref('')

function statusType(s) {
  return { 待处理: 'warning', 处理中: 'primary', 已解决: 'success', 已关闭: 'info' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const params = {}
    for (const k of ['status', 'category', 'urgency', 'q']) if (filters[k]) params[k] = filters[k]
    list.value = await listTickets(params)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
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
