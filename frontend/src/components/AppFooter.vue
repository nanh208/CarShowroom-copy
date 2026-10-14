<template>
  <footer class="ftr">
    <div class="in">
      <div class="news">
        <div><b>Nhận tin hàng mới & ưu đãi</b><span>Mẫu xe vừa về, flash sale, phiên đấu giá hàng hiếm và voucher riêng cho thành viên.</span></div>
        <form @submit.prevent="subscribe"><input v-model="email" type="email" required placeholder="Email của bạn" /><button class="btn btn-sm" :disabled="busy">Đăng ký</button></form>
      </div>
      <div class="main">
        <router-link to="/" class="logo"><img loading="lazy" decoding="async" src="/images/logo-mark.png" alt="" /><span><b>Ares</b> Toys</span></router-link>
        <nav>
          <router-link to="/about">Về chúng tôi</router-link>
          <router-link to="/services">Liên hệ</router-link>
          <router-link to="/faq">Hỗ trợ</router-link>
          <router-link to="/about#terms">Điều khoản dịch vụ</router-link>
          <router-link to="/about#privacy">Chính sách bảo mật</router-link>
        </nav>
      </div>
      <div class="bottom">
        <span>ARES Toys — xe mô hình, xe RC & phụ kiện chính hãng · 123 Nguyễn Văn Linh, Q.7, TP.HCM · Hotline {{ HOTLINE }}</span>
        <span>© {{ new Date().getFullYear() }} ARES Toys</span>
      </div>
    </div>
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
.ftr { background: #d4d4d4; color: #5a5a5f; margin-top: 48px; }
.in { max-width: 1180px; margin: 0 auto; padding: 0 24px; }
.news { display: flex; justify-content: space-between; align-items: center; gap: 24px; padding: 24px 0; border-bottom: 1px solid #c2c2c2; flex-wrap: wrap; }
.news b { display: block; font-family: var(--head); font-size: 1.05rem; color: #3a3a3f; } .news span { font-size: .85rem; }
.news form { display: flex; gap: 8px; flex: 0 1 420px; }
.news input { flex: 1; height: 36px; padding: 0 14px; border-radius: 8px; border: 1px solid #bdbdbd; background: #ececec; font: inherit; outline: none; }
.news input:focus { border-color: var(--bronze); background: #fff; }
.main { display: flex; align-items: center; justify-content: space-between; gap: 32px; padding: 36px 0 18px; flex-wrap: wrap; }
.logo { display: flex; align-items: center; gap: 8px; font-family: var(--head); font-size: 1.5rem; color: #6f6f74; }
.logo img { height: 52px; } .logo b { color: var(--bronze); }
nav { display: flex; gap: 36px; flex-wrap: wrap; }
nav a { color: #6a6a70; font-family: var(--head); font-size: 1rem; } nav a:hover { color: var(--bronze); }
.bottom { display: flex; justify-content: space-between; gap: 16px; padding: 0 0 24px; font-size: .8rem; color: #85858a; flex-wrap: wrap; }
</style>
