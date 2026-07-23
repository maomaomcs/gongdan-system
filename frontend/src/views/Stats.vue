<template>
  <div>
    <div class="page-title">数据统计</div>
    <p class="page-desc">报修工单的整体情况与分布</p>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="8" class="stat-col">
        <el-card class="stat-card">
          <div style="display:flex;align-items:center;gap:14px">
            <div class="stat-ic" style="background:#f6e9ea">📦</div>
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

    <!-- ===== 网站访问统计 ===== -->
    <div class="page-title" style="margin-top:30px">网站访问统计</div>
    <div class="page-desc va-head">
      <span>老师端(报修/查询/登录)页面的访问情况 · UV 按访客当天去重估算,IP 已脱敏</span>
      <el-radio-group v-model="days" size="small" @change="loadAnalytics">
        <el-radio-button :value="7">近7天</el-radio-button>
        <el-radio-button :value="30">近30天</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="16" v-loading="vaLoading">
      <el-col :xs="12" :sm="6" class="stat-col">
        <el-card class="stat-card"><div class="num">{{ av.today?.uv ?? 0 }}</div><div class="lbl">今日访客(UV)</div><div class="sub">昨日 {{ av.yesterday?.uv ?? 0 }}</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="6" class="stat-col">
        <el-card class="stat-card"><div class="num">{{ av.today?.pv ?? 0 }}</div><div class="lbl">今日访问量(PV)</div><div class="sub">昨日 {{ av.yesterday?.pv ?? 0 }}</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="6" class="stat-col">
        <el-card class="stat-card"><div class="num">{{ av.range?.uv ?? 0 }}</div><div class="lbl">近{{ av.range?.days ?? days }}天访客</div><div class="sub">去重 UV</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="6" class="stat-col">
        <el-card class="stat-card"><div class="num">{{ av.range?.pv ?? 0 }}</div><div class="lbl">近{{ av.range?.days ?? days }}天访问量</div><div class="sub">总 PV</div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" v-loading="vaLoading">
      <el-col :xs="24" :sm="24" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="trendOpt" autoresize /></div></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="refererOpt" autoresize /></div></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="pagesOpt" autoresize /></div></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="regionOpt" autoresize /></div></el-card>
      </el-col>
      <el-col :xs="24" :sm="12" class="chart-col">
        <el-card><div class="chart-box"><v-chart :option="deviceOpt" autoresize /></div></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getStats, getAnalytics } from '../api'

use([CanvasRenderer, PieChart, BarChart, LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

const stats = reactive({ total: 0, open: 0, avgHours: null })
const statusOpt = ref({})
const categoryOpt = ref({})
const locationOpt = ref({})
const monthOpt = ref({})

// 网站访问统计
const days = ref(7)
const vaLoading = ref(false)
const av = reactive({ today: {}, yesterday: {}, range: {} })
const trendOpt = ref({})
const refererOpt = ref({})
const pagesOpt = ref({})
const regionOpt = ref({})
const deviceOpt = ref({})

function pageLabel(path) {
  const map = { '/': '首页', '/report': '我要报修', '/my': '我的报修', '/login': '登录', '/register': '注册' }
  return map[path] || path
}

async function loadAnalytics() {
  vaLoading.value = true
  try {
    const a = await getAnalytics(days.value)
    av.today = a.today; av.yesterday = a.yesterday; av.range = a.range
    const tdays = (a.trend || []).map((t) => t.day.slice(5))
    trendOpt.value = {
      title: { text: '访问趋势', left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0, data: ['访问量PV', '访客UV'] },
      grid: { left: 10, right: 24, bottom: 34, top: 46, containLabel: true },
      xAxis: { type: 'category', data: tdays },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '访问量PV', type: 'line', smooth: true, data: (a.trend || []).map((t) => t.pv), itemStyle: { color: '#a4232a' }, areaStyle: { opacity: 0.08 } },
        { name: '访客UV', type: 'line', smooth: true, data: (a.trend || []).map((t) => t.uv), itemStyle: { color: '#b8863b' }, areaStyle: { opacity: 0.08 } },
      ],
    }
    refererOpt.value = barOption('访问来源 Top', a.referers || [], '#b8863b')
    pagesOpt.value = barOption('热门页面 Top', (a.pages || []).map((p) => ({ name: pageLabel(p.name), count: p.count })), '#a4232a')
    regionOpt.value = barOption('访问地区 Top(按 IP 归属地)', a.regions || [], '#7a1519')
    deviceOpt.value = {
      title: { text: '访问设备', left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie', radius: ['40%', '65%'], center: ['50%', '48%'],
        data: (a.devices || []).map((d, i) => ({ name: d.name === 'mobile' ? '手机' : (d.name === 'desktop' ? '电脑' : d.name), value: d.count, itemStyle: { color: PALETTE[i % PALETTE.length] } })),
        label: { formatter: '{b}\n{c}' },
      }],
    }
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    vaLoading.value = false
  }
}

const PALETTE = ['#a4232a', '#b8863b', '#c9302c', '#7a1519', '#d8b463', '#8a6d3b', '#5a7d5a', '#a05a2c', '#6b6156']

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
    categoryOpt.value = barOption('按故障类型(哪类最多)', s.byCategory, '#a4232a')
    locationOpt.value = barOption('按位置/教室(哪里最坏)', s.byLocation, '#b8863b')
    monthOpt.value = {
      title: { text: '按月份趋势', left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'axis' },
      grid: { left: 10, right: 24, bottom: 10, top: 46, containLabel: true },
      xAxis: { type: 'category', data: s.byMonth.map((b) => b.name) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: s.byMonth.map((b) => b.count), barMaxWidth: 40, itemStyle: { color: '#7a1519', borderRadius: [6, 6, 0, 0] } }],
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
  loadAnalytics()
})
</script>

<style scoped>
.va-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.stat-card .sub { font-size: 12px; color: #a89e91; margin-top: 2px; }
</style>
