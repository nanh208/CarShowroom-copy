<template>
  <div class="pcard">
    <router-link ref="thumb" :to="`/products/${p.id}`" class="thumb">
      <ProductImage :product="p" />
      <span v-if="flashPrice" class="flash">⚡ Flash sale</span>
      <div class="tools" @click.prevent>
        <button :class="['tool wish', { on: prefs.isWished(p.id) }]" :aria-pressed="prefs.isWished(p.id)" title="Yêu thích" @click.prevent="prefs.toggleWish(p)">
          <svg viewBox="0 0 24 24" width="18" height="18" :fill="prefs.isWished(p.id) ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z"/></svg></button>
        <button :class="['tool', { on: prefs.inCompare(p.id) }]" title="So sánh" @click.prevent="prefs.toggleCompare(p)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M8 21H3v-5M21 3l-7 7M3 21l7-7"/></svg></button>
      </div>
    </router-link>
    <div class="body">
      <router-link :to="`/products/${p.id}`" class="name">{{ p.name }}</router-link>
      <div class="tagrow">
        <span class="badge tag-yellow">{{ tag }}</span>
        <span v-if="p.scale" class="meta">{{ p.scale }}</span>
        <span class="meta">{{ p.ageMin }}+</span>
        <span v-if="p.reviewCount" class="rate">★ {{ p.rating }}</span>
        <span v-if="p.stock <= 0" class="out">Hết hàng</span>
      </div>
      <div class="foot">
        <div>
          <div class="price"><small v-if="p.hasVariants">từ </small>{{ vnd(flashPrice || p.price) }}</div>
          <span v-if="flashPrice || p.oldPrice" class="old-price">{{ vnd(flashPrice ? p.price : p.oldPrice) }}</span>
        </div>
        <button class="btn btn-red btn-sm buy" :disabled="p.stock <= 0 || busy" :aria-label="`Thêm ${p.name} vào giỏ`" @click="add">{{ busy ? '…' : '+ Giỏ' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { vnd, TYPES } from '../format'
import { useCart } from '../stores/cart'
import ProductImage from './ProductImage.vue'
import { usePrefs, toast } from '../stores/prefs'
import { flyToCart } from '../motion'
import api from '../api'
const prefs = usePrefs(), router = useRouter(), thumb = ref(null)
const props = defineProps({ p: Object, flash: Object })
const cart = useCart()
const flashPrice = computed(() => props.flash?.live !== false && props.flash?.salePrice ? props.flash.salePrice : null)
const tag = computed(() => {
  const p = props.p, base = flashPrice.value ? p.price : p.oldPrice
  if (base) return `Giảm ${Math.round(100 - (flashPrice.value || p.price) * 100 / base)}%`
  return p.isNew ? 'Mới' : TYPES[p.type] || p.type
})
const busy = ref(false)
// Thêm nhanh: sản phẩm có biến thể thì lấy biến thể còn hàng rẻ nhất (đổi được trong giỏ / trang chi tiết)
const add = async (e) => {
  const p = props.p, el = e.currentTarget
  if (p.hasVariants) {
    busy.value = true
    try {
      const { data } = await api.get(`/products/${p.id}`)
      const v = (data.variants || []).filter(x => x.active !== false && x.stock > 0).sort((a, b) => a.price - b.price)[0]
      if (!v) return router.push(`/products/${p.id}`)
      cart.add({ productId: p.id, variantId: v.id, variantName: v.name, name: p.name, image: v.image || p.images?.[0], unitPrice: v.price, oldPrice: v.oldPrice,
        quantity: 1, stock: v.stock })
      flyToCart(v.image || p.images?.[0], thumb.value?.$el || el)
      toast(`Đã thêm “${p.name} – ${v.name}” vào giỏ`)
    } catch { router.push(`/products/${p.id}`) } finally { busy.value = false }
    return
  }
  cart.add({ productId: p.id, name: p.name, image: p.images?.[0], unitPrice: flashPrice.value || p.price, oldPrice: p.oldPrice,
    quantity: 1, stock: p.stock, flash: !!flashPrice.value })
  flyToCart(p.images?.[0], thumb.value?.$el || el)
  toast(`Đã thêm “${p.name}” vào giỏ hàng`)
}
</script>

<style scoped>
.pcard { position: relative; background: #fff; border-radius: 10px; display: flex; flex-direction: column; }
.thumb { position: relative; display: block; overflow: hidden; aspect-ratio: 4/3; background: var(--tile); border-radius: 10px; }
.thumb :deep(.pimg) { width: 100%; height: 100%; aspect-ratio: auto; border-radius: 0; }
.flash { position: absolute; left: 10px; top: 10px; background: #c62828; color: #fff; font-size: .72rem; font-weight: 700; padding: 3px 8px; border-radius: 4px; }
.body { padding: 10px 2px 4px; display: flex; flex-direction: column; gap: 6px; flex: 1; }
.name { color: var(--text); font-weight: 600; font-size: .95rem; line-height: 1.3; min-height: 2.6em; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.name { transition: color .15s ease-out; } .name:hover { color: var(--bronze); }
.tagrow { display: flex; gap: 6px; align-items: center; font-size: .75rem; flex-wrap: wrap; }
.meta { color: var(--muted); border: 1px solid var(--border); border-radius: 4px; padding: 0 5px; }
.rate { color: #b58a00; font-weight: 600; } .out { color: var(--danger); font-weight: 600; }
.foot { margin-top: auto; display: flex; justify-content: space-between; align-items: flex-end; gap: 8px; }
.foot .price { font-size: 1.05rem; font-weight: 700; color: var(--text); } .price small { font-weight: 500; color: var(--muted); font-size: .75rem; }
.foot .old-price { display: block; margin: 0; font-size: .78rem; }
.buy { padding: 7px 18px; flex-shrink: 0; }
.foot > div { min-width: 0; }
.body, .info { container-type: inline-size; }
@container (max-width: 190px) { .foot { flex-wrap: wrap; } .foot .buy { width: 100%; padding: 8px 10px; } .foot .price { font-size: .95rem; } }
.tools { position: absolute; top: 10px; right: 10px; display: flex; gap: 6px; }
.tool { width: 32px; height: 32px; border-radius: 50%; border: none; background: rgba(255,255,255,.92); transition: color .15s ease-out; color: var(--text); cursor: pointer; display: flex; align-items: center; justify-content: center; }
.tool.on { color: #e53935; } .tool:nth-child(2).on { color: var(--bronze); }
</style>
