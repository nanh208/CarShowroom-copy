<template>
  <div class="container page">
    <div v-if="!p" class="spinner" />
    <template v-else>
      <Breadcrumb :items="[{ label: 'Mẫu xe', to: '/products' }, { label: p.brand?.name, to: { path: '/products', query: { brandId: p.brand?.id } } }, { label: p.name }]" />
      <div class="top">
        <div class="gallery">
          <div class="main-img">
            <div v-if="view === 'design' || !p.images.length" class="design"><CarPreview :body="paintHex" :wheel="wheel.hex" :interior="interior.hex" :type="p.type" :scale="p.year ? String(p.year) : ''" /></div>
            <img v-else :src="p.images[view]" :alt="p.name" class="photo" />
          </div>
          <div class="thumbs">
            <button v-for="(img, i) in p.images" :key="i" :class="['thumb', { on: view === i }]" @click="view = i"><img :src="img" alt="" /></button>
            <button v-if="canCustomize" :class="['thumb', 'thumb-design', { on: view === 'design' }]" @click="view = 'design'">Bản phối màu</button>
          </div>
          <div v-if="keySpecs.length" class="keyspecs">
            <div v-for="k in keySpecs" :key="k[0]"><span>{{ k[0] }}</span><b>{{ k[1] }}</b></div>
          </div>
        </div>

        <aside class="buybox">
          <div class="iconbtns">
            <button :class="{ on: prefs.isWished(p.id) }" title="Yêu thích" @click="prefs.toggleWish(p)"><svg viewBox="0 0 24 24" width="20" height="20" :fill="prefs.isWished(p.id) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z"/></svg></button>
            <button v-if="p.type !== 'ACCESSORY'" :class="{ on: prefs.inCompare(p.id) }" title="So sánh" @click="prefs.toggleCompare(p)"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M8 21H3v-5M21 3l-7 7M3 21l7-7"/></svg></button>
          </div>
          <div class="brandline"><img v-if="p.brand?.logoUrl" :src="p.brand.logoUrl" alt="" />{{ p.brand?.name }}<span v-if="p.isNew" class="badge badge-new">Mới</span></div>
          <h1 class="title">{{ p.name }}</h1>
          <a v-if="rv.count" href="#reviews" class="ratingline"><Stars :value="rv.avg" /><b>{{ rv.avg }}</b><span>{{ rv.count }} đánh giá</span><span>·</span><span>Đã bán {{ p.sold }}</span></a>
          <div class="chips"><span>{{ TYPES[p.type] }}</span><span v-if="p.fuel && p.type !== 'ACCESSORY'">{{ p.fuel }}</span><span v-if="p.year">{{ p.year }}</span></div>
          <div class="pricebox">
            <small>{{ extra ? 'Giá theo cấu hình' : 'Giá niêm yết' }}</small>
            <div><span class="price-lg">{{ vnd(unitPrice) }}</span><span v-if="p.oldPrice && !extra" class="old-price">{{ vnd(p.oldPrice) }}</span></div>
            <small v-if="extra">Giá xe {{ vnd(p.price) }} + tuỳ chọn {{ vnd(extra) }}</small>
            <small v-else-if="p.type !== 'ACCESSORY'">Đặt cọc từ <b>{{ vnd(Math.round(p.price * 0.1)) }}</b> · Trả góp từ 20%</small>
          </div>

          <div v-if="canCustomize" class="designer">
            <div class="opt" v-for="g in groups" :key="g.key">
              <div class="opt-head"><span>{{ g.label }}</span><b>{{ g.cur.name }}<em v-if="g.cur.price"> +{{ ty(g.cur.price) }}</em></b></div>
              <div class="swatches"><button v-for="o in g.list" :key="o.id" :class="['swatch', { active: g.cur.id === o.id }]" :title="o.name"
                :style="{ background: g.key === 'wheel' ? `radial-gradient(circle, #111 25%, ${o.hex} 30%)` : (o.hex || p.baseColor) }" @click="sel[g.key] = o.id; view = 'design'" /></div>
            </div>
          </div>

          <div class="stockline"><span :class="p.stock ? 'ok' : 'out'">● {{ p.stock ? `Còn ${p.stock} ${p.type === 'ACCESSORY' ? 'sản phẩm' : 'xe tại showroom'}` : 'Tạm hết hàng' }}</span>
            <div class="qty"><button @click="qty = Math.max(1, qty - 1)">−</button><span>{{ qty }}</span><button @click="qty = Math.min(p.stock, qty + 1)">+</button></div></div>
          <div class="actions">
            <button class="btn btn-lg" :disabled="!p.stock" @click="add(true)">{{ p.type === 'ACCESSORY' ? 'Mua ngay' : 'Đặt cọc ngay' }}</button>
            <button class="btn btn-lg btn-outline" :disabled="!p.stock" @click="add(false)">Thêm vào giỏ</button>
          </div>
          <div v-if="added" class="alert alert-success">Đã thêm vào giỏ hàng. <router-link to="/cart">Xem giỏ →</router-link></div>
          <router-link v-if="p.type !== 'ACCESSORY'" :to="{ path: '/services', query: { productId: p.id } }" class="testdrive">Đăng ký lái thử tại showroom →</router-link>
          <ul v-if="p.type === 'ACCESSORY'" class="perks"><li>Hàng chính hãng, bảo hành 12 tháng</li><li>Lắp đặt miễn phí tại xưởng dịch vụ ARES</li></ul>
          <ul v-else class="perks">
            <li>Xe mới chính hãng, bảo hành 3–10 năm</li><li>Đặt cọc 10% hoặc trả góp đến 80% giá trị xe</li><li>Giao xe tận nhà, miễn phí toàn quốc</li>
          </ul>
          <div class="share"><span>Chia sẻ</span>
            <a :href="`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`" target="_blank" rel="noopener" title="Facebook">f</a>
            <a :href="`https://zalo.me/share?url=${encodeURIComponent(url)}`" target="_blank" rel="noopener" title="Zalo">Z</a>
            <a :href="`https://twitter.com/intent/tweet?url=${encodeURIComponent(url)}&text=${encodeURIComponent(p.name)}`" target="_blank" rel="noopener" title="X">𝕏</a>
            <button title="Sao chép liên kết" @click="copyLink"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7"/></svg></button>
          </div>
        </aside>
      </div>

      <section v-if="p.type !== 'ACCESSORY' && unitPrice >= 300000000" class="loan section">
        <div class="loan-l"><span class="eyebrow">Công cụ tính trả góp</span><h2>Ước tính khoản trả hàng tháng</h2>
          <p class="muted">Lãi suất phẳng tham khảo. Số liệu chính xác do ngân hàng duyệt theo hồ sơ của bạn.</p>
          <div class="sl"><div class="sl-h"><span>Trả trước</span><b>{{ loan.down }}% · {{ vnd(downAmt) }}</b></div><input v-model.number="loan.down" type="range" min="20" max="80" step="5" /></div>
          <div class="sl"><div class="sl-h"><span>Kỳ hạn</span><b>{{ loan.months }} tháng</b></div><input v-model.number="loan.months" type="range" min="12" max="84" step="12" /></div>
          <div class="sl"><div class="sl-h"><span>Lãi suất</span><b>{{ loan.rate.toFixed(1) }}%/tháng</b></div><input v-model.number="loan.rate" type="range" min="0.4" max="1.2" step="0.1" /></div>
        </div>
        <div class="loan-r">
          <small>Trả hàng tháng</small><div class="monthly">{{ vnd(monthly) }}</div>
          <dl><dt>Giá xe theo cấu hình</dt><dd>{{ vnd(unitPrice) }}</dd><dt>Số tiền vay</dt><dd>{{ vnd(loanAmt) }}</dd>
            <dt>Tổng lãi</dt><dd>{{ vnd(interest) }}</dd><dt>Tổng phải trả</dt><dd>{{ vnd(downAmt + loanAmt + interest) }}</dd></dl>
          <button class="btn btn-block" :disabled="!p.stock" @click="add(true)">Mua trả góp</button>
        </div>
      </section>

      <div class="info section">
        <div><span class="eyebrow">Giới thiệu</span><h2>{{ p.name }}</h2><p class="desc">{{ p.description }}</p></div>
        <div><span class="eyebrow">Thông số kỹ thuật</span>
          <dl class="spec"><template v-for="(v, k) in p.specs" :key="k"><dt>{{ k }}</dt><dd>{{ v }}</dd></template></dl>
        </div>
      </div>

      <section id="reviews" class="section reviews">
        <div class="section-head"><div><span class="eyebrow">Khách hàng nói gì</span><h2>Đánh giá & nhận xét</h2></div></div>
        <div class="rv-grid">
          <div class="rv-sum">
            <div class="big">{{ rv.count ? rv.avg : '—' }}</div><Stars :value="rv.avg" size="1.3rem" /><div class="muted small">{{ rv.count }} đánh giá</div>
            <div v-for="i in [5, 4, 3, 2, 1]" :key="i" class="dist"><span>{{ i }}★</span><div class="bar"><div :style="{ width: (rv.count ? (rv.dist?.[i - 1] || 0) * 100 / rv.count : 0) + '%' }" /></div><span>{{ rv.dist?.[i - 1] || 0 }}</span></div>
          </div>
          <div>
            <form v-if="auth.loggedIn && !rv.mine" class="rv-form" @submit.prevent="sendReview">
              <b>Viết đánh giá của bạn</b>
              <Stars v-model:value="mine.rating" pick size="1.6rem" />
              <textarea v-model="mine.comment" class="input" rows="3" placeholder="Chia sẻ trải nghiệm lái thử, mua xe, dịch vụ…" />
              <button class="btn btn-sm" :disabled="sending">Gửi đánh giá</button>
            </form>
            <div v-else-if="!auth.loggedIn" class="rv-login"><router-link :to="{ path: '/login', query: { redirect: $route.fullPath } }">Đăng nhập</router-link> để viết đánh giá.</div>
            <div v-if="!rv.items?.length" class="muted">Chưa có đánh giá nào. Hãy là người đầu tiên!</div>
            <div v-for="r in rv.items?.slice(0, showAll ? 99 : 4)" :key="r.id" class="rv">
              <div class="rv-h"><span class="av">{{ r.authorName?.split(' ').pop()[0] }}</span><div><b>{{ r.authorName }}</b><div class="muted small">{{ d(r.createdAt) }}</div></div><Stars :value="r.rating" size=".9rem" /></div>
              <p>{{ r.comment }}</p>
            </div>
            <button v-if="rv.items?.length > 4 && !showAll" class="btn btn-sm btn-outline" @click="showAll = true">Xem tất cả {{ rv.items.length }} đánh giá</button>
          </div>
        </div>
      </section>

      <section v-if="suggested.length" class="section">
        <div class="section-head"><div><span class="eyebrow">Gợi ý cho bạn</span><h2>Có thể bạn quan tâm</h2></div><router-link :to="{ path: '/products', query: { type: p.type } }">Xem thêm →</router-link></div>
        <div class="grid grid-3"><ProductCard v-for="r in suggested" :key="r.id" :p="r" /></div>
      </section>

      <section v-if="recent.length" class="section">
        <div class="section-head"><div><span class="eyebrow">Lịch sử</span><h2>Đã xem gần đây</h2></div></div>
        <div class="recent"><router-link v-for="r in recent" :key="r.id" :to="`/products/${r.id}`" class="rc"><img :src="r.images?.[0]" alt="" /><b>{{ r.name }}</b><span class="price">{{ vnd(r.price) }}</span></router-link></div>
      </section>

      <section v-if="related.length" class="section">
        <div class="section-head"><div><span class="eyebrow">{{ p.brand?.name }}</span><h2>Cùng thương hiệu</h2></div></div>
        <div class="grid grid-3"><ProductCard v-for="r in related.slice(0, 3)" :key="r.id" :p="r" /></div>
      </section>
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
const qty = ref(1), view = ref('design'), added = ref(false)
const sel = reactive({ paint: 'original', wheel: 'standard', interior: 'black' })

