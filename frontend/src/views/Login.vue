<template>
  <div class="login-page">
    <div class="brand-side">
      <div class="brand-inner">
        <span class="logo-emoji">🛰️</span>
        <h1>天枢 · 低空智能运营管控平台</h1>
        <p>低空物流「申请 — 审批 — 调度 — 监控 — 结算」全流程线上管控</p>
        <div class="features">
          <div class="feature"><span>📦</span><div><b>订单全流程</b><i>下单 → 承接 → 调度 → 跟踪 → 结算</i></div></div>
          <div class="feature"><span>🚁</span><div><b>低空资源调度</b><i>飞行器 · 起降场站 · 作业人员</i></div></div>
          <div class="feature"><span>🛡️</span><div><b>空域合规管控</b><i>飞行计划申报审批 · 违规预警</i></div></div>
        </div>
      </div>
    </div>

    <div class="form-side">
      <div class="form-card">
        <h2>{{ isRegister ? '注册账号' : '欢迎回来' }}</h2>
        <p class="sub">{{ isRegister ? '终端客户或物流运营商家均可注册' : '登录后进入对应角色的工作台' }}</p>

        <div class="role-tabs">
          <span :class="{ active: !isRegister }" @click="isRegister = false">登录</span>
          <span :class="{ active: isRegister }" @click="isRegister = true">注册</span>
        </div>

        <el-form class="login-form" @submit.prevent>
          <template v-if="isRegister">
            <el-radio-group v-model="form.role" class="role-pick">
              <el-radio-button :label="2">我是终端客户</el-radio-button>
              <el-radio-button :label="1">我是物流运营商家</el-radio-button>
            </el-radio-group>
            <el-input v-model="form.realName" :placeholder="form.role === 1 ? '企业名称' : '姓名'" size="large" />
            <el-input v-model="form.phone" placeholder="联系电话" size="large" />
            <template v-if="form.role === 1">
              <el-input v-model="form.companyName" placeholder="企业全称（营业执照名称）" size="large" />
              <el-input v-model="form.licenseNo" placeholder="空域运营许可编号" size="large" />
              <div class="reg-tip">商家注册需填写企业名称与运营许可编号，否则无法承接订单</div>
            </template>
          </template>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
          <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large" />
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submit">
            {{ isRegister ? '注册并登录' : '登录' }}
          </el-button>
        </el-form>

        <div class="demo-tip">
          <div class="demo-title">演示账号（密码均为 123456）</div>
          <div class="demo-list">
            <span @click="fill('admin')">平台管理员 admin</span>
            <span @click="fill('merchant01')">物流商家 merchant01</span>
            <span @click="fill('customer01')">终端客户 customer01</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login as loginApi, register as registerApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()

const isRegister = ref(false)
const loading = ref(false)
const form = reactive({ username: '', password: '', realName: '', phone: '', companyName: '', licenseNo: '', role: 2 })

/** 角色 → 首页。必须与 router/index.js 的 ROLE_HOME 保持一致 */
const ROLE_HOME = { 0: '/admin/dashboard', 1: '/merchant/workbench', 2: '/customer/home' }

function fill(name) {
  isRegister.value = false
  form.username = name
  form.password = '123456'
}

/**
 * 登录态落库。
 * 后端 /auth/login 返回的是扁平结构 { token, id, username, ..., role }，
 * 不是 { user: {...} }，按扁平结构解析。
 */
function persistAuth(data) {
  const info = {
    id: data.id,
    username: data.username,
    nickname: data.nickname,
    avatar: data.avatar,
    role: Number(data.role ?? 2),
    realName: data.realName,
    companyName: data.companyName
  }
  localStorage.setItem('token', data.token)
  localStorage.setItem('userInfo', JSON.stringify(info))
  return info
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (isRegister.value && !form.realName) {
    ElMessage.warning('请填写名称')
    return
  }
  if (isRegister.value && form.role === 1 && (!form.companyName || !form.licenseNo)) {
    ElMessage.warning('商家注册需填写企业名称与运营许可编号')
    return
  }
  loading.value = true
  try {
    const data = isRegister.value
      ? await registerApi({ ...form })
      : await loginApi({ username: form.username, password: form.password })
    const info = persistAuth(data)
    ElMessage.success(isRegister.value ? '注册成功' : '欢迎回来，' + (info.companyName || info.nickname || info.username))
    router.push(route.query.redirect || ROLE_HOME[info.role] || '/customer/home')
  } catch (e) {
    // 错误提示已由 request.js 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: 1.15fr 1fr; }
.brand-side { background: var(--navy); color: #fff; display: flex; align-items: center; justify-content: center; }
.brand-inner { max-width: 470px; padding: 40px; }
.logo-emoji { font-size: 40px; }
.brand-logo h1 { font-family: var(--font-title); font-size: 30px; margin: 12px 0 8px; line-height: 1.3; }
.brand-inner h1 { font-family: var(--font-title); font-size: 29px; margin: 12px 0 8px; line-height: 1.3; }
.brand-inner p { color: rgba(255, 255, 255, 0.72); line-height: var(--leading-body); font-size: var(--fs-sm); }
.features { margin-top: 32px; display: flex; flex-direction: column; gap: 16px; }
.feature { display: flex; gap: 12px; align-items: flex-start; }
.feature span { font-size: 22px; }
.feature b { display: block; font-size: var(--fs-sm); }
.feature i { font-style: normal; font-size: var(--fs-xs); color: rgba(255, 255, 255, 0.62); }
.form-side { display: flex; align-items: center; justify-content: center; background: var(--bg); }
.form-card { width: 400px; background: var(--card); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 32px 30px; box-shadow: var(--shadow-card); }
.form-card h2 { margin: 0 0 6px; font-size: var(--fs-h2); }
.sub { color: var(--ink-2); font-size: var(--fs-sm); margin: 0 0 18px; }
.role-tabs { display: flex; gap: 6px; margin-bottom: 18px; }
.role-tabs span { flex: 1; text-align: center; padding: 8px 0; border-radius: var(--radius-sm); font-size: var(--fs-sm); font-weight: 700; color: var(--ink-2); cursor: pointer; background: var(--surface-soft); }
.role-tabs span.active { background: var(--brand); color: #fff; }
.login-form { display: flex; flex-direction: column; gap: 12px; }
.role-pick { width: 100%; }
.submit-btn { width: 100%; }
.reg-tip { font-size: var(--fs-xs); color: var(--ink-3); line-height: 1.6; margin-top: -4px; }
.demo-tip { margin-top: 20px; padding-top: 16px; border-top: 1px dashed var(--line); }
.demo-title { font-size: var(--fs-xs); color: var(--ink-3); margin-bottom: 8px; }
.demo-list { display: flex; flex-wrap: wrap; gap: 8px; }
.demo-list span { padding: 5px 11px; border-radius: 999px; background: var(--surface-soft); font-size: var(--fs-xs); cursor: pointer; transition: all 0.16s; }
.demo-list span:hover { background: var(--brand-soft); color: var(--brand-deep); }
@media (max-width: 900px) { .login-page { grid-template-columns: 1fr; } .brand-side { display: none; } }
</style>

