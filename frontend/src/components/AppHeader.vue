<template>
  <header class="hdr">
    <div class="bar">
      <div class="container row bar-inner">
        <router-link to="/" class="logo">
          <img src="/images/logo.png" alt="ARES" />
          <span>ARES</span>
        </router-link>
        <nav class="nav">
          <router-link to="/" exact-active-class="on">Trang chủ</router-link>
          <router-link to="/products" active-class="on">Sản phẩm</router-link>
          <router-link to="/promotions" active-class="on">Ưu đãi</router-link>
          <router-link to="/services" active-class="on">Dịch vụ</router-link>
          <a href="#contact" @click.prevent="$router.push('/')">Liên hệ</a>
        </nav>
        <div class="actions row">
          <button class="icon" title="Tìm kiếm" @click="showSearch = !showSearch">🔍</button>
          <router-link to="/cart" class="icon cart">🛒<span v-if="cart.count" class="cnt">{{ cart.count }}</span></router-link>
          <router-link v-if="auth.isAdmin" to="/admin" class="icon" title="Admin">⚙️</router-link>
          <router-link v-if="auth.loggedIn" to="/account" class="icon" title="Tài khoản">👤</router-link>
          <router-link v-else to="/login" class="icon" title="Đăng nhập">👤</router-link>
        </div>
      </div>
    </div>
    <form v-if="showSearch" class="search-row" @submit.prevent="search">
      <div class="container row">
        <input v-model="q" class="input grow" placeholder="Tìm mô hình, thương hiệu... (VD: Ferrari, 1:18)" autofocus />
        <button class="btn btn-sm">Tìm</button>
        <button type="button" class="btn btn-sm btn-outline" @click="showSearch = false">Đóng</button>
      </div>
    </form>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { useCart } from '../stores/cart'
const auth = useAuth(), cart = useCart(), router = useRouter()
const q = ref(''), showSearch = ref(false)
const search = () => { router.push({ path: '/products', query: q.value ? { q: q.value } : {} }); showSearch.value = false }
</script>

<style scoped>
.hdr { position: sticky; top: 0; z-index: 50; }
.bar { background: var(--header); height: 48px; }
.bar-inner { height: 48px; gap: 22px; flex-wrap: nowrap; }
.logo { display: flex; align-items: center; gap: 8px; color: #fff; font: 700 .95rem 'Montserrat', sans-serif; letter-spacing: .12em; white-space: nowrap; }
.logo img { height: 28px; filter: brightness(1.1); }
.nav { display: flex; gap: 4px; flex: 1; justify-content: center; }
.nav a { color: rgba(255,255,255,.92); padding: 6px 12px; font: 500 .82rem 'Montserrat', sans-serif; letter-spacing: .04em; text-transform: uppercase; }
.nav a:hover, .nav a.on { color: #fff; background: rgba(0,0,0,.15); }
.actions { flex-wrap: nowrap; gap: 4px; }
.icon { background: none; border: none; color: #fff; font-size: 1.15rem; cursor: pointer; padding: 4px 8px; position: relative; text-decoration: none; }
.icon:hover { opacity: .85; color: #fff; }
.cnt { position: absolute; top: -2px; right: 0; background: var(--red); color: #fff; font-size: .65rem; font-weight: 700; border-radius: 999px; padding: 0 5px; }
.search-row { background: #fff; border-bottom: 1px solid var(--line); padding: 10px 0; }
@media (max-width: 800px) {
  .nav { display: none; }
  .bar-inner { justify-content: space-between; }
}
</style>
