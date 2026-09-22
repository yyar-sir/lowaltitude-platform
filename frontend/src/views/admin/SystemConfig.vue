<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>⚙️</span>系统配置</h1>
      <p class="sub">配置大模型接口（AI 助手能力）并查看平台操作日志。</p></div>
    <el-tabs v-model="tab">
      <el-tab-pane label="AI 接口配置" name="ai">
        <div class="k-card">
          <div class="card-head">
            <div class="side-title">大模型配置</div>
            <span class="k-tag" :class="aiForm.maskedKey ? 'k-tag--green' : 'k-tag--yellow'">
              {{ aiForm.maskedKey ? '已配置' : '未配置' }}</span>
          </div>
          <el-alert v-if="!aiForm.maskedKey" type="warning" :closable="false" show-icon style="margin-bottom: 16px">
            <template #title>尚未配置 API Key</template>
            未配置时 AI 问答与合规检查会返回业务码 1101（前端提示去配置），
            <b>但订单、调度、跟踪、结算等其他功能完全不受影响</b>；
            调度建议页也会自动回退为不依赖模型的规则建议。
          </el-alert>
          <div class="ai-grid">
            <div class="field"><label>服务商</label><el-input v-model="aiForm.provider" placeholder="deepseek" /></div>
            <div class="field"><label>Base URL</label><el-input v-model="aiForm.baseUrl" placeholder="https://api.deepseek.com/v1" /></div>
            <div class="field"><label>模型</label><el-input v-model="aiForm.model" placeholder="deepseek-chat" /></div>
            <div class="field"><label>API Key（留空表示不修改）</label>
              <el-input v-model="aiForm.apiKey" show-password :placeholder="aiForm.maskedKey || '未配置'" /></div>
            <div class="field"><label>超时（秒）</label>
              <el-input-number v-model="aiForm.timeoutSec" :min="5" :max="180" style="width: 100%" /></div>
            <div class="field"><label>每分钟限流（次/人）</label>
              <el-input-number v-model="aiForm.rateLimitPerMin" :min="1" :max="200" style="width: 100%" /></div>
            <div class="field"><label>内容安全过滤</label>
              <el-switch v-model="aiForm.contentFilter" :active-value="1" :inactive-value="0" /></div>
          </div>
          <div class="ai-actions">
            <button class="k-btn" :disabled="saving" @click="saveAi">保存配置</button>
            <span class="hint">已保存的 Key 只回显后 4 位：{{ aiForm.maskedKey || '（未配置）' }}</span>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <el-table :data="logs" stripe style="width: 100%">
          <el-table-column prop="opType" label="操作类型" width="160" />
          <el-table-column prop="detail" label="详情" min-width="280" />
          <el-table-column label="操作人" width="150">
            <template #default="{ row }">
              <span class="k-tag" :class="roleClass(row.operatorRole)">{{ roleName(row.operatorRole) }}</span>
              <span class="uid">#{{ row.operatorId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="createdAt" label="时间" min-width="170" />
        </el-table>
        <el-empty v-if="!logs.length" description="暂无操作日志" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { logs as fetchLogs } from '@/api/admin'
import { config as fetchAiConfig, updateConfig } from '@/api/ai'

const loading = ref(false)
const saving = ref(false)
const tab = ref('ai')
const logs = ref([])
const aiForm = reactive({ provider: 'deepseek', baseUrl: '', model: 'deepseek-chat', apiKey: '', maskedKey: '', timeoutSec: 60, rateLimitPerMin: 20, contentFilter: 1 })

function roleName(r) { return { 0: '管理员', 1: '商家', 2: '客户' }[r] || '系统' }
function roleClass(r) { return { 0: 'k-tag--orange', 1: 'k-tag--teal', 2: '' }[r] || '' }

async function saveAi() {
  saving.value = true
  try {
    // apiKey 传空串 → 后端保留原值，不会误清空
    const r = await updateConfig({ ...aiForm })
    aiForm.apiKey = ''
    aiForm.maskedKey = r?.apiKey || ''
    ElMessage.success('AI 配置已保存')
  } catch (e) { /* 已提示 */ } finally { saving.value = false }
}
onMounted(async () => {
  loading.value = true
  try {
    const c = await fetchAiConfig()
    Object.assign(aiForm, {
      provider: c?.provider || 'deepseek', baseUrl: c?.baseUrl || '', model: c?.model || 'deepseek-chat',
      maskedKey: c?.apiKey || '', timeoutSec: c?.timeoutSec || 60,
      rateLimitPerMin: c?.rateLimitPerMin || 20, contentFilter: c?.contentFilter ?? 1
    })
  } catch (e) { /* 已提示 */ }
  try { logs.value = (await fetchLogs()) || [] } catch (e) { logs.value = [] }
  loading.value = false
})
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.side-title { font-weight: 700; font-size: var(--fs-sm); }
.ai-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
.field label { display: block; font-size: var(--fs-xs); color: var(--ink-2); font-weight: 600; margin-bottom: 6px; }
.ai-actions { display: flex; align-items: center; gap: 14px; margin-top: 18px; }
.hint { font-size: var(--fs-xs); color: var(--ink-3); }
.uid { font-size: var(--fs-xs); color: var(--ink-3); margin-left: 6px; }
</style>

