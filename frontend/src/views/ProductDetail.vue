<template>
  <div class="container page pd">
    <div v-if="!p" class="spinner" />
    <template v-else>
      <Breadcrumb :items="[{ label: 'Danh mục xe', to: '/products' }, { label: p.brand?.name, to: { path: '/products', query: { brandId: p.brand?.id } } }, { label: p.name }]" />

      <!-- Carousel 1280x500 -->
      <div class="carousel">
        <div v-if="view === 'design'" class="design"><CarPreview :body="paintHex" :wheel="wheel.hex" :interior="interior.hex" :type="p.type" :scale="p.year ? String(p.year) : ''" /></div>
        <img v-else :src="p.images[view]" :alt="p.name" class="photo" />
        <button v-if="slides.length > 1" class="arr l" aria-label="Ảnh trước" @click="step(-1)">‹</button>
        <button v-if="slides.length > 1" class="arr r" aria-label="Ảnh sau" @click="step(1)">›</button>
        <div class="iconbtns">
          <button :class="{ on: prefs.isWished(p.id) }" title="Yêu thích" @click="prefs.toggleWish(p)"><svg viewBox="0 0 24 24" width="18" height="18" :fill="prefs.isWished(p.id) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z"/></svg></button>
          <button v-if="p.type !== 'ACCESSORY'" :class="{ on: prefs.inCompare(p.id) }" title="So sánh" @click="prefs.toggleCompare(p)"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M8 21H3v-5M21 3l-7 7M3 21l7-7"/></svg></button>
          <button title="Sao chép liên kết" @click="copyLink"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7"/></svg></button>
        </div>
      </div>
      <div class="cdots"><button v-for="(v, i) in slides" :key="i" :class="{ on: view === v }" :aria-label="`Ảnh ${i + 1}`" @click="view = v" /></div>

      <div class="head">
        <div>
          <div class="brandline"><img v-if="p.brand?.logoUrl" :src="p.brand.logoUrl" alt="" />{{ p.brand?.name }} · {{ TYPES[p.type] }}<span v-if="p.year"> · {{ p.year }}</span><span v-if="p.isNew" class="badge tag-yellow">Mới</span></div>
          <h1 class="title">{{ p.name }}</h1>
          <a v-if="rv.count" href="#reviews" class="ratingline"><Stars :value="rv.avg" /><b>{{ rv.avg }}</b><span>{{ rv.count }} đánh giá · Đã bán {{ p.sold }}</span></a>
        </div>
        <div class="pricecol">
          <span v-if="p.oldPrice" class="old-price">{{ vnd(p.oldPrice) }}</span>
          <div class="price-lg">{{ vnd(p.price) }}</div>
          <small>{{ p.type === 'ACCESSORY' ? 'Miễn phí lắp đặt' : 'Miễn phí vận chuyển toàn quốc' }}</small>
        </div>
      </div>

      <p class="desc">{{ p.description }}</p>
      <div v-if="keySpecs.length" class="keyspecs"><div v-for="k in keySpecs" :key="k[0]"><span>{{ k[0] }}</span><b>{{ k[1] }}</b></div></div>

      <div class="specs">
        <button class="specs-h" @click="specsOpen = !specsOpen"><span>Thông số kỹ thuật</span><i :class="{ open: specsOpen }">⌄</i></button>
        <dl v-show="specsOpen" class="spec"><template v-for="(v, k) in p.specs" :key="k"><dt>{{ k }}</dt><dd>{{ v }}</dd></template></dl>
      </div>

      <section id="reviews" class="panel reviews">
        <div class="ph"><div class="rv-title"><h2>Đánh giá</h2><span v-if="rv.count" class="rate-pill">★ {{ rv.avg }} · {{ rv.count }}</span></div>
          <button v-if="rv.items?.length > 3" class="link" @click="showAll = !showAll">{{ showAll ? 'Thu gọn' : 'Xem tất cả →' }}</button></div>
        <div v-if="!rv.items?.length" class="muted">Chưa có đánh giá nào. Hãy là người đầu tiên!</div>
        <div :class="showAll ? 'rv-list' : 'rv-row'">
          <div v-for="r in rv.items?.slice(0, showAll ? 99 : 3)" :key="r.id" class="rv">
            <div class="rv-h"><span class="av">{{ r.authorName?.split(' ').pop()[0] }}</span><div><b>{{ r.authorName }}</b><div class="muted small">{{ d(r.createdAt) }}</div></div><span class="rate-pill sm">★ {{ r.rating }}</span></div>
            <p>{{ r.comment }}</p>
          </div>
        </div>
        <form v-if="auth.loggedIn && !rv.mine" class="rv-form" @submit.prevent="sendReview">
          <b>Viết đánh giá của bạn</b><Stars v-model:value="mine.rating" pick size="1.4rem" />
          <input v-model="mine.comment" class="input" placeholder="Chia sẻ trải nghiệm lái thử, mua xe, dịch vụ…" />
          <button class="btn btn-sm" :disabled="sending">Gửi</button>
        </form>
        <div v-else-if="!auth.loggedIn" class="rv-login"><router-link :to="{ path: '/login', query: { redirect: $route.fullPath } }">Đăng nhập</router-link> để viết đánh giá.</div>
      </section>

      <router-link to="/promotions" class="promo"><img :src="p.images[1] || p.images[0]" alt="" /><div><small>Ưu đãi tháng 10</small><b>Giảm đến 50 triệu + tặng gói phụ kiện</b><span>Nhập mã THANG10 khi thanh toán →</span></div></router-link>

      <section v-if="p.type !== 'ACCESSORY'" class="feature">
        <img :src="p.images[2] || p.images[0]" alt="" />
        <div><span class="eyebrow">Điểm nổi bật</span><h2>{{ keySpecs[0]?.[1] || p.name }}</h2>
          <p class="muted">{{ p.description?.split('.')[0] }}. Bảo hành chính hãng 3–10 năm, giao xe tận nhà và hỗ trợ trả góp đến 80% giá trị xe.</p>
          <router-link :to="{ path: '/services', query: { productId: p.id } }" class="btn btn-outline btn-sm">Đăng ký lái thử →</router-link></div>
      </section>

      <section v-if="p.type !== 'ACCESSORY' && unitPrice >= 300000000" class="loan">
        <div class="loan-l"><span class="eyebrow">Công cụ tính trả góp</span><h2>Ước tính khoản trả hàng tháng</h2>
          <div class="sl"><div class="sl-h"><span>Trả trước</span><b>{{ loan.down }}% · {{ vnd(downAmt) }}</b></div><input v-model.number="loan.down" type="range" min="20" max="80" step="5" /></div>
          <div class="sl"><div class="sl-h"><span>Kỳ hạn</span><b>{{ loan.months }} tháng</b></div><input v-model.number="loan.months" type="range" min="12" max="84" step="12" /></div>
          <div class="sl"><div class="sl-h"><span>Lãi suất</span><b>{{ loan.rate.toFixed(1) }}%/tháng</b></div><input v-model.number="loan.rate" type="range" min="0.4" max="1.2" step="0.1" /></div>
        </div>
        <div class="loan-r"><small>Trả hàng tháng</small><div class="monthly">{{ vnd(monthly) }}</div>
          <dl><dt>Số tiền vay</dt><dd>{{ vnd(loanAmt) }}</dd><dt>Tổng lãi</dt><dd>{{ vnd(interest) }}</dd><dt>Tổng phải trả</dt><dd>{{ vnd(downAmt + loanAmt + interest) }}</dd></dl></div>
      </section>

      <section v-if="likes.length" class="block">
        <div class="ph"><h2>Có thể bạn thích</h2><router-link :to="{ path: '/products', query: { type: p.type } }" class="link">Xem tất cả →</router-link></div>
        <div class="grid grid-4"><ProductCard v-for="r in likes" :key="r.id" :p="r" /></div>
      </section>
      <section v-if="recent.length" class="block">
        <div class="ph"><h2>Bạn đã xem gần đây</h2></div>
        <div class="recent"><router-link v-for="r in recent" :key="r.id" :to="`/products/${r.id}`" class="rc"><img :src="r.images?.[0]" alt="" /><b>{{ r.name }}</b><span>{{ vnd(r.price) }}</span></router-link></div>
      </section>

      <!-- Configurator: mở khi chọn "Tùy chỉnh" -->
      <div v-if="mode === 'custom'" class="cfg-back" @click.self="mode = 'ready'">
        <div class="cfg">
          <div class="cfg-prev"><CarPreview :body="paintHex" :wheel="wheel.hex" :interior="interior.hex" :type="p.type" :scale="p.year ? String(p.year) : ''" /></div>
          <div class="cfg-side"><h3>Tùy chỉnh {{ p.name }}</h3>
            <div v-for="g in groups" :key="g.key" class="opt">
              <div class="opt-head"><span>{{ g.label }}</span><b>{{ g.cur.name }}<em v-if="g.cur.price"> +{{ ty(g.cur.price) }}</em></b></div>
              <div class="swatches"><button v-for="o in g.list" :key="o.id" :class="['swatch', { active: g.cur.id === o.id }]" :title="o.name"
                :style="{ background: g.key === 'wheel' ? `radial-gradient(circle, #111 25%, ${o.hex} 30%)` : (o.hex || p.baseColor) }" @click="sel[g.key] = o.id" /></div>
            </div>
            <div class="cfg-total"><span>Giá theo cấu hình</span><b>{{ vnd(unitPrice) }}</b></div>
            <button class="btn btn-block" @click="mode = 'custom-done'; view = 'design'">Áp dụng cấu hình</button>
          </div>
        </div>
      </div>

      <!-- Sticky buy bar -->
      <div class="buybar">
        <div class="bb-in">
          <div class="bb-l">
            <img :src="p.images[0]" alt="" />
            <div><b>{{ p.name }}</b>
              <select v-if="p.type !== 'ACCESSORY'" v-model="modeSel" class="input mode">
                <option value="ready">Mua sẵn — {{ p.stock ? `còn ${p.stock} xe` : 'hết hàng' }}</option>
                <option v-if="canCustomize" value="custom">Tùy chỉnh (màu sơn, mâm, nội thất)</option>
                <option value="acc">Kèm phụ kiện</option>
              </select>
              <small v-else :class="p.stock ? 'ok' : 'out'">{{ p.stock ? `Còn ${p.stock} sản phẩm` : 'Tạm hết hàng' }}</small>
            </div>
          </div>
          <div v-if="modeSel === 'acc'" class="accs"><label v-for="a in accs" :key="a.id" :class="{ on: accSel.includes(a.id) }"><input v-model="accSel" type="checkbox" :value="a.id" />{{ a.name.split(' ').slice(0, 3).join(' ') }} <em>+{{ ty(a.price) }}</em></label></div>
          <div class="qty"><button @click="qty = Math.max(1, qty - 1)">−</button><span>{{ qty }}</span><button @click="qty = Math.min(p.stock, qty + 1)">+</button></div>
          <div class="bb-price"><small v-if="extra || accTotal">{{ extra ? 'Theo cấu hình' : 'Gồm phụ kiện' }}</small><b>{{ vnd(unitPrice * qty + accTotal) }}</b></div>
          <button class="btn btn-outline" :disabled="!p.stock" @click="add(false)">Thêm vào giỏ</button>
          <button class="btn" :disabled="!p.stock" @click="add(true)">{{ p.type === 'ACCESSORY' ? 'Mua ngay' : 'Mua ngay' }}</button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { vnd, ty, d, TYPES } from '../format'
