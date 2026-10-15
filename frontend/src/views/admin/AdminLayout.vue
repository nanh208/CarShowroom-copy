<template>
  <div :class="['admin', { mini: collapsed }]">
    <div v-if="drawer" class="scrim" @click="drawer = false" />
    <aside :class="['side', { open: drawer }]" aria-label="Menu quản trị">
      <div class="side-top">
        <router-link to="/" class="logo"><span>ARES</span> <b>TOYS</b></router-link>
        <button class="collapse" :aria-label="collapsed ? 'Mở rộng menu' : 'Thu gọn menu'" :title="collapsed ? 'Mở rộng' : 'Thu gọn'" @click="collapsed = !collapsed">{{ collapsed ? '»' : '«' }}</button>
      </div>
      <nav>
        <router-link v-for="m in menu" :key="m.to" :to="m.to" :exact-active-class="m.exact ? 'on' : ''" :active-class="m.exact ? '' : 'on'" :title="collapsed ? m.label : null"><svg viewBox="0 0 24 24" aria-hidden="true"><path :d="m.d"/></svg><span class="lbl">{{ m.label }}</span></router-link>
      </nav>
      <div class="foot">
        <div class="small muted lbl">{{ auth.user?.fullName }} · <b>{{ auth.user?.role }}</b></div>
        <router-link v-if="auth.isAdmin" to="/admin/appearance" class="small lbl">🎨 Giao diện toàn site</router-link>
        <router-link to="/" class="small lbl">← Về cửa hàng</router-link>
        <a href="#" class="small lbl" @click.prevent="logout">Đăng xuất</a>
      </div>
    </aside>
    <div class="main">
      <header class="topbar">
        <button class="burger" aria-label="Mở menu" @click="drawer = true"><i /><i /><i /></button>
        <button class="kbtn" @click="pal?.toggle()"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg><span>Tìm nhanh…</span><kbd>Ctrl K</kbd></button>
        <div class="bellwrap">
          <button class="bell" :aria-label="`Thông báo (${unread} chưa đọc)`" @click="toggleBell"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"/></svg><b v-if="unread" class="cnt">{{ unread }}</b></button>
          <div v-if="bell" class="npanel">
            <div class="nh"><b>Thông báo</b><button v-if="unread" class="lnk" @click="readAll">Đánh dấu đã đọc</button></div>
            <div v-if="!notes.length" class="none">Chưa có thông báo.</div>
            <router-link v-for="n in notes" :key="n.id" :to="n.link || '/admin'" :class="['ni', { unread: !n.read }]" @click="bell = false"><b>{{ n.title }}</b><small>{{ n.body }}</small></router-link>
          </div>
        </div>
      </header>
      <main class="content">
        <div v-if="denied" class="card"><h1>Không có quyền</h1><p class="muted">Tài khoản nhân viên chưa được cấp quyền truy cập mục này. Liên hệ quản trị viên.</p></div>
        <router-view v-else />
      </main>
    </div>
    <CommandPalette ref="pal" :pages="menu" />
  </div>
