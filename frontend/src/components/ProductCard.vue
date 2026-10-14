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
      <div class="tags">
        <span v-if="p.isNew" class="badge badge-new">Mới</span>
        <span v-if="p.oldPrice" class="badge badge-sale">−{{ Math.round(100 - p.price * 100 / p.oldPrice) }}%</span>
      </div>
    </router-link>
    <div class="body">
      <div class="meta">
        <img v-if="p.brand?.logoUrl" :src="p.brand.logoUrl" alt="" />
        <span>{{ p.brand?.name }}</span><span class="dot">·</span><span>{{ TYPES[p.type] }}</span>
        <span v-if="p.year" class="dot">·</span><span v-if="p.year">{{ p.year }}</span>
      </div>
      <router-link :to="`/products/${p.id}`" class="name">{{ p.name }}</router-link>
      <div v-if="ratings[p.id]" class="rate"><Stars :value="ratings[p.id][0]" size=".85rem" /><span>{{ ratings[p.id][0] }} ({{ ratings[p.id][1] }})</span></div>
      <div v-if="p.type !== 'ACCESSORY'" class="specs">
        <span v-if="p.specs?.['Công suất']">{{ p.specs['Công suất'] }}</span>
        <span v-if="p.fuel">{{ p.fuel }}</span>
        <span v-if="p.specs?.['Số chỗ ngồi']">{{ p.specs['Số chỗ ngồi'] }}</span>
      </div>
      <div class="foot">
        <div>
          <small>{{ p.type === 'ACCESSORY' ? 'Giá' : 'Giá niêm yết từ' }}</small>
          <div class="price">{{ vnd(p.price) }}</div><span v-if="p.oldPrice" class="old-price">{{ vnd(p.oldPrice) }}</span>
          <small :class="p.stock > 0 ? 'ok' : 'out'">{{ p.stock > 0 ? `Còn ${p.stock} ${p.type === 'ACCESSORY' ? 'sản phẩm' : 'xe'}` : 'Tạm hết hàng' }}</small>
        </div>
        <button class="add" :disabled="p.stock <= 0" title="Thêm vào giỏ" @click="add">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { vnd, TYPES } from '../format'
import { useCart } from '../stores/cart'
import ProductImage from './ProductImage.vue'
import Stars from './Stars.vue'
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
.pcard { background: var(--card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; display: flex; flex-direction: column; transition: transform .3s, border-color .3s, box-shadow .3s; }
.pcard:hover { transform: translateY(-4px); border-color: rgba(56,189,248,.35); box-shadow: 0 24px 48px -24px rgba(2,132,199,.55); }
.thumb { position: relative; display: block; overflow: hidden; aspect-ratio: 16/10; }
.thumb :deep(.pimg) { width: 100%; height: 100%; aspect-ratio: auto; border-radius: 0; transition: transform .6s ease; }
.pcard:hover .thumb :deep(.pimg) { transform: scale(1.05); }
.tags { position: absolute; top: 16px; left: 16px; display: flex; gap: 8px; }
.body { padding: 24px; display: flex; flex-direction: column; gap: 8px; flex: 1; }
.meta { white-space: nowrap; overflow: hidden; display: flex; align-items: center; gap: 6px; color: var(--muted); font-size: .8rem; font-weight: 600; text-transform: uppercase; letter-spacing: .06em; }
.meta img { height: 16px; width: 16px; object-fit: contain; opacity: .8; margin-right: 2px; }
.dot { opacity: .5; }
.name { color: var(--text); font-weight: 700; font-size: 1.2rem; line-height: 1.3; letter-spacing: -.01em; }
.name:hover { color: var(--accent); }
.specs { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
.specs span { font-size: .8rem; color: #cbd5e1; padding: 4px 10px; border: 1px solid var(--border); border-radius: 999px; }
.foot { margin-top: auto; padding-top: 16px; border-top: 1px solid var(--border); display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; }
.foot small { display: block; color: var(--muted); font-size: .8rem; }
.foot .price { font-size: 1.3rem; }
.foot .old-price { display: block; margin: 0; font-size: .8rem; }
.ok { color: #86efac !important; margin-top: 4px; } .out { color: #fca5a5 !important; margin-top: 4px; }
.add { width: 44px; height: 44px; border-radius: 50%; border: 1px solid rgba(255,255,255,.2); background: transparent; color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: .2s; flex-shrink: 0; }
.add:hover { background: var(--primary); border-color: var(--primary); }
.add:disabled { opacity: .4; cursor: not-allowed; }
.tools { position: absolute; top: 16px; right: 16px; display: flex; gap: 8px; opacity: 0; transform: translateY(-4px); transition: .25s; }
.pcard:hover .tools, .tools:has(.on) { opacity: 1; transform: none; }
.tool { width: 38px; height: 38px; border-radius: 50%; border: none; background: rgba(10,11,14,.7); backdrop-filter: blur(8px); color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: .2s; }
.tool:hover { background: rgba(10,11,14,.95); } .tool.on { color: #f472b6; } .tool.on + .tool.on, .tool:nth-child(2).on { color: var(--accent); }
.rate { display: flex; align-items: center; gap: 8px; font-size: .8rem; color: var(--muted); }
</style>