import Breadcrumb from '../components/Breadcrumb.vue'
import Stars from '../components/Stars.vue'
import { usePrefs, toast, loadRatings } from '../stores/prefs'
import { useAuth } from '../stores/auth'
import { useCart } from '../stores/cart'
import CarPreview from '../components/CarPreview.vue'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute(), router = useRouter(), cart = useCart(), prefs = usePrefs(), auth = useAuth()
const rv = ref({}), mine = reactive({ rating: 5, comment: '' }), sending = ref(false), showAll = ref(false), suggested = ref([])
const loan = reactive({ down: 30, months: 60, rate: 0.6 })
const url = computed(() => location.origin + route.fullPath)
const recent = computed(() => prefs.recent.filter(r => r.id !== p.value?.id).slice(0, 6))
const copyLink = () => { navigator.clipboard?.writeText(url.value); toast('Đã sao chép liên kết') }
const loadReviews = async id => { rv.value = (await api.get(`/products/${id}/reviews`)).data }
async function sendReview() {
  sending.value = true
  try { await api.post(`/products/${p.value.id}/reviews`, mine); toast('Cảm ơn bạn đã đánh giá!'); mine.comment = ''; await loadReviews(p.value.id); loadRatings(true) }
  catch (e) { toast(e.response?.data?.message || 'Không gửi được đánh giá', 'error') } finally { sending.value = false }
}
const p = ref(null), related = ref([]), opts = ref({ paints: [], wheels: [], interiors: [] })
const qty = ref(1), view = ref(0), added = ref(false), specsOpen = ref(false), mode = ref('ready'), accs = ref([]), accSel = ref([])
const modeSel = computed({ get: () => mode.value === 'custom-done' ? 'custom' : mode.value, set: v => { mode.value = v; if (v !== 'acc') accSel.value = [] } })
const slides = computed(() => [...(p.value?.images || []).map((_, i) => i), ...(canCustomize.value && mode.value !== 'ready' && mode.value !== 'acc' ? ['design'] : [])])
const step = k => { const l = slides.value, i = l.indexOf(view.value); view.value = l[(i + k + l.length) % l.length] }
const accTotal = computed(() => accs.value.filter(a => accSel.value.includes(a.id)).reduce((t, a) => t + a.price, 0))
const likes = computed(() => [...suggested.value, ...related.value].filter((x, i, a) => a.findIndex(y => y.id === x.id) === i).slice(0, 4))
const sel = reactive({ paint: 'original', wheel: 'standard', interior: 'black' })

