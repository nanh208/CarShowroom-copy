<template>
  <div class="container page">
    <h1 style="margin-bottom:20px">Giỏ hàng</h1>
    <div v-if="!cart.items.length && !cart.saved.length" class="empty card" style="margin-bottom:16px"><div class="spinner idle" />Giỏ hàng đang trống. <router-link to="/products">Tiếp tục mua sắm →</router-link></div>
    <div v-else class="layout">
      <div>
        <div class="card">
          <div v-if="!cart.items.length" class="muted">Giỏ hàng trống — bạn có sản phẩm đã lưu bên dưới.</div>
          <transition-group name="line" tag="div">
            <div v-for="i in cart.items" :key="i.key" class="line">
              <img loading="lazy" decoding="async" :src="i.image" class="cimg" alt="" />
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
            <img loading="lazy" decoding="async" :src="i.image" class="cimg" alt="" />
            <div class="grow"><router-link :to="`/products/${i.productId}`" class="name">{{ i.name }}</router-link><div class="muted small">{{ i.variantName }} {{ vnd(i.unitPrice) }}</div></div>
            <button class="btn btn-sm" @click="cart.moveToCart(i.key)">Thêm vào giỏ</button>
            <button class="btn btn-sm btn-outline" @click="cart.removeSaved(i.key)">Xoá</button>
          </div>
        </div>
      </div>
      <div class="card summary">
        <h3>Tóm tắt</h3>
        <div class="row between"><span>Tạm tính ({{ cart.count }} sp)</span><b>{{ vnd(cart.subtotal) }}</b></div>
        <div v-if="bundleOff" class="row between ok"><span>Combo ưu đãi</span><b>−{{ vnd(bundleOff) }}</b></div>
        <router-link v-else to="/bundles" class="small">🎁 Xem combo mua 2 tặng 1, giá gói →</router-link>
        <div class="ship">
          <div class="road"><div class="fill" :style="{ width: Math.min(100, cart.subtotal * 100 / free) + '%' }" /><span class="car" :style="{ left: Math.min(100, cart.subtotal * 100 / free) + '%' }">🚚</span></div>
          <small v-if="cart.subtotal >= free" class="ok">Đơn hàng được miễn phí vận chuyển!</small>
          <small v-else>Mua thêm <b>{{ vnd(free - cart.subtotal) }}</b> để được <b>freeship</b></small>
        </div>
        <div class="row between muted small"><span>Phí vận chuyển</span><span>Tính theo khu vực ở bước thanh toán</span></div>
        <router-link to="/checkout" :class="['btn btn-block', { disabled: !cart.items.length }]" style="margin-top:16px">Tiến hành thanh toán →</router-link>
        <p class="muted small" style="margin-top:10px">Mã giảm tốt nhất được tự áp ở bước thanh toán. <router-link to="/promotions">Xem voucher</router-link></p>
      </div>
    </div>
    <section v-if="auth.loggedIn" class="card scarts">
      <div class="row between"><h3>Giỏ hàng đã lưu</h3>
        <form v-if="cart.items.length" class="row sform" @submit.prevent="saveCart"><input v-model.trim="cname" class="input" maxlength="40" placeholder="Tên giỏ, VD: Quà sinh nhật" aria-label="Tên giỏ" /><button class="btn btn-sm">Lưu giỏ hiện tại</button></form></div>
      <p v-if="!carts.length" class="muted small">Lưu nhiều giỏ để mua sau, so sánh hoặc chuẩn bị quà — khôi phục chỉ với một chạm.</p>
      <div v-for="c in carts" :key="c.id" class="sc">
        <div class="imgs"><img v-for="(i, n) in c.items.slice(0, 4)" :key="n" :src="i.image" alt="" /></div>
        <div class="grow"><b>{{ c.name }}</b><div class="small muted">{{ c.items.reduce((a, i) => a + i.quantity, 0) }} sản phẩm · {{ vnd(c.items.reduce((a, i) => a + i.unitPrice * i.quantity, 0)) }} · {{ ago(c.updatedAt) }}</div></div>
        <button class="btn btn-sm" :disabled="busy" @click="restore(c)">Khôi phục</button><button class="btn btn-sm btn-outline" :aria-label="`Xoá giỏ ${c.name}`" @click="del(c)">Xoá</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import api, { errMsg } from '../api'
