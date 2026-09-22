<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>🚨</span>反馈与应急</h1>
      <p class="sub">配送异常、物品损坏等问题可在此上报，平台会跟进处理。</p></div>
    <div class="k-card">
      <div class="side-title">上报新事件</div>
      <el-form label-position="top">
        <el-form-item label="事件标题"><el-input v-model="form.title" maxlength="50" placeholder="如：配送延迟未送达" /></el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="form.incidentType" style="width: 100%">
            <el-option v-for="(n, i) in typeNames" :key="i" :label="n" :value="i" v-show="i > 0" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-radio-group v-model="form.level">
            <el-radio :label="1">一般</el-radio><el-radio :label="2">较大</el-radio><el-radio :label="3">重大</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细描述"><el-input v-model="form.content" type="textarea" :rows="3" maxlength="300" show-word-limit /></el-form-item>
        <button class="k-btn" :disabled="submitting" @click="doReport">提交上报</button>
      </el-form>
    </div>

    <div class="k-section-head"><h2>我的上报记录</h2></div>
    <el-table :data="list" stripe style="width: 100%">
      <el-table-column prop="title" label="事件" min-width="200" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ typeNames[row.incidentType] || '其他' }}</template>
      </el-table-column>
      <el-table-column label="程度" width="90">
        <template #default="{ row }">
          <span class="k-tag" :class="levelClass(row.level)">{{ levelName(row.level) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span class="k-tag" :class="statusClass(row.status)">{{ statusName(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="handleResult" label="处理结果" min-width="180" />
      <el-table-column prop="createdAt" label="上报时间" min-width="150" />
    </el-table>
    <el-empty v-if="!loading && !list.length" description="暂无上报记录" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { list as incidentList, report } from '@/api/incident'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const typeNames = ['', '设备故障', '空域冲突', '恶劣天气', '其他']
const form = reactive({ title: '', content: '', incidentType: 4, level: 1 })

function levelName(l) { return { 1: '一般', 2: '较大', 3: '重大' }[l] || '一般' }
function levelClass(l) { return { 1: '', 2: 'k-tag--orange', 3: 'k-tag--pink' }[l] || '' }
function statusName(s) { return { 0: '待处理', 1: '处理中', 2: '已解决' }[s] || '待处理' }
function statusClass(s) { return { 0: 'k-tag--yellow', 1: 'k-tag--orange', 2: 'k-tag--green' }[s] || '' }

async function doReport() {
  if (!form.title.trim()) { ElMessage.warning('请填写事件标题'); return }
  submitting.value = true
  try {
    await report({ ...form })
    ElMessage.success('上报成功，平台会尽快处理')
    form.title = ''; form.content = ''
    await load()
  } catch (e) { /* 已提示 */ } finally { submitting.value = false }
}
async function load() {
  loading.value = true
  try { list.value = (await incidentList()) || [] } catch (e) { list.value = [] }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 14px; }
</style>

