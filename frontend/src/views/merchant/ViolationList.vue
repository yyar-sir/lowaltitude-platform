<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>⚠️</span>违规记录</h1>
      <p class="sub">平台记录的空域与作业违规事项，请及时整改。</p></div>
    <el-table :data="list" stripe style="width: 100%">
      <el-table-column prop="violationType" label="违规类型" width="150" />
      <el-table-column prop="detail" label="违规说明" min-width="240" />
      <el-table-column label="等级" width="90">
        <template #default="{ row }">
          <span class="k-tag" :class="levelClass(row.level)">{{ levelName(row.level) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="处理状态" width="110">
        <template #default="{ row }">
          <span class="k-tag" :class="row.handleStatus === 1 ? 'k-tag--green' : 'k-tag--yellow'">
            {{ row.handleStatus === 1 ? '已处理' : '待处理' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="handleResult" label="处理结果" min-width="180" />
      <el-table-column prop="createdAt" label="发生时间" min-width="150" />
    </el-table>
    <el-empty v-if="!loading && !list.length" description="暂无违规记录，保持良好运营 👍" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { violations } from '@/api/airspace'

const loading = ref(false)
const list = ref([])
function levelName(l) { return { 1: '轻微', 2: '一般', 3: '严重' }[l] || '轻微' }
function levelClass(l) { return { 1: '', 2: 'k-tag--orange', 3: 'k-tag--pink' }[l] || '' }
async function load() {
  loading.value = true
  try { list.value = (await violations()) || [] } catch (e) { list.value = [] }
  loading.value = false
}
onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
</style>

