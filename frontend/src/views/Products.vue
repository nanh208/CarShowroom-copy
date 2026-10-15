<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: title }]" />
    <div class="filters">
    <div class="fbar">
      <div class="chips">
        <button :class="['chip', { on: !f.type }]" @click="setType('')">Tất cả</button>
        <button v-for="c in shownCats" :key="c.code" :class="['chip', { on: f.type === c.code }]" @click="setType(c.code)">{{ c.name }}</button>
      </div>
      <button class="btn btn-sm btn-outline fbtn" type="button" :aria-expanded="sheet" aria-controls="fsheet" @click="sheet = true">Bộ lọc<b v-if="activeCount" class="fcount">{{ activeCount }}</b></button>
      <select v-model="f.sort" class="input sort" aria-label="Sắp xếp" @change="apply()">
        <option value="featured">Sắp xếp: Nổi bật</option><option value="newest">Mới nhất</option><option value="bestseller">Bán chạy</option>
        <option value="rating">Đánh giá cao</option><option value="price_asc">Giá tăng dần</option><option value="price_desc">Giá giảm dần</option><option value="name">Tên A–Z</option>
      </select>
    </div>
    <transition name="sh-fade"><div v-if="sheet" class="sh-scrim" @click="sheet = false" /></transition>
    <div id="fsheet" :class="['fbar2', { open: sheet }]" :role="sheet ? 'dialog' : null" :aria-modal="sheet ? 'true' : null" aria-label="Bộ lọc sản phẩm">
      <div class="sh-head"><b>Bộ lọc</b><button type="button" class="sh-x" aria-label="Đóng bộ lọc" @click="sheet = false">✕</button></div>
      <input v-model="f.q" class="input" type="search" aria-label="Tìm sản phẩm" placeholder="Tìm tên, hãng, mã SKU..." @input="typed" @keyup.enter="apply()" />
      <select v-model="f.brandId" class="input" @change="apply()"><option value="">Mọi thương hiệu</option><option v-for="b in catalog.brands" :key="b.id" :value="String(b.id)">{{ b.name }}</option></select>
      <select v-model="price" class="input" @change="apply()">
        <option value="">Mọi mức giá</option><option value="0-300000">Dưới 300K</option><option value="300000-1000000">300K – 1 triệu</option>
        <option value="1000000-5000000">1 – 5 triệu</option><option value="5000000-15000000">5 – 15 triệu</option><option value="15000000-">Trên 15 triệu</option>
      </select>
      <select v-model="f.scale" class="input" @change="apply()"><option value="">Mọi tỷ lệ</option><option v-for="s in catalog.options.scales" :key="s" :value="s">Tỷ lệ {{ s }}</option></select>
      <select v-model="f.age" class="input" @change="apply()"><option value="">Mọi độ tuổi</option><option v-for="(v, a) in AGES" :key="a" :value="String(a)">Phù hợp bé {{ a }}+</option></select>
      <select v-model="f.minRating" class="input" @change="apply()"><option value="">Mọi đánh giá</option><option value="4.5">★ 4.5 trở lên</option><option value="4">★ 4 trở lên</option><option value="3">★ 3 trở lên</option></select>
      <label class="check"><input v-model="f.inStock" type="checkbox" @change="apply()" /> Còn hàng</label>
      <button class="btn btn-sm btn-outline" @click="reset">Xoá lọc</button>
      <button type="button" class="btn sh-done" @click="sheet = false">Xem {{ total }} sản phẩm</button>
    </div>
    </div>
    <div v-if="meta && f.q" class="smart small">
      <span v-if="meta.didYouMean">Có phải bạn muốn tìm <a href="#" @click.prevent="f.q = meta.didYouMean">“{{ meta.didYouMean }}”</a>? · </span>
      <span v-if="meta.scale" class="chip">Tỉ lệ {{ meta.scale }}</span><span v-if="meta.brand" class="chip">{{ meta.brand }}</span>
      <span v-if="meta.maxPrice" class="chip">≤ {{ meta.maxPrice.toLocaleString('vi-VN') }}đ</span><span v-if="meta.age" class="chip">{{ meta.age }}+ tuổi</span>
    </div>
    <div v-if="loadErr" class="alert alert-error">Không tải được sản phẩm: {{ loadErr }} <button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <p class="muted small cnt">{{ total }} sản phẩm<span v-if="f.q"> cho “{{ f.q }}”</span> · Trang {{ page + 1 }}/{{ Math.max(pages, 1) }} · <router-link to="/finder">Chưa biết chọn gì? Tìm mẫu phù hợp →</router-link></p>
    <section>
      <Skeleton v-if="loading && !items.length" :count="8" />
      <div v-else-if="!items.length && !loading" class="empty card">Không tìm thấy sản phẩm phù hợp. <button class="link" @click="reset">Xoá bộ lọc</button></div>
      <div v-else v-stagger class="grid grid-4" :class="{ dim: loading }" :aria-busy="loading"><ProductCard v-for="(p, i) in items" :key="p.id" :p="p" :flash="flashOf(p.id)" /></div>
      <div v-if="pages > 1" class="row pager">
        <button class="btn btn-sm btn-outline" :disabled="page === 0" @click="go(page - 1)">← Trước</button>
        <button v-for="i in pages" :key="i" :class="['btn btn-sm', i - 1 === page ? '' : 'btn-outline']" @click="go(i - 1)">{{ i }}</button>
        <button class="btn btn-sm btn-outline" :disabled="page >= pages - 1" @click="go(page + 1)">Sau →</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import Breadcrumb from '../components/Breadcrumb.vue'
