<template>

  <div class="container page">

    <h1 style="margin-bottom:20px">Thanh toán</h1>

    <div v-if="cartValidationErrors.length" class="alert alert-error" style="margin-bottom:16px;border-left:4px solid #ef4444;background:#fef2f2;padding:12px 16px;border-radius:8px">
      <div style="font-weight:700;margin-bottom:6px;display:flex;align-items:center;gap:6px;color:#b91c1c">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>PHÁT HIỆN SẢN PHẨM TẠM ẨN HOẶC HẾT HÀNG TRONG GIỎ:</span>
      </div>
      <ul style="margin:0;padding-left:20px;font-size:0.86rem;color:#dc2626">
        <li v-for="(err, eidx) in cartValidationErrors" :key="eidx">{{ err.message }}</li>
      </ul>
      <div style="margin-top:8px">
        <router-link to="/cart" class="btn btn-sm btn-outline">← Quay lại giỏ hàng để cập nhật</router-link>
      </div>
    </div>
    <div v-if="guest && cart.items.length" class="guestbar card"><span>Bạn đang đặt hàng <b>không cần tài khoản</b> (thanh toán khi nhận hàng). Hoá đơn gửi qua email.</span>

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



        <!-- BƯỚC 2: VẬN CHUYỂN THEO KHOẢNG CÁCH & ĐỐI TÁC VẬN CHUYỂN -->

        <div class="card block shipping-block">

          <div class="sec-header">

            <h3>2. Đơn vị vận chuyển & Cước phí theo khoảng cách</h3>

            <span class="hub-pill" title="Tổng kho xuất hàng"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px"><path d="M3 21h18M3 7v14M21 7v14M6 11h2M6 15h2M11 11h2M11 15h2M16 11h2M16 15h2M9 3h6l3 4H6z"/></svg> {{ shippingCalc?.originName || 'Showroom & Tổng kho ARES Hoàng Huy Commerce (Hải Phòng)' }}</span>

          </div>



          <!-- Khoảng cách & Lộ trình giao hàng -->

          <div v-if="shippingCalc" class="dist-banner">

            <div class="dist-icon"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="10" r="3"/><path d="M12 2a8 8 0 0 0-8 8c0 5.25 8 12 8 12s8-6.75 8-12a8 8 0 0 0-8-8z"/></svg></div>

            <div class="dist-main">

              <div class="dist-row">

                <span class="dist-lbl">Cự ly tính cước:</span>

                <b class="dist-km">{{ shippingCalc.distanceKm }} km</b>

                <span class="dist-dest">đến {{ shippingCalc.destinationDistrict ? shippingCalc.destinationDistrict + ', ' : '' }}{{ shippingCalc.destinationProvince }}</span>

              </div>

              <div class="dist-note">

                <span v-if="shippingCalc.isFreeShip" class="tag-free">Đơn từ 500.000đ: Đã kích hoạt MIỄN PHÍ CƯỚC gói tiêu chuẩn!</span>

                <span v-else class="tag-calc">Giá cước tính tự động theo khoảng cách km từ Tổng kho ARES qua API nhà vận chuyển</span>

              </div>

            </div>

          </div>



          <!-- Cảnh báo an toàn hàng cháy nổ / Pin LiPo -->

          <div v-if="hasHazardous" class="hazmat-checkout-alert">

            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#c2410c" stroke-width="2.2" style="flex-shrink:0;margin-top:2px"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>

            <div>

              <b>Lưu ý hàng dễ cháy nổ & Pin LiPo (+{{ vnd(hazardousFee) }})</b>

              <p>Kiện hàng chứa sản phẩm nhạy cảm ({{ hazardousItems.map(i => i.product?.name || i.name).slice(0, 2).join(', ') }}). Đã áp dụng phụ phí đóng gói cách nhiệt chống sốc & vận chuyển đường bộ an toàn theo quy định PCCC.</p>

            </div>

          </div>



          <!-- Danh sách nhà vận chuyển -->

          <div class="carrier-list" role="radiogroup" aria-label="Đơn vị vận chuyển">

            <label

              v-for="o in shipOpts"

              :key="o.method"

              :class="['carrier-card', { on: f.shippingMethod === o.method, instant: o.serviceType === 'INSTANT', express: o.serviceType === 'EXPRESS' }]"

            >

              <input v-model="f.shippingMethod" type="radio" :value="o.method" />

              <div class="carrier-left">

                <span :class="['carrier-badge', o.carrierId || o.method]">

                  {{ o.carrierId === 'GHN' ? 'GHN' : (o.carrierId === 'GHTK' ? 'GHTK' : (o.carrierId === 'VIETTEL_POST' ? 'VTP' : (o.carrierId === 'AHAMOVE' ? 'AHA' : (o.serviceType === 'EXPRESS' ? '⚡' : 'SHIP')))) }}

                </span>

                <div class="carrier-info">

                  <div class="carrier-title-row">

                    <b class="carrier-name">{{ o.carrierName || o.label }}</b>

                    <span v-if="o.serviceType === 'INSTANT'" class="pill-fast">Giao 2H</span>

                    <span v-else-if="o.serviceType === 'EXPRESS'" class="pill-express">Hỏa tốc</span>

                  </div>

                  <div class="carrier-desc small muted">{{ o.description || o.label }}</div>

                  <div class="carrier-meta">

                    <span class="eta-tag">Nhận: <b>{{ o.etaText || etaText(o) }}</b></span>

                    <span v-if="o.distanceKm" class="dist-tag">{{ o.distanceKm }} km</span>

                  </div>

                </div>

              </div>



              <div class="carrier-right">

                <template v-if="(freeShip && o.serviceType !== 'EXPRESS' && o.serviceType !== 'INSTANT') || o.isFree || o.fee === 0">

                  <span class="free-badge">Miễn phí</span>

                  <del v-if="o.rawFee" class="old-price">{{ vnd(o.rawFee) }}</del>

                </template>

                <template v-else>

                  <b class="fee-price">{{ vnd(o.fee) }}</b>

                  <del v-if="o.discount > 0" class="old-price">{{ vnd(o.rawFee) }}</del>

                </template>

              </div>

            </label>

          </div>

          <p v-if="!f.city" class="small muted" style="margin-top:10px">Vui lòng nhập địa chỉ / tỉnh thành ở bước 1 để hệ thống tính chính xác khoảng cách km và giá cước các hãng.</p>

        </div>



        <div class="card block">

          <h3>3. Phương thức thanh toán</h3>

          <div class="pay-grid">

            <label v-for="(v, k) in PAY_METHOD" :key="k" :class="['pay', { on: f.paymentMethod === k, disabled: payOff(k) }]">

              <input v-model="f.paymentMethod" type="radio" :value="k" :disabled="payOff(k)" />

              <span class="pico" :class="k">{{ icons[k] }}</span>

              <div><b>{{ v }}</b><div class="small muted">{{ guest && k !== 'COD' ? 'Đăng nhập để dùng' : k === 'INSTALLMENT' && total < instMin ? `Áp dụng cho đơn từ ${vnd(instMin)}` : gatewayOff(k) ? 'Tạm chưa hỗ trợ' : payDesc[k] }}</div></div>

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

          <img loading="lazy" decoding="async" :src="i.image" alt="" />
          <span class="grow">
            {{ i.name }} × {{ i.quantity }}
            <span v-if="getItemErr(i)" class="badge" :class="getItemErr(i).type === 'HIDDEN' || getItemErr(i).type === 'OUT_OF_STOCK' ? 'badge-CANCELLED' : 'badge-PENDING'" style="margin-left:4px;font-size:0.7rem">
              {{ getItemErr(i).type === 'HIDDEN' ? 'Tạm ẩn' : (getItemErr(i).type === 'OUT_OF_STOCK' ? 'Hết hàng' : `Còn ${getItemErr(i).stock}`) }}
            </span>
            <br /><span class="muted">{{ i.variantName }}</span>
          </span>
          <b>{{ vnd(i.unitPrice * i.quantity) }}</b>

        </div>

        <div class="row promo">

          <input v-model="promoCode" class="input grow" placeholder="Mã giảm giá" @keyup.enter="checkPromo(true)" />

          <button class="btn btn-sm btn-promo" @click="checkPromo(true)">Áp dụng</button>

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

        <div class="row between"><span>Phí vận chuyển <small v-if="curShip" class="muted">({{ curShip.carrierName || curShip.label }})</small></span><span>{{ !f.city ? 'Chọn tỉnh/thành' : shipFee ? vnd(shipFee) : 'Miễn phí' }}</span></div>

        <div v-if="hasHazardous" class="row between hazmat-sum-row" style="color:#c2410c;font-weight:600">

          <span>

            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="#ea580c" stroke-width="2.2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><path d="M12 2c0 0-6 4-6 10a6 6 0 0 0 12 0c0-6-6-10-6-10z"/><path d="M12 18a3 3 0 0 0 3-3c0-2-3-4-3-4s-3 2-3 4a3 3 0 0 0 3 3z"/></svg>

            Phụ phí pin & hàng cháy nổ

          </span>

          <b>+{{ vnd(hazardousFee) }}</b>

        </div>

        <div v-if="f.giftWrap" class="row between"><span>Gói quà</span><span>{{ vnd(GIFT_FEE) }}</span></div>

        <div v-if="creditOff" class="row between ok"><span>Thẻ quà / ví tín dụng</span><span>−{{ vnd(creditOff) }}</span></div>

        <div class="row between total"><span>Tổng cộng</span><span class="price">{{ vnd(total) }}</span></div>

        <div v-if="curShip && f.city" class="eta small">Dự kiến nhận hàng: <b>{{ curShip.etaText || etaText(curShip) }}</b> ({{ curShip.carrierName || 'Vận chuyển' }})</div>

        <div v-if="!guest" class="small muted">+{{ Math.floor(total / catalog.options.earnPer) }} điểm khi đơn hoàn tất</div>

        <div v-if="error" class="checkout-err-box" role="alert">

          <div class="err-title">

            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#ef4444" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>

            <b>LỖI ĐẶT HÀNG:</b>

          </div>

          <p class="err-msg">{{ error }}</p>

        </div>

        <button
          v-if="cartValidationErrors.length"
          type="button"
          class="btn btn-block"
          style="background:#94a3b8;cursor:not-allowed;color:#fff"
          @click="error = cartValidationErrors[0]?.message || 'Có sản phẩm đã tạm ẩn hoặc hết hàng!'; launch = 'err'"
        >
          Không thể đặt hàng (Có hàng lỗi)
        </button>
        <button v-else class="btn btn-block place" :disabled="busy" @click="submit">{{ busy ? 'Đang xử lý...' : 'Đặt hàng' }}</button>

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

  shippingMethod: localStorage.getItem('lastShip') || 'GHN', giftCardCode: '', referralCode: localStorage.getItem('refCode') || '', useCredit: false })

