<template>
  <div class="cust-layout">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="logo" @click="$router.push('/customer/home')">
          <span class="logo-emoji">🛰️</span>
          <span class="logo-text">天枢低空</span>
        </div>
        <nav class="menus">
          <RouterLink v-for="m in menus" :key="m.path" :to="m.path" class="menu-item"
            :class="{ active: isActive(m.path) }">{{ m.title }}</RouterLink>
        </nav>
        <div class="right-area">
          <span class="bell" @click="$router.push('/customer/messages')" title="消息中心">
            🔔<span v-if="unread > 0" class="dot">{{ unread > 99 ? '99+' : unread }}</span>
          </span>
          <span class="role-chip">终端客户</span>
          <span class="user-chip" @click="logout">退出</span>
        </div>
      </div>
    </header>
    <main class="content k-page"><RouterView /></main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth } from '@/api/request'
import { unread as fetchUnread } from '@/api/message'

const router = useRouter()
const unread = ref(0)

/* 客户端的核心动作是「下单」与「看订单进度」，导航按这个顺序排 */
const menus = [
  { title: '首页', path: '/customer/home' },
  { title: '创建订单', path: '/customer/create' },
  { title: '我的订单', path: '/customer/orders' },
  { title: '反馈与应急', path: '/customer/incidents' },
  { title: '消息', path: '/customer/messages' },
  { title: '个人中心', path: '/customer/profile' }
]

function isActive(path) {
  const cur = router.currentRoute.value.path
  return path === '/customer/home' ? cur === path : cur.startsWith(path)
}

let timer = null
async function refreshUnread() {
  try {
    const d = await fetchUnread()
    unread.value = d?.count || 0
  } catch (e) {
    // 未读数失败不影响页面主体
  }
}
onMounted(() => {
  refreshUnread()
  timer = setInterval(refreshUnread, 30000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.cust-layout { min-height: 100vh; display: flex; flex-direction: column; }
.topbar { position: sticky; top: 0; z-index: 100; background: rgba(255,255,255,0.94); backdrop-filter: blur(12px); border-bottom: 1px solid var(--line); }
.topbar-inner { max-width: 1280px; margin: 0 auto; padding: 0 20px; height: 66px; display: flex; align-items: center; gap: 12px; }
.logo { display: flex; align-items: center; gap: 8px; cursor: pointer; flex-shrink: 0; }
.logo-emoji { font-size: 26px; }
.logo-text { font-family: var(--font-title); font-size: 19px; color: var(--brand-deep); }
.menus { display: flex; gap: 4px; flex: 1; justify-content: center; flex-wrap: wrap; }
.menu-item { padding: 8px 14px; border-radius: var(--radius-md); font-size: var(--fs-sm); font-weight: 600; white-space: nowrap; color: var(--ink-2); text-decoration: none; transition: color .18s, background .18s; }
.menu-item:hover { color: var(--brand); background: var(--brand-soft); }
.menu-item.active { color: #fff; background: var(--brand); }
.right-area { flex-shrink: 0; display: flex; align-items: center; gap: 10px; }
.bell { position: relative; cursor: pointer; font-size: 19px; line-height: 1; }
.bell .dot { position: absolute; top: -5px; right: -10px; min-width: 17px; height: 17px; padding: 0 4px; border-radius: 999px; background: var(--accent); color: #fff; font-size: 11px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.role-chip { padding: 4px 12px; border-radius: 999px; background: var(--brand-soft); color: var(--brand-deep); font-size: var(--fs-xs); font-weight: 700; }
.user-chip { padding: 6px 14px; border-radius: 999px; background: var(--surface-soft); font-size: var(--fs-sm); font-weight: 600; cursor: pointer; }
.user-chip:hover { color: var(--brand); }
.content { flex: 1; }
</style>

