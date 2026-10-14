<template>
  <div class="home">
    <!-- Hero carousel: menu đè lên phần trên của banner -->
    <section class="hero" @mouseenter="pause = true" @mouseleave="pause = false">
      <div class="slides">
        <router-link v-for="(s, i) in slides" :key="s.title" :to="s.to" :class="['slide', { on: i === cur }]" :style="{ backgroundImage: `url(${s.img})` }">
          <div class="shade" />
          <div class="txt"><small>{{ s.kicker }}</small><h1>{{ s.title }}</h1><span>{{ s.sub }}</span></div>
        </router-link>
      </div>
    </section>
    <div class="dots"><button v-for="(s, i) in slides" :key="i" :class="{ on: i === cur }" :aria-label="`Ảnh ${i + 1}`" @click="cur = i" /></div>

    <div class="container">
      <router-link to="/promotions" class="promo-banner">
        <img src="/images/cars/vf9.jpg" alt="" />
        <div /><div class="pb-txt" style="z-index:1"><b>Tháng vàng ARES</b><i>Ưu đãi mùa thu</i></div>
        <div class="pb-off"><small>Giảm đến</small><b>50 triệu</b><small>Mã THANG10</small></div>
        <div class="pb-moon" />
      </router-link>

      <section class="block">
        <h2>Danh mục</h2>
        <div class="cats">
          <router-link v-for="c in cats" :key="c.label" :to="c.to" class="cat"><span>{{ c.label }}</span><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" v-html="c.icon" /></router-link>
        </div>
      </section>

      <div v-if="loading" class="spinner" />
      <template v-else>
        <section class="block">
          <div class="bh"><h2>Bán chạy</h2><router-link :to="{ path: '/products', query: { sort: 'bestseller' } }">Xem tất cả →</router-link></div>
          <div class="grid grid-3"><Tile v-for="p in best" :key="p.id" :p="p" /></div>
        </section>

        <section v-if="flash.length" class="block">
          <div class="bh"><h2>Flash sale <span class="cd">kết thúc sau <Countdown :to="endOfDay" /></span></h2><router-link :to="{ path: '/products', query: { sort: 'price_asc' } }">Xem tất cả →</router-link></div>
          <div class="grid grid-3"><Tile v-for="p in flash" :key="p.id" :p="p" sale /></div>
        </section>

        <section class="block">
          <div class="bh"><h2>Siêu xe nổi bật</h2><router-link :to="{ path: '/products', query: { type: 'SUPERCAR' } }">Xem tất cả →</router-link></div>
          <div class="grid grid-3"><Tile v-for="p in supercars" :key="p.id" :p="p" /></div>
        </section>

        <section class="block">
          <div class="bh"><h2>SUV & xe điện nổi bật</h2><router-link :to="{ path: '/products', query: { type: 'SUV' } }">Xem tất cả →</router-link></div>
          <div class="grid grid-3"><Tile v-for="p in suvs" :key="p.id" :p="p" /></div>
        </section>

        <section v-if="auctions.length" class="block">
          <div class="bh"><h2>Đấu giá xe hiếm</h2><router-link to="/auctions">Tất cả phiên →</router-link></div>
          <div class="grid grid-3">
            <router-link v-for="a in auctions" :key="a.id" :to="`/auctions/${a.id}`" class="tile">
              <img :src="a.images?.[0]" :alt="a.title" />
              <span :class="['live', a.status]">{{ a.status === 'LIVE' ? '● Đang đấu giá' : 'Sắp mở' }}</span>
              <div class="cap"><span>{{ a.title }}</span><small>{{ ty(a.currentPrice) }} · <Countdown :to="a.status === 'LIVE' ? a.endTime : a.startTime" /></small></div>
            </router-link>
          </div>
        </section>

        <section v-if="prefs.recent.length" class="block">
          <div class="bh"><h2>Bạn đã xem gần đây</h2></div>
          <div class="recent"><router-link v-for="r in prefs.recent.slice(0, 6)" :key="r.id" :to="`/products/${r.id}`" class="rc"><img :src="r.images?.[0]" alt="" /><b>{{ r.name }}</b></router-link></div>
        </section>

        <section v-if="reviews.length" class="block">
          <div class="bh"><h2>Đánh giá của khách hàng</h2></div>
          <div class="grid grid-3">
            <router-link v-for="r in reviews" :key="r.id" :to="`/products/${r.productId}`" class="rev">
              <div class="rimg"><img :src="imgOf(r.productId)" alt="" /></div>
              <div class="rbody">
                <div class="rh"><b>{{ r.author }}</b><span class="rate"><Stars :value="r.rating" size=".95rem" /></span></div>
                <p>“{{ r.comment }}”</p>
                <small>Đã mua {{ r.productName }} · {{ d(r.createdAt) }}</small>
              </div>
            </router-link>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, h } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../api'
