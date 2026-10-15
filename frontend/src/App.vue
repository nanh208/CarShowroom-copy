<template>
  <RoadProgress />
  <template v-if="$route.meta.bare || $route.path.startsWith('/admin')">
    <router-view />
  </template>
  <template v-else>
    <AppHeader />
    <main>
      <router-view v-slot="{ Component, route }">
        <transition name="page" mode="out-in"><component :is="Component" :key="route.path" /></transition>
      </router-view>
    </main>
    <AppFooter />
    <ChatWidget />
    <CompareBar />
    <TabBar />
    <MobileNav />
    <MiniCart />
    <RecentBuyers />
  </template>
  <Toasts />
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'
import ChatWidget from './components/ChatWidget.vue'
import CompareBar from './components/CompareBar.vue'
import Toasts from './components/Toasts.vue'
import RoadProgress from './components/RoadProgress.vue'
import TabBar from './components/TabBar.vue'
import MobileNav from './components/MobileNav.vue'
import MiniCart from './components/MiniCart.vue'
import RecentBuyers from './components/RecentBuyers.vue'
import { useCart } from './stores/cart'
import { useAuth } from './stores/auth'
import api from './api'
import { watch } from 'vue'
import { loadCatalog } from './format'
loadCatalog()
// Đồng bộ giỏ lên máy chủ (để nhắc giỏ bị bỏ quên) — chỉ khi đăng nhập, debounce 4s
const cart = useCart(), auth = useAuth(); let syncT
watch(() => cart.items.map(i => i.key + ':' + i.quantity).join(), () => {
  if (!auth.loggedIn) return
  clearTimeout(syncT); syncT = setTimeout(() => api.put('/public/cart-sync', { items: cart.items.map(i => ({ productId: i.productId, variantId: i.variantId, name: i.name, image: i.image, quantity: i.quantity, unitPrice: i.unitPrice })), subtotal: cart.subtotal }).catch(() => {}), 4000)
})
</script>
