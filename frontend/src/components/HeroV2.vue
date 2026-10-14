<template>
  <div class="v2home">
    <section class="v2hero">
      <div class="v2wrap">
        <div class="copy">
          <span class="kick">Ares Toys</span>
          <h1><span>Nhỏ tỉ lệ.</span><span class="or">Lớn đam mê.</span></h1>
          <p>Thế giới xe mô hình và RC chính hãng dành cho những người thật sự đam mê.</p>
          <div class="cta">
            <router-link to="/products" class="btn v2-black">Khám phá bộ sưu tập <b>›</b></router-link>
            <router-link :to="{ path: '/products', query: { sort: 'newest' } }" class="btn btn-outline">Xem xe mới về <b>›</b></router-link>
          </div>
        </div>
        <div class="stage" aria-hidden="true">
          <div class="slash" /><div class="slash s2" />
          <div class="big">1:64</div>
          <span class="tag-top">Diecast cars<br />RC models<br />for collectors</span>
          <span class="series">Collector series<br /><b>{{ new Date().getFullYear() }}</b></span>
          <router-link :to="hero.link" class="car" tabindex="-1"><img :src="hero.img" alt="" fetchpriority="high" /></router-link>
          <span class="note">{{ hero.name }}<br />{{ hero.scale }} scale · {{ hero.brand }}</span>
          <span class="script">Small scale<br />Big passion</span>
          <i class="checker" />
        </div>
      </div>
    </section>

    <div class="ticker">
      <div class="v2wrap tk">
        <span class="flow">Diecast · RC · Limited edition · Ares Toys</span><i class="checker sm" />
        <span v-for="b in badges" :key="b[1]" class="tb"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" v-html="b[0]" />{{ b[1] }}</span>
      </div>
    </div>

    <section class="v2wrap pick">
      <div class="pick-copy">
        <span class="kick">Bộ sưu tập nổi bật</span>
        <h2><span>Chọn gu.</span><span class="or">Chọn xe.</span></h2>
        <p>Từ những huyền thoại thu nhỏ đến mẫu RC mạnh mẽ và phụ kiện cao cấp — tất cả dành cho nhà sưu tầm như bạn.</p>
        <i class="checker" />
      </div>
      <router-link v-for="(c, i) in cards" :key="c.title" :to="c.to" class="ccard">
        <img :src="c.img" alt="" loading="lazy" decoding="async" />
        <div class="cc-top"><b class="num">0{{ i + 1 }}</b><div><h3>{{ c.title }}</h3><small>{{ c.sub }}</small></div><span class="arr">→</span></div>
        <div v-if="c.price" class="cc-bot"><small>{{ c.name }}</small><b>{{ vnd(c.price) }}</b></div>
      </router-link>
      <router-link to="/promotions" class="deal">
        <b>Deal</b><strong>-{{ deal }}%</strong><small>Phụ kiện<br />chính hãng</small><span class="arr">→</span>
      </router-link>
    </section>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { vnd } from '../format'
const props = defineProps({ products: { type: Array, default: () => [] } })
const find = re => props.products.find(p => re.test(p.images?.[0] || ''))
const hero = computed(() => {
  const p = find(/minigt-supra/)
  return { img: '/images/toys/minigt-supra-0.jpg', link: p ? `/products/${p.id}` : '/products?type=MODEL', name: p?.name || 'Mini GT Toyota Supra', scale: p?.scale || '1:64', brand: p?.brand?.name || 'Mini GT' }
})
const card = (re, title, sub, to, img) => { const p = find(re); return { title, sub, to, img, name: p?.name, price: p?.price } }
const cards = computed(() => [
  card(/bburago-f40/, 'Xe mô hình', 'Diecast · 1:64 · 1:18 · Limited edition', { path: '/products', query: { type: 'MODEL,DISPLAY' } }, '/images/toys/bburago-f40-0.jpg'),
  card(/traxxas-trx4/, 'Xe RC', 'Off-road · On-road · Drift · RTR', { path: '/products', query: { type: 'RC' } }, '/images/toys/traxxas-trx4-0.jpg'),
  card(/wheels-sc/, 'Phụ kiện', 'Pin & sạc · Linh kiện · Bánh xe', { path: '/products', query: { grp: 'ACCESSORY' } }, '/images/toys/wheels-sc-0.jpg')])
