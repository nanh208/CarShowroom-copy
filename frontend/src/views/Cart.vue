<template>
  <div class="container page">
    <h1 style="margin-bottom:20px">Giỏ hàng</h1>
    <div v-if="!cart.items.length && !cart.saved.length" class="empty card" style="margin-bottom:16px"><div class="spinner idle" />Giỏ hàng đang trống. <router-link to="/products">Tiếp tục mua sắm →</router-link></div>
    <div v-else class="layout">
      <div>
        <div class="card">
          <div v-if="cartErrors.length" class="cart-alert-box">
            <div class="cab-title">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <b>PHÁT HIỆN SẢN PHẨM TẠM ẨN HOẶC HẾT HÀNG</b>
            </div>
            <ul class="cab-list">
              <li v-for="(err, idx) in cartErrors" :key="idx">{{ err.message }}</li>
            </ul>
            <small class="muted" style="display:block;margin-top:6px">Vui lòng xoá hoặc giảm số lượng các sản phẩm lỗi bên dưới để tiếp tục thanh toán.</small>
          </div>
          <div v-if="!cart.items.length" class="muted">Giỏ hàng trống — bạn có sản phẩm đã lưu bên dưới.</div>
          <transition-group name="line" tag="div">
            <template v-for="i in cart.items" :key="i.key">
              <!-- GÓI COMBO TRỌN GÓI THỐNG NHẤT -->
              <div v-if="i.isBundle" class="bundle-cart-card">
                <div class="bundle-top-row">
                  <div class="bundle-tag-group">
                    <span class="bundle-badge-vip">🎁 GÓI COMBO TRỌN GÓI</span>
                    <span v-if="i.save" class="bundle-save-pill">Tiết kiệm {{ vnd(i.save * i.quantity) }}</span>
                  </div>
                  <button class="bundle-remove-btn" title="Xoá gói combo này" @click="cart.remove(i.key)">✕ Xoá gói combo</button>
                </div>

                <div class="bundle-main-row">
                  <div class="bundle-gallery">
                    <img v-for="(p, pidx) in i.bundleProducts" :key="pidx" :src="p.image" :alt="p.name" class="bundle-mini-img" />
                  </div>
                  <div class="bundle-details">
                    <h3 class="bundle-name">{{ i.name }}</h3>
                    <div class="bundle-price-area">
                      <b class="price" style="font-size:1.2rem">{{ vnd(i.unitPrice) }}</b>
                      <s v-if="i.oldPrice" class="muted small">{{ vnd(i.oldPrice) }}</s>
                    </div>

                    <!-- Danh sách các sản phẩm bên trong gói combo -->
                    <div class="bundle-items-inside">
                      <div class="inside-header">Bao gồm {{ i.bundleProducts?.length || 3 }} sản phẩm:</div>
                      <div v-for="p in i.bundleProducts" :key="p.productId || p.id" class="inside-line">
                        <span class="inside-check">✓</span>
                        <router-link :to="`/products/${p.productId || p.id}`" class="inside-pname">{{ p.name }}</router-link>
                        <span class="inside-pprice muted">{{ vnd(p.price) }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="bundle-right-col">
                    <div class="qty">
                      <button :aria-label="`Giảm ${i.name}`" @click="cart.setQty(i.key, i.quantity - 1)">−</button>
                      <span>{{ i.quantity }}</span>
                      <button :aria-label="`Tăng ${i.name}`" @click="cart.setQty(i.key, i.quantity + 1)">+</button>
                    </div>
                    <b class="sum price">{{ vnd(i.unitPrice * i.quantity) }}</b>
                  </div>
                </div>
              </div>

              <!-- SẢN PHẨM LẺ BÌNH THƯỜNG -->
              <div v-else class="line">
                <img loading="lazy" decoding="async" :src="i.image" class="cimg" alt="" />
                <div class="grow">
                  <div class="name-row">
                    <router-link :to="`/products/${i.productId}`" class="name">{{ i.name }}</router-link>
                    <span v-if="getItemError(i)" class="badge" :class="getItemError(i).type === 'HIDDEN' || getItemError(i).type === 'OUT_OF_STOCK' ? 'badge-CANCELLED' : 'badge-PENDING'" style="margin-left:6px">
                      {{ getItemError(i).type === 'HIDDEN' ? 'Tạm ẩn' : (getItemError(i).type === 'OUT_OF_STOCK' ? 'Hết hàng' : `Còn ${getItemError(i).stock}`) }}
                    </span>
                    <span v-if="i.bundleTitle" class="bundle-badge">🎁 Gói: {{ i.bundleTitle }}</span>
                  </div>
                  <div v-if="i.variantName" class="muted small">Phân loại: {{ i.variantName }}</div>
                  <div class="muted small">Đơn giá {{ vnd(i.unitPrice) }} <s v-if="i.oldPrice">{{ vnd(i.oldPrice) }}</s> <span v-if="i.flash" class="fl">⚡ Flash sale</span></div>
                  <button class="link" @click="cart.saveForLater(i.key)">Lưu để mua sau</button>
                </div>
                <div class="qty"><button @click="cart.setQty(i.key, i.quantity - 1)">−</button><span>{{ i.quantity }}</span><button @click="cart.setQty(i.key, i.quantity + 1)">+</button></div>
                <b class="sum">{{ vnd(i.unitPrice * i.quantity) }}</b>
                <button class="btn btn-sm btn-outline" title="Xoá" @click="cart.remove(i.key)">Xoá</button>
              </div>
            </template>
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
        <div v-if="bundleOff" class="row between ok">
          <div>
            <span>Combo ưu đãi</span>
            <small class="db muted" style="font-size:.74rem;display:block">Đã giảm theo giá trọn gói combo</small>
          </div>
          <b>−{{ vnd(bundleOff) }}</b>
        </div>
        <div v-if="bundleOff" class="row between total-sub" style="font-weight:700;padding:6px 0;border-top:1px dashed var(--border)">
          <span>Tiền hàng sau combo:</span>
          <b style="color:var(--accent)">{{ vnd(cart.subtotal - bundleOff) }}</b>
        </div>
        <router-link v-else to="/bundles" class="small">🎁 Xem combo mua 2 tặng 1, giá gói →</router-link>
        <div class="ship">
          <div class="road"><div class="fill" :style="{ width: Math.min(100, cart.subtotal * 100 / free) + '%' }" /><span class="car" :style="{ left: Math.min(100, cart.subtotal * 100 / free) + '%' }">🚚</span></div>
          <small v-if="cart.subtotal >= free" class="ok">Đơn hàng được miễn phí vận chuyển!</small>
          <small v-else>Mua thêm <b>{{ vnd(free - cart.subtotal) }}</b> để được <b>freeship</b></small>
        </div>
        <div class="row between muted small"><span>Phí vận chuyển</span><span>Tính theo khu vực ở bước thanh toán</span></div>
        <button
          v-if="hasInvalidItems"
          type="button"
          class="btn btn-block btn-cart-blocked"
          style="margin-top:16px"
          @click="toast('Vui lòng xoá sản phẩm đã tạm ẩn hoặc hết hàng khỏi giỏ trước khi thanh toán!', 'error')"
        >
          Không thể thanh toán (Có hàng lỗi)
        </button>
        <router-link
          v-else
          to="/checkout"
          :class="['btn btn-block', { disabled: !cart.items.length }]"
          style="margin-top:16px"
        >
          Tiến hành thanh toán →
        </router-link>
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
const bundleOff = ref(0), carts = ref([]), cname = ref(''), busy = ref(false), cartErrors = ref([])
const quote = async () => {
  const loose = cart.items.filter(i => !i.isBundle)
  bundleOff.value = loose.length ? (await api.post('/public/bundle-quote', loose.map(i => ({ productId: i.productId, variantId: i.variantId, quantity: i.quantity, unitPrice: i.unitPrice })))).data.discount : 0
}

async function validateCart() {
  if (!cart.items.length) { cartErrors.value = []; return }
  try {
    const payload = cart.items.map(i => ({ productId: i.productId, variantId: i.variantId || null, quantity: i.quantity }))
    const { data } = await api.post('/public/cart-validate', payload)
    cartErrors.value = data.errors || []
  } catch (e) {
    console.error('Cart validate error', e)
  }
}

watch(() => cart.items.map(i => i.key + i.quantity).join(), () => {
  quote().catch(() => {})
  validateCart()
}, { immediate: true })

function getItemError(i) {
  return cartErrors.value.find(e => e.productId === i.productId && (!i.variantId || e.variantId === i.variantId))
}
const hasInvalidItems = computed(() => cartErrors.value.some(e => e.type === 'HIDDEN' || e.type === 'OUT_OF_STOCK'))
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
.name-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 2px; }
.bundle-badge { font-size: .72rem; font-weight: 700; color: #fff; background: var(--accent); padding: 2px 8px; border-radius: 4px; display: inline-flex; align-items: center; gap: 4px; }
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

.bundle-cart-card {
  background: var(--surface);
  border: 2px solid var(--accent);
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 14px;
  box-shadow: 0 4px 16px rgba(249, 115, 22, 0.08);
  position: relative;
}
.bundle-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dashed var(--border);
  padding-bottom: 10px;
  margin-bottom: 12px;
}
.bundle-tag-group { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.bundle-badge-vip {
  background: var(--accent);
  color: #fff;
  font: 700 .75rem 'Oswald', sans-serif;
  letter-spacing: .08em;
  padding: 3px 8px;
  border-radius: 6px;
  text-transform: uppercase;
}
.bundle-save-pill {
  background: #22c55e;
  color: #fff;
  font-weight: 700;
  font-size: .75rem;
  padding: 3px 8px;
  border-radius: 6px;
}
.bundle-remove-btn {
  background: none;
  border: 1px solid var(--border);
  color: var(--danger);
  border-radius: 6px;
  font-size: .78rem;
  padding: 3px 8px;
  cursor: pointer;
  transition: all .2s;
}
.bundle-remove-btn:hover { background: rgba(239, 68, 68, 0.1); border-color: var(--danger); }
.bundle-main-row {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.bundle-gallery {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.bundle-mini-img {
  width: 58px;
  height: 58px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-2);
}
.bundle-details { flex: 1; min-width: 240px; }
.bundle-name { margin: 0 0 4px; font-size: 1.05rem; }
.bundle-price-area { display: flex; align-items: baseline; gap: 8px; margin-bottom: 8px; }
.bundle-items-inside {
  background: var(--bg-2);
  border-radius: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border);
}
.inside-header { font-size: .76rem; color: var(--muted); margin-bottom: 4px; font-weight: 600; }
.inside-line { display: flex; align-items: center; gap: 8px; font-size: .84rem; padding: 2px 0; }
.inside-check { color: #22c55e; font-weight: 800; font-size: .9rem; }
.inside-pname { color: var(--text); text-decoration: none; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.inside-pname:hover { color: var(--accent); }
.inside-pprice { font-size: .78rem; }
.bundle-right-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  margin-left: auto;
}


.cart-alert-box {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-left: 4px solid #ef4444;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.cab-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #b91c1c;
  font-size: 0.92rem;
  margin-bottom: 6px;
}
.cab-list {
  margin: 0;
  padding-left: 20px;
  color: #dc2626;
  font-size: 0.85rem;
}
.btn-cart-blocked {
  background: #94a3b8 !important;
  color: #fff !important;
  cursor: not-allowed;
  opacity: 0.85;
}

</style>
