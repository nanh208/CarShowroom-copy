<template>
  <div class="container page">
    <h1 style="margin-bottom:20px">Thanh toán</h1>
    <div v-if="guest && cart.items.length" class="guestbar card"><span>🛈 Bạn đang đặt hàng <b>không cần tài khoản</b> (thanh toán khi nhận hàng). Hoá đơn gửi qua email.</span>
      <router-link :to="{ path: '/login', query: { redirect: '/checkout' } }" class="btn btn-sm btn-outline">Đăng nhập để tích điểm</router-link></div>
    <div v-if="!cart.items.length" class="empty card">Giỏ hàng trống. <router-link to="/products">Mua sắm ngay →</router-link></div>
    <div v-else class="layout">
      <div>
        <div class="card block">
          <h3>1. Thông tin người nhận</h3>
          <div v-if="addresses.length" class="addr-list">
            <label v-for="a in addresses" :key="a.id" :class="['addr', { on: addrId === a.id }]">
              <input v-model="addrId" type="radio" :value="a.id" @change="useAddr(a)" />
              <div><b>{{ a.label || 'Địa chỉ' }}</b> <span v-if="a.isDefault" class="badge">Mặc định</span><br />
                <span class="small">{{ a.receiverName }} · {{ a.phone }}</span><br /><span class="small muted">{{ fmtAddr(a) }}</span></div>
            </label>
            <label :class="['addr', { on: addrId === 0 }]"><input v-model="addrId" type="radio" :value="0" @change="f.shippingAddress = ''" /> <div>Nhập địa chỉ khác</div></label>
          </div>
          <template v-if="!addresses.length || addrId === 0">
          <div class="form-grid">
            <div class="field"><label>Họ và tên *</label><input v-model="f.buyerName" class="input" /></div>
            <div class="field"><label>Số điện thoại *</label><input v-model="f.phone" class="input" /></div>
          </div>
          <div class="field"><label>Email{{ guest ? ' *' : '' }}</label><input v-model="f.email" class="input" type="email" autocomplete="email" :placeholder="guest ? 'Nhận hoá đơn & theo dõi đơn' : ''" /></div>
          <AddressPicker v-model="addr" :cities="catalog.options.cities" />
          </template>
          <div class="field"><label for="co-note">Ghi chú cho đơn hàng</label><textarea id="co-note" v-model="f.note" class="input" rows="2" maxlength="500" placeholder="VD: Giao giờ hành chính, gọi trước khi giao…" /></div>
          <div :class="['gift', { on: f.giftWrap }]">
            <label class="check"><input v-model="f.giftWrap" type="checkbox" /> <span><b>Gói quà</b> — hộp carton đen, nơ cam, thiệp viết tay <span class="muted">(+{{ vnd(GIFT_FEE) }})</span></span></label>
            <textarea v-if="f.giftWrap" v-model="f.giftMessage" class="input" rows="2" maxlength="300" placeholder="Lời nhắn trên thiệp (tối đa 300 ký tự)" aria-label="Lời nhắn trên thiệp" />
          </div>
        </div>

        <div class="card block">
          <h3>2. Phương thức vận chuyển</h3>
          <div class="ship-grid" role="radiogroup" aria-label="Phương thức vận chuyển">
            <label v-for="o in shipOpts" :key="o.method" :class="['pay', { on: f.shippingMethod === o.method }]">
              <input v-model="f.shippingMethod" type="radio" :value="o.method" />
              <span class="pico" :class="o.method">{{ o.method === 'EXPRESS' ? '⚡' : '🚚' }}</span>
              <div class="grow"><b>{{ o.label }}</b><div class="small muted">Nhận {{ etaText(o) }}</div></div>
              <b class="small">{{ freeShip && o.method === 'STANDARD' ? 'Miễn phí' : o.fee ? vnd(o.fee) : 'Miễn phí' }}</b>
            </label>
          </div>
          <p v-if="!f.city" class="small muted" style="margin-top:8px">Chọn tỉnh/thành để tính chính xác phí và ngày giao.</p>
        </div>

        <div class="card block">
          <h3>3. Phương thức thanh toán</h3>
          <div class="pay-grid">
            <label v-for="(v, k) in PAY_METHOD" :key="k" :class="['pay', { on: f.paymentMethod === k, disabled: payOff(k) }]">
              <input v-model="f.paymentMethod" type="radio" :value="k" :disabled="payOff(k)" />
              <span class="pico" :class="k">{{ icons[k] }}</span>
              <div><b>{{ v }}</b><div class="small muted">{{ guest && k !== 'COD' ? 'Đăng nhập để dùng' : k === 'INSTALLMENT' && total < instMin ? `Áp dụng cho đơn từ ${vnd(instMin)}` : payDesc[k] }}</div></div>
            </label>
          </div>
          <label v-if="!guest" class="check remember"><input v-model="rememberPay" type="checkbox" /> Ghi nhớ <b>{{ PAY_METHOD[f.paymentMethod] }}</b> làm phương thức mặc định</label>
          <div v-if="f.paymentMethod === 'INSTALLMENT'" class="field" style="margin-top:12px">
            <label>Kỳ hạn trả góp 0% (thẻ tín dụng)</label>
            <div class="row"><button v-for="m in [3, 6, 12]" :key="m" :class="['btn btn-sm', f.installmentMonths === m ? '' : 'btn-outline']" @click="f.installmentMonths = m">{{ m }} tháng · {{ vnd(Math.ceil(total / m)) }}/th</button></div>
          </div>
        </div>
      </div>

      <div class="card summary">
        <h3>Đơn hàng ({{ cart.count }} sản phẩm)</h3>
        <div v-for="i in cart.items" :key="i.key" class="item">
          <img loading="lazy" decoding="async" :src="i.image" alt="" /><span class="grow">{{ i.name }} × {{ i.quantity }}<br /><span class="muted">{{ i.variantName }}</span></span><b>{{ vnd(i.unitPrice * i.quantity) }}</b>
        </div>
        <div class="row promo">
          <input v-model="promoCode" class="input grow" placeholder="Mã giảm giá" @keyup.enter="checkPromo(true)" />
          <button class="btn btn-sm btn-outline" @click="checkPromo(true)">Áp dụng</button>
        </div>
        <div class="vchips"><button v-for="v in vouchers" :key="v.code" class="vchip" :title="v.description" @click="promoCode = v.code; checkPromo(true)">{{ v.code }}</button></div>
        <div v-if="promoMsg" :class="['small', promo ? 'ok' : 'err']">{{ promoMsg }}</div>
        <details class="extra" :open="!!f.giftCardCode || !!f.referralCode">
          <summary>Thẻ quà tặng{{ guest ? '' : ' · mã giới thiệu' }}</summary>
          <div class="row promo"><input v-model.trim="f.giftCardCode" class="input grow" placeholder="Mã thẻ quà (GC…)" aria-label="Mã thẻ quà" @keyup.enter="checkGift" /><button class="btn btn-sm btn-outline" @click="checkGift">Kiểm tra</button></div>
          <div v-if="giftMsg" :class="['small', gift ? 'ok' : 'err']">{{ giftMsg }}</div>
          <div v-if="!guest" class="row promo"><input v-model.trim="f.referralCode" class="input grow" placeholder="Mã giới thiệu (đơn đầu tiên −50K)" aria-label="Mã giới thiệu" /></div>
        </details>
        <label v-if="wallet.storeCredit > 0" class="check pts"><input v-model="f.useCredit" type="checkbox" /> Dùng ví tín dụng ({{ vnd(wallet.storeCredit) }})</label>
        <div v-if="pts.points" class="pts">
          <label class="check"><input v-model="usePts" type="checkbox" /> Dùng {{ maxPts }} điểm (−{{ vnd(maxPts * catalog.options.pointValue) }})</label>
          <small class="muted">Bạn có {{ pts.points }} điểm · hạng {{ pts.tier }}</small>
        </div>
        <div class="row between"><span>Tạm tính</span><span>{{ vnd(cart.subtotal) }}</span></div>
        <div v-if="bundleOff" class="row between ok"><span>Combo ưu đãi</span><span>−{{ vnd(bundleOff) }}</span></div>
        <div v-if="discount" class="row between ok"><span>Voucher ({{ promo.code }})<small v-if="autoApplied" class="auto">tự áp tốt nhất</small></span><span>−{{ vnd(discount) }}</span></div>
        <div v-if="refOff" class="row between ok"><span>Mã giới thiệu <small class="muted">(xác nhận khi đặt)</small></span><span>−{{ vnd(refOff) }}</span></div>
        <div v-if="ptsDiscount" class="row between ok"><span>Điểm thưởng</span><span>−{{ vnd(ptsDiscount) }}</span></div>
        <div class="row between"><span>Phí vận chuyển <small class="muted">{{ curShip ? (curShip.method === 'EXPRESS' ? 'hoả tốc' : 'tiêu chuẩn') : '' }}</small></span><span>{{ !f.city ? 'Chọn tỉnh/thành' : shipFee ? vnd(shipFee) : 'Miễn phí' }}</span></div>
        <div v-if="f.giftWrap" class="row between"><span>Gói quà</span><span>{{ vnd(GIFT_FEE) }}</span></div>
        <div v-if="creditOff" class="row between ok"><span>Thẻ quà / ví tín dụng</span><span>−{{ vnd(creditOff) }}</span></div>
        <div class="row between total"><span>Tổng cộng</span><span class="price">{{ vnd(total) }}</span></div>
        <div v-if="curShip && f.city" class="eta small">📦 Dự kiến nhận hàng <b>{{ etaText(curShip) }}</b></div>
        <div v-if="!guest" class="small muted">+{{ Math.floor(total / catalog.options.earnPer) }} điểm khi đơn hoàn tất</div>
        <div v-if="error" class="alert alert-error">{{ error }}</div>
        <button class="btn btn-block place" :disabled="busy" @click="submit">{{ busy ? 'Đang xử lý...' : 'Đặt hàng' }}</button>
      </div>
      <div class="mbar"><div><small>Tổng cộng</small><b class="price">{{ vnd(total) }}</b></div><button class="btn" :disabled="busy" @click="submit">Đặt hàng</button></div>
    </div>
    <OrderLaunch :state="launch" :order="placed" :error="error" :pay-next="payNext" @close="launch = 'idle'" @view="goOrder" @shop="router.push('/products')" @pay="goPay" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { useCart } from '../stores/cart'
