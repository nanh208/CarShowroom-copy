<template>
  <div>
    <section class="hero">
      <div class="container hero-inner">
        <span class="eyebrow">ARES Auto · Showroom 2026</span>
        <h1>Lái chiếc xe<br />bạn hằng mơ ước.</h1>
        <p>14 thương hiệu từ Toyota, VinFast đến Porsche, Lamborghini, Ferrari. Cấu hình màu sơn, mâm, nội thất — đặt cọc chỉ 10%.</p>
        <div class="cta-row">
          <router-link to="/products" class="btn btn-lg">Khám phá mẫu xe</router-link>
          <router-link to="/services" class="btn btn-lg btn-outline">Đăng ký lái thử</router-link>
        </div>
        <dl class="stats">
          <div><dt>20+</dt><dd>Mẫu xe sẵn giao</dd></div>
          <div><dt>10%</dt><dd>Đặt cọc giữ xe</dd></div>
          <div><dt>84</dt><dd>Tháng trả góp</dd></div>
        </dl>
      </div>
      <div class="hero-fade" />
    </section>

    <div class="container">
      <div v-if="loading" class="spinner" />
      <template v-else>
        <section class="brands-strip">
          <router-link v-for="b in carBrands" :key="b.id" :to="{ path: '/products', query: { brandId: b.id } }" class="brand" :title="b.name">
            <img v-if="b.logoUrl" :src="b.logoUrl" :alt="b.name" />
            <span>{{ b.name }}</span>
          </router-link>
        </section>

        <section v-if="flash.length" class="section flash">
          <div class="flash-head">
            <div><span class="eyebrow">Flash sale · Chỉ trong hôm nay</span><h2>Giá tốt nhất tháng</h2></div>
            <div class="clock"><small>Kết thúc sau</small><Countdown :to="endOfDay" /></div>
          </div>
          <div class="grid grid-3"><ProductCard v-for="p in flash" :key="p.id" :p="p" /></div>
        </section>

        <section class="section">
          <div class="section-head"><div><span class="eyebrow">Mới về showroom</span><h2>Xe mới nhất</h2></div><router-link :to="{ path: '/products', query: { sort: 'newest' } }">Xem tất cả →</router-link></div>
          <div class="grid grid-3"><ProductCard v-for="p in data.newArrivals?.slice(0, 6)" :key="p.id" :p="p" /></div>
        </section>

        <section v-if="auctions.length" class="section">
          <div class="section-head"><div><span class="eyebrow">Đấu giá trực tuyến</span><h2>Xe sưu tầm hiếm</h2></div><router-link to="/auctions">Tất cả phiên →</router-link></div>
          <div class="grid grid-3">
            <router-link v-for="a in auctions" :key="a.id" :to="`/auctions/${a.id}`" class="au">
              <div class="au-img"><img :src="a.images?.[0]" :alt="a.title" /><span class="live" :class="a.status">{{ a.status === 'LIVE' ? '● Đang diễn ra' : 'Sắp mở' }}</span></div>
              <div class="au-body">
                <h3>{{ a.title }}</h3>
                <div class="au-meta"><div><small>Giá hiện tại</small><b>{{ ty(a.currentPrice) }}</b></div>
                  <div class="r"><small>{{ a.status === 'LIVE' ? 'Kết thúc sau' : 'Mở sau' }}</small><b><Countdown :to="a.status === 'LIVE' ? a.endTime : a.startTime" /></b></div></div>
              </div>
            </router-link>
          </div>
        </section>

        <section class="feature">
          <div class="feature-text">
            <span class="eyebrow">Cấu hình theo ý bạn</span>
            <h2>Mỗi chiếc xe là<br />một tác phẩm riêng.</h2>
            <p>Chọn màu sơn, mâm và chất liệu nội thất ngay trên trang chi tiết. Giá cập nhật tức thì, trả góp đến 84 tháng.</p>
            <router-link :to="{ path: '/products', query: { type: 'SUPERCAR' } }" class="btn">Cấu hình siêu xe</router-link>
          </div>
        </section>

        <section class="section">
          <div class="section-head"><div><span class="eyebrow">Được yêu thích</span><h2>Bán chạy nhất</h2></div><router-link :to="{ path: '/products', query: { sort: 'bestseller' } }">Xem tất cả →</router-link></div>
          <div class="grid grid-3"><ProductCard v-for="p in data.bestsellers?.filter(p => p.type !== 'ACCESSORY').slice(0, 3)" :key="p.id" :p="p" /></div>
        </section>

        <section v-if="data.promotions?.length" class="section">
          <div class="section-head"><div><span class="eyebrow">Ưu đãi</span><h2>Chương trình tháng này</h2></div><router-link to="/promotions">Tất cả ưu đãi →</router-link></div>
          <div class="grid grid-3">
            <div v-for="p in data.promotions" :key="p.id" class="promo">
              <div class="pct">−{{ p.discountPercent }}%</div>
              <h3>{{ p.title }}</h3>
              <p>{{ p.description }}</p>
              <span class="code">{{ p.code }}</span>
            </div>
          </div>
        </section>

        <section v-if="prefs.recent.length" class="section">
          <div class="section-head"><div><span class="eyebrow">Lịch sử</span><h2>Bạn đã xem gần đây</h2></div></div>
          <div class="recent"><router-link v-for="r in prefs.recent.slice(0, 6)" :key="r.id" :to="`/products/${r.id}`" class="rc"><img :src="r.images?.[0]" alt="" /><b>{{ r.name }}</b><span class="price">{{ vnd(r.price) }}</span></router-link></div>
        </section>

        <section class="section services">
          <div v-for="s in services" :key="s[0]" class="svc">
            <span class="num">{{ s[0] }}</span><h3>{{ s[1] }}</h3><p>{{ s[2] }}</p>
            <router-link :to="s[3]">{{ s[4] }} →</router-link>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import ProductCard from '../components/ProductCard.vue'
