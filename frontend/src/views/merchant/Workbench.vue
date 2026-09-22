<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>🏢</span>商家工作台</h1>
      <p class="sub">订单、运力与合规事项一屏概览。</p></div>
    <div class="k-grid stat-grid">
      <div v-for="c in cards" :key="c.label" class="k-card stat-card" @click="c.to && $router.push(c.to)">
        <div class="icon-box"><span>{{ c.emoji }}</span></div>
        <div><div class="stat-num" :class="{ warn: c.warn }">{{ c.value }}</div>
          <div class="stat-label">{{ c.label }}</div></div>
      </div>
    </div>
    <div v-if="todos.length" class="todo">
      <span class="todo-title">⏰ 待办提醒</span>
      <span v-for="(t, i) in todos" :key="i" class="todo-item">{{ t }}</span>
    </div>
    <div class="two">
      <div class="k-card">
        <div class="side-title">资源概览</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="飞行器总数">{{ res.aircraftTotal || 0 }}</el-descriptions-item>
          <el-descriptions-item label="可用飞行器">{{ res.aircraftAvailable || 0 }}</el-descriptions-item>
          <el-descriptions-item label="维保中">{{ res.aircraftMaintaining || 0 }}</el-descriptions-item>
          <el-descriptions-item label="起降场站">{{ res.stationTotal || 0 }}</el-descriptions-item>
          <el-descriptions-item label="作业人员">{{ res.pilotTotal || 0 }}</el-descriptions-item>
          <el-descriptions-item label="定价规则">{{ res.pricingTotal || 0 }}</el-descriptions-item>
        </el-descriptions>
        <button class="k-btn k-btn--ghost full" style="margin-top: 14px" @click="$router.push('/merchant/resources')">管理低空资源</button>
      </div>
      <div class="k-card">
        <div class="side-title">快捷操作</div>
        <button class="k-btn full" @click="$router.push('/merchant/orders')">📦 处理订单（接单/调度）</button>
        <button class="k-btn k-btn--ghost full mt" @click="$router.push('/merchant/plans')">🛡️ 申报飞行计划</button>
        <button class="k-btn k-btn--ghost full mt" @click="$router.push('/merchant/pricing')">💰 配置定价规则</button>
        <button class="k-btn k-btn--ghost full mt" @click="$router.push('/merchant/violations')">⚠️ 查看违规记录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { dashboard } from '@/api/dashboard'
import { overview } from '@/api/resource'

const loading = ref(false)
const cards = ref([])
const todos = ref([])
const res = ref({})

onMounted(async () => {
  loading.value = true
  try {
    const d = await dashboard()
    const s = d.summary || {}
    todos.value = d.todos || []
    cards.value = [
      { emoji: '📥', label: '待接单', value: s.orderPending || 0, warn: s.orderPending > 0, to: '/merchant/orders' },
      { emoji: '🗂️', label: '待调度', value: s.orderDispatching || 0, to: '/merchant/orders' },
      { emoji: '🚁', label: '飞行中', value: s.orderFlying || 0, to: '/merchant/orders' },
      { emoji: '✅', label: '已完成', value: s.orderFinished || 0 },
      { emoji: '📈', label: '完成率(%)', value: s.finishRate || 0 },
      { emoji: '💰', label: '营收(元)', value: s.revenue || 0 }
    ]
  } catch (e) { cards.value = [] }
  try { res.value = (await overview()) || {} } catch (e) { res.value = {} }
  loading.value = false
})
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.stat-grid { grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); }
.stat-card { display: flex; align-items: center; gap: 12px; padding: 16px 18px; cursor: pointer; }
.stat-num { font-family: var(--font-title); font-size: 23px; color: var(--brand-deep); line-height: 1.1; }
.stat-num.warn { color: var(--accent-deep); }
.stat-label { font-size: var(--fs-xs); color: var(--ink-3); }
.todo { margin-top: 18px; padding: 14px 20px; border-radius: var(--radius-md); background: var(--accent-soft); display: flex; gap: 18px; flex-wrap: wrap; }
.todo-title { font-weight: 700; color: var(--accent-deep); font-size: var(--fs-sm); }
.todo-item { font-size: var(--fs-sm); color: var(--accent-deep); }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-top: 18px; align-items: start; }
@media (max-width: 900px) { .two { grid-template-columns: 1fr; } }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 14px; }
.full { width: 100%; }
.mt { margin-top: 9px; }
</style>

