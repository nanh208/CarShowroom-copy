<template>
  <div>
    <section class="hero">
      <div class="container hero-inner">
        <div class="hero-text">
          <span class="badge">🏁 Showroom 2026</span>
          <h1>Chiếc xe mơ ước<br /><span>chỉ cách bạn một cú nhấp</span></h1>
          <p>Showroom ô tô đa thương hiệu: Toyota, Honda, VinFast, Mercedes-Benz, BMW, Porsche, Lamborghini, Ferrari... Cấu hình màu sơn, mâm và nội thất theo phong cách của bạn.</p>
          <div class="row">
            <router-link to="/products" class="btn">Khám phá ngay →</router-link>
            <router-link to="/services" class="btn btn-outline">Đăng ký lái thử</router-link>
          </div>
        </div>
      </div>
    </section>

    <div class="container">
      <div class="perks grid">
        <div class="card" v-for="x in perks" :key="x[0]"><div class="ico">{{ x[0] }}</div><b>{{ x[1] }}</b><div class="muted small">{{ x[2] }}</div></div>
      </div>

      <div v-if="loading" class="spinner" />
      <template v-else>
        <section class="section">
          <div class="section-head"><h2>Thương <span>hiệu</span></h2><router-link to="/products">Xem tất cả →</router-link></div>
          <div class="brands">
            <router-link v-for="b in data.brands" :key="b.id" :to="{ path: '/products', query: { brandId: b.id } }" class="brand">
              <div class="bmark">{{ b.name.slice(0, 1) }}</div>
              <b>{{ b.name }}</b><span class="muted small">{{ b.country }}</span>
            </router-link>
          </div>
        </section>

        <section v-if="data.promotions?.length" class="section">
          <div class="section-head"><h2>Ưu đãi <span>đang diễn ra</span></h2><router-link to="/promotions">Tất cả ưu đãi →</router-link></div>
          <div class="grid promos">
            <div v-for="p in data.promotions" :key="p.id" class="promo" :style="{ background: `linear-gradient(135deg, ${p.bannerColor}, #0b0c10)` }">
              <div class="pct">-{{ p.discountPercent }}%</div>
              <b>{{ p.title }}</b>
              <div class="small">{{ p.description }}</div>
              <div class="code">Mã: <b>{{ p.code }}</b></div>
            </div>
          </div>
        </section>

        <section v-if="auctions.length" class="section">
          <div class="section-head"><h2>🔨 Đấu giá <span>xe hiếm</span></h2><router-link to="/auctions">Tất cả phiên →</router-link></div>
          <div class="grid grid-4">
            <router-link v-for="a in auctions" :key="a.id" :to="`/auctions/${a.id}`" class="au card">
              <img :src="a.images?.[0]" :alt="a.title" />
              <b>{{ a.title }}</b>
              <div class="row between small"><span class="price">{{ ty(a.currentPrice) }}</span>
                <span class="muted">{{ a.status === 'LIVE' ? 'Còn ' : 'Mở sau ' }}<Countdown :to="a.status === 'LIVE' ? a.endTime : a.startTime" /></span></div>
            </router-link>
          </div>
        </section>

        <section class="section">
          <div class="section-head"><h2>Xe <span>mới về</span></h2><router-link :to="{ path: '/products', query: { sort: 'newest' } }">Xem thêm →</router-link></div>
          <div class="grid grid-4"><ProductCard v-for="p in data.newArrivals" :key="p.id" :p="p" /></div>
        </section>

        <section class="section">
          <div class="section-head"><h2>Bán <span>chạy nhất</span></h2><router-link :to="{ path: '/products', query: { sort: 'bestseller' } }">Xem thêm →</router-link></div>
          <div class="grid grid-4"><ProductCard v-for="p in data.bestsellers" :key="p.id" :p="p" /></div>
        </section>

        <section class="section cta card row between">
          <div><h2>🔧 Đến hạn bảo dưỡng xe?</h2><p class="muted">Đặt lịch bảo dưỡng định kỳ, sửa chữa, đồng sơn tại xưởng dịch vụ ARES — nhận xe tận nhà, có xe thay thế.</p></div>
          <router-link to="/services?tab=maintenance" class="btn">Đặt lịch sửa chữa</router-link>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import ProductCard from '../components/ProductCard.vue'
import Countdown from '../components/Countdown.vue'
import { ty } from '../format'
const data = ref({}), loading = ref(true), auctions = ref([])
const perks = [['🚚', 'Giao xe tận nhà', 'Miễn phí toàn quốc'], ['🛡️', 'Chính hãng 100%', 'Bảo hành 3–10 năm'],
  ['💳', 'Trả góp đến 80%', 'Đặt cọc chỉ 10%'], ['🎨', 'Thiết kế theo ý bạn', 'Sơn, mâm, nội thất']]
onMounted(async () => {
  data.value = (await api.get('/products/home')).data; loading.value = false
  auctions.value = (await api.get('/auctions')).data.filter(a => a.status === 'LIVE' || a.status === 'SCHEDULED').slice(0, 4)
})
</script>

<style scoped>
.hero { background: linear-gradient(90deg, rgba(10,12,16,.92) 30%, rgba(10,12,16,.35)), url('/images/bg-pagani.jpg') center/cover; min-height: 460px; display: flex; align-items: center; }
.hero-text { max-width: 560px; padding: 60px 0; }
.hero h1 { font-size: 3rem; margin: 16px 0; } .hero h1 span { color: var(--accent); }
.hero p { color: var(--label); margin-bottom: 26px; }
.perks { grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); margin-top: -40px; position: relative; }
.perks .card { padding: 18px; } .ico { font-size: 1.6rem; }
.brands { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 14px; }
.brand { background: var(--card); border: 1px solid var(--border); border-radius: var(--radius); padding: 16px; display: flex; flex-direction: column; align-items: center; gap: 4px; color: var(--text); transition: .2s; }
.brand:hover { border-color: var(--accent); transform: translateY(-3px); color: var(--text); }
.bmark { width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), #1e293b); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 1.3rem; }
.promos { grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); }
.promo { border-radius: var(--radius); padding: 22px; border: 1px solid var(--border); display: flex; flex-direction: column; gap: 6px; }
.pct { font-size: 2.2rem; font-weight: 800; }
.code { margin-top: 8px; background: rgba(0,0,0,.3); padding: 6px 10px; border-radius: 8px; align-self: flex-start; border: 1px dashed rgba(255,255,255,.4); }
.cta h2 { font-size: 1.3rem; margin-bottom: 6px; }
.au { padding: 12px; display: flex; flex-direction: column; gap: 8px; color: var(--text); }
.au img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 10px; }
.au:hover { border-color: var(--accent); color: var(--text); }
</style>