const find = (list, id) => list.find(o => o.id === id) || list[0] || { name: '', price: 0 }
const paint = computed(() => find(opts.value.paints, sel.paint))
const wheel = computed(() => find(opts.value.wheels, sel.wheel))
const interior = computed(() => find(opts.value.interiors, sel.interior))
const canCustomize = computed(() => p.value?.customizable && p.value.type !== 'ACCESSORY')
const paintHex = computed(() => paint.value.hex || p.value?.baseColor)
const extra = computed(() => canCustomize.value && mode.value.startsWith('custom') ? paint.value.price + wheel.value.price + interior.value.price : 0)
const unitPrice = computed(() => p.value.price + extra.value)
const downAmt = computed(() => Math.round(unitPrice.value * loan.down / 100))
const loanAmt = computed(() => unitPrice.value - downAmt.value)
const interest = computed(() => Math.round(loanAmt.value * loan.rate / 100 * loan.months))
const monthly = computed(() => Math.round((loanAmt.value + interest.value) / loan.months))
const groups = computed(() => [
  { key: 'paint', label: 'Màu sơn', list: opts.value.paints, cur: paint.value },
  { key: 'wheel', label: 'Mâm xe', list: opts.value.wheels, cur: wheel.value },
  { key: 'interior', label: 'Nội thất', list: opts.value.interiors, cur: interior.value }])
