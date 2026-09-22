<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>🛡️</span>飞行计划申报</h1>
      <p class="sub">飞行前申报计划，审批通过后方可执行。申报高度不能超过可用空域上限。</p></div>
    <div class="plan-grid">
      <div class="k-card">
        <div class="side-title">申报新计划</div>
        <el-form label-position="top">
          <el-form-item label="计划航线"><el-input v-model="form.route" placeholder="如 ST-001→高新区科技园→南山区智慧园" /></el-form-item>
          <el-form-item label="飞行高度（米）">
            <el-input-number v-model="form.altitude" :min="30" :max="300" :step="10" style="width: 100%" />
            <div class="tip">可用空域上限：{{ maxAltitude }} 米</div>
          </el-form-item>
          <el-form-item label="计划开始时间">
            <el-date-picker v-model="form.planStart" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="计划结束时间">
            <el-date-picker v-model="form.planEnd" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>
          <button class="k-btn full" :disabled="submitting" @click="submit">提交申报</button>
        </el-form>
      </div>
      <div class="k-card">
        <div class="side-title">我的申报记录（{{ list.length }}）</div>
        <el-table :data="list" stripe size="small">
          <el-table-column prop="planNo" label="计划编号" width="150" />
          <el-table-column prop="route" label="航线" min-width="200" show-overflow-tooltip />
          <el-table-column label="高度" width="80">
            <template #default="{ row }">{{ row.altitude }}m</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="k-tag" :class="statusClass(row.status)">{{ statusName(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="rejectReason" label="驳回原因" min-width="150" />
        </el-table>
        <el-empty v-if="!list.length" description="暂无申报记录" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { plans as fetchPlans, submitPlan, zones } from '@/api/airspace'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const zoneList = ref([])
const form = reactive({ route: '', altitude: 100, planStart: '', planEnd: '' })

const maxAltitude = computed(() => {
  const usable = zoneList.value.filter(z => z.zoneType === 1 && z.status === 1)
  return usable.length ? Math.max(...usable.map(z => z.maxAltitude || 0)) : 120
})
function statusName(s) { return { 0: '待审批', 1: '已通过', 2: '已驳回', 3: '已执行' }[s] || '待审批' }
function statusClass(s) { return { 0: 'k-tag--yellow', 1: 'k-tag--green', 2: 'k-tag--pink', 3: 'k-tag--teal' }[s] || '' }

async function submit() {
  if (!form.route.trim()) { ElMessage.warning('请填写计划航线'); return }
  submitting.value = true
  try {
    await submitPlan({ ...form })
    ElMessage.success('申报已提交，等待平台审批')
    form.route = ''
    await load()
  } catch (e) { /* 高度超限等由拦截器提示 */ } finally { submitting.value = false }
}
async function load() {
  loading.value = true
  try {
    list.value = (await fetchPlans()) || []
    zoneList.value = (await zones({ status: 1 })) || []
  } catch (e) { /* 已提示 */ }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.plan-grid { display: grid; grid-template-columns: 360px minmax(0,1fr); gap: 18px; align-items: start; }
@media (max-width: 1000px) { .plan-grid { grid-template-columns: 1fr; } }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 14px; }
.full { width: 100%; }
.tip { font-size: var(--fs-xs); color: var(--ink-3); margin-top: 5px; }
</style>

