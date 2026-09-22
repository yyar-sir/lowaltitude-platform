<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>🚨</span>应急与客服</h1>
      <p class="sub">全平台上报的应急事件与客户反馈，按「待处理 → 处理中 → 已解决」闭环处置。</p></div>
    <div class="k-card bar">
      <el-radio-group v-model="status" size="small" @change="load">
        <el-radio-button :label="null">全部</el-radio-button>
        <el-radio-button :label="0">待处理</el-radio-button>
        <el-radio-button :label="1">处理中</el-radio-button>
        <el-radio-button :label="2">已解决</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <span class="count">共 {{ list.length }} 条</span>
    </div>
    <el-table :data="list" stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="title" label="事件标题" min-width="200" />
      <el-table-column prop="content" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ typeName(row.incidentType) }}</template>
      </el-table-column>
      <el-table-column label="等级" width="90">
        <template #default="{ row }">
          <span class="k-tag" :class="row.level >= 3 ? 'k-tag--pink' : row.level === 2 ? 'k-tag--orange' : ''">
            {{ ['', '一般', '较大', '重大'][row.level] }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span class="k-tag" :class="statusClass(row.status)">{{ statusName(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上报时间" min-width="150" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" size="small" type="primary" @click="handle(row, 1)">开始处理</el-button>
          <el-button v-if="row.status === 1" size="small" type="success" @click="handle(row, 2)">标记已解决</el-button>
          <span v-if="row.status === 2" class="muted">{{ row.handleResult || '已解决' }}</span>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !list.length" description="暂无应急事件" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { handle as handleIncident, list as incidentList } from '@/api/incident'

const loading = ref(false)
const list = ref([])
const status = ref(null)
function typeName(t) { return { 1: '设备故障', 2: '空域冲突', 3: '恶劣天气', 4: '其他' }[t] || '其他' }
function statusName(s) { return { 0: '待处理', 1: '处理中', 2: '已解决' }[s] || '待处理' }
function statusClass(s) { return { 0: 'k-tag--yellow', 1: 'k-tag--orange', 2: 'k-tag--green' }[s] || '' }

async function handle(row, target) {
  let result = ''
  if (target === 2) {
    try { const r = await ElMessageBox.prompt('请填写处置结果', '标记已解决', { inputPlaceholder: '如：已协调绕行并安全送达' }); result = r.value }
    catch (e) { return }
  } else {
    try { await ElMessageBox.confirm('确定开始处理该事件吗？', '处理事件', { type: 'warning' }) } catch (e) { return }
  }
  try { await handleIncident(row.id, target, result); ElMessage.success('已更新'); await load() }
  catch (e) { /* 不允许跳步时后端会提示 */ }
}
async function load() {
  loading.value = true
  try { list.value = (await incidentList(status.value)) || [] } catch (e) { list.value = [] }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.bar { display: flex; align-items: center; gap: 12px; padding: 12px 18px; }
.bar .spacer { flex: 1; }
.count { font-size: var(--fs-sm); color: var(--ink-3); }
.muted { color: var(--ink-3); font-size: var(--fs-xs); }
</style>