const deal = computed(() => {
  const acc = props.products.filter(p => p.grp === 'ACCESSORY' && p.oldPrice)
  return acc.length ? Math.max(...acc.map(p => Math.round(100 - p.price * 100 / p.oldPrice))) : 25
})
const badges = [
  ['<path d="M3 7h11v9H3zM14 10h4l3 3v3h-7"/><circle cx="7" cy="17" r="2"/><circle cx="17" cy="17" r="2"/>', 'Freeship từ 500K'],
  ['<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6z"/><path d="m9 12 2 2 4-4"/>', 'Hàng chính hãng'],
  ['<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/>', 'Đổi trả 7 ngày'],
  ['<rect x="3" y="6" width="18" height="12" rx="2"/><path d="M3 10h18"/>', 'Trả góp 0%']]
</script>
<style scoped>
.v2wrap { max-width: 1240px; margin: 0 auto; padding: 0 24px; }
.kick { display: block; font: 600 .7rem/1 'Oswald', sans-serif; letter-spacing: .35em; text-transform: uppercase; color: #333; margin-bottom: 14px; }
.v2hero { padding-top: 76px; overflow: hidden; background: #f3efe6; }
.v2hero .v2wrap { display: grid; grid-template-columns: 1fr 1.25fr; align-items: center; min-height: 470px; }
.copy { position: relative; z-index: 2; padding: 30px 0; }
h1, h2 { font-family: 'Anton', 'Oswald', sans-serif; font-style: italic; font-weight: 400; text-transform: uppercase; line-height: .95; letter-spacing: .005em; transform: skewX(-6deg); transform-origin: left; }
h1 { font-size: clamp(3rem, 6.4vw, 5.6rem); color: #111; } h2 { font-size: clamp(2.4rem, 4.2vw, 3.8rem); color: #111; }
h1 span, h2 span { display: block; } .or { color: var(--v2-orange); }
.copy p, .pick-copy p { max-width: 380px; margin: 16px 0 22px; color: #333; font-size: .95rem; }
.cta { display: flex; gap: 12px; flex-wrap: wrap; } .cta .btn b { margin-left: 8px; }
.stage { position: relative; height: 470px; }
.slash { position: absolute; right: -40%; top: -10%; width: 70%; height: 130%; background: var(--v2-orange); transform: skewX(-18deg); }
.slash.s2 { right: -12%; width: 6%; background: #111; opacity: .9; }
.big { position: absolute; left: 6%; top: -2%; font: italic 400 clamp(8rem, 17vw, 15rem)/1 'Anton', sans-serif; color: transparent; -webkit-text-stroke: 2px #1d1d1d; opacity: .9; letter-spacing: -.01em; }
.car { position: absolute; left: 12%; right: 8%; bottom: 58px; height: 58%; z-index: 2; filter: drop-shadow(14px 18px 0 rgba(17,17,17,.9)); }
.car img { width: 100%; height: 100%; object-fit: cover; object-position: 50% 60%; clip-path: polygon(7% 0, 100% 0, 93% 100%, 0 100%); }
.tag-top { position: absolute; left: 0; top: 18px; font: 500 .62rem/1.5 'Oswald', sans-serif; letter-spacing: .2em; text-transform: uppercase; color: #333; }
.series { position: absolute; right: 2%; top: 22px; z-index: 2; font: 500 .62rem/1.4 'Oswald', sans-serif; letter-spacing: .2em; text-transform: uppercase; color: #fff; }
.series b { font: italic 400 1.6rem 'Anton', sans-serif; letter-spacing: .02em; }
.note { position: absolute; left: 0; bottom: 6px; z-index: 4; background: var(--v2-cream); padding-right: 8px; font: 600 .62rem/1.35 'Oswald', sans-serif; letter-spacing: .12em; text-transform: uppercase; color: #111; padding-left: 10px; border-left: 2px solid #111; }
.script { position: absolute; right: 2%; bottom: 4px; z-index: 4; font: italic 600 1.15rem/1.05 Georgia, serif; color: #111; text-align: right; }
.checker { display: block; width: 46px; height: 18px; background: conic-gradient(#111 25%, transparent 0 50%, #111 0 75%, transparent 0) 0 0/9px 9px; transform: skewX(-20deg); }
.stage > .checker { position: absolute; right: 22%; bottom: 10px; z-index: 4; }
.ticker { background: #111; color: #eee; }
.tk { display: flex; align-items: center; gap: 28px; height: 46px; overflow-x: auto; white-space: nowrap; scrollbar-width: none; }
.flow { font: 500 .66rem 'Oswald', sans-serif; letter-spacing: .4em; text-transform: uppercase; color: #aaa; }
.checker.sm { width: 34px; height: 12px; background: conic-gradient(#fff 25%, transparent 0 50%, #fff 0 75%, transparent 0) 0 0/6px 6px; }
.tb { display: inline-flex; align-items: center; gap: 8px; font-size: .78rem; margin-left: auto; } .tb ~ .tb { margin-left: 0; }
.pick { display: grid; grid-template-columns: 1.15fr 1fr 1fr 1fr .5fr; gap: 14px; padding-top: 34px; padding-bottom: 10px; align-items: stretch; }
.pick-copy .checker { margin-top: 8px; }
.ccard { position: relative; min-height: 340px; overflow: hidden; color: #fff; clip-path: polygon(9% 0, 100% 0, 91% 100%, 0 100%); background: #1a1a1a; display: flex; flex-direction: column; justify-content: space-between; }
.ccard img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; filter: brightness(.62) contrast(1.1); }
.ccard::after { content: ''; position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.55), transparent 40%, transparent 60%, rgba(0,0,0,.75)); }
.cc-top, .cc-bot { position: relative; z-index: 1; padding: 18px 22px 18px 34px; }
.cc-top { display: flex; gap: 12px; align-items: flex-start; }
.num { font: italic 400 1.5rem/1 'Anton', sans-serif; color: var(--v2-orange); }
.cc-top h3 { font: italic 600 1.35rem/1.1 'Oswald', sans-serif; text-transform: none; color: #fff; }
.cc-top small { display: block; font: 500 .58rem/1.4 'Oswald', sans-serif; letter-spacing: .14em; text-transform: uppercase; color: #ccc; margin-top: 4px; }
.arr { margin-left: auto; font-size: 1.2rem; }
.cc-bot { padding-bottom: 22px; padding-left: 26px; } .cc-bot small { display: block; font: 600 .66rem 'Oswald', sans-serif; letter-spacing: .1em; text-transform: uppercase; color: #ddd; }
.cc-bot b { font: italic 400 1.6rem 'Anton', sans-serif; color: var(--v2-orange); }
.ccard:hover { color: #fff; } .ccard:hover .arr { color: var(--v2-orange); }
.deal { position: relative; background: var(--v2-orange); color: #fff; display: flex; flex-direction: column; padding: 20px 16px; gap: 4px; }
.deal b { font: italic 400 1.9rem/1 'Anton', sans-serif; text-transform: uppercase; }
.deal strong { font: italic 400 2.4rem/1 'Anton', sans-serif; }
.deal small { font: 600 .66rem/1.4 'Oswald', sans-serif; letter-spacing: .14em; text-transform: uppercase; }
.deal .arr { margin-top: auto; margin-left: 0; } .deal:hover { color: #fff; background: #d94a0e; }
@media (max-width: 1000px) {
  .v2hero .v2wrap { grid-template-columns: 1fr; } .stage { height: 340px; }
  .pick { grid-template-columns: 1fr 1fr; } .pick-copy, .deal { grid-column: span 2; } .ccard { min-height: 260px; }
}
</style>
