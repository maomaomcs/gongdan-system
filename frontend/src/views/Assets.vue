<template>
  <div class="assets-page">
    <!-- 顶部统计 -->
    <div class="stat-cards" v-if="stats">
      <div class="stat-card">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-label">资产总数</div>
      </div>
      <div class="stat-card" v-for="s in stats.byStatus" :key="s.name">
        <div class="stat-num">{{ s.count }}</div>
        <div class="stat-label">{{ s.name }}</div>
      </div>
    </div>

    <!-- 筛选 + 操作 -->
    <div class="toolbar">
      <el-select v-model="filters.type" placeholder="类型" clearable style="width:130px" @change="reload">
        <el-option v-for="t in TYPES" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:120px" @change="reload">
        <el-option v-for="s in STATUSES" :key="s" :label="s" :value="s" />
      </el-select>
      <el-input v-model="filters.location" placeholder="位置" clearable style="width:140px" @keyup.enter="reload" @clear="reload" />
      <el-input v-model="filters.q" placeholder="编号/型号/SN/IP/责任人" clearable style="width:220px" @keyup.enter="reload" @clear="reload" />
      <el-button type="primary" @click="reload"><el-icon><Search /></el-icon>查询</el-button>
      <div class="spacer" />
      <el-button type="success" @click="openCreate"><el-icon><Plus /></el-icon>新增资产</el-button>
      <el-button @click="downloadTemplate"><el-icon><Document /></el-icon>导入模板</el-button>
      <el-upload :show-file-list="false" accept=".xlsx" :http-request="doImport" style="display:inline-block">
        <el-button type="warning" plain :loading="importing"><el-icon><Upload /></el-icon>批量导入</el-button>
      </el-upload>
      <el-button @click="doExport"><el-icon><Download /></el-icon>导出Excel</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="rows" v-loading="loading" border stripe size="small" style="width:100%">
      <el-table-column prop="assetNo" label="资产编号" width="140" fixed />
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="brandModel" label="品牌型号" width="150" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="120" />
      <el-table-column prop="location" label="位置" width="130" show-overflow-tooltip />
      <el-table-column prop="owner" label="责任人" width="90" />
      <el-table-column prop="department" label="科室" width="90" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small" effect="light">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="保修到期" width="150">
        <template #default="{ row }">
          <span v-if="row.warrantyEnd" :class="warrantyClass(row.warrantyState)">
            {{ row.warrantyEnd }}
            <el-tag v-if="row.warrantyState === '即将到期'" type="warning" size="small">{{ row.warrantyDays }}天</el-tag>
            <el-tag v-else-if="row.warrantyState === '已过保'" type="danger" size="small">已过保</el-tag>
          </span>
          <span v-else style="color:#bbb">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="warning" size="small" @click="openTickets(row)">报障记录</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      background
      layout="total, prev, pager, next, sizes"
      :total="total"
      :current-page="page + 1"
      :page-size="size"
      :page-sizes="[20, 50, 100, 200]"
      @current-change="(p) => { page = p - 1; load() }"
      @size-change="(s) => { size = s; page = 0; load() }"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑资产' : '新增资产'" :width="isMobile ? '94%' : '680px'" top="6vh">
      <el-form :model="form" label-width="82px" size="default">
        <el-row :gutter="12">
          <el-col :span="span"><el-form-item label="资产编号" required><el-input v-model="form.assetNo" placeholder="如 PC-教A-001" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="类型" required>
            <el-select v-model="form.type" filterable allow-create style="width:100%">
              <el-option v-for="t in TYPES" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="span"><el-form-item label="品牌型号"><el-input v-model="form.brandModel" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="序列号SN"><el-input v-model="form.serialNo" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="IP"><el-input v-model="form.ip" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="MAC"><el-input v-model="form.mac" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="位置"><el-input v-model="form.location" placeholder="楼栋-房间" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="责任人"><el-input v-model="form.owner" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="使用科室"><el-input v-model="form.department" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="状态">
            <el-select v-model="form.status" style="width:100%">
              <el-option v-for="s in STATUSES" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="span"><el-form-item label="购入日期"><el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="保修到期"><el-date-picker v-model="form.warrantyEnd" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="供应商"><el-input v-model="form.supplier" /></el-form-item></el-col>
          <el-col :span="span"><el-form-item label="采购单号"><el-input v-model="form.purchaseOrder" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可写关联工单号等" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 报障记录抽屉 -->
    <el-drawer v-model="ticketDrawer" :title="`报障记录 · ${ticketAsset?.assetNo || ''}`" :size="isMobile ? '92%' : '620px'">
      <div v-loading="ticketLoading">
        <el-alert v-if="ticketAsset" :closable="false" type="info" style="margin-bottom:12px">
          {{ ticketAsset.type }}{{ ticketAsset.brandModel ? ' · ' + ticketAsset.brandModel : '' }}
          {{ ticketAsset.location ? ' · ' + ticketAsset.location : '' }} —— 共报障
          <b style="color:#a4232a">{{ tickets.length }}</b> 次
        </el-alert>
        <el-empty v-if="!ticketLoading && tickets.length === 0" description="这台设备暂无报障记录" />
        <el-timeline v-else>
          <el-timeline-item v-for="t in tickets" :key="t.id"
            :timestamp="fmtTime(t.createdAt)" placement="top"
            :type="tlType(t.status)">
            <el-card shadow="never" body-style="padding:10px 12px">
              <div style="display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap">
                <b>{{ t.title }}</b>
                <el-tag size="small" :type="tlType(t.status)">{{ t.status }}</el-tag>
              </div>
              <div style="color:#8a7f70;font-size:12px;margin-top:4px">
                {{ t.code }} · {{ t.category }} · 报修人 {{ t.reporter }}
              </div>
              <div v-if="t.resolution" style="margin-top:6px;font-size:13px">处理:{{ t.resolution }}</div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <!-- 导入结果 -->
    <el-dialog v-model="importDialog" title="导入结果" :width="isMobile ? '92%' : '520px'">
      <div v-if="importResult">
        <p>共 <b>{{ importResult.total }}</b> 行:成功
          <b style="color:#67c23a">{{ importResult.success }}</b>,跳过
          <b style="color:#e6a23c">{{ importResult.skipped }}</b>,失败
          <b style="color:#f56c6c">{{ importResult.failed }}</b>。</p>
        <div v-if="importResult.errors && importResult.errors.length" class="import-errs">
          <div v-for="(e, i) in importResult.errors" :key="i">{{ e }}</div>
        </div>
      </div>
      <template #footer><el-button type="primary" @click="importDialog = false">知道了</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAssets, createAsset, updateAsset, deleteAsset, getAssetStats, exportAssetsExcel,
  getAssetTickets, importAssets, downloadAssetTemplate } from '../api'
