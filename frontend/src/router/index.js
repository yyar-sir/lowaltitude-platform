/* ============================================================
   路由：三端分离（/customer /merchant /admin）
   终端客户 8 页 / 物流运营商家 8 页 / 平台管理员 5 页
   ============================================================ */
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  { path: '/', redirect: '/customer/home' },

  // ---------- 终端客户：顶部导航 ----------
  {
    path: '/customer',
    component: () => import('@/views/customer/CustomerLayout.vue'),
    redirect: '/customer/home',
    children: [
      { path: 'home', name: 'CustomerHome', component: () => import('@/views/customer/CustomerHome.vue'), meta: { title: '首页' } },
      { path: 'create', name: 'OrderCreate', component: () => import('@/views/customer/OrderCreate.vue'), meta: { title: '创建订单' } },
      { path: 'orders', name: 'CustomerOrders', component: () => import('@/views/customer/OrderList.vue'), meta: { title: '我的订单' } },
      { path: 'track/:id', name: 'CustomerTrack', component: () => import('@/views/customer/OrderTrack.vue'), meta: { title: '订单跟踪' } },
      { path: 'settlement/:id', name: 'CustomerSettlement', component: () => import('@/views/customer/OrderSettlement.vue'), meta: { title: '订单结算' } },
      { path: 'incidents', name: 'CustomerIncidents', component: () => import('@/views/customer/IncidentList.vue'), meta: { title: '反馈与应急' } },
      { path: 'messages', name: 'CustomerMessages', component: () => import('@/views/shared/MessageCenter.vue'), meta: { title: '消息中心' } },
      { path: 'profile', name: 'CustomerProfile', component: () => import('@/views/shared/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },

  // ---------- 物流运营商家：侧栏 ----------
  {
    path: '/merchant',
    component: () => import('@/views/shared/SidebarLayout.vue'),
    props: { role: 'merchant' },
    redirect: '/merchant/workbench',
    children: [
      { path: 'workbench', name: 'MerchantWorkbench', component: () => import('@/views/merchant/Workbench.vue'), meta: { title: '工作台' } },
      { path: 'orders', name: 'MerchantOrders', component: () => import('@/views/merchant/OrderDispatch.vue'), meta: { title: '订单调度' } },
      { path: 'track/:id', name: 'MerchantTrack', component: () => import('@/views/customer/OrderTrack.vue'), meta: { title: '订单跟踪' } },
      { path: 'resources', name: 'MerchantResources', component: () => import('@/views/merchant/ResourceManage.vue'), meta: { title: '低空资源' } },
      { path: 'pricing', name: 'MerchantPricing', component: () => import('@/views/merchant/PricingRule.vue'), meta: { title: '定价规则' } },
      { path: 'plans', name: 'MerchantPlans', component: () => import('@/views/merchant/FlightPlan.vue'), meta: { title: '飞行计划' } },
      { path: 'violations', name: 'MerchantViolations', component: () => import('@/views/merchant/ViolationList.vue'), meta: { title: '违规记录' } },
      { path: 'stats', name: 'MerchantStats', component: () => import('@/views/shared/Dashboard.vue'), meta: { title: '运营数据' } },
      { path: 'messages', name: 'MerchantMessages', component: () => import('@/views/shared/MessageCenter.vue'), meta: { title: '消息中心' } },
      { path: 'profile', name: 'MerchantProfile', component: () => import('@/views/shared/Profile.vue'), meta: { title: '账号资料' } }
    ]
  },

  // ---------- 平台管理员：侧栏（复用同一布局件） ----------
  {
    path: '/admin',
    component: () => import('@/views/shared/SidebarLayout.vue'),
    props: { role: 'admin', showNotice: true },
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue'), meta: { title: '用户与商家' } },
      { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrderMonitor.vue'), meta: { title: '订单监管' } },
      { path: 'airspace', name: 'AdminAirspace', component: () => import('@/views/admin/AirspaceManage.vue'), meta: { title: '空域与合规' } },
      { path: 'incidents', name: 'AdminIncidents', component: () => import('@/views/admin/IncidentManage.vue'), meta: { title: '应急与客服' } },
      { path: 'settings', name: 'AdminSettings', component: () => import('@/views/admin/SystemConfig.vue'), meta: { title: '系统配置' } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/* ============================================================
   登录态与角色守卫
   ------------------------------------------------------------
   为什么必须有这一段：守卫若不拦，未登录访问受保护页时页面会先挂载，
   组件各自打接口，多个请求全部 401，每个都弹一次「请先登录」。

   ⚠️ 前端守卫只是体验层拦截，真正的权限校验在后端
      AuthInterceptor + @RequireRole，前端守卫不能替代后端。
   ============================================================ */
const ROLE_HOME = { 0: '/admin/dashboard', 1: '/merchant/workbench', 2: '/customer/home' }
const ROLE_PREFIX = { 0: '/admin', 1: '/merchant', 2: '/customer' }
const GUARDED_PREFIXES = ['/admin', '/merchant', '/customer']

function currentRole() {
  if (!localStorage.getItem('token')) return null
  try {
    const role = Number(JSON.parse(localStorage.getItem('userInfo') || '{}').role)
    return Number.isInteger(role) && role >= 0 && role <= 2 ? role : 2
  } catch (e) {
    return 2
  }
}

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 天枢低空平台` : '天枢 · 低空智能运营管控平台'

  const role = currentRole()

  // ① 未登录：只放行登录页，其余转登录页并记录回跳地址
  if (role === null) {
    if (to.path === '/login') return true
    const query = to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : {}
    return { path: '/login', query }
  }

  // ② 已登录还去登录页（含退出后手滑回退）→ 回自己角色的首页
  if (to.path === '/login') {
    return ROLE_HOME[role] || '/customer/home'
  }

  // ③ 角色越权（如客户直接敲 /admin/dashboard）→ 送回自己的首页
  const allowed = ROLE_PREFIX[role]
  if (GUARDED_PREFIXES.some((p) => to.path.startsWith(p)) && !to.path.startsWith(allowed)) {
    return ROLE_HOME[role] || '/customer/home'
  }

  return true
})

export default router
