<template>
  <div>
    <div class="page-title">数据统计</div>
    <p class="page-desc">报修工单的整体情况与分布</p>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="8" class="stat-col">
        <el-card class="stat-card">
          <div style="display:flex;align-items:center;gap:14px">
            <div class="stat-ic" style="background:#eef2ff">📦</div>
            <div><div class="num">{{ stats.total }}</div><div class="lbl">工单总数</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" class="stat-col">
        <el-card class="stat-card">
          <div style="display:flex;align-items:center;gap:14px">
            <div class="stat-ic" style="background:#fff7ed">⏳</div>
            <div><div class="num">{{ stats.open }}</div><div class="lbl">待处理 / 处理中</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" class="stat-col">
        <el-card class="stat-card">
          <div style="display:flex;align-items:center;gap:14px">
            <div class="stat-ic" style="background:#ecfdf5">⚡</div>
            <div><div class="num">{{ stats.avgHours ?? '—' }}</div><div class="lbl">平均解决时长(小时)</div></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="statusOpt" autoresize /></div></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="categoryOpt" autoresize /></div></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="locationOpt" autoresize /></div></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="monthOpt" autoresize /></div></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getStats } from '../api'

use([CanvasRenderer, PieChart, BarChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

const stats = reactive({ total: 0, open: 0, avgHours: null })
const statusOpt = ref({})
const categoryOpt = ref({})
const locationOpt = ref({})
const monthOpt = ref({})

const PALETTE = ['#6366f1', '#8b5cf6', '#f59e0b', '#10b981', '#ef4444', '#3b82f6', '#ec4899', '#14b8a6', '#a855f7']

function barOption(title, buckets, color) {
  const names = buckets.map((b) => b.name)
  const values = buckets.map((b) => b.count)
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 15 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 24, bottom: 10, top: 46, containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: names.reverse(), axisLabel: { fontSize: 12 } },
    series: [{
      type: 'bar', data: values.reverse(), barMaxWidth: 22,
      itemStyle: { color, borderRadius: [0, 6, 6, 0] },
      label: { show: true, position: 'right' },
    }],
  }
}

onMounted(async () => {
  try {
    const s = await getStats()
    stats.total = s.total
    stats.open = s.open
    stats.avgHours = s.avgHours

    statusOpt.value = {
      title: { text: '按状态分布', left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie', radius: ['40%', '65%'], center: ['50%', '48%'],
        data: s.byStatus.map((b, i) => ({ name: b.name, value: b.count, itemStyle: { color: PALETTE[i % PALETTE.length] } })),
        label: { formatter: '{b}\n{c}' },
      }],
    }
    categoryOpt.value = barOption('按故障类型(哪类最多)', s.byCategory, '#6366f1')
    locationOpt.value = barOption('按位置/教室(哪里最坏)', s.byLocation, '#f59e0b')
    monthOpt.value = {
      title: { text: '按月份趋势', left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'axis' },
      grid: { left: 10, right: 24, bottom: 10, top: 46, containLabel: true },
      xAxis: { type: 'category', data: s.byMonth.map((b) => b.name) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: s.byMonth.map((b) => b.count), barMaxWidth: 40, itemStyle: { color: '#10b981', borderRadius: [6, 6, 0, 0] } }],
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
})
</script>