import Countdown from '../components/Countdown.vue'
import { ty, vnd } from '../format'
import { usePrefs } from '../stores/prefs'
const data = ref({}), loading = ref(true), auctions = ref([]), flash = ref([]), prefs = usePrefs()
const endOfDay = (() => { const d = new Date(); d.setHours(23, 59, 59, 0); return d.toISOString() })()
const carBrands = computed(() => (data.value.brands || []).filter(b => !b.name.startsWith('ARES')))
const services = [
  ['01', 'Lái thử tận nơi', 'Đặt lịch lái thử tại showroom hoặc tại nhà, có chuyên viên đi cùng.', '/services', 'Đặt lịch lái thử'],
  ['02', 'Bảo dưỡng & sửa chữa', 'Xưởng dịch vụ chuẩn hãng, nhận xe tận nhà và có xe thay thế.', '/services?tab=maintenance', 'Đặt lịch xưởng'],
  ['03', 'Tư vấn 1:1', 'Chuyên viên trực tuyến 8:00–21:00 qua live chat và hotline 1900 6868.', '/services', 'Liên hệ tư vấn']]
onMounted(async () => {
  data.value = (await api.get('/products/home')).data; loading.value = false
  api.get('/products', { params: { size: 60 } }).then(r => { flash.value = r.data.content.filter(p => p.oldPrice && p.stock > 0).slice(0, 3) })
  auctions.value = (await api.get('/auctions')).data.filter(a => a.status === 'LIVE' || a.status === 'SCHEDULED').slice(0, 3)
})
</script>

<style scoped>
.hero { position: relative; min-height: calc(100vh - 72px); max-height: 860px; display: flex; align-items: center; margin-top: -72px; padding-top: 72px;
  background: linear-gradient(90deg, rgba(8,9,12,.94) 0%, rgba(8,9,12,.7) 38%, rgba(8,9,12,.05) 70%), url('/images/hero-huracan.jpg') 70% center/cover; }
