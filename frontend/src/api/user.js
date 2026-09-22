import request from '@/api/request'

export const profile = () => request.get('/user/profile')
export const updateProfile = (data) => request.post('/user/profile', data)
/** 改密后旧 token 全部失效（后端写 password_changed_at） */
export const changePassword = (data) => request.post('/user/password', data)