const keySpecs = computed(() => ['Công suất', 'Động cơ', 'Hộp số', 'Số chỗ ngồi'].filter(k => p.value?.specs?.[k]).map(k => [k, p.value.specs[k]]))

async function load() {
  p.value = null; added.value = false; qty.value = 1; view.value = 0; mode.value = 'ready'; accSel.value = []; specsOpen.value = false
  Object.assign(sel, { paint: 'original', wheel: 'standard', interior: 'black' })
  const [d, o] = await Promise.all([api.get(`/products/${route.params.id}`), api.get('/options')])
  p.value = d.data.product; related.value = d.data.related; opts.value = o.data.customizer
  prefs.viewed(p.value); if (!accs.value.length) api.get('/products', { params: { type: 'ACCESSORY', size: 8 } }).then(r => { accs.value = r.data.content }); showAll.value = false; loadReviews(p.value.id)
  const relIds = new Set([p.value.id, ...related.value.map(r => r.id)])
  api.get('/products', { params: { type: p.value.type, size: 8, sort: 'bestseller' } }).then(r => { suggested.value = r.data.content.filter(x => !relIds.has(x.id)).slice(0, 4) })
}
function add(buyNow) {
  const v = p.value
  cart.add({
    productId: v.id, name: v.name, fuel: v.fuel, type: v.type, baseColor: paintHex.value, wheelHex: wheel.value.hex, interiorHex: interior.value.hex,
    image: v.images?.[0], unitPrice: unitPrice.value, quantity: qty.value, stock: v.stock,
    options: extra.value || mode.value.startsWith('custom') ? { ...sel } : {},
    optionLabel: mode.value.startsWith('custom') ? `${paint.value.name} · ${wheel.value.name} · Nội thất ${interior.value.name}` : (v.type === 'ACCESSORY' ? '' : 'Màu nguyên bản')
  })
  accs.value.filter(a => accSel.value.includes(a.id)).forEach(a => cart.add({ productId: a.id, name: a.name, type: a.type, image: a.images?.[0], unitPrice: a.price, quantity: 1, stock: a.stock, options: {}, optionLabel: '' }))
  if (buyNow) router.push('/cart'); else { added.value = true; toast(`Đã thêm “${v.name}” vào giỏ hàng`) }
}
watch(() => route.params.id, id => id && load(), { immediate: true })
</script>

