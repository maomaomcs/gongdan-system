<template>
  <div class="public-wrap">
    <div class="public-card">
      <div class="public-header">
        <div class="logo">🛠️</div>
        <h1>设备报修</h1>
        <p>设备坏了？填一下,我会尽快处理</p>
      </div>

      <el-card v-if="!submitted">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="报修人姓名" prop="reporter">
                <el-input v-model="form.reporter" placeholder="如:张老师" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系方式">
                <el-input v-model="form.contact" placeholder="手机 / 微信(选填)" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="位置 / 教室" prop="location">
            <el-input v-model="form.location" placeholder="如:教学楼3楼 302 教室 / 办公室" />
          </el-form-item>

          <el-form-item label="故障类型" prop="category">
            <el-select v-model="form.category" placeholder="请选择…" style="width:100%">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>

          <el-form-item label="问题简述" prop="title">
            <el-input v-model="form.title" placeholder="一句话说明,如:投影仪打不开" />
          </el-form-item>

          <el-form-item label="详细描述">
            <el-input v-model="form.description" type="textarea" :rows="3"
              placeholder="什么时候坏的、有什么现象、试过什么方法(选填)" />
          </el-form-item>

          <el-form-item label="上传照片(选填)">
            <el-upload
              action="/api/upload"
              list-type="picture-card"
              accept="image/*"
              :limit="6"
              :on-success="onUploadSuccess"
              :on-remove="onUploadRemove"
              :on-error="onUploadError"
              :before-upload="beforeUpload">
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div style="font-size:12px;color:#94a3b8">拍下坏掉的设备,最多6张,单张≤8MB</div>
          </el-form-item>

          <el-form-item label="紧急程度">
            <el-radio-group v-model="form.urgency">
              <el-radio-button value="普通">普通</el-radio-button>
              <el-radio-button value="紧急">🔴 紧急(影响上课)</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">
            提交报修
          </el-button>
        </el-form>
      </el-card>

      <el-card v-else>
        <el-result icon="success" title="报修提交成功" sub-title="请记下工单号,可凭此号查询处理进度">
          <template #extra>
            <div class="result-code">{{ resultCode }}</div>
            <div style="margin-top:18px">
              <el-button type="primary" @click="goQuery">查询进度</el-button>
              <el-button @click="reset">再报一单</el-button>
            </div>
          </template>
        </el-result>
      </el-card>

      <div style="text-align:center;margin-top:16px">
        <el-link type="primary" @click="$router.push('/query')">查询已提交的报修进度 →</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getConfig, createTicket } from '../api'

const images = ref([])  // 已上传图片文件名
function beforeUpload(file) {
  if (file.size > 8 * 1024 * 1024) { ElMessage.error('图片不能超过 8MB'); return false }
  return true
}
function onUploadSuccess(resp) {
  if (resp && resp.name) images.value.push(resp.name)
}
function onUploadRemove(file) {
  const name = file.response && file.response.name
  if (name) images.value = images.value.filter((n) => n !== name)
}
function onUploadError() {
  ElMessage.error('图片上传失败,请重试')
}

const router = useRouter()
const formRef = ref()
const categories = ref([])
const loading = ref(false)
const submitted = ref(false)
const resultCode = ref('')

const form = reactive({
  reporter: '', contact: '', location: '', category: '', title: '', description: '', urgency: '普通',
})

const rules = {
  reporter: [{ required: true, message: '请填写报修人', trigger: 'blur' }],
  location: [{ required: true, message: '请填写位置', trigger: 'blur' }],
  category: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  title: [{ required: true, message: '请填写问题简述', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const cfg = await getConfig()
    categories.value = cfg.categories
  } catch (e) {
    ElMessage.error(e.message)
  }
})

async function submit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const r = await createTicket({ ...form, images: images.value })
      resultCode.value = r.code
      submitted.value = true
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      loading.value = false
    }
  })
}

function goQuery() {
  router.push({ path: '/query', query: { code: resultCode.value } })
}

function reset() {
  Object.assign(form, { reporter: '', contact: '', location: '', category: '', title: '', description: '', urgency: '普通' })
  images.value = []
  submitted.value = false
}
</script>