const find = (list, id) => list.find(o => o.id === id) || list[0] || { name: '', price: 0 }
const paint = computed(() => find(opts.value.paints, sel.paint))
const wheel = computed(() => find(opts.value.wheels, sel.wheel))
const interior = computed(() => find(opts.value.interiors, sel.interior))
const canCustomize = computed(() => p.value?.customizable && p.value.type !== 'ACCESSORY')
const paintHex = computed(() => paint.value.hex || p.value?.baseColor)
const extra = computed(() => canCustomize.value ? paint.value.price + wheel.value.price + interior.value.price : 0)
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
  p.value = null; added.value = false; qty.value = 1; view.value = 0
  Object.assign(sel, { paint: 'original', wheel: 'standard', interior: 'black' })
  const [d, o] = await Promise.all([api.get(`/products/${route.params.id}`), api.get('/options')])
  p.value = d.data.product; related.value = d.data.related; opts.value = o.data.customizer
  prefs.viewed(p.value); showAll.value = false; loadReviews(p.value.id)
  const relIds = new Set([p.value.id, ...related.value.map(r => r.id)])
  api.get('/products', { params: { type: p.value.type, size: 8, sort: 'bestseller' } }).then(r => { suggested.value = r.data.content.filter(x => !relIds.has(x.id)).slice(0, 3) })
}
function add(buyNow) {
  const v = p.value
  cart.add({
    productId: v.id, name: v.name, fuel: v.fuel, type: v.type, baseColor: paintHex.value, wheelHex: wheel.value.hex, interiorHex: interior.value.hex,
    image: v.images?.[0], unitPrice: unitPrice.value, quantity: qty.value, stock: v.stock,
    options: canCustomize.value ? { ...sel } : {},
    optionLabel: canCustomize.value ? `${paint.value.name} · ${wheel.value.name} · Nội thất ${interior.value.name}` : ''
  })
  if (buyNow) router.push('/cart'); else { added.value = true; toast(`Đã thêm “${v.name}” vào giỏ hàng`) }
}
watch(() => route.params.id, id => id && load(), { immediate: true })
</script>