watch(() => f.shippingMethod, v => localStorage.setItem('lastShip', v))

// Phương thức online chỉ bật khi backend đã cấu hình cổng/tài khoản thật (GET /payment/config)
const payCfg = ref(null)
const gatewayOff = k => !!payCfg.value && ((k === 'MOMO' && !payCfg.value.momo) || (k === 'VNPAY' && !payCfg.value.vnpay) || ((k === 'BANK_QR' || k === 'INSTALLMENT') && !payCfg.value.bankQr))
const payOff = k => (guest.value && k !== 'COD') || (k === 'INSTALLMENT' && total.value < instMin.value) || gatewayOff(k)
api.get('/payment/config').then(r => { payCfg.value = r.data; if (payOff(f.paymentMethod)) f.paymentMethod = 'COD' }).catch(() => {})

const shipOpts = ref([

  { method: 'GHN', carrierId: 'GHN', carrierName: 'Giao Hàng Nhanh (GHN)', serviceType: 'STANDARD', label: 'GHN Tiêu chuẩn (Đường bộ)', fee: 26000, rawFee: 26000, distanceKm: 0, etaText: '2–3 ngày' },

  { method: 'GHTK', carrierId: 'GHTK', carrierName: 'Giao Hàng Tiết Kiệm (GHTK)', serviceType: 'STANDARD', label: 'GHTK Tiết kiệm', fee: 24000, rawFee: 24000, distanceKm: 0, etaText: '2–3 ngày' },

  { method: 'VIETTEL_POST', carrierId: 'VIETTEL_POST', carrierName: 'Viettel Post', serviceType: 'STANDARD', label: 'Viettel Tiết kiệm', fee: 28000, rawFee: 28000, distanceKm: 0, etaText: '2–4 ngày' }

])

