<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>📊</span>全平台数据看板</h1>
      <p class="sub">用户、订单、运力与合规运营的全局概览。</p></div>
    <div class="k-grid stat-grid">
      <div v-for="c in cards" :key="c.label" class="k-card stat-card">
        <div class="icon-box"><span>{{ c.emoji }}</span></div>
        <div><div class="stat-num">{{ c.value }}</div><div class="stat-label">{{ c.label }}</div></div>
      </div>
    </div>
    <div class="chart-grid">
      <div class="k-card wide"><div class="chart-title">近 14 天订单趋势</div><div ref="trendRef" class="chart" /></div>
      <div class="k-card"><div class="chart-title">订单状态分布</div><div ref="pieRef" class="chart" /></div>
      <div class="k-card"><div class="chart-title">飞行器状态</div><div ref="airRef" class="chart" /></div>
      <div class="k-card"><div class="chart-title">违规类型分布</div><div ref="vioRef" class="chart" /></div>
    </div>
    <div v-if="todos.length" class="k-card todo-card">
      <div class="side-title">待办提醒</div>
      <div v-for="(t, i) in todos" :key="i" class="todo-item">· {{ t }}</div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { dashboard } from '@/api/dashboard'

const loading = ref(false)
const cards = ref([])
const todos = ref([])
const trendRef = ref(null), pieRef = ref(null), airRef = ref(null), vioRef = ref(null)
const charts = []
const C = { brand: '#1e4fd8', accent: '#ff8a3d', teal: '#178a7d', green: '#2e9e6b', line: '#e6eaf3', ink3: '#8896b4' }
function draw(el, option) { if (!el) return; const c = echarts.init(el); c.setOption(option); charts.push(c) }
onMounted(async () => {
  loading.value = true
  try {
    const d = await dashboard()
    const s = d.summary || {}
    todos.value = d.todos || []
    cards.value = [
      { emoji: '👥', label: '注册用户', value: s.userTotal || 0 },
      { emoji: '🚁', label: '入驻商家', value: s.merchantTotal || 0 },
      { emoji: '🏢', label: '终端客户', value: s.customerTotal || 0 },
      { emoji: '🛩️', label: '飞行器', value: s.aircraftTotal || 0 },
      { emoji: '📦', label: '订单总数', value: s.orderTotal || 0 },
      { emoji: '📈', label: '完成率(%)', value: s.finishRate || 0 }
    ]
    await nextTick()
    const trend = d.orderTrend || []
    draw(trendRef.value, {
      grid: { top: 20, right: 20, bottom: 28, left: 40 },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trend.map(x => x.date), boundaryGap: false, axisLabel: { color: C.ink3 } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: C.line } } },
      series: [{ type: 'line', smooth: true, data: trend.map(x => x.count),
        itemStyle: { color: C.brand }, lineStyle: { width: 3, color: C.brand },
        areaStyle: { color: 'rgba(30,79,216,0.10)' } }]
    })
    draw(pieRef.value, { tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: C.ink3, fontSize: 11 } },
      color: [C.brand, C.accent, C.teal, C.green, '#e05c8a', '#9ca3af'],
      series: [{ type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 }, label: { show: false }, data: d.orderStatusDist || [] }] })
    draw(airRef.value, { tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: C.ink3, fontSize: 11 } },
      color: [C.green, C.brand, C.accent, '#9ca3af'],
      series: [{ type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 }, label: { show: false }, data: d.aircraftStatusDist || [] }] })
    draw(vioRef.value, { tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: C.ink3, fontSize: 11 } },
      color: [C.accent, '#e05c8a', C.teal],
      series: [{ type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 }, label: { show: false },
        data: (d.violationDist || []).length ? d.violationDist : [{ name: '暂无违规', value: 1 }] }] })
  } catch (e) { cards.value = [] }
  loading.value = false
})
onBeforeUnmount(() => { charts.forEach(c => c.dispose()); charts.length = 0 })
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.stat-grid { grid-template-columns: repeat(auto-fit, minmax(165px, 1fr)); }
.stat-card { display: flex; align-items: center; gap: 12px; padding: 16px 18px; }
.stat-num { font-family: var(--font-title); font-size: 23px; color: var(--brand-deep); line-height: 1.1; }
.stat-label { font-size: var(--fs-xs); color: var(--ink-3); }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 18px; }
.chart-grid .wide { grid-column: 1 / -1; }
@media (max-width: 900px) { .chart-grid { grid-template-columns: 1fr; } }
.chart-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 10px; }
.chart { height: 240px; }
.todo-card { margin-top: 18px; }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 10px; }
.todo-item { font-size: var(--fs-sm); color: var(--accent-deep); line-height: 1.9; }
</style>

