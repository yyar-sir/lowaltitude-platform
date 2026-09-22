<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>📦</span>订单调度</h1>
      <p class="sub">接单 → 生成调度方案 → 跟踪飞行 → 标记送达，订单全流程都在这里。</p></div>
    <el-tabs v-model="tab">
      <el-tab-pane :label="'待接单 ' + pending.length" name="pending">
        <el-table :data="pending" stripe style="width: 100%; margin-top: 12px">
          <el-table-column prop="orderNo" label="订单号" min-width="165" />
          <el-table-column label="路线" min-width="240">
            <template #default="{ row }">{{ row.sendAddress }} → {{ row.receiveAddress }}</template>
          </el-table-column>
          <el-table-column label="物品" width="140">
            <template #default="{ row }">{{ row.goodsType }} · {{ row.goodsWeight }}kg</template>
          </el-table-column>
          <el-table-column label="优先级" width="90">
            <template #default="{ row }">
              <span class="k-tag" :class="row.priority >= 3 ? 'k-tag--pink' : ''">{{ row.priorityName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预估费用" width="100">
            <template #default="{ row }">￥{{ row.estimateFee }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="doAccept(row)">接单</el-button>
              <el-button size="small" @click="doReject(row)">拒单</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!pending.length" description="暂无待接单订单" />
      </el-tab-pane>

      <el-tab-pane label="我的订单" name="mine">
        <el-table :data="mine" stripe style="width: 100%; margin-top: 12px">
          <el-table-column prop="orderNo" label="订单号" min-width="165" />
          <el-table-column label="路线" min-width="230">
            <template #default="{ row }">{{ row.sendAddress }} → {{ row.receiveAddress }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="k-tag" :class="statusClass(row.orderStatus)">{{ row.orderStatusName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="飞行器" width="130">
            <template #default="{ row }">{{ row.aircraftNo || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.orderStatus === 2" type="primary" size="small" @click="openDispatch(row)">生成调度方案</el-button>
              <el-button v-if="row.orderStatus === 3" size="small" @click="$router.push('/merchant/track/' + row.id)">跟踪</el-button>
              <el-button v-if="row.orderStatus === 3" size="small" type="success" plain @click="doArrive(row)">标记送达</el-button>
              <span v-if="row.orderStatus >= 4" class="muted">无需操作</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!mine.length" description="还没有承接订单" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" title="生成调度方案" width="620px">
      <div class="dlg-order">订单 {{ current.orderNo }} · {{ current.goodsWeight }}kg · 航程 {{ current.distanceKm }}km</div>
      <el-form label-width="100px" style="margin-top: 14px">
        <el-form-item label="飞行器">
          <el-select v-model="form.aircraftId" placeholder="选择可用飞行器（已按载重与电量过滤）" style="width: 100%">
            <el-option v-for="a in options.aircraft || []" :key="a.id"
              :label="a.aircraftNo + ' · 载重' + a.maxPayload + 'kg · 电量' + a.battery + '%'" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="起降场站">
          <el-select v-model="form.stationId" placeholder="选择起降场站" style="width: 100%">
            <el-option v-for="s in options.stations || []" :key="s.id" :label="s.stationNo + ' · ' + s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行机长">
          <el-select v-model="form.pilotId" placeholder="选择在岗机长（可不指派）" clearable style="width: 100%">
            <el-option v-for="p in options.pilots || []" :key="p.id" :label="p.name + ' · ' + p.licenseNo" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="飞行路径">
          <el-input v-model="form.flightPath" placeholder="留空则由系统自动规划（场站 → 收件地址）" />
        </el-form-item>
      </el-form>
      <el-alert v-if="advice.advice" type="success" :closable="false" style="margin-bottom: 10px">
        <template #title>AI 调度建议</template>
        <div class="advice">{{ advice.advice }}</div>
        <div class="rule-advice">规则建议：{{ advice.ruleAdvice }}</div>
      </el-alert>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doDispatch">提交调度指令</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { accept, arrive, dispatch, dispatchOptions, list as orderList, pending as pendingList, reject } from '@/api/order'
import { dispatchAdvice } from '@/api/ai'

const loading = ref(false)
const submitting = ref(false)
const tab = ref('pending')
const pending = ref([])
const mine = ref([])
const dialogVisible = ref(false)
const current = ref({})
const options = ref({})
const advice = ref({})
const form = reactive({ orderId: null, aircraftId: null, stationId: null, pilotId: null, flightPath: '' })

function statusClass(s) {
  return { 1: 'k-tag--yellow', 2: 'k-tag--orange', 3: '', 4: 'k-tag--teal', 5: 'k-tag--green', 6: 'k-tag--pink' }[s] || ''
}
async function load() {
  loading.value = true
  try {
    pending.value = (await pendingList()) || []
    const all = (await orderList({})) || []
    mine.value = all.filter(o => o.orderStatus >= 2)
  } catch (e) { /* 已提示 */ }
  loading.value = false
}
async function doAccept(row) {
  try {
    await accept(row.id)
    ElMessage.success('接单成功，订单进入待调度')
    tab.value = 'mine'
    await load()
  } catch (e) { /* 已提示 */ }
}
async function doReject(row) {
  try {
    const { value } = await ElMessageBox.prompt('请填写拒单原因（会同步给客户）', '拒单', {
      inputPlaceholder: '如：超出服务空域范围'
    })
    await reject(row.id, value)
    ElMessage.success('已拒单')
    await load()
  } catch (e) { /* 取消或已提示 */ }
}
async function openDispatch(row) {
  current.value = row
  Object.assign(form, { orderId: row.id, aircraftId: null, stationId: null, pilotId: null, flightPath: '' })
  options.value = {}
  advice.value = {}
  dialogVisible.value = true
  try { options.value = (await dispatchOptions(row.id)) || {} } catch (e) { /* 已提示 */ }
  try { advice.value = (await dispatchAdvice(row.id)) || {} } catch (e) { /* AI 不可用不影响调度 */ }
}
async function doDispatch() {
  if (!form.aircraftId) { ElMessage.warning('请选择飞行器'); return }
  submitting.value = true
  try {
    await dispatch({ ...form })
    ElMessage.success('调度指令已下达，订单进入飞行中')
    dialogVisible.value = false
    await load()
  } catch (e) { /* 载重不足等由拦截器提示 */ } finally { submitting.value = false }
}
async function doArrive(row) {
  try {
    await arrive(row.id)
    ElMessage.success('已标记送达，等待客户结算')
    await load()
  } catch (e) { /* 已提示 */ }
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.dlg-order { font-size: var(--fs-sm); color: var(--ink-2); }
.advice { font-size: var(--fs-sm); line-height: 1.7; }
.rule-advice { font-size: var(--fs-xs); color: var(--ink-2); margin-top: 6px; }
.muted { color: var(--ink-3); font-size: var(--fs-xs); }
</style>

