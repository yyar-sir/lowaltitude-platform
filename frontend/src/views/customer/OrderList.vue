<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>📋</span>我的订单</h1>
      <p class="sub">查看全部订单状态，可取消未接单的订单。</p></div>
    <div class="k-card bar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="load">
        <el-option v-for="(n, i) in statusNames" :key="i" :label="n" :value="i" v-show="i > 0" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="订单号 / 地址" clearable style="max-width: 220px" @keyup.enter="load" />
      <button class="k-btn" style="padding: 8px 18px" @click="load">查询</button>
      <div class="spacer" />
      <button class="k-btn k-btn--orange" style="padding: 8px 18px" @click="$router.push('/customer/create')">+ 新建订单</button>
    </div>
    <el-table :data="list" stripe style="width: 100%; margin-top: 16px">
      <el-table-column prop="orderNo" label="订单号" min-width="170" />
      <el-table-column label="路线" min-width="250">
        <template #default="{ row }">{{ row.sendAddress }} → {{ row.receiveAddress }}</template>
      </el-table-column>
      <el-table-column label="物品" width="140">
        <template #default="{ row }">{{ row.goodsType }} · {{ row.goodsWeight }}kg</template>
      </el-table-column>
      <el-table-column label="时效" width="100">
        <template #default="{ row }">
          <span class="k-tag" :class="row.deliveryType === 2 ? 'k-tag--orange' : ''">{{ row.deliveryTypeName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="费用" width="100">
        <template #default="{ row }">￥{{ row.actualFee || row.estimateFee }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <span class="k-tag" :class="statusClass(row.orderStatus)">{{ row.orderStatusName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" min-width="150" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="go(row)">{{ row.orderStatus >= 4 ? '结算' : '跟踪' }}</el-button>
          <el-button v-if="row.orderStatus <= 2" size="small" type="danger" plain @click="cancelOrder(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !list.length" description="暂无订单" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancel, list as orderList } from '@/api/order'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const query = reactive({ status: null, keyword: '' })
const statusNames = ['', '待接单', '待调度', '飞行中', '已送达', '已完成', '已取消']

function statusClass(s) {
  return { 1: 'k-tag--yellow', 2: 'k-tag--orange', 3: '', 4: 'k-tag--teal', 5: 'k-tag--green', 6: 'k-tag--pink' }[s] || ''
}
function go(row) {
  router.push(row.orderStatus >= 4 ? '/customer/settlement/' + row.id : '/customer/track/' + row.id)
}
async function cancelOrder(row) {
  try {
    await ElMessageBox.confirm('确定取消订单 ' + row.orderNo + ' 吗？', '取消订单', { type: 'warning' })
  } catch (e) { return }
  try { await cancel(row.id, '客户主动取消'); ElMessage.success('已取消'); await load() } catch (e) { /* 已提示 */ }
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
</style>

