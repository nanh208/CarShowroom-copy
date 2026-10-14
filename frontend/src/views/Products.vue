<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: title }]" />
    <div class="fbar">
      <div class="chips">
        <button :class="['chip', { on: !f.type }]" @click="setType('')">Tất cả</button>
        <button v-for="c in shownCats" :key="c.code" :class="['chip', { on: f.type === c.code }]" @click="setType(c.code)">{{ c.name }}</button>
      </div>
      <select v-model="f.sort" class="input sort" @change="apply()">
        <option value="featured">Sắp xếp: Nổi bật</option><option value="newest">Mới nhất</option><option value="bestseller">Bán chạy</option>
        <option value="rating">Đánh giá cao</option><option value="price_asc">Giá tăng dần</option><option value="price_desc">Giá giảm dần</option><option value="name">Tên A–Z</option>
      </select>
    </div>
    <div class="fbar2">
      <input v-model="f.q" class="input" placeholder="Tìm tên, hãng, mã SKU..." @keyup.enter="apply()" />
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
    </div>
    <p class="muted small cnt">{{ total }} sản phẩm<span v-if="f.q"> cho “{{ f.q }}”</span> · Trang {{ page + 1 }}/{{ Math.max(pages, 1) }}</p>
    <section>
      <Skeleton v-if="loading" :count="8" />
      <div v-else-if="!items.length" class="empty card">Không tìm thấy sản phẩm phù hợp. <button class="link" @click="reset">Xoá bộ lọc</button></div>
      <div v-else class="grid grid-4"><ProductCard v-for="(p, i) in items" :key="p.id" v-reveal:[i%4*70]="'up'" :p="p" :flash="flashOf(p.id)" /></div>
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
import api from '../api'
import { catalog, loadCatalog, AGES, TYPES } from '../format'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute(), router = useRouter()
const items = ref([]), total = ref(0), pages = ref(0), page = ref(0), loading = ref(true), price = ref(''), flashes = ref([])
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
async function load() {
  loading.value = true
  const [min, max] = price.value ? price.value.split('-') : []
  const params = { ...f, inStock: f.inStock || undefined, minPrice: min || undefined, maxPrice: max || undefined, page: page.value, size: 12 }
  Object.keys(params).forEach(k => (params[k] === '' || params[k] === undefined) && delete params[k])
  const { data } = await api.get('/products', { params })
  items.value = data.content; total.value = data.totalElements; pages.value = data.totalPages; loading.value = false
}
function apply(p = 0) {
  const query = { ...f, inStock: f.inStock ? '1' : '', price: price.value, page: p || '' }
  Object.keys(query).forEach(k => !query[k] && delete query[k])
  if (query.sort === 'featured') delete query.sort
  router.push({ path: '/products', query })
}
const setType = c => { f.type = c; apply() }
const go = p => apply(p)
const reset = () => router.push({ path: '/products', query: f.grp ? { grp: f.grp } : {} })
watch(() => route.query, () => { if (route.path === '/products') { fromQuery(); load() } })
onMounted(() => { fromQuery(); load(); loadCatalog(); api.get('/flash-sales').then(r => { flashes.value = r.data }) })
</script>

<style scoped>
.fbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 10px 12px; background: var(--bg-2); border-radius: 10px; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { background: #fff; border: 1px solid var(--border); color: var(--text-2); border-radius: 999px; padding: 6px 14px; cursor: pointer; font-family: inherit; font-size: .85rem; transition: .2s; }
.chip:hover { border-color: var(--bronze); } .chip.on { background: var(--bronze); color: #fff; border-color: var(--bronze); }
.sort { width: 200px; padding: 8px 12px; }
.fbar2 { display: grid; grid-template-columns: 1.5fr repeat(5, 1fr) auto auto; gap: 8px; align-items: center; margin: 12px 0 6px; }
.fbar2 .input { padding: 8px 10px; font-size: .85rem; } .check { display: flex; gap: 6px; align-items: center; font-size: .85rem; white-space: nowrap; }
.cnt { margin-bottom: 14px; }
.grid-4 { gap: 22px 18px; }
.link { background: none; border: none; color: var(--bronze); cursor: pointer; font: inherit; }
.pager { justify-content: center; margin-top: 26px; }
@media (max-width: 960px) { .fbar { flex-direction: column; align-items: stretch; } .fbar2 { grid-template-columns: 1fr 1fr; } }
</style>
