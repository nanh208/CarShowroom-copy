<template>
  <div class="container page">
    <div v-if="!p" class="spinner" />
    <template v-else>
      <div class="crumb muted small"><router-link to="/">Trang chủ</router-link> / <router-link to="/products">Sản phẩm</router-link> /
        <router-link :to="{ path: '/products', query: { brandId: p.brand?.id } }">{{ p.brand?.name }}</router-link> / {{ p.name }}</div>

      <div class="top grid">
        <div>
          <div class="main-img">
            <img v-if="view !== 'design' && gallery[view]" :src="gallery[view]" :alt="p.name" class="photo" />
            <CarPreview v-else :body="paintHex" :wheel="wheel.hex" :interior="interior.hex" :type="p.type" :scale="p.scale" />
          </div>
          <div class="row thumbs">
            <button v-for="(img, i) in gallery" :key="i" :class="['thumb', { on: view === i }]" @click="view = i"><img :src="img" /></button>
            <button v-if="canCustomize" :class="['thumb', { on: view === 'design' }]" @click="view = 'design'">🎨</button>
          </div>
        </div>

        <div>
          <div class="row"><span v-if="p.isNew" class="badge badge-new">MỚI</span><span class="badge">{{ TYPES[p.type] }}</span><span class="badge">{{ p.scale }}</span></div>
          <h1 class="title">{{ p.name }}</h1>
          <div class="muted">{{ p.brand?.name }} · Đã bán {{ p.sold }}</div>
          <div class="pricebox">
            <span class="price big">{{ vnd(unitPrice) }}</span>
            <span v-if="p.oldPrice && !extra" class="old-price">{{ vnd(p.oldPrice) }}</span>
            <div v-if="extra" class="small muted">Gốc {{ vnd(p.price) }} + thiết kế {{ vnd(extra) }}</div>
          </div>
          <p class="desc-short">{{ p.description?.slice(0, 180) }}…</p>

          <div v-if="canCustomize" class="designer">
            <h3>Thiết kế mô hình</h3>
            <div v-for="g in groups" :key="g.key" class="opt">
              <label>{{ g.label }}: <b>{{ g.cur.name }}</b><span v-if="g.cur.price" class="muted"> (+{{ vnd(g.cur.price) }})</span></label>
              <div class="row">
                <button v-for="o in g.list" :key="o.id" :class="['swatch', { active: sel[g.key] === o.id }]" :title="o.name"
                  :style="{ background: g.key === 'wheel' ? `radial-gradient(circle, #111 25%, ${o.hex} 30%)` : (o.hex || p.baseColor) }"
                  @click="sel[g.key] = o.id; view = 'design'" />
              </div>
            </div>
          </div>

          <div class="row buy">
            <div class="qty"><button @click="qty = Math.max(1, qty - 1)">−</button><span>{{ qty }}</span><button @click="qty = Math.min(p.stock, qty + 1)">+</button></div>
            <span :class="p.stock ? 'ok' : 'out'">{{ p.stock ? `Còn ${p.stock}` : 'Hết hàng' }}</span>
          </div>
          <div class="row">
            <button class="btn grow" :disabled="!p.stock" @click="add(false)">Thêm vào giỏ</button>
            <button class="btn btn-red grow" :disabled="!p.stock" @click="add(true)">Mua ngay</button>
          </div>
          <div v-if="added" class="alert alert-success" style="margin-top:12px">Đã thêm vào giỏ. <router-link to="/cart">Xem giỏ →</router-link></div>
        </div>
      </div>

      <div class="specs-row">
        <div class="spec-box" v-for="(v, k) in topSpecs" :key="k"><div class="k">{{ k }}</div><div class="v">{{ v }}</div></div>
      </div>

      <div class="detail-block grid grid-2">
        <div class="main-img"><ProductImage :product="p" :index="gallery.length > 1 ? 1 : 0" /></div>
        <div>
          <h3>Mô tả sản phẩm</h3>
          <p class="desc">{{ p.description }}</p>
          <table class="table" style="margin-top:14px"><tbody>
            <tr v-for="(v, k) in p.specs" :key="k"><th>{{ k }}</th><td>{{ v }}</td></tr>
            <tr><th>Chất liệu</th><td>{{ p.material }}</td></tr>
          </tbody></table>
        </div>
      </div>

      <section v-if="related.length" class="section">
        <div class="section-head"><h2>Sản phẩm <span>liên quan</span></h2></div>
        <div class="grid grid-4"><ProductCard v-for="r in related" :key="r.id" :p="r" /></div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { vnd, TYPES } from '../format'
import { useCart } from '../stores/cart'
import CarPreview from '../components/CarPreview.vue'
import ProductCard from '../components/ProductCard.vue'
import ProductImage from '../components/ProductImage.vue'

