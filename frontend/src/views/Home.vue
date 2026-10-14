<template>
  <div>
    <section class="hero">
      <div class="word">H U R A C A N</div>
      <img src="/images/cars/huracan.jpg" alt="Huracán" class="hero-car" />
    </section>

    <div class="promo-strip">
      <span class="gold">★ ƯU ĐÃI MÙA THU ★</span>
      &nbsp; Giảm đến 15% siêu xe · Mã <b>SUPERCAR15</b> · Miễn phí giao hàng đơn từ 2.000.000đ
    </div>

    <div class="container">
      <div class="chips-row">
        <button v-for="c in chips" :key="c.q || c.type || 'all'" :class="['chip', { on: activeChip === c.key }]" @click="goChip(c)">{{ c.label }}</button>
      </div>

      <div v-if="loading" class="spinner" />
      <template v-else>
        <section class="section">
          <div class="section-head"><h2>Bán <span>chạy</span></h2><router-link to="/products?sort=bestseller">Xem tất cả →</router-link></div>
          <div class="grid grid-3 photo-grid">
            <router-link v-for="p in data.bestsellers?.filter(p => p.type !== 'ACCESSORY').slice(0, 3)" :key="p.id" :to="`/products/${p.id}`" class="photo-card">
              <ProductImage :product="p" />
              <div class="cap"><b>{{ p.name }}</b><span>{{ vnd(p.price) }}</span></div>
            </router-link>
          </div>
        </section>

        <section class="section">
          <div class="section-head"><h2>Hàng <span>mới</span></h2><router-link to="/products?sort=newest">Xem tất cả →</router-link></div>
          <div class="grid grid-3 photo-grid">
            <router-link v-for="p in data.newArrivals?.filter(p => p.type !== 'ACCESSORY').slice(0, 3)" :key="p.id" :to="`/products/${p.id}`" class="photo-card">
              <ProductImage :product="p" />
              <div class="cap"><b>{{ p.name }}</b><span>{{ vnd(p.price) }}</span></div>
            </router-link>
          </div>
        </section>

        <section class="section">
          <div class="section-head"><h2>Thương <span>hiệu</span></h2></div>
          <div class="brands">
            <router-link v-for="b in data.brands" :key="b.id" :to="{ path: '/products', query: { brandId: b.id } }" class="brand">
              <div class="bmark">{{ b.name.slice(0, 1) }}</div>
              <b>{{ b.name }}</b>
            </router-link>
          </div>
        </section>

        <section v-if="data.promotions?.length" class="section">
          <div class="section-head"><h2>Ưu đãi <span>đang diễn ra</span></h2><router-link to="/promotions">Tất cả →</router-link></div>
          <div class="grid grid-3">
            <div v-for="p in data.promotions" :key="p.id" class="promo-card">
              <div class="pct">-{{ p.discountPercent }}%</div>
              <b>{{ p.title }}</b>
              <div class="small muted">{{ p.description }}</div>
              <div class="code">{{ p.code }}</div>
            </div>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { vnd } from '../format'
import ProductImage from '../components/ProductImage.vue'
const data = ref({}), loading = ref(true), activeChip = ref('all'), router = useRouter()
const chips = [
  { key: 'all', label: 'Tất cả', q: {} },
  { key: 'SUPERCAR', label: 'Siêu xe', q: { type: 'SUPERCAR' } },
  { key: 'CLASSIC', label: 'Xe cổ điển', q: { type: 'CLASSIC' } },
  { key: 'SUV', label: 'SUV', q: { type: 'SUV' } },
  { key: '18', label: 'Tỉ lệ 1:18', q: { scale: '1:18' } },
  { key: 'ACCESSORY', label: 'Phụ kiện', q: { type: 'ACCESSORY' } }
]
const goChip = c => { activeChip.value = c.key; router.push({ path: '/products', query: c.q }) }
onMounted(async () => { data.value = (await api.get('/products/home')).data; loading.value = false })
</script>

<style scoped>
.hero { background: #000; min-height: 340px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 36px 16px 20px; overflow: hidden; position: relative; }
.word { color: #fff; font: 800 clamp(1.8rem, 6vw, 3.6rem) 'Montserrat', sans-serif; letter-spacing: .55em; margin-left: .55em; text-align: center; z-index: 1; margin-bottom: 8px; }
.hero-car { width: min(980px, 94vw); max-height: 300px; object-fit: cover; -webkit-mask-image: radial-gradient(ellipse 60% 70% at 50% 55%, #000 55%, transparent 100%); mask-image: radial-gradient(ellipse 60% 70% at 50% 55%, #000 55%, transparent 100%); }
.chips-row { display: flex; flex-wrap: wrap; gap: 8px; padding: 22px 0 8px; justify-content: center; }
.photo-grid { gap: 16px; }
.photo-card { display: block; background: #fff; border: 1px solid var(--line); overflow: hidden; color: var(--text); transition: .2s; }
.photo-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,.1); color: var(--text); }
.photo-card :deep(.pimg), .photo-card :deep(.car-preview) { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 0; }
.cap { display: flex; justify-content: space-between; gap: 8px; padding: 10px 12px; font-size: .9rem; border-top: 1px solid var(--line); }
.cap span { color: var(--bronze-dark); font-weight: 700; white-space: nowrap; }
.brands { display: grid; grid-template-columns: repeat(auto-fill, minmax(110px, 1fr)); gap: 10px; }
.brand { background: #fff; border: 1px solid var(--line); padding: 14px 8px; display: flex; flex-direction: column; align-items: center; gap: 6px; color: var(--text); font-size: .85rem; }
.brand:hover { border-color: var(--bronze); color: var(--text); }
.bmark { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--bronze), #5a3d18); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 800; }
.promo-card { border: 1px solid var(--line); padding: 18px; background: #fff; display: flex; flex-direction: column; gap: 6px; }
.pct { font-size: 1.8rem; font-weight: 800; color: var(--bronze); }
.code { align-self: flex-start; margin-top: 6px; border: 1px dashed var(--bronze); padding: 4px 10px; font-weight: 700; color: var(--bronze-dark); letter-spacing: .06em; }
</style>
