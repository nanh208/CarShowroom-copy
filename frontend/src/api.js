import axios from 'axios'

const api = axios.create({ baseURL: (import.meta.env.VITE_API_URL || '') + '/api' })

api.interceptors.request.use(cfg => {
  const t = localStorage.getItem('token')
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

api.interceptors.response.use(r => r, err => {
  if (err.response?.status === 401 && localStorage.getItem('token')) {
    localStorage.removeItem('token')
    if (!location.pathname.startsWith('/login')) location.href = '/login?expired=1'
  }
  return Promise.reject(err)
})

export const errMsg = e => e?.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại'
export default api
