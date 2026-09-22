<template>
  <div class="side-layout">
    <aside class="sidebar">
      <div class="brand"><span class="emoji">🛰️</span>
        <span class="text">{{ isAdmin ? '平台管控' : '商家运营' }}</span></div>
      <nav class="nav">
        <RouterLink v-for="m in menus" :key="m.path" :to="m.path" class="nav-item"
          :class="{ active: isActive(m.path) }">{{ m.title }}</RouterLink>
      </nav>
    </aside>
    <div class="main">
      <header class="topbar">
        <div class="spacer" />
        <span v-if="showNotice" class="bell" @click="goMessages" title="消息中心">
          🔔<span v-if="unread > 0" class="dot">{{ unread > 99 ? '99+' : unread }}</span>
        </span>
        <span class="role-chip">{{ isAdmin ? '平台管理员' : '物流运营商家' }}</span>
        <span class="user-chip" @click="logout">退出</span>
      </header>
      <main class="content k-page"><RouterView /></main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth } from '@/api/request'
import { unread as fetchUnread } from '@/api/message'

const props = defineProps({
  role: { type: String, default: 'merchant' },
  showNotice: { type: Boolean, default: true }
})
const router = useRouter()
const unread = ref(0)
const isAdmin = computed(() => props.role === 'admin')

const MENUS = {
  merchant: [
    { title: '工作台', path: '/merchant/workbench' },
    { title: '订单调度', path: '/merchant/orders' },
    { title: '低空资源', path: '/merchant/resources' },
    { title: '定价规则', path: '/merchant/pricing' },
    { title: '飞行计划', path: '/merchant/plans' },
    { title: '违规记录', path: '/merchant/violations' },
    { title: '运营数据', path: '/merchant/stats' },
    { title: '消息中心', path: '/merchant/messages' },
    { title: '账号资料', path: '/merchant/profile' }
  ],
  admin: [
    { title: '数据看板', path: '/admin/dashboard' },
    { title: '用户与商家', path: '/admin/users' },
    { title: '订单监管', path: '/admin/orders' },
    { title: '空域与合规', path: '/admin/airspace' },
    { title: '应急与客服', path: '/admin/incidents' },
    { title: '系统配置', path: '/admin/settings' }
  ]
}
const menus = computed(() => MENUS[props.role] || [])

function isActive(path) { return router.currentRoute.value.path.startsWith(path) }
let timer = null
onMounted(() => {
  if (!props.showNotice) return
  const refresh = async () => {
    try { unread.value = (await fetchUnread())?.count || 0 } catch (e) { /* 静默 */ }
  }
  refresh()
  timer = setInterval(refresh, 30000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
function goMessages() { router.push(isAdmin.value ? '/admin/dashboard' : '/merchant/messages') }
function logout() { clearAuth(); router.push('/login') }
</script>

<style scoped>
/* ⚠️ 右侧必须写 minmax(0, 1fr) 而不是 1fr：
   grid 的 1fr 隐含最小尺寸是 auto，会被内容撑开，
   表现为主内容区被压成一条窄列（统计卡片全变成单列堆叠）。
   这与 .main { min-width: 0 } 是两处不同的约束，两者都要有。 */
.side-layout { min-height: 100vh; display: grid; grid-template-columns: 220px minmax(0, 1fr); }
.sidebar { background: var(--navy); color: #fff; padding: 20px 14px; display: flex; flex-direction: column; gap: 18px; }
.brand { display: flex; align-items: center; gap: 8px; padding: 0 8px; }
.brand .emoji { font-size: 24px; }
.brand .text { font-family: var(--font-title); font-size: 17px; }
.nav { display: flex; flex-direction: column; gap: 4px; }
.nav-item { padding: 10px 14px; border-radius: var(--radius-md); font-size: var(--fs-sm); font-weight: 600; color: rgba(255,255,255,0.74); text-decoration: none; transition: background .18s, color .18s; }
.nav-item:hover { background: rgba(255,255,255,0.1); color: #fff; }
.nav-item.active { background: var(--brand); color: #fff; }
.main { display: flex; flex-direction: column; min-width: 0; }
.topbar { height: 62px; display: flex; align-items: center; gap: 14px; padding: 0 24px; background: var(--card); border-bottom: 1px solid var(--line); }
.spacer { flex: 1; }
.bell { position: relative; cursor: pointer; font-size: 19px; line-height: 1; }
.bell .dot { position: absolute; top: -5px; right: -10px; min-width: 17px; height: 17px; padding: 0 4px; border-radius: 999px; background: var(--accent); color: #fff; font-size: 11px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.role-chip { padding: 4px 12px; border-radius: 999px; background: var(--brand-soft); color: var(--brand-deep); font-size: var(--fs-xs); font-weight: 700; }
.user-chip { padding: 6px 14px; border-radius: 999px; background: var(--surface-soft); font-size: var(--fs-sm); font-weight: 600; cursor: pointer; }
.user-chip:hover { color: var(--brand); }
.content { flex: 1; width: 100%; min-width: 0; }
</style>