</template>
<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import CommandPalette from '../../components/CommandPalette.vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../../stores/auth'
import api from '../../api'
const auth = useAuth(), router = useRouter(), route = useRoute()
const ALL = [{to:'/admin',label:'Tổng quan',d:'M3 13h8V3H3zm10 8h8V11h-8zM3 21h8v-6H3zm10-18v6h8V3z',exact:true},{to:'/admin/products',key:'products',label:'Sản phẩm',d:'M5 17h14l-1.5-6.5A2 2 0 0 0 15.6 9H8.4a2 2 0 0 0-1.9 1.5zM7 17v2m10-2v2'},{to:'/admin/catalog',key:'catalog',label:'Danh mục & hãng',d:'M4 4h7v7H4zm9 0h7v7h-7zM4 13h7v7H4zm9 0h7v7h-7z'},{to:'/admin/inventory',key:'inventory',label:'Kho hàng',d:'M3 7l9-4 9 4v10l-9 4-9-4zM3 7l9 4 9-4M12 11v10'},{to:'/admin/orders',key:'orders',label:'Đơn hàng',d:'M6 2h12v20l-3-2-3 2-3-2-3 2zM9 7h6M9 11h6M9 15h4'},{to:'/admin/payments',key:'orders',label:'Giao dịch thanh toán',d:'M2 5h20v14H2zM2 10h20M6 15h4'},{to:'/admin/fulfillment',key:'orders',label:'Xuất kho & giao vận',d:'M1 3h15v13H1zM16 8h4l3 3v5h-7M5.5 21a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zm13 0a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z'},{to:'/admin/purchasing',key:'purchasing',label:'Nhập hàng & gợi ý',d:'M3 3h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6M12 3v6m-3-3h6'},{to:'/admin/warehouses',key:'inventory',label:'Kho theo chi nhánh',d:'M3 21h18M5 21V8l7-5 7 5v13M9 21v-6h6v6'},{to:'/admin/preorders',key:'preorders',label:'Đặt trước',d:'M8 2v4m8-4v4M3 10h18M5 4h14v18H5zM9 15l2 2 4-4'},{to:'/admin/returns',key:'returns',label:'Đổi trả / hoàn tiền',d:'M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5'},{to:'/admin/customers',key:'customers',label:'Khách hàng',d:'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8'},{to:'/admin/segments',key:'segments',label:'Phân khúc khách',d:'M21 12a9 9 0 1 1-9-9v9z M12 3a9 9 0 0 1 9 9h-9z'},{to:'/admin/carts',key:'carts',label:'Giỏ bị bỏ quên',d:'M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6M10 20h.01M17 20h.01M13 11l3-3m0 3-3-3'},{to:'/admin/promotions',key:'promotions',label:'Voucher',d:'M20 12v9H4v-9M2 7h20v5H2zm10 14V7'},{to:'/admin/flash-sales',key:'flash-sales',label:'Flash sale',d:'M13 2 3 14h9l-1 8 10-12h-9z'},{to:'/admin/bundles',key:'bundles',label:'Combo & thẻ quà',d:'M20 12v9H4v-9M2 7h20v5H2zm10 14V7M12 7H7.5a2.5 2.5 0 1 1 0-5C11 2 12 7 12 7zm0 0h4.5a2.5 2.5 0 1 0 0-5C13 2 12 7 12 7z'},{to:'/admin/banners',key:'banners',label:'Banner & lịch',d:'M3 5h18v14H3zM3 15l5-5 4 4 3-3 6 6'},{to:'/admin/collections',key:'collections',label:'Bộ sưu tập',d:'M4 4h16v4H4zM6 8v12h12V8M10 12h4'},{to:'/admin/broadcast',key:'broadcast',label:'Gửi thông báo',d:'M3 11v2a2 2 0 0 0 2 2h1l4 4V5L6 9H5a2 2 0 0 0-2 2zm13-3a5 5 0 0 1 0 8'},{to:'/admin/home-layout',key:'home-layout',label:'Bố cục trang chủ',d:'M3 3h18v6H3zM3 13h8v8H3zM15 13h6v8h-6z'},{to:'/admin/auctions',key:'auctions',label:'Đấu giá hàng hiếm',d:'m14 13-8.5 8.5a2.1 2.1 0 0 1-3-3L11 10m5 6 6-6m-14-2 6-6m-3 3 8 8'},{to:'/admin/reviews',key:'reviews',label:'Đánh giá',d:'m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z'},{to:'/admin/questions',key:'questions',label:'Hỏi đáp',d:'M9 9a3 3 0 1 1 4 2.8c-.6.3-1 .9-1 1.6V15m0 3h.01M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20'},{to:'/admin/tickets',key:'tickets',label:'Phiếu hỗ trợ',d:'M4 4h16v12H8l-4 4zM8 9h8M8 12h5'},{to:'/admin/chatbot',key:'chatbot',label:'Trợ lý ảo (chatbot)',d:'M12 2v3M5 8h14v11H5zM9 13h.01M15 13h.01M9 16h6'},{to:'/admin/chat',key:'chat',label:'Live chat',d:'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'},{to:'/admin/analytics',key:'analytics',label:'Doanh số theo nhóm',d:'M3 3v18h18M7 15v3m4-7v7m4-10v10m4-6v6'},{to:'/admin/emails',key:'emails',label:'Hộp thư đi',d:'M4 4h16v16H4zM4 6l8 7 8-7'},{to:'/admin/reports',label:'Báo cáo CSV',d:'M14 2H6v20h12V6zM14 2v4h4M8 13h8M8 17h8',admin:true},{to:'/admin/audit',label:'Nhật ký thao tác',d:'M12 8v4l3 2M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0',admin:true},{to:'/admin/price-alerts',key:'promotions',label:'Theo dõi giá',d:'M3 17l6-6 4 4 8-8M14 7h7v7'},{to:'/admin/jobs',label:'Tác vụ nền',d:'M12 8v4l3 2M12 2a10 10 0 1 0 10 10',admin:true},{to:'/admin/appearance',label:'Giao diện',d:'M12 3a9 9 0 1 0 0 18c1 0 1.5-.8 1.5-1.6 0-.9-.7-1.4-.7-2.3 0-1 .8-1.6 1.8-1.6H17a4 4 0 0 0 4-4c0-4.6-4-8.5-9-8.5zM7.5 12h.01M10 7.5h.01M15 7.5h.01',admin:true},{to:'/admin/permissions',label:'Phân quyền nhân viên',d:'M12 2 4 5v6c0 5 3.5 9 8 11 4.5-2 8-6 8-11V5zM9 12l2 2 4-4',admin:true}]
const allowed = ref(null)
const menu = computed(() => ALL.filter(m => (!m.admin || auth.isAdmin) && (auth.isAdmin || !m.key || !allowed.value || allowed.value.includes(m.key))))
const denied = computed(() => { if (auth.isAdmin || !allowed.value) return false; const m = ALL.find(x => x.key && route.path.startsWith(x.to)); return !!m && !allowed.value.includes(m.key) })
const collapsed = ref(localStorage.getItem('adminMini') === '1'), drawer = ref(false)
watch(collapsed, v => localStorage.setItem('adminMini', v ? '1' : '0'))
watch(() => route.fullPath, () => { drawer.value = false })
const pal = ref()
// Thông báo cho nhân viên (tồn kho thấp, …)
const bell = ref(false), notes = ref([]), unread = ref(0)
let poll
const loadNotes = async () => { try { const r = await api.get('/account/notifications'); notes.value = r.data.items.slice(0, 15); unread.value = r.data.unread } catch {} }
const toggleBell = () => { bell.value = !bell.value; if (bell.value) loadNotes() }
const readAll = async () => { await api.post('/account/notifications/read'); notes.value.forEach(n => (n.read = true)); unread.value = 0 }
const outside = e => { if (bell.value && !e.target.closest('.bellwrap')) bell.value = false }
onMounted(async () => {
  loadNotes(); poll = setInterval(loadNotes, 30000); document.addEventListener('click', outside)
  if (!auth.isAdmin) try { allowed.value = (await api.get('/admin/permissions')).data.staff } catch {}
})
onUnmounted(() => { clearInterval(poll); document.removeEventListener('click', outside) })
const logout = () => { auth.logout(); router.push('/login') }
</script>
<style scoped>
.admin { display: flex; min-height: 100vh; background: var(--bg); }
.side { width: 248px; flex-shrink: 0; background: var(--surface); border-right: 1px solid var(--border); padding: 18px 12px; overflow-y: auto; overscroll-behavior: contain; display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh; transition: width .2s ease-out; }
.side-top { display: flex; align-items: center; justify-content: space-between; margin: 0 4px 18px 8px; }
.logo { color: var(--text); font-size: 1.05rem; letter-spacing: .14em; white-space: nowrap; overflow: hidden; } .logo b { color: var(--accent); }
.collapse { width: 30px; height: 30px; border: 1px solid var(--border); background: none; color: var(--text); cursor: pointer; flex-shrink: 0; }
nav a { display: flex; align-items: center; gap: 12px; color: var(--text-2); padding: 9px 12px; margin-bottom: 2px; font-size: .92rem; font-weight: 500; position: relative; white-space: nowrap; }
nav svg { width: 20px; height: 20px; flex-shrink: 0; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
nav a:hover { background: rgba(0,0,0,.05); color: var(--text); }
nav a.on { background: var(--bronze-soft); color: var(--text); } nav a.on svg { color: var(--accent); }
nav a.on::before { content: ''; position: absolute; left: -12px; top: 8px; bottom: 8px; width: 3px; background: var(--accent); }
.foot { margin-top: auto; display: flex; flex-direction: column; gap: 8px; padding: 14px 8px 4px; border-top: 1px solid var(--border); }
.foot a { color: var(--muted); } .foot a:hover { color: var(--text); }
.mini .side { width: 68px; } .mini .lbl, .mini .logo span { display: none; } .mini .side-top { flex-direction: column; gap: 10px; margin-left: 4px; } .mini nav a { justify-content: center; padding: 10px 0; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar { position: sticky; top: 0; z-index: 30; display: flex; align-items: center; gap: 10px; height: 56px; padding: 0 clamp(14px, 3vw, 40px); background: var(--surface); border-bottom: 1px solid var(--border); }
.burger { display: none; width: 40px; height: 40px; border: none; background: none; flex-direction: column; justify-content: center; align-items: center; gap: 5px; cursor: pointer; color: var(--text); }
.burger i { width: 20px; height: 2px; background: currentColor; }
.kbtn { display: flex; align-items: center; gap: 8px; height: 38px; padding: 0 12px; width: min(420px, 100%); border: 1px solid var(--border); background: var(--bg); color: var(--muted); font: 500 .88rem inherit; font-family: inherit; cursor: pointer; }
.kbtn span { flex: 1; text-align: left; } .kbtn kbd { font: 600 .68rem ui-monospace, monospace; padding: 2px 6px; border: 1px solid var(--border); background: var(--surface); }
.bellwrap { position: relative; margin-left: auto; }
.bell { position: relative; width: 40px; height: 40px; border: none; background: none; color: var(--text); cursor: pointer; display: grid; place-items: center; }
.cnt { position: absolute; top: 2px; right: 0; min-width: 18px; height: 18px; padding: 0 4px; border-radius: 9px; background: var(--danger); color: #fff; font-size: .66rem; display: grid; place-items: center; }
.npanel { position: absolute; right: 0; top: 46px; width: min(360px, calc(100vw - 24px)); max-height: 440px; overflow-y: auto; background: var(--surface); border: 1px solid var(--border); box-shadow: 0 24px 48px -12px rgba(0,0,0,.3); z-index: 60; }
.nh { display: flex; justify-content: space-between; padding: 12px 14px; border-bottom: 1px solid var(--border); position: sticky; top: 0; background: var(--surface); }
.lnk { background: none; border: none; color: var(--accent); font: 600 .8rem inherit; font-family: inherit; cursor: pointer; }
.ni { display: block; padding: 10px 14px; border-bottom: 1px solid var(--border); color: var(--text); } .ni.unread { background: var(--bronze-soft); } .ni b { display: block; font-size: .86rem; } .ni small { color: var(--muted); }
.none { padding: 16px; color: var(--muted); }
.content { flex: 1; padding: clamp(16px, 3vw, 40px) clamp(14px, 3vw, 44px); min-width: 0; }
.scrim { display: none; }
:deep(.card) { padding: clamp(14px, 2vw, 24px); }
:deep(h1) { font-size: clamp(1.5rem, 1.1rem + 1.6vw, 2rem); font-weight: 800; letter-spacing: -.02em; margin-bottom: clamp(16px, 2.5vw, 32px); }
@media (max-width: 899px) {
  .side { position: fixed; z-index: 200; left: 0; top: 0; bottom: 0; width: min(280px, 84vw) !important; transform: translateX(-100%); transition: transform .22s ease-out; box-shadow: 12px 0 40px rgba(0,0,0,.25); }
  .side.open { transform: none; } .mini .lbl, .mini .logo span { display: initial; } .mini nav a { justify-content: flex-start; padding: 9px 12px; }
  .collapse { display: none; } .burger { display: flex; }
  .scrim { display: block; position: fixed; inset: 0; z-index: 190; background: rgba(0,0,0,.4); }
  .kbtn kbd { display: none; }
  :deep(.row.between) { flex-wrap: wrap; gap: 10px; }
  :deep(.row.between > .input) { max-width: 100% !important; width: 100%; }
}
@media (prefers-reduced-motion: reduce) { .side { transition: none; } }
</style>
