<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>👤</span>账号资料</h1>
      <p class="sub">维护账号信息与通知设置，可修改登录密码。</p></div>
    <div class="profile-grid">
      <div class="k-card">
        <div class="side-title">基本信息</div>
        <div class="avatar-row"><div class="avatar">{{ form.avatar }}</div>
          <div><div class="name">{{ form.realName }}</div><div class="uname">@{{ form.username }}</div></div></div>
        <div class="field"><label>显示名称</label><el-input v-model="form.nickname" maxlength="20" /></div>
        <div class="field"><label>联系电话</label><el-input v-model="form.phone" maxlength="20" /></div>
        <div v-if="isMerchant" class="field"><label>企业名称</label><el-input v-model="form.companyName" /></div>
        <div v-if="isMerchant" class="field"><label>运营许可编号</label><el-input :model-value="form.licenseNo" disabled /></div>
        <div class="field switch-field"><label>接收站内消息</label>
          <el-switch v-model="form.notifyEnabled" :active-value="1" :inactive-value="0" /></div>
        <button class="k-btn full" :disabled="saving" @click="save">保存资料</button>
      </div>
      <div class="k-card">
        <div class="side-title">修改密码</div>
        <div class="field"><label>原密码</label><el-input v-model="pwd.oldPassword" type="password" show-password /></div>
        <div class="field"><label>新密码（6-20 位）</label><el-input v-model="pwd.newPassword" type="password" show-password /></div>
        <div class="field"><label>确认新密码</label><el-input v-model="pwd.confirm" type="password" show-password /></div>
        <button class="k-btn k-btn--orange full" :disabled="changing" @click="changePwd">修改密码</button>
        <div class="tip">修改后其他设备上的登录会失效，需要重新登录。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword, profile as fetchProfile, updateProfile } from '@/api/user'

const loading = ref(false)
const saving = ref(false)
const changing = ref(false)
const form = reactive({ username: '', realName: '', nickname: '', avatar: '🙂', phone: '', companyName: '', licenseNo: '', notifyEnabled: 1 })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const isMerchant = computed(() => Number(JSON.parse(localStorage.getItem('userInfo') || '{}').role) === 1)

async function save() {
  saving.value = true
  try {
    const u = await updateProfile({ ...form })
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    localStorage.setItem('userInfo', JSON.stringify({ ...info, nickname: u.nickname, avatar: u.avatar }))
    ElMessage.success('资料已保存')
  } catch (e) { /* 已提示 */ } finally { saving.value = false }
}
async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) { ElMessage.warning('请填写原密码与新密码'); return }
  if (pwd.newPassword !== pwd.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }
  try { await ElMessageBox.confirm('修改后其他设备需重新登录，确定继续吗？', '确认修改', { type: 'warning' }) } catch (e) { return }
  changing.value = true
  try {
    await changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    setTimeout(() => {
      localStorage.removeItem('token'); localStorage.removeItem('userInfo')
      window.location.href = '/login'
    }, 1200)
  } catch (e) { /* 已提示 */ } finally { changing.value = false }
}
onMounted(async () => {
  loading.value = true
  try {
    const u = await fetchProfile()
    Object.assign(form, {
      username: u.username, realName: u.realName, nickname: u.nickname, avatar: u.avatar,
      phone: u.phone, companyName: u.companyName, licenseNo: u.licenseNo,
      notifyEnabled: u.notifyEnabled ?? 1
    })
  } catch (e) { /* 已提示 */ }
  loading.value = false
})
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.profile-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 18px; align-items: start; }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 14px; }
.avatar-row { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.avatar { width: 60px; height: 60px; border-radius: 50%; background: var(--brand-soft); display: flex; align-items: center; justify-content: center; font-size: 30px; }
.name { font-family: var(--font-title); font-size: var(--fs-h3); }
.uname { font-size: var(--fs-xs); color: var(--ink-3); }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: var(--fs-xs); color: var(--ink-2); font-weight: 600; margin-bottom: 6px; }
.switch-field { display: flex; align-items: center; justify-content: space-between; }
.switch-field label { margin-bottom: 0; }
.full { width: 100%; }
.tip { margin-top: 10px; font-size: var(--fs-xs); color: var(--ink-3); }
</style>