const shippingCalc = ref(null)

const curShip = computed(() => shipOpts.value.find(o => o.method === f.shippingMethod) || shipOpts.value[0])

const DOW = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

const dshort = s => { if (!s) return '—'; const d = new Date(s + 'T00:00:00'); return `${DOW[d.getDay()]} ${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}` }

const etaText = o => !o?.etaFrom ? (o?.etaText || '—') : o.etaFrom === o.etaTo ? dshort(o.etaFrom) : `${dshort(o.etaFrom)} – ${dshort(o.etaTo)}`

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

const icons = { COD: 'COD', BANK_QR: 'QR', MOMO: 'MOMO', VNPAY: 'VNPAY', INSTALLMENT: '%' }

const payDesc = { COD: 'Trả tiền mặt khi nhận hàng', BANK_QR: 'Quét VietQR bằng app ngân hàng', MOMO: 'Thanh toán qua ví MoMo', VNPAY: 'Thẻ ATM / QR VNPay', INSTALLMENT: '0% lãi, 3–12 tháng' }

const instMin = computed(() => catalog.options.installmentMin)

const after = computed(() => Math.max(0, cart.subtotal - discount.value - bundleOff.value - refOff.value))

const maxPts = computed(() => Math.min(pts.value.points, Math.floor(after.value / 2 / catalog.options.pointValue)))