import Countdown from '../components/Countdown.vue'
import Stars from '../components/Stars.vue'
import { ty, vnd, d } from '../format'
import { usePrefs } from '../stores/prefs'

// Thẻ ảnh có tên đè lên (wireframe "Model Name")
const Tile = (props) => h(RouterLink, { to: `/products/${props.p.id}`, class: 'tile' }, () => [
  h('img', { src: props.p.images?.[0], alt: props.p.name }),
  props.sale && props.p.oldPrice ? h('span', { class: 'off' }, `−${Math.round(100 - props.p.price * 100 / props.p.oldPrice)}%`) : null,
  h('div', { class: 'cap' }, [h('span', props.p.name), h('small', vnd(props.p.price))])])
Tile.props = ['p', 'sale']

const prefs = usePrefs()
const all = ref([]), loading = ref(true), auctions = ref([]), reviews = ref([]), home = ref({})
const slides = [
  { title: 'HURACÁN', kicker: 'Lamborghini · 2024', sub: 'V10 640 mã lực — cấu hình riêng tại ARES', img: '/images/hero-huracan.jpg', to: '/products/17' },
  { title: 'TAYCAN', kicker: 'Porsche · Xe điện', sub: 'Sport Turismo GTS — lái thử ngay hôm nay', img: '/images/hero-taycan.jpg', to: '/products/16' },
  { title: 'SF90 SPIDER', kicker: 'Ferrari · Hybrid 1.000 hp', sub: 'Đặt cọc chỉ 10%, giao xe toàn quốc', img: '/images/cars/sf90b.jpg', to: '/products/19' },
  { title: 'F40', kicker: 'Đấu giá xe hiếm', sub: 'Phiên đấu giá Ferrari F40 1990 đang diễn ra', img: '/images/cars/f40.jpg', to: '/auctions/1' },
  { title: 'VF 9', kicker: 'VinFast · Xe điện 7 chỗ', sub: 'Trả góp đến 84 tháng', img: '/images/cars/vf9.jpg', to: '/products/10' }]
const cur = ref(0), pause = ref(false); let t
const I = { car: '<path d="M5 17h14l-1.5-6.5A2 2 0 0 0 15.6 9H8.4a2 2 0 0 0-1.9 1.5zM7 17v2m10-2v2"/>', suv: '<path d="M3 16h18v-4l-2-5H6L3 12zM7 16v2m10-2v2M3 12h18"/>',
  bolt: '<path d="M13 2 4 14h7l-1 8 9-12h-7z"/>', flag: '<path d="M4 22V4m0 0h13l-2 4 2 4H4"/>', tool: '<path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.5-.5-.5-2.5z"/>' }
const cats = [
  { label: 'Sedan', icon: I.car, to: { path: '/products', query: { type: 'SEDAN' } } },
  { label: 'SUV / Crossover', icon: I.suv, to: { path: '/products', query: { type: 'SUV' } } },
  { label: 'Siêu xe', icon: I.flag, to: { path: '/products', query: { type: 'SUPERCAR' } } },
  { label: 'Xe điện', icon: I.bolt, to: { path: '/products', query: { type: 'ELECTRIC' } } },
  { label: 'Phụ kiện', icon: I.tool, to: { path: '/products', query: { type: 'ACCESSORY' } } }]
const cars = computed(() => all.value.filter(p => p.type !== 'ACCESSORY'))
const best = computed(() => (home.value.bestsellers || []).filter(p => p.type !== 'ACCESSORY').slice(0, 3))
const flash = computed(() => cars.value.filter(p => p.oldPrice && p.stock > 0 && !best.value.some(b => b.id === p.id)).slice(0, 3))
const supercars = computed(() => cars.value.filter(p => ['SUPERCAR', 'SPORT'].includes(p.type)).slice(0, 3))
const suvs = computed(() => cars.value.filter(p => ['SUV', 'ELECTRIC'].includes(p.type) && !flash.value.includes(p)).slice(0, 3))
const imgOf = id => all.value.find(p => p.id === id)?.images?.[0] || '/images/hero-taycan.jpg'
const endOfDay = (() => { const x = new Date(); x.setHours(23, 59, 59, 0); return x.toISOString() })()
onMounted(async () => {
  t = setInterval(() => { if (!pause.value) cur.value = (cur.value + 1) % slides.length }, 5000)
  const [hm, list] = await Promise.all([api.get('/products/home'), api.get('/products', { params: { size: 60, sort: 'bestseller' } })])
  home.value = hm.data; all.value = list.data.content; loading.value = false
  api.get('/auctions').then(r => { auctions.value = r.data.filter(a => a.status === 'LIVE' || a.status === 'SCHEDULED').slice(0, 3) })
  api.get('/products/reviews/latest', { params: { limit: 3 } }).then(r => { reviews.value = r.data })
})
onUnmounted(() => clearInterval(t))
</script>

