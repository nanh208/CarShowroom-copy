<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Về chúng tôi' }]" />
    <h1>Về ARES Toys</h1>
    <p>Cửa hàng xe đồ chơi cho mọi lứa tuổi tại TP.HCM: xe mô hình die-cast, xe RC, xe trưng bày, bộ lắp ráp và phụ kiện RC chính hãng — kèm sân chơi RC và đấu giá hàng hiếm cho nhà sưu tầm.</p>
    <div class="hero-img"><img src="/images/toys/hw-r34-1.jpg" alt="ARES Toys" /></div>
    <div class="grid grid-3 facts">
      <div class="card"><b>17</b><span>thương hiệu chính hãng</span></div>
      <div class="card"><b>300 m²</b><span>cửa hàng & sân chạy thử RC</span></div>
      <div class="card"><b>8:00–21:00</b><span>mở cửa mỗi ngày · {{ HOTLINE }}</span></div>
    </div>
    <section id="terms" class="section card">
      <h3>Điều khoản dịch vụ</h3>
      <p>Giá niêm yết đã gồm VAT. Đổi trả trong 7 ngày với sản phẩm còn nguyên tem, đủ phụ kiện. Voucher không cộng dồn; điểm thưởng dùng tối đa 50% giá trị đơn. Người thắng đấu giá cần thanh toán trong 48 giờ; quá hạn phiên có thể được trao cho người trả giá cao kế tiếp.</p>
    </section>
    <section id="privacy" class="section card">
      <h3>Chính sách bảo mật</h3>
      <p>Thông tin cá nhân (họ tên, số điện thoại, email, địa chỉ) chỉ dùng để xử lý đơn hàng, hỗ trợ và chăm sóc khách hàng; chỉ chia sẻ với đơn vị vận chuyển và cổng thanh toán cần thiết để hoàn tất giao dịch.</p>
    </section>
  </div>
</template>
<script setup>
import Breadcrumb from '../components/Breadcrumb.vue'
import { HOTLINE } from '../format'
</script>
<style scoped>
.hero-img { border-radius: 12px; overflow: hidden; aspect-ratio: 16/6; margin-bottom: 24px; } .hero-img img { width: 100%; height: 100%; object-fit: cover; }
.facts b { display: block; font-family: var(--head); font-size: 1.8rem; color: var(--bronze); } .facts span { color: var(--muted); }
.section p { color: var(--text-2); line-height: 1.7; }
</style>
