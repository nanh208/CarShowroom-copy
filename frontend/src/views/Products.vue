<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Danh mục xe' }]" />
    <div class="fbar">
      <div class="chips">
        <button v-for="(v, k) in { '': 'Tất cả', ...TYPES }" :key="k" :class="['chip', { on: f.type === k }]" @click="f.type = k; apply()">{{ v }}</button>
      </div>
      <select v-model="f.sort" class="input sort" @change="apply">
        <option value="featured">Sắp xếp: Nổi bật</option><option value="newest">Mới nhất</option><option value="bestseller">Bán chạy</option>
        <option value="price_asc">Giá tăng dần</option><option value="price_desc">Giá giảm dần</option><option value="name">Tên A–Z</option>
      </select>
    </div>
    <div class="fbar2">
      <input v-model="f.q" class="input" placeholder="Tìm tên xe, hãng..." @keyup.enter="apply" />
      <select v-model="f.brandId" class="input" @change="apply"><option value="">Mọi thương hiệu</option><option v-for="b in brands" :key="b.id" :value="String(b.id)">{{ b.name }}</option></select>
      <select v-model="f.fuel" class="input" @change="apply"><option value="">Mọi nhiên liệu</option><option v-for="s in FUELS" :key="s" :value="s">{{ s }}</option></select>
      <select v-model="price" class="input" @change="apply">
        <option value="">Mọi mức giá</option><option value="0-1000000000">Dưới 1 tỷ</option><option value="1000000000-2000000000">1 – 2 tỷ</option><option value="2000000000-5000000000">2 – 5 tỷ</option><option value="5000000000-15000000000">5 – 15 tỷ</option><option value="15000000000-">Trên 15 tỷ</option>
      </select>
      <label class="check"><input v-model="f.inStock" type="checkbox" @change="apply" /> Còn hàng</label>
      <span class="muted small cnt">{{ total }} sản phẩm</span>
      <button class="btn btn-sm btn-outline" @click="reset">Xoá lọc</button>
    </div>
    <section>
        <div v-if="loading" class="spinner" />
        <div v-else-if="!items.length" class="empty card">Không tìm thấy sản phẩm phù hợp </div>
        <div v-else class="grid grid-4"><ProductCard v-for="p in items" :key="p.id" :p="p" /></div>
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
import { ref, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { TYPES, FUELS } from '../format'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute(), router = useRouter()
const brands = ref([]), items = ref([]), total = ref(0), pages = ref(0), page = ref(0), loading = ref(true), price = ref('')
const f = reactive({ q: '', brandId: '', type: '', fuel: '', inStock: false, sort: 'featured' })

function fromQuery() {
  const q = route.query
  Object.assign(f, { q: q.q || '', brandId: q.brandId || '', type: q.type || '', fuel: q.fuel || '', inStock: q.inStock === '1', sort: q.sort || 'featured' })
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
const reset = () => router.push('/products')
watch(() => route.query, () => { if (route.path === '/products') { fromQuery(); load() } })
onMounted(async () => { fromQuery(); load(); brands.value = (await api.get('/brands')).data })
</script>

<style scoped>
.fbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 10px 12px; background: var(--bg-2); border-radius: 10px; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { background: #fff; border: 1px solid var(--border); color: var(--text-2); border-radius: 999px; padding: 6px 14px; cursor: pointer; font-family: inherit; font-size: .85rem; }
.chip.on { background: var(--bronze); color: #fff; border-color: var(--bronze); }
.sort { width: 200px; padding: 8px 12px; }
.fbar2 { display: grid; grid-template-columns: 1.4fr 1fr 1fr 1fr auto auto auto; gap: 10px; align-items: center; margin: 12px 0 20px; }
.fbar2 .input { padding: 8px 12px; } .check { display: flex; gap: 6px; align-items: center; font-size: .85rem; white-space: nowrap; } .cnt { white-space: nowrap; }
.grid-4 { gap: 22px 18px; }
.pager { justify-content: center; margin-top: 26px; }
@media (max-width: 860px) { .fbar { flex-direction: column; align-items: stretch; } .fbar2 { grid-template-columns: 1fr 1fr; } }
</style>