const ptsDiscount = computed(() => usePts.value ? maxPts.value * catalog.options.pointValue : 0)

const shipFee = computed(() => {

  if (freeShip.value && curShip.value && curShip.value.serviceType !== 'EXPRESS' && curShip.value.serviceType !== 'INSTANT') return 0

  return curShip.value?.fee || 0

})

function isItemHazardous(name) {

  if (!name) return false

  const lower = name.toLowerCase()

  return lower.includes('pin ') || lower.startsWith('pin') || lower.includes('lipo') || lower.includes('battery') || lower.includes('ắc quy') || lower.includes('ac quy') || lower.includes('cháy nổ')

}



const hazardousItems = computed(() => {

  return cart.items.filter(it => isItemHazardous(it.name) || isItemHazardous(it.product?.name) || it.product?.hazardous || it.product?.type === 'BATTERY')

})



const hasHazardous = computed(() => hazardousItems.value.length > 0)



const hazardousFee = computed(() => {

  if (!hasHazardous.value) return 0

  let maxFee = 25000

  for (const it of hazardousItems.value) {

    if (it.product?.hazardousFee && it.product.hazardousFee > maxFee) {

      maxFee = it.product.hazardousFee

    }

  }

  return maxFee

})



const gross = computed(() => after.value - ptsDiscount.value + (f.city ? shipFee.value : 0) + (f.giftWrap ? GIFT_FEE : 0) + hazardousFee.value)

const creditOff = computed(() => { let c = 0, left = gross.value; if (gift.value) { c += Math.min(gift.value.balance, left); left -= c } if (f.useCredit) c += Math.min(wallet.value.storeCredit, left); return c })