import { useAuth } from '../stores/auth'
import { vnd, PAY_METHOD, catalog, loadCatalog, toShipCity } from '../format'
import { toast } from '../stores/prefs'
import AddressPicker from '../components/AddressPicker.vue'
import OrderLaunch from '../components/OrderLaunch.vue'

const cart = useCart(), auth = useAuth(), router = useRouter()
const GIFT_FEE = 20000
const guest = computed(() => !auth.loggedIn)
const f = reactive({ buyerName: auth.user?.fullName || '', phone: auth.user?.phone || '', email: auth.user?.email || '', shippingAddress: '', city: '', note: '', giftWrap: false, giftMessage: '',
  paymentMethod: !auth.loggedIn ? 'COD' : auth.user?.preferredPayment || localStorage.getItem('lastPay') || 'COD', installmentMonths: 6,
  shippingMethod: localStorage.getItem('lastShip') || 'STANDARD', giftCardCode: '', referralCode: localStorage.getItem('refCode') || '', useCredit: false })
watch(() => f.shippingMethod, v => localStorage.setItem('lastShip', v))
const payOff = k => (guest.value && k !== 'COD') || (k === 'INSTALLMENT' && total.value < instMin.value)
const shipOpts = ref([{ method: 'STANDARD', label: 'Tiêu chuẩn', fee: 0 }, { method: 'EXPRESS', label: 'Hoả tốc', fee: 0 }])
const curShip = computed(() => shipOpts.value.find(o => o.method === f.shippingMethod))
const DOW = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
const dshort = s => { const d = new Date(s + 'T00:00:00'); return `${DOW[d.getDay()]} ${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}` }
const etaText = o => !o?.etaFrom ? '—' : o.etaFrom === o.etaTo ? dshort(o.etaFrom) : `${dshort(o.etaFrom)} – ${dshort(o.etaTo)}`
const bundleOff = ref(0), wallet = ref({ storeCredit: 0 }), gift = ref(null), giftMsg = ref(''), autoApplied = ref(false)
const refOff = computed(() => !guest.value && f.referralCode ? 50000 : 0)
watch(() => f.paymentMethod, v => localStorage.setItem('lastPay', v))
const rememberPay = ref(!!auth.user?.preferredPayment && auth.user.preferredPayment === f.paymentMethod)
const addr = ref({})
watch(addr, a => { if (addrId.value === 0 || !addresses.value.length) { f.city = a.city || ''; f.shippingAddress = a.full || '' } })
const launch = ref('idle'), placed = ref(null), payNext = computed(() => ['MOMO', 'VNPAY'].includes(placed.value?.paymentMethod))
const goOrder = () => router.push(placed.value.guest ? `/track/${placed.value.code}` : `/orders/${placed.value.code}?new=1`)
const goPay = () => router.push(`/pay/${placed.value.code}`)
const addresses = ref([]), addrId = ref(0), vouchers = ref([]), pts = ref({ points: 0 }), usePts = ref(false)
const promoCode = ref(''), promo = ref(null), discount = ref(0), freeShip = ref(false), promoMsg = ref(''), error = ref(''), busy = ref(false), ship = ref({ fee: 0, eta: '' })
const icons = { COD: '💵', BANK_QR: '▦', MOMO: 'M', VNPAY: 'V', INSTALLMENT: '💳' }
const payDesc = { COD: 'Trả tiền mặt khi nhận hàng', BANK_QR: 'Quét VietQR bằng app ngân hàng', MOMO: 'Thanh toán qua ví MoMo', VNPAY: 'Thẻ ATM / QR VNPay', INSTALLMENT: '0% lãi, 3–12 tháng' }
const instMin = computed(() => catalog.options.installmentMin)
const after = computed(() => Math.max(0, cart.subtotal - discount.value - bundleOff.value - refOff.value))
const maxPts = computed(() => Math.min(pts.value.points, Math.floor(after.value / 2 / catalog.options.pointValue)))
const ptsDiscount = computed(() => usePts.value ? maxPts.value * catalog.options.pointValue : 0)
const shipFee = computed(() => freeShip.value && f.shippingMethod !== 'EXPRESS' ? 0 : curShip.value?.fee || 0)
const gross = computed(() => after.value - ptsDiscount.value + (f.city ? shipFee.value : 0) + (f.giftWrap ? GIFT_FEE : 0))
const creditOff = computed(() => { let c = 0, left = gross.value; if (gift.value) { c += Math.min(gift.value.balance, left); left -= c } if (f.useCredit) c += Math.min(wallet.value.storeCredit, left); return c })
const total = computed(() => gross.value - creditOff.value)
const fmtAddr = a => [a.line, a.ward, a.district, a.city].filter(Boolean).join(', ')
const useAddr = a => { f.buyerName = a.receiverName; f.phone = a.phone; f.shippingAddress = [a.line, a.ward, a.district].filter(Boolean).join(', '); f.city = toShipCity(a.city) }
const quote = async () => { shipOpts.value = (await api.get('/public/shipping-options', { params: { city: f.city || undefined, amount: after.value - ptsDiscount.value } })).data }
watch([() => f.city, after, ptsDiscount], quote)
const items = () => cart.items.map(i => ({ productId: i.productId, variantId: i.variantId, quantity: i.quantity }))
const bundleQuote = async () => { bundleOff.value = cart.items.length ? (await api.post('/public/bundle-quote', items())).data.discount : 0 }
watch(() => cart.items.map(i => i.key + i.quantity).join(), bundleQuote)
async function autoBest() {
  if (promoCode.value && !autoApplied.value) return
  const { data } = await api.get('/public/best-promo', { params: { subtotal: cart.subtotal } })
  if (data.code && data.code !== promo.value?.code) { promoCode.value = data.code; await checkPromo(); autoApplied.value = !!promo.value; if (promo.value) promoMsg.value = `Đã tự áp mã tốt nhất ${data.code}: giảm ${vnd(data.discount)}` }
}
watch(() => cart.subtotal, () => autoBest().catch(() => {}))
async function checkGift() {
  gift.value = null; giftMsg.value = ''; if (!f.giftCardCode) return
  try { const { data } = await api.get('/public/giftcards/' + encodeURIComponent(f.giftCardCode)); if (!data.balance) throw new Error(); gift.value = data; giftMsg.value = `Thẻ còn ${vnd(data.balance)}` }
  catch { giftMsg.value = 'Mã thẻ không hợp lệ hoặc đã hết số dư' }
}
watch(total, v => { if (f.paymentMethod === 'INSTALLMENT' && v < instMin.value) f.paymentMethod = 'COD' })

