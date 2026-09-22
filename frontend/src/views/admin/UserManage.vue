<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>👥</span>用户与商家</h1>
      <p class="sub">管理平台管理员、物流运营商家与终端客户账号，可审核资质、冻结账号、重置密码。</p></div>
    <div class="k-card bar">
      <el-input v-model="query.keyword" placeholder="用户名 / 姓名 / 企业" clearable style="max-width: 220px" @keyup.enter="load" />
      <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 150px" @change="load">
        <el-option label="平台管理员" :value="0" />
        <el-option label="物流运营商家" :value="1" />
        <el-option label="终端客户" :value="2" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="load">
        <el-option label="启用" :value="1" /><el-option label="禁用" :value="0" />
      </el-select>
      <button class="k-btn" style="padding: 8px 18px" @click="load">查询</button>
      <div class="spacer" />
      <button class="k-btn k-btn--orange" style="padding: 8px 18px" @click="open()">+ 新增用户</button>
    </div>
    <el-table :data="list" stripe style="width: 100%; margin-top: 16px">
      <el-table-column label="账号" min-width="180">
        <template #default="{ row }">
          <span class="avatar">{{ row.avatar || '🙂' }}</span>
          <span class="name">{{ row.realName }}</span>
          <span class="uname">@{{ row.username }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="130">
        <template #default="{ row }">
          <span class="k-tag" :class="roleClass(row.role)">{{ roleName(row.role) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="companyName" label="企业名称" min-width="170" />
      <el-table-column prop="licenseNo" label="运营许可" min-width="150" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span class="k-tag" :class="row.status === 1 ? 'k-tag--green' : 'k-tag--pink'">
            {{ row.status === 1 ? '启用' : '禁用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="open(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggle(row)">
            {{ row.status === 1 ? '冻结' : '解冻' }}</el-button>
          <el-button size="small" plain @click="resetPwd(row)">重置密码</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !list.length" description="没有符合条件的用户" />

    <el-dialog v-model="visible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form label-width="110px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password :placeholder="form.id ? '留空表示不修改' : '默认 123456'" />
        </el-form-item>
        <el-form-item label="姓名/企业"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="平台管理员" :value="0" />
            <el-option label="物流运营商家" :value="1" />
            <el-option label="终端客户" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <template v-if="form.role === 1">
          <el-form-item label="企业名称"><el-input v-model="form.companyName" /></el-form-item>
          <el-form-item label="运营许可编号"><el-input v-model="form.licenseNo" /></el-form-item>
        </template>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createUser, deleteUser, resetPassword, toggleUserStatus, updateUser, users } from '@/api/admin'

const loading = ref(false)
const list = ref([])
const visible = ref(false)
const query = reactive({ keyword: '', role: null, status: null })
const form = reactive({ id: null, username: '', password: '', realName: '', role: 2, phone: '', companyName: '', licenseNo: '', status: 1 })

function roleName(r) { return { 0: '平台管理员', 1: '物流运营商家', 2: '终端客户' }[r] || '终端客户' }
function roleClass(r) { return { 0: 'k-tag--orange', 1: 'k-tag--teal', 2: '' }[r] || '' }

function open(row) {
  Object.assign(form, row
    ? { ...row, password: '' }
    : { id: null, username: '', password: '', realName: '', role: 2, phone: '', companyName: '', licenseNo: '', status: 1 })
  visible.value = true
}
async function save() {
  if (!form.id && !form.username) { ElMessage.warning('请填写用户名'); return }
  try {
    if (form.id) await updateUser(form.id, { ...form })
    else await createUser({ ...form })
    ElMessage.success(form.id ? '已保存' : '用户已创建（默认密码 123456）')
    visible.value = false
    await load()
  } catch (e) { /* 已提示 */ }
}
async function toggle(row) {
  const action = row.status === 1 ? '冻结' : '解冻'
  try { await ElMessageBox.confirm('确定' + action + '账号 ' + row.username + ' 吗？', action, { type: 'warning' }) }
  catch (e) { return }
  try { await toggleUserStatus(row.id, row.status === 1 ? 0 : 1); ElMessage.success(action + '成功'); await load() }
  catch (e) { /* 已提示 */ }
}
async function resetPwd(row) {
  try { await ElMessageBox.confirm('确定把 ' + row.username + ' 的密码重置为 123456 吗？', '重置密码', { type: 'warning' }) }
  catch (e) { return }
  try { const r = await resetPassword(row.id); ElMessage.success('已重置，新密码：' + r.newPassword) }
  catch (e) { /* 已提示 */ }
}
async function remove(row) {
  try { await ElMessageBox.confirm('删除后不可恢复，确定删除 ' + row.username + ' 吗？', '删除用户', { type: 'warning' }) }
  catch (e) { return }
  try { await deleteUser(row.id); ElMessage.success('已删除'); await load() } catch (e) { /* 已提示 */ }
}
async function load() {
  loading.value = true
  try { list.value = (await users({ ...query, keyword: query.keyword || undefined }))?.list || [] }
  catch (e) { list.value = [] }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.bar { display: flex; align-items: center; gap: 10px; padding: 14px 18px; flex-wrap: wrap; }
.bar .spacer { flex: 1; }
.avatar { margin-right: 6px; }
.name { font-weight: 600; }
.uname { font-size: var(--fs-xs); color: var(--ink-3); margin-left: 6px; }
</style>

