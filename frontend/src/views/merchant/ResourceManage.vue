<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>🚁</span>低空资源</h1>
      <p class="sub">飞行器台账、起降场站与作业人员，是订单调度的前提。</p></div>
    <el-tabs v-model="tab">
      <el-tab-pane label="飞行器" name="aircraft">
        <div class="bar"><button class="k-btn" style="padding: 7px 16px" @click="openAircraft()">+ 新增飞行器</button></div>
        <el-table :data="aircraftList" stripe style="width: 100%; margin-top: 12px">
          <el-table-column prop="aircraftNo" label="编号" width="140" />
          <el-table-column prop="model" label="型号" min-width="130" />
          <el-table-column label="最大载重" width="110">
            <template #default="{ row }">{{ row.maxPayload }} kg</template>
          </el-table-column>
          <el-table-column label="续航" width="100">
            <template #default="{ row }">{{ row.enduranceMinutes }} 分钟</template>
          </el-table-column>
          <el-table-column label="电量" width="100">
            <template #default="{ row }">
              <b :class="row.battery < 30 ? 'low' : ''">{{ row.battery }}%</b>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="k-tag" :class="aircraftStatusClass(row.status)">{{ aircraftStatusName(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openAircraft(row)">编辑</el-button>
              <el-button v-if="row.status !== 3" size="small" plain @click="changeStatus(row, 3)">转维保</el-button>
              <el-button v-else size="small" type="success" plain @click="changeStatus(row, 1)">完成维保</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="起降场站" name="station">
        <div class="bar"><button class="k-btn" style="padding: 7px 16px" @click="openStation()">+ 新增场站</button></div>
        <el-table :data="stationList" stripe style="width: 100%; margin-top: 12px">
          <el-table-column prop="stationNo" label="编号" width="120" />
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column prop="address" label="地址" min-width="200" />
          <el-table-column label="容量" width="90">
            <template #default="{ row }">{{ row.capacity }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <span class="k-tag" :class="row.status === 1 ? 'k-tag--green' : 'k-tag--pink'">
                {{ row.status === 1 ? '启用' : '停用' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openStation(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="作业人员" name="pilot">
        <div class="bar"><button class="k-btn" style="padding: 7px 16px" @click="openPilot()">+ 新增机长</button></div>
        <el-table :data="pilotList" stripe style="width: 100%; margin-top: 12px">
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column prop="licenseNo" label="驾驶证编号" min-width="150" />
          <el-table-column prop="licenseExpire" label="到期日" width="130" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span class="k-tag" :class="pilotStatusClass(row.status)">{{ pilotStatusName(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openPilot(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="aircraftVisible" :title="aircraftForm.id ? '编辑飞行器' : '新增飞行器'" width="480px">
      <el-form label-width="110px">
        <el-form-item label="飞行器编号"><el-input v-model="aircraftForm.aircraftNo" placeholder="如 TS-FC30-004" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="aircraftForm.model" placeholder="如 天枢 FC30" /></el-form-item>
        <el-form-item label="最大载重(kg)"><el-input-number v-model="aircraftForm.maxPayload" :min="0.5" :max="100" :step="0.5" /></el-form-item>
        <el-form-item label="续航(分钟)"><el-input-number v-model="aircraftForm.enduranceMinutes" :min="5" :max="300" /></el-form-item>
        <el-form-item label="电量(%)"><el-input-number v-model="aircraftForm.battery" :min="0" :max="100" /></el-form-item>
        <el-form-item label="停放场站"><el-input v-model="aircraftForm.stationNo" placeholder="如 ST-001" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="aircraftVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAircraftForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="stationVisible" :title="stationForm.id ? '编辑场站' : '新增场站'" width="480px">
      <el-form label-width="100px">
        <el-form-item label="场站编号"><el-input v-model="stationForm.stationNo" placeholder="如 ST-004" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="stationForm.name" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="stationForm.address" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="stationForm.capacity" :min="1" :max="50" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="stationVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStationForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="pilotVisible" :title="pilotForm.id ? '编辑机长' : '新增机长'" width="480px">
      <el-form label-width="110px">
        <el-form-item label="姓名"><el-input v-model="pilotForm.name" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="pilotForm.phone" /></el-form-item>
        <el-form-item label="驾驶证编号"><el-input v-model="pilotForm.licenseNo" /></el-form-item>
        <el-form-item label="驾驶证到期">
          <el-date-picker v-model="pilotForm.licenseExpire" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="pilotVisible = false">取消</el-button>
        <el-button type="primary" @click="savePilotForm">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aircraft as fetchAircraft, changeAircraftStatus, pilots as fetchPilots, saveAircraft, savePilot, saveStation, stations as fetchStations } from '@/api/resource'

const loading = ref(false)
const tab = ref('aircraft')
const aircraftList = ref([])
const stationList = ref([])
const pilotList = ref([])
const aircraftVisible = ref(false), stationVisible = ref(false), pilotVisible = ref(false)
const aircraftForm = reactive({ id: null, aircraftNo: '', model: '', maxPayload: 5, enduranceMinutes: 45, battery: 100, stationNo: '' })
const stationForm = reactive({ id: null, stationNo: '', name: '', address: '', capacity: 3 })
const pilotForm = reactive({ id: null, name: '', phone: '', licenseNo: '', licenseExpire: '' })

function aircraftStatusName(s) { return { 1: '可用', 2: '任务中', 3: '维保中', 4: '停用' }[s] || '未知' }
function aircraftStatusClass(s) { return { 1: 'k-tag--green', 2: '', 3: 'k-tag--orange', 4: 'k-tag--pink' }[s] || '' }
function pilotStatusName(s) { return { 1: '在岗', 2: '任务中', 0: '休息' }[s] || '未知' }
function pilotStatusClass(s) { return { 1: 'k-tag--green', 2: '', 0: 'k-tag--yellow' }[s] || '' }

function openAircraft(row) {
  Object.assign(aircraftForm, row || { id: null, aircraftNo: '', model: '', maxPayload: 5, enduranceMinutes: 45, battery: 100, stationNo: '' })
  aircraftVisible.value = true
}
function openStation(row) {
  Object.assign(stationForm, row || { id: null, stationNo: '', name: '', address: '', capacity: 3 })
  stationVisible.value = true
}
function openPilot(row) {
  Object.assign(pilotForm, row || { id: null, name: '', phone: '', licenseNo: '', licenseExpire: '' })
  pilotVisible.value = true
}
async function saveAircraftForm() {
  try { await saveAircraft({ ...aircraftForm }); ElMessage.success('已保存'); aircraftVisible.value = false; await load() }
  catch (e) { /* 已提示 */ }
}
async function saveStationForm() {
  try { await saveStation({ ...stationForm }); ElMessage.success('已保存'); stationVisible.value = false; await load() }
  catch (e) { /* 已提示 */ }
}
async function savePilotForm() {
  try { await savePilot({ ...pilotForm }); ElMessage.success('已保存'); pilotVisible.value = false; await load() }
  catch (e) { /* 已提示 */ }
}
async function changeStatus(row, status) {
  try { await changeAircraftStatus(row.id, status); ElMessage.success('状态已更新'); await load() }
  catch (e) { /* 已提示 */ }
}
async function load() {
  loading.value = true
  try {
    aircraftList.value = (await fetchAircraft()) || []
    stationList.value = (await fetchStations()) || []
    pilotList.value = (await fetchPilots()) || []
  } catch (e) { /* 已提示 */ }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.bar { display: flex; justify-content: flex-end; }
.low { color: var(--c-pink); }
</style>

