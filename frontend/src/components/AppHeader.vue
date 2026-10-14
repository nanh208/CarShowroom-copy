<template>
  <header :class="['hdr', { dark: onDark, scrolled }]" @mouseleave="mega = ''">
    <div class="wrap">
      <router-link to="/" class="logo" @mouseenter="mega = ''">
        <img src="/images/logo-mark.png" alt="" /><span><b>Ares</b> Toys</span>
      </router-link>
      <nav class="pill">
        <div class="menu">
          <router-link to="/" exact-active-class="on" @mouseenter="mega = ''">Trang chủ</router-link>
          <router-link to="/products" :class="{ on: isCars }" @mouseenter="mega = 'cars'">Danh mục xe</router-link>
          <router-link :to="{ path: '/products', query: { grp: 'ACCESSORY' } }" :class="{ on: isAcc }" @mouseenter="mega = 'acc'">Phụ kiện</router-link>
          <router-link to="/auctions" active-class="on" @mouseenter="mega = ''">Đấu giá</router-link>
          <router-link to="/support" active-class="on" @mouseenter="mega = ''">Dịch vụ</router-link>
          <router-link to="/faq" active-class="on" @mouseenter="mega = ''">Hỗ trợ</router-link>
          <router-link to="/about" active-class="on" @mouseenter="mega = ''">Về chúng tôi</router-link>
        </div>
        <div class="tools" @mouseenter="mega = ''">
          <div class="search" :class="{ open: sOpen }">
            <button class="ib" title="Tìm kiếm" @click="openSearch"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.4"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg></button>
            <input ref="sInput" v-model="q" placeholder="Tìm xe đồ chơi, hãng…" @input="suggest" @blur="close" @keydown.enter.prevent="search" @keydown.down.prevent="move(1)" @keydown.up.prevent="move(-1)" @keydown.esc="sOpen = false" />
            <div v-if="sOpen && q.trim() && (sugg.length || searched)" class="sugg">
              <router-link v-for="(x, i) in sugg" :key="x.id" :to="`/products/${x.id}`" :class="['si', { hl: i === hl }]" @mousedown.prevent="go(x)">
                <img :src="x.image" alt="" /><div><b v-html="mark(x.name)" /><small>{{ x.brand }} · {{ vnd(x.price) }}</small></div></router-link>
              <div v-if="!sugg.length" class="none">Không tìm thấy “{{ q }}”</div>
              <button v-else class="all" @mousedown.prevent="search">Xem tất cả kết quả →</button>
            </div>
          </div>
          <router-link to="/wishlist" class="ib" title="Yêu thích">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z"/></svg>
            <span v-if="prefs.wish.length" class="cnt">{{ prefs.wish.length }}</span></router-link>
          <div v-if="auth.loggedIn" class="bellwrap">
            <button class="ib" title="Thông báo" @click="toggleBell"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"/></svg>
              <span v-if="unread" class="cnt red">{{ unread }}</span></button>
            <div v-if="bell" class="panel">
              <div class="ph"><b>Thông báo</b><button v-if="notes.length" class="lnk" @click="readAll">Đánh dấu đã đọc</button></div>
              <div v-if="!notes.length" class="none">Chưa có thông báo nào.</div>
              <router-link v-for="n in notes" :key="n.id" :to="n.link || '/account'" :class="['ni', { unread: !n.read }]" @click="bell = false">
                <span :class="['kind', n.kind]">{{ n.kind === 'AUCTION' ? 'Đấu giá' : n.kind === 'ORDER' ? 'Đơn hàng' : 'Hệ thống' }}</span>
                <b>{{ n.title }}</b><p>{{ n.body }}</p><small>{{ dt(n.createdAt) }}</small></router-link>
            </div>
          </div>
          <router-link id="cart-target" to="/cart" :class="['cartpill', { bump }]">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6"/><circle cx="10" cy="20" r="1.3"/><circle cx="17" cy="20" r="1.3"/></svg>
            <span>{{ cart.count }} món</span>
          </router-link>
          <div class="userwrap">
            <router-link :to="auth.loggedIn ? '/account' : '/login'" class="avatar" :title="auth.loggedIn ? auth.user.fullName : 'Đăng nhập'">
              <template v-if="auth.loggedIn">{{ (auth.user.fullName || '?').split(' ').pop()[0] }}</template>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0z"/></svg>
            </router-link>
            <div v-if="auth.loggedIn" class="umenu">
              <b>{{ auth.user.fullName }}</b><small>{{ auth.user.email }}</small>
              <router-link to="/account">Tài khoản của tôi</router-link>
              <router-link to="/compare">So sánh xe ({{ prefs.compare.length }})</router-link>
              <router-link v-if="auth.isStaff" to="/admin">Trang quản trị</router-link>
              <button @click="logout">Đăng xuất</button>
            </div>
          </div>
        </div>
      </nav>
    </div>

    <transition name="mega">
      <div v-if="mega" class="mega" @mouseenter="mega = mega">
        <div class="mega-in">
          <div class="mtitle">{{ megaData[mega].title }}</div>
          <router-link v-for="t in megaData[mega].tiles" :key="t.label" :to="t.to" class="mtile" @click="mega = ''">
            <img :src="t.img" :alt="t.label" /><span>{{ t.label }}</span>
          </router-link>
          <router-link :to="megaData[mega].all" class="mall" @click="mega = ''">Tất cả<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M5 12h14m-6-6 6 6-6 6"/></svg></router-link>
        </div>
      </div>
    </transition>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../api'
