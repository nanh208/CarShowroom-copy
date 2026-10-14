<template>
  <div class="admin">
    <header class="top">
      <div class="container row between">
        <div class="row">
          <router-link to="/admin" class="brand">ARES <b>ADMIN</b></router-link>
          <nav class="nav">
            <router-link to="/admin" exact-active-class="on">Tổng quan</router-link>
            <router-link to="/admin/products" active-class="on">Sản phẩm</router-link>
            <router-link to="/admin/orders" active-class="on">Đơn hàng</router-link>
            <router-link to="/admin/customers" active-class="on">Khách hàng</router-link>
            <router-link to="/admin/promotions" active-class="on">Ưu đãi</router-link>
            <router-link to="/admin/bookings" active-class="on">Lịch hẹn</router-link>
            <router-link to="/admin/chat" active-class="on">Live chat</router-link>
          </nav>
        </div>
        <div class="row">
          <span class="small">{{ auth.user?.fullName }}</span>
          <router-link to="/" class="btn btn-sm btn-outline light">Cửa hàng</router-link>
          <button class="btn btn-sm light" @click="logout">Đăng xuất</button>
        </div>
      </div>
    </header>
    <main class="content container"><router-view /></main>
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '../../stores/auth'
const auth = useAuth(), router = useRouter()
const logout = () => { auth.logout(); router.push('/login') }
</script>
<style scoped>
.admin { min-height: 100vh; background: #f5f5f5; }
.top { background: var(--header); color: #fff; height: 52px; display: flex; align-items: center; position: sticky; top: 0; z-index: 40; }
.brand { color: #fff; font: 700 1rem 'Montserrat', sans-serif; letter-spacing: .1em; margin-right: 18px; }
.brand b { color: #fff; opacity: .9; } .brand:hover { color: #fff; }
.nav { display: flex; gap: 2px; flex-wrap: wrap; }
.nav a { color: rgba(255,255,255,.9); padding: 6px 10px; font: 500 .78rem 'Montserrat', sans-serif; text-transform: uppercase; letter-spacing: .03em; }
.nav a:hover, .nav a.on { background: rgba(0,0,0,.18); color: #fff; }
.light { border-color: rgba(255,255,255,.5); color: #fff; background: rgba(0,0,0,.15); }
.light:hover { background: rgba(0,0,0,.3); color: #fff; }
.content { padding: 24px 20px 50px; }
:deep(h1) { margin-bottom: 16px; font-size: 1.45rem; }
@media (max-width: 900px) { .nav { display: none; } }
</style>
