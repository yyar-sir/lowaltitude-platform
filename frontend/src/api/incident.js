import request from '@/api/request'

export const list = (status) => request.get('/incident/list', { params: { status } })
export const report = (data) => request.post('/incident/report', data)
export const handle = (id, status, result) =>
  request.post(`/incident/${id}/handle`, null, { params: { status, result } })

