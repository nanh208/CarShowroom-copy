<template>
  <div class="container page">
    <h1 style="margin-bottom:20px">Giỏ hàng</h1>
    <div v-if="!cart.items.length && !cart.saved.length" class="empty card"><div class="spinner idle" />Giỏ hàng đang trống. <router-link to="/products">Tiếp tục mua sắm →</router-link></div>
    <div v-else class="layout">
      <div>
        <div class="card">
          <div v-if="!cart.items.length" class="muted">Giỏ hàng trống — bạn có sản phẩm đã lưu bên dưới.</div>
          <transition-group name="line" tag="div">
            <div v-for="i in cart.items" :key="i.key" class="line">
              <img :src="i.image" class="cimg" alt="" />
              <div class="grow">
                <router-link :to="`/products/${i.productId}`" class="name">{{ i.name }}</router-link>
                <div v-if="i.variantName" class="muted small">Phân loại: {{ i.variantName }}</div>
                <div class="muted small">Đơn giá {{ vnd(i.unitPrice) }} <s v-if="i.oldPrice">{{ vnd(i.oldPrice) }}</s> <span v-if="i.flash" class="fl">⚡ Flash sale</span></div>
                <button class="link" @click="cart.saveForLater(i.key)">Lưu để mua sau</button>
              </div>
              <div class="qty"><button @click="cart.setQty(i.key, i.quantity - 1)">−</button><span>{{ i.quantity }}</span><button @click="cart.setQty(i.key, i.quantity + 1)">+</button></div>
              <b class="sum">{{ vnd(i.unitPrice * i.quantity) }}</b>
              <button class="btn btn-sm btn-outline" title="Xoá" @click="cart.remove(i.key)">Xoá</button>
            </div>
          </transition-group>
          <div class="row between" style="margin-top:14px">
            <router-link to="/products">← Tiếp tục mua sắm</router-link>
            <button v-if="cart.items.length" class="btn btn-sm btn-outline" @click="cart.clear()">Xoá giỏ hàng</button>
          </div>
        </div>
        <div v-if="cart.saved.length" class="card saved">
          <h3>Đã lưu để mua sau ({{ cart.saved.length }})</h3>
          <div v-for="i in cart.saved" :key="i.key" class="line">
            <img :src="i.image" class="cimg" alt="" />
            <div class="grow"><router-link :to="`/products/${i.productId}`" class="name">{{ i.name }}</router-link><div class="muted small">{{ i.variantName }} {{ vnd(i.unitPrice) }}</div></div>
            <button class="btn btn-sm" @click="cart.moveToCart(i.key)">Thêm vào giỏ</button>
            <button class="btn btn-sm btn-outline" @click="cart.removeSaved(i.key)">Xoá</button>
          </div>
        </div>
      </div>
      <div class="card summary">
        <h3>Tóm tắt</h3>
        <div class="row between"><span>Tạm tính ({{ cart.count }} sp)</span><b>{{ vnd(cart.subtotal) }}</b></div>
        <div class="ship">
          <div class="road"><div class="fill" :style="{ width: Math.min(100, cart.subtotal * 100 / free) + '%' }" /><span class="car" :style="{ left: Math.min(100, cart.subtotal * 100 / free) + '%' }">🚚</span></div>
          <small v-if="cart.subtotal >= free" class="ok">Đơn hàng được miễn phí vận chuyển!</small>
          <small v-else>Mua thêm <b>{{ vnd(free - cart.subtotal) }}</b> để được <b>freeship</b></small>
        </div>
        <div class="row between muted small"><span>Phí vận chuyển</span><span>Tính theo khu vực ở bước thanh toán</span></div>
        <router-link to="/checkout" :class="['btn btn-block', { disabled: !cart.items.length }]" style="margin-top:16px">Tiến hành thanh toán →</router-link>
        <p class="muted small" style="margin-top:10px">Có mã giảm giá? Nhập ở bước thanh toán. <router-link to="/promotions">Xem voucher</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCart } from '../stores/cart'
import { vnd, catalog, loadCatalog } from '../format'
const cart = useCart(); loadCatalog()
const free = computed(() => catalog.options.freeShipFrom || 500000)
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 1fr 340px; gap: 20px; align-items: start; }
@media (max-width: 860px) { .layout { grid-template-columns: 1fr; } }
.line { display: flex; align-items: center; gap: 14px; padding: 12px 0; border-bottom: 1px solid var(--border); }
.cimg { width: 96px; height: 68px; object-fit: cover; border-radius: 8px; background: var(--tile); }
.name { font-weight: 600; color: var(--text); } .fl { color: var(--danger); font-weight: 600; }
.link { background: none; border: none; padding: 0; color: var(--bronze); font: inherit; font-size: .8rem; cursor: pointer; }
.qty { display: flex; align-items: center; border: 1px solid var(--border); border-radius: 8px; } .qty button { width: 30px; height: 32px; border: none; background: none; cursor: pointer; } .qty span { width: 28px; text-align: center; }
.sum { width: 120px; text-align: right; }
.summary { position: sticky; top: 110px; display: flex; flex-direction: column; gap: 10px; }
.saved { margin-top: 16px; } .saved h3 { margin-bottom: 4px; }
.ship { background: var(--bg-2); border-radius: 10px; padding: 12px; }
.road { position: relative; height: 8px; border-radius: 4px; background: repeating-linear-gradient(90deg, #ccc 0 10px, #e3e3e3 10px 20px); margin: 6px 6px 10px; }
.fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg, var(--bronze), #f5c400); transition: width .8s cubic-bezier(.2,.8,.2,1); }
.car { position: absolute; top: -13px; transform: translateX(-50%) scaleX(-1); transition: left .8s cubic-bezier(.2,.8,.2,1); font-size: 1.1rem; }
.ship small { font-size: .82rem; } .ok { color: var(--success); font-weight: 600; }
.disabled { pointer-events: none; opacity: .5; }
.line-leave-active { transition: .35s; } .line-leave-to { opacity: 0; transform: translateX(60px); }
.idle { animation-duration: 3s; margin: 10px auto 16px; }
</style>
