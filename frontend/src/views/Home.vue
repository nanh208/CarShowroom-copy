<template>
  <div class="home">
    <!-- Hero carousel (banner quản lý trong admin) — menu đè lên phần trên, ảnh parallax -->
    <HeroV2 v-if="theme === 'v2' && show('hero')" :products="all" />
    <template v-else-if="show('hero')">
    <section class="hero" @mouseenter="pause = true" @mouseleave="pause = false">
      <div class="slides">
        <router-link v-for="(s, i) in hero" :key="s.id" :to="s.link || '/products'" :class="['slide', { on: i === cur }]">
          <div class="bg" :style="{ backgroundImage: `url(${s.image})` }" />
          <div class="shade" />
          <div class="txt"><small>{{ s.kicker }}</small><h1>{{ s.title }}</h1><span>{{ s.subtitle }}</span></div>
        </router-link>
        <div v-if="!hero.length" class="slide on sk" />
      </div>
    </section>
    <div class="dots"><button v-for="(s, i) in hero" :key="i" :class="{ on: i === cur }" :aria-label="`Ảnh ${i + 1}`" @click="cur = i" /></div>
    </template>

    <div class="container">
      <router-link v-if="promo && theme !== 'v2'" v-reveal :to="promo.link || '/promotions'" class="promo-banner">
        <img loading="lazy" decoding="async" :src="promo.image" alt="" />
        <div />
        <div class="pb-txt"><b>{{ promo.title }}</b><i>{{ promo.kicker }}</i></div>
        <div class="pb-off"><small>Ưu đãi</small><b>{{ promo.subtitle }}</b></div>
        <div class="pb-moon"><svg class="wheel-ico" viewBox="0 0 40 40" width="74" height="74"><circle cx="20" cy="20" r="18" fill="#26262a"/><circle cx="20" cy="20" r="11" fill="#d9d9de"/><g stroke="#8a8a90" stroke-width="2.4"><path d="M20 9v22M9 20h22M12 12l16 16M28 12 12 28"/></g><circle cx="20" cy="20" r="3.5" fill="#555"/></svg></div>
      </router-link>

      <section v-if="theme !== 'v2'" v-reveal class="block">
        <h2>Danh mục</h2>
        <div class="cats">
          <router-link v-for="c in catalog.categories" :key="c.code" :to="{ path: '/products', query: { type: c.code } }" class="cat">
            <span>{{ c.name }}</span>
            <svg class="wheel-ico" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" v-html="icon(c.code)" />
          </router-link>
        </div>
      </section>

      <div v-if="theme !== 'v2'" class="usp" v-reveal>
        <div><b><CountUp :value="catalog.options.freeShipFrom / 1000" />K</b><span>Freeship từ</span></div>
        <div><b><CountUp :value="totalProducts" />+</b><span>Mẫu xe & phụ kiện</span></div>
        <div><b><CountUp :value="7" /> ngày</b><span>Đổi trả miễn phí</span></div>
        <div><b>0%</b><span>Trả góp RC từ 3 triệu</span></div>
      </div>

      <Skeleton v-if="loading" :count="6" :cols="3" />
      <div v-else class="hsections">
        <section v-if="flash.length && show('flash')" v-reveal class="block" :style="ord('flash')">
          <div class="bh"><h2>⚡ Flash sale <span class="cd">kết thúc sau <Countdown :to="flashEnd" /></span><span class="livedot" title="Cập nhật trực tiếp">LIVE</span></h2><router-link to="/promotions">Xem tất cả →</router-link></div>
          <div class="grid grid-4"><ProductCard v-for="(x, i) in flash" :key="x.flash.id" v-reveal :p="x.product" :flash="x.flash" /></div>
        </section>

        <section v-if="bundles.length && show('bundles')" class="block" :style="ord('bundles')">
          <div class="bh"><h2>Combo ưu đãi</h2><router-link to="/bundles">Tất cả combo →</router-link></div>
          <div class="bgrid"><BundleCard v-for="b in bundles.slice(0, 3)" :key="b.bundle.id" :b="b" /></div>
        </section>

        <div v-if="show('featured')" :style="ord('featured')">
        <section v-for="(s, si) in sections" :key="s.title" class="block">
          <div class="bh"><h2>{{ s.title }}</h2><router-link :to="s.to">Xem tất cả →</router-link></div>
          <div class="grid grid-3">
            <router-link v-for="(p, i) in s.items" :key="p.id" v-reveal :to="`/products/${p.id}`" class="tile">
              <img :src="p.images?.[0]" :alt="p.name" loading="lazy" />
              <span v-if="p.oldPrice" class="off">−{{ Math.round(100 - p.price * 100 / p.oldPrice) }}%</span>
              <div class="cap"><span>{{ p.name }}</span><small>{{ vnd(p.price) }}</small></div>
            </router-link>
          </div>
        </section>
        </div>

        <section v-if="auctions.length && show('auctions')" class="block" :style="ord('auctions')">
          <div class="bh"><h2>Đấu giá hàng hiếm</h2><router-link to="/auctions">Tất cả phiên →</router-link></div>
          <div class="grid grid-3">
            <router-link v-for="(a, i) in auctions" :key="a.id" v-reveal :to="`/auctions/${a.id}`" class="tile">
              <img :src="a.images?.[0]" :alt="a.title" loading="lazy" />
              <span :class="['live', a.status]">{{ a.status === 'LIVE' ? '● Đang đấu giá' : 'Sắp mở' }}</span>
              <div class="cap"><span>{{ a.title }}</span><small>{{ vnd(a.currentPrice) }} · <Countdown :to="a.status === 'LIVE' ? a.endTime : a.startTime" /></small></div>
            </router-link>
          </div>
        </section>

        <section v-if="prefs.recent.length && show('recent')" v-reveal class="block" :style="ord('recent')">
          <div class="bh"><h2>Bạn đã xem gần đây</h2></div>
          <div class="recent"><router-link v-for="r in prefs.recent.slice(0, 6)" :key="r.id" :to="`/products/${r.id}`" class="rc"><img loading="lazy" decoding="async" :src="r.images?.[0]" alt="" /><b>{{ r.name }}</b></router-link></div>
        </section>

        <section v-if="reviews.length && show('reviews')" class="block" :style="ord('reviews')">
          <div class="bh"><h2>Đánh giá của khách hàng</h2></div>
          <div class="grid grid-3">
            <router-link v-for="(r, i) in reviews" :key="r.id" v-reveal :to="`/products/${r.productId}`" class="rev">
              <div class="rimg"><img :src="imgOf(r.productId)" alt="" loading="lazy" /></div>
              <div class="rbody">
                <div class="rh"><b>{{ r.author }}</b><span class="rate">★ {{ r.rating }}</span></div>
                <p>“{{ r.comment }}”</p>
                <small>Đã mua {{ r.productName }} · {{ d(r.createdAt) }}</small>
              </div>
            </router-link>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../api'
