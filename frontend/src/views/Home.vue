<template>
  <div>
    <section class="hero">
      <div class="container hero-inner">
        <div class="hero-text">
          <span class="badge">🏁 Bộ sưu tập 2026</span>
          <h1>Sở hữu siêu xe<br /><span>trong lòng bàn tay</span></h1>
          <p>Hơn 500 mẫu mô hình ô tô chính hãng tỉ lệ 1:18 – 1:64 từ Bburago, AUTOart, Minichamps, Norev... Tuỳ chỉnh màu sơn, mâm và nội thất theo phong cách của bạn.</p>
          <div class="row">
            <router-link to="/products" class="btn">Khám phá ngay →</router-link>
            <router-link to="/services" class="btn btn-outline">Đặt lịch xem mẫu</router-link>
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

        <section class="section">
          <div class="section-head"><h2>Hàng <span>mới về</span></h2><router-link :to="{ path: '/products', query: { sort: 'newest' } }">Xem thêm →</router-link></div>
          <div class="grid grid-4"><ProductCard v-for="p in data.newArrivals" :key="p.id" :p="p" /></div>
        </section>

        <section class="section">
          <div class="section-head"><h2>Bán <span>chạy nhất</span></h2><router-link :to="{ path: '/products', query: { sort: 'bestseller' } }">Xem thêm →</router-link></div>
          <div class="grid grid-4"><ProductCard v-for="p in data.bestsellers" :key="p.id" :p="p" /></div>
        </section>

        <section class="section cta card row between">
          <div><h2>🔧 Mô hình bị bong sơn, gãy gương?</h2><p class="muted">Đặt lịch mang mô hình đến xưởng ARES để vệ sinh, phục chế, sơn lại hoặc thay linh kiện.</p></div>
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
const data = ref({}), loading = ref(true)
const perks = [['🚚', 'Giao hàng toàn quốc', 'Miễn phí đơn từ 2 triệu'], ['🛡️', 'Chính hãng 100%', 'Có giấy chứng nhận'],
  ['💳', 'Trả góp 0%', 'Đặt cọc chỉ 30%'], ['🎨', 'Thiết kế theo ý bạn', 'Sơn, mâm, nội thất']]
onMounted(async () => { data.value = (await api.get('/products/home')).data; loading.value = false })
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
</style>