const total = computed(() => gross.value - creditOff.value)

const fmtAddr = a => [a.line, a.ward, a.district, a.city].filter(Boolean).join(', ')

const useAddr = a => { f.buyerName = a.receiverName; f.phone = a.phone; f.shippingAddress = [a.line, a.ward, a.district].filter(Boolean).join(', '); f.city = toShipCity(a.city) }



const quote = async () => {

  if (!f.city) {

    shippingCalc.value = null

    return

  }

  try {

    const res = await api.get('/public/shipping-quotes', {

      params: {

        province: f.city,

        district: addr.value?.district || undefined,

        ward: addr.value?.ward || undefined,

        address: f.shippingAddress || undefined,

        amount: after.value - ptsDiscount.value,

        hasHazardous: hasHazardous.value

      }

    })

    shippingCalc.value = res.data

    const quotes = res.data?.quotes || []

    if (quotes.length) {

      shipOpts.value = quotes.map(q => ({

        method: q.carrierId,

        carrierId: q.carrierId,

        carrierName: q.carrierName,

        serviceType: q.serviceType,

        label: q.serviceName,

        logo: q.logo,

        fee: q.finalFee,

        rawFee: q.rawFee,

        discount: q.discount,

        isFree: q.isFree,

        distanceKm: q.distanceKm,

        etaText: q.etaText,

        etaFrom: q.etaFrom,

        etaTo: q.etaTo,

        description: q.description

      }))

      if (!shipOpts.value.some(o => o.method === f.shippingMethod)) {

        f.shippingMethod = shipOpts.value[0]?.method || 'GHN'

      }

    }

  } catch (e) {

    try {

      const opts = (await api.get('/public/shipping-options', { params: { city: f.city, amount: after.value - ptsDiscount.value } })).data

      if (Array.isArray(opts) && opts.length) shipOpts.value = opts

    } catch {}

  }

}



watch([() => f.city, () => addr.value?.district, () => f.shippingAddress, after, ptsDiscount], quote)

const items = () => {

  const list = []

  for (const i of cart.items) {

    if (i.isBundle && i.bundleProducts?.length) {

      const sum = i.bundleProducts.reduce((a, b) => a + (b.price || 0), 0)

      const ratio = i.unitPrice / (sum || 1)

      let allocated = 0

      i.bundleProducts.forEach((bp, idx) => {

        let pPrice = Math.round((bp.price || 0) * ratio)

        if (idx === i.bundleProducts.length - 1) {

          pPrice = i.unitPrice - allocated

        } else {

          allocated += pPrice

        }

        list.push({

          productId: bp.productId || bp.id,

          variantId: bp.variantId || null,

          quantity: (bp.quantity || 1) * i.quantity,

          unitPrice: pPrice

        })

      })

    } else {

      list.push({ productId: i.productId, variantId: i.variantId, quantity: i.quantity, unitPrice: i.unitPrice })

    }

  }

  return list

}

const bundleQuote = async () => {

  const loose = cart.items.filter(i => !i.isBundle)

  bundleOff.value = loose.length ? (await api.post('/public/bundle-quote', loose.map(i => ({ productId: i.productId, variantId: i.variantId, quantity: i.quantity, unitPrice: i.unitPrice })))).data.discount : 0

}

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

const cartValidationErrors = ref([])
async function validateCartItems() {
  if (!cart.items.length) { cartValidationErrors.value = []; return }
  try {
    const list = items()
    const payload = list.map(i => ({ productId: i.productId, variantId: i.variantId || null, quantity: i.quantity }))
    const { data } = await api.post('/public/cart-validate', payload)
    cartValidationErrors.value = data.errors || []
  } catch (e) {
    console.error('Cart validate error', e)
  }
}
function getItemErr(i) {
  return cartValidationErrors.value.find(e => e.productId === i.productId && (!i.variantId || e.variantId === i.variantId))
}