import { useMobile } from '../composables/useMobile'

const { isMobile } = useMobile()
const span = computed(() => (isMobile.value ? 24 : 12))

const TYPES = ['台式电脑', '笔记本', '打印机', '一体机', '交换机', '路由器', '投影仪', '服务器', '监控', '其他']
const STATUSES = ['在用', '闲置', '维修中', '报废']

const rows = ref([])
const total = ref(0)
const page = ref(0)
const size = ref(20)
const loading = ref(false)
const stats = ref(null)
const filters = reactive({ type: '', status: '', location: '', q: '' })

function statusTag(s) {
  return { 在用: 'success', 闲置: 'info', 维修中: 'warning', 报废: 'danger' }[s] || 'info'
}
function warrantyClass(state) {
  if (state === '已过保') return 'w-expired'
  if (state === '即将到期') return 'w-soon'
  return ''
}

async function load() {
  loading.value = true
  try {
    const params = { ...filters, page: page.value, size: size.value }
    const res = await listAssets(params)
    rows.value = res.list
    total.value = res.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
function reload() { page.value = 0; load() }

async function loadStats() {
  try { stats.value = await getAssetStats() } catch (e) { /* ignore */ }
}

// ---- 表单 ----
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})
const EMPTY = {
  id: null, assetNo: '', type: '台式电脑', brandModel: '', serialNo: '', ip: '', mac: '',
  location: '', owner: '', department: '', purchaseDate: null, warrantyEnd: null,
  supplier: '', purchaseOrder: '', status: '在用', remark: '',
}
function openCreate() {
  Object.assign(form, EMPTY)
  dialog.value = true
}
function openEdit(row) {
  Object.assign(form, EMPTY, row)
  dialog.value = true
}
async function save() {
  if (!form.assetNo || !form.assetNo.trim()) return ElMessage.warning('请填写资产编号')
  if (!form.type || !form.type.trim()) return ElMessage.warning('请选择类型')
  saving.value = true
  try {
    if (form.id) await updateAsset(form.id, form)
    else await createAsset(form)
    ElMessage.success('保存成功')
    dialog.value = false
    load(); loadStats()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}
async function doDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除资产「${row.assetNo}」?`, '提示', { type: 'warning' })
    await deleteAsset(row.id)
    ElMessage.success('已删除')
    load(); loadStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message)
  }
}
async function doExport() {
  try { await exportAssetsExcel(filters); ElMessage.success('已导出') }
  catch (e) { ElMessage.error(e.message) }
}

// ---- 批量导入 ----
const importing = ref(false)
const importDialog = ref(false)
const importResult = ref(null)
async function downloadTemplate() {
  try { await downloadAssetTemplate() } catch (e) { ElMessage.error(e.message) }
}
async function doImport(opt) {
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', opt.file)
    importResult.value = await importAssets(fd)
    importDialog.value = true
    load(); loadStats()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    importing.value = false
  }
}

// ---- 报障记录 ----
const ticketDrawer = ref(false)
const ticketLoading = ref(false)
const ticketAsset = ref(null)
const tickets = ref([])
async function openTickets(row) {
  ticketAsset.value = row
  tickets.value = []
  ticketDrawer.value = true
  ticketLoading.value = true
  try {
    const res = await getAssetTickets(row.id)
    tickets.value = res.tickets || []
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    ticketLoading.value = false
  }
}
function fmtTime(s) { return s ? String(s).replace('T', ' ').slice(0, 16) : '' }
function tlType(s) {
  return { 待处理: 'danger', 处理中: 'warning', 已解决: 'success', 已关闭: 'info', 已取消: 'info' }[s] || 'primary'
}

onMounted(() => { load(); loadStats() })
</script>

<style scoped>
.assets-page { padding: 4px; }
.stat-cards { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.stat-card {
  background: #fff; border: 1px solid #ece3d3; border-radius: 8px;
  padding: 12px 20px; min-width: 92px; text-align: center;
  box-shadow: 0 1px 3px rgba(122,21,25,.05);
}
.stat-num { font-size: 24px; font-weight: 700; color: #a4232a; font-family: 'Noto Serif SC','STSong',serif; }
.stat-label { font-size: 13px; color: #8a7f70; margin-top: 2px; }
.toolbar { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 12px; }
.toolbar .spacer { flex: 1; }
.pager { margin-top: 14px; justify-content: flex-end; }
.w-expired { color: #c0392b; font-weight: 600; }
.w-soon { color: #b8863b; font-weight: 600; }
.import-errs {
  margin-top: 10px; max-height: 260px; overflow: auto;
  background: #fdf6e8; border: 1px solid #ece3d3; border-radius: 6px;
  padding: 8px 10px; font-size: 12px; color: #8a6d3b; line-height: 1.9;
}
@media (max-width: 768px) {
  .toolbar .spacer { display: none; flex: 0; }
  .toolbar > * { flex: 1 1 auto; }
}
</style>
