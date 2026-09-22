<template>
  <div>
    <div class="page-head">
      <h1 class="k-h1-icon"><span>📦</span>创建订单</h1>
      <p class="sub">填写寄收地址与物品信息，系统会校验空域范围并预估费用。</p>
    </div>

    <div class="create-grid">
      <div class="k-card">
        <div class="side-title">订单信息</div>
        <el-form label-position="top">
          <el-form-item label="寄件地址"><el-input v-model="form.sendAddress" placeholder="如：高新区科技园A座" /></el-form-item>
          <el-form-item label="收件地址">
            <el-input v-model="form.receiveAddress" placeholder="如：经开区创新大厦B座" />
            <div class="tip">收件地址不能位于禁飞区或临时管制区内</div>
          </el-form-item>
          <el-form-item label="物品类型">
            <el-select v-model="form.goodsType" style="width: 100%">
              <el-option v-for="t in goodsTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="物品重量（kg）">
            <el-input-number v-model="form.goodsWeight" :min="0.1" :max="50" :step="0.5" style="width: 100%" />
          </el-form-item>
          <el-form-item label="配送时效">
            <el-radio-group v-model="form.deliveryType">
              <el-radio :label="1">普通配送</el-radio>
              <el-radio :label="2">加急配送</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="优先级">
            <el-radio-group v-model="form.priority">
              <el-radio :label="1">普通</el-radio>
              <el-radio :label="2">较高</el-radio>
              <el-radio :label="3">紧急</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="100" /></el-form-item>
          <button class="k-btn full" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中…' : '提交订单' }}
          </button>
        </el-form>
      </div>

      <div>
        <div class="k-card">
          <div class="side-title">空域提示</div>
          <div v-for="z in zones" :key="z.id" class="zone" :class="zoneClass(z.zoneType)">
            <span class="zone-name">{{ z.name }}</span>
            <span class="k-tag" :class="zoneTag(z.zoneType)">{{ zoneTypeName(z.zoneType) }}</span>
          </div>
          <el-empty v-if="!zones.length" description="暂无空域数据" :image-size="60" />
        </div>
        <div class="k-card" style="margin-top: 14px">
          <div class="side-title">下单说明</div>
          <ol class="rules">
            <li>提交后系统自动校验收件地址是否在服务空域内</li>
            <li>校验通过立即生成订单，状态为「待接单」</li>
            <li>商家接单并完成调度后，可在「我的订单」实时查看飞行轨迹</li>
            <li>送达后进入结算页完成支付与评价</li>
          </ol>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { create } from '@/api/order'
import { zones as fetchZones } from '@/api/airspace'

const router = useRouter()
const submitting = ref(false)
const zones = ref([])
const goodsTypes = ['文件类', '生鲜冷链', '小型配件', '医疗物资', '日用百货', '其他']

const form = reactive({
  sendAddress: '', receiveAddress: '', goodsType: '文件类',
  goodsWeight: 2, deliveryType: 1, priority: 1, remark: ''
})

function zoneTypeName(t) {
  return { 1: '可用空域', 2: '禁飞区', 3: '临时管制区' }[t] || '未知'
}
function zoneTag(t) {
  return { 1: 'k-tag--green', 2: 'k-tag--pink', 3: 'k-tag--orange' }[t] || ''
}
function zoneClass(t) {
  return { 1: 'ok', 2: 'danger', 3: 'warn' }[t] || ''
}

async function submit() {
  if (!form.sendAddress || !form.receiveAddress) {
    ElMessage.warning('请填写寄件与收件地址')
    return
  }
  submitting.value = true
  try {
    const order = await create({ ...form })
    ElMessage.success('下单成功，订单号 ' + order.orderNo)
    router.push('/customer/orders')
  } catch (e) {
    // 空域校验失败等原因由拦截器提示
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try { zones.value = (await fetchZones({ status: 1 })) || [] } catch (e) { zones.value = [] }
})
</script>

<style scoped>
.page-head h1 { margin: 0 0 6px; }
.page-head .sub { color: var(--ink-3); font-size: var(--fs-sm); margin: 0 0 18px; }
.create-grid { display: grid; grid-template-columns: minmax(0,1fr) 320px; gap: 18px; align-items: start; }
@media (max-width: 1000px) { .create-grid { grid-template-columns: 1fr; } }
.side-title { font-weight: 700; font-size: var(--fs-sm); margin-bottom: 14px; }
.full { width: 100%; }
.tip { font-size: var(--fs-xs); color: var(--ink-3); margin-top: 5px; }
.zone { display: flex; align-items: center; justify-content: space-between; padding: 9px 12px; border-radius: var(--radius-sm); margin-bottom: 8px; font-size: var(--fs-sm); }
.zone.ok { background: #e4f4ec; }
.zone.danger { background: #fceaf1; }
.zone.warn { background: var(--accent-soft); }
.rules { margin: 0; padding-left: 18px; font-size: var(--fs-sm); color: var(--ink-2); line-height: 1.9; }
</style>

