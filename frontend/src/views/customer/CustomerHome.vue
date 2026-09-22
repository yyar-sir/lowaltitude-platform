<template>
  <div v-loading="loading">
    <section class="hero">
      <div>
        <h1>你好，{{ user.companyName || user.nickname || user.username }} 👋</h1>
        <p class="sub">低空物流配送，从下单到送达全程可追踪</p>
        <div class="actions">
          <button class="k-btn" @click="$router.push('/customer/create')">📦 立即下单</button>
          <button class="k-btn k-btn--ghost" @click="$router.push('/customer/orders')">查看我的订单</button>
        </div>
      </div>
      <div class="hero-stat">
        <div class="ring">{{ summary.finishRate || 0 }}%</div>
        <div class="ring-label">订单完成率</div>
      </div>
    </section>

    <div class="k-grid stat-grid">
      <div v-for="s in cards" :key="s.label" class="k-card stat-card">
        <div class="icon-box"><span>{{ s.emoji }}</span></div>
        <div><div class="stat-num">{{ s.value }}</div><div class="stat-label">{{ s.label }}</div></div>
      </div>
    </div>

    <div v-if="todos.length" class="todo">
      <span class="todo-title">⏰ 待办提醒</span>
      <span v-for="(t, i) in todos" :key="i" class="todo-item">{{ t }}</span>
    </div>

    <div class="k-section-head"><h2>最近订单</h2><span class="more" @click="$router.push('/customer/orders')">全部订单 →</span></div>
    <el-table :data="recentOrders" stripe style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" min-width="170" />
      <el-table-column label="路线" min-width="240">
        <template #default="{ row }">{{ row.sendAddress }} → {{ row.receiveAddress }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <span class="k-tag" :class="statusClass(row.orderStatus)">{{ row.orderStatusName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="estimateFee" label="预估费用" width="110" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="goTrack(row)">查看进度</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !recentOrders.length" description="还没有订单，去创建一个吧" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dashboard } from '@/api/dashboard'
import { list as orderList } from '@/api/order'

const router = useRouter()
const loading = ref(false)
const summary = ref({})
const todos = ref([])
const orders = ref([])
const user = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

const cards = computed(() => [
  { emoji: '📦', label: '全部订单', value: summary.value.orderTotal || 0 },
  { emoji: '🚁', label: '配送中', value: summary.value.orderFlying || 0 },
  { emoji: '✅', label: '已完成', value: summary.value.orderFinished || 0 },
  { emoji: '💰', label: '累计消费(元)', value: summary.value.revenue || 0 }
])
const recentOrders = computed(() => orders.value.slice(0, 5))

function statusClass(s) {
  return { 1: 'k-tag--yellow', 2: 'k-tag--orange', 3: '', 4: 'k-tag--teal', 5: 'k-tag--green', 6: 'k-tag--pink' }[s] || ''
}
function goTrack(row) {
  router.push(row.orderStatus >= 4 ? '/customer/settlement/' + row.id : '/customer/track/' + row.id)
}

onMounted(async () => {
  loading.value = true
  try {
    const d = await dashboard()
    summary.value = d.summary || {}
    todos.value = d.todos || []
  } catch (e) { /* 已提示 */ }
  try {
    orders.value = (await orderList({})) || []
  } catch (e) { orders.value = [] }
  loading.value = false
})
</script>

<style scoped>
.hero { display: flex; align-items: center; justify-content: space-between; gap: 24px; background: var(--navy); color: #fff; border-radius: var(--radius-lg); padding: 30px 34px; }
.hero h1 { color: #fff; font-size: 26px; margin: 0 0 8px; }
.hero .sub { color: rgba(255,255,255,0.8); font-size: var(--fs-sm); margin: 0 0 18px; }
.actions { display: flex; gap: 10px; }
.hero-stat { text-align: center; }
.ring { width: 108px; height: 108px; border-radius: 50%; border: 3px solid var(--accent); display: flex; align-items: center; justify-content: center; font-family: var(--font-title); font-size: 25px; }
.ring-label { font-size: var(--fs-xs); color: rgba(255,255,255,0.7); margin-top: 6px; }
.stat-grid { grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); margin-top: 20px; }
.stat-card { display: flex; align-items: center; gap: 13px; padding: 16px 18px; }
.stat-num { font-family: var(--font-title); font-size: 23px; color: var(--brand-deep); line-height: 1.1; }
.stat-label { font-size: var(--fs-xs); color: var(--ink-3); }
.todo { margin-top: 18px; padding: 14px 20px; border-radius: var(--radius-md); background: var(--accent-soft); display: flex; gap: 18px; flex-wrap: wrap; }
.todo-title { font-weight: 700; color: var(--accent-deep); font-size: var(--fs-sm); }
.todo-item { font-size: var(--fs-sm); color: var(--accent-deep); }
</style>

