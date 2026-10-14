<template>
  <footer id="contact" class="footer">
    <div class="container row between foot-inner">
      <div class="brand">
        <img src="/images/logo.png" alt="" />
        <div>
          <b>ARES MODELS</b>
          <div class="small">Mô hình ô tô sưu tầm chính hãng · 1:12 – 1:64</div>
        </div>
      </div>
      <div class="links row">
        <router-link to="/products">Sản phẩm</router-link>
        <router-link to="/promotions">Ưu đãi</router-link>
        <router-link to="/services">Dịch vụ</router-link>
        <router-link to="/account">Tài khoản</router-link>
        <a :href="`tel:${HOTLINE.replace(/ /g,'')}`">{{ HOTLINE }}</a>
      </div>
    </div>
    <div class="copy">© {{ new Date().getFullYear() }} ARES Models · 123 Nguyễn Văn Linh, Q.7, TP.HCM · cskh@ares.vn</div>
  </footer>
</template>
<script setup>import { HOTLINE } from '../format'</script>
<style scoped>
.footer { background: var(--footer); color: #eee; margin-top: 40px; }
.foot-inner { padding: 28px 20px; gap: 20px; }
.brand { display: flex; gap: 12px; align-items: center; }
.brand img { height: 40px; filter: brightness(1.2); }
.links a { color: #ddd; font-size: .9rem; padding: 4px 10px; }
.links a:hover { color: #fff; }
.copy { text-align: center; font-size: .78rem; color: #bbb; padding: 12px; border-top: 1px solid rgba(255,255,255,.12); }
@media (max-width: 700px) { .foot-inner { flex-direction: column; align-items: flex-start; } }
</style>