<style scoped>
.hero { max-width: 1180px; margin: 0 auto; padding: 0 24px; }
.slides { position: relative; height: 400px; border-radius: 0 0 12px 12px; overflow: hidden; background: #111; }
.slide { position: absolute; inset: 0; background-size: cover; background-position: center; opacity: 0; transition: opacity .8s; color: #fff; }
.slide.on { opacity: 1; z-index: 1; }
.shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.55) 0%, rgba(0,0,0,.05) 35%, rgba(0,0,0,.15) 60%, rgba(0,0,0,.65) 100%); }
.txt { position: absolute; left: 0; right: 0; top: 84px; text-align: center; }
.txt small { letter-spacing: .3em; text-transform: uppercase; font-size: .75rem; opacity: .85; }
.txt h1 { font-family: var(--head); font-size: 5.2rem; letter-spacing: .28em; font-weight: 500; line-height: 1; margin: 6px 0 0 .28em; text-shadow: 0 6px 30px rgba(0,0,0,.5); }
.txt span { position: absolute; left: 0; right: 0; top: 250px; font-size: .95rem; }
.dots { display: flex; justify-content: center; gap: 6px; margin: 12px 0 18px; }
.dots button { width: 8px; height: 8px; border-radius: 50%; border: none; background: #cfcfcf; cursor: pointer; padding: 0; transition: .2s; }
.dots button.on { background: #555; width: 20px; border-radius: 4px; }
.promo-banner { position: relative; display: grid; grid-template-columns: 1fr 1.2fr .8fr; padding-left: 0; align-items: center; height: 96px; border-radius: 10px; overflow: hidden; background: #13235a; color: #fff; }
.promo-banner img { position: absolute; left: 0; top: 0; height: 100%; width: 40%; object-fit: cover; opacity: .55; mask-image: linear-gradient(90deg, #000 40%, transparent); }
.pb-txt { text-align: center; } .pb-txt b { display: block; font-family: var(--head); font-size: 2rem; color: #f6c445; line-height: 1; } .pb-txt i { font-family: Georgia, serif; font-size: 1.05rem; }
.pb-off { z-index: 1; } .pb-off small { display: block; font-size: .75rem; color: #f6c445; } .pb-off b { font-family: var(--head); font-size: 1.6rem; color: #f6c445; line-height: 1.1; }
.pb-moon { position: absolute; right: 36px; top: 10px; width: 120px; height: 120px; border-radius: 50%; background: #f2b632; box-shadow: 0 0 40px rgba(242,182,50,.5); }
.block { margin-top: 22px; }
.block h2, .bh h2 { font-size: 1.15rem; color: #5a5a5f; margin-bottom: 10px; }
.bh { display: flex; justify-content: space-between; align-items: baseline; } .bh a { color: var(--muted); font-size: .85rem; } .bh a:hover { color: var(--bronze); }
.bh .cd { font-family: 'Be Vietnam Pro'; font-size: .8rem; color: var(--danger); margin-left: 8px; font-weight: 600; }
.cats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.cat { display: flex; justify-content: space-between; align-items: center; height: 40px; padding: 0 14px; border-radius: 8px; background: var(--tile-2); color: #3a3a3f; font-size: .88rem; font-weight: 500; transition: .15s; }
.cat:hover { background: var(--bronze); color: #fff; }
.grid-3 { gap: 12px; }
:deep(.tile) { aspect-ratio: 16/7.6; }
:deep(.off) { position: absolute; top: 10px; left: 10px; background: var(--danger); color: #fff; font-size: .75rem; font-weight: 700; padding: 3px 8px; border-radius: 4px; }
.live { position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,.6); color: #fff; font-size: .72rem; font-weight: 700; padding: 4px 8px; border-radius: 4px; }
.live.LIVE { background: var(--danger); }
.recent { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.rc { color: var(--text); font-size: .82rem; } .rc img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 8px; margin-bottom: 6px; } .rc b { font-weight: 500; }
.rev { display: block; border-radius: 10px; overflow: hidden; background: var(--tile); color: var(--text); transition: .2s; }
.rev:hover { box-shadow: var(--shadow); color: var(--text); transform: translateY(-2px); }
.rimg { height: 120px; background: #222; } .rimg img { width: 100%; height: 100%; object-fit: cover; }
.rbody { padding: 12px 16px 14px; }
.rh { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; } .rh b { font-family: var(--head); font-size: 1rem; color: #3a3a3f; }
.rate { background: var(--tile-2); border-radius: 4px; padding: 2px 8px; }
.rate :deep(.s) { color: #bdbdbd; } .rate :deep(.s.on) { color: #f5c400; }
.rbody p { font-size: .85rem; color: #4a4a4f; line-height: 1.5; min-height: 3em; }
.rbody small { display: block; margin-top: 8px; color: #8a8a8f; font-size: .74rem; }
@media (max-width: 860px) { .cats { grid-template-columns: repeat(2, 1fr); } .recent { grid-template-columns: repeat(3, 1fr); } .txt h1 { font-size: 3rem; } }
</style>