<style scoped>
.crumb { display: flex; gap: 8px; color: var(--muted); font-size: .9rem; margin-bottom: 24px; } .crumb a { color: var(--muted); } .crumb a:hover { color: #fff; }
.top { display: grid; grid-template-columns: minmax(0, 7fr) minmax(0, 5fr); gap: 48px; align-items: start; }
@media (max-width: 960px) { .top { grid-template-columns: 1fr; } }
.main-img { border-radius: 20px; overflow: hidden; background: #111318; border: 1px solid var(--border); aspect-ratio: 16/10; }
.photo { width: 100%; height: 100%; object-fit: cover; }
.design { height: 100%; display: flex; align-items: center; padding: 32px; background: radial-gradient(ellipse at 50% 70%, rgba(56,189,248,.12), transparent 60%); }
.thumbs { display: flex; gap: 12px; margin-top: 16px; }
.thumb { width: 120px; aspect-ratio: 16/10; border-radius: 12px; overflow: hidden; border: 2px solid transparent; background: #111318; cursor: pointer; padding: 0; opacity: .6; transition: .2s; color: var(--text); font: 600 .8rem inherit; font-family: inherit; }
.thumb img { width: 100%; height: 100%; object-fit: cover; } .thumb:hover { opacity: 1; } .thumb.on { border-color: var(--accent); opacity: 1; }
.thumb-design { border: 1px dashed rgba(255,255,255,.25); }
.keyspecs { display: grid; grid-template-columns: repeat(4, 1fr); margin-top: 32px; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.keyspecs div { padding: 24px 16px; } .keyspecs div + div { border-left: 1px solid var(--border); }
.keyspecs span { display: block; color: var(--muted); font-size: .8rem; text-transform: uppercase; letter-spacing: .08em; margin-bottom: 8px; }
.keyspecs b { font-size: 1.15rem; }
.buybox { position: sticky; top: 96px; }
.brandline { display: flex; align-items: center; gap: 8px; color: var(--muted); font-weight: 600; text-transform: uppercase; letter-spacing: .1em; font-size: .8rem; }
.brandline img { height: 20px; width: 20px; object-fit: contain; } .brandline .badge { margin-left: 8px; }
.title { font-size: 2.5rem; font-weight: 800; letter-spacing: -.03em; line-height: 1.1; margin: 16px 0; }
.chips { display: flex; gap: 8px; } .chips span { font-size: .85rem; padding: 4px 12px; border: 1px solid var(--border); border-radius: 999px; color: #cbd5e1; }
.pricebox { margin: 32px 0; padding: 24px 0; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.pricebox small { display: block; color: var(--muted); font-size: .85rem; margin: 4px 0; } .pricebox small b { color: #fff; }
.designer { margin-bottom: 32px; }
.opt { margin-bottom: 24px; }
.opt-head { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: .9rem; } .opt-head span { color: var(--muted); }
.opt-head em { font-style: normal; color: var(--accent); font-weight: 600; margin-left: 4px; }
.swatches { display: flex; gap: 12px; flex-wrap: wrap; }
.swatch { width: 40px; height: 40px; }
.stockline { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; font-size: .9rem; }
.ok { color: #86efac; } .out { color: #fca5a5; }
.actions { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.testdrive { display: inline-block; margin: 8px 0 24px; font-weight: 600; }
.perks { list-style: none; color: var(--muted); font-size: .9rem; } .perks li { padding: 8px 0 8px 24px; position: relative; }
.perks li::before { content: '✓'; position: absolute; left: 0; color: var(--accent); }
.info { display: grid; grid-template-columns: 1fr 1fr; gap: 64px; padding-top: 64px; border-top: 1px solid var(--border); }
@media (max-width: 860px) { .info { grid-template-columns: 1fr; } }
.info h2 { font-size: 1.75rem; letter-spacing: -.02em; margin-bottom: 16px; }
.desc { color: #cbd5e1; white-space: pre-line; font-size: 1.05rem; line-height: 1.7; }
.spec { display: grid; grid-template-columns: 1fr 1fr; margin-top: 16px; }
.spec dt, .spec dd { padding: 16px 0; border-bottom: 1px solid var(--border); } .spec dt { color: var(--muted); } .spec dd { font-weight: 600; text-align: right; }
.grid-3 { grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); }
.buybox { position: relative; }
.top { margin-bottom: 96px; }
.loan.section { margin-top: 0; }
.iconbtns { position: absolute; top: 0; right: 0; display: flex; gap: 8px; }
.iconbtns button { width: 44px; height: 44px; border-radius: 50%; border: 1px solid var(--border); background: rgba(255,255,255,.04); color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: .2s; }
.iconbtns button:hover { border-color: rgba(255,255,255,.3); } .iconbtns button.on { color: #f472b6; border-color: rgba(244,114,182,.4); } .iconbtns button:nth-child(2).on { color: var(--accent); border-color: rgba(56,189,248,.4); }
.ratingline { display: flex; align-items: center; gap: 8px; color: var(--muted); font-size: .9rem; margin: -4px 0 16px; } .ratingline b { color: #fff; }
.share { display: flex; align-items: center; gap: 8px; margin-top: 24px; padding-top: 24px; border-top: 1px solid var(--border); color: var(--muted); font-size: .85rem; }
.share span { margin-right: 8px; }
.share a, .share button { width: 36px; height: 36px; border-radius: 50%; border: 1px solid var(--border); background: none; color: #cbd5e1; display: flex; align-items: center; justify-content: center; font-weight: 700; cursor: pointer; font-family: inherit; }
.share a:hover, .share button:hover { color: #fff; border-color: var(--accent); }
.loan { display: grid; grid-template-columns: 1.3fr 1fr; gap: 48px; padding: 48px; border-radius: 24px; background: linear-gradient(135deg, rgba(2,132,199,.14), rgba(24,26,31,.8) 60%); border: 1px solid var(--border); }
@media (max-width: 860px) { .loan { grid-template-columns: 1fr; } }
.loan h2 { font-size: 1.75rem; letter-spacing: -.02em; margin-bottom: 8px; } .loan-l > p { margin-bottom: 32px; }
.sl { margin-bottom: 24px; } .sl-h { display: flex; justify-content: space-between; margin-bottom: 12px; } .sl-h span { color: var(--muted); }
.sl input { width: 100%; accent-color: var(--accent); height: 4px; }
.loan-r { background: rgba(8,9,12,.6); border: 1px solid var(--border); border-radius: 20px; padding: 32px; }
.loan-r small { color: var(--muted); } .monthly { font-size: 2.5rem; font-weight: 800; letter-spacing: -.03em; color: var(--accent); margin: 4px 0 24px; }
.loan-r dl { display: grid; grid-template-columns: 1fr auto; gap: 12px; margin-bottom: 24px; font-size: .92rem; } .loan-r dt { color: var(--muted); } .loan-r dd { font-weight: 600; text-align: right; }
.rv-grid { display: grid; grid-template-columns: 300px 1fr; gap: 64px; } @media (max-width: 860px) { .rv-grid { grid-template-columns: 1fr; } }
.rv-sum .big { font-size: 4rem; font-weight: 800; letter-spacing: -.04em; line-height: 1; margin-bottom: 8px; }
.rv-sum .muted { margin: 8px 0 24px; }
.dist { display: grid; grid-template-columns: 32px 1fr 24px; gap: 12px; align-items: center; font-size: .85rem; color: var(--muted); margin-bottom: 8px; }
.dist .bar { height: 6px; border-radius: 3px; background: rgba(255,255,255,.08); overflow: hidden; } .dist .bar div { height: 100%; background: #fbbf24; }
.rv-form { display: flex; flex-direction: column; gap: 12px; align-items: flex-start; padding: 24px; border: 1px solid var(--border); border-radius: 16px; margin-bottom: 32px; background: var(--card); }
.rv-login { margin-bottom: 24px; color: var(--muted); }
.rv { padding: 24px 0; border-bottom: 1px solid var(--border); }
.rv-h { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; } .rv-h > div { flex: 1; }
.av { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), #0c4a6e); display: flex; align-items: center; justify-content: center; font-weight: 700; }
.rv p { color: #cbd5e1; line-height: 1.7; }
.recent { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; }
.rc { display: flex; flex-direction: column; gap: 8px; color: var(--text); } .rc img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; transition: .3s; }
.rc:hover img { opacity: .8; } .rc b { font-size: .9rem; font-weight: 600; } .rc .price { font-size: .9rem; }
</style>
