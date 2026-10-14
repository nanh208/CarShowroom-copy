export const vnd = n => (Number(n) || 0).toLocaleString('vi-VN') + 'đ'
export const dt = s => s ? new Date(s).toLocaleString('vi-VN') : ''
export const d = s => s ? new Date(s).toLocaleDateString('vi-VN') : ''

export const TYPES = { SUPERCAR: 'Siêu xe', SEDAN: 'Sedan / Coupe', SUV: 'SUV', CLASSIC: 'Xe cổ điển', RACING: 'Xe đua', TRUCK: 'Bán tải / Tải', ACCESSORY: 'Phụ kiện' }
export const ORDER_STATUS = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', SHIPPING: 'Đang giao', COMPLETED: 'Hoàn tất', CANCELLED: 'Đã huỷ' }
export const PAY_METHOD = { FULL: 'Thanh toán toàn bộ', DEPOSIT: 'Đặt cọc 30%', INSTALLMENT: 'Trả góp 0%', COD: 'Thanh toán khi nhận hàng' }
export const PAY_STATUS = { UNPAID: 'Chưa thanh toán', DEPOSITED: 'Đã đặt cọc', PARTIAL: 'Đang trả góp', PAID: 'Đã thanh toán' }
export const BOOKING_TYPE = { VIEWING: 'Xem & trải nghiệm mô hình', MAINTENANCE: 'Sửa chữa & bảo dưỡng' }
export const BOOKING_STATUS = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', DONE: 'Hoàn thành', CANCELLED: 'Đã huỷ' }
export const HOTLINE = '1900 6868'
