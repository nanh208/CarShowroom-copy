<template>
  <footer class="footer">
    <div class="container grid cols">
      <div>
        <div class="brand">ARES <b>AUTO</b></div>
        <p class="muted small">Showroom ô tô đa thương hiệu: xe mới chính hãng, cấu hình theo ý bạn, trả góp, đấu giá xe hiếm, dịch vụ hậu mãi trọn đời.</p>
      </div>
      <div>
        <h4>Mua sắm</h4>
        <router-link to="/products">Tất cả sản phẩm</router-link>
        <router-link to="/promotions">Chương trình ưu đãi</router-link>
        <router-link to="/auctions">Đấu giá xe hiếm</router-link>
        <router-link to="/cart">Giỏ hàng</router-link>
      </div>
      <div>
        <h4>Dịch vụ</h4>
        <router-link to="/services">Đăng ký lái thử</router-link>
        <router-link to="/services?tab=maintenance">Sửa chữa & bảo dưỡng</router-link>
        <router-link to="/account">Tài khoản của tôi</router-link>
      </div>
      <div>
        <h4>Liên hệ</h4>
        <p>📍 123 Nguyễn Văn Linh, Q.7, TP.HCM</p>
        <p>📞 Hotline: <a :href="`tel:${HOTLINE.replace(/ /g, '')}`">{{ HOTLINE }}</a></p>
        <p>✉️ cskh@ares.vn</p>
        <p>🕘 8:00 – 21:00 (T2 – CN)</p>
      </div>
    </div>
    <div class="copy">© {{ new Date().getFullYear() }} ARES Auto. All rights reserved.</div>
  </footer>
</template>
<script setup>import { HOTLINE } from '../format'</script>
<style scoped>
.footer { background: #08090c; border-top: 1px solid var(--border); padding: 44px 0 0; margin-top: 40px; }
.cols { grid-template-columns: 2fr 1fr 1fr 1.4fr; }
@media (max-width: 800px) { .cols { grid-template-columns: 1fr 1fr; } }
.brand { font-size: 1.3rem; margin-bottom: 10px; } .brand b { color: var(--accent); }
h4 { margin-bottom: 12px; }
a { display: block; color: var(--muted); margin-bottom: 6px; } a:hover { color: var(--accent); }
p { color: var(--muted); margin-bottom: 6px; font-size: .92rem; }
.copy { text-align: center; color: var(--muted); font-size: .8rem; padding: 18px; border-top: 1px solid var(--border); margin-top: 30px; }
</style>
