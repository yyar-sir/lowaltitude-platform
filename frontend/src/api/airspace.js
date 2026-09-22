import request from '@/api/request'

// 空域规则
export const zones = (params) => request.get('/airspace/zones', { params })
export const saveZone = (data) => request.post('/airspace/zones', data)
export const deleteZone = (id) => request.delete(`/airspace/zones/${id}`)

// 飞行计划
export const plans = (status) => request.get('/airspace/plans', { params: { status } })
export const submitPlan = (data) => request.post('/airspace/plans', data)
export const reviewPlan = (id, pass, reason) =>
  request.post(`/airspace/plans/${id}/review`, null, { params: { pass, reason } })

// 违规记录
export const violations = (handleStatus) => request.get('/airspace/violations', { params: { handleStatus } })
export const handleViolation = (id, result) =>
  request.post(`/airspace/violations/${id}/handle`, null, { params: { result } })