async function submit() {
  error.value = ''
  await validateCartItems()
  if (cartValidationErrors.value.length) {
    error.value = cartValidationErrors.value[0]?.message || 'Giỏ hàng có sản phẩm đã tạm ẩn hoặc hết hàng!'
    launch.value = 'err'
    return
  }

  if (!f.city) return (error.value = 'Vui lòng chọn tỉnh/thành để tính phí vận chuyển')

  if ((addrId.value === 0 || !addresses.value.length) && !addr.value.valid) return (error.value = 'Vui lòng chọn đủ tỉnh/thành, quận/huyện, phường/xã từ gợi ý và nhập số nhà')

  if (guest.value) {
    if (!f.buyerName.trim() || !/^\S+@\S+\.\S+$/.test(f.email) || !/^0\d{9,10}$/.test(f.phone.replace(/\s/g, ''))) {
      return (error.value = 'Vui lòng nhập họ tên, số điện thoại (10–11 số) và email hợp lệ')
    }
  } else {
    if (!f.buyerName || !f.buyerName.trim()) return (error.value = 'Vui lòng nhập họ tên người nhận hàng')
    if (!f.phone || !f.phone.trim()) return (error.value = 'Vui lòng nhập số điện thoại nhận hàng')
  }

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

    placed.value = data

    cart.clear()

    // Nếu chọn thanh toán online (QR, Momo, VNPay) => chuyển ngay đến cổng thanh toán, KHÔNG hiện popup thành công trước khi trả tiền

    if (['BANK_QR', 'MOMO', 'VNPAY'].includes(data.paymentMethod)) {

      launch.value = 'idle'

      goPay()

      return

    }

    // Chỉ khi là COD (Thanh toán khi nhận hàng) mới hiện popup đặt hàng thành công

    launch.value = 'ok'

  } catch (e) { error.value = e?.response?.data?.message || (typeof e === 'string' ? e : errMsg(e)); launch.value = 'err' } finally { busy.value = false }

}

onMounted(async () => {

  await loadCatalog(); quote(); bundleQuote().catch(() => {}); autoBest().catch(() => {}); validateCartItems().catch(() => {})

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

.block { margin-bottom: 16px; } .block h3 { margin-bottom: 0; }

.addr-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 12px; }

.addr { display: flex; gap: 10px; padding: 10px 12px; border: 1.5px solid var(--border); border-radius: 10px; cursor: pointer; } .addr.on { border-color: var(--bronze); background: var(--bronze-soft); }

.pay-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 12px; }

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



/* SHIPPING & DISTANCE CALCULATIONS */

.sec-header { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }

.hub-pill { font-size: .76rem; font-weight: 600; padding: 4px 10px; border-radius: 999px; background: rgba(224, 86, 36, .12); color: var(--accent); border: 1px solid rgba(224, 86, 36, .25); }

.dist-banner { display: flex; align-items: center; gap: 12px; padding: 12px 14px; background: var(--bg-2); border-radius: 10px; border: 1px solid var(--border); margin-bottom: 12px; }

.dist-icon { font-size: 1.5rem; line-height: 1; flex-shrink: 0; }

.dist-main { display: flex; flex-direction: column; gap: 3px; font-size: .86rem; }

.dist-row { display: flex; align-items: baseline; gap: 6px; flex-wrap: wrap; }

.dist-lbl { color: var(--muted); }

.dist-km { font-size: 1.15rem; color: var(--accent); font-weight: 800; }

.dist-dest { color: var(--text); font-weight: 600; }

.dist-note { font-size: .78rem; }

.tag-free { color: var(--success); font-weight: 700; }

.tag-calc { color: var(--muted); }



.carrier-list { display: flex; flex-direction: column; gap: 10px; }

.carrier-card { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px 14px; border: 1.5px solid var(--border); border-radius: 12px; cursor: pointer; transition: all .2s ease; background: var(--surface); }

.carrier-card:hover { border-color: rgba(224, 86, 36, .5); }

