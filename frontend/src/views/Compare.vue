<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'So sánh xe' }]" />
    <h1>So sánh xe</h1>
    <p>Đặt tối đa 3 mẫu xe cạnh nhau. Những dòng có thông số khác nhau được làm nổi bật.</p>
    <div v-if="!items.length" class="empty card">Chưa có xe nào để so sánh. Bấm biểu tượng ⇄ trên ảnh xe để thêm. <router-link to="/products">Chọn xe →</router-link></div>
    <template v-else>
      <label class="check diffonly"><input v-model="diffOnly" type="checkbox" /> Chỉ hiện thông số khác nhau</label>
      <div class="table-wrap card flush">
        <table class="cmp">
          <thead><tr><th />
            <th v-for="p in items" :key="p.id">
              <div class="head">
                <button class="rm" title="Bỏ khỏi so sánh" @click="prefs.toggleCompare(p)">×</button>
                <router-link :to="`/products/${p.id}`"><img :src="p.images?.[0]" alt="" /></router-link>
                <small>{{ p.brand?.name }}</small>
                <router-link :to="`/products/${p.id}`" class="nm">{{ p.name }}</router-link>
                <div class="price">{{ vnd(p.price) }}</div>
                <button class="btn btn-sm" :disabled="!p.stock" @click="add(p)">Thêm vào giỏ</button>
              </div>
            </th>
            <th v-for="i in 3 - items.length" :key="'e' + i"><router-link to="/products" class="addslot">+ Thêm xe</router-link></th>
          </tr></thead>
          <tbody>
            <tr v-for="r in rows" :key="r.label" :class="{ diff: r.diff }">
              <td>{{ r.label }}</td><td v-for="(v, i) in r.values" :key="i">{{ v || '—' }}</td><td v-for="i in 3 - items.length" :key="'e' + i" />
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import Breadcrumb from '../components/Breadcrumb.vue'
import { usePrefs, toast } from '../stores/prefs'
import { useCart } from '../stores/cart'
import { vnd, TYPES } from '../format'
const prefs = usePrefs(), cart = useCart(), diffOnly = ref(false)
const fresh = ref({})
// lấy dữ liệu mới nhất (giá/tồn kho) từ API
onMounted(() => prefs.compare.forEach(async c => { try { fresh.value[c.id] = (await api.get(`/products/${c.id}`)).data.product } catch {} }))
const items = computed(() => prefs.compare.map(c => fresh.value[c.id] || c))
const rows = computed(() => {
  const keys = new Set(); items.value.forEach(p => Object.keys(p.specs || {}).forEach(k => keys.add(k)))
  const base = [['Giá niêm yết', p => vnd(p.price)], ['Hãng', p => p.brand?.name], ['Dòng xe', p => TYPES[p.type]], ['Tồn kho', p => p.stock ? `${p.stock} xe` : 'Hết hàng']]
  const all = [...base.map(([label, f]) => ({ label, values: items.value.map(f) })),
    ...[...keys].map(k => ({ label: k, values: items.value.map(p => p.specs?.[k]) }))]
  all.forEach(r => { r.diff = items.value.length > 1 && new Set(r.values.map(v => v ?? '')).size > 1 })
  return diffOnly.value ? all.filter(r => r.diff) : all
})
const add = p => { cart.add({ productId: p.id, name: p.name, fuel: p.fuel, type: p.type, baseColor: p.baseColor, image: p.images?.[0], unitPrice: p.price, quantity: 1, stock: p.stock, options: {}, optionLabel: 'Màu nguyên bản' }); toast(`Đã thêm “${p.name}” vào giỏ hàng`) }
</script>
<style scoped>
.diffonly { margin-bottom: 16px; }
.flush { padding: 0; overflow: hidden; }
.cmp { width: 100%; border-collapse: collapse; table-layout: fixed; }
.cmp th, .cmp td { padding: 16px 24px; border-bottom: 1px solid var(--border); text-align: left; vertical-align: top; }
.cmp th:first-child, .cmp td:first-child { width: 200px; color: var(--muted); }
.cmp td + td, .cmp th + th { border-left: 1px solid var(--border); }
.head { position: relative; display: flex; flex-direction: column; gap: 8px; font-weight: 400; }
.head img { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; }
.head small { color: var(--muted); text-transform: uppercase; letter-spacing: .08em; font-size: .75rem; }
.nm { color: #fff; font-weight: 700; font-size: 1.1rem; } .head .price { font-size: 1.2rem; }
.rm { position: absolute; top: 8px; right: 8px; width: 32px; height: 32px; border-radius: 50%; border: none; background: rgba(0,0,0,.6); color: #fff; font-size: 1.1rem; cursor: pointer; z-index: 1; }
.addslot { display: flex; align-items: center; justify-content: center; aspect-ratio: 16/10; border: 1px dashed rgba(255,255,255,.2); border-radius: 12px; color: var(--muted); }
.addslot:hover { border-color: var(--accent); color: var(--accent); }
tr.diff td:not(:first-child) { color: #fff; font-weight: 600; } tr.diff td:first-child { color: var(--accent); }
</style>
