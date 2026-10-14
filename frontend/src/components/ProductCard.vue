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
      <div class="meta">{{ p.brand?.name }} · {{ p.scale }}</div>
      <router-link :to="`/products/${p.id}`" class="name">{{ p.name }}</router-link>
      <div class="actions">
        <button class="btn btn-sm gold" :disabled="p.stock <= 0" @click="add">{{ vnd(p.price) }}</button>
        <button class="btn btn-sm btn-red" :disabled="p.stock <= 0" @click="buy">Mua</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { vnd } from '../format'
import { useCart } from '../stores/cart'
import { useRouter } from 'vue-router'
import ProductImage from './ProductImage.vue'
const props = defineProps({ p: Object })
const cart = useCart(), router = useRouter()
const line = () => ({ productId: props.p.id, name: props.p.name, scale: props.p.scale, type: props.p.type, baseColor: props.p.baseColor,
  image: props.p.images?.[0], unitPrice: props.p.price, quantity: 1, stock: props.p.stock, options: {}, optionLabel: props.p.type === 'ACCESSORY' ? '' : 'Màu nguyên bản' })
const add = () => cart.add(line())
const buy = () => { cart.add(line()); router.push('/cart') }
</script>
<style scoped>
.pcard { background: #fff; border: 1px solid var(--line); display: flex; flex-direction: column; transition: .2s; }
.pcard:hover { box-shadow: 0 6px 20px rgba(0,0,0,.1); transform: translateY(-2px); }
.thumb { position: relative; display: block; background: #f7f7f7; aspect-ratio: 4/3; overflow: hidden; }
.thumb :deep(.pimg), .thumb :deep(.car-preview) { width: 100%; height: 100%; aspect-ratio: auto; border-radius: 0; object-fit: cover; }
.tags { position: absolute; top: 8px; left: 8px; display: flex; gap: 4px; }
.body { padding: 12px 12px 14px; display: flex; flex-direction: column; gap: 6px; }
.meta { font-size: .75rem; color: var(--muted); text-transform: uppercase; letter-spacing: .04em; }
.name { color: var(--text); font-weight: 700; font-size: .95rem; min-height: 2.5em; line-height: 1.3; }
.name:hover { color: var(--bronze); }
.actions { display: grid; grid-template-columns: 1.4fr 1fr; gap: 6px; margin-top: 4px; }
.gold { background: var(--bronze); }
</style>
