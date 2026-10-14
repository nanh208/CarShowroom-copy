<template>
  <div class="container page">
    <h1>Danh mục mô hình</h1>
    <p class="muted" style="margin-bottom:20px">{{ total }} sản phẩm<span v-if="f.q"> cho "{{ f.q }}"</span></p>
    <div class="layout">
      <aside class="card filters">
        <div class="field"><label>Từ khoá</label><input v-model="f.q" class="input" placeholder="Tên xe, hãng..." @keyup.enter="apply" /></div>
        <div class="field"><label>Thương hiệu</label>
          <select v-model="f.brandId" class="input"><option value="">Tất cả</option><option v-for="b in brands" :key="b.id" :value="String(b.id)">{{ b.name }}</option></select></div>
        <div class="field"><label>Loại xe</label>
          <select v-model="f.type" class="input"><option value="">Tất cả</option><option v-for="(v, k) in TYPES" :key="k" :value="k">{{ v }}</option></select></div>
        <div class="field"><label>Tỉ lệ</label>
          <div class="chips"><button v-for="s in ['', ...scales]" :key="s" :class="['chip', { on: f.scale === s }]" @click="f.scale = s">{{ s || 'Tất cả' }}</button></div></div>
        <div class="field"><label>Khoảng giá</label>
          <select v-model="price" class="input">
            <option value="">Tất cả</option><option value="0-500000">Dưới 500.000đ</option><option value="500000-1000000">500.000đ – 1 triệu</option>
            <option value="1000000-3000000">1 – 3 triệu</option><option value="3000000-">Trên 3 triệu</option>
          </select></div>
        <label class="check field"><input v-model="f.inStock" type="checkbox" /> Chỉ hiện còn hàng</label>
        <div class="row"><button class="btn btn-sm grow" @click="apply">Lọc</button><button class="btn btn-sm btn-outline" @click="reset">Xoá lọc</button></div>
      </aside>
      <section>
        <div class="row between toolbar">
          <span class="muted small">Trang {{ page + 1 }}/{{ Math.max(pages, 1) }}</span>
          <select v-model="f.sort" class="input sort" @change="apply">
            <option value="newest">Mới nhất</option><option value="bestseller">Bán chạy</option>
            <option value="price_asc">Giá tăng dần</option><option value="price_desc">Giá giảm dần</option><option value="name">Tên A–Z</option>
          </select>
        </div>
        <div v-if="loading" class="spinner" />
        <div v-else-if="!items.length" class="empty card">Không tìm thấy sản phẩm phù hợp 😢</div>
        <div v-else class="grid grid-4"><ProductCard v-for="p in items" :key="p.id" :p="p" /></div>
        <div v-if="pages > 1" class="row pager">
          <button v-for="i in pages" :key="i" :class="['btn btn-sm', i - 1 === page ? '' : 'btn-outline']" @click="go(i - 1)">{{ i }}</button>
        </div>
      </section>
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
const scales = ['1:18', '1:24', '1:43', '1:64']
const f = reactive({ q: '', brandId: '', type: '', scale: '', inStock: false, sort: 'newest' })

function fromQuery() {
  const q = route.query
  Object.assign(f, { q: q.q || '', brandId: q.brandId || '', type: q.type || '', scale: q.scale || '', inStock: q.inStock === '1', sort: q.sort || 'newest' })
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
  if (query.sort === 'newest') delete query.sort
  router.push({ path: '/products', query })
}
const go = p => apply(p)
const reset = () => router.push('/products')
watch(() => route.query, () => { if (route.path === '/products') { fromQuery(); load() } })
onMounted(async () => { fromQuery(); load(); brands.value = (await api.get('/brands')).data })
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 260px 1fr; gap: 24px; align-items: start; }
@media (max-width: 860px) { .layout { grid-template-columns: 1fr; } }
.filters { position: sticky; top: 150px; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { background: rgba(255,255,255,.06); border: 1px solid var(--border); color: var(--label); border-radius: 999px; padding: 5px 12px; cursor: pointer; font-family: inherit; }
.chip.on { background: var(--primary); color: #fff; border-color: var(--primary); }
.toolbar { margin-bottom: 14px; } .sort { width: 190px; padding: 8px 12px; }
.pager { justify-content: center; margin-top: 26px; }
</style>
