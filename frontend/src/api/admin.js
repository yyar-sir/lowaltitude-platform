import request from '@/api/request'

export const users = (params) => request.get('/admin/users', { params })
export const createUser = (data) => request.post('/admin/users', data)
export const updateUser = (id, data) => request.post(`/admin/users/${id}`, data)
export const toggleUserStatus = (id, status) =>
  request.post(`/admin/users/${id}/status`, null, { params: { status } })
export const deleteUser = (id) => request.delete(`/admin/users/${id}`)
export const resetPassword = (id) => request.post(`/admin/users/${id}/reset-password`)
export const logs = () => request.get('/admin/logs')

