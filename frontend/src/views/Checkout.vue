<template>
  <div class="container page">
    <h1 style="margin-bottom:20px">Thanh toán</h1>
    <div v-if="!cart.items.length" class="empty card">Giỏ hàng trống. <router-link to="/products">Mua sắm ngay →</router-link></div>
    <div v-else class="layout">
      <div>
        <div class="card block">
          <h3>1. Thông tin người mua</h3>
          <div class="form-grid">
            <div class="field"><label>Họ và tên *</label><input v-model="f.buyerName" class="input" /></div>
            <div class="field"><label>Số điện thoại *</label><input v-model="f.phone" class="input" /></div>
          </div>
          <div class="field"><label>Email</label><input v-model="f.email" class="input" type="email" /></div>
        </div>

        <div class="card block">
          <h3>2. Địa chỉ nhận hàng</h3>
          <div v-if="addresses.length" class="addr-list">
            <label v-for="a in addresses" :key="a.id" :class="['addr', { on: addrId === a.id }]">
              <input v-model="addrId" type="radio" :value="a.id" @change="useAddr(a)" />
              <div><b>{{ a.label || 'Địa chỉ' }}</b> <span v-if="a.isDefault" class="badge">Mặc định</span><br />
                <span class="small">{{ a.receiverName }} · {{ a.phone }}</span><br /><span class="small muted">{{ fmtAddr(a) }}</span></div>
            </label>
            <label :class="['addr', { on: addrId === 0 }]"><input v-model="addrId" type="radio" :value="0" /> <div>Nhập địa chỉ khác</div></label>
          </div>
          <div v-if="!addresses.length || addrId === 0" class="field"><label>Địa chỉ nhận hàng *</label>
            <textarea v-model="f.shippingAddress" class="input" rows="2" placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành" /></div>
          <p v-if="!addresses.length" class="small muted">Mẹo: lưu địa chỉ trong <router-link to="/account?tab=addresses">Sổ địa chỉ</router-link> để thanh toán nhanh hơn.</p>
          <div class="field"><label>Ghi chú</label><input v-model="f.note" class="input" placeholder="VD: Giao giờ hành chính, gói quà..." /></div>
        </div>

        <div class="card block">
          <h3>3. Phương thức thanh toán</h3>
          <div class="pay-grid">
            <label v-for="(v, k) in PAY_METHOD" :key="k" :class="['pay', { on: f.paymentMethod === k, disabled: k === 'INSTALLMENT' && total < 1000000 }]">
              <input v-model="f.paymentMethod" type="radio" :value="k" :disabled="k === 'INSTALLMENT' && total < 1000000" />
              <div><b>{{ icons[k] }} {{ v }}</b><div class="small muted">{{ payDesc[k] }}</div></div>
            </label>
          </div>
          <div v-if="f.paymentMethod === 'INSTALLMENT'" class="field" style="margin-top:12px">
            <label>Kỳ hạn trả góp</label>
            <div class="row"><button v-for="m in [3, 6, 9, 12]" :key="m" :class="['btn btn-sm', f.installmentMonths === m ? '' : 'btn-outline']" @click="f.installmentMonths = m">{{ m }} tháng</button></div>
          </div>
          <div v-if="['FULL', 'DEPOSIT', 'INSTALLMENT'].includes(f.paymentMethod)" class="card mockpay">
            <div class="small muted" style="margin-bottom:8px">💳 Cổng thanh toán thử nghiệm (không trừ tiền thật)</div>
            <div class="form-grid">
              <div class="field"><label>Số thẻ</label><input v-model="card.no" class="input" placeholder="4111 1111 1111 1111" /></div>
              <div class="field"><label>Chủ thẻ</label><input v-model="card.name" class="input" placeholder="NGUYEN VAN A" /></div>
            </div>
          </div>
        </div>
      </div>

      <div class="card summary">
        <h3>Đơn hàng ({{ cart.count }} sản phẩm)</h3>
        <div v-for="i in cart.items" :key="i.key" class="row between small item">
          <span class="grow">{{ i.name }} × {{ i.quantity }}<br /><span class="muted">{{ i.optionLabel }}</span></span><b>{{ vnd(i.unitPrice * i.quantity) }}</b>
        </div>
        <div class="row promo">
          <input v-model="promoCode" class="input grow" placeholder="Mã ưu đãi" />
          <button class="btn btn-sm btn-outline" @click="checkPromo">Áp dụng</button>
        </div>
        <div v-if="promoMsg" :class="['small', promo ? 'ok' : 'err']">{{ promoMsg }}</div>
        <div class="row between"><span>Tạm tính</span><span>{{ vnd(cart.subtotal) }}</span></div>
        <div v-if="discount" class="row between ok"><span>Giảm giá ({{ promo.code }})</span><span>−{{ vnd(discount) }}</span></div>
        <div class="row between"><span>Vận chuyển</span><span>{{ ship ? vnd(ship) : 'Miễn phí' }}</span></div>
        <div class="row between total"><span>Tổng cộng</span><span class="price">{{ vnd(total) }}</span></div>
        <div class="row between payNow"><span>Thanh toán ngay</span><b>{{ vnd(payNow) }}</b></div>
        <div v-if="f.paymentMethod === 'INSTALLMENT'" class="small muted">Trả {{ vnd(monthly) }}/tháng × {{ f.installmentMonths }} tháng (lãi 0%)</div>
        <div v-if="f.paymentMethod === 'DEPOSIT'" class="small muted">Còn lại {{ vnd(total - payNow) }} thanh toán khi nhận hàng</div>
        <div v-if="error" class="alert alert-error">{{ error }}</div>
        <button class="btn btn-block" :disabled="busy" @click="submit">{{ busy ? 'Đang xử lý...' : 'Đặt hàng' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { vnd, PAY_METHOD } from '../format'
import { useCart } from '../stores/cart'
import { useAuth } from '../stores/auth'

const cart = useCart(), auth = useAuth(), router = useRouter()
const f = reactive({ buyerName: auth.user?.fullName || '', phone: auth.user?.phone || '', email: auth.user?.email || '', shippingAddress: '', note: '', paymentMethod: 'FULL', installmentMonths: 6 })
const card = reactive({ no: '', name: '' })
const addresses = ref([]), addrId = ref(0), promoCode = ref(''), promo = ref(null), promoMsg = ref(''), error = ref(''), busy = ref(false)
const icons = { FULL: '💳', DEPOSIT: '🔖', INSTALLMENT: '📆', COD: '🚚' }
const payDesc = { FULL: 'Thanh toán 100% ngay', DEPOSIT: 'Cọc 30%, phần còn lại khi nhận hàng', INSTALLMENT: '3–12 tháng, đơn từ 1.000.000đ', COD: 'Trả tiền mặt khi nhận hàng' }

const discount = computed(() => {
  if (!promo.value) return 0
  const d = Math.floor(cart.subtotal * promo.value.discountPercent / 100)
  return promo.value.maxDiscount > 0 ? Math.min(d, promo.value.maxDiscount) : d
})
const ship = computed(() => cart.subtotal - discount.value >= 2000000 ? 0 : 40000)
const total = computed(() => cart.subtotal - discount.value + ship.value)
const monthly = computed(() => Math.ceil(total.value / f.installmentMonths))
const payNow = computed(() => ({ FULL: total.value, DEPOSIT: Math.round(total.value * 0.3), INSTALLMENT: monthly.value, COD: 0 })[f.paymentMethod])

const fmtAddr = a => [a.line, a.ward, a.district, a.city].filter(Boolean).join(', ')
function useAddr(a) { f.shippingAddress = fmtAddr(a); if (a.receiverName) f.buyerName = a.receiverName; if (a.phone) f.phone = a.phone }

async function checkPromo() {
  promo.value = null; promoMsg.value = ''
  if (!promoCode.value) return
  try {
    const { data } = await api.post('/promotions/check', { code: promoCode.value, subtotal: cart.subtotal })
    promo.value = data.promotion; promoMsg.value = `✔ Áp dụng "${data.promotion.title}"`
  } catch (e) { promoMsg.value = errMsg(e) }
}

async function submit() {
  error.value = ''
  if (['FULL', 'DEPOSIT', 'INSTALLMENT'].includes(f.paymentMethod) && card.no.replace(/\s/g, '').length < 12) {
    error.value = 'Vui lòng nhập số thẻ (thử nghiệm) để thanh toán'; return
  }
  busy.value = true
  try {
    const items = cart.items.map(i => ({ productId: i.productId, quantity: i.quantity, ...i.options }))
    const { data } = await api.post('/orders', { ...f, promoCode: promo.value?.code, items })
    cart.clear()
    router.push(`/orders/${data.code}?new=1`)
  } catch (e) { error.value = errMsg(e) } finally { busy.value = false }
}

onMounted(async () => {
  addresses.value = (await api.get('/account/addresses')).data
  const def = addresses.value.find(a => a.isDefault) || addresses.value[0]
  if (def) { addrId.value = def.id; useAddr(def) }
})
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 1fr 380px; gap: 24px; align-items: start; }
@media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
.block { margin-bottom: 18px; }
.addr-list, .pay-grid { display: grid; gap: 10px; margin-bottom: 12px; }
.pay-grid { grid-template-columns: 1fr 1fr; }
@media (max-width: 640px) { .pay-grid { grid-template-columns: 1fr; } }
.addr, .pay { display: flex; gap: 12px; align-items: flex-start; padding: 12px 14px; border: 1px solid var(--border); border-radius: 10px; cursor: pointer; margin: 0; color: var(--text); }
.addr.on, .pay.on { border-color: var(--accent); background: rgba(56,189,248,.08); }
.pay.disabled { opacity: .45; cursor: not-allowed; }
.mockpay { margin-top: 14px; padding: 14px; box-shadow: none; }
.summary { display: flex; flex-direction: column; gap: 10px; position: sticky; top: 150px; }
.item { padding-bottom: 8px; border-bottom: 1px solid var(--border); align-items: flex-start; }
.promo { flex-wrap: nowrap; } .promo .input { padding: 8px 12px; }
.ok { color: #86efac; } .err { color: #fca5a5; }
.total { font-size: 1.15rem; font-weight: 700; border-top: 1px solid var(--border); padding-top: 10px; }
.payNow { background: rgba(2,132,199,.15); padding: 8px 10px; border-radius: 8px; }
</style>