import { useAuth } from '../stores/auth'
import { useCart } from '../stores/cart'
import { usePrefs } from '../stores/prefs'
import { vnd, dt } from '../format'
const auth = useAuth(), cart = useCart(), prefs = usePrefs(), router = useRouter(), route = useRoute()
const bump = ref(false)
watch(() => cart.fly, () => { bump.value = false; requestAnimationFrame(() => { bump.value = true; setTimeout(() => bump.value = false, 600) }) })
const mega = ref(''), scrolled = ref(false), scrollY = ref(0)
const isAcc = computed(() => route.path === '/products' && route.query.grp === 'ACCESSORY')
const isCars = computed(() => (route.path === '/products' && route.query.grp !== 'ACCESSORY') || route.path.startsWith('/products/'))
// Chữ logo đổi trắng/đen theo độ sáng nền: trên hero tối (trang chủ, đầu trang) => chữ trắng
const onDark = computed(() => route.meta.darkHero && scrollY.value < 380)
const onScroll = () => { scrollY.value = window.scrollY; scrolled.value = window.scrollY > 8 }
const megaData = {
  cars: { title: 'Danh mục xe', all: { path: '/products', query: { grp: 'TOY' } }, tiles: [
    { label: 'Xe Model', img: '/images/toys/hw-r34-0.jpg', to: { path: '/products', query: { type: 'MODEL' } } },
    { label: 'Xe RC', img: '/images/toys/traxxas-slash-0.jpg', to: { path: '/products', query: { type: 'RC' } } },
    { label: 'Xe Trưng Bày', img: '/images/toys/kyosho-countach-0.jpg', to: { path: '/products', query: { type: 'DISPLAY' } } }] },
  acc: { title: 'Phụ kiện', all: { path: '/products', query: { grp: 'ACCESSORY' } }, tiles: [
    { label: 'Pin', img: '/images/toys/lipo-2s-0.jpg', to: { path: '/products', query: { type: 'BATTERY' } } },
    { label: 'Khiển RC', img: '/images/toys/futaba-0.jpg', to: { path: '/products', query: { type: 'REMOTE' } } },
    { label: 'Bánh xe RC', img: '/images/toys/wheels-sc-0.jpg', to: { path: '/products', query: { type: 'WHEEL' } } }] }
}
const q = ref(''), sugg = ref([]), sOpen = ref(false), hl = ref(-1), searched = ref(false), sInput = ref(null)
const openSearch = async () => { if (sOpen.value && q.value.trim()) return search(); sOpen.value = !sOpen.value; await nextTick(); sInput.value?.focus() }
let timer
const suggest = () => { clearTimeout(timer); hl.value = -1; searched.value = false; const term = q.value.trim(); if (!term) { sugg.value = []; return }
  timer = setTimeout(async () => { const r = await api.get('/products/suggest', { params: { q: term } }); if (q.value.trim() === term) { sugg.value = r.data; searched.value = true } }, 200) }
