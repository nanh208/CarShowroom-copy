<template>
  <div class="container page">
    <h1 style="margin-bottom:20px">Giỏ hàng</h1>
    <div v-if="!cart.items.length" class="empty card">Giỏ hàng đang trống. <router-link to="/products">Tiếp tục mua sắm →</router-link></div>
    <div v-else class="layout">
      <div class="card">
        <div v-for="i in cart.items" :key="i.key" class="line">
          <div class="img"><CarPreview :body="i.baseColor" :wheel="i.wheelHex" :interior="i.interiorHex" :type="i.type" /></div>
          <div class="grow">
            <router-link :to="`/products/${i.productId}`" class="name">{{ i.name }}</router-link>
            <div class="muted small">{{ i.type === 'ACCESSORY' ? 'Phụ kiện' : 'Tỉ lệ ' + i.scale }}<span v-if="i.optionLabel"> · {{ i.optionLabel }}</span></div>
            <div class="price">{{ vnd(i.unitPrice) }}</div>
          </div>
          <div class="qty"><button @click="cart.setQty(i.key, i.quantity - 1)">−</button><span>{{ i.quantity }}</span><button @click="cart.setQty(i.key, i.quantity + 1)">+</button></div>
          <b class="sum">{{ vnd(i.unitPrice * i.quantity) }}</b>
          <button class="btn btn-sm btn-outline" title="Xoá" @click="cart.remove(i.key)">🗑</button>
        </div>
        <div class="row between" style="margin-top:14px">
          <router-link to="/products">← Tiếp tục mua sắm</router-link>
          <button class="btn btn-sm btn-outline" @click="cart.clear()">Xoá giỏ hàng</button>
        </div>
      </div>
      <div class="card summary">
        <h3>Tóm tắt</h3>
        <div class="row between"><span>Tạm tính ({{ cart.count }} sp)</span><b>{{ vnd(cart.subtotal) }}</b></div>
        <div class="row between muted small"><span>Phí vận chuyển</span><span>{{ cart.subtotal >= 2000000 ? 'Miễn phí' : vnd(40000) }}</span></div>
        <p v-if="cart.subtotal < 2000000" class="small muted">Mua thêm {{ vnd(2000000 - cart.subtotal) }} để được miễn phí vận chuyển.</p>
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
.layout { display: grid; grid-template-columns: 1fr 340px; gap: 24px; align-items: start; }
@media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
.line { display: flex; gap: 16px; align-items: center; padding: 14px 0; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
.img { width: 120px; } .name { color: var(--text); font-weight: 700; }
.sum { width: 120px; text-align: right; }
.summary { display: flex; flex-direction: column; gap: 10px; position: sticky; top: 150px; }
</style>
