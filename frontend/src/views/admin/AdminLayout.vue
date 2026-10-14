<template>
  <div class="admin">
    <aside class="side">
      <router-link to="/" class="logo">ARES <b>TOYS</b></router-link>
      <nav>
        <router-link v-for="m in menu" :key="m.to" :to="m.to" :exact-active-class="m.exact ? 'on' : ''" :active-class="m.exact ? '' : 'on'"><svg viewBox="0 0 24 24"><path :d="m.d"/></svg>{{ m.label }}</router-link>
      </nav>
      <div class="foot">
        <div class="small muted">{{ auth.user?.fullName }} · <b>{{ auth.user?.role }}</b></div>
        <router-link to="/" class="small">← Về cửa hàng</router-link>
        <a href="#" class="small" @click.prevent="logout">Đăng xuất</a>
      </div>
    </aside>
    <main class="content"><router-view /></main>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../../stores/auth'
const auth = useAuth(), router = useRouter()
const ALL = [{to:'/admin',label:'Tổng quan',d:'M3 13h8V3H3zm10 8h8V11h-8zM3 21h8v-6H3zm10-18v6h8V3z',admin:false,exact:true},{to:'/admin/products',label:'Sản phẩm',d:'M5 17h14l-1.5-6.5A2 2 0 0 0 15.6 9H8.4a2 2 0 0 0-1.9 1.5zM7 17v2m10-2v2',admin:false,exact:false},{to:'/admin/catalog',label:'Danh mục & hãng',d:'M4 4h7v7H4zm9 0h7v7h-7zM4 13h7v7H4zm9 0h7v7h-7z',admin:false,exact:false},{to:'/admin/inventory',label:'Kho hàng',d:'M3 7l9-4 9 4v10l-9 4-9-4zM3 7l9 4 9-4M12 11v10',admin:false,exact:false},{to:'/admin/orders',label:'Đơn hàng',d:'M6 2h12v20l-3-2-3 2-3-2-3 2zM9 7h6M9 11h6M9 15h4',admin:false,exact:false},{to:'/admin/returns',label:'Đổi trả / hoàn tiền',d:'M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5',admin:false,exact:false},{to:'/admin/customers',label:'Khách hàng',d:'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8',admin:false,exact:false},{to:'/admin/promotions',label:'Voucher',d:'M20 12v9H4v-9M2 7h20v5H2zm10 14V7',admin:false,exact:false},{to:'/admin/flash-sales',label:'Flash sale',d:'M13 2 3 14h9l-1 8 10-12h-9z',admin:false,exact:false},{to:'/admin/banners',label:'Banner',d:'M3 5h18v14H3zM3 15l5-5 4 4 3-3 6 6',admin:false,exact:false},{to:'/admin/auctions',label:'Đấu giá hàng hiếm',d:'m14 13-8.5 8.5a2.1 2.1 0 0 1-3-3L11 10m5 6 6-6m-14-2 6-6m-3 3 8 8',admin:false,exact:false},{to:'/admin/reviews',label:'Đánh giá',d:'m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z',admin:false,exact:false},{to:'/admin/questions',label:'Hỏi đáp',d:'M9 9a3 3 0 1 1 4 2.8c-.6.3-1 .9-1 1.6V15m0 3h.01M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20',admin:false,exact:false},{to:'/admin/tickets',label:'Phiếu hỗ trợ',d:'M4 4h16v12H8l-4 4zM8 9h8M8 12h5',admin:false,exact:false},{to:'/admin/chat',label:'Live chat',d:'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z',admin:false,exact:false},{to:'/admin/reports',label:'Báo cáo CSV',d:'M14 2H6v20h12V6zM14 2v4h4M8 13h8M8 17h8',admin:true,exact:false}]
const menu = computed(() => ALL.filter(m => !m.admin || auth.isAdmin))
const logout = () => { auth.logout(); router.push('/login') }
</script>
<style scoped>
.admin { display: flex; min-height: 100vh; background: #fff; }
.side { width: 256px; flex-shrink: 0; background: #fff;  border-right: 1px solid var(--border); padding: 24px 16px; overflow-y: auto; display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh; }
.logo { display: flex; align-items: center; gap: 10px; color: var(--text); font-size: 1.05rem; letter-spacing: .14em; margin: 0 8px 24px; } .logo img { height: 32px; } .logo b { color: var(--accent); }
nav a { display: flex; align-items: center; gap: 12px; color: var(--text-2); padding: 9px 14px; border-radius: 10px; margin-bottom: 2px; font-size: .93rem; font-weight: 500; position: relative; transition: .15s; }
nav svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
nav a:hover { background: rgba(0,0,0,.05); color: var(--text); }
nav a.on { background: rgba(168,122,60,.16); color: var(--text); }
nav a.on::before { content: ''; position: absolute; left: -16px; top: 10px; bottom: 10px; width: 3px; border-radius: 0 3px 3px 0; background: var(--accent); }
nav a.on svg { color: var(--accent); }
.foot { margin-top: auto; display: flex; flex-direction: column; gap: 8px; padding: 16px; border-top: 1px solid var(--border); }
.foot a { color: var(--muted); } .foot a:hover { color: var(--text); }
.content { flex: 1; padding: 40px 48px; min-width: 0; }
:deep(.card) { padding: 24px; }
:deep(h1) { font-size: 2rem; font-weight: 800; letter-spacing: -.02em; margin-bottom: 32px; }
</style>
