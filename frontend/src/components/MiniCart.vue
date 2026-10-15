<template>
  <Teleport to="body">
    <transition name="mc">
      <div v-if="cartOpen" class="mc-wrap" @keydown.esc="cartOpen = false">
        <div class="mc-dim" @click="cartOpen = false" />
        <aside ref="panel" class="mc" role="dialog" aria-modal="true" aria-label="Giỏ hàng nhanh" tabindex="-1">
          <header><h3>Giỏ hàng <small>({{ cart.count }})</small></h3><button class="x" aria-label="Đóng" @click="cartOpen = false">×</button></header>
          <div v-if="remain > 0 && cart.items.length" class="fs"><span>Mua thêm <b>{{ vnd(remain) }}</b> để được miễn phí vận chuyển</span><div class="bar"><i :style="{ width: pct + '%' }" /></div></div>
          <div v-else-if="cart.items.length" class="fs ok">🎉 Đơn của bạn được <b>miễn phí vận chuyển</b></div>
          <div v-if="!cart.items.length" class="empty"><p>Giỏ hàng đang trống.</p><router-link to="/products" class="btn btn-sm" @click="cartOpen = false">Khám phá sản phẩm</router-link></div>
          <ul v-else class="lines">
            <li v-for="i in cart.items" :key="i.key">
              <img :src="i.image" alt="" />
              <div class="info"><router-link :to="`/products/${i.productId}`" class="nm" @click="cartOpen = false">{{ i.name }}</router-link>
                <small v-if="i.variantName" class="muted">{{ i.variantName }}</small>
                <div class="qrow"><div class="qty"><button :aria-label="`Giảm ${i.name}`" @click="cart.setQty(i.key, i.quantity - 1)">−</button><span>{{ i.quantity }}</span><button :aria-label="`Tăng ${i.name}`" @click="cart.setQty(i.key, i.quantity + 1)">+</button></div>
                  <b>{{ vnd(i.unitPrice * i.quantity) }}</b></div></div>
              <button class="rm" :aria-label="`Xoá ${i.name}`" @click="cart.remove(i.key)">×</button>
            </li>
          </ul>
          <footer v-if="cart.items.length">
            <div v-if="bundleOff" class="row between ok small"><span>Combo ưu đãi</span><span>−{{ vnd(bundleOff) }}</span></div>
            <div class="row between sub"><span>Tạm tính</span><b class="price">{{ vnd(cart.subtotal - bundleOff) }}</b></div>
            <div class="acts"><router-link to="/cart" class="btn btn-outline" @click="cartOpen = false">Xem giỏ</router-link><router-link to="/checkout" class="btn" @click="cartOpen = false">Thanh toán</router-link></div>
          </footer>
        </aside>
      </div>
    </transition>
  </Teleport>
</template>
<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { useCart } from '../stores/cart'
import { cartOpen } from '../stores/ui'
import { vnd, catalog } from '../format'
const cart = useCart(), route = useRoute(), panel = ref(), bundleOff = ref(0)
const remain = computed(() => Math.max(0, catalog.options.freeShipFrom - cart.subtotal))
const pct = computed(() => Math.min(100, cart.subtotal * 100 / (catalog.options.freeShipFrom || 1)))
watch(cartOpen, async v => { document.documentElement.style.overflow = v ? 'hidden' : ''; if (v) { await nextTick(); panel.value?.focus(); quote() } })
watch(() => route.fullPath, () => { cartOpen.value = false })
watch(() => cart.items.map(i => i.key + i.quantity).join(), () => cartOpen.value && quote())
async function quote() { bundleOff.value = cart.items.length ? (await api.post('/public/bundle-quote', cart.items.map(i => ({ productId: i.productId, variantId: i.variantId, quantity: i.quantity }))).catch(() => ({ data: { discount: 0 } }))).data.discount : 0 }
</script>
<style scoped>
.mc-wrap { position: fixed; inset: 0; z-index: 200; }
.mc-dim { position: absolute; inset: 0; background: rgba(0,0,0,.4); }
.mc { position: absolute; right: 0; top: 0; bottom: 0; width: min(400px, 100vw); background: var(--surface); color: var(--text); display: flex; flex-direction: column; box-shadow: -20px 0 40px -10px rgba(0,0,0,.3); outline: none; }
header { display: flex; justify-content: space-between; align-items: center; padding: 16px 18px; border-bottom: 1px solid var(--border); }
header small { color: var(--muted); font-weight: 500; } .x { background: none; border: none; font-size: 1.6rem; line-height: 1; cursor: pointer; color: var(--text); width: 36px; height: 36px; }
.fs { padding: 10px 18px; font-size: .82rem; background: var(--bg-2); } .fs.ok { color: var(--success); }
.bar { height: 5px; background: var(--border); margin-top: 6px; overflow: hidden; } .bar i { display: block; height: 100%; background: var(--accent); transition: width .3s ease-out; }
.empty { padding: 40px 18px; text-align: center; display: flex; flex-direction: column; gap: 12px; align-items: center; }
.lines { list-style: none; flex: 1; overflow-y: auto; padding: 4px 18px; }
.lines li { display: flex; gap: 10px; padding: 12px 0; border-bottom: 1px solid var(--border); align-items: flex-start; }
.lines img { width: 64px; height: 48px; object-fit: cover; flex-shrink: 0; background: var(--bg-2); }
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; } .nm { font-weight: 600; font-size: .86rem; color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.qrow { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; gap: 8px; }
.qty { display: inline-flex; border: 1px solid var(--border); } .qty button { width: 28px; height: 28px; border: none; background: none; cursor: pointer; color: var(--text); font-size: 1rem; } .qty span { min-width: 26px; text-align: center; line-height: 28px; font-size: .85rem; }
.rm { background: none; border: none; color: var(--muted); cursor: pointer; font-size: 1.2rem; width: 28px; height: 28px; } .rm:hover { color: var(--danger); }
footer { border-top: 1px solid var(--border); padding: 14px 18px calc(14px + env(safe-area-inset-bottom)); display: flex; flex-direction: column; gap: 10px; }
.sub { font-size: 1rem; } .acts { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; } .acts .btn { text-align: center; justify-content: center; }
.ok { color: var(--success); }
.mc-enter-active, .mc-leave-active { transition: opacity .2s ease-out; } .mc-enter-active .mc, .mc-leave-active .mc { transition: transform .22s ease-out; }
.mc-enter-from, .mc-leave-to { opacity: 0; } .mc-enter-from .mc, .mc-leave-to .mc { transform: translateX(100%); }
@media (prefers-reduced-motion: reduce) { .mc-enter-active, .mc-leave-active, .mc-enter-active .mc, .mc-leave-active .mc { transition: none; } }
</style>
