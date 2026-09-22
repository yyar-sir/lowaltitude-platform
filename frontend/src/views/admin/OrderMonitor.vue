<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>📦</span>订单监管</h1>
      <p class="sub">全平台订单总览，可查看任一订单的完整流转记录。</p></div>
    <div class="k-card bar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="load">
        <el-option v-for="(n, i) in statusNames" :key="i" :label="n" :value="i" v-show="i > 0" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="订单号 / 地址" clearable style="max-width: 220px" @keyup.enter="load" />
      <button class="k-btn" style="padding: 8px 18px" @click="load">查询</button>
      <div class="spacer" />
      <span class="count">共 {{ list.length }} 条</span>
    </div>
    <el-table :data="list" stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="orderNo" label="订单号" min-width="165" />
      <el-table-column prop="customerName" label="客户" width="130" />
      <el-table-column prop="merchantName" label="承接商家" min-width="160" />
      <el-table-column label="路线" min-width="230">
        <template #default="{ row }">{{ row.sendAddress }} → {{ row.receiveAddress }}</template>
      </el-table-column>
      <el-table-column label="时效" width="100">
        <template #default="{ row }">{{ row.deliveryTypeName }}</template>
      </el-table-column>
      <el-table-column label="费用" width="90">
        <template #default="{ row }">￥{{ row.actualFee || row.estimateFee }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span class="k-tag" :class="statusClass(row.orderStatus)">{{ row.orderStatusName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !list.length" description="暂无订单" />

    <el-dialog v-model="visible" title="订单详情" width="620px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span class="k-tag">{{ current.orderStatusName }}</span></el-descriptions-item>
        <el-descriptions-item label="客户">{{ current.customerName }}</el-descriptions-item>
        <el-descriptions-item label="商家">{{ current.merchantName || '未接单' }}</el-descriptions-item>
        <el-descriptions-item label="寄件">{{ current.sendAddress }}</el-descriptions-item>
        <el-descriptions-item label="收件">{{ current.receiveAddress }}</el-descriptions-item>
        <el-descriptions-item label="物品">{{ current.goodsType }} · {{ current.goodsWeight }}kg</el-descriptions-item>
        <el-descriptions-item label="航程">{{ current.distanceKm }} km</el-descriptions-item>
        <el-descriptions-item label="飞行器">{{ current.aircraftNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="机长">{{ current.pilotName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="拒单/取消原因" :span="2">{{ current.rejectReason || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div class="side-title" style="margin-top: 16px">状态流转</div>
      <el-steps :active="activeStep" simple>
        <el-step v-for="t in current.timeline || []" :key="t.label" :title="t.label" :description="t.time || ''" />
      </el-steps>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { detail, list as orderList } from '@/api/order'

const loading = ref(false)
const list = ref([])
const visible = ref(false)
const current = ref({})
const query = reactive({ status: null, keyword: '' })
const statusNames = ['', '待接单', '待调度', '飞行中', '已送达', '已完成', '已取消']

const activeStep = computed(() => (current.value.timeline || []).filter(t => t.done).length)
function statusClass(s) { return { 1: 'k-tag--yellow', 2: 'k-tag--orange', 3: '', 4: 'k-tag--teal', 5: 'k-tag--green', 6: 'k-tag--pink' }[s] || '' }

async function showDetail(row) {
  try { current.value = (await detail(row.id)) || {}; visible.value = true } catch (e) { /* 已提示 */ }
}
async function load() {
  loading.value = true
  try { list.value = (await orderList({ status: query.status, keyword: query.keyword || undefined })) || [] }
  catch (e) { list.value = [] }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.bar { display: flex; align-items: center; gap: 10px; padding: 14px 18px; flex-wrap: wrap; }
.bar .spacer { flex: 1; }
.count { font-size: var(--fs-sm); color: var(--ink-3); }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 10px; }
</style>

