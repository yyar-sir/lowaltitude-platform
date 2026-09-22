import request from '@/api/request'

export const list = (type) => request.get('/message/list', { params: { type } })
export const unread = () => request.get('/message/unread')
export const markRead = (id) => request.post(`/message/${id}/read`)
export const markAllRead = () => request.post('/message/read-all')

