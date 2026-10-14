<template>
  <header class="header" :class="{ solid: scrolled }">
    <div class="container bar">
      <router-link to="/" class="logo">
        <img src="/images/logo.png" alt="ARES" @error="e => e.target.style.display = 'none'" />
        <span>ARES <b>AUTO</b></span>
      </router-link>
      <nav class="nav">
        <router-link to="/products" active-class="on">Mẫu xe</router-link>
        <router-link :to="{ path: '/products', query: { type: 'SUPERCAR' } }">Siêu xe</router-link>
        <router-link :to="{ path: '/products', query: { type: 'ELECTRIC' } }">Xe điện</router-link>
        <router-link to="/auctions" active-class="on">Đấu giá</router-link>
        <router-link to="/promotions" active-class="on">Ưu đãi</router-link>
        <router-link to="/services" active-class="on">Dịch vụ</router-link>
      </nav>
      <div class="actions">
        <form class="search" @submit.prevent="search">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
          <input v-model="q" placeholder="Tìm mẫu xe…" @input="suggest" @focus="open = true" @blur="close" @keydown.down.prevent="move(1)" @keydown.up.prevent="move(-1)" />
          <div v-if="open && q.trim() && (sugg.length || searched)" class="sugg">
            <router-link v-for="(x, i) in sugg" :key="x.id" :to="`/products/${x.id}`" :class="['si', { hl: i === hl }]" @mousedown.prevent="go(x)">
              <img :src="x.images?.[0]" alt="" /><div><b v-html="mark(x.name)" /><small>{{ x.brand?.name }} · {{ vnd(x.price) }}</small></div></router-link>
            <div v-if="!sugg.length" class="none">Không tìm thấy “{{ q }}”</div>
            <button v-else class="all" @mousedown.prevent="search">Xem tất cả kết quả →</button>
          </div>
        </form>
        <router-link to="/wishlist" class="icon" title="Yêu thích">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z"/></svg>
          <span v-if="prefs.wish.length" class="cnt">{{ prefs.wish.length }}</span>
        </router-link>
        <div v-if="auth.loggedIn" class="bellwrap">
          <button class="icon bell" title="Thông báo" @click="toggleBell">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"/></svg>
            <span v-if="unread" class="cnt red">{{ unread }}</span>
          </button>
          <div v-if="bell" class="panel">
            <div class="ph"><b>Thông báo</b><button v-if="notes.length" class="link" @click="readAll">Đánh dấu đã đọc</button></div>
            <div v-if="!notes.length" class="none">Chưa có thông báo nào.</div>
            <router-link v-for="n in notes" :key="n.id" :to="n.link || '/account'" :class="['ni', { unread: !n.read }]" @click="bell = false">
              <span :class="['kind', n.kind]">{{ n.kind === 'AUCTION' ? 'Đấu giá' : n.kind === 'ORDER' ? 'Đơn hàng' : 'Hệ thống' }}</span>
              <b>{{ n.title }}</b><p>{{ n.body }}</p><small>{{ dt(n.createdAt) }}</small></router-link>
          </div>
        </div>
        <router-link to="/cart" class="icon" title="Giỏ hàng">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6"/><circle cx="10" cy="20" r="1.3"/><circle cx="17" cy="20" r="1.3"/></svg>
          <span v-if="cart.count" class="cnt">{{ cart.count }}</span>
        </router-link>
        <template v-if="auth.loggedIn">
          <router-link v-if="auth.isAdmin" to="/admin" class="link">Quản trị</router-link>
          <router-link to="/account" class="avatar" :title="auth.user.fullName">{{ (auth.user.fullName || '?').split(' ').pop()[0] }}</router-link>
          <button class="link" @click="logout">Thoát</button>
        </template>
        <router-link v-else to="/login" class="btn btn-sm">Đăng nhập</router-link>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { useAuth } from '../stores/auth'
import { useCart } from '../stores/cart'
import { usePrefs } from '../stores/prefs'
import { vnd, dt } from '../format'
const auth = useAuth(), cart = useCart(), prefs = usePrefs(), router = useRouter()
const q = ref(''), scrolled = ref(false), sugg = ref([]), open = ref(false), hl = ref(-1), searched = ref(false)
const onScroll = () => { scrolled.value = window.scrollY > 8 }
// ===== Gợi ý tìm kiếm =====
let timer
const suggest = () => { clearTimeout(timer); hl.value = -1; searched.value = false; const term = q.value.trim(); if (!term) { sugg.value = []; return }
  timer = setTimeout(async () => { const r = await api.get('/products', { params: { q: term, size: 5 } }); if (q.value.trim() === term) { sugg.value = r.data.content; searched.value = true } }, 200) }
const esc = s => s.replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
const mark = name => { const t = q.value.trim(); const i = name.toLowerCase().indexOf(t.toLowerCase()); return i < 0 ? esc(name) : esc(name.slice(0, i)) + '<mark>' + esc(name.slice(i, i + t.length)) + '</mark>' + esc(name.slice(i + t.length)) }
const move = d => { if (sugg.value.length) hl.value = (hl.value + d + sugg.value.length) % sugg.value.length }
const close = () => setTimeout(() => { open.value = false }, 120)
const go = x => { open.value = false; q.value = ''; sugg.value = []; router.push(`/products/${x.id}`) }
const search = () => { if (hl.value >= 0 && sugg.value[hl.value]) return go(sugg.value[hl.value]); open.value = false; router.push({ path: '/products', query: q.value ? { q: q.value } : {} }) }
// ===== Thông báo =====
const bell = ref(false), notes = ref([]), unread = ref(0)
let poll
const loadNotes = async () => { if (!auth.loggedIn) return; try { const r = await api.get('/account/notifications'); notes.value = r.data.items; unread.value = r.data.unread } catch {} }
const toggleBell = () => { bell.value = !bell.value; if (bell.value) loadNotes() }
const readAll = async () => { await api.post('/account/notifications/read'); notes.value.forEach(n => (n.read = true)); unread.value = 0 }
const outside = e => { if (bell.value && !e.target.closest('.bellwrap')) bell.value = false }
watch(() => auth.loggedIn, v => { v ? loadNotes() : (notes.value = [], unread.value = 0) })
onMounted(() => { window.addEventListener('scroll', onScroll); document.addEventListener('click', outside); onScroll(); loadNotes(); poll = setInterval(loadNotes, 30000) })
onUnmounted(() => { window.removeEventListener('scroll', onScroll); document.removeEventListener('click', outside); clearInterval(poll) })
const logout = () => { auth.logout(); router.push('/') }
</script>

