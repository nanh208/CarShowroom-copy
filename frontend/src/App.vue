<template>
  <transition name="fade"><div v-if="!online" class="offline-bar" role="status">{{ $t('Bạn đang ngoại tuyến — đang hiển thị dữ liệu đã lưu.') }}</div></transition>
  <SiteBackground />
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
import SiteBackground from './components/SiteBackground.vue'
import { loadAppearance } from './stores/theme'
loadAppearance()
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
import { usePrefs } from './stores/prefs'
const prefsStore = usePrefs()
import { reconnect as rtReconnect } from './realtime'
import { syncLocale } from './i18n'
import { online } from './pwa'
watch(() => auth.loggedIn && auth.user?.id, (v, old) => { if (v) { prefsStore.syncFromServer(); syncLocale() } if (old !== undefined) rtReconnect() }, { immediate: true })
watch(() => cart.items.map(i => i.key + ':' + i.quantity).join(), () => {
  if (!auth.loggedIn) return
  clearTimeout(syncT); syncT = setTimeout(() => api.put('/public/cart-sync', { items: cart.items.map(i => ({ productId: i.productId, variantId: i.variantId, name: i.name, image: i.image, quantity: i.quantity, unitPrice: i.unitPrice })), subtotal: cart.subtotal }).catch(() => {}), 4000)
})
</script>

<style>
.offline-bar { position: fixed; left: 50%; bottom: 76px; transform: translateX(-50%); z-index: 90; background: #1d1d1f; color: #fff; padding: 8px 14px; font-size: .82rem; border-radius: 99px; box-shadow: 0 8px 24px -8px rgba(0,0,0,.4); max-width: calc(100% - 24px); text-align: center; }
</style>
