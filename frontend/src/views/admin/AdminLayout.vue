<template>
  <div class="admin">
    <aside class="side">
      <router-link to="/" class="logo">ARES <b>ADMIN</b></router-link>
      <nav>
        <router-link to="/admin" exact-active-class="on">📊 Tổng quan</router-link>
        <router-link to="/admin/products" active-class="on">🏎️ Sản phẩm</router-link>
        <router-link to="/admin/orders" active-class="on">🧾 Đơn hàng</router-link>
        <router-link to="/admin/customers" active-class="on">👥 Khách hàng</router-link>
        <router-link to="/admin/promotions" active-class="on">🎁 Ưu đãi</router-link>
        <router-link to="/admin/bookings" active-class="on">📅 Lịch hẹn</router-link>
        <router-link to="/admin/chat" active-class="on">💬 Live chat</router-link>
      </nav>
      <div class="foot">
        <div class="small muted">{{ auth.user?.fullName }}</div>
        <router-link to="/" class="small">← Về cửa hàng</router-link>
        <a href="#" class="small" @click.prevent="logout">Đăng xuất</a>
      </div>
    </aside>
    <main class="content"><router-view /></main>
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '../../stores/auth'
const auth = useAuth(), router = useRouter()
const logout = () => { auth.logout(); router.push('/login') }
</script>
<style scoped>
.admin { display: flex; min-height: 100vh; }
.side { width: 230px; background: #08090c; border-right: 1px solid var(--border); padding: 22px 14px; display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh; }
.logo { color: #fff; font-size: 1.25rem; margin: 0 8px 26px; } .logo b { color: var(--accent); }
nav a { display: block; color: var(--label); padding: 10px 12px; border-radius: 8px; margin-bottom: 4px; font-weight: 600; }
nav a:hover { background: rgba(255,255,255,.05); color: #fff; }
nav a.on { background: var(--primary); color: #fff; }
.foot { margin-top: auto; display: flex; flex-direction: column; gap: 6px; padding: 0 8px; }
.content { flex: 1; padding: 28px; min-width: 0; }
:deep(h1) { margin-bottom: 18px; }
</style>
