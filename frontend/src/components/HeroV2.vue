<template>
  <div class="v2home">
    <section class="v2hero">
      <div class="hpanel" aria-hidden="true"><i class="hstripe" /><span class="series">Collector series<b>{{ year }}</b></span></div>
      <div class="v2wrap grid">
        <div class="copy">
          <span class="kick">Ares Toys</span>
          <h1><span>Nhỏ tỉ lệ.</span><span class="or">Lớn đam mê.</span></h1>
          <p>Thế giới xe mô hình và RC chính hãng dành cho những người thật sự đam mê.</p>
          <div class="cta">
            <router-link to="/products" class="btn v2-black">Khám phá bộ sưu tập <b aria-hidden="true">›</b></router-link>
            <router-link :to="{ path: '/products', query: { sort: 'newest' } }" class="btn btn-outline">Xem xe mới về <b aria-hidden="true">›</b></router-link>
          </div>
        </div>
        <div class="stage">
          <span class="tag-top" aria-hidden="true">Diecast cars<br />RC models<br />for collectors</span>
          <div class="big" aria-hidden="true">{{ hero.scale }}</div>
          <router-link :to="hero.link" class="car" :aria-label="hero.name"><img src="/images/toys/cut/hw-racer.webp" width="1600" height="990" alt="" fetchpriority="high" /></router-link>
          <router-link :to="hero.link" class="note"><b>{{ hero.name }}</b><span>{{ hero.scale }} scale · {{ hero.brand }}</span><span class="pr">{{ vnd(hero.price) }}</span></router-link>
          <div class="script" aria-hidden="true">Small scale · Big passion</div>
        </div>
      </div>
    </section>

    <div class="ticker">
      <div class="v2wrap tk">
        <span class="flow">Diecast · RC · Limited edition · Ares Toys</span><i class="checker sm" aria-hidden="true" />
        <ul class="badges">
          <li v-for="b in badges" :key="b[1]"><svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true" v-html="b[0]" />{{ b[1] }}</li>
        </ul>
      </div>
    </div>

    <section class="v2wrap pick">
      <div class="pick-copy">
        <span class="kick">Bộ sưu tập nổi bật</span>
        <h2><span>Chọn gu.</span><span class="or">Chọn xe.</span></h2>
        <p>Từ những huyền thoại thu nhỏ đến mẫu RC mạnh mẽ và phụ kiện cao cấp — tất cả dành cho nhà sưu tầm như bạn.</p>
        <i class="checker" aria-hidden="true" />
      </div>
      <div class="cards">
        <router-link v-for="(c, i) in cards" :key="c.title" :to="c.to" class="ccard">
          <img :src="c.img" alt="" loading="lazy" decoding="async" />
          <b class="num">0{{ i + 1 }}</b><span class="arr" aria-hidden="true">→</span>
          <div class="cc-bot"><h3>{{ c.title }}</h3><small class="sub">{{ c.sub }}</small><small v-if="c.name" class="nm">{{ c.name }}</small><b v-if="c.price">{{ vnd(c.price) }}</b></div>
        </router-link>
        <router-link to="/promotions" class="deal">
          <b>Deal</b><strong>-{{ deal }}%</strong><small>Phụ kiện chính hãng</small><span class="arr" aria-hidden="true">→</span>
        </router-link>
      </div>
    </section>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { vnd } from '../format'
