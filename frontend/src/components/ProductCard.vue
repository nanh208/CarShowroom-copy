<template>
  <div class="pcard">
    <router-link :to="`/products/${p.id}`" class="thumb">
      <ProductImage :product="p" />
      <div class="tags">
        <span v-if="p.isNew" class="badge badge-new">MỚI</span>
        <span v-if="p.oldPrice" class="badge badge-sale">-{{ Math.round(100 - p.price * 100 / p.oldPrice) }}%</span>
      </div>
    </router-link>
    <div class="body">
      <div class="muted small">{{ p.brand?.name }} · {{ TYPES[p.type] }}</div>
      <router-link :to="`/products/${p.id}`" class="name">{{ p.name }}</router-link>
      <div><span class="price">{{ vnd(p.price) }}</span><span v-if="p.oldPrice" class="old-price">{{ vnd(p.oldPrice) }}</span></div>
      <div class="row between small">
        <span :class="p.stock > 0 ? 'stock-ok' : 'stock-out'">{{ p.stock > 0 ? `Còn ${p.stock} sản phẩm` : 'Hết hàng' }}</span>
        <span class="muted">Đã bán {{ p.sold }}</span>
      </div>
      <button class="btn btn-sm btn-block" :disabled="p.stock <= 0" @click="add">🛒 Thêm vào giỏ</button>
    </div>
  </div>
</template>

<script setup>
import { vnd, TYPES } from '../format'
import { useCart } from '../stores/cart'
import ProductImage from './ProductImage.vue'
const props = defineProps({ p: Object })
const cart = useCart()
const add = () => {
  const p = props.p
  cart.add({ productId: p.id, name: p.name, scale: p.scale, type: p.type, baseColor: p.baseColor, image: p.images?.[0],
    unitPrice: p.price, quantity: 1, stock: p.stock, options: {}, optionLabel: p.type === 'ACCESSORY' ? '' : 'Màu nguyên bản' })
}
</script>

<style scoped>
.pcard { background: var(--card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; display: flex; flex-direction: column; transition: .25s; }
.pcard:hover { transform: translateY(-4px); border-color: rgba(56,189,248,.35); box-shadow: 0 18px 40px -18px rgba(2,132,199,.5); }
.thumb { position: relative; display: block; padding: 10px 10px 0; }
.tags { position: absolute; top: 18px; left: 18px; display: flex; gap: 6px; }
.body { padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 8px; flex: 1; }
.name { color: var(--text); font-weight: 700; font-size: 1.02rem; min-height: 2.6em; }
.name:hover { color: var(--accent); }
.stock-ok { color: #86efac; } .stock-out { color: #fca5a5; }
.body .btn { margin-top: auto; }
</style>
