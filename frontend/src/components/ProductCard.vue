<template>
  <div class="pcard">
    <router-link :to="`/products/${p.id}`" class="thumb">
      <ProductImage :product="p" />
      <div class="tools" @click.prevent>
        <button :class="['tool', { on: prefs.isWished(p.id) }]" title="Yêu thích" @click.prevent="prefs.toggleWish(p)">
          <svg viewBox="0 0 24 24" width="18" height="18" :fill="prefs.isWished(p.id) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z"/></svg></button>
        <button v-if="p.type !== 'ACCESSORY'" :class="['tool', { on: prefs.inCompare(p.id) }]" title="So sánh" @click.prevent="prefs.toggleCompare(p)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M8 21H3v-5M21 3l-7 7M3 21l7-7"/></svg></button>
      </div>
    </router-link>
    <div class="body">
      <router-link :to="`/products/${p.id}`" class="name">{{ p.name }}</router-link>
      <div class="tagrow">
        <span class="badge tag-yellow">{{ p.isNew ? 'Mới' : p.oldPrice ? `Giảm ${Math.round(100 - p.price * 100 / p.oldPrice)}%` : TYPES[p.type] }}</span>
        <span v-if="ratings[p.id]" class="rate">★ {{ ratings[p.id][0] }}</span>
        <span v-if="p.stock <= 0" class="out">Hết hàng</span>
      </div>
      <div class="foot">
        <div><div class="price">{{ vnd(p.price) }}</div><span v-if="p.oldPrice" class="old-price">{{ vnd(p.oldPrice) }}</span></div>
        <button class="btn btn-red btn-sm buy" :disabled="p.stock <= 0" @click="add">Mua</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { vnd, TYPES } from '../format'
import { useCart } from '../stores/cart'
import ProductImage from './ProductImage.vue'
import { usePrefs, ratings, loadRatings, toast } from '../stores/prefs'
const prefs = usePrefs(); loadRatings()
const props = defineProps({ p: Object })
const cart = useCart()
const add = () => {
  const p = props.p
  cart.add({ productId: p.id, name: p.name, fuel: p.fuel, type: p.type, baseColor: p.baseColor, image: p.images?.[0],
    unitPrice: p.price, quantity: 1, stock: p.stock, options: {}, optionLabel: p.type === 'ACCESSORY' ? '' : 'Màu nguyên bản' })
  toast(`Đã thêm “${p.name}” vào giỏ hàng`)
}
</script>

<style scoped>
.pcard { background: #fff; border-radius: 10px; display: flex; flex-direction: column; transition: transform .25s; }
.pcard:hover { transform: translateY(-3px); }
.thumb { position: relative; display: block; overflow: hidden; aspect-ratio: 4/3; background: var(--tile); border-radius: 10px; }
.thumb :deep(.pimg) { width: 100%; height: 100%; aspect-ratio: auto; border-radius: 0; transition: transform .5s ease; }
.pcard:hover .thumb :deep(.pimg) { transform: scale(1.04); }
.body { padding: 10px 2px 4px; display: flex; flex-direction: column; gap: 6px; flex: 1; }
.name { color: var(--text); font-weight: 600; font-size: .98rem; line-height: 1.3; min-height: 2.6em; }
.name:hover { color: var(--bronze); }
.tagrow { display: flex; gap: 6px; align-items: center; font-size: .75rem; }
.rate { color: var(--muted); } .out { color: var(--danger); font-weight: 600; }
.foot { margin-top: auto; display: flex; justify-content: space-between; align-items: flex-end; gap: 8px; }
.foot .price { font-size: 1.05rem; font-weight: 700; color: var(--text); }
.foot .old-price { display: block; margin: 0; font-size: .78rem; }
.buy { padding: 7px 18px; }
.tools { position: absolute; top: 10px; right: 10px; display: flex; gap: 6px; opacity: 0; transition: .25s; }
.pcard:hover .tools, .tools:has(.on) { opacity: 1; }
.tool { width: 34px; height: 34px; border-radius: 50%; border: none; background: #fff; color: var(--text); cursor: pointer; display: flex; align-items: center; justify-content: center; }
.tool.on { color: #e53935; } .tool:nth-child(2).on { color: var(--bronze); }
</style>