const props = defineProps({ products: { type: Array, default: () => [] } })
const year = new Date().getFullYear()
const find = re => props.products.find(p => re.test(p.images?.[0] || ''))
const hero = computed(() => {
  const p = find(/hw-r34-0/)
  return { link: p ? `/products/${p.id}` : '/products', name: p?.name || 'Hot Wheels Premium', scale: p?.scale || '1:64', brand: p?.brand?.name || 'Hot Wheels', price: p?.price || 0, ghost: (p?.brand?.name || 'Hot Wheels').toUpperCase() }
})
const card = (re, title, sub, to, img) => { const p = find(re); return { title, sub, to, img, name: p?.name, price: p?.price } }
const cards = computed(() => [
  card(/bburago-sf90-0/, 'Xe mô hình', 'Diecast · 1:64 · 1:18 · Limited', { path: '/products', query: { type: 'MODEL,DISPLAY' } }, '/images/toys/bburago-sf90-0.jpg'),
  card(/traxxas-xmaxx-0/, 'Xe RC', 'Off-road · On-road · Drift · RTR', { path: '/products', query: { type: 'RC' } }, '/images/toys/traxxas-xmaxx-0.jpg'),
  card(/charger-[01]/, 'Phụ kiện', 'Pin & sạc · Khiển RC · Bánh xe', { path: '/products', query: { grp: 'ACCESSORY' } }, '/images/toys/charger-1.jpg')])
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
.v2wrap { max-width: 1240px; margin: 0 auto; padding: 0 32px; }
.kick { display: block; font: 600 .72rem/1 'Oswald', sans-serif; letter-spacing: .38em; text-transform: uppercase; color: #222; margin-bottom: 18px; }
h1, h2 { font-family: 'Anton', 'Oswald', sans-serif; font-style: italic; font-weight: 400; text-transform: uppercase; letter-spacing: .01em; color: #111; margin: 0; }
h1 span, h2 span { display: block; line-height: 1.04; padding-top: .1em; } /* chừa chỗ cho dấu tiếng Việt chồng (Ỏ, Ệ, Ố) */
h1 { font-size: clamp(3.4rem, 6vw, 5.4rem); } h2 { font-size: clamp(2.6rem, 3.8vw, 3.6rem); }
.or { color: var(--v2-orange); }
.copy p, .pick-copy p { max-width: 400px; margin: 20px 0 26px; color: #2b2b2b; font-size: 1rem; line-height: 1.6; }

/* HERO */
.v2hero { position: relative; padding-top: 64px; background: var(--v2-cream); overflow: hidden; }
.hpanel { position: absolute; z-index: 0; top: 64px; bottom: 0; right: 0; width: max(38%, calc(50% - 620px + 470px)); background: var(--v2-orange); clip-path: polygon(34% 0, 100% 0, 100% 100%, 0 100%); }
.hstripe { position: absolute; top: 0; bottom: 0; left: 21%; width: 30px; background: #111; transform: skewX(-21deg); }
.series { position: absolute; right: 32px; top: 26px; color: #fff; font: 600 .66rem/1.4 'Oswald', sans-serif; letter-spacing: .26em; text-transform: uppercase; text-align: right; }
.series b { display: block; font: italic 400 2rem/1.1 'Anton', sans-serif; letter-spacing: .02em; }
.ghost { position: absolute; right: 30px; top: 104px; font: italic 400 2.2rem/1 'Anton', sans-serif; color: transparent; -webkit-text-stroke: 1.2px rgba(255,255,255,.7); letter-spacing: .04em; white-space: nowrap; }
.grid { position: relative; z-index: 1; display: grid; grid-template-columns: 5fr 7fr; align-items: center; min-height: 540px; }
.copy { position: relative; z-index: 3; padding: 48px 0 56px; }
.cta { display: flex; gap: 14px; flex-wrap: wrap; } .cta .btn { height: 48px; padding: 0 22px; } .cta .btn b { margin-left: 10px; font-size: 1.1rem; }
.stage { position: relative; height: 540px; }
.tag-top { position: absolute; left: 0; top: 40px; z-index: 1; font: 600 .62rem/1.6 'Oswald', sans-serif; letter-spacing: .24em; text-transform: uppercase; color: #333; }
.big { position: absolute; left: 21%; top: 6px; z-index: 0; font: italic 400 clamp(8.5rem, 12.5vw, 13.5rem)/1 'Anton', sans-serif; color: transparent; -webkit-text-stroke: 2px rgba(17,17,17,.85); letter-spacing: -.01em; white-space: nowrap; pointer-events: none; }
.car { position: absolute; z-index: 2; left: 12%; right: -2%; bottom: 92px; }
.car img { display: block; width: 100%; height: auto; filter: drop-shadow(0 18px 14px rgba(0,0,0,.30)) drop-shadow(0 4px 4px rgba(0,0,0,.25)); }
.car::after { content: ''; position: absolute; z-index: -1; left: 8%; right: 6%; bottom: -14px; height: 40px; background: radial-gradient(closest-side, rgba(0,0,0,.42), transparent); }
.note { position: absolute; z-index: 3; left: 0; bottom: 24px; display: flex; flex-direction: column; gap: 2px; padding: 8px 12px 8px 14px; background: #fff; border-left: 4px solid var(--v2-orange); color: #111; max-width: 250px; box-shadow: 0 6px 18px rgba(0,0,0,.08); }
.note b { font: 600 .82rem/1.3 'Oswald', sans-serif; text-transform: uppercase; letter-spacing: .04em; }
.note span { font-size: .72rem; color: #555; letter-spacing: .06em; text-transform: uppercase; } .note .pr { font: italic 400 1.2rem/1.1 'Anton', sans-serif; color: var(--v2-orange); letter-spacing: 0; text-transform: none; }
.note:hover { color: #111; } .note:hover b { color: var(--v2-orange); }
.script { position: absolute; z-index: 3; right: 0; bottom: 30px; font: italic 600 1.15rem/1 Georgia, 'Times New Roman', serif; color: #fff; white-space: nowrap; }
.checker { display: block; width: 52px; height: 18px; background: conic-gradient(#111 25%, transparent 0 50%, #111 0 75%, transparent 0) 0 0/9px 9px; transform: skewX(-20deg); }

/* TICKER */
.ticker { background: #111; color: #eee; }
.tk { display: flex; align-items: center; gap: 24px; height: 54px; }
.flow { font: 500 .68rem 'Oswald', sans-serif; letter-spacing: .42em; text-transform: uppercase; color: #9a9a9a; white-space: nowrap; }
.checker.sm { width: 38px; height: 13px; flex-shrink: 0; background: conic-gradient(#fff 25%, transparent 0 50%, #fff 0 75%, transparent 0) 0 0/6.5px 6.5px; }
.badges { list-style: none; display: flex; gap: 36px; margin-left: auto; }
.badges li { display: flex; align-items: center; gap: 9px; font-size: .84rem; font-weight: 500; white-space: nowrap; }
.badges svg { color: var(--v2-orange); }

/* CHỌN GU */
.pick { display: grid; grid-template-columns: 270px 1fr; gap: 28px; padding-top: 48px; padding-bottom: 24px; align-items: stretch; }
.pick-copy { align-self: center; } .pick-copy p { font-size: .95rem; }
.cards { display: grid; grid-template-columns: 1fr 1fr 1fr 140px; gap: 0; height: 420px; }
.ccard { position: relative; margin-right: -26px; overflow: hidden; color: #fff; background: #1a1a1a; display: flex; flex-direction: column; justify-content: space-between;
  clip-path: polygon(26px 0, 100% 0, calc(100% - 26px) 100%, 0 100%); }
.ccard + .ccard, .deal { margin-left: 8px; }
.ccard img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; transition: transform .25s ease-out; }
.ccard::after { content: ''; position: absolute; inset: 0; background: linear-gradient(180deg, transparent 0%, transparent 42%, rgba(0,0,0,.55) 64%, rgba(0,0,0,.9) 100%); }
.num, .arr, .cc-bot { position: relative; z-index: 1; }
.num { position: absolute; top: 18px; left: 40px; font: italic 400 1.5rem/1 'Anton', sans-serif; color: #fff; background: var(--v2-orange); padding: 6px 10px 4px; transform: skewX(-12deg); }
.ccard > .arr { position: absolute; top: 18px; right: 40px; width: 34px; height: 34px; display: grid; place-items: center; background: #111; color: #fff; font-size: 1.1rem; }
.ccard:hover > .arr { background: var(--v2-orange); }
.cc-bot { margin-top: auto; padding: 0 44px 24px 26px; }
.cc-bot h3 { font: italic 600 1.5rem/1.2 'Oswald', sans-serif; color: #fff; margin: 0; white-space: nowrap; }
.cc-bot .sub { display: block; font: 500 .66rem/1.5 'Oswald', sans-serif; letter-spacing: .14em; text-transform: uppercase; color: #ddd; margin: 4px 0 12px; }
.cc-bot .nm { display: block; font: 500 .78rem/1.35 'Be Vietnam Pro', sans-serif; color: #f2f2f2; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cc-bot b { font: italic 400 1.8rem/1.05 'Anton', sans-serif; color: var(--v2-orange); }
.ccard:hover, .deal:hover { color: #fff; }
.deal { position: relative; background: var(--v2-orange); color: #fff; display: flex; flex-direction: column; gap: 6px; padding: 26px 18px 22px 30px;
  clip-path: polygon(26px 0, 100% 0, 100% 100%, 0 100%); transition: background-color .15s ease-out; }
.deal:hover { background: #d94a0e; }
.deal b { font: italic 400 2.2rem/1 'Anton', sans-serif; text-transform: uppercase; padding-top: .05em; }
.deal strong { font: italic 400 2.6rem/1 'Anton', sans-serif; }
.deal small { font: 600 .7rem/1.5 'Oswald', sans-serif; letter-spacing: .14em; text-transform: uppercase; }
.deal .arr { margin: auto 0 0; }
@media (max-width: 1365px) { .script { display: none; } }
@media (max-width: 1000px) { .pick { grid-template-columns: 1fr; } .pick-copy p { max-width: 640px; } }
@media (max-width: 1200px) { .pick { grid-template-columns: 220px 1fr; gap: 20px; } .cards { grid-template-columns: 1fr 1fr 1fr 110px; } .cc-bot h3 { font-size: 1.25rem; } .cc-bot b { font-size: 1.5rem; } .num { left: 34px; } .ccard > .arr { right: 34px; } .cc-bot .sub { display: none; } .deal { padding: 22px 10px 18px 28px; } .deal b { font-size: 1.6rem; } .deal strong { font-size: 1.85rem; } .deal small { font-size: .6rem; letter-spacing: .08em; } .tag-top { display: none; } .big { left: 4%; } .ghost { display: none; } }
@media (max-width: 1100px) { .grid { grid-template-columns: 1fr 1fr; } .car { left: 0; right: 2%; bottom: 110px; } .badges { gap: 20px; } .badges li:nth-child(4) { display: none; } }
@media (max-width: 860px) {
  .grid { grid-template-columns: 1fr; } .hpanel { width: 60%; top: auto; height: 420px; } .stage { height: 420px; } .car { bottom: 96px; }
  .cards { grid-template-columns: 1fr 1fr; gap: 10px; } .ccard { min-height: 300px; margin: 0 !important; clip-path: none; } .deal { margin: 0; clip-path: none; }
  .badges { display: none; }
}
</style>
