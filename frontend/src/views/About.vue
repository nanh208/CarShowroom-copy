<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Về chúng tôi' }]" />
    <h1>Về ARES Auto</h1>
    <p>Showroom ô tô đa thương hiệu tại TP.HCM: xe mới chính hãng, cấu hình theo ý bạn, trả góp linh hoạt và đấu giá xe sưu tầm.</p>
    <div class="hero-img"><img src="/images/hero-taycan.jpg" alt="Showroom ARES" /></div>
    <div class="grid grid-3 facts">
      <div class="card"><b>14</b><span>thương hiệu phân phối</span></div>
      <div class="card"><b>3.000 m²</b><span>showroom & xưởng dịch vụ</span></div>
      <div class="card"><b>8:00–21:00</b><span>mở cửa mỗi ngày · {{ HOTLINE }}</span></div>
    </div>
    <section id="terms" class="section card">
      <h3>Điều khoản dịch vụ</h3>
      <p>Giá niêm yết đã gồm VAT, chưa gồm lệ phí trước bạ, biển số, đăng kiểm và bảo hiểm. Tiền đặt cọc 10% được hoàn lại nếu ARES không giao được xe đúng cam kết. Người thắng đấu giá cần đặt cọc trong 48 giờ; quá hạn phiên có thể được trao cho người trả giá cao kế tiếp.</p>
    </section>
    <section id="privacy" class="section card">
      <h3>Chính sách bảo mật</h3>
      <p>Thông tin cá nhân (họ tên, số điện thoại, email, địa chỉ) chỉ dùng để xử lý đơn hàng, lịch hẹn và chăm sóc khách hàng; không chia sẻ cho bên thứ ba ngoài ngân hàng đối tác khi bạn yêu cầu trả góp.</p>
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