import Skeleton from '../components/Skeleton.vue'
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { catalog, loadCatalog, AGES, TYPES } from '../format'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute(), router = useRouter()
const items = ref([]), total = ref(0), pages = ref(0), page = ref(0), loading = ref(true), price = ref(''), flashes = ref([])
const sheet = ref(false)
const activeCount = computed(() => [f.brandId, price.value, f.scale, f.age, f.minRating, f.inStock].filter(Boolean).length)
watch(sheet, v => { document.documentElement.style.overflow = v ? 'hidden' : '' })
const f = reactive({ q: '', brandId: '', type: '', grp: '', scale: '', age: '', minRating: '', inStock: false, sort: 'featured' })
const accMode = computed(() => f.grp === 'ACCESSORY' || catalog.categories.find(c => c.code === f.type)?.grp === 'ACCESSORY')
const shownCats = computed(() => catalog.categories.filter(c => !f.grp || c.grp === f.grp || c.code === f.type))
const title = computed(() => f.type ? TYPES[f.type] : f.grp === 'ACCESSORY' ? 'Phụ kiện' : f.grp === 'TOY' ? 'Danh mục xe' : 'Tất cả sản phẩm')
const flashOf = id => flashes.value.find(x => x.flash.productId === id && x.live)?.flash

function fromQuery() {
  const q = route.query
  Object.assign(f, { q: q.q || '', brandId: q.brandId || '', type: q.type || '', grp: q.grp || '', scale: q.scale || '', age: q.age || '', minRating: q.minRating || '', inStock: q.inStock === '1', sort: q.sort || 'featured' })
  price.value = q.price || ''; page.value = Number(q.page || 0)
}
let reqId = 0
const loadErr = ref(''), meta = ref(null)
async function load() {
  loading.value = true
  const [min, max] = price.value ? price.value.split('-') : []
  const params = { ...f, inStock: f.inStock || undefined, minPrice: min || undefined, maxPrice: max || undefined, page: page.value, size: 12 }
  Object.keys(params).forEach(k => (params[k] === '' || params[k] === undefined) && delete params[k])
  const id = ++reqId
  let data
  try { data = (await api.get('/products', { params })).data; loadErr.value = '' }
  catch (e) { if (id === reqId) { loadErr.value = errMsg(e); loading.value = false } return }
  if (id !== reqId) return // bỏ kết quả cũ khi gõ nhanh
  meta.value = null; if (f.q) api.get('/products/search-meta', { params: { q: f.q } }).then(r => { if (id === reqId) meta.value = r.data }).catch(() => {})
  items.value = data.content; total.value = data.totalElements; pages.value = data.totalPages; loading.value = false
}
function apply(p = 0) {
  const query = { ...f, inStock: f.inStock ? '1' : '', price: price.value, page: p || '' }
  Object.keys(query).forEach(k => !query[k] && delete query[k])
  if (query.sort === 'featured') delete query.sort
  router.replace({ path: '/products', query })
  if (p) scrollTo({ top: 0 })
}
let deb
const typed = () => { clearTimeout(deb); deb = setTimeout(() => apply(), 300) }
const setType = c => { f.type = c; apply() }
const go = p => apply(p)
const reset = () => router.push({ path: '/products', query: f.grp ? { grp: f.grp } : {} })
watch(() => route.query, () => { if (route.path === '/products') { fromQuery(); load() } })
onMounted(() => { fromQuery(); load(); loadCatalog(); api.get('/flash-sales').then(r => { flashes.value = r.data }) })
</script>