<style scoped>
.header { position: sticky; top: 0; z-index: 50; background: rgba(10,11,14,.55); backdrop-filter: blur(20px) saturate(140%); -webkit-backdrop-filter: blur(20px) saturate(140%); border-bottom: 1px solid transparent; transition: .25s; }
.header.solid { background: rgba(10,11,14,.88); border-color: var(--border); }
.bar { height: 72px; display: flex; align-items: center; gap: 24px; }
.logo { flex-shrink: 0; display: flex; align-items: center; gap: 10px; color: #fff; font-size: 1.15rem; letter-spacing: .14em; white-space: nowrap; font-weight: 500; }
.logo img { height: 36px; }
.logo b { color: var(--accent); font-weight: 800; }
.nav { display: flex; gap: 0; flex: 1; margin-left: 8px; }
.nav a { white-space: nowrap; color: #cbd5e1; padding: 8px 11px; font-weight: 500; font-size: .95rem; border-radius: 999px; position: relative; transition: .2s; }
.nav a:hover { color: #fff; background: rgba(255,255,255,.06); }
.nav a.on { color: #fff; }
.nav a.on::after { content: ''; position: absolute; left: 14px; right: 14px; bottom: 0; height: 2px; background: var(--accent); border-radius: 2px; }
.actions { display: flex; align-items: center; gap: 18px; }
.search { display: flex; align-items: center; gap: 8px; height: 40px; padding: 0 16px; border-radius: 999px; background: rgba(255,255,255,.06); border: 1px solid var(--border); color: var(--muted); transition: .2s; }
.search:focus-within { border-color: var(--accent); background: rgba(255,255,255,.1); }
.search input { background: none; border: none; outline: none; color: #fff; font: inherit; font-size: .9rem; width: 96px; }
.icon { position: relative; color: #e2e8f0; display: flex; } .icon:hover { color: var(--accent); }
.cnt { position: absolute; top: -6px; right: -8px; background: var(--primary); color: #fff; font-size: .68rem; font-weight: 700; border-radius: 999px; min-width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; }
.link { background: none; border: none; color: #cbd5e1; font: 500 .9rem inherit; font-family: inherit; cursor: pointer; }
.link:hover { color: #fff; }
.avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), #0c4a6e); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; }
@media (max-width: 1520px) { .search input { width: 0; } .search { padding: 0 11px; } .search:focus-within input { width: 140px; } }
@media (max-width: 900px) { .nav { overflow-x: auto; } .nav a { white-space: nowrap; } }
.search { position: relative; }
.sugg { position: absolute; top: 48px; right: 0; width: 380px; background: rgba(16,17,21,.98); border: 1px solid var(--border); border-radius: 16px; box-shadow: 0 24px 48px -12px rgba(0,0,0,.8); padding: 8px; z-index: 70; }
.si { display: flex; gap: 12px; align-items: center; padding: 8px; border-radius: 10px; color: var(--text); }
.si:hover, .si.hl { background: rgba(255,255,255,.06); color: var(--text); }
.si img { width: 72px; height: 45px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.si b { display: block; font-size: .9rem; font-weight: 600; } .si small { color: var(--muted); font-size: .8rem; }
.si :deep(mark) { background: none; color: var(--accent); }
.none { padding: 16px; color: var(--muted); font-size: .9rem; }
.all { width: 100%; background: none; border: none; border-top: 1px solid var(--border); color: var(--accent); padding: 12px; margin-top: 4px; cursor: pointer; font: 600 .85rem inherit; font-family: inherit; }
.bell { background: none; border: none; cursor: pointer; padding: 0; }
.cnt.red { background: var(--danger); }
.bellwrap { position: relative; display: flex; }
.panel { position: absolute; top: 44px; right: -60px; width: 380px; max-height: 480px; overflow-y: auto; background: rgba(16,17,21,.98); border: 1px solid var(--border); border-radius: 16px; box-shadow: 0 24px 48px -12px rgba(0,0,0,.8); z-index: 70; }
.ph { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--border); position: sticky; top: 0; background: rgba(16,17,21,.98); }
.ph .link { color: var(--accent); font-size: .8rem; }
.ni { display: block; padding: 16px 20px; border-bottom: 1px solid var(--border); color: var(--text); }
.ni:hover { background: rgba(255,255,255,.04); color: var(--text); } .ni.unread { background: rgba(2,132,199,.08); }
.ni b { display: block; font-size: .92rem; margin: 6px 0 4px; } .ni p { color: var(--muted); font-size: .85rem; } .ni small { color: var(--muted); font-size: .75rem; }
.kind { font-size: .7rem; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; color: var(--accent); } .kind.AUCTION { color: #fbbf24; }
</style>
