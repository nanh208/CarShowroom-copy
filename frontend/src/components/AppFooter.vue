<template>
  <footer class="footer">
    <div class="container news">
      <div><h3>Nhận tin xe mới & ưu đãi</h3><p>Mẫu xe vừa về, phiên đấu giá sắp mở và ưu đãi riêng cho thành viên. Không spam.</p></div>
      <form @submit.prevent="subscribe"><input v-model="email" type="email" required placeholder="Email của bạn" /><button class="btn" :disabled="busy">Đăng ký</button></form>
    </div>
    <div class="container top">
      <div class="about">
        <div class="brand">ARES <b>AUTO</b></div>
        <p>Showroom ô tô đa thương hiệu. Xe mới chính hãng, cấu hình theo ý bạn, trả góp linh hoạt và đấu giá xe sưu tầm.</p>
        <a class="hot" :href="`tel:${HOTLINE.replace(/ /g, '')}`">{{ HOTLINE }}</a>
      </div>
      <div><h4>Mua xe</h4>
        <router-link to="/products">Tất cả mẫu xe</router-link><router-link to="/auctions">Đấu giá xe hiếm</router-link>
        <router-link :to="{ path: '/products', query: { type: 'ACCESSORY' } }">Phụ kiện</router-link><router-link to="/promotions">Ưu đãi</router-link><router-link to="/cart">Giỏ hàng</router-link></div>
      <div><h4>Dịch vụ</h4>
        <router-link to="/services">Lái thử</router-link><router-link to="/services?tab=maintenance">Bảo dưỡng & sửa chữa</router-link>
        <router-link to="/faq">Câu hỏi thường gặp</router-link><router-link to="/compare">So sánh xe</router-link><router-link to="/account">Tài khoản</router-link></div>
      <div><h4>Showroom</h4>
        <p>123 Nguyễn Văn Linh, Quận 7, TP.HCM</p><p>cskh@ares.vn</p><p>8:00 – 21:00, mỗi ngày</p></div>
    </div>
    <div class="container copy"><span>© {{ new Date().getFullYear() }} ARES Auto</span><span>Giá xe mang tính tham khảo</span></div>
  </footer>
</template>
<script setup>
import { ref } from 'vue'
import api from '../api'
import { HOTLINE } from '../format'
import { toast } from '../stores/prefs'
const email = ref(''), busy = ref(false)
async function subscribe() {
  busy.value = true
  try { const r = await api.post('/newsletter', { email: email.value }); toast(r.data.message); email.value = '' }
  catch (e) { toast(e.response?.data?.message || 'Không đăng ký được', 'error') } finally { busy.value = false }
}
</script>
<style scoped>
.footer { background: rgba(6,7,9,.92); border-top: 1px solid var(--border); padding-top: 64px; margin-top: 0; }
.top { display: grid; grid-template-columns: 2fr 1fr 1fr 1.2fr; gap: 48px; padding-bottom: 48px; }
@media (max-width: 800px) { .top { grid-template-columns: 1fr 1fr; } }
.brand { font-size: 1.2rem; letter-spacing: .14em; margin-bottom: 16px; } .brand b { color: var(--accent); }
.about p { color: var(--muted); max-width: 360px; margin-bottom: 16px; }
.hot { font-size: 1.5rem; font-weight: 700; color: #fff; }
h4 { font-size: .75rem; text-transform: uppercase; letter-spacing: .16em; color: var(--muted); margin-bottom: 16px; }
a { display: block; color: #e2e8f0; margin-bottom: 8px; } a:hover { color: var(--accent); }
p { color: #e2e8f0; margin-bottom: 8px; }
.copy { display: flex; justify-content: space-between; color: var(--muted); font-size: .85rem; padding: 24px 32px; border-top: 1px solid var(--border); }
.news { display: flex; justify-content: space-between; align-items: center; gap: 48px; padding-bottom: 48px; margin-bottom: 48px; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
.news h3 { font-size: 1.5rem; letter-spacing: -.02em; margin-bottom: 8px; } .news p { color: var(--muted); }
.news form { display: flex; gap: 8px; flex: 0 1 480px; }
.news input { flex: 1; height: 48px; padding: 0 20px; border-radius: 999px; border: 1px solid var(--border); background: rgba(255,255,255,.05); color: #fff; font: inherit; outline: none; }
.news input:focus { border-color: var(--accent); }
</style>