import { useCart } from '../stores/cart'
import { useAuth } from '../stores/auth'
import { toast } from '../stores/prefs'
import { addById } from '../cartAdd'
import { vnd, ago, catalog, loadCatalog } from '../format'
const cart = useCart(), auth = useAuth(); loadCatalog()
const bundleOff = ref(0), carts = ref([]), cname = ref(''), busy = ref(false)
const quote = async () => { bundleOff.value = cart.items.length ? (await api.post('/public/bundle-quote', cart.items.map(i => ({ productId: i.productId, variantId: i.variantId, quantity: i.quantity })))).data.discount : 0 }
watch(() => cart.items.map(i => i.key + i.quantity).join(), () => quote().catch(() => {}), { immediate: true })
const loadCarts = async () => { if (!auth.loggedIn) return; carts.value = (await api.get('/account/saved-carts')).data.map(c => ({ ...c, items: JSON.parse(c.itemsJson || '[]') })) }
async function saveCart() {
  try { await api.post('/account/saved-carts', { name: cname.value, items: cart.items.map(i => ({ productId: i.productId, variantId: i.variantId, name: i.name, image: i.image, quantity: i.quantity, unitPrice: i.unitPrice })) }); cname.value = ''; toast('Đã lưu giỏ hàng'); loadCarts() }
  catch (e) { toast(errMsg(e), 'error') }
}
async function restore(c) {
  busy.value = true; let n = 0
  try { for (const i of c.items) if (await addById(i.productId, i.quantity, i.variantId)) n++; toast(n === c.items.length ? `Đã khôi phục “${c.name}”` : `Đã thêm ${n}/${c.items.length} món (một số đã hết hàng)`, n ? 'success' : 'error') }
  finally { busy.value = false }
}
async function del(c) { await api.delete('/account/saved-carts/' + c.id); loadCarts() }
onMounted(() => loadCarts().catch(() => {}))
const free = computed(() => catalog.options.freeShipFrom || 500000)
</script>

<style scoped>
.scarts { margin-top: 20px; display: flex; flex-direction: column; gap: 10px; } .scarts > .row { gap: 10px; flex-wrap: wrap; }
.sform { gap: 8px; flex-wrap: nowrap; } .sform .input { min-width: 0; width: 220px; }
.sc { display: flex; gap: 12px; align-items: center; padding: 10px 0; border-top: 1px solid var(--border); flex-wrap: wrap; } .sc .grow { min-width: 0; flex: 1 1 180px; }
.imgs { display: flex; } .imgs img { width: 40px; height: 32px; object-fit: cover; border: 2px solid var(--surface); margin-left: -8px; } .imgs img:first-child { margin-left: 0; }
.ok { color: var(--success); }
@media (max-width: 599px) { .sform { width: 100%; } .sform .input { flex: 1; width: auto; } }

.layout > * { min-width: 0; }
.layout { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 20px; align-items: start; }
@media (max-width: 860px) { .layout { grid-template-columns: 1fr; } }
.line { display: flex; align-items: center; gap: 14px; padding: 12px 0; border-bottom: 1px solid var(--border); }
.line .grow { min-width: 0; }
@media (max-width: 599px) { .line { flex-wrap: wrap; gap: 10px; } .line .grow { flex: 1 1 calc(100% - 90px); } }
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
.line-leave-active { transition: opacity .18s ease-out; } .line-leave-to { opacity: 0; }
.idle { animation-duration: 3s; margin: 10px auto 16px; }
</style>