const esc = s => s.replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
const mark = name => { const t = q.value.trim(); const i = name.toLowerCase().indexOf(t.toLowerCase()); return i < 0 ? esc(name) : esc(name.slice(0, i)) + '<mark>' + esc(name.slice(i, i + t.length)) + '</mark>' + esc(name.slice(i + t.length)) }
const move = d => { if (sugg.value.length) hl.value = (hl.value + d + sugg.value.length) % sugg.value.length }
const close = () => setTimeout(() => { if (!q.value.trim()) sOpen.value = false; searched.value = false }, 150)
const go = x => { sOpen.value = false; q.value = ''; sugg.value = []; router.push(`/products/${x.id}`) }
const search = () => { if (hl.value >= 0 && sugg.value[hl.value]) return go(sugg.value[hl.value]); sOpen.value = false; router.push({ path: '/products', query: q.value ? { q: q.value } : {} }); q.value = '' }
// ===== Thông báo =====
const bell = ref(false), notes = ref([]), unread = ref(0)
let poll
const loadNotes = async () => { if (!auth.loggedIn) return; try { const r = await api.get('/account/notifications'); notes.value = r.data.items; unread.value = r.data.unread } catch {} }
const toggleBell = () => { bell.value = !bell.value; if (bell.value) loadNotes() }
const readAll = async () => { await api.post('/account/notifications/read'); notes.value.forEach(n => (n.read = true)); unread.value = 0 }
const outside = e => { if (bell.value && !e.target.closest('.bellwrap')) bell.value = false }
watch(() => auth.loggedIn, v => { v ? loadNotes() : (notes.value = [], unread.value = 0) })
watch(() => route.fullPath, () => { mega.value = ''; bell.value = false })
onMounted(() => { window.addEventListener('scroll', onScroll); document.addEventListener('click', outside); onScroll(); loadNotes(); poll = setInterval(loadNotes, 30000) })
onUnmounted(() => { window.removeEventListener('scroll', onScroll); document.removeEventListener('click', outside); clearInterval(poll) })
const logout = () => { auth.logout(); router.push('/') }
</script>

