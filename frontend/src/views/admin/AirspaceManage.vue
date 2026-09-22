<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>🛡️</span>空域与合规</h1>
      <p class="sub">维护空域规则（可用空域 / 禁飞区 / 临时管制区），审批商家飞行计划，处置违规记录。</p></div>
    <el-tabs v-model="tab">
      <el-tab-pane :label="'空域规则 ' + zones.length" name="zones">
        <div class="bar"><button class="k-btn" style="padding: 7px 16px" @click="openZone()">+ 新增空域</button></div>
        <el-table :data="zones" stripe style="width: 100%; margin-top: 12px">
          <el-table-column prop="name" label="空域名称" min-width="180" />
          <el-table-column label="类型" width="130">
            <template #default="{ row }">
              <span class="k-tag" :class="zoneClass(row.zoneType)">{{ zoneName(row.zoneType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="高度上限" width="110">
            <template #default="{ row }">{{ row.maxAltitude }} m</template>
          </el-table-column>
          <el-table-column label="生效期" min-width="220">
            <template #default="{ row }">
              {{ row.effectiveFrom ? row.effectiveFrom + ' ~ ' + (row.effectiveTo || '长期') : '长期有效' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openZone(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="removeZone(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!zones.length" description="暂无空域规则" />
      </el-tab-pane>

      <el-tab-pane :label="'飞行计划审批 ' + pendingPlans.length" name="plans">
        <el-table :data="pendingPlans" stripe style="width: 100%; margin-top: 12px">
          <el-table-column prop="planNo" label="计划编号" width="150" />
          <el-table-column prop="merchantId" label="申报商家ID" width="110" />
          <el-table-column prop="route" label="计划航线" min-width="220" show-overflow-tooltip />
          <el-table-column label="高度" width="90">
            <template #default="{ row }">{{ row.altitude }}m</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="review(row, true)">通过</el-button>
              <el-button size="small" @click="review(row, false)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!pendingPlans.length" description="没有待审批的飞行计划" />
      </el-tab-pane>

      <el-tab-pane :label="'违规记录 ' + violations.length" name="violations">
        <el-table :data="violations" stripe style="width: 100%; margin-top: 12px">
          <el-table-column prop="violationType" label="违规类型" width="150" />
          <el-table-column prop="detail" label="说明" min-width="220" />
          <el-table-column label="等级" width="90">
            <template #default="{ row }">
              <span class="k-tag" :class="row.level >= 3 ? 'k-tag--pink' : row.level === 2 ? 'k-tag--orange' : ''">
                {{ ['', '轻微', '一般', '严重'][row.level] }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="k-tag" :class="row.handleStatus === 1 ? 'k-tag--green' : 'k-tag--yellow'">
                {{ row.handleStatus === 1 ? '已处理' : '待处理' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.handleStatus !== 1" size="small" type="primary" @click="handle(row)">处置</el-button>
              <span v-else class="muted">{{ row.handleResult || '已处置' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!violations.length" description="暂无违规记录" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="zoneVisible" :title="zoneForm.id ? '编辑空域' : '新增空域'" width="520px">
      <el-form label-width="110px">
        <el-form-item label="空域名称"><el-input v-model="zoneForm.name" placeholder="如 高新区可用空域" /></el-form-item>
        <el-form-item label="空域类型">
          <el-select v-model="zoneForm.zoneType" style="width: 100%">
            <el-option label="可用空域" :value="1" />
            <el-option label="禁飞区" :value="2" />
            <el-option label="临时管制区" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="高度上限(米)"><el-input-number v-model="zoneForm.maxAltitude" :min="0" :max="500" :step="10" style="width: 100%" /></el-form-item>
        <el-form-item label="边界坐标"><el-input v-model="zoneForm.boundary" type="textarea" :rows="2" placeholder="经度,纬度;经度,纬度;..." /></el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="zoneForm.effectiveFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker v-model="zoneForm.effectiveTo" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="zoneVisible = false">取消</el-button>
        <el-button type="primary" @click="saveZoneForm">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteZone, handleViolation, plans as fetchPlans, reviewPlan, saveZone, violations as fetchViolations, zones as fetchZones } from '@/api/airspace'

const loading = ref(false)
const tab = ref('zones')
const zones = ref([])
const plans = ref([])
const violations = ref([])
const zoneVisible = ref(false)
const zoneForm = reactive({ id: null, name: '', zoneType: 1, maxAltitude: 120, boundary: '', effectiveFrom: '', effectiveTo: '', status: 1 })

const pendingPlans = ref([])
function zoneName(t) { return { 1: '可用空域', 2: '禁飞区', 3: '临时管制区' }[t] || '未知' }
function zoneClass(t) { return { 1: 'k-tag--green', 2: 'k-tag--pink', 3: 'k-tag--orange' }[t] || '' }

function openZone(row) {
  Object.assign(zoneForm, row || { id: null, name: '', zoneType: 1, maxAltitude: 120, boundary: '', effectiveFrom: '', effectiveTo: '', status: 1 })
  zoneVisible.value = true
}
async function saveZoneForm() {
  if (!zoneForm.name.trim()) { ElMessage.warning('请填写空域名称'); return }
  try { await saveZone({ ...zoneForm }); ElMessage.success('已保存'); zoneVisible.value = false; await load() }
  catch (e) { /* 已提示 */ }
}
async function removeZone(row) {
  try { await ElMessageBox.confirm('确定删除空域「' + row.name + '」吗？', '删除', { type: 'warning' }) } catch (e) { return }
  try { await deleteZone(row.id); ElMessage.success('已删除'); await load() } catch (e) { /* 已提示 */ }
}
async function review(row, pass) {
  let reason = ''
  if (!pass) {
    try { const r = await ElMessageBox.prompt('请填写驳回原因', '驳回计划', { inputPlaceholder: '如：航线穿越禁飞区' }); reason = r.value }
    catch (e) { return }
  } else {
    try { await ElMessageBox.confirm('确定通过该飞行计划吗？', '审批', { type: 'warning' }) } catch (e) { return }
  }
  try { await reviewPlan(row.id, pass, reason); ElMessage.success(pass ? '已通过' : '已驳回'); await load() }
  catch (e) { /* 已提示 */ }
}
async function handle(row) {
  try {
    const r = await ElMessageBox.prompt('请填写处置结果', '违规处置', { inputPlaceholder: '如：已提醒商家补报飞行计划' })
    await handleViolation(row.id, r.value)
    ElMessage.success('已处置')
    await load()
  } catch (e) { /* 取消或已提示 */ }
}
async function load() {
  loading.value = true
  try {
    zones.value = (await fetchZones()) || []
    plans.value = (await fetchPlans()) || []
    pendingPlans.value = plans.value.filter(p => p.status === 0)
    violations.value = (await fetchViolations()) || []
  } catch (e) { /* 已提示 */ }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.bar { display: flex; justify-content: flex-end; }
.muted { color: var(--ink-3); font-size: var(--fs-xs); }
</style>