async function checkPromo(manual) {
  if (manual === true) autoApplied.value = false
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
  if ((addrId.value === 0 || !addresses.value.length) && !addr.value.valid) return (error.value = 'Vui lòng chọn đủ tỉnh/thành, quận/huyện, phường/xã từ gợi ý và nhập số nhà')
  if (guest.value && (!f.buyerName.trim() || !/^\S+@\S+\.\S+$/.test(f.email) || !/^0\d{9,10}$/.test(f.phone.replace(/\s/g, '')))) return (error.value = 'Vui lòng nhập họ tên, số điện thoại (10–11 số) và email hợp lệ')
  busy.value = true; launch.value = 'run'
  const t0 = performance.now()
  try {
    const body = { ...f, giftMessage: f.giftWrap ? f.giftMessage : null, promoCode: promo.value?.code, usePoints: usePts.value ? maxPts.value : 0,
      giftCardCode: gift.value ? f.giftCardCode : null, referralCode: f.referralCode || null, items: items() }
    const { data } = guest.value ? await api.post('/public/checkout', body).then(r => ({ data: { ...r.data, paymentMethod: 'COD', guest: true } })) : await api.post('/orders', body)
    if (f.referralCode) localStorage.removeItem('refCode')
    if (rememberPay.value && auth.user?.preferredPayment !== f.paymentMethod) api.put('/account/payment-preference', { method: f.paymentMethod }).then(() => { auth.user.preferredPayment = f.paymentMethod; localStorage.setItem('user', JSON.stringify(auth.user)) }).catch(() => {})
    // cho xe kịp chạy tới chặng "Thanh toán" trước khi về đích
    await new Promise(r => setTimeout(r, Math.max(0, 560 - (performance.now() - t0))))
    placed.value = data; cart.clear(); launch.value = 'ok'
    if (['MOMO', 'VNPAY'].includes(data.paymentMethod)) setTimeout(() => { if (launch.value === 'ok') goPay() }, 2400)
  } catch (e) { error.value = errMsg(e); launch.value = 'err' } finally { busy.value = false }
}
onMounted(async () => {
  await loadCatalog(); quote(); bundleQuote().catch(() => {}); autoBest().catch(() => {})
  api.get('/promotions').then(r => { vouchers.value = r.data })
  if (guest.value) return
  api.get('/account/points').then(r => { pts.value = r.data })
  api.get('/account/wallet').then(r => { wallet.value = r.data }).catch(() => {})
  addresses.value = (await api.get('/account/addresses')).data
  const def = addresses.value.find(a => a.isDefault) || addresses.value[0]
  if (def) { addrId.value = def.id; useAddr(def) }
})
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 1fr 380px; gap: 20px; align-items: start; }
@media (max-width: 1023px) { .layout { grid-template-columns: 1fr; } .summary { position: static; } }
.gift { margin-top: 4px; padding: 10px 12px; border: 1.5px dashed var(--border); } .gift.on { border-color: var(--accent); background: var(--bronze-soft); }
.gift textarea { margin-top: 8px; } .remember { margin-top: 12px; font-size: .88rem; }
.mbar { display: none; }
@media (max-width: 899px) {
  .addr-list, .pay-grid { grid-template-columns: 1fr; }
  .place { display: none; }
  .mbar { display: flex; position: fixed; left: 0; right: 0; bottom: var(--tabbar-h, 0px); z-index: 70; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 16px; background: var(--surface); border-top: 2px solid var(--accent); box-shadow: 0 -8px 24px rgba(0,0,0,.08); }
  .mbar small { display: block; color: var(--muted); font-size: .72rem; } .mbar b { font-size: 1.15rem; } .mbar .btn { flex: 0 0 auto; min-width: 140px; }
  .layout { padding-bottom: 70px; }
}
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
.ship-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; } .pico.EXPRESS { background: var(--accent); color: #fff; }
.guestbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; padding: 12px 16px; border-left: 4px solid var(--accent); }
.extra summary { cursor: pointer; font-size: .86rem; font-weight: 600; padding: 4px 0; } .extra .promo { margin-top: 6px; }
.auto { margin-left: 6px; font-size: .68rem; background: var(--success); color: #fff; padding: 1px 6px; border-radius: 4px; }
.eta { background: var(--bg-2); padding: 8px 10px; border-radius: 8px; }
@media (max-width: 599px) { .addr-list, .pay-grid, .ship-grid { grid-template-columns: 1fr; } .remember { display: block; } }
</style>