import Countdown from '../components/Countdown.vue'
import CountUp from '../components/CountUp.vue'
import Skeleton from '../components/Skeleton.vue'
import ProductCard from '../components/ProductCard.vue'
import HeroV2 from '../components/HeroV2.vue'
import BundleCard from '../components/BundleCard.vue'
import { theme } from '../stores/theme'
import { vnd, d, catalog, loadCatalog } from '../format'
import { usePrefs } from '../stores/prefs'

const prefs = usePrefs()
const layout = ref([]), bundles = ref([])
const show = k => { const x = layout.value.find(l => l.key === k); return !x || x.visible !== false }
const ord = k => ({ order: Math.max(0, layout.value.findIndex(l => l.key === k)) })
const all = ref([]), loading = ref(true), auctions = ref([]), reviews = ref([]), banners = ref([]), flash = ref([]), totalProducts = ref(0)
const hero = computed(() => banners.value.filter(b => b.position === 'HERO'))
const promo = computed(() => banners.value.find(b => b.position === 'PROMO'))
const cur = ref(0), pause = ref(false); let t
const I = {
  MODEL: '<path d="M3 15h18l-1.5-4.5A2 2 0 0 0 17.6 9H9l-3 3H4a1 1 0 0 0-1 1z"/><circle cx="7.5" cy="16" r="2"/><circle cx="16.5" cy="16" r="2"/>',
  RC: '<path d="M4 15h16l-2-5H7z"/><circle cx="7" cy="16" r="2.4"/><circle cx="17" cy="16" r="2.4"/><path d="M15 10V4m0 0 3 1"/>',
  DISPLAY: '<rect x="3" y="4" width="18" height="15" rx="2"/><path d="M6 14h12l-1-3H8z"/><path d="M3 19h18"/>',
  KIT: '<rect x="4" y="9" width="16" height="10" rx="1"/><rect x="6" y="6" width="4" height="3"/><rect x="14" y="6" width="4" height="3"/>',
  BATTERY: '<rect x="3" y="7" width="16" height="10" rx="2"/><path d="M21 10v4M11 9l-2 3h4l-2 3"/>',
  REMOTE: '<path d="M8 21v-6l-3-3V6a2 2 0 0 1 2-2h6l5 5v3l-4 3v6z"/><circle cx="12" cy="9" r="1.6"/>',
  WHEEL: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4"/><path d="M12 3v5m0 8v5M3 12h5m8 0h5"/>',
  PART: '<path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.5-.5-.5-2.5z"/>' }
