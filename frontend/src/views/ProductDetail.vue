<template>
  <div class="container page pd">
    <div v-if="loadErr" class="nf"><h1>Không tìm thấy sản phẩm</h1><p class="muted">{{ loadErr }}</p><router-link to="/products" class="btn">← Xem sản phẩm khác</router-link></div>
    <div v-else-if="!p" class="pdsk"><div class="sk" /><div><div class="sk" style="height:28px;width:70%" /><div class="sk" style="height:18px;width:40%" /><div class="sk" style="height:120px" /></div></div>
    <template v-else>
      <Breadcrumb :items="[{ label: isAcc(p) ? 'Phụ kiện' : 'Danh mục xe', to: { path: '/products', query: { grp: p.grp } } }, { label: TYPES[p.type], to: { path: '/products', query: { type: p.type } } }, { label: p.name }]" />

      <div ref="gal" class="carousel">
        <transition name="fade" mode="out-in"><img fetchpriority="high" :key="curImg" :src="curImg" :alt="p.name" class="photo" /></transition>
        <button v-if="imgs.length > 1" class="arr l" aria-label="Ảnh trước" @click="step(-1)">‹</button>
        <button v-if="imgs.length > 1" class="arr r" aria-label="Ảnh sau" @click="step(1)">›</button>
        <span v-if="fs" class="flashtag">⚡ Flash sale · còn <Countdown :to="fs.endTime" /><template v-if="fs.quota"> · {{ fs.quota - fs.sold }} suất</template></span>
        <div class="mediabtns"><button type="button" @click="v360 = '360'">⟳ 360°</button><button v-if="model3d" type="button" @click="v360 = '3d'">◈ 3D · AR</button><button type="button" @click="v360 = 'video'">▶ Video</button></div>
        <div class="iconbtns">
          <button :class="['wish', { on: prefs.isWished(p.id) }]" :aria-pressed="prefs.isWished(p.id)" title="Yêu thích" @click="prefs.toggleWish(p)"><svg viewBox="0 0 24 24" width="18" height="18" :fill="prefs.isWished(p.id) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z"/></svg></button>
          <button :class="{ on: prefs.inCompare(p.id) }" title="So sánh" @click="prefs.toggleCompare(p)"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M8 21H3v-5M21 3l-7 7M3 21l7-7"/></svg></button>
          <button title="Chia sẻ" @click="share"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4"/></svg></button>
        </div>
      </div>
      <div class="thumbs"><button v-for="(im, i) in imgs" :key="im + i" :class="{ on: im === curImg }" @click="view = i"><img loading="lazy" decoding="async" :src="im" alt="" /></button></div>

      <div class="head">
        <div>
          <div class="brandline"><img loading="lazy" decoding="async" v-if="p.brand?.logoUrl" :src="p.brand.logoUrl" alt="" />{{ p.brand?.name }} · {{ TYPES[p.type] }}<span v-if="p.scale"> · Tỷ lệ {{ p.scale }} <button type="button" class="lnk" @click="guide = true">Hướng dẫn tỷ lệ</button></span> · {{ p.ageMin }}+ tuổi<span v-if="p.isNew" class="badge tag-yellow">Mới</span></div>
          <h1 class="title">{{ p.name }}</h1>
          <a v-if="rv.count" href="#reviews" class="ratingline"><Stars :value="rv.avg" /><b>{{ rv.avg }}</b><span>{{ rv.count }} đánh giá · Đã bán {{ p.sold }} · SKU {{ p.sku }}</span></a>
        </div>
        <div class="pricecol">
          <span v-if="oldPrice" class="old-price">{{ vnd(oldPrice) }}</span>
          <div class="price-lg">{{ vnd(unitPrice) }}</div>
          <small>{{ unitPrice >= catalog.options.freeShipFrom ? 'Miễn phí vận chuyển' : `Freeship cho đơn từ ${vnd(catalog.options.freeShipFrom)}` }}</small>
        </div>
      </div>

      <div v-if="variants.length" class="variants">
        <span class="vl">{{ variants.some(v => v.color) ? 'Màu sắc' : 'Phiên bản' }}: <b>{{ variant?.name || 'Chọn' }}</b></span>
        <div class="vlist">
          <button v-for="v in variants" :key="v.id" :class="['vbtn', { on: variant?.id === v.id, out: v.stock <= 0 }]" :disabled="v.stock <= 0" @click="pickVariant(v)">
            <i v-if="v.color" :style="{ background: v.color }" /><span>{{ v.name }}</span><small>{{ vnd(v.price) }} · {{ v.stock > 0 ? `còn ${v.stock}` : 'hết' }}</small>
          </button>
        </div>
      </div>

      <div v-if="p.preorder" class="notify pre">
        <b>Mẫu sắp ra mắt — mở đặt trước</b><span>Dự kiến phát hành {{ d(p.releaseDate) }}<template v-if="p.preorderDeposit"> · cọc {{ vnd(p.preorderDeposit) }} khi hàng về</template>.</span>
        <router-link to="/upcoming" class="btn btn-sm" style="align-self:flex-start">Đặt trước ngay</router-link>
      </div>
      <div v-else-if="!stock" class="notify">
        <b>Tạm hết hàng</b><span>Để lại email, chúng tôi báo ngay khi có hàng.</span>
        <form class="row" @submit.prevent="notifyMe"><input v-model.trim="nEmail" type="email" class="input grow" :placeholder="auth.user?.email || 'Email của bạn'" aria-label="Email nhận thông báo" /><button class="btn btn-sm" :disabled="nDone">{{ nDone ? '✓ Đã đăng ký' : 'Báo tôi' }}</button></form>
      </div>

      <PriceWatch v-if="!p.preorder" :product-id="p.id" :price="unitPrice" />
      <SubscribeBox v-if="!p.preorder" :product-id="p.id" :variant-id="variantId" :qty="qty" :price="unitPrice" :disabled="!stock || (variants.length > 0 && !variantId)" />
      <div class="perks">
        <div><b>🚚 Giao dự kiến {{ etaLabel }}</b><span>{{ etaCity }} · hoả tốc nhận sớm hơn 1 ngày</span></div>
        <div><b>↩ Đổi trả 7 ngày</b><span>Lỗi do nhà sản xuất đổi mới miễn phí</span></div>
        <div><b>🛡 Bảo hành</b><span>{{ p.warrantyMonths ? p.warrantyMonths + ' tháng chính hãng' : 'Đổi mới 7 ngày nếu lỗi' }}</span></div>
        <div v-if="unitPrice >= catalog.options.installmentMin"><b>💳 Trả góp 0%</b><span>3–12 tháng qua thẻ tín dụng · từ {{ vnd(Math.ceil(unitPrice / 12)) }}/tháng</span></div>
        <div v-else><b>⭐ Tích điểm</b><span>+{{ Math.floor(unitPrice / catalog.options.earnPer) }} điểm khi mua sản phẩm này</span></div>
      </div>

      <p class="desc">{{ p.description }}</p>

      <section v-if="pBundles.length" class="pbundles"><h3>Mua kèm tiết kiệm hơn</h3><div class="bgrid"><BundleCard v-for="b in pBundles" :key="b.bundle.id" :b="b" /></div></section>

      <div class="specs">
        <button class="specs-h" @click="specsOpen = !specsOpen"><span>Thông số kỹ thuật</span><i :class="{ open: specsOpen }">⌄</i></button>
        <transition name="collapse"><dl v-show="specsOpen" class="spec"><template v-for="(v, k) in p.specs" :key="k"><dt>{{ k }}</dt><dd>{{ v }}</dd></template></dl></transition>
      </div>

      <section id="reviews" v-reveal class="panel">
        <div class="ph"><div class="rv-title"><h2>Đánh giá</h2><span v-if="rv.count" class="rate-pill">★ {{ rv.avg }} · {{ rv.count }}</span></div>
          <div class="filters"><button v-for="f in ['all', 'photo', 5, 4, 3]" :key="f" :class="['fchip', { on: rf === f }]" @click="rf = f">{{ f === 'all' ? 'Tất cả' : f === 'photo' ? 'Có ảnh' : f + '★' }}</button>
            <button v-if="filtered.length > 3" class="link" @click="showAll = !showAll">{{ showAll ? 'Thu gọn' : 'Xem tất cả →' }}</button></div></div>
        <div v-if="!filtered.length" class="muted">Chưa có đánh giá phù hợp.</div>
        <div :class="showAll ? 'rv-list' : 'rv-row'">
          <div v-for="r in filtered.slice(0, showAll ? 99 : 3)" :key="r.id" class="rv">
            <div class="rv-h"><span class="av">{{ r.authorName?.split(' ').pop()[0] }}</span><div><b>{{ r.authorName }}</b><div class="muted small">{{ d(r.createdAt) }}<span v-if="r.verified" class="vb"> · ✓ Đã mua hàng</span><span v-if="r.variantName"> · {{ r.variantName }}</span></div></div><span class="rate-pill sm">★ {{ r.rating }}</span></div>
            <p>{{ r.comment }}</p>
            <div v-if="r.images?.length" class="rimgs"><a v-for="im in r.images" :key="im" :href="im" target="_blank"><img loading="lazy" decoding="async" :src="im" alt="" /></a></div>
            <button type="button" :class="['helpful', { on: voted[r.id] }]" :aria-pressed="!!voted[r.id]" @click="helpful(r)">👍 Hữu ích<span v-if="r.helpful"> ({{ r.helpful }})</span></button>
          </div>
        </div>
        <form v-if="auth.loggedIn && !rv.mine" id="write-review" class="rv-form" @submit.prevent="sendReview">
          <div class="rf1"><b>Viết đánh giá</b><Stars v-model:value="mine.rating" pick size="1.4rem" /></div>
          <textarea v-model="mine.comment" class="input" rows="2" placeholder="Chia sẻ cảm nhận về sản phẩm…" />
          <div class="rf2">
            <label class="up">📷 Thêm ảnh<input type="file" accept="image/*" multiple hidden @change="upload" /></label>
            <img loading="lazy" decoding="async" v-for="(im, i) in mine.images" :key="im" :src="im" class="thumb-up" :title="'Xoá ảnh'" @click="mine.images.splice(i, 1)" />
            <span v-if="uploading" class="muted small">Đang tải ảnh…</span>
            <button class="btn btn-sm" :disabled="sending">Gửi đánh giá</button>
          </div>
        </form>
        <div v-else-if="!auth.loggedIn" class="rv-login"><router-link :to="{ path: '/login', query: { redirect: $route.fullPath } }">Đăng nhập</router-link> để viết đánh giá.</div>
      </section>

      <section id="qa" v-reveal class="panel">
        <div class="ph"><h2>Hỏi đáp ({{ qa.length }})</h2></div>
        <form v-if="auth.loggedIn" class="qa-form" @submit.prevent="ask"><input v-model="question" class="input" placeholder="Bạn có thắc mắc về sản phẩm? Đặt câu hỏi cho shop…" /><button class="btn btn-sm" :disabled="question.trim().length < 5">Gửi câu hỏi</button></form>
        <div v-else class="rv-login"><router-link :to="{ path: '/login', query: { redirect: $route.fullPath } }">Đăng nhập</router-link> để đặt câu hỏi.</div>
        <div v-for="q in qa" :key="q.id" class="qa">
          <div class="q"><b>Hỏi</b><span>{{ q.question }}</span><small>{{ q.authorName }} · {{ d(q.createdAt) }}</small></div>
          <div v-if="q.answer" class="a"><b>Đáp</b><span>{{ q.answer }}</span><small>{{ q.answeredBy }} · {{ d(q.answeredAt) }}</small></div>
          <div v-else class="a pending">Shop sẽ trả lời sớm.</div>
        </div>
      </section>

      <router-link v-reveal to="/promotions" class="promo"><img loading="lazy" decoding="async" :src="imgs[1] || imgs[0]" alt="" /><div><small>Voucher tháng này</small><b>Freeship từ 199K · Giảm 50K đơn từ 500K</b><span>Xem tất cả mã giảm giá →</span></div></router-link>

      <section v-if="!isAcc(p) && imgs.length" v-reveal class="feature">
        <img loading="lazy" decoding="async" :src="imgs[imgs.length - 1]" alt="" />
        <div><span class="eyebrow">Điểm nổi bật</span><h2>{{ p.scale ? 'Tỷ lệ ' + p.scale : p.name }} · {{ p.material }}</h2>
          <p class="muted">{{ p.description }}</p>
          <router-link to="/support" class="btn btn-outline btn-sm">Cần tư vấn? Chat với ARES →</router-link></div>
      </section>

      <section v-if="also.length" class="block">
        <div class="ph"><h2>Thường được mua cùng</h2></div>
        <div v-stagger class="grid grid-4"><ProductCard v-for="r in also" :key="r.id" :p="r" /></div>
      </section>
      <section v-if="likes.length" class="block">
        <div class="ph"><h2>Có thể bạn thích</h2><router-link :to="{ path: '/products', query: { type: p.type } }" class="link">Xem tất cả →</router-link></div>
        <div class="grid grid-4"><ProductCard v-for="(r, i) in likes" :key="r.id" v-reveal :p="r" /></div>
      </section>
      <section v-if="recent.length" class="block">
        <div class="ph"><h2>Bạn đã xem gần đây</h2></div>
        <div class="recent"><router-link v-for="r in recent" :key="r.id" :to="`/products/${r.id}`" class="rc"><img loading="lazy" decoding="async" :src="r.images?.[0]" alt="" /><b>{{ r.name }}</b><span>{{ vnd(r.price) }}</span></router-link></div>
      </section>

      <div class="buybar">
        <div class="bb-in">
          <div class="bb-l">
            <img loading="lazy" decoding="async" :src="curImg" alt="" />
            <div><b>{{ p.name }}</b>
              <select v-if="variants.length" v-model="variantId" class="input mode">
                <option v-for="v in variants" :key="v.id" :value="v.id" :disabled="v.stock <= 0">{{ v.name }} — {{ vnd(v.price) }}{{ v.stock <= 0 ? ' (hết)' : '' }}</option>
              </select>
              <small v-else :class="stock ? 'ok' : 'out'">{{ stock ? `Còn ${stock} sản phẩm` : 'Tạm hết hàng' }}</small>
            </div>
          </div>
          <div class="qty"><button @click="qty = Math.max(1, qty - 1)">−</button><span>{{ qty }}</span><button @click="qty = Math.min(stock, qty + 1)">+</button></div>
          <div class="bb-price"><small v-if="fs">Giá flash sale</small><b>{{ vnd(unitPrice * qty) }}</b></div>
          <button class="btn btn-outline" :disabled="!stock" @click="add(false)">Thêm vào giỏ</button>
          <BuyNowButton :disabled="!stock" :before="canBuy" :after="buyNow" />
        </div>
      </div>
      <ScaleGuide v-if="guide" :current="p.scale" @close="guide = false" />
      <Viewer360 v-if="v360" :key="v360" :images="imgs" :name="p.name" :video="p.videoUrl" :model="model3d" :color="p.baseColor" :placeholder="!p.model3d" :start="v360" @close="v360 = ''" />
    </template>
  </div>
