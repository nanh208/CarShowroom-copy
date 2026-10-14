<template>
  <div class="container page">
    <h1 style="margin-bottom:20px">Giỏ hàng</h1>
    <div v-if="!cart.items.length" class="empty card">Giỏ hàng đang trống. <router-link to="/products">Tiếp tục mua sắm →</router-link></div>
    <div v-else class="layout">
      <div class="card">
        <div v-for="i in cart.items" :key="i.key" class="line">
          <div class="img"><img v-if="i.image" :src="i.image" class="cimg" /><CarPreview v-else :body="i.baseColor" :wheel="i.wheelHex" :interior="i.interiorHex" :type="i.type" /></div>
          <div class="grow">
            <router-link :to="`/products/${i.productId}`" class="name">{{ i.name }}</router-link>
            <div class="muted small">Đơn giá {{ vnd(i.unitPrice) }}</div>
            <div class="muted small">{{ i.type === 'ACCESSORY' ? 'Phụ kiện' : i.fuel }}<span v-if="i.optionLabel"> · {{ i.optionLabel }}</span></div>
                      </div>
          <div class="qty"><button @click="cart.setQty(i.key, i.quantity - 1)">−</button><span>{{ i.quantity }}</span><button @click="cart.setQty(i.key, i.quantity + 1)">+</button></div>
          <b class="sum">{{ vnd(i.unitPrice * i.quantity) }}</b>
          <button class="btn btn-sm btn-outline" title="Xoá" @click="cart.remove(i.key)">Xoá</button>
        </div>
        <div class="row between" style="margin-top:14px">
          <router-link to="/products">← Tiếp tục mua sắm</router-link>
          <button class="btn btn-sm btn-outline" @click="cart.clear()">Xoá giỏ hàng</button>
        </div>
      </div>
      <div class="card summary">
        <h3>Tóm tắt</h3>
        <div class="row between"><span>Tạm tính ({{ cart.count }} sp)</span><b>{{ vnd(cart.subtotal) }}</b></div>
        <div class="row between muted small"><span>Giao xe</span><span>Miễn phí (giao xe tận nhà)</span></div>
        <router-link to="/checkout" class="btn btn-block" style="margin-top:16px">Tiến hành thanh toán →</router-link>
      </div>
    </div>
  </div>
</template>
<script setup>
import { useCart } from '../stores/cart'
import { vnd } from '../format'
import CarPreview from '../components/CarPreview.vue'
const cart = useCart()
</script>
<style scoped>
.layout { display: grid; grid-template-columns: 1fr 380px; gap: 32px; align-items: start; }
@media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
.line { display: flex; gap: 24px; align-items: center; padding: 24px 0; border-bottom: 1px solid var(--border); flex-wrap: nowrap; }
.line .grow { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.line .btn-sm { flex-shrink: 0; } .line .qty { flex-shrink: 0; }
.img { width: 176px; flex-shrink: 0; } .cimg { width: 176px; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; } .name { color: var(--text); font-weight: 700; font-size: 1.1rem; }
.sum { width: 150px; flex-shrink: 0; text-align: right; font-weight: 700; font-size: 1.05rem; }
.summary { display: flex; flex-direction: column; gap: 16px; position: sticky; top: 96px; }
</style>
