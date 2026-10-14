<template>
  <div class="container page">
    <h1 class="page-title">Danh mục mô hình</h1>
    <p class="page-sub">{{ total }} sản phẩm<span v-if="f.q"> cho "{{ f.q }}"</span></p>

    <div class="filter-bar">
      <div class="chips">
        <button :class="['chip', { on: !f.type && !f.scale }]" @click="quick({})">Tất cả</button>
        <button v-for="(v, k) in TYPES" :key="k" :class="['chip', { on: f.type === k }]" @click="quick({ type: k })">{{ v }}</button>
        <button v-for="s in ['1:18','1:24','1:43','1:64']" :key="s" :class="['chip', { on: f.scale === s }]" @click="quick({ scale: s })">{{ s }}</button>
      </div>
      <div class="row tools">
        <input v-model="f.q" class="input" style="width:200px" placeholder="Tìm..." @keyup.enter="apply" />
        <select v-model="f.brandId" class="input" style="width:150px" @change="apply"><option value="">Thương hiệu</option><option v-for="b in brands" :key="b.id" :value="String(b.id)">{{ b.name }}</option></select>
        <select v-model="price" class="input" style="width:160px" @change="apply">
          <option value="">Giá</option><option value="0-500000">Dưới 500k</option><option value="500000-1000000">500k – 1tr</option>
          <option value="1000000-3000000">1 – 3tr</option><option value="3000000-">Trên 3tr</option>
        </select>
        <select v-model="f.sort" class="input" style="width:140px" @change="apply">
          <option value="featured">Nổi bật</option><option value="newest">Mới nhất</option><option value="bestseller">Bán chạy</option>
          <option value="price_asc">Giá ↑</option><option value="price_desc">Giá ↓</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="spinner" />
    <div v-else-if="!items.length" class="empty card">Không tìm thấy sản phẩm phù hợp.</div>
    <div v-else class="grid grid-4"><ProductCard v-for="p in items" :key="p.id" :p="p" /></div>
    <div v-if="pages > 1" class="row pager">
      <button v-for="i in pages" :key="i" :class="['btn btn-sm', i - 1 === page ? '' : 'btn-outline']" @click="go(i - 1)">{{ i }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { TYPES } from '../format'
import ProductCard from '../components/ProductCard.vue'
const route = useRoute(), router = useRouter()
const brands = ref([]), items = ref([]), total = ref(0), pages = ref(0), page = ref(0), loading = ref(true), price = ref('')
const f = reactive({ q: '', brandId: '', type: '', scale: '', inStock: false, sort: 'featured' })
function fromQuery() {
  const q = route.query
  Object.assign(f, { q: q.q || '', brandId: q.brandId || '', type: q.type || '', scale: q.scale || '', inStock: q.inStock === '1', sort: q.sort || 'featured' })
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
  const query = { ...f, inStock: f.inStock ? '1' : '', price: price.value, page: typeof p === 'number' && p ? p : '' }
  Object.keys(query).forEach(k => !query[k] && delete query[k])
  if (query.sort === 'featured') delete query.sort
  router.push({ path: '/products', query })
}
const go = p => apply(p)
function quick(extra) {
  Object.assign(f, { type: '', scale: '', ...extra })
  apply()
}
watch(() => route.query, () => { if (route.path === '/products') { fromQuery(); load() } })
onMounted(async () => { fromQuery(); load(); brands.value = (await api.get('/brands')).data })
</script>

<style scoped>
.filter-bar { background: var(--bg-soft); padding: 14px 16px; margin-bottom: 22px; border: 1px solid var(--line); display: flex; flex-direction: column; gap: 12px; }
.tools { justify-content: flex-end; }
.tools .input { padding: 8px 10px; font-size: .88rem; }
.pager { justify-content: center; margin-top: 26px; }
</style>
