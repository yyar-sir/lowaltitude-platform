<template>
  <div v-loading="loading">
    <div class="page-head">
      <h1 class="k-h1-icon"><span>🛰️</span>订单跟踪</h1>
      <p class="sub">实时查看订单状态与飞行轨迹。</p>
    </div>

    <div class="track-grid">
      <div class="k-card">
        <div class="side-title">订单信息</div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <span class="k-tag">{{ order.orderStatusName }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="寄件地址">{{ order.sendAddress }}</el-descriptions-item>
          <el-descriptions-item label="收件地址">{{ order.receiveAddress }}</el-descriptions-item>
          <el-descriptions-item label="物品">{{ order.goodsType }} · {{ order.goodsWeight }}kg</el-descriptions-item>
          <el-descriptions-item label="时效">{{ order.deliveryTypeName }} · 优先级{{ order.priorityName }}</el-descriptions-item>
          <el-descriptions-item label="航程">{{ order.distanceKm }} km</el-descriptions-item>
          <el-descriptions-item label="费用">￥{{ order.actualFee || order.estimateFee }}</el-descriptions-item>
        </el-descriptions>

        <div class="side-title" style="margin-top: 18px">状态流转</div>
        <div class="timeline">
          <div v-for="(t, i) in order.timeline || []" :key="i" class="tl-node" :class="{ done: t.done }">
            <span class="tl-dot" />
            <div class="tl-body"><div class="tl-label">{{ t.label }}</div><div class="tl-time">{{ t.time || '—' }}</div></div>
          </div>
        </div>
      </div>

      <div>
        <div class="k-card">
          <div class="side-title">调度与飞行器</div>
          <el-descriptions v-if="order.aircraftNo" :column="1" border size="small">
            <el-descriptions-item label="飞行器">{{ order.aircraftNo }}</el-descriptions-item>
            <el-descriptions-item label="机长">{{ order.pilotName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="航线">{{ order.flightPath }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="尚未生成调度方案" :image-size="60" />
        </div>

        <div class="k-card" style="margin-top: 14px">
          <div class="side-title">飞行轨迹（{{ tracks.length }} 个跟踪点）</div>
          <div class="track-canvas">
            <svg viewBox="0 0 300 160" class="svg">
              <polyline :points="polyline" fill="none" stroke="var(--brand)" stroke-width="2" />
              <circle v-for="(p, i) in points" :key="i" :cx="p.x" :cy="p.y" r="3" fill="var(--accent)" />
            </svg>
          </div>
          <div v-for="t in anomalyTracks" :key="t.id" class="anomaly">
            ⚠️ {{ t.abnormalDesc || '飞行异常' }}（{{ (t.collectTime || '').slice(5, 16) }}）
          </div>
          <el-empty v-if="!tracks.length" description="暂无跟踪数据" :image-size="60" />
        </div>

        <div class="k-card" style="margin-top: 14px">
          <div class="side-title">进度推进</div>
          <div v-if="isMerchant && order.orderStatus === 3">
            <button class="k-btn k-btn--ghost full" @click="simulate">模拟推进飞行</button>
            <button class="k-btn full" style="margin-top: 9px" @click="doArrive">标记已送达</button>
          </div>
          <div v-else-if="isCustomer && order.orderStatus === 4">
            <button class="k-btn" @click="$router.push('/customer/settlement/' + order.id)">去结算并评价</button>
          </div>
          <div v-else class="muted">当前状态无需操作</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { arrive, detail, simulateFlight, tracks as fetchTracks } from '@/api/order'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref({})
const tracks = ref([])
const user = JSON.parse(localStorage.getItem('userInfo') || '{}')
const isMerchant = computed(() => Number(user.role) === 1)
const isCustomer = computed(() => Number(user.role) === 2)

/** 把经纬度映射到 SVG 坐标（演示用线性映射） */
const points = computed(() => {
  if (!tracks.value.length) return []
  const lngs = tracks.value.map(t => Number(t.longitude))
  const lats = tracks.value.map(t => Number(t.latitude))
  const minLng = Math.min(...lngs), maxLng = Math.max(...lngs)
  const minLat = Math.min(...lats), maxLat = Math.max(...lats)
  const spanLng = maxLng - minLng || 0.001
  const spanLat = maxLat - minLat || 0.001
  return tracks.value.map(t => ({
    x: 20 + ((Number(t.longitude) - minLng) / spanLng) * 260,
    y: 140 - ((Number(t.latitude) - minLat) / spanLat) * 120
  }))
})
const polyline = computed(() => points.value.map(p => p.x + ',' + p.y).join(' '))
const anomalyTracks = computed(() => tracks.value.filter(t => t.abnormalType && t.abnormalType !== 0))

async function load() {
  loading.value = true
  try {
    order.value = (await detail(route.params.id)) || {}
    tracks.value = (await fetchTracks(route.params.id)) || []
  } catch (e) { /* 已提示 */ }
  loading.value = false
}

async function simulate() {
  try {
    await simulateFlight(order.value.id)
    ElMessage.success('已补充飞行跟踪点')
    await load()
  } catch (e) { /* 已提示 */ }
}

async function doArrive() {
  try {
    await arrive(order.value.id)
    ElMessage.success('已标记送达，等待客户结算')
    await load()
  } catch (e) { /* 已提示 */ }
}

let timer = null
onMounted(() => {
  load()
  // 飞行中每 15 秒刷新一次轨迹，模拟实时监控
  timer = setInterval(() => {
    if (order.value.orderStatus === 3) load()
  }, 15000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.track-grid { display: grid; grid-template-columns: 380px minmax(0,1fr); gap: 18px; align-items: start; }
@media (max-width: 1000px) { .track-grid { grid-template-columns: 1fr; } }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 12px; }
.timeline { padding-left: 6px; }
.tl-node { display: flex; gap: 10px; padding-bottom: 14px; position: relative; }
.tl-node:not(:last-child)::before { content: ''; position: absolute; left: 4px; top: 14px; bottom: 0; width: 1px; background: var(--line); }
.tl-dot { width: 9px; height: 9px; border-radius: 50%; background: var(--line); margin-top: 5px; flex-shrink: 0; z-index: 1; }
.tl-node.done .tl-dot { background: var(--brand); }
.tl-label { font-size: var(--fs-sm); font-weight: 600; }
.tl-time { font-size: var(--fs-xs); color: var(--ink-3); }
.track-canvas { background: var(--surface-soft); border-radius: var(--radius-md); padding: 8px; }
.svg { width: 100%; height: 160px; }
.anomaly { margin-top: 9px; padding: 8px 12px; border-radius: var(--radius-sm); background: var(--accent-soft); color: var(--accent-deep); font-size: var(--fs-xs); }
.full { width: 100%; }
.muted { color: var(--ink-3); font-size: var(--fs-sm); }
</style>

