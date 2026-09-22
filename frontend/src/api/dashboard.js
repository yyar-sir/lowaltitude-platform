import request from '@/api/request'

/** 数据看板（按角色自动收敛数据范围） */
export const dashboard = () => request.get('/dashboard')