</template>

<script setup>
import PriceWatch from '../components/PriceWatch.vue'
import SubscribeBox from '../components/SubscribeBox.vue'
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { uploadImage, errMsg } from '../api'
import { vnd, d, TYPES, catalog, loadCatalog, isAcc } from '../format'
import Breadcrumb from '../components/Breadcrumb.vue'
import Stars from '../components/Stars.vue'
import Countdown from '../components/Countdown.vue'
import { usePrefs, toast } from '../stores/prefs'
import { useAuth } from '../stores/auth'
import { useCart } from '../stores/cart'
import ProductCard from '../components/ProductCard.vue'
import { flyToCart } from '../motion'
import BuyNowButton from '../components/BuyNowButton.vue'
import BundleCard from '../components/BundleCard.vue'
import ScaleGuide from '../components/ScaleGuide.vue'
import Viewer360 from '../components/Viewer360.vue'

const route = useRoute(), router = useRouter(), cart = useCart(), prefs = usePrefs(), auth = useAuth()
const p = ref(null), related = ref([]), variants = ref([]), flashes = ref([]), suggested = ref([]), qa = ref([]), gal = ref(null)
const rv = ref({}), mine = reactive({ rating: 5, comment: '', images: [] }), sending = ref(false), uploading = ref(false), showAll = ref(false), rf = ref('all')
const model3d = computed(() => p.value?.model3d || (p.value?.type === 'RC' ? '/models/rc-truck.glb' : ['MODEL', 'KIT', 'DISPLAY'].includes(p.value?.type) ? '/models/car.glb' : ''))
const guide = ref(false), v360 = ref(''), pBundles = ref([]), nEmail = ref(''), nDone = ref(false), eta = ref(null)
const voted = reactive(JSON.parse(localStorage.getItem('helpfulVotes') || '{}'))
const etaCity = computed(() => auth.user?.city || localStorage.getItem('etaCity') || 'TP. Hồ Chí Minh')
const etaLabel = computed(() => { if (!eta.value) return ''; const f = x => new Date(x + 'T00:00:00').toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' }); return eta.value.etaFrom === eta.value.etaTo ? f(eta.value.etaFrom) : `${f(eta.value.etaFrom)} – ${f(eta.value.etaTo)}` })
async function notifyMe() {
  try { const { data } = await api.post('/public/notify-me', { productId: p.value.id, email: nEmail.value }); nDone.value = true; toast(`Sẽ báo về ${data.email} khi có hàng`) } catch (e) { toast(errMsg(e), 'error') }
}
async function helpful(r) {
  if (!auth.loggedIn) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  try { const { data } = await api.post(`/reviews/${r.id}/helpful`); r.helpful = data.helpful; voted[r.id] = data.voted; if (!data.voted) delete voted[r.id]; localStorage.setItem('helpfulVotes', JSON.stringify(voted)) } catch (e) { toast(errMsg(e), 'error') }
}
const qty = ref(1), view = ref(0), specsOpen = ref(false), variantId = ref(null), question = ref('')
const variant = computed(() => variants.value.find(v => v.id === variantId.value))
const imgs = computed(() => { const l = [...(p.value?.images || [])]; variants.value.forEach(v => v.image && !l.includes(v.image) && l.push(v.image)); return l })
const curImg = computed(() => imgs.value[view.value] || imgs.value[0])
const fs = computed(() => flashes.value.find(f => !f.variantId || f.variantId === variantId.value))
const stock = computed(() => variant.value ? variant.value.stock : p.value?.stock || 0)
const basePrice = computed(() => variant.value ? variant.value.price : p.value.price)
const unitPrice = computed(() => fs.value ? fs.value.salePrice : basePrice.value)
const oldPrice = computed(() => fs.value ? basePrice.value : (variant.value ? variant.value.oldPrice : p.value.oldPrice))
const filtered = computed(() => (rv.value.items || []).filter(r => rf.value === 'all' || (rf.value === 'photo' ? r.images?.length : r.rating === rf.value)))
const also = ref([])
const likes = computed(() => [...suggested.value, ...related.value].filter(x => !also.value.some(a => a.id === x.id)).filter((x, i, a) => a.findIndex(y => y.id === x.id) === i).slice(0, 4))
const recent = computed(() => prefs.recent.filter(r => r.id !== p.value?.id).slice(0, 6))
const step = k => { view.value = (view.value + k + imgs.value.length) % imgs.value.length }
const pickVariant = v => { variantId.value = v.id; qty.value = 1; if (v.image) view.value = imgs.value.indexOf(v.image) }
const share = async () => { const url = location.origin + route.fullPath; if (navigator.share) { try { await navigator.share({ title: p.value.name, url }) } catch {} } else { navigator.clipboard?.writeText(url); toast('Đã sao chép liên kết') } }
const loadReviews = async id => { rv.value = (await api.get(`/products/${id}/reviews`)).data }
const loadQa = async id => { qa.value = (await api.get(`/products/${id}/questions`)).data }

async function upload(e) {
  uploading.value = true
  try { for (const f of [...e.target.files].slice(0, 5 - mine.images.length)) mine.images.push(await uploadImage(f)) }
  catch (err) { toast(errMsg(err), 'error') } finally { uploading.value = false; e.target.value = '' }
}
async function sendReview() {
  sending.value = true
  try { await api.post(`/products/${p.value.id}/reviews`, { ...mine, variantName: variant.value?.name }); toast('Cảm ơn bạn đã đánh giá!'); Object.assign(mine, { comment: '', images: [] }); await loadReviews(p.value.id) }
  catch (e) { toast(errMsg(e), 'error') } finally { sending.value = false }
}
async function ask() {
  try { await api.post(`/products/${p.value.id}/questions`, { question: question.value }); question.value = ''; toast('Đã gửi câu hỏi, shop sẽ trả lời sớm'); loadQa(p.value.id) }
  catch (e) { toast(errMsg(e), 'error') }
}
const loadErr = ref('')
async function load() {
  p.value = null; qty.value = 1; view.value = 0; specsOpen.value = false; variantId.value = null; rf.value = 'all'; showAll.value = false
  loadCatalog()
  loadErr.value = ''
  let data; try { data = (await api.get(`/products/${route.params.id}`)).data } catch (e) { loadErr.value = errMsg(e); return }
  p.value = data.product; related.value = data.related; variants.value = data.variants; flashes.value = data.flash
  variantId.value = data.variants.find(v => v.stock > 0)?.id || data.variants[0]?.id || null
  if (route.hash) nextTick(() => setTimeout(() => document.querySelector(route.hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 300))
  prefs.viewed(p.value); loadReviews(p.value.id); loadQa(p.value.id); nDone.value = false
  api.get(`/products/${p.value.id}/also-bought`).then(r => { also.value = r.data }).catch(() => { also.value = [] })
  api.get('/public/bundles', { params: { productId: p.value.id } }).then(r => { pBundles.value = r.data }).catch(() => {})
  api.get('/public/shipping-options', { params: { city: etaCity.value, amount: 0 } }).then(r => { eta.value = r.data[0] }).catch(() => {})
  const relIds = new Set([p.value.id, ...related.value.map(r => r.id)])
  api.get('/products', { params: { type: p.value.type, size: 8, sort: 'bestseller' } }).then(r => { suggested.value = r.data.content.filter(x => !relIds.has(x.id)).slice(0, 4) })
}
function add(buyNow, silent) {
  const v = p.value
  if (variants.value.length && !variant.value) return toast('Vui lòng chọn phiên bản', 'error')
  cart.add({ productId: v.id, variantId: variant.value?.id || null, variantName: variant.value?.name || '', name: v.name, image: variant.value?.image || v.images?.[0],
    unitPrice: unitPrice.value, oldPrice: oldPrice.value, quantity: qty.value, stock: stock.value, flash: !!fs.value })
  if (buyNow) { if (!silent) router.push('/cart') }
  else { flyToCart(curImg.value, gal.value); toast(`Đã thêm “${v.name}” vào giỏ hàng`) }
}
const canBuy = () => { if (variants.value.length && !variant.value) { toast('Vui lòng chọn phiên bản', 'error'); return false } }
const buyNow = async () => { add(true, true); await router.push('/checkout') }
watch(() => route.params.id, id => id && load(), { immediate: true })
</script>

<style scoped>
.mediabtns { position: absolute; left: 12px; bottom: 12px; display: flex; gap: 6px; z-index: 2; }
.mediabtns button { background: rgba(17,17,17,.82); color: #fff; border: none; padding: 6px 10px; font: 600 .76rem inherit; font-family: inherit; cursor: pointer; }
.mediabtns button:hover { background: var(--accent); }
.lnk { background: none; border: none; padding: 0; color: var(--accent); font: inherit; text-decoration: underline; cursor: pointer; }
.notify { border: 1.5px dashed var(--accent); padding: 12px 14px; margin: 12px 0; display: flex; flex-direction: column; gap: 6px; } .notify span { font-size: .86rem; color: var(--muted); }
.notify .row { gap: 8px; flex-wrap: nowrap; } .notify .input { min-width: 0; }
.pbundles { margin: 18px 0; } .pbundles h3 { margin-bottom: 10px; } .bgrid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 280px), 1fr)); gap: 12px; }
.helpful { margin-top: 8px; background: none; border: 1px solid var(--border); padding: 4px 10px; font: 500 .78rem inherit; font-family: inherit; cursor: pointer; color: var(--text-2, var(--text)); }
.helpful.on { border-color: var(--accent); color: var(--accent); background: var(--bronze-soft); }

