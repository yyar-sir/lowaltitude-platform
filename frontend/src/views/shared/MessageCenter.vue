<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>🔔</span>消息中心</h1>
      <p class="sub">订单状态变更、计划审批结果、违规与事件通知都在这里。</p></div>
    <div class="k-card bar">
      <el-radio-group v-model="filter" size="small" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="order">订单</el-radio-button>
        <el-radio-button label="plan">计划审批</el-radio-button>
        <el-radio-button label="violation">违规</el-radio-button>
        <el-radio-button label="info">系统</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <span class="unread">未读 {{ unreadCount }}</span>
      <button class="k-btn k-btn--ghost" style="padding: 7px 16px" :disabled="!unreadCount" @click="readAll">全部已读</button>
    </div>
    <div class="list">
      <div v-for="m in list" :key="m.id" class="k-card item" :class="{ unread: m.isRead === 0 }" @click="openItem(m)">
        <div class="icon-box"><span>{{ typeEmoji(m.type) }}</span></div>
        <div class="main">
          <div class="title-row"><span class="title">{{ m.title }}</span>
            <span v-if="m.isRead === 0" class="dot" /></div>
          <div class="content">{{ m.content }}</div>
        </div>
        <div class="time">{{ (m.createdAt || '').slice(5, 16) }}</div>
      </div>
      <el-empty v-if="!loading && !list.length" description="暂时没有消息" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { list as fetchList, markAllRead, markRead, unread } from '@/api/message'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const filter = ref('')
const unreadCount = ref(0)

function typeEmoji(t) { return { order: '📦', plan: '🛡️', violation: '⚠️', announce: '📢', info: '💬' }[t] || '💬' }
async function load() {
  loading.value = true
  try {
    list.value = (await fetchList(filter.value || undefined)) || []
    unreadCount.value = (await unread())?.count || 0
  } catch (e) { list.value = [] }
  loading.value = false
}
async function openItem(m) {
  if (m.isRead === 0) {
    try { await markRead(m.id); m.isRead = 1; unreadCount.value = Math.max(0, unreadCount.value - 1) } catch (e) { /* 静默 */ }
  }
  if (m.link) router.push(m.link)
}
async function readAll() {
  try {
    await markAllRead()
    list.value.forEach(m => { m.isRead = 1 })
    unreadCount.value = 0
  } catch (e) { /* 静默 */ }
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.bar { display: flex; align-items: center; gap: 12px; padding: 12px 18px; }
.bar .spacer { flex: 1; }
.unread { font-size: var(--fs-sm); color: var(--accent-deep); font-weight: 600; }
.list { display: flex; flex-direction: column; gap: 10px; margin-top: 16px; }
.item { display: flex; align-items: flex-start; gap: 14px; padding: 16px 20px; cursor: pointer; }
.item.unread { background: #fbfdff; border-left: 3px solid var(--brand); }
.main { flex: 1; min-width: 0; }
.title-row { display: flex; align-items: center; gap: 8px; }
.title { font-weight: 600; font-size: var(--fs-sm); }
.dot { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); }
.content { font-size: var(--fs-xs); color: var(--ink-2); margin-top: 4px; }
.time { font-size: var(--fs-xs); color: var(--ink-3); white-space: nowrap; }
</style>

