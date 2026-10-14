<template>
  <div class="admin">
    <aside class="side">
      <router-link to="/" class="logo">ARES <b>ADMIN</b></router-link>
      <nav>
        <router-link to="/admin" exact-active-class="on"><svg viewBox="0 0 24 24"><path d="M3 13h8V3H3zm10 8h8V11h-8zM3 21h8v-6H3zm10-18v6h8V3z"/></svg>Tổng quan</router-link>
        <router-link to="/admin/products" active-class="on"><svg viewBox="0 0 24 24"><path d="M5 17h14l-1.5-6.5A2 2 0 0 0 15.6 9H8.4a2 2 0 0 0-1.9 1.5zM7 17v2m10-2v2M7.5 13.5h.01m9 0h.01"/></svg>Sản phẩm</router-link>
        <router-link to="/admin/orders" active-class="on"><svg viewBox="0 0 24 24"><path d="M6 2h12v20l-3-2-3 2-3-2-3 2zM9 7h6M9 11h6M9 15h4"/></svg>Đơn hàng</router-link>
        <router-link to="/admin/customers" active-class="on"><svg viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8m13 10v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"/></svg>Khách hàng</router-link>
        <router-link to="/admin/promotions" active-class="on"><svg viewBox="0 0 24 24"><path d="M20 12v9H4v-9M2 7h20v5H2zm10 14V7m0 0H7.5a2.5 2.5 0 1 1 0-5C11 2 12 7 12 7m0 0h4.5a2.5 2.5 0 1 0 0-5C13 2 12 7 12 7"/></svg>Ưu đãi</router-link>
        <router-link to="/admin/auctions" active-class="on"><svg viewBox="0 0 24 24"><path d="m14 13-8.5 8.5a2.1 2.1 0 0 1-3-3L11 10m5 6 6-6m-14-2 6-6m-3 3 8 8"/></svg>Đấu giá</router-link>
        <router-link to="/admin/reviews" active-class="on"><svg viewBox="0 0 24 24"><path d="m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z"/></svg>Đánh giá</router-link>
        <router-link to="/admin/bookings" active-class="on"><svg viewBox="0 0 24 24"><path d="M3 5h18v16H3zM16 3v4M8 3v4M3 10h18"/></svg>Lịch hẹn</router-link>
        <router-link to="/admin/chat" active-class="on"><svg viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>Live chat</router-link>
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
.admin { display: flex; min-height: 100vh; background: #fff; }
.side { width: 256px; flex-shrink: 0; background: #fff;  border-right: 1px solid var(--border); padding: 24px 16px; display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh; }
.logo { display: flex; align-items: center; gap: 10px; color: var(--text); font-size: 1.05rem; letter-spacing: .14em; margin: 0 8px 40px; } .logo img { height: 32px; } .logo b { color: var(--accent); }
nav a { display: flex; align-items: center; gap: 12px; color: var(--text-2); padding: 12px 16px; border-radius: 12px; margin-bottom: 4px; font-weight: 500; position: relative; transition: .15s; }
nav svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
nav a:hover { background: rgba(0,0,0,.05); color: var(--text); }
nav a.on { background: rgba(168,122,60,.16); color: var(--text); }
nav a.on::before { content: ''; position: absolute; left: -16px; top: 10px; bottom: 10px; width: 3px; border-radius: 0 3px 3px 0; background: var(--accent); }
nav a.on svg { color: var(--accent); }
.foot { margin-top: auto; display: flex; flex-direction: column; gap: 8px; padding: 16px; border-top: 1px solid var(--border); }
.foot a { color: var(--muted); } .foot a:hover { color: var(--text); }
.content { flex: 1; padding: 40px 48px; min-width: 0; }
:deep(.card) { padding: 24px; }
:deep(h1) { font-size: 2rem; font-weight: 800; letter-spacing: -.02em; margin-bottom: 32px; }
</style>
