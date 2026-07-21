<template>
  <div class="u-page">
    <div v-if="!submitted">
      <h2 class="u-title">我要报修</h2>
      <el-card>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
          <el-form-item label="报修人" prop="reporter">
            <el-input v-model="form.reporter" placeholder="你的姓名" />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model="form.contact" placeholder="手机 / 微信(选填)" />
          </el-form-item>
          <el-form-item label="位置 / 教室" prop="location">
            <el-select v-if="locations.length" v-model="form.location" filterable allow-create default-first-option
              placeholder="选择或直接输入,如:教学楼3楼 302 教室" style="width:100%">
              <el-option v-for="l in locations" :key="l" :label="l" :value="l" />
            </el-select>
            <el-input v-else v-model="form.location" placeholder="如:教学楼3楼 302 教室" />
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
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="现象、什么时候坏的(选填)" />
          </el-form-item>
          <el-form-item label="上传照片(选填)">
            <el-upload action="/api/upload" list-type="picture-card" accept="image/*" :limit="6"
              :on-success="onUp" :on-remove="onRm" :before-upload="beforeUp" :on-error="() => $message?.error('上传失败')">
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="u-tip">拍下坏掉的设备,最多6张</div>
          </el-form-item>
          <el-form-item label="紧急程度">
            <el-radio-group v-model="form.urgency">
              <el-radio-button value="普通">普通</el-radio-button>
              <el-radio-button value="紧急">🔴 紧急</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">提交报修</el-button>
        </el-form>
      </el-card>
    </div>

    <div v-else>
      <el-card>
        <el-result icon="success" title="报修提交成功" sub-title="可在「我的报修」随时查看处理进度">
          <template #extra>
            <div class="result-code">{{ resultCode }}</div>
            <div style="margin-top:18px">
              <el-button type="primary" @click="$router.push('/my')">查看我的报修</el-button>
              <el-button @click="reset">再报一单</el-button>
            </div>
          </template>
        </el-result>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getConfig, submitTicket } from '../../api'

const formRef = ref()
const categories = ref([])
const locations = ref([])
const loading = ref(false)
const submitted = ref(false)
const resultCode = ref('')
const images = ref([])

const form = reactive({
  reporter: localStorage.getItem('user_name') || '',
  contact: '', location: '', category: '', title: '', description: '', urgency: '普通',
})
const rules = {
  reporter: [{ required: true, message: '请填写报修人', trigger: 'blur' }],
  location: [{ required: true, message: '请填写位置', trigger: 'blur' }],
  category: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  title: [{ required: true, message: '请填写问题简述', trigger: 'blur' }],
}

function beforeUp(file) { if (file.size > 8 * 1024 * 1024) { ElMessage.error('图片不能超过8MB'); return false } return true }
function onUp(resp) { if (resp && resp.name) images.value.push(resp.name) }
function onRm(file) { const n = file.response && file.response.name; if (n) images.value = images.value.filter(x => x !== n) }

onMounted(async () => {
  try {
    const cfg = await getConfig()
    categories.value = cfg.categories || []
    locations.value = cfg.locations || []
  } catch (e) { /* ignore */ }
})

async function submit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const r = await submitTicket({ ...form, images: images.value })
      resultCode.value = r.code
      submitted.value = true
      window.scrollTo(0, 0)
    } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
  })
}

function reset() {
  Object.assign(form, { contact: '', location: '', category: '', title: '', description: '', urgency: '普通' })
  images.value = []
  submitted.value = false
}
</script>
