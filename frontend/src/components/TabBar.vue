<template>
  <nav class="tabbar" aria-label="Điều hướng nhanh">
    <router-link to="/" :class="{ on: route.path === '/' }"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 11 12 4l9 7v9h-6v-6H9v6H3z"/></svg><span>Trang chủ</span></router-link>
    <router-link to="/products" :class="{ on: route.path.startsWith('/products') }"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg><span>Danh mục</span></router-link>
    <router-link id="cart-target-m" to="/cart" :class="{ on: route.path === '/cart' || route.path === '/checkout', bump }"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6"/><circle cx="10" cy="20" r="1.3"/><circle cx="17" cy="20" r="1.3"/></svg><span>Giỏ hàng</span><b v-if="cart.count" class="bdg" :aria-label="`${cart.count} sản phẩm`">{{ cart.count > 99 ? '99+' : cart.count }}</b></router-link>
    <router-link :to="auth.loggedIn ? '/account' : '/login'" :class="{ on: route.path.startsWith('/account') || route.path === '/login' }"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg><span>Tài khoản</span></router-link>
  </nav>
</template>
<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useCart } from '../stores/cart'
import { useAuth } from '../stores/auth'
const route = useRoute(), cart = useCart(), auth = useAuth(), bump = ref(false)
watch(() => cart.fly, () => { bump.value = false; requestAnimationFrame(() => { bump.value = true; setTimeout(() => (bump.value = false), 320) }) })
</script>
<style scoped>
.tabbar { display: none; }
@media (max-width: 899px) {
  .tabbar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 120; display: grid; grid-template-columns: repeat(4, 1fr); background: var(--surface); border-top: 1px solid var(--border);
    padding-bottom: env(safe-area-inset-bottom); box-shadow: 0 -6px 18px rgba(0,0,0,.06); }
  .tabbar a { position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px; height: 58px; color: var(--muted); font: 600 .68rem/1 'Be Vietnam Pro', sans-serif; }
  .tabbar a.on { color: var(--accent); } .tabbar a.on::before { content: ''; position: absolute; top: 0; left: 30%; right: 30%; height: 3px; background: var(--accent); }
  .tabbar svg { width: 22px; height: 22px; fill: none; stroke: currentColor; stroke-width: 1.9; stroke-linejoin: round; }
  .bdg { position: absolute; top: 6px; left: calc(50% + 6px); min-width: 18px; height: 18px; padding: 0 4px; border-radius: 9px; background: var(--accent); color: #fff; font-size: .66rem; display: grid; place-items: center; }
  .bump svg, .bump .bdg { animation: tb-bump .3s ease-out; }
}
@keyframes tb-bump { 40% { transform: scale(1.28); } }
@media (prefers-reduced-motion: reduce) { .bump svg, .bump .bdg { animation: none; } }
</style>
