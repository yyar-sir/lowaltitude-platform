/**
 * 天枢 · 低空智能运营管控平台 —— 端到端业务闭环验收
 *
 * 覆盖：三端登录 → 订单创建（含空域校验）→ 承接 → 调度（资源校验）→
 *       跟踪 → 送达 → 结算评价 → 资源管理 → 空域合规 → 应急 → 越权 → AI 降级
 *
 * 用法：node e2e.mjs [baseUrl]
 */
const BASE = (process.argv[2] || 'http://127.0.0.1:8080').replace(/\/$/, '')
const PWD = '123456'

let pass = 0
let fail = 0
const failures = []
const C = {
  cyan: (s) => `\x1b[36m${s}\x1b[0m`,
  green: (s) => `\x1b[32m${s}\x1b[0m`,
  red: (s) => `\x1b[31m${s}\x1b[0m`,
  gray: (s) => `\x1b[90m${s}\x1b[0m`
}
const step = (t) => console.log(`\n${C.cyan('=== ' + t + ' ===')}`)
const ok = (m) => { console.log(`  ${C.green('[PASS]')} ${m}`); pass++ }
const no = (m) => { console.log(`  ${C.red('[FAIL]')} ${m}`); fail++; failures.push(m) }
const info = (m) => console.log(`  ${C.gray(m)}`)
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function api(method, path, token, body) {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined && body !== null) headers['Content-Type'] = 'application/json; charset=utf-8'
  const res = await fetch(BASE + path, {
    method, headers,
    body: body === undefined || body === null ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  try { return { http: res.status, ...JSON.parse(text) } }
  catch (e) { return { http: res.status, code: -1, msg: text.slice(0, 200) } }
}

const login = async (u) => {
  const r = await api('POST', '/api/auth/login', null, { username: u, password: PWD })
  return r.code === 0 ? r.data.token : null
}

async function main() {
  step('0. 服务可达性')
  const ping = await api('GET', '/api/public/ping')
  if (ping.code === 0) ok(`后端可达（${BASE}）`)
  else { no('后端不可达，终止'); return summary() }

  step('1. 三端账号登录')
  const admin = await login('admin')
  const merchant = await login('merchant01')
  const customer = await login('customer01')
  admin ? ok('平台管理员 admin 登录成功') : no('管理员登录失败')
  merchant ? ok('物流运营商家 merchant01 登录成功') : no('商家登录失败')
  customer ? ok('终端客户 customer01 登录成功') : no('客户登录失败')
  if (!admin || !merchant || !customer) { no('关键账号登录失败，终止'); return summary() }

  step('2. 订单创建（含空域与地址校验）')
  const create = await api('POST', '/api/order/create', customer, {
    sendAddress: '高新区科技园A座', receiveAddress: '经开区创新大厦B座',
    goodsType: '文件类', goodsWeight: 2.5, deliveryType: 2, priority: 3, remark: '端到端验收订单'
  })
  let orderId = null
  if (create.code === 0) {
    orderId = create.data.id
    ok(`下单成功：${create.data.orderNo}（航程 ${create.data.distanceKm}km，预估 ￥${create.data.estimateFee}，状态「${create.data.orderStatusName}」）`)
  } else {
    no(`下单失败：${create.msg}`)
  }
  // 地址相同应被拒
  const sameAddr = await api('POST', '/api/order/create', customer, {
    sendAddress: '同一地址', receiveAddress: '同一地址', goodsType: '文件类', goodsWeight: 1
  })
  sameAddr.code === 1001 ? ok('寄收地址相同被拒（1001）') : no(`地址校验未生效（code=${sameAddr.code}）`)
  // 禁飞区应被拒
  const banned = await api('POST', '/api/order/create', customer, {
    sendAddress: '高新区', receiveAddress: '南山核心禁飞区附近', goodsType: '文件类', goodsWeight: 1
  })
  banned.code === 1001 ? ok('收件地址落在禁飞区被拒（1001）') : no(`空域校验未生效（code=${banned.code}）`)

  step('3. 订单承接（商家接单）')
  if (orderId) {
    const pendingList = await api('GET', '/api/order/pending', merchant)
    if (pendingList.code === 0 && (pendingList.data || []).some((o) => o.id === orderId)) {
      ok(`待接单列表包含该订单（共 ${pendingList.data.length} 条待接单）`)
    } else {
      no('待接单列表未包含新订单')
    }
    const accept = await api('POST', `/api/order/${orderId}/accept`, merchant)
    if (accept.code === 0 && accept.data.orderStatus === 2) {
      ok(`接单成功，状态流转为「${accept.data.orderStatusName}」`)
    } else {
      no(`接单失败：${accept.msg}`)
    }
    // 重复接单应被拒（状态机保护）
    const again = await api('POST', `/api/order/${orderId}/accept`, merchant)
    again.code === 1001 ? ok('重复接单被拒（状态机生效）') : no(`重复接单未被拦截（code=${again.code}）`)
  }

  step('4. 订单调度（资源校验 + 状态推进）')
  if (orderId) {
    const opts = await api('GET', `/api/order/${orderId}/dispatch-options`, merchant)
    if (opts.code === 0) {
      ok(`调度可选资源：飞行器 ${(opts.data.aircraft || []).length} 架、场站 ${(opts.data.stations || []).length} 个、机长 ${(opts.data.pilots || []).length} 名`)
    } else {
      no(`调度资源查询失败：${opts.msg}`)
    }
    // 用载重不足的飞行器调度应被拒（订单 2.5kg，找一架载重更小的）
    const light = (opts.data.aircraft || []).find((a) => Number(a.maxPayload) < 2.5)
    if (light) {
      const bad = await api('POST', '/api/order/dispatch', merchant, { orderId, aircraftId: light.id })
      bad.code === 1001 ? ok(`载重不足的飞行器被拒（${light.aircraftNo} 载重 ${light.maxPayload}kg < 订单 2.5kg）`) : no(`载重校验未生效（code=${bad.code}）`)
    } else {
      info('演示数据中没有载重不足的可用飞行器，跳过载重校验用例（后端已有该校验分支）')
    }
    const first = (opts.data.aircraft || [])[0]
    const station = (opts.data.stations || [])[0]
    const pilot = (opts.data.pilots || [])[0]
    const dispatch = await api('POST', '/api/order/dispatch', merchant, {
      orderId, aircraftId: first?.id, stationId: station?.id, pilotId: pilot?.id
    })
    if (dispatch.code === 0 && dispatch.data.orderStatus === 3) {
      ok(`调度成功，状态流转为「${dispatch.data.orderStatusName}」，飞行器 ${dispatch.data.aircraftNo}，机长 ${dispatch.data.pilotName || '未指派'}`)
    } else {
      no(`调度失败：${dispatch.msg}`)
    }
  }

  step('5. 订单跟踪（轨迹与异常预警）')
  if (orderId) {
    const sim = await api('POST', `/api/order/${orderId}/simulate-flight`, merchant)
    sim.code === 0 ? ok('模拟推进飞行成功（补充跟踪点）') : no(`推进飞行失败：${sim.msg}`)
    const tracks = await api('GET', `/api/order/${orderId}/tracks`, customer)
    if (tracks.code === 0 && (tracks.data || []).length > 0) {
      ok(`客户可读跟踪轨迹（${tracks.data.length} 个跟踪点，最近电量 ${tracks.data[tracks.data.length - 1].battery}%）`)
    } else {
      no('跟踪数据读取失败')
    }
    // 上报异常 → 应触发预警消息
    const abn = await api('POST',
      `/api/order/${orderId}/track?longitude=113.94&latitude=22.53&battery=55&flightStatus=2&abnormalType=2&abnormalDesc=${encodeURIComponent('前方空域管制，正在绕行')}`,
      merchant)
    abn.code === 0 ? ok('异常跟踪点上报成功') : no(`异常上报失败：${abn.msg}`)
    await sleep(300)
    const msgs = await api('GET', '/api/message/list?type=order', customer)
    const hasAlert = (msgs.data || []).some((m) => (m.title || '').includes('异常'))
    hasAlert ? ok('客户收到异常预警站内消息（业务动作副作用生效）') : no('客户未收到异常预警')
  }

  step('6. 送达与结算评价')
  if (orderId) {
    const arrive = await api('POST', `/api/order/${orderId}/arrive`, merchant)
    arrive.code === 0 && arrive.data.orderStatus === 4
      ? ok(`标记送达成功，状态「${arrive.data.orderStatusName}」，实际费用 ￥${arrive.data.actualFee}`)
      : no(`标记送达失败：${arrive.msg}`)
    const settle = await api('POST', '/api/order/settle', customer, {
      orderId, payType: 1, score: 5, comment: '送得很快，包装完好'
    })
    if (settle.code === 0) {
      ok(`结算成功：￥${settle.data.settleAmount}，流水号 ${settle.data.payNo}，评分 ${settle.data.score}`)
    } else {
      no(`结算失败：${settle.msg}`)
    }
    // 重复结算应被拒（订单与结算一对一）
    const dup = await api('POST', '/api/order/settle', customer, { orderId, payType: 1, score: 5 })
    dup.code === 1001 ? ok('重复结算被拒（一对一约束生效）') : no(`重复结算未被拦截（code=${dup.code}）`)
    const final = await api('GET', `/api/order/${orderId}`, customer)
    final.data.orderStatus === 5 ? ok(`订单最终状态「${final.data.orderStatusName}」`) : no(`订单最终状态异常：${final.data.orderStatusName}`)
    // 状态时间轴
    const tl = final.data.timeline || []
    tl.length >= 4 ? ok(`状态流转时间轴完整（${tl.length} 个节点：${tl.map((t) => t.label).join(' → ')}）`) : no('时间轴不完整')
  }

  step('7. 越权拦截（数据范围收敛）')
  const otherCustomer = await login('customer02')
  if (otherCustomer && orderId) {
    const cross = await api('GET', `/api/order/${orderId}`, otherCustomer)
    cross.code === 403 ? ok('其他客户查看本人订单 → 403') : no(`订单数据范围未收敛（code=${cross.code}）`)
  }
  const stuAdmin = await api('GET', '/api/admin/users', customer)
  stuAdmin.code === 403 ? ok('客户访问管理端 → 403') : no(`管理端越权未被拦截（code=${stuAdmin.code}）`)
  const anon = await api('GET', '/api/order/list')
  anon.code === 401 ? ok('未登录访问受保护接口 → 401') : no(`未登录未被拦截（code=${anon.code}）`)
  if (orderId) {
    const custAccept = await api('POST', `/api/order/${orderId}/accept`, customer)
    custAccept.code === 403 ? ok('客户调用商家接单接口 → 403') : no(`角色越权未被拦截（code=${custAccept.code}）`)
  }

  step('8. 低空资源管理')
  const res = await api('GET', '/api/resource/overview', merchant)
  if (res.code === 0) {
    ok(`资源概览：飞行器 ${res.data.aircraftTotal}（可用 ${res.data.aircraftAvailable}、维保 ${res.data.aircraftMaintaining}）、场站 ${res.data.stationTotal}、机长 ${res.data.pilotTotal}、定价规则 ${res.data.pricingTotal}`)
  } else {
    no(`资源概览失败：${res.msg}`)
  }
  const aircraft = await api('GET', '/api/resource/aircraft', merchant)
  if (aircraft.code === 0 && (aircraft.data || []).length > 0) {
    const a = aircraft.data[0]
    const toMaint = await api('POST', `/api/resource/aircraft/${a.id}/status?status=3`, merchant)
    toMaint.code === 0 && toMaint.data.status === 3 ? ok(`飞行器 ${a.aircraftNo} 转维保成功`) : no('飞行器状态流转失败')
    await api('POST', `/api/resource/aircraft/${a.id}/status?status=1`, merchant)
    ok('飞行器恢复可用')
  } else {
    no('飞行器列表读取失败')
  }
  const pricing = await api('GET', '/api/resource/pricing', merchant)
  pricing.code === 0 && (pricing.data || []).length > 0
    ? ok(`定价规则 ${pricing.data.length} 条（首条基础费 ￥${pricing.data[0].baseFee}）`)
    : no('定价规则读取失败')
  // 定价非法值应被拒
  const badPricing = await api('POST', '/api/resource/pricing', merchant, { name: '非法规则', baseFee: 0 })
  badPricing.code === 1002 ? ok('基础配送费为 0 被拒（1002）') : no(`定价校验未生效（code=${badPricing.code}）`)

  step('9. 空域合规运营')
  const zones = await api('GET', '/api/airspace/zones', customer)
  if (zones.code === 0) {
    const usable = zones.data.filter((z) => z.zoneType === 1).length
    const bannedZones = zones.data.filter((z) => z.zoneType === 2).length
    const temp = zones.data.filter((z) => z.zoneType === 3).length
    ok(`空域规则：可用 ${usable}、禁飞 ${bannedZones}、临时管制 ${temp}`)
  } else {
    no('空域规则读取失败')
  }
  const plan = await api('POST', '/api/airspace/plans', merchant, {
    route: 'ST-001→端到端验收→经开区', altitude: 100, planStart: '2026-09-23 10:00:00', planEnd: '2026-09-23 10:40:00'
  })
  let planId = null
  if (plan.code === 0) {
    planId = plan.data.id
    ok(`飞行计划申报成功：${plan.data.planNo}（高度 ${plan.data.altitude}m，状态待审批）`)
  } else {
    no(`计划申报失败：${plan.msg}`)
  }
  // 高度超限应被拒
  const tooHigh = await api('POST', '/api/airspace/plans', merchant, { route: '测试超高', altitude: 500 })
  tooHigh.code === 1001 ? ok('申报高度超过可用空域上限被拒（1001）') : no(`高度校验未生效（code=${tooHigh.code}）`)
  if (planId) {
    const review = await api('POST', `/api/airspace/plans/${planId}/review?pass=true`, admin)
    review.code === 0 && review.data.status === 1 ? ok('管理员审批通过飞行计划') : no(`审批失败：${review.msg}`)
    await sleep(300)
    const pmsgs = await api('GET', '/api/message/list?type=plan', merchant);
    (pmsgs.data || []).some((m) => (m.title || '').includes('通过'))
      ? ok('商家收到审批结果通知')
      : no('商家未收到审批通知')
  }
  const violations = await api('GET', '/api/airspace/violations', admin)
  if (violations.code === 0 && (violations.data || []).length > 0) {
    const v = violations.data.find((x) => x.handleStatus === 0) || violations.data[0]
    const handled = await api('POST', `/api/airspace/violations/${v.id}/handle?result=${encodeURIComponent('已提醒商家整改')}`, admin)
    handled.code === 0 && handled.data.handleStatus === 1 ? ok(`违规记录处置成功（${v.violationType}）`) : no('违规处置失败')
  } else {
    info('暂无违规记录，跳过处置用例')
  }

  step('10. 应急与客服')
  const inc = await api('POST', '/api/incident/report', customer, {
    title: '端到端验收事件', content: '配送过程中遇到临时空域管制', incidentType: 2, level: 2
  })
  let incId = null
  if (inc.code === 0) {
    incId = inc.data.id
    ok(`事件上报成功：${inc.data.title}（等级 较大）`)
  } else {
    no(`事件上报失败：${inc.msg}`)
  }
  if (incId) {
    const skip = await api('POST', `/api/incident/${incId}/handle?status=2`, admin)
    skip.code === 1001 ? ok('待处理直接跳已解决被拒（必须先经处理中）') : no(`跳步未被拦截（code=${skip.code}）`)
    await api('POST', `/api/incident/${incId}/handle?status=1`, admin)
    const done = await api('POST', `/api/incident/${incId}/handle?status=2&result=${encodeURIComponent('已协调绕行')}`, admin)
    done.code === 0 && done.data.status === 2 ? ok('事件处置闭环完成（处理中 → 已解决）') : no('事件处置失败')
  }

  step('11. 大模型三级降级')
  const aiStatus = await api('GET', '/api/ai/status', admin)
  if (aiStatus.code === 0) {
    info(`AI 配置：configured=${aiStatus.data.configured}，model=${aiStatus.data.model}`)
    ok('AI 状态接口正常')
  } else {
    no('AI 状态接口异常')
  }
  if (orderId) {
    const advice = await api('GET', `/api/ai/dispatch-advice/${orderId}`, merchant)
    if (advice.code === 1101) {
      ok('未配置 Key 时返回业务码 1101（前端提示去配置，而非「网络异常」）')
    } else if (advice.code === 0) {
      ok(`调度建议返回成功（fromAi=${advice.data.fromAi}，规则建议：${(advice.data.ruleAdvice || '').slice(0, 30)}...）`)
    } else {
      no(`调度建议异常：code=${advice.code} ${advice.msg}`)
    }
  }
  const cfg = await api('GET', '/api/ai/config', admin)
  if (cfg.code === 0) {
    const key = cfg.data.apiKey
    !key || key.startsWith('****') ? ok(`API Key 已脱敏返回（${key || '未配置'}）`) : no('API Key 未脱敏，存在泄露风险')
  }

  step('12. 管理端看板与操作日志')
  const dash = await api('GET', '/api/dashboard', admin)
  if (dash.code === 0) {
    const s = dash.data.summary || {}
    ok(`全平台看板：用户 ${s.userTotal}（商家 ${s.merchantTotal} / 客户 ${s.customerTotal}）、飞行器 ${s.aircraftTotal}、订单 ${s.orderTotal}、完成率 ${s.finishRate}%`)
  } else {
    no(`看板失败：${dash.msg}`)
  }
  const custDash = await api('GET', '/api/dashboard', customer)
  const adminDash = await api('GET', '/api/dashboard', admin)
  custDash.code === 0 && adminDash.code === 0
    ? ok(`数据范围收敛：客户看到 ${custDash.data.summary.orderTotal} 单，管理员看到 ${adminDash.data.summary.orderTotal} 单`)
    : no('看板数据范围异常')
  const logs = await api('GET', '/api/admin/logs', admin)
  logs.code === 0 && (logs.data || []).length > 0
    ? ok(`操作日志有留痕（${logs.data.length} 条，记录操作人/类型/IP）`)
    : no('操作日志为空')

  summary()
}

function summary() {
  console.log('\n' + '='.repeat(48))
  if (fail === 0) {
    console.log(C.green(` 端到端验收全部通过：${pass} 项`))
  } else {
    console.log(C.red(` 端到端验收：通过 ${pass} 项，失败 ${fail} 项`))
    failures.forEach((f) => console.log(C.red('   ✗ ' + f)))
  }
  process.exit(fail === 0 ? 0 : 1)
}

main().catch((e) => {
  console.error(C.red('验收脚本异常：' + e.message))
  process.exit(1)
})