.pd { padding-bottom: 120px; }
.carousel { position: relative; height: 500px; border-radius: 12px; overflow: hidden; background: var(--tile); }
.photo { width: 100%; height: 100%; object-fit: contain; background: #f1f1f1; }
.fade-enter-active { transition: opacity .15s ease-out; } .fade-enter-from { opacity: 0; }
.arr { position: absolute; top: 50%; transform: translateY(-50%); width: 44px; height: 44px; border-radius: 50%; border: none; background: rgba(255,255,255,.9); font-size: 1.8rem; line-height: 1; cursor: pointer; color: #333; transition: .2s; }
.arr.l { left: 16px; } .arr.r { right: 16px; } .arr:hover { background: #fff; }
.flashtag { position: absolute; left: 14px; top: 14px; background: #c62828; color: #fff; font-weight: 700; font-size: .82rem; padding: 6px 12px; border-radius: 6px; }
.iconbtns { position: absolute; top: 14px; right: 14px; display: flex; gap: 8px; }
.iconbtns button { width: 38px; height: 38px; border-radius: 50%; border: none; background: rgba(255,255,255,.92); color: #333; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.iconbtns button.on { color: #e53935; } .iconbtns button:nth-child(2).on { color: var(--bronze); }
.thumbs { display: flex; gap: 8px; justify-content: center; margin: 12px 0 18px; }
.thumbs button { width: 64px; height: 44px; border-radius: 6px; overflow: hidden; border: 2px solid transparent; padding: 0; cursor: pointer; opacity: .6; transition: .2s; background: none; }
.thumbs button.on { border-color: var(--bronze); opacity: 1; } .thumbs img { width: 100%; height: 100%; object-fit: cover; }
.head { display: flex; justify-content: space-between; gap: 24px; align-items: flex-start; }
.brandline { display: flex; align-items: center; gap: 6px; color: var(--muted); font-size: .8rem; font-weight: 600; text-transform: uppercase; letter-spacing: .06em; flex-wrap: wrap; }
.brandline img { height: 20px; width: 20px; border-radius: 4px; } .brandline .badge { margin-left: 6px; }
.title { font-family: var(--head); font-size: 2rem; line-height: 1.2; margin: 6px 0; }
.ratingline { display: flex; align-items: center; gap: 8px; color: var(--muted); font-size: .88rem; } .ratingline b { color: var(--text); }
.pricecol { text-align: right; flex-shrink: 0; } .pricecol .old-price { margin: 0; } .pricecol .price-lg { color: var(--text); } .pricecol small { color: var(--success); font-weight: 600; font-size: .82rem; }
.variants { margin-top: 16px; } .vl { font-size: .9rem; color: var(--muted); } .vl b { color: var(--text); }
.vlist { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.vbtn { display: grid; grid-template-columns: auto 1fr; column-gap: 8px; align-items: center; text-align: left; padding: 8px 12px; border: 1.5px solid var(--border); background: #fff; border-radius: 8px; cursor: pointer; font-family: inherit; transition: .2s; }
.vbtn i { grid-row: span 2; width: 22px; height: 22px; border-radius: 50%; box-shadow: inset 0 0 0 1px rgba(0,0,0,.2); }
.vbtn span { font-weight: 600; font-size: .86rem; } .vbtn small { color: var(--muted); font-size: .74rem; }
.vbtn.on { border-color: var(--bronze); background: var(--bronze-soft); } .vbtn.out { opacity: .45; cursor: not-allowed; }
.perks { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 18px; }
.perks div { background: var(--bg-2); border-radius: 10px; padding: 12px 14px; } .perks b { display: block; font-size: .88rem; } .perks span { font-size: .78rem; color: var(--muted); }
.desc { color: var(--text-2); white-space: pre-line; line-height: 1.7; margin: 18px 0; max-width: 880px; }
.specs { margin-top: 8px; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }
.specs-h { width: 100%; display: flex; justify-content: space-between; align-items: center; padding: 16px 0; background: none; border: none; cursor: pointer; font: 600 1.05rem var(--head); color: var(--text); }
.specs-h i { font-style: normal; transition: transform .18s ease-out; font-size: 1.3rem; } .specs-h i.open { transform: rotate(180deg); }
.spec { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; column-gap: 32px; padding-bottom: 12px; overflow: hidden; }
.collapse-enter-active, .collapse-leave-active { transition: opacity .3s, transform .3s; } .collapse-enter-from, .collapse-leave-to { opacity: 0; transform: translateY(-8px); }
.spec dt, .spec dd { padding: 10px 0; border-bottom: 1px dashed var(--border); font-size: .9rem; } .spec dt { color: var(--muted); } .spec dd { font-weight: 600; text-align: right; }
.panel { margin-top: 24px; background: var(--bg-2); border-radius: 12px; padding: 20px 22px; }
.ph { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 12px; flex-wrap: wrap; } .ph h2 { font-size: 1.2rem; }
.rv-title { display: flex; gap: 10px; align-items: center; }
.filters { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.fchip { border: 1px solid var(--border); background: #fff; border-radius: 999px; padding: 4px 12px; font: inherit; font-size: .8rem; cursor: pointer; } .fchip.on { border-color: var(--bronze); color: var(--bronze); font-weight: 600; }
.link { background: none; border: none; color: var(--muted); cursor: pointer; font: inherit; font-size: .88rem; } .link:hover { color: var(--bronze); }
.rate-pill { background: #fff; border-radius: 999px; padding: 3px 10px; font-size: .82rem; font-weight: 700; color: #b58a00; } .rate-pill.sm { font-size: .75rem; }
.rv-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; } .rv-list { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.rv { background: #fff; border-radius: 10px; padding: 14px 16px; }
.rv-h { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; } .rv-h > div { flex: 1; } .vb { color: var(--success); }
.av { width: 34px; height: 34px; border-radius: 50%; background: var(--bronze); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; flex-shrink: 0; }
.rv p { color: var(--text-2); font-size: .9rem; line-height: 1.6; }
.rimgs { display: flex; gap: 6px; margin-top: 8px; } .rimgs img { width: 64px; height: 64px; object-fit: cover; border-radius: 6px; }
.rv-form { margin-top: 14px; background: #fff; border-radius: 10px; padding: 14px; display: flex; flex-direction: column; gap: 10px; }
.rf1, .rf2 { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; } .rf2 .btn { margin-left: auto; }
.up { border: 1px dashed var(--bronze); color: var(--bronze); border-radius: 8px; padding: 6px 12px; cursor: pointer; font-size: .85rem; }
.thumb-up { width: 44px; height: 44px; object-fit: cover; border-radius: 6px; cursor: pointer; }
.rv-login { margin-top: 12px; color: var(--muted); font-size: .9rem; }
.qa-form { display: flex; gap: 10px; margin-bottom: 12px; } .qa-form .input { flex: 1; padding: 8px 12px; }
.qa { background: #fff; border-radius: 10px; padding: 12px 16px; margin-top: 8px; }
.q, .a { display: grid; grid-template-columns: 44px 1fr; column-gap: 8px; font-size: .9rem; } .q b { color: var(--bronze); } .a { margin-top: 8px; padding-top: 8px; border-top: 1px dashed var(--border); } .a b { color: var(--success); }
.q small, .a small { grid-column: 2; color: var(--muted); font-size: .75rem; } .a.pending { display: block; color: var(--muted); font-size: .82rem; }
.promo { display: grid; grid-template-columns: 1fr 1fr; margin-top: 24px; min-height: 170px; border-radius: 12px; overflow: hidden; background: #13235a; color: #fff; }
.promo img { width: 100%; height: 100%; object-fit: cover; } .promo div { padding: 26px 30px; display: flex; flex-direction: column; justify-content: center; gap: 6px; }
.promo small { color: #f6c445; text-transform: uppercase; letter-spacing: .12em; font-size: .75rem; } .promo b { font-family: var(--head); font-size: 1.35rem; line-height: 1.25; } .promo span { opacity: .8; font-size: .9rem; }
.feature { display: grid; grid-template-columns: 1.2fr 1fr; gap: 36px; align-items: center; margin-top: 28px; }
.feature img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; }
.feature h2 { font-family: var(--head); font-size: 1.5rem; margin: 6px 0 10px; } .feature p { margin-bottom: 18px; line-height: 1.7; }
.block { margin-top: 32px; }
.recent { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.rc { display: flex; flex-direction: column; gap: 4px; color: var(--text); font-size: .82rem; } .rc img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 8px; } .rc b { font-weight: 500; } .rc span { color: var(--muted); }
.buybar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 60; background: #fff; box-shadow: 0 -6px 24px rgba(0,0,0,.1); border-top: 1px solid var(--border); animation: up .2s ease-out; }
@keyframes up { from { transform: translateY(100%); } }
.bb-in { max-width: 1180px; margin: 0 auto; padding: 10px 24px; display: flex; align-items: center; gap: 14px; }
.bb-l { display: flex; gap: 12px; align-items: center; flex: 1; min-width: 0; }
.bb-l img { width: 72px; height: 48px; object-fit: cover; border-radius: 6px; } .bb-l b { display: block; font-size: .9rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mode { padding: 5px 10px; font-size: .82rem; margin-top: 3px; width: 300px; }
.ok { color: var(--success); } .out { color: var(--danger); }
.qty { display: flex; align-items: center; border: 1px solid var(--border); border-radius: 8px; } .qty button { width: 30px; height: 34px; border: none; background: none; cursor: pointer; font-size: 1rem; } .qty span { width: 26px; text-align: center; }
.bb-price { text-align: right; } .bb-price small { display: block; color: var(--danger); font-size: .72rem; font-weight: 600; } .bb-price b { font-size: 1.15rem; white-space: nowrap; }
@media (max-width: 899px) {
  .buybar { bottom: var(--tabbar-h, 0px); }
  .bb-in { display: grid !important; grid-template-columns: auto 1fr 1fr; gap: 8px; padding: 8px 12px; flex-wrap: nowrap; }
  .bb-l { grid-column: 1 / -1; display: flex; gap: 8px; align-items: center; min-width: 0; } .bb-l img { width: 44px; height: 32px; }
  .bb-l b { display: block; font-size: .82rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 62vw; }
  .bb-l .mode { min-height: 36px; font-size: 14px; padding: 4px 8px; }
  .bb-price { display: none; } .bb-in .btn { padding: 0 10px; font-size: .8rem; }
}
@media (max-width: 860px) { .carousel { height: 300px; } .head, .bb-in { flex-wrap: wrap; } .rv-row, .spec, .feature, .promo, .perks { grid-template-columns: 1fr; } .recent { grid-template-columns: repeat(3, 1fr); } .mode { width: 100%; } }
.nf { text-align: center; padding: 60px 0; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.pdsk { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr); gap: 32px; } .pdsk > .sk { aspect-ratio: 4/3; } .pdsk > div:last-child { display: flex; flex-direction: column; gap: 12px; } @media (max-width: 760px) { .pdsk { grid-template-columns: 1fr; } }
#write-review { scroll-margin-top: 100px; }
</style>
