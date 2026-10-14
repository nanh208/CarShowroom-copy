<template>
  <div class="container page">
    <h1 style="margin-bottom:20px">Thanh toán</h1>
    <div v-if="!cart.items.length" class="empty card">Giỏ hàng trống. <router-link to="/products">Mua sắm ngay →</router-link></div>
    <div v-else class="layout">
      <div>
        <div v-reveal class="card block">
          <h3>1. Thông tin người nhận</h3>
          <div v-if="addresses.length" class="addr-list">
            <label v-for="a in addresses" :key="a.id" :class="['addr', { on: addrId === a.id }]">
              <input v-model="addrId" type="radio" :value="a.id" @change="useAddr(a)" />
              <div><b>{{ a.label || 'Địa chỉ' }}</b> <span v-if="a.isDefault" class="badge">Mặc định</span><br />
                <span class="small">{{ a.receiverName }} · {{ a.phone }}</span><br /><span class="small muted">{{ fmtAddr(a) }}</span></div>
            </label>
            <label :class="['addr', { on: addrId === 0 }]"><input v-model="addrId" type="radio" :value="0" /> <div>Nhập địa chỉ khác</div></label>
          </div>
          <div class="form-grid">
            <div class="field"><label>Họ và tên *</label><input v-model="f.buyerName" class="input" /></div>
            <div class="field"><label>Số điện thoại *</label><input v-model="f.phone" class="input" /></div>
          </div>
          <div class="form-grid">
            <div class="field"><label>Email</label><input v-model="f.email" class="input" type="email" /></div>
            <div class="field"><label>Tỉnh / Thành phố *</label><select v-model="f.city" class="input"><option value="">-- Chọn --</option><option v-for="c in catalog.options.cities" :key="c">{{ c }}</option></select></div>
          </div>
          <div v-if="!addresses.length || addrId === 0" class="field"><label>Địa chỉ *</label><input v-model="f.shippingAddress" class="input" placeholder="Số nhà, đường, phường/xã, quận/huyện" /></div>
          <div class="field"><label>Ghi chú</label><input v-model="f.note" class="input" placeholder="VD: Gói quà sinh nhật, giao giờ hành chính…" /></div>
        </div>

        <div v-reveal class="card block">
          <h3>2. Phương thức thanh toán</h3>
          <div class="pay-grid">
            <label v-for="(v, k) in PAY_METHOD" :key="k" :class="['pay', { on: f.paymentMethod === k, disabled: k === 'INSTALLMENT' && total < instMin }]">
              <input v-model="f.paymentMethod" type="radio" :value="k" :disabled="k === 'INSTALLMENT' && total < instMin" />
              <span class="pico" :class="k">{{ icons[k] }}</span>
              <div><b>{{ v }}</b><div class="small muted">{{ k === 'INSTALLMENT' && total < instMin ? `Áp dụng cho đơn từ ${vnd(instMin)}` : payDesc[k] }}</div></div>
            </label>
          </div>
          <div v-if="f.paymentMethod === 'INSTALLMENT'" class="field" style="margin-top:12px">
            <label>Kỳ hạn trả góp 0% (thẻ tín dụng)</label>
            <div class="row"><button v-for="m in [3, 6, 12]" :key="m" :class="['btn btn-sm', f.installmentMonths === m ? '' : 'btn-outline']" @click="f.installmentMonths = m">{{ m }} tháng · {{ vnd(Math.ceil(total / m)) }}/th</button></div>
          </div>
        </div>
      </div>

      <div class="card summary">
        <h3>Đơn hàng ({{ cart.count }} sản phẩm)</h3>
        <div v-for="i in cart.items" :key="i.key" class="item">
          <img :src="i.image" alt="" /><span class="grow">{{ i.name }} × {{ i.quantity }}<br /><span class="muted">{{ i.variantName }}</span></span><b>{{ vnd(i.unitPrice * i.quantity) }}</b>
        </div>
        <div class="row promo">
          <input v-model="promoCode" class="input grow" placeholder="Mã giảm giá" @keyup.enter="checkPromo" />
          <button class="btn btn-sm btn-outline" @click="checkPromo">Áp dụng</button>
        </div>
        <div class="vchips"><button v-for="v in vouchers" :key="v.code" class="vchip" :title="v.description" @click="promoCode = v.code; checkPromo()">{{ v.code }}</button></div>
        <div v-if="promoMsg" :class="['small', promo ? 'ok' : 'err']">{{ promoMsg }}</div>
        <div v-if="pts.points" class="pts">
          <label class="check"><input v-model="usePts" type="checkbox" /> Dùng {{ maxPts }} điểm (−{{ vnd(maxPts * catalog.options.pointValue) }})</label>
          <small class="muted">Bạn có {{ pts.points }} điểm · hạng {{ pts.tier }}</small>
        </div>
        <div class="row between"><span>Tạm tính</span><span>{{ vnd(cart.subtotal) }}</span></div>
        <div v-if="discount" class="row between ok"><span>Voucher ({{ promo.code }})</span><span>−{{ vnd(discount) }}</span></div>
        <div v-if="ptsDiscount" class="row between ok"><span>Điểm thưởng</span><span>−{{ vnd(ptsDiscount) }}</span></div>
        <div class="row between"><span>Phí vận chuyển <small class="muted">{{ f.city ? ship.eta : '' }}</small></span><span>{{ !f.city ? 'Chọn tỉnh/thành' : shipFee ? vnd(shipFee) : 'Miễn phí' }}</span></div>
        <div class="row between total"><span>Tổng cộng</span><span class="price">{{ vnd(total) }}</span></div>
        <div class="small muted">+{{ Math.floor(total / catalog.options.earnPer) }} điểm khi đơn hoàn tất</div>
        <div v-if="error" class="alert alert-error">{{ error }}</div>
        <button class="btn btn-block" :disabled="busy" @click="submit">{{ busy ? 'Đang xử lý...' : 'Đặt hàng' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { useCart } from '../stores/cart'
import { useAuth } from '../stores/auth'
import { vnd, PAY_METHOD, catalog, loadCatalog } from '../format'
import { toast } from '../stores/prefs'

const cart = useCart(), auth = useAuth(), router = useRouter()
const f = reactive({ buyerName: auth.user?.fullName || '', phone: auth.user?.phone || '', email: auth.user?.email || '', shippingAddress: '', city: '', note: '', paymentMethod: 'COD', installmentMonths: 6 })
const addresses = ref([]), addrId = ref(0), vouchers = ref([]), pts = ref({ points: 0 }), usePts = ref(false)
const promoCode = ref(''), promo = ref(null), discount = ref(0), freeShip = ref(false), promoMsg = ref(''), error = ref(''), busy = ref(false), ship = ref({ fee: 0, eta: '' })
const icons = { COD: '💵', BANK_QR: '▦', MOMO: 'M', VNPAY: 'V', INSTALLMENT: '💳' }
const payDesc = { COD: 'Trả tiền mặt khi nhận hàng', BANK_QR: 'Quét VietQR bằng app ngân hàng', MOMO: 'Thanh toán qua ví MoMo', VNPAY: 'Thẻ ATM / QR VNPay', INSTALLMENT: '0% lãi, 3–12 tháng' }
const instMin = computed(() => catalog.options.installmentMin)
const after = computed(() => cart.subtotal - discount.value)
const maxPts = computed(() => Math.min(pts.value.points, Math.floor(after.value / 2 / catalog.options.pointValue)))
const ptsDiscount = computed(() => usePts.value ? maxPts.value * catalog.options.pointValue : 0)
const shipFee = computed(() => freeShip.value ? 0 : ship.value.fee)
const total = computed(() => after.value - ptsDiscount.value + (f.city ? shipFee.value : 0))
const fmtAddr = a => [a.line, a.ward, a.district, a.city].filter(Boolean).join(', ')
const useAddr = a => { f.buyerName = a.receiverName; f.phone = a.phone; f.shippingAddress = [a.line, a.ward, a.district].filter(Boolean).join(', '); f.city = catalog.options.cities.includes(a.city) ? a.city : 'Tỉnh khác' }
const quote = async () => { ship.value = (await api.get('/shipping/quote', { params: { city: f.city || undefined, amount: after.value - ptsDiscount.value } })).data }
watch([() => f.city, after, ptsDiscount], quote)

async function checkPromo() {
  promoMsg.value = ''; promo.value = null; discount.value = 0; freeShip.value = false
  if (!promoCode.value.trim()) return
  try {
    const { data } = await api.post('/promotions/check', { code: promoCode.value, subtotal: cart.subtotal })
    promo.value = data.promotion; discount.value = data.discount; freeShip.value = data.freeShip
    promoMsg.value = data.freeShip ? 'Đã áp dụng: miễn phí vận chuyển' : `Đã áp dụng: giảm ${vnd(data.discount)}`
  } catch (e) { promoMsg.value = errMsg(e) }
}
async function submit() {
  error.value = ''
  if (!f.city) return (error.value = 'Vui lòng chọn tỉnh/thành để tính phí vận chuyển')
  busy.value = true
  try {
    const { data } = await api.post('/orders', { ...f, promoCode: promo.value?.code, usePoints: usePts.value ? maxPts.value : 0,
      items: cart.items.map(i => ({ productId: i.productId, variantId: i.variantId, quantity: i.quantity })) })
    cart.clear()
    toast('Đặt hàng thành công!')
    if (['MOMO', 'VNPAY'].includes(data.paymentMethod)) router.push(`/pay/${data.code}`)
    else router.push(`/orders/${data.code}?new=1`)
  } catch (e) { error.value = errMsg(e) } finally { busy.value = false }
}
onMounted(async () => {
  await loadCatalog(); quote()
  api.get('/promotions').then(r => { vouchers.value = r.data })
  api.get('/account/points').then(r => { pts.value = r.data })
  addresses.value = (await api.get('/account/addresses')).data
  const def = addresses.value.find(a => a.isDefault) || addresses.value[0]
  if (def) { addrId.value = def.id; useAddr(def) }
})
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 1fr 380px; gap: 20px; align-items: start; }
@media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
.block { margin-bottom: 16px; } .block h3 { margin-bottom: 12px; }
.addr-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 12px; }
.addr { display: flex; gap: 10px; padding: 10px 12px; border: 1.5px solid var(--border); border-radius: 10px; cursor: pointer; } .addr.on { border-color: var(--bronze); background: var(--bronze-soft); }
.pay-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.pay { display: flex; gap: 10px; align-items: center; padding: 12px; border: 1.5px solid var(--border); border-radius: 10px; cursor: pointer; transition: .2s; }
.pay.on { border-color: var(--bronze); background: var(--bronze-soft); } .pay.disabled { opacity: .5; cursor: not-allowed; } .pay input { display: none; }
.pico { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 800; background: var(--bg-2); flex-shrink: 0; }
.pico.MOMO { background: #a50064; color: #fff; } .pico.VNPAY { background: #005baa; color: #fff; } .pico.BANK_QR { background: #1d1d1f; color: #fff; }
.summary { position: sticky; top: 110px; display: flex; flex-direction: column; gap: 10px; }
.item { display: flex; gap: 10px; align-items: center; font-size: .85rem; } .item img { width: 52px; height: 38px; object-fit: cover; border-radius: 6px; }
.promo { gap: 8px; } .vchips { display: flex; gap: 6px; flex-wrap: wrap; }
.vchip { border: 1px dashed var(--bronze); background: var(--bronze-soft); color: var(--bronze-3); border-radius: 6px; padding: 2px 8px; font: 600 .75rem inherit; font-family: inherit; cursor: pointer; }
.pts { background: var(--bg-2); border-radius: 8px; padding: 8px 10px; display: flex; flex-direction: column; } .check { display: flex; gap: 6px; align-items: center; font-size: .88rem; }
.total { font-weight: 700; padding-top: 10px; border-top: 1px solid var(--border); } .ok { color: var(--success); } .err { color: var(--danger); }
</style>
