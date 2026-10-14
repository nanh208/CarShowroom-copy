export const vnd = n => (Number(n) || 0).toLocaleString('vi-VN') + 'đ'
export const dt = s => s ? new Date(s).toLocaleString('vi-VN') : ''
export const d = s => s ? new Date(s).toLocaleDateString('vi-VN') : ''

export const TYPES = { SEDAN: 'Sedan', SUV: 'SUV / Crossover', PICKUP: 'Bán tải', SPORT: 'Xe thể thao', SUPERCAR: 'Siêu xe', ELECTRIC: 'Xe điện', CLASSIC: 'Xe cổ điển', ACCESSORY: 'Phụ kiện' }
export const FUELS = ['Xăng', 'Dầu', 'Hybrid', 'Điện']
export const ORDER_STATUS = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', SHIPPING: 'Đang giao', COMPLETED: 'Hoàn tất', CANCELLED: 'Đã huỷ' }
export const PAY_METHOD = { FULL: 'Thanh toán toàn bộ', DEPOSIT: 'Đặt cọc 10%', INSTALLMENT: 'Trả góp ngân hàng', COD: 'Thanh toán khi nhận hàng' }
export const PAY_STATUS = { UNPAID: 'Chưa thanh toán', DEPOSITED: 'Đã đặt cọc', PARTIAL: 'Đang trả góp', PAID: 'Đã thanh toán' }
export const BOOKING_TYPE = { TEST_DRIVE: 'Lái thử', MAINTENANCE: 'Sửa chữa & bảo dưỡng' }
export const BOOKING_STATUS = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', DONE: 'Hoàn thành', CANCELLED: 'Đã huỷ' }
export const AUCTION_STATUS = { SCHEDULED: 'Sắp diễn ra', LIVE: 'Đang đấu giá', ENDED: 'Đã kết thúc', CANCELLED: 'Đã huỷ' }
export const ty = n => { n = Number(n) || 0; return n >= 1e9 ? (n / 1e9).toLocaleString('vi-VN', { maximumFractionDigits: 3 }) + ' tỷ' : (n / 1e6).toLocaleString('vi-VN', { maximumFractionDigits: 1 }) + ' triệu' }
export const HOTLINE = '1900 6868'