const icon = c => I[c] || I.PART
const pick = (types, n = 3, skip = []) => all.value.filter(p => types.includes(p.type) && !skip.includes(p.id)).slice(0, n)
const sections = computed(() => {
  const best = all.value.filter(p => p.grp === 'TOY').slice(0, 3)
  const used = best.map(p => p.id)
  return [
    { title: 'Bán chạy', to: { path: '/products', query: { sort: 'bestseller' } }, items: best },
    { title: 'Xe RC nổi bật', to: { path: '/products', query: { type: 'RC' } }, items: pick(['RC'], 3, used) },
    { title: 'Xe Model & Trưng Bày', to: { path: '/products', query: { type: 'MODEL,DISPLAY' } }, items: pick(['DISPLAY', 'MODEL'], 3, used) },
    { title: 'Lắp ráp LEGO & Tamiya', to: { path: '/products', query: { type: 'KIT' } }, items: pick(['KIT'], 3, used) },
    { title: 'Phụ kiện RC', to: { path: '/products', query: { grp: 'ACCESSORY' } }, items: all.value.filter(p => p.grp === 'ACCESSORY').slice(0, 3) }
  ].filter(s => s.items.length)
})
const flashEnd = computed(() => flash.value.filter(x => x.live).map(x => x.flash.endTime).sort()[0])
const imgOf = id => all.value.find(p => p.id === id)?.images?.[0] || '/images/toys/hw-r34-0.jpg'
onMounted(async () => {
  loadCatalog()
  t = setInterval(() => { if (!pause.value && hero.value.length) cur.value = (cur.value + 1) % hero.value.length }, 5500)
  api.get('/banners').then(r => { banners.value = r.data })
  api.get('/public/home-layout').then(r => { layout.value = r.data }).catch(() => {})
  api.get('/public/bundles').then(r => { bundles.value = r.data }).catch(() => {})
  // tồn kho flash sale cập nhật trực tiếp
  ft = setInterval(async () => { if (document.hidden || !flash.value.length) return
    const { data } = await api.get('/flash-sales'); const m = Object.fromEntries(data.map(x => [x.flash.id, x]))
    flash.value = flash.value.map(x => m[x.flash.id] ? { ...x, flash: m[x.flash.id].flash, live: m[x.flash.id].live } : x).filter(x => x.live !== false) }, 15000)
  const [list, fl] = await Promise.all([api.get('/products', { params: { size: 60, sort: 'bestseller' } }), api.get('/flash-sales')])
  all.value = list.data.content; totalProducts.value = list.data.totalElements; flash.value = fl.data.filter(x => x.live).slice(0, 4); loading.value = false
  api.get('/auctions').then(r => { auctions.value = r.data.filter(a => a.status === 'LIVE' || a.status === 'SCHEDULED').slice(0, 3) })
  api.get('/products/reviews/latest', { params: { limit: 3 } }).then(r => { reviews.value = r.data })
})
let ft
onUnmounted(() => { clearInterval(t); clearInterval(ft) })
</script>

<style scoped>
.hsections { display: flex; flex-direction: column; }
.bgrid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr)); gap: 16px; }
.livedot { margin-left: 10px; font: 700 .62rem/1 'Oswald', sans-serif; letter-spacing: .14em; color: #fff; background: #c62828; padding: 3px 6px; vertical-align: middle; }

.hero { max-width: 1180px; margin: 0 auto; padding: 0 24px; }
.slides { position: relative; height: 420px; border-radius: 0 0 12px 12px; overflow: hidden; background: #111; }
.slide { position: absolute; inset: 0; opacity: 0; transition: opacity .25s ease-out; color: #fff; overflow: hidden; }
.slide.on { opacity: 1; z-index: 1; }
.bg { position: absolute; inset: 0; background-size: cover; background-position: center; }
.shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.6) 0%, rgba(0,0,0,.1) 38%, rgba(0,0,0,.2) 60%, rgba(0,0,0,.7) 100%); }
.txt { position: absolute; left: 0; right: 0; top: 90px; text-align: center; }
.txt small { letter-spacing: .3em; text-transform: uppercase; font-size: .75rem; opacity: .85; }
.txt h1 { font-family: var(--head); font-size: 4.2rem; letter-spacing: .14em; font-weight: 600; line-height: 1.05; margin: 8px 0 0; text-transform: uppercase; text-shadow: 0 6px 30px rgba(0,0,0,.5); }

