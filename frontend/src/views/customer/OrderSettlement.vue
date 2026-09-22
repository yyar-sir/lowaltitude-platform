<template>
  <div v-loading="loading">
    <div class="page-head"><h1 class="k-h1-icon"><span>💰</span>订单结算</h1>
      <p class="sub">核对费用明细，完成支付并评价本次配送服务。</p></div>

    <div class="settle-grid">
      <div class="k-card">
        <div class="side-title">费用明细</div>
        <div class="fee-row"><span>订单号</span><b>{{ order.orderNo }}</b></div>
        <div class="fee-row"><span>配送路线</span><span>{{ order.sendAddress }} → {{ order.receiveAddress }}</span></div>
        <div class="fee-row"><span>物品</span><span>{{ order.goodsType }} · {{ order.goodsWeight }}kg</span></div>
        <div class="fee-row"><span>航程</span><span>{{ order.distanceKm }} km</span></div>
        <div class="fee-row"><span>预估费用</span><span>￥{{ order.estimateFee }}</span></div>
        <el-divider />
        <div class="fee-row total"><span>应付金额</span><b>￥{{ order.actualFee || order.estimateFee }}</b></div>

        <div v-if="!settled">
          <div class="side-title" style="margin-top: 18px">支付方式</div>
          <el-radio-group v-model="form.payType">
            <el-radio :label="1">微信支付</el-radio>
            <el-radio :label="2">支付宝</el-radio>
            <el-radio :label="3">对公转账</el-radio>
          </el-radio-group>
          <button class="k-btn full" style="margin-top: 16px" :disabled="submitting" @click="submit">确认支付</button>
        </div>
        <el-alert v-else type="success" :closable="false" show-icon style="margin-top: 16px"
          title="该订单已完成结算" />
      </div>

      <div class="k-card">
        <div class="side-title">服务评价</div>
        <div v-if="settled && settlement.score">
          <div class="score-big">{{ settlement.score }} 分</div>
          <div class="comment">{{ settlement.comment || '未填写评价内容' }}</div>
          <div class="paid">支付流水号：{{ settlement.payNo }}</div>
        </div>
        <template v-else>
          <div class="field"><label>配送时效评分</label>
            <el-rate v-model="form.score" :max="5" show-score /></div>
          <div class="field"><label>评价内容（选填）</label>
            <el-input v-model="form.comment" type="textarea" :rows="3" maxlength="200" show-word-limit /></div>
          <div class="tip">评价会随结算记录一并保存，用于平台服务质量统计</div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { detail, settle, settlement as fetchSettlement } from '@/api/order'

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const order = ref({})
const settlement = ref({})
const settled = ref(false)
const form = reactive({ payType: 1, score: 5, comment: '' })

async function load() {
  loading.value = true
  try {
    order.value = (await detail(route.params.id)) || {}
    const s = await fetchSettlement(route.params.id)
    if (s) { settlement.value = s; settled.value = true }
  } catch (e) { /* 未结算时接口返回 null 是正常的 */ }
  loading.value = false
}

async function submit() {
  submitting.value = true
  try {
    const s = await settle({ orderId: Number(route.params.id), ...form })
    settlement.value = s
    settled.value = true
    ElMessage.success('支付成功，感谢评价！')
  } catch (e) { /* 已提示 */ } finally { submitting.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.settle-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 18px; align-items: start; }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 14px; }
.fee-row { display: flex; align-items: center; justify-content: space-between; padding: 7px 0; font-size: var(--fs-sm); }
.fee-row span:first-child { color: var(--ink-2); }
.fee-row.total b { font-family: var(--font-title); font-size: 21px; color: var(--accent-deep); }
.full { width: 100%; }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: var(--fs-xs); color: var(--ink-2); font-weight: 600; margin-bottom: 6px; }
.score-big { font-family: var(--font-title); font-size: 34px; color: var(--accent-deep); }
.comment { font-size: var(--fs-sm); color: var(--ink-2); margin-top: 8px; }
.paid { font-size: var(--fs-xs); color: var(--ink-3); margin-top: 10px; }
.tip { font-size: var(--fs-xs); color: var(--ink-3); }
</style>

