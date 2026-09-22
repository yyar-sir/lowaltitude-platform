import request from '@/api/request'

/** 运营问答（超时放宽，生成类请求较慢） */
export const ask = (question) => request.post('/ai/ask', null, { params: { question }, timeout: 60000 })
/** 智能调度建议（失败时后端回退规则建议） */
export const dispatchAdvice = (orderId) => request.get(`/ai/dispatch-advice/${orderId}`, { timeout: 60000 })
/** 合规检查辅助 */
export const complianceCheck = (orderId) => request.get(`/ai/compliance-check/${orderId}`, { timeout: 60000 })
export const status = () => request.get('/ai/status')
export const config = () => request.get('/ai/config')
export const updateConfig = (data) => request.post('/ai/config', data)