.hero-fade { position: absolute; left: 0; right: 0; bottom: 0; height: 160px; background: linear-gradient(transparent, #08090c); }
.hero-inner { position: relative; z-index: 1; }
.hero h1 { font-size: clamp(2.75rem, 5.5vw, 4.75rem); font-weight: 800; line-height: 1.04; letter-spacing: -.035em; margin: 8px 0 24px; }
.hero p { font-size: 1.2rem; color: #cbd5e1; max-width: 520px; margin-bottom: 40px; }
.cta-row { display: flex; gap: 16px; flex-wrap: wrap; }
.stats { display: flex; gap: 48px; margin-top: 64px; }
.stats dt { font-size: 2rem; font-weight: 800; letter-spacing: -.02em; }
.stats dd { color: var(--muted); font-size: .9rem; }
.brands-strip { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px 0; padding: 32px 0; margin-top: 16px; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.brand { width: 140px; border-radius: 12px; height: 104px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--muted); font-size: .8rem; font-weight: 600; transition: .25s; }
.brand img { height: 36px; width: 72px; object-fit: contain; opacity: .7; transition: .25s; }
.brand:hover { background: rgba(255,255,255,.05); color: #fff; } .brand:hover img { opacity: 1; transform: scale(1.08); }
.grid-3 { grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); }
.au { display: block; border-radius: var(--radius); overflow: hidden; background: var(--card); border: 1px solid var(--border); color: var(--text); transition: .3s; }
.au:hover { transform: translateY(-4px); border-color: rgba(56,189,248,.4); color: var(--text); }
.au-img { position: relative; aspect-ratio: 16/10; overflow: hidden; }
.au-img img { width: 100%; height: 100%; object-fit: cover; transition: transform .6s; }
.au:hover .au-img img { transform: scale(1.05); }
.live { position: absolute; top: 16px; left: 16px; padding: 6px 12px; border-radius: 999px; font-size: .75rem; font-weight: 700; background: rgba(0,0,0,.6); backdrop-filter: blur(8px); }
.live.LIVE { color: #fca5a5; }
.au-body { padding: 24px; } .au-body h3 { font-size: 1.15rem; margin-bottom: 16px; }
.au-meta { display: flex; justify-content: space-between; } .au-meta small { display: block; color: var(--muted); font-size: .8rem; margin-bottom: 4px; }
.au-meta b { font-size: 1.15rem; font-variant-numeric: tabular-nums; } .r { text-align: right; } .r b { color: var(--accent); }
.feature { margin: 96px 0; border-radius: 24px; overflow: hidden; min-height: 480px; display: flex; align-items: center;
  background: linear-gradient(90deg, rgba(8,9,12,.92) 30%, rgba(8,9,12,.1) 75%), url('/images/hero-taycan.jpg') right center/cover; border: 1px solid var(--border); }
.feature-text { padding: 64px; max-width: 560px; }
.feature h2 { font-size: 2.5rem; font-weight: 800; letter-spacing: -.03em; line-height: 1.1; margin-bottom: 16px; }
.feature p { color: #cbd5e1; font-size: 1.05rem; margin-bottom: 32px; }
.promo { padding: 32px; border-radius: var(--radius); background: linear-gradient(160deg, rgba(2,132,199,.22), rgba(24,26,31,.8) 60%); border: 1px solid var(--border); display: flex; flex-direction: column; gap: 8px; }
.pct { font-size: 3rem; font-weight: 800; letter-spacing: -.04em; line-height: 1; margin-bottom: 8px; }
.promo h3 { font-size: 1.1rem; } .promo p { color: var(--muted); flex: 1; }
.code { align-self: flex-start; margin-top: 16px; padding: 8px 16px; border: 1px dashed rgba(255,255,255,.35); border-radius: 8px; font-weight: 700; letter-spacing: .1em; }
.services { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0; border-top: 1px solid var(--border); }
.svc { padding: 48px 32px 0 0; } .svc + .svc { padding-left: 32px; border-left: 1px solid var(--border); }
.num { color: var(--accent); font-weight: 700; font-size: .85rem; } .svc h3 { font-size: 1.35rem; margin: 16px 0 8px; } .svc p { color: var(--muted); margin-bottom: 16px; }
@media (max-width: 800px) { .services { grid-template-columns: 1fr; } .svc + .svc { border-left: none; padding-left: 0; } .stats { gap: 24px; } }
.flash { padding: 48px; border-radius: 24px; background: linear-gradient(135deg, rgba(239,68,68,.16), rgba(24,26,31,.7) 50%); border: 1px solid rgba(239,68,68,.25); }
.flash-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; gap: 24px; flex-wrap: wrap; }
.flash h2 { font-size: 2rem; font-weight: 700; letter-spacing: -.02em; } .flash .eyebrow { color: #fca5a5; }
.clock { text-align: right; } .clock small { display: block; color: var(--muted); margin-bottom: 4px; } .clock :deep(.cd) { font-size: 2rem; color: #fca5a5; letter-spacing: .02em; }
.recent { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; }
.rc { display: flex; flex-direction: column; gap: 8px; color: var(--text); } .rc img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; transition: .3s; }
.rc:hover img { opacity: .8; } .rc b { font-size: .9rem; font-weight: 600; } .rc .price { font-size: .9rem; }
</style>