.carrier-card.on { border-color: var(--accent); background: rgba(224, 86, 36, .05); box-shadow: 0 0 0 1px var(--accent); }

.carrier-card input { display: none; }

.carrier-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }

.carrier-badge { width: 50px; height: 38px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: .75rem; font-weight: 800; letter-spacing: .5px; flex-shrink: 0; text-transform: uppercase; color: #fff; background: #475569; }

.carrier-badge.GHN, .carrier-badge.GHN_EXPRESS { background: #ea580c; }

.carrier-badge.GHTK { background: #166534; }

.carrier-badge.VIETTEL_POST { background: #b91c1c; }

.carrier-badge.AHAMOVE { background: #d97706; }

.carrier-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }

.carrier-title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.carrier-name { font-size: .95rem; }

.pill-fast { font-size: .7rem; padding: 1px 7px; border-radius: 4px; background: #fef08a; color: #854d0e; font-weight: 700; }

.pill-express { font-size: .7rem; padding: 1px 7px; border-radius: 4px; background: #fee2e2; color: #991b1b; font-weight: 700; }

.carrier-desc { font-size: .8rem; }

.carrier-meta { display: flex; align-items: center; gap: 8px; font-size: .78rem; margin-top: 2px; }

.carrier-meta .eta-tag { color: var(--text); }

.carrier-meta .dist-tag { color: var(--accent); font-weight: 600; }

.carrier-right { text-align: right; flex-shrink: 0; display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }

.fee-price { font-size: 1rem; color: var(--text); }

.free-badge { color: var(--success); font-weight: 800; font-size: .95rem; }

.old-price { color: var(--muted); font-size: .78rem; }



.guestbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; padding: 12px 16px; border-left: 4px solid var(--accent); }

.extra summary { cursor: pointer; font-size: .86rem; font-weight: 600; padding: 4px 0; } .extra .promo { margin-top: 6px; }

.auto { margin-left: 6px; font-size: .68rem; background: var(--success); color: #fff; padding: 1px 6px; border-radius: 4px; }

.eta { background: var(--bg-2); padding: 8px 10px; border-radius: 8px; }

@media (max-width: 599px) { .addr-list, .pay-grid { grid-template-columns: 1fr; } .remember { display: block; } .carrier-card { flex-direction: column; align-items: flex-start; } .carrier-right { align-items: flex-start; } }

.checkout-err-box {

  background: #fef2f2;

  border: 1.5px solid #fecaca;

  border-radius: 8px;

  padding: 12px 14px;

  margin: 12px 0;

  text-align: left;

}

.err-title {

  display: flex;

  align-items: center;

  gap: 8px;

  color: #b91c1c;

  font-size: 0.88rem;

  font-weight: 700;

  letter-spacing: .02em;

}

.err-msg {

  margin: 4px 0 0 26px;

  color: #dc2626;

  font-size: 0.92rem;

  font-weight: 600;

  line-height: 1.4;

}

.hazmat-checkout-alert { display: flex; gap: 10px; background: #fff7ed; border: 1px solid #fdba74; border-radius: 8px; padding: 12px 14px; margin-bottom: 16px; color: #9a3412; font-size: .85rem; }

.hazmat-checkout-alert b { display: block; font-size: .88rem; color: #c2410c; margin-bottom: 2px; }

.hazmat-checkout-alert p { margin: 0; line-height: 1.35; font-size: .82rem; }

.hazmat-tag { display: inline-flex; align-items: center; gap: 3px; background: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; padding: 1px 5px; border-radius: 3px; font-size: .7rem; font-weight: 700; margin-left: 6px; vertical-align: middle; }


.btn-promo {
  background: var(--accent, #a87a3c) !important;
  color: #fff !important;
  border: 1px solid var(--accent, #a87a3c) !important;
  font-weight: 600 !important;
  min-width: 86px;
  cursor: pointer;
}
.btn-promo:hover {
  filter: brightness(1.1);
  color: #fff !important;
}

</style>

