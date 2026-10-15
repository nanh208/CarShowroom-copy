import axios from 'axios'

const api = axios.create({ baseURL: (import.meta.env.VITE_API_URL || '') + '/api' })

api.interceptors.request.use(cfg => {
  const t = localStorage.getItem('token')
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  try { const a = JSON.parse(localStorage.getItem('aff') || 'null'); if (a && a.exp > Date.now() && /\/(orders|public\/checkout)$/.test(cfg.url || '')) cfg.headers['X-Aff'] = a.code } catch { /* bỏ qua */ }
  cfg.headers['Accept-Language'] = localStorage.getItem('locale') === 'en' ? 'en' : 'vi'
  return cfg
})

api.interceptors.response.use(r => r, err => {
  // Chỉ đăng xuất khi chính token hiện tại bị từ chối (tránh request cũ/không token xoá phiên mới)
  const cur = localStorage.getItem('token'), sent = err.config?.headers?.Authorization
  if (err.response?.status === 401 && cur && sent === `Bearer ${cur}`) {
    localStorage.removeItem('token')
    if (!location.pathname.startsWith('/login')) location.href = '/login?expired=1'
  }
  return Promise.reject(err)
})

export const errMsg = e => e?.response?.data?.message || 'Có lỗi xảy ra, vui lòng thử lại'
export default api

/** Tải ảnh lên server, trả về URL. */
export async function uploadImage(file) {
  const fd = new FormData(); fd.append('file', file)
  return (await api.post('/uploads', fd)).data.url
}