<style scoped>
.smart { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; margin-bottom: 10px; } .smart .chip { background: var(--bronze-soft); padding: 2px 10px; border-radius: 99px; } .smart a { color: var(--accent); font-weight: 600; }
.filters { position: sticky; top: 64px; z-index: 20; background: #fff; padding: 8px 0 2px; margin: 0 -4px; }
.dim { opacity: .55; transition: opacity .15s ease-out; pointer-events: none; }
.fbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 10px 12px; background: var(--bg-2); border-radius: 10px; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { background: #fff; border: 1px solid var(--border); color: var(--text-2); border-radius: 999px; padding: 6px 14px; cursor: pointer; font-family: inherit; font-size: .85rem; transition: background-color .15s ease-out, border-color .15s ease-out; }
.chip:hover { border-color: var(--bronze); } .chip.on { background: var(--bronze); color: #fff; border-color: var(--bronze); }
.sort { width: 200px; padding: 8px 12px; }
.fbar2 { display: grid; grid-template-columns: 1.5fr repeat(5, 1fr) auto auto; gap: 8px; align-items: center; margin: 12px 0 6px; }
.fbar2 .input { padding: 8px 10px; font-size: .85rem; } .check { display: flex; gap: 6px; align-items: center; font-size: .85rem; white-space: nowrap; }
.cnt { margin-bottom: 14px; }
.grid-4 { gap: 22px 18px; }
.link { background: none; border: none; color: var(--bronze); cursor: pointer; font: inherit; }
.pager { justify-content: center; margin-top: 26px; }
.fbtn, .sh-head, .sh-done { display: none; }
.fcount { display: inline-grid; place-items: center; min-width: 18px; height: 18px; margin-left: 6px; border-radius: 9px; background: var(--accent); color: #fff; font-size: .7rem; }
@media (max-width: 1100px) and (min-width: 900px) { .fbar2 { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 899px) {
  .filters { top: 56px; margin: 0 -16px; padding: 8px 16px 6px; }
  .fbar { display: grid; grid-template-columns: 1fr auto auto; gap: 8px; padding: 8px; }
  .chips { grid-column: 1 / -1; flex-wrap: nowrap; overflow-x: auto; scrollbar-width: none; margin: 0 -2px; padding-bottom: 2px; }
  .chip { flex-shrink: 0; min-height: 36px; }
  .fbtn { display: inline-flex; grid-column: 2; } .sort { width: auto; grid-column: 3; max-width: 46vw; }
  .fbar2 { position: fixed; left: 0; right: 0; bottom: 0; z-index: 185; margin: 0; display: flex; flex-direction: column; gap: 10px; padding: 14px 16px calc(16px + env(safe-area-inset-bottom));
    background: var(--surface); max-height: 82vh; overflow-y: auto; box-shadow: 0 -20px 50px rgba(0,0,0,.3); transform: translateY(105%); visibility: hidden; transition: transform .22s ease-out, visibility 0s .22s; }
  .fbar2.open { transform: none; visibility: visible; transition: transform .22s ease-out; }
  .fbar2 .input { min-height: 44px; font-size: 16px; } .check { min-height: 40px; }
  .sh-head { display: flex; justify-content: space-between; align-items: center; } .sh-head b { font: 600 1.1rem 'Oswald', sans-serif; text-transform: uppercase; }
  .sh-x { width: 40px; height: 40px; border: none; background: none; font-size: 1.1rem; cursor: pointer; color: var(--text); }
  .sh-done { display: inline-flex; }
  .sh-scrim { position: fixed; inset: 0; z-index: 184; background: rgba(0,0,0,.4); }
  .grid-4 { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 10px; }
}
@media (min-width: 600px) and (max-width: 899px) { .grid-4 { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
.sh-fade-enter-active, .sh-fade-leave-active { transition: opacity .2s ease-out; } .sh-fade-enter-from, .sh-fade-leave-to { opacity: 0; }
@media (prefers-reduced-motion: reduce) { .fbar2, .fbar2.open { transition: none; } }
</style>
