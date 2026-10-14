<template>
  <div class="container page">
    <div v-if="!p" class="spinner" />
    <template v-else>
      <div class="crumb muted small"><router-link to="/">Trang chủ</router-link> / <router-link to="/products">Sản phẩm</router-link> /
        <router-link :to="{ path: '/products', query: { brandId: p.brand?.id } }">{{ p.brand?.name }}</router-link> / {{ p.name }}</div>
      <div class="grid grid-2 top">
        <div>
          <div class="card main-img">
            <CarPreview v-if="view === 'design' || !p.images.length" :body="paintHex" :wheel="wheel.hex" :interior="interior.hex" :type="p.type" :scale="p.scale" />
            <img v-else :src="p.images[view]" :alt="p.name" class="photo" />
          </div>
          <div class="row thumbs">
            <button :class="['thumb', { on: view === 'design' }]" @click="view = 'design'">🎨 Bản thiết kế</button>
            <button v-for="(img, i) in p.images" :key="i" :class="['thumb', { on: view === i }]" @click="view = i"><img :src="img" /></button>
          </div>
        </div>

        <div>
          <div class="row"><span v-if="p.isNew" class="badge badge-new">MỚI</span><span class="badge">{{ TYPES[p.type] }}</span><span class="badge">Tỉ lệ {{ p.scale }}</span></div>
          <h1 class="title">{{ p.name }}</h1>
          <div class="muted">Thương hiệu: <b>{{ p.brand?.name }}</b> · Đã bán {{ p.sold }}</div>
          <div class="pricebox">
            <span class="price big">{{ vnd(unitPrice) }}</span>
            <span v-if="p.oldPrice && !extra" class="old-price">{{ vnd(p.oldPrice) }}</span>
            <div v-if="extra" class="small muted">Giá gốc {{ vnd(p.price) }} + tuỳ chọn thiết kế {{ vnd(extra) }}</div>
          </div>

          <div v-if="canCustomize" class="card designer">
            <h3>🎨 Thiết kế mô hình của bạn</h3>
            <div class="opt">
              <label>Màu sơn: <b>{{ paint.name }}</b> <span v-if="paint.price" class="muted">(+{{ vnd(paint.price) }})</span></label>
              <div class="row"><button v-for="o in opts.paints" :key="o.id" :class="['swatch', { active: paint.id === o.id }]" :title="o.name"
                :style="{ background: o.hex || p.baseColor }" @click="sel.paint = o.id; view = 'design'" /></div>
            </div>
            <div class="opt">
              <label>Mâm xe: <b>{{ wheel.name }}</b> <span v-if="wheel.price" class="muted">(+{{ vnd(wheel.price) }})</span></label>
              <div class="row"><button v-for="o in opts.wheels" :key="o.id" :class="['swatch', { active: wheel.id === o.id }]" :title="o.name"
                :style="{ background: `radial-gradient(circle, #111 25%, ${o.hex} 30%)` }" @click="sel.wheel = o.id; view = 'design'" /></div>
            </div>
            <div class="opt">
              <label>Màu nội thất: <b>{{ interior.name }}</b> <span v-if="interior.price" class="muted">(+{{ vnd(interior.price) }})</span></label>
              <div class="row"><button v-for="o in opts.interiors" :key="o.id" :class="['swatch', { active: interior.id === o.id }]" :title="o.name"
                :style="{ background: o.hex }" @click="sel.interior = o.id; view = 'design'" /></div>
            </div>
          </div>

          <div class="row buy">
            <div class="qty"><button @click="qty = Math.max(1, qty - 1)">−</button><span>{{ qty }}</span><button @click="qty = Math.min(p.stock, qty + 1)">+</button></div>
            <span :class="p.stock ? 'ok' : 'out'">{{ p.stock ? `Còn ${p.stock} sản phẩm` : 'Tạm hết hàng' }}</span>
          </div>
          <div class="row">
            <button class="btn grow" :disabled="!p.stock" @click="add(false)">🛒 Thêm vào giỏ</button>
            <button class="btn btn-outline grow" :disabled="!p.stock" @click="add(true)">Mua ngay</button>
          </div>
          <div v-if="added" class="alert alert-success" style="margin-top:12px">Đã thêm vào giỏ hàng! <router-link to="/cart">Xem giỏ hàng →</router-link></div>
          <div class="row" style="margin-top:12px">
            <router-link :to="{ path: '/services', query: { productId: p.id } }" class="btn btn-sm btn-outline">📅 Đặt lịch xem mẫu thật tại showroom</router-link>
          </div>
          <ul class="perks muted small">
            <li>✔ Hàng chính hãng, kèm hộp & giấy chứng nhận</li><li>✔ Đặt cọc 30% hoặc trả góp 0% (đơn từ 1 triệu)</li><li>✔ Đổi trả 7 ngày nếu lỗi nhà sản xuất</li>
          </ul>
        </div>
      </div>

      <div class="grid grid-2 section">
        <div class="card"><h3>Mô tả sản phẩm</h3><p class="desc">{{ p.description }}</p></div>
        <div class="card"><h3>Thông số kỹ thuật</h3>
          <table class="table"><tbody>
            <tr v-for="(v, k) in p.specs" :key="k"><th>{{ k }}</th><td>{{ v }}</td></tr>
            <tr><th>Chất liệu</th><td>{{ p.material }}</td></tr>
          </tbody></table>
        </div>
      </div>

      <section v-if="related.length" class="section">
        <div class="section-head"><h2>Cùng <span>thương hiệu</span></h2></div>
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

const route = useRoute(), router = useRouter(), cart = useCart()
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

async function load() {
  p.value = null; added.value = false; qty.value = 1; view.value = 'design'
  Object.assign(sel, { paint: 'original', wheel: 'standard', interior: 'black' })
  const [d, o] = await Promise.all([api.get(`/products/${route.params.id}`), api.get('/options')])
  p.value = d.data.product; related.value = d.data.related; opts.value = o.data.customizer
}
function add(buyNow) {
  const v = p.value
  cart.add({
    productId: v.id, name: v.name, scale: v.scale, type: v.type, baseColor: paintHex.value, wheelHex: wheel.value.hex, interiorHex: interior.value.hex,
    image: v.images?.[0], unitPrice: unitPrice.value, quantity: qty.value, stock: v.stock,
    options: canCustomize.value ? { ...sel } : {},
    optionLabel: canCustomize.value ? `${paint.value.name} · ${wheel.value.name} · Nội thất ${interior.value.name}` : ''
  })
  if (buyNow) router.push('/cart'); else added.value = true
}
watch(() => route.params.id, id => id && load(), { immediate: true })
</script>

<style scoped>
.crumb { margin-bottom: 18px; }
.top { align-items: start; }
.main-img { padding: 12px; } .photo { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; }
.thumbs { margin-top: 10px; }
.thumb { background: var(--card); border: 2px solid var(--border); border-radius: 10px; color: var(--text); padding: 8px 12px; cursor: pointer; font-family: inherit; height: 60px; }
.thumb img { height: 40px; } .thumb.on { border-color: var(--accent); }
.title { font-size: 2rem; margin: 10px 0 6px; }
.pricebox { margin: 16px 0; } .big { font-size: 2rem; }
.designer { margin-bottom: 18px; padding: 18px; } .opt { margin-bottom: 14px; }
.buy { margin-bottom: 12px; } .ok { color: #86efac; } .out { color: #fca5a5; }
.perks { list-style: none; margin-top: 16px; } .perks li { margin-bottom: 4px; }
.desc { color: var(--label); white-space: pre-line; }
.table th { width: 45%; text-transform: none; font-size: .88rem; }
</style>
