<template>
  <header class="header">
    <div class="topbar">
      <div class="container row between">
        <span>📞 Hotline: <b>{{ HOTLINE }}</b> · ✉️ cskh@ares.vn</span>
        <span class="hide-sm">🚗 Giao xe tận nhà miễn phí · Trả góp đến 80% · Đấu giá xe hiếm mỗi tuần</span>
      </div>
    </div>
    <div class="container main row">
      <router-link to="/" class="logo">
        <img src="/images/logo.png" alt="ARES" @error="e => e.target.style.display = 'none'" />
        <span>ARES <b>AUTO</b></span>
      </router-link>
      <form class="search grow" @submit.prevent="search">
        <input v-model="q" class="input" placeholder="Tìm xe, thương hiệu... (VD: Camry, VinFast, SUV)" />
        <button class="btn btn-sm">🔍</button>
      </form>
      <div class="row actions">
        <router-link to="/cart" class="cart">🛒<span v-if="cart.count" class="cnt">{{ cart.count }}</span></router-link>
        <template v-if="auth.loggedIn">
          <router-link v-if="auth.isAdmin" to="/admin" class="btn btn-sm btn-outline">⚙️ Quản trị</router-link>
          <router-link to="/account" class="user">👤 {{ auth.user.fullName?.split(' ').pop() }}</router-link>
          <button class="btn btn-sm btn-outline" @click="logout">Đăng xuất</button>
        </template>
        <template v-else>
          <router-link to="/login" class="btn btn-sm">Đăng nhập</router-link>
        </template>
      </div>
    </div>
    <nav class="nav">
      <div class="container row">
        <router-link to="/" exact-active-class="on">Trang chủ</router-link>
        <router-link to="/products" active-class="on">Sản phẩm</router-link>
        <router-link :to="{ path: '/products', query: { type: 'SUV' } }">SUV</router-link>
        <router-link :to="{ path: '/products', query: { type: 'ELECTRIC' } }">Xe điện</router-link>
        <router-link :to="{ path: '/products', query: { type: 'SUPERCAR' } }">Siêu xe</router-link>
        <router-link to="/auctions" active-class="on">🔨 Đấu giá</router-link>
        <router-link :to="{ path: '/products', query: { type: 'ACCESSORY' } }">Phụ kiện</router-link>
        <router-link to="/promotions" active-class="on">Ưu đãi</router-link>
        <router-link to="/services" active-class="on">Dịch vụ & Hậu mãi</router-link>
      </div>
    </nav>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { useCart } from '../stores/cart'
import { HOTLINE } from '../format'
const auth = useAuth(), cart = useCart(), router = useRouter()
const q = ref('')
const search = () => router.push({ path: '/products', query: q.value ? { q: q.value } : {} })
const logout = () => { auth.logout(); router.push('/') }
</script>

<style scoped>
.header { position: sticky; top: 0; z-index: 50; background: var(--card); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); border-bottom: 1px solid var(--border); box-shadow: 0 10px 30px rgba(0,0,0,.4); }
.topbar { background: var(--primary-active); font-size: .8rem; padding: 5px 0; color: #e0f2fe; }
.main { padding: 14px 20px; gap: 22px; flex-wrap: nowrap; }
.logo { display: flex; align-items: center; gap: 10px; color: #fff; font-size: 1.3rem; letter-spacing: .04em; white-space: nowrap; }
.logo img { height: 40px; }
.logo b { color: var(--accent); }
.search { display: flex; gap: 8px; max-width: 560px; }
.search .input { padding: 9px 14px; }
.actions { flex-wrap: nowrap; }
.cart { position: relative; font-size: 1.5rem; color: #fff; }
.cnt { position: absolute; top: -6px; right: -10px; background: var(--danger); color: #fff; font-size: .7rem; font-weight: 700; border-radius: 999px; padding: 1px 6px; }
.user { color: var(--text); font-weight: 600; white-space: nowrap; }
.nav { border-top: 1px solid var(--border); }
.nav a { color: var(--label); padding: 10px 14px; font-weight: 600; font-size: .92rem; }
.nav a:hover, .nav a.on { color: var(--accent); }
@media (max-width: 900px) { .main { flex-wrap: wrap; } .search { order: 3; max-width: none; flex-basis: 100%; } .hide-sm { display: none; } }
</style>
