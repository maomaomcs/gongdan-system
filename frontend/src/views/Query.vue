<template>
  <div class="public-wrap">
    <div class="public-card">
      <div class="public-header">
        <div class="logo">🔎</div>
        <h1>查询报修进度</h1>
        <p>输入工单号,查看处理进度</p>
      </div>

      <el-card>
        <div style="display:flex;gap:10px">
          <el-input v-model="code" size="large" placeholder="如:BX20260720-A1B2" @keyup.enter="query" />
          <el-button type="primary" size="large" :loading="loading" @click="query">查询</el-button>
        </div>
      </el-card>

      <el-card v-if="ticket" style="margin-top:18px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
          <h2 style="margin:0">{{ ticket.title }}</h2>
          <el-tag :type="statusType(ticket.status)" effect="light">{{ ticket.status }}</el-tag>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="工单号">{{ ticket.code }}</el-descriptions-item>
          <el-descriptions-item label="报修人">{{ ticket.reporter }}</el-descriptions-item>
          <el-descriptions-item label="位置">{{ ticket.location }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ ticket.category }}</el-descriptions-item>
          <el-descriptions-item label="紧急度">
            <el-tag v-if="ticket.urgency==='紧急'" type="danger" size="small">紧急</el-tag>
            <span v-else>普通</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ ticket.createdAt }}</el-descriptions-item>
          <el-descriptions-item v-if="ticket.description" label="详情">{{ ticket.description }}</el-descriptions-item>
          <el-descriptions-item v-if="ticket.handler" label="处理人">{{ ticket.handler }}</el-descriptions-item>
          <el-descriptions-item v-if="ticket.resolution" label="解决方案">{{ ticket.resolution }}</el-descriptions-item>
          <el-descriptions-item v-if="ticket.images && ticket.images.length" label="照片">
            <el-image v-for="n in ticket.images" :key="n" :src="'/api/files/' + n"
              :preview-src-list="ticket.images.map(x => '/api/files/' + x)" fit="cover"
              style="width:72px;height:72px;border-radius:8px;margin:2px" />
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="ticket.logs && ticket.logs.length" style="margin-top:16px">
          <h4>处理记录</h4>
          <el-timeline>
            <el-timeline-item v-for="l in ticket.logs" :key="l.id" :timestamp="l.createdAt" placement="top">
              {{ l.content }} <span v-if="l.author" style="color:#94a3b8">— {{ l.author }}</span>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-card>

      <div style="text-align:center;margin-top:16px">
        <el-link type="primary" @click="$router.push('/')">← 返回报修</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryByCode } from '../api'

const route = useRoute()
const code = ref('')
const ticket = ref(null)
const loading = ref(false)

function statusType(s) {
  return { 待处理: 'warning', 处理中: 'primary', 已解决: 'success', 已关闭: 'info' }[s] || 'info'
}

async function query() {
  if (!code.value.trim()) return ElMessage.warning('请输入工单号')
  loading.value = true
  ticket.value = null
  try {
    ticket.value = await queryByCode(code.value.trim())
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.code) {
    code.value = route.query.code
    query()
  }
})
</script>
