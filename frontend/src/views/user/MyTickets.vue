<template>
  <div class="u-page">
    <h2 class="u-title">我的报修</h2>

    <el-empty v-if="!loading && !list.length" description="还没有报修记录" />

    <div v-loading="loading">
      <el-card v-for="t in list" :key="t.id" class="ticket-item" shadow="never" @click="open(t)">
        <div class="ti-head">
          <span class="ti-title">{{ t.title }}</span>
          <el-tag :type="statusType(t.status)" size="small" effect="light">{{ t.status }}</el-tag>
        </div>
        <div class="ti-meta">
          <span>{{ t.category }}</span>
          <span v-if="t.urgency === '紧急'" class="urgent">🔴 紧急</span>
        </div>
        <div class="ti-sub">
          <span>{{ t.location }}</span>
          <span class="ti-code">{{ t.code }}</span>
        </div>
        <div class="ti-time">{{ t.createdAt }}</div>
      </el-card>
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
import { getMyTickets, getMyTicket } from '../../api'

const list = ref([])
const loading = ref(false)
const show = ref(false)
const cur = ref(null)

function statusType(s) {
  return { 待处理: 'warning', 处理中: 'primary', 已解决: 'success', 已关闭: 'info' }[s] || 'info'
}

async function load() {
  loading.value = true
  try { list.value = await getMyTickets() }
  catch (e) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

async function open(t) {
  try { cur.value = await getMyTicket(t.id); show.value = true }
  catch (e) { ElMessage.error(e.message) }
}

onMounted(load)
</script>