const route = useRoute(), router = useRouter(), cart = useCart()
const p = ref(null), related = ref([]), opts = ref({ paints: [], wheels: [], interiors: [] })
const qty = ref(1), view = ref(0), added = ref(false)
const sel = reactive({ paint: 'original', wheel: 'standard', interior: 'black' })
const find = (list, id) => list.find(o => o.id === id) || list[0] || { name: '', price: 0, hex: '#888' }
const paint = computed(() => find(opts.value.paints, sel.paint))
const wheel = computed(() => find(opts.value.wheels, sel.wheel))
const interior = computed(() => find(opts.value.interiors, sel.interior))
const canCustomize = computed(() => p.value?.customizable && p.value.type !== 'ACCESSORY')
const paintHex = computed(() => paint.value.hex || p.value?.baseColor)
const extra = computed(() => canCustomize.value ? paint.value.price + wheel.value.price + interior.value.price : 0)
const unitPrice = computed(() => p.value.price + extra.value)
const groups = computed(() => [
  { key: 'paint', label: 'Màu sơn', list: opts.value.paints, cur: paint.value },
  { key: 'wheel', label: 'Mâm xe', list: opts.value.wheels, cur: wheel.value },
  { key: 'interior', label: 'Nội thất', list: opts.value.interiors, cur: interior.value }
])
const gallery = computed(() => p.value?.images?.length ? p.value.images : [])
const topSpecs = computed(() => {
  const s = p.value?.specs || {}
  const keys = Object.keys(s).slice(0, 3)
  const out = {}
  keys.forEach(k => out[k] = s[k])
  if (!Object.keys(out).length) return { 'Tỉ lệ': p.value.scale, 'Thương hiệu': p.value.brand?.name, 'Chất liệu': p.value.material }
  return out
})

async function load() {
  p.value = null; added.value = false; qty.value = 1; view.value = 0
  Object.assign(sel, { paint: 'original', wheel: 'standard', interior: 'black' })
  const [d, o] = await Promise.all([api.get(`/products/${route.params.id}`), api.get('/options')])
  p.value = d.data.product; related.value = d.data.related; opts.value = o.data.customizer
}
function add(buyNow) {
  const v = p.value
  cart.add({ productId: v.id, name: v.name, scale: v.scale, type: v.type, baseColor: paintHex.value, wheelHex: wheel.value.hex, interiorHex: interior.value.hex,
    image: v.images?.[0], unitPrice: unitPrice.value, quantity: qty.value, stock: v.stock,
    options: canCustomize.value ? { ...sel } : {},
    optionLabel: canCustomize.value ? `${paint.value.name} · ${wheel.value.name} · ${interior.value.name}` : '' })
  if (buyNow) router.push('/cart'); else added.value = true
}
watch(() => route.params.id, id => id && load(), { immediate: true })
</script>

<style scoped>
.crumb { margin-bottom: 16px; }
.top { grid-template-columns: 1.1fr 1fr; gap: 28px; align-items: start; }
@media (max-width: 860px) { .top { grid-template-columns: 1fr; } }
.main-img { background: #f5f5f5; border: 1px solid var(--line); aspect-ratio: 16/10; overflow: hidden; display: flex; align-items: center; justify-content: center; }
.photo { width: 100%; height: 100%; object-fit: cover; }
.thumbs { margin-top: 8px; }
.thumb { background: #fff; border: 2px solid var(--line); width: 64px; height: 48px; padding: 0; cursor: pointer; overflow: hidden; }
.thumb img { width: 100%; height: 100%; object-fit: cover; } .thumb.on { border-color: var(--bronze); }
.title { font-size: 1.7rem; margin: 10px 0 6px; letter-spacing: .02em; }
.pricebox { margin: 14px 0; } .big { font-size: 1.8rem; }
.desc-short { color: var(--muted); margin-bottom: 14px; }
.designer { border: 1px solid var(--line); padding: 14px; margin-bottom: 14px; background: #fafafa; }
.designer h3 { font-size: 1rem; margin-bottom: 10px; } .opt { margin-bottom: 10px; }
.buy { margin-bottom: 10px; } .ok { color: #1e7a3a; } .out { color: #922b21; }
.specs-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin: 28px 0; }
.spec-box { background: var(--bg-soft); border: 1px solid var(--line); padding: 16px; text-align: center; }
.spec-box .k { font-size: .75rem; color: var(--muted); text-transform: uppercase; letter-spacing: .06em; margin-bottom: 4px; }
.spec-box .v { font-weight: 700; }
.detail-block { margin: 20px 0 10px; gap: 24px; align-items: start; }
.detail-block .main-img { aspect-ratio: 4/3; }
.desc { color: #444; white-space: pre-line; }
.table th { width: 40%; text-transform: none; font-size: .88rem; color: var(--muted); }
@media (max-width: 640px) { .specs-row { grid-template-columns: 1fr; } }
</style>
