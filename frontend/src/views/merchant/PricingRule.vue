<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>💰</span>定价规则</h1>
      <p class="sub">订单费用按下述规则计算：基础配送费 + 每公里费用 × 航程 + 空域使用费（加急另加）。</p></div>
    <div class="bar"><button class="k-btn" @click="open()">+ 新增规则</button></div>
    <el-table :data="list" stripe style="width: 100%; margin-top: 14px">
      <el-table-column prop="name" label="规则名称" min-width="160" />
      <el-table-column label="基础配送费" width="120">
        <template #default="{ row }">￥{{ row.baseFee }}</template>
      </el-table-column>
      <el-table-column label="每公里" width="110">
        <template #default="{ row }">￥{{ row.perKmFee }}</template>
      </el-table-column>
      <el-table-column label="空域使用费" width="120">
        <template #default="{ row }">￥{{ row.airspaceFee }}</template>
      </el-table-column>
      <el-table-column label="加急费" width="110">
        <template #default="{ row }">￥{{ row.urgentFee }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span class="k-tag" :class="row.status === 1 ? 'k-tag--green' : 'k-tag--pink'">
            {{ row.status === 1 ? '启用' : '停用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="open(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !list.length" description="还没有定价规则，新增一条才能正常计费" />

    <el-dialog v-model="visible" :title="form.id ? '编辑规则' : '新增规则'" width="480px">
      <el-form label-width="120px">
        <el-form-item label="规则名称"><el-input v-model="form.name" placeholder="如 城区普通配送标准" /></el-form-item>
        <el-form-item label="基础配送费(元)"><el-input-number v-model="form.baseFee" :min="0.01" :step="1" style="width: 100%" /></el-form-item>
        <el-form-item label="每公里费用(元)"><el-input-number v-model="form.perKmFee" :min="0" :step="0.5" style="width: 100%" /></el-form-item>
        <el-form-item label="空域使用费(元)"><el-input-number v-model="form.airspaceFee" :min="0" :step="1" style="width: 100%" /></el-form-item>
        <el-form-item label="加急费(元)"><el-input-number v-model="form.urgentFee" :min="0" :step="1" style="width: 100%" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pricing as fetchPricing, savePricing } from '@/api/resource'

const loading = ref(false)
const list = ref([])
const visible = ref(false)
const form = reactive({ id: null, name: '', baseFee: 20, perKmFee: 3, airspaceFee: 5, urgentFee: 8, status: 1 })

function open(row) {
  Object.assign(form, row || { id: null, name: '', baseFee: 20, perKmFee: 3, airspaceFee: 5, urgentFee: 8, status: 1 })
  visible.value = true
}
async function save() {
  if (!form.name.trim()) { ElMessage.warning('请填写规则名称'); return }
  try { await savePricing({ ...form }); ElMessage.success('已保存'); visible.value = false; await load() }
  catch (e) { /* 已提示 */ }
}
async function load() {
  loading.value = true
  try { list.value = (await fetchPricing()) || [] } catch (e) { list.value = [] }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.bar { display: flex; justify-content: flex-end; }
</style>

