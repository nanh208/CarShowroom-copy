<template>
  <transition name="mn-fade"><div v-if="navOpen" class="mn-scrim" @click="navOpen = false" /></transition>
  <transition name="mn-slide">
    <aside v-if="navOpen" ref="panel" class="mnav" role="dialog" aria-modal="true" aria-label="Menu" @keydown.esc="navOpen = false">
      <div class="mn-head">
        <router-link to="/" class="mn-logo"><img src="/images/logo-mark.png" alt="" /><span><b>Ares</b> Toys</span></router-link>
        <button ref="closeBtn" class="mn-x" aria-label="Đóng menu" @click="navOpen = false">✕</button>
      </div>
      <form class="mn-search" role="search" @submit.prevent="search">
        <input v-model="q" type="search" placeholder="Tìm xe, hãng, phụ kiện…" aria-label="Tìm kiếm" enterkeyhint="search" />
        <button aria-label="Tìm">→</button>
      </form>
      <nav class="mn-links" aria-label="Danh mục">
        <router-link v-for="l in links" :key="l.label" :to="l.to">{{ l.label }}<span aria-hidden="true">›</span></router-link>
      </nav>
      <div class="mn-sec">
        <template v-if="auth.loggedIn">
          <div class="mn-user"><span class="av">{{ (auth.user.fullName || '?').split(' ').pop()[0] }}</span><div><b>{{ auth.user.fullName }}</b><small>{{ auth.user.email }}</small></div></div>
          <router-link to="/account">Tài khoản của tôi</router-link>
          <router-link to="/wishlist">Yêu thích ({{ prefs.wish.length }})</router-link>
          <router-link to="/compare">So sánh ({{ prefs.compare.length }})</router-link>
          <router-link v-if="auth.isStaff" to="/admin">Trang quản trị</router-link>
          <button class="mn-out" @click="logout">Đăng xuất</button>
        </template>
        <router-link v-else to="/login" class="btn btn-block">Đăng nhập / Đăng ký</router-link>
      </div>
    </aside>
  </transition>
</template>
<script setup>
import { ref, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { navOpen } from '../stores/ui'
import { useAuth } from '../stores/auth'
import { usePrefs } from '../stores/prefs'
const auth = useAuth(), prefs = usePrefs(), router = useRouter(), route = useRoute()
const q = ref(''), closeBtn = ref(null)
const links = [
  { label: 'Xe mô hình', to: { path: '/products', query: { type: 'MODEL,DISPLAY' } } },
  { label: 'Xe RC', to: { path: '/products', query: { type: 'RC' } } },
  { label: 'Phụ kiện', to: { path: '/products', query: { grp: 'ACCESSORY' } } },
  { label: 'Đấu giá', to: '/auctions' }, { label: 'Khuyến mãi', to: '/promotions' }, { label: 'Combo ưu đãi', to: '/bundles' }, { label: 'Bộ sưu tập', to: '/collections' }, { label: 'Sắp ra mắt', to: '/upcoming' }, { label: 'Tìm mẫu phù hợp', to: '/finder' }, { label: 'Phần thưởng', to: '/rewards' }, { label: 'Thông báo', to: '/notifications' }, { label: 'Thẻ quà tặng', to: '/gift-cards' }, { label: 'Tra cứu đơn hàng', to: '/track' },
  { label: 'Trung tâm hỗ trợ', to: '/support' }, { label: 'Câu hỏi thường gặp', to: '/faq' }, { label: 'Về chúng tôi', to: '/about' }]
const search = () => { navOpen.value = false; router.push({ path: '/products', query: q.value.trim() ? { q: q.value.trim() } : {} }); q.value = '' }
const logout = () => { auth.logout(); navOpen.value = false; router.push('/') }
watch(() => route.fullPath, () => { navOpen.value = false })
watch(navOpen, async v => { document.documentElement.style.overflow = v ? 'hidden' : ''; if (v) { await nextTick(); closeBtn.value?.focus() } })
</script>
<style scoped>
.mn-scrim { position: fixed; inset: 0; z-index: 180; background: rgba(0,0,0,.45); }
.mnav { position: fixed; top: 0; bottom: 0; left: 0; z-index: 190; width: min(88vw, 360px); background: var(--surface); color: var(--text); overflow-y: auto; overscroll-behavior: contain;
  display: flex; flex-direction: column; padding-bottom: calc(24px + env(safe-area-inset-bottom)); box-shadow: 12px 0 40px rgba(0,0,0,.25); }
.mn-head { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--border); }
.mn-logo { display: flex; align-items: center; gap: 6px; font-weight: 700; color: var(--text); } .mn-logo img { height: 30px; } .mn-logo b { color: var(--accent); }
.mn-x { width: 44px; height: 44px; border: none; background: none; color: var(--text); font-size: 1.2rem; cursor: pointer; }
.mn-search { display: flex; margin: 14px 16px 6px; border: 1.5px solid var(--text); }
.mn-search input { flex: 1; min-width: 0; height: 44px; border: none; padding: 0 12px; background: transparent; color: var(--text); font: inherit; outline: none; }
.mn-search button { width: 48px; border: none; background: var(--accent); color: #fff; font-size: 1.1rem; cursor: pointer; }
.mn-links { display: flex; flex-direction: column; padding: 4px 0; }
.mn-links a { display: flex; justify-content: space-between; align-items: center; min-height: 48px; padding: 0 18px; color: var(--text); font: 600 1rem/1.3 'Oswald', 'Be Vietnam Pro', sans-serif; text-transform: uppercase; letter-spacing: .03em; border-bottom: 1px solid var(--border); }
.mn-links a span { color: var(--accent); font-size: 1.3rem; }
.mn-sec { padding: 16px 18px; border-bottom: 1px solid var(--border); display: flex; flex-direction: column; gap: 2px; }
.mn-sec > a, .mn-out { min-height: 42px; display: flex; align-items: center; color: var(--text); background: none; border: none; font: 500 .95rem inherit; font-family: inherit; cursor: pointer; padding: 0; text-align: left; }
.mn-sec > a.btn { color: #fff; justify-content: center; }
.mn-h { font-size: .75rem; letter-spacing: .14em; text-transform: uppercase; color: var(--muted); margin-bottom: 10px; }
.mn-user { display: flex; gap: 10px; align-items: center; margin-bottom: 8px; } .mn-user small { display: block; color: var(--muted); font-size: .8rem; }
.av { width: 38px; height: 38px; border-radius: 50%; display: grid; place-items: center; background: var(--accent); color: #fff; font-weight: 800; }
.mn-slide-enter-active, .mn-slide-leave-active { transition: transform .22s ease-out; } .mn-slide-enter-from, .mn-slide-leave-to { transform: translateX(-100%); }
.mn-fade-enter-active, .mn-fade-leave-active { transition: opacity .2s ease-out; } .mn-fade-enter-from, .mn-fade-leave-to { opacity: 0; }
@media (prefers-reduced-motion: reduce) { .mn-slide-enter-active, .mn-slide-leave-active, .mn-fade-enter-active, .mn-fade-leave-active { transition: none; } }
</style>