.txt span { display: block; margin-top: 170px; font-size: .95rem; }
.dots { display: flex; justify-content: center; gap: 6px; margin: 12px 0 18px; }
.dots button { width: 8px; height: 8px; border-radius: 50%; border: none; background: #cfcfcf; cursor: pointer; padding: 0; transition: background-color .2s ease-out, width .2s ease-out; }
.dots button.on { background: #555; width: 22px; border-radius: 4px; }
.promo-banner { position: relative; display: grid; grid-template-columns: 1fr 1.2fr .8fr; align-items: center; height: 100px; border-radius: 10px; overflow: hidden; background: #13235a; color: #fff; }
.promo-banner img { position: absolute; left: 0; top: 0; height: 100%; width: 42%; object-fit: cover; opacity: .6; mask-image: linear-gradient(90deg, #000 40%, transparent); }
.pb-txt { text-align: center; z-index: 1; } .pb-txt b { display: block; font-family: var(--head); font-size: 2rem; color: #f6c445; line-height: 1; } .pb-txt i { font-family: Georgia, serif; }
.pb-off { z-index: 1; } .pb-off small { display: block; font-size: .75rem; color: #f6c445; } .pb-off b { font-family: var(--head); font-size: 1.15rem; color: #fff; line-height: 1.2; }
.pb-moon { position: absolute; right: 34px; top: 13px; }
.block { margin-top: 24px; }
.block h2, .bh h2 { font-size: 1.15rem; color: #5a5a5f; margin-bottom: 10px; }
.bh { display: flex; justify-content: space-between; align-items: baseline; } .bh a { color: var(--muted); font-size: .85rem; } .bh a:hover { color: var(--bronze); }
.bh .cd { font-family: 'Be Vietnam Pro'; font-size: .8rem; color: var(--danger); margin-left: 8px; font-weight: 600; }
.cats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.cat { display: flex; justify-content: space-between; align-items: center; height: 42px; padding: 0 14px; border-radius: 8px; background: var(--tile-2); color: #3a3a3f; font-size: .88rem; font-weight: 500; transition: .2s; }
.cat:hover { background: var(--bronze); color: #fff; }
.usp { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 18px; }
.usp div { background: var(--bronze-soft); border-radius: 10px; padding: 12px 16px; display: flex; flex-direction: column; }
.usp b { font-family: var(--head); font-size: 1.35rem; color: var(--bronze-3); } .usp span { font-size: .8rem; color: var(--text-2); }
.grid-3 { gap: 12px; }
.tile { aspect-ratio: 16/9.5; }
.off { position: absolute; top: 10px; left: 10px; background: var(--danger); color: #fff; font-size: .75rem; font-weight: 700; padding: 3px 8px; border-radius: 4px; z-index: 1; }
.live { position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,.6); color: #fff; font-size: .72rem; font-weight: 700; padding: 4px 8px; border-radius: 4px; z-index: 1; }
.live.LIVE { background: var(--danger); }
.recent { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.rc { color: var(--text); font-size: .82rem; } .rc img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 8px; margin-bottom: 6px; } .rc b { font-weight: 500; }
.rev { position: relative; display: block; border-radius: 10px; overflow: hidden; background: var(--tile); color: var(--text); }
.rimg { height: 130px; background: #222; } .rimg img { width: 100%; height: 100%; object-fit: cover; }
.rbody { padding: 12px 16px 14px; }
.rh { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; } .rh b { font-family: var(--head); font-size: 1rem; color: #3a3a3f; }
.rate { background: var(--tile-2); border-radius: 4px; padding: 2px 8px; color: #b58a00; font-weight: 700; font-size: .85rem; }
.rbody p { font-size: .85rem; color: #4a4a4f; line-height: 1.5; min-height: 3em; }
.rbody small { display: block; margin-top: 8px; color: #8a8a8f; font-size: .74rem; }
@media (max-width: 860px) { .cats, .usp { grid-template-columns: repeat(2, 1fr); } .recent { grid-template-columns: repeat(3, 1fr); } .txt h1 { font-size: 2.6rem; } }
</style>