<style scoped>
.pd { padding-bottom: 120px; }
.carousel { position: relative; height: 500px; border-radius: 12px; overflow: hidden; background: var(--tile); }
.photo { width: 100%; height: 100%; object-fit: cover; }
.design { height: 100%; display: flex; align-items: center; justify-content: center; padding: 32px 120px; background: radial-gradient(ellipse at 50% 70%, rgba(168,122,60,.15), var(--tile) 65%); }
.arr { position: absolute; top: 50%; transform: translateY(-50%); width: 44px; height: 44px; border-radius: 50%; border: none; background: rgba(255,255,255,.85); font-size: 1.8rem; line-height: 1; cursor: pointer; color: #333; }
.arr.l { left: 16px; } .arr.r { right: 16px; } .arr:hover { background: #fff; }
.iconbtns { position: absolute; top: 14px; right: 14px; display: flex; gap: 8px; }
.iconbtns button { width: 38px; height: 38px; border-radius: 50%; border: none; background: rgba(255,255,255,.9); color: #333; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.iconbtns button.on { color: #e53935; } .iconbtns button:nth-child(2).on { color: var(--bronze); }
.cdots { display: flex; justify-content: center; gap: 6px; margin: 12px 0 20px; }
.cdots button { width: 8px; height: 8px; border-radius: 50%; border: none; background: #cfcfcf; cursor: pointer; padding: 0; }
.cdots button.on { background: #555; width: 20px; border-radius: 4px; }
.head { display: flex; justify-content: space-between; gap: 24px; align-items: flex-start; }
.brandline { display: flex; align-items: center; gap: 6px; color: var(--muted); font-size: .82rem; font-weight: 600; text-transform: uppercase; letter-spacing: .08em; }
.brandline img { height: 18px; width: 18px; object-fit: contain; filter: invert(1) brightness(.3); } .brandline .badge { margin-left: 6px; }
.title { font-family: var(--head); font-size: 2.2rem; line-height: 1.15; margin: 6px 0; }
.ratingline { display: flex; align-items: center; gap: 8px; color: var(--muted); font-size: .88rem; } .ratingline b { color: var(--text); }
.pricecol { text-align: right; flex-shrink: 0; } .pricecol .old-price { margin: 0; }
.pricecol .price-lg { color: var(--text); } .pricecol small { color: var(--success); font-weight: 600; font-size: .82rem; }
.desc { color: var(--text-2); white-space: pre-line; line-height: 1.7; margin: 18px 0; max-width: 880px; }
.keyspecs { display: grid; grid-template-columns: repeat(4, 1fr); background: var(--bg-2); border-radius: 10px; }
.keyspecs div { padding: 14px 18px; } .keyspecs span { display: block; color: var(--muted); font-size: .75rem; text-transform: uppercase; letter-spacing: .06em; } .keyspecs b { font-size: 1rem; }
.specs { margin-top: 16px; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.specs-h { width: 100%; display: flex; justify-content: space-between; align-items: center; padding: 16px 0; background: none; border: none; cursor: pointer; font: 600 1.05rem var(--head); color: var(--text); }
.specs-h i { font-style: normal; transition: .2s; font-size: 1.3rem; } .specs-h i.open { transform: rotate(180deg); }
.spec { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; column-gap: 32px; padding-bottom: 12px; }
.spec dt, .spec dd { padding: 10px 0; border-bottom: 1px dashed var(--border); font-size: .9rem; } .spec dt { color: var(--muted); } .spec dd { font-weight: 600; text-align: right; }
.panel { margin-top: 28px; background: var(--bg-2); border-radius: 12px; padding: 20px 22px; }
.ph { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; } .ph h2 { font-size: 1.2rem; }
.rv-title { display: flex; gap: 10px; align-items: center; }
.link { background: none; border: none; color: var(--muted); cursor: pointer; font: inherit; font-size: .88rem; } .link:hover { color: var(--bronze); }
.rate-pill { background: #fff; border-radius: 999px; padding: 3px 10px; font-size: .82rem; font-weight: 700; color: #b58a00; } .rate-pill.sm { font-size: .75rem; }
.rv-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; } .rv-list { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.rv { background: #fff; border-radius: 10px; padding: 14px 16px; }
.rv-h { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; } .rv-h > div { flex: 1; }
.av { width: 34px; height: 34px; border-radius: 50%; background: var(--bronze); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.rv p { color: var(--text-2); font-size: .9rem; line-height: 1.6; }
.rv-form { display: flex; gap: 12px; align-items: center; margin-top: 14px; } .rv-form .input { flex: 1; padding: 8px 12px; }
.rv-login { margin-top: 12px; color: var(--muted); font-size: .9rem; }
.promo { display: grid; grid-template-columns: 1fr 1fr; margin-top: 28px; min-height: 180px; border-radius: 12px; overflow: hidden; background: #13235a; color: #fff; }
.promo img { width: 100%; height: 100%; object-fit: cover; } .promo div { padding: 28px 32px; display: flex; flex-direction: column; justify-content: center; gap: 6px; }
.promo small { color: #f6c445; text-transform: uppercase; letter-spacing: .12em; font-size: .75rem; } .promo b { font-family: var(--head); font-size: 1.35rem; line-height: 1.2; } .promo span { opacity: .8; font-size: .9rem; }
.feature { display: grid; grid-template-columns: 1.2fr 1fr; gap: 36px; align-items: center; margin-top: 28px; }
.feature img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; }
.feature h2 { font-family: var(--head); font-size: 1.8rem; margin: 6px 0 10px; } .feature p { margin-bottom: 18px; line-height: 1.7; }
.loan { display: grid; grid-template-columns: 1.3fr 1fr; gap: 36px; margin-top: 28px; padding: 28px; border-radius: 12px; background: var(--bronze-soft); }
.loan h2 { font-size: 1.3rem; margin-bottom: 16px; }
.sl { margin-bottom: 16px; } .sl-h { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: .9rem; } .sl-h span { color: var(--muted); }
.sl input { width: 100%; accent-color: var(--bronze); }
.loan-r { background: #fff; border-radius: 12px; padding: 22px; } .loan-r small { color: var(--muted); }
.monthly { font-family: var(--head); font-size: 2rem; font-weight: 700; color: var(--bronze); margin: 2px 0 14px; }
.loan-r dl { display: grid; grid-template-columns: 1fr auto; gap: 8px; font-size: .9rem; } .loan-r dt { color: var(--muted); } .loan-r dd { font-weight: 600; text-align: right; }
.block { margin-top: 32px; }
.recent { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.rc { display: flex; flex-direction: column; gap: 4px; color: var(--text); font-size: .82rem; } .rc img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 8px; } .rc b { font-weight: 500; } .rc span { color: var(--muted); }
.buybar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 60; background: #fff; box-shadow: 0 -6px 24px rgba(0,0,0,.1); border-top: 1px solid var(--border); }
.bb-in { max-width: 1180px; margin: 0 auto; padding: 10px 24px; display: flex; align-items: center; gap: 14px; }
.bb-l { display: flex; gap: 12px; align-items: center; flex: 1; min-width: 0; }
.bb-l img { width: 72px; height: 48px; object-fit: cover; border-radius: 6px; } .bb-l b { display: block; font-size: .9rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mode { padding: 5px 10px; font-size: .82rem; margin-top: 3px; width: 280px; }
.ok { color: var(--success); } .out { color: var(--danger); }
.accs { display: flex; gap: 6px; flex-wrap: wrap; max-width: 380px; }
.accs label { font-size: .74rem; border: 1px solid var(--border); border-radius: 999px; padding: 3px 9px; cursor: pointer; display: flex; gap: 4px; align-items: center; } .accs input { display: none; }
.accs label.on { border-color: var(--bronze); background: var(--bronze-soft); } .accs em { font-style: normal; color: var(--bronze); }
.qty { display: flex; align-items: center; border: 1px solid var(--border); border-radius: 8px; } .qty button { width: 30px; height: 34px; border: none; background: none; cursor: pointer; font-size: 1rem; } .qty span { width: 26px; text-align: center; }
.bb-price { text-align: right; } .bb-price small { display: block; color: var(--muted); font-size: .72rem; } .bb-price b { font-size: 1.15rem; white-space: nowrap; }
.cfg-back { position: fixed; inset: 0; z-index: 90; background: rgba(0,0,0,.45); display: flex; align-items: center; justify-content: center; padding: 24px; }
.cfg { background: #fff; border-radius: 14px; width: min(1080px, 100%); display: grid; grid-template-columns: 1.5fr 1fr; overflow: hidden; }
.cfg-prev { background: radial-gradient(ellipse at 50% 70%, rgba(168,122,60,.15), var(--tile) 65%); display: flex; align-items: center; padding: 30px; }
.cfg-side { padding: 26px; } .cfg-side h3 { font-family: var(--head); margin-bottom: 18px; }
.opt { margin-bottom: 18px; } .opt-head { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: .88rem; } .opt-head span { color: var(--muted); } .opt-head em { font-style: normal; color: var(--bronze); }
.swatches { display: flex; gap: 10px; flex-wrap: wrap; } .swatch { width: 34px; height: 34px; }
.cfg-total { display: flex; justify-content: space-between; padding: 14px 0; border-top: 1px solid var(--border); margin-bottom: 12px; } .cfg-total b { font-size: 1.2rem; }
@media (max-width: 860px) { .carousel { height: 280px; } .head, .bb-in { flex-wrap: wrap; } .rv-row, .keyspecs, .spec, .feature, .loan, .cfg, .promo { grid-template-columns: 1fr; } .recent { grid-template-columns: repeat(3, 1fr); } }
</style>