<style scoped>
.hdr { position: fixed; top: 0; left: 0; right: 0; z-index: 50; padding: 14px 0 0; pointer-events: none; transition: background .25s; }
.hdr.scrolled:not(.dark) { background: linear-gradient(#fff 70%, rgba(255,255,255,0)); }
.wrap { max-width: 1180px; margin: 0 auto; padding: 0 24px; display: flex; align-items: center; gap: 16px; pointer-events: auto; }
.logo { display: flex; align-items: center; gap: 6px; flex-shrink: 0; color: #4a4a4f; font-family: var(--head); font-size: 1.05rem; transition: color .25s; }
.logo img { height: 34px; } .logo b { color: var(--bronze); font-weight: 700; }
.dark .logo { color: #fff; } .dark .logo b { color: #e0b878; }
.pill { flex: 1; display: flex; align-items: center; justify-content: space-between; height: 44px; padding: 0 6px 0 10px; border-radius: 10px; background: var(--bronze); box-shadow: 0 10px 24px -10px rgba(111,79,34,.55); }
.menu { display: flex; align-items: center; height: 100%; }
.menu a { position: relative; color: #fff; font-weight: 500; font-size: .88rem; padding: 0 14px; height: 100%; display: flex; align-items: center; white-space: nowrap; opacity: .92; }
.menu a:hover { opacity: 1; background: rgba(0,0,0,.08); color: #fff; }
.menu a.on { opacity: 1; font-weight: 700; }
.menu a.on::after { content: ''; position: absolute; left: 12px; right: 12px; bottom: 6px; height: 2px; border-radius: 2px; background: #f7d9a8; }
.tools { display: flex; align-items: center; gap: 6px; }
.ib { position: relative; width: 32px; height: 32px; border-radius: 7px; border: none; background: rgba(0,0,0,.18); color: #fff; display: flex; align-items: center; justify-content: center; cursor: pointer; }
.ib:hover { background: rgba(0,0,0,.3); color: #fff; }
.cnt { position: absolute; top: -5px; right: -5px; min-width: 16px; height: 16px; border-radius: 8px; background: #fff; color: var(--bronze-3); font-size: .62rem; font-weight: 800; display: flex; align-items: center; justify-content: center; padding: 0 3px; }
.cnt.red { background: var(--danger); color: #fff; }
.search { position: relative; display: flex; align-items: center; }
.search input { width: 0; padding: 0; border: none; outline: none; background: rgba(0,0,0,.18); color: #fff; height: 32px; border-radius: 0 7px 7px 0; font: inherit; font-size: .85rem; transition: width .25s, padding .25s; }
.search input::placeholder { color: rgba(255,255,255,.75); }
.search.open .ib { border-radius: 7px 0 0 7px; } .search.open input { width: 170px; padding: 0 10px 0 2px; }
.cartpill { display: flex; align-items: center; gap: 8px; height: 32px; padding: 0 12px; border-radius: 7px; background: rgba(0,0,0,.18); color: #fff; font-size: .82rem; font-weight: 600; }
.cartpill:hover { background: rgba(0,0,0,.3); color: #fff; }
.userwrap { position: relative; }
.avatar { width: 32px; height: 32px; border-radius: 50%; background: #f4ebdd; color: var(--bronze-3); display: flex; align-items: center; justify-content: center; font-weight: 800; margin-left: 4px; }
.umenu { display: none; position: absolute; right: 0; top: 40px; width: 230px; background: #fff; border-radius: 10px; box-shadow: 0 20px 40px -12px rgba(0,0,0,.3); padding: 12px; border: 1px solid var(--border); }
.userwrap:hover .umenu { display: flex; flex-direction: column; }
.umenu::before { content: ''; position: absolute; top: -10px; left: 0; right: 0; height: 10px; }
.umenu b { font-size: .92rem; } .umenu small { color: var(--muted); margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid var(--border); }
.umenu a, .umenu button { padding: 8px; border-radius: 6px; color: var(--text); font: 500 .88rem inherit; font-family: inherit; background: none; border: none; text-align: left; cursor: pointer; }
.umenu a:hover, .umenu button:hover { background: var(--bg-2); }
/* mega menu */
.mega { pointer-events: auto; max-width: 1180px; margin: 8px auto 0; padding: 0 24px; }
.mega-in { display: grid; grid-template-columns: 1.2fr 1fr 1fr 1fr .45fr; gap: 10px; padding: 10px; border-radius: 12px; background: var(--bronze); box-shadow: 0 24px 48px -16px rgba(0,0,0,.45); }
.mtitle { display: flex; align-items: center; padding-left: 14px; color: #fff; font-family: var(--head); font-size: 1.3rem; }
.mtile { position: relative; border-radius: 8px; overflow: hidden; aspect-ratio: 1.1; background: #eee; }
.mtile img { width: 100%; height: 100%; object-fit: cover; transition: transform .4s; } .mtile:hover img { transform: scale(1.06); }
.mtile span { position: absolute; left: 0; right: 0; bottom: 0; padding: 24px 10px 8px; background: linear-gradient(transparent, rgba(0,0,0,.6)); color: #fff; font-family: var(--head); font-weight: 600; }
.mall { border-radius: 8px; background: var(--bronze-3); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; font-family: var(--head); font-weight: 600; }
.mall:hover { background: #5a3f19; color: #fff; }
.mega-enter-active, .mega-leave-active { transition: .18s; } .mega-enter-from, .mega-leave-to { opacity: 0; transform: translateY(-6px); }
/* gợi ý tìm kiếm & thông báo */
.sugg, .panel { position: absolute; top: 42px; right: 0; width: 380px; background: #fff; border: 1px solid var(--border); border-radius: 12px; box-shadow: 0 24px 48px -12px rgba(0,0,0,.3); z-index: 70; }
.sugg { padding: 8px; }
.si { display: flex; gap: 12px; align-items: center; padding: 8px; border-radius: 8px; color: var(--text); }
.si:hover, .si.hl { background: var(--bg-2); color: var(--text); }
.si img { width: 72px; height: 45px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.si b { display: block; font-size: .9rem; font-weight: 600; } .si small { color: var(--muted); font-size: .8rem; }
.si :deep(mark) { background: none; color: var(--bronze); font-weight: 800; }
.none { padding: 16px; color: var(--muted); font-size: .9rem; }
.all { width: 100%; background: none; border: none; border-top: 1px solid var(--border); color: var(--bronze); padding: 12px; margin-top: 4px; cursor: pointer; font: 600 .85rem inherit; font-family: inherit; }
.bellwrap { position: relative; }
.panel { right: -110px; max-height: 480px; overflow-y: auto; }
.ph { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; border-bottom: 1px solid var(--border); position: sticky; top: 0; background: #fff; }
.lnk { background: none; border: none; color: var(--bronze); font: 600 .8rem inherit; font-family: inherit; cursor: pointer; }
.ni { display: block; padding: 14px 18px; border-bottom: 1px solid var(--border); color: var(--text); }
.ni:hover { background: #fafafa; color: var(--text); } .ni.unread { background: var(--bronze-soft); }
.ni b { display: block; font-size: .9rem; margin: 4px 0 2px; } .ni p { color: var(--text-2); font-size: .84rem; } .ni small { color: var(--muted); font-size: .74rem; }
.kind { font-size: .68rem; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; color: var(--bronze); } .kind.AUCTION { color: #c62828; }
@media (max-width: 1100px) { .menu a { padding: 0 9px; font-size: .82rem; } }
@media (max-width: 900px) { .menu { overflow-x: auto; } .logo span { display: none; } }
.cartpill.bump { animation: bump .6s cubic-bezier(.3,1.6,.5,1); }
@keyframes bump { 30% { transform: scale(1.18); } 60% { transform: scale(.94); } }
</style>
