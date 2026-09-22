import request from '@/api/request'

export const overview = () => request.get('/resource/overview')

export const aircraft = (status) => request.get('/resource/aircraft', { params: { status } })
export const saveAircraft = (data) => request.post('/resource/aircraft', data)
export const changeAircraftStatus = (id, status) =>
  request.post(`/resource/aircraft/${id}/status`, null, { params: { status } })

export const stations = () => request.get('/resource/station')
export const saveStation = (data) => request.post('/resource/station', data)

export const pilots = () => request.get('/resource/pilot')
export const savePilot = (data) => request.post('/resource/pilot', data)

export const pricing = () => request.get('/resource/pricing')
export const savePricing = (data) => request.post('/resource/pricing', data)

