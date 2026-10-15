import { reactive } from 'vue'
import api from './api'

// ===== Đa tiền tệ: chỉ HIỂN THỊ (thanh toán luôn VND). Tỷ giá do admin đặt ở backend (/api/public/currency). =====
export const currency = reactive({ code: localStorage.getItem('currency') === 'USD' ? 'USD' : 'VND', rates: { VND: 1, USD: 25400 }, updatedAt: '', loaded: false })
export const vndRaw = n => (Number(n) || 0).toLocaleString('vi-VN') + 'đ'
const forceVnd = () => typeof location !== 'undefined' && /^\/(admin|pay)/.test(location.pathname)
export const vnd = n => {
  if (currency.code === 'VND' || forceVnd()) return vndRaw(n)
  const r = Number(currency.rates[currency.code]) || 0; if (!r) return vndRaw(n)
  return '$' + ((Number(n) || 0) / r).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
export async function loadCurrency() { try { const { data } = await api.get('/public/currency'); currency.rates = data.rates; currency.updatedAt = data.updatedAt; currency.loaded = true } catch { /* dùng mặc định */ } }
export function setCurrency(code, { persist = true } = {}) {
  currency.code = code === 'USD' ? 'USD' : 'VND'; localStorage.setItem('currency', currency.code)
  if (persist && localStorage.getItem('token')) api.put('/account/prefs/currency', JSON.stringify(currency.code), { headers: { 'Content-Type': 'application/json' } }).catch(() => {})
}
export const dt = s => s ? new Date(s).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' }) : ''
export const d = s => s ? new Date(s).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : ''
export const k = n => { n = Number(n) || 0; return n >= 1e6 ? (n / 1e6).toLocaleString('vi-VN', { maximumFractionDigits: 2 }) + ' tr' : (n / 1e3).toLocaleString('vi-VN') + 'K' }
export const ty = k

/** Danh mục động (lấy từ API). TYPES[code] = tên hiển thị. */
export const TYPES = reactive({ MODEL: 'Xe Model', RC: 'Xe RC', DISPLAY: 'Xe Trưng Bày', KIT: 'Lắp ráp & Kit', BATTERY: 'Pin & Sạc', REMOTE: 'Khiển RC', WHEEL: 'Bánh xe RC', PART: 'Linh kiện & Phụ kiện' })
export const catalog = reactive({ categories: [], brands: [], options: { scales: [], ages: [3, 6, 8, 14], cities: [], freeShipFrom: 500000, pointValue: 1000, earnPer: 10000, installmentMin: 3000000 }, loaded: false })
let loading
export function loadCatalog(force) {
  if (force) loading = null
  if (!loading) loading = Promise.all([api.get('/categories'), api.get('/brands'), api.get('/options')]).then(([c, b, o]) => {
    catalog.categories = c.data; catalog.brands = b.data; Object.assign(catalog.options, o.data); catalog.loaded = true
    c.data.forEach(x => { TYPES[x.code] = x.name })
  }).catch(() => { loading = null })
  return loading
}
export const isAcc = p => p?.grp === 'ACCESSORY'

export const ORDER_STATUS = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', SHIPPING: 'Đang giao', COMPLETED: 'Đã giao', CANCELLED: 'Đã huỷ', RETURNED: 'Đã trả hàng' }
export const PAY_METHOD = { COD: 'Thanh toán khi nhận hàng (COD)', BANK_QR: 'Chuyển khoản QR', MOMO: 'Ví MoMo', VNPAY: 'VNPay', INSTALLMENT: 'Trả góp 0% qua thẻ' }
export const PAY_STATUS = { UNPAID: 'Chưa thanh toán', PAID: 'Đã thanh toán', PARTIAL: 'Đang trả góp', REFUNDED: 'Đã hoàn tiền' }
export const RETURN_STATUS = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', REFUNDED: 'Đã hoàn tiền' }
export const TICKET_STATUS = { OPEN: 'Đang chờ', ANSWERED: 'Đã phản hồi', CLOSED: 'Đã đóng' }
export const AUCTION_STATUS = { SCHEDULED: 'Sắp diễn ra', LIVE: 'Đang đấu giá', ENDED: 'Đã kết thúc', CANCELLED: 'Đã huỷ' }
export const VOUCHER_KIND = { PERCENT: 'Giảm %', FIXED: 'Giảm tiền', FREESHIP: 'Miễn phí ship' }
export const HOTLINE = '1900 6868'
export const AGES = { 3: 'Từ 3 tuổi', 6: 'Từ 6 tuổi', 8: 'Từ 8 tuổi', 14: 'Từ 14 tuổi' }

// Tên tỉnh/thành (đầy đủ, VD "Thành phố Hồ Chí Minh") → khu vực tính phí ship của hệ thống
export const stripPlace = s => (s || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/^(tinh|thanh pho)\s+/, '').replace(/^tp\.?\s*/, '').trim()
export const toShipCity = (name, cities = catalog.options.cities) => { if (!name) return ''; const k = stripPlace(name); return cities.find(c => stripPlace(c) === k) || (cities.includes('Tỉnh khác') ? 'Tỉnh khác' : name) }
// Thời gian tương đối tiếng Việt: "vừa xong", "5 phút trước", "hôm qua"…
export const ago = s => {
  if (!s) return ''
  const sec = Math.max(0, (Date.now() - new Date(s).getTime()) / 1000)
  if (sec < 60) return 'vừa xong'
  if (sec < 3600) return Math.floor(sec / 60) + ' phút trước'
  if (sec < 86400) return Math.floor(sec / 3600) + ' giờ trước'
  if (sec < 172800) return 'hôm qua'
  if (sec < 2592000) return Math.floor(sec / 86400) + ' ngày trước'
  return new Date(s).toLocaleDateString('vi-VN')
}
export const dateLong = s => s ? new Date(s).toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' }) : ''
