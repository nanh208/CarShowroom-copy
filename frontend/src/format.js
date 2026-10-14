import { reactive } from 'vue'
import api from './api'

export const vnd = n => (Number(n) || 0).toLocaleString('vi-VN') + 'đ'
export const dt = s => s ? new Date(s).toLocaleString('vi-VN') : ''
export const d = s => s ? new Date(s).toLocaleDateString('vi-VN') : ''
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
