import request from '@/api/request'

// ---------- 订单全流程 ----------
/** 订单列表：客户=自己、商家=本商家、管理员=全部 */
export const list = (params) => request.get('/order/list', { params })
/** 订单详情（含状态时间轴与调度摘要） */
export const detail = (id) => request.get(`/order/${id}`)

// 一、创建（客户）
export const create = (data) => request.post('/order/create', data)
export const cancel = (id, reason) => request.post(`/order/${id}/cancel`, null, { params: { reason } })

// 二、承接（商家）
export const pending = () => request.get('/order/pending')
export const accept = (id) => request.post(`/order/${id}/accept`)
export const reject = (id, reason) => request.post(`/order/${id}/reject`, null, { params: { reason } })

// 三、调度（商家）
export const dispatchOptions = (id) => request.get(`/order/${id}/dispatch-options`)
export const dispatch = (data) => request.post('/order/dispatch', data)
export const simulateFlight = (id) => request.post(`/order/${id}/simulate-flight`)
export const arrive = (id) => request.post(`/order/${id}/arrive`)

// 四、跟踪
export const tracks = (id) => request.get(`/order/${id}/tracks`)

// 五、结算
export const settle = (data) => request.post('/order/settle', data)
export const settlement = (id) => request.get(`/order/${id}/settlement`)

