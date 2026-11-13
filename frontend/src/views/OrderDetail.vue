<template>

  <div class="container page">

    <div v-if="!o && err && !loaded" class="nf"><h1>Không mở được đơn hàng</h1><p class="muted">{{ err }}</p><router-link to="/account?tab=orders" class="btn">← Đơn hàng của tôi</router-link></div>

    <div v-else-if="!o" class="spinner" />

    <template v-else>

      <Breadcrumb :items="[{ label: 'Tài khoản', to: '/account?tab=orders' }, { label: 'Đơn ' + o.code }]" />

      <div v-if="$route.query.new" class="alert alert-success">Đặt hàng thành công! Mã đơn của bạn là <b>{{ o.code }}</b>.</div>

      <div v-if="o.giftWrap" class="alert gift-note"><b>Gói quà</b> (+{{ vnd(o.giftFee) }})<span v-if="o.giftMessage"> · Thiệp: “{{ o.giftMessage }}”</span></div>

          <div v-if="route.query.pay_status === 'failed'" class="alert alert-warning" style="margin-bottom:18px;border-left:4px solid #ef4444;background:#fff5f5;color:#991b1b;padding:14px 18px;border-radius:8px">

      <div style="font-weight:700;font-size:1.05rem;margin-bottom:4px;display:flex;align-items:center;gap:8px">

        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#ef4444" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>

        <span>GIAO DỊCH CHƯA HOÀN TẤT HOẶC ĐÃ BỊ HUỶ</span>

      </div>

      <p style="margin:0;font-size:.9rem;line-height:1.4">Đơn hàng của bạn đang ở trạng thái <b>Chưa thanh toán</b>. Bạn có thể bấm nút <b>Quét mã QR thanh toán ngay</b> bên dưới để thử lại bất cứ lúc nào.</p>

    </div>



    <div class="row between"><h1>Đơn hàng {{ o.code }}</h1>

        <div class="row acts"><button class="btn btn-sm" :disabled="busy" @click="reorder"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg>Mua lại</button><button class="btn btn-sm btn-outline" :disabled="mailed" @click="mailInvoice">

            <template v-if="mailed">

              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><polyline points="20 6 9 17 4 12"/></svg>

              <span>Đã gửi email</span>

            </template>

            <template v-else>

              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>

              <span>Gửi hoá đơn</span>

            </template>

          </button><router-link :to="`/orders/${o.code}/invoice`" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-outline"><svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>Hoá đơn</router-link><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span></div></div>

      <p class="muted">Đặt lúc {{ dt(o.createdAt) }}<template v-if="o.etaFrom && !['COMPLETED', 'CANCELLED'].includes(o.status)"> · Dự kiến giao <b>{{ dshort(o.etaFrom) }}{{ o.etaTo !== o.etaFrom ? ' – ' + dshort(o.etaTo) : '' }}</b> ({{ o.shippingMethod === 'EXPRESS' ? 'hoả tốc' : 'tiêu chuẩn' }})</template></p>



      <div v-reveal class="card track">
        <div class="row between track-header">
          <h3>Theo dõi đơn hàng</h3>
          <span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] || o.status }}</span>
        </div>

        <!-- Khối thông báo khi đơn huỷ hoặc đã hoàn tiền -->
        <div v-if="o.status === 'CANCELLED'" class="cancel-alert-box">
          <div class="cab-icon">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          </div>
          <div>
            <b style="color:var(--danger)">Đơn hàng đã được huỷ</b>
            <p v-if="o.cancelReason" class="small muted" style="margin:2px 0 0">Lý do: {{ o.cancelReason }}</p>
            <p v-if="o.paymentStatus === 'REFUNDED' || o.refundedAmount" class="small ok" style="margin:4px 0 0;font-weight:700">
              Đã tự động hoàn trả {{ vnd(o.refundedAmount || o.paidAmount || o.total) }} về phương thức thanh toán ban đầu ({{ o.paymentMethod || 'MoMo' }}).
            </p>
          </div>
        </div>

        <!-- Trục tiến trình đường chạy xe mô hình liên kết mốc sự kiện -->
        <div v-else class="road-track-container">
          <div class="road-highway">
            <div class="road-highway-dashes" />
            <div class="road-highway-fill" :style="{ width: progress + '%' }" />

            <!-- Xe vận chuyển / xe mô hình ARES chạy mượt theo tiến trình -->
            <div class="vehicle-runner" :style="{ left: progress + '%' }">
              <div class="runner-icon-wrap" title="Đơn hàng đang ở chặng này">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 3h15v13H1zM16 8h4l3 3v5h-7V8z" fill="#0f172a" stroke="#0f172a"/>
                  <circle cx="5.5" cy="18.5" r="2.5" fill="#f05a1a" stroke="#fff" stroke-width="1.5"/>
                  <circle cx="18.5" cy="18.5" r="2.5" fill="#f05a1a" stroke="#fff" stroke-width="1.5"/>
                </svg>
              </div>
            </div>

            <!-- Các mốc điểm kiểm tra (Checkpoints) cố định trên mặt đường -->
            <div class="road-checkpoints">
              <div
                v-for="(st, idx) in steps"
                :key="st.key"
                :class="['checkpoint-node', st.state, { 'is-active': idx === activeIndex }]"
              >
                <div class="checkpoint-disc">
                  <svg v-if="st.state === 'done'" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                  <span v-else class="checkpoint-core" />
                </div>
              </div>
            </div>
          </div>

          <!-- Chi tiết thông tin tương ứng từng chặng (Căn chỉnh đồng bộ) -->
          <div class="road-details-row">
            <div
              v-for="st in steps"
              :key="st.key"
              :class="['road-step-col', st.state]"
            >
              <b class="rsc-title">{{ st.label }}</b>
              <p class="rsc-desc">{{ st.desc }}</p>
              <small v-if="st.time" class="rsc-time">{{ dt(st.time) }}</small>
            </div>
          </div>
        </div>
      </div>



      <div v-if="needPay" v-reveal class="card paybox">

        <template v-if="o.paymentMethod === 'BANK_QR'">

          <img loading="lazy" decoding="async" :src="bank.qr" alt="VietQR" class="qr" />

          <div><h3>Chuyển khoản qua VietQR</h3>

            <p>Ngân hàng <b>{{ bank.bank }}</b> · STK <b>{{ bank.account }}</b> · {{ bank.holder }}</p>

            <p>Số tiền <b class="price">{{ vnd(o.total - o.paidAmount) }}</b> · Nội dung <b>{{ o.code }}</b></p>

            <p class="small muted">Môi trường demo: nhấn nút dưới để giả lập ngân hàng báo có.</p>

            <button class="btn btn-sm" @click="act('pay')">Tôi đã chuyển khoản</button></div>

        </template>

        <template v-else-if="['MOMO', 'VNPAY'].includes(o.paymentMethod)">

          <div><h3>Chưa hoàn tất thanh toán {{ o.paymentMethod }}</h3><router-link :to="`/pay/${o.code}`" class="btn btn-sm">Thanh toán ngay →</router-link></div>

        </template>

        <template v-else-if="o.paymentMethod === 'INSTALLMENT'">

          <div><h3>Trả góp 0% · {{ o.installmentMonths }} kỳ × {{ vnd(o.monthlyInstallment) }}</h3>

            <p class="small muted">Đã thanh toán {{ vnd(o.paidAmount) }} / {{ vnd(o.total) }}</p>

            <button class="btn btn-sm" @click="act('pay')">Thanh toán kỳ tiếp theo (giả lập thẻ)</button></div>

        </template>

      </div>



      <div class="grid grid-2">

        <div class="card">

          <h3>Sản phẩm</h3>

          <div v-for="i in o.items" :key="i.id" class="item">

            <img loading="lazy" decoding="async" v-if="i.image" :src="i.image" alt="" />

            <div class="grow"><router-link v-if="i.productId" :to="`/products/${i.productId}`">{{ i.productName }}</router-link><span v-else>{{ i.productName }}</span> × {{ i.quantity }}

              <div class="small muted">{{ i.variantName }}<span v-if="i.flashSale"> · Flash sale</span><span v-if="i.warrantyMonths"> · BH {{ i.warrantyMonths }} tháng</span></div></div>

            <b>{{ vnd(i.unitPrice * i.quantity) }}</b>

            <router-link v-if="o.status === 'COMPLETED' && i.productId" :to="`/products/${i.productId}#write-review`" class="btn btn-sm btn-outline rvb">★ Đánh giá</router-link>

          </div>

          <div class="row between"><span>Tạm tính</span><span>{{ vnd(o.subtotal) }}</span></div>

          <div v-if="o.bundleDiscount" class="row between"><span>Combo ưu đãi</span><span>−{{ vnd(o.bundleDiscount) }}</span></div>

          <div v-if="o.discount - (o.bundleDiscount || 0) > 0" class="row between"><span>Giảm giá<template v-if="o.promoCode"> ({{ o.promoCode }})</template></span><span>−{{ vnd(o.discount - (o.bundleDiscount || 0)) }}</span></div>

          <div v-if="o.pointsDiscount" class="row between"><span>Dùng {{ o.pointsUsed }} điểm</span><span>−{{ vnd(o.pointsDiscount) }}</span></div>

          <div class="row between"><span>Phí vận chuyển<small v-if="o.shippingMethod === 'EXPRESS'" class="muted"> (hoả tốc)</small></span><span>{{ o.shippingFee ? vnd(o.shippingFee) : 'Miễn phí' }}</span></div>

          <div v-if="o.hazardousFee" class="row between" style="color:#c2410c;font-weight:600">

            <span>

              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="#ea580c" stroke-width="2.2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><path d="M12 2c0 0-6 4-6 10a6 6 0 0 0 12 0c0-6-6-10-6-10z"/><path d="M12 18a3 3 0 0 0 3-3c0-2-3-4-3-4s-3 2-3 4a3 3 0 0 0 3 3z"/></svg>

              Phụ phí pin & hàng cháy nổ

            </span>

            <span>+{{ vnd(o.hazardousFee) }}</span>

          </div>

          <div v-if="o.giftFee" class="row between"><span>Gói quà</span><span>{{ vnd(o.giftFee) }}</span></div>

          <div v-if="o.creditUsed" class="row between"><span>Thẻ quà / ví tín dụng</span><span>−{{ vnd(o.creditUsed) }}</span></div>

          <div class="row between total"><span>Tổng cộng</span><span class="price">{{ vnd(o.total) }}</span></div>

          <p v-if="o.pointsEarned" class="small ok">+{{ o.pointsEarned }} điểm thưởng đã được cộng</p>

        </div>

        <div>

          <div class="card" style="margin-bottom:18px">

            <h3>Giao nhận</h3>

            <p><b>{{ o.buyerName }}</b> · {{ o.phone }}</p><p class="muted">{{ o.email }}</p><p>{{ o.shippingAddress }}</p>

            <p v-if="o.carrierName || o.shippingMethod" class="small"><b>Đơn vị vận chuyển:</b> {{ o.carrierName || o.shippingMethod }}</p>

            <div v-if="o.hasHazardous || o.hazardousFee" class="hazmat-alert-box" style="background:#fff7ed;border:1px solid #fdba74;border-radius:6px;padding:8px 10px;margin:8px 0;font-size:.8rem;color:#9a3412;display:flex;gap:6px;align-items:flex-start">

              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="#ea580c" stroke-width="2.2" style="flex-shrink:0;margin-top:2px"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>

              <span><b>Lưu ý an toàn:</b> Kiện hàng chứa sản phẩm pin LiPo / dễ cháy nổ. Đã đóng gói túi cách nhiệt chống cháy và kiểm định an toàn vận chuyển đường bộ.</span>

            </div>

            <p v-if="o.shippingDistanceKm" class="small muted">Cự ly vận chuyển: ~{{ o.shippingDistanceKm }} km (từ Showroom ARES Hoàng Huy Commerce Hải Phòng)</p>

            <p v-if="o.note" class="muted small">Ghi chú: {{ o.note }}</p>

          </div>

          <div class="card">

            <h3>Thanh toán</h3>

            <p>{{ PAY_METHOD[o.paymentMethod] }} <span :class="['badge', 'badge-' + o.paymentStatus]">{{ PAY_STATUS[o.paymentStatus] }}</span></p>

            <div class="row between"><span>Đã thanh toán</span><b class="ok">{{ vnd(o.paidAmount) }}</b></div>

            <p v-if="o.transactionRef" class="small muted">Mã giao dịch: {{ o.transactionRef }}</p>

            <div v-if="o.refundedAmount" class="row between"><span>Đã hoàn tiền</span><b>{{ vnd(o.refundedAmount) }}</b></div>

            <div v-if="o.paymentStatus !== 'PAID' && o.status !== 'CANCELLED' && ['BANK_QR', 'MOMO', 'VNPAY'].includes(o.paymentMethod)" style="margin-top:12px">

              <router-link :to="'/pay/' + o.code" class="btn btn-sm btn-block" style="background:#f05a1a;color:#fff;display:flex;align-items:center;justify-content:center;gap:6px">

                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>

                <span>Quét mã QR thanh toán ngay ({{ vnd(o.total - o.paidAmount) }})</span>

              </router-link>

            </div>

            <div class="row" style="margin-top:12px; flex-wrap:wrap">

              <button v-if="['PENDING', 'CONFIRMED'].includes(o.status)" class="btn btn-sm btn-danger" @click="cancelOpen = !cancelOpen">Huỷ đơn</button>

              <button v-if="canReturn" class="btn btn-sm btn-outline" @click="retOpen = !retOpen">Đổi trả / Hoàn tiền</button>

              <router-link :to="{ path: '/support', query: { order: o.code } }" class="btn btn-sm btn-outline">Cần hỗ trợ?</router-link>

            </div>

            <div v-if="cancelOpen" class="sub">

              <select v-model="cancelReason" class="input"><option v-for="r in reasons" :key="r">{{ r }}</option></select>

              <button class="btn btn-sm btn-danger" @click="cancel">Xác nhận huỷ</button>

            </div>

            <ReturnWizard v-if="retOpen" :order="o" @cancel="retOpen = false" @done="retDone" />

            <div v-for="r in returns" :key="r.id" class="retbox">

              <b>{{ r.kind === 'REFUND' ? 'Yêu cầu hoàn tiền' : 'Yêu cầu đổi trả' }}</b> <span :class="['badge', 'badge-' + r.status]">{{ RETURN_STATUS[r.status] }}</span>

              <p class="small">{{ r.reason }}</p><p v-if="r.adminNote" class="small muted">Shop: {{ r.adminNote }}</p>

            </div>

            <div v-if="err" class="alert alert-error" style="margin-top:10px">{{ err }}</div>

          </div>

        </div>

      </div>

      <router-link to="/account?tab=orders" style="display:inline-block;margin-top:20px">← Lịch sử đơn hàng</router-link>

    </template>

  </div>

</template>

<script setup>

import ReturnWizard from '../components/ReturnWizard.vue'

import Breadcrumb from '../components/Breadcrumb.vue'

import { ref, reactive, computed, onMounted } from 'vue'

import { useRoute, useRouter } from 'vue-router'

import { addById } from '../cartAdd'

import api, { errMsg, uploadImage } from '../api'

import { vnd, dt, ORDER_STATUS, PAY_METHOD, PAY_STATUS, RETURN_STATUS } from '../format'

import { toast } from '../stores/prefs'



const route = useRoute(), router = useRouter()

const busy = ref(false), mailed = ref(false)

const dshort = s => new Date(s + 'T00:00:00').toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' })

async function reorder() {

  busy.value = true; let n = 0; const its = o.value.items.filter(i => i.productId)

  try { for (const i of its) if (await addById(i.productId, i.quantity, i.variantId)) n++ } finally { busy.value = false }

  if (!n) return toast('Các sản phẩm trong đơn đã hết hàng', 'error')

  toast(n === its.length ? 'Đã thêm lại toàn bộ sản phẩm vào giỏ' : `Đã thêm ${n}/${its.length} sản phẩm (một số đã hết hàng)`); router.push('/cart')

}

async function mailInvoice() { try { const { data } = await api.post(`/orders/${o.value.code}/email-invoice`); mailed.value = true; toast(`Đã gửi hoá đơn tới ${data.sentTo}`) } catch (e) { toast(errMsg(e), 'error') } }

const o = ref(null), bank = ref({}), returns = ref([]), err = ref(''), cancelOpen = ref(false), retOpen = ref(false)

const reasons = ['Đổi ý, không muốn mua nữa', 'Muốn đổi sang sản phẩm/màu khác', 'Tìm được giá tốt hơn', 'Thời gian giao quá lâu', 'Lý do khác']

const cancelReason = ref(reasons[0]), ret = reactive({ kind: 'RETURN', reason: '', images: [] })

const STEP_DESC = { PENDING: 'Shop đã nhận đơn', CONFIRMED: 'Đang đóng gói sản phẩm', SHIPPING: 'Đơn vị vận chuyển đang giao', COMPLETED: 'Giao hàng thành công' }

const KEYS = ['PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED']

const steps = computed(() => {

  const tl = o.value.timeline || {}, idx = KEYS.indexOf(o.value.status)

  const list = KEYS.map((k, i) => ({ key: k, label: ORDER_STATUS[k], desc: STEP_DESC[k], time: tl[k], state: idx < 0 ? (tl[k] ? 'done' : '') : i < idx ? 'done' : i === idx ? 'current' : '' }))

  if (o.value.status === 'CANCELLED') list.push({ key: 'C', label: 'Đã huỷ', desc: o.value.cancelReason || '', time: tl.CANCELLED, state: 'cancel' })

  if (o.value.status === 'RETURNED') list.push({ key: 'R', label: 'Đã trả hàng & hoàn tiền', desc: '', time: tl.RETURNED, state: 'cancel' })

  return list

})

const activeIndex = computed(() => {
  const i = KEYS.indexOf(o.value.status)
  return i < 0 ? 0 : i
})

const progress = computed(() => {
  if (o.value.status === 'CANCELLED' || o.value.status === 'RETURNED') return 0
  const i = KEYS.indexOf(o.value.status)
  if (i < 0) return 0
  return (i / (KEYS.length - 1)) * 100
})

const needPay = computed(() => o.value.status !== 'CANCELLED' && o.value.paymentMethod !== 'COD' && o.value.paidAmount < o.value.total)

const canReturn = computed(() => o.value.status === 'COMPLETED' && !returns.value.some(r => ['PENDING', 'APPROVED'].includes(r.status)))

const loaded = ref(false)

const load = async () => { try { const { data } = await api.get(`/orders/${route.params.code}`); o.value = data.order; bank.value = data.bank; returns.value = data.returns; loaded.value = true } catch (e) { err.value = errMsg(e) } }

async function act(a) { err.value = ''; try { await api.post(`/orders/${o.value.code}/${a}`); toast('Thanh toán thành công'); await load() } catch (e) { err.value = errMsg(e) } }

async function cancel() { err.value = ''; try { await api.post(`/orders/${o.value.code}/cancel`, { reason: cancelReason.value }); cancelOpen.value = false; toast('Đã huỷ đơn hàng'); await load() } catch (e) { err.value = errMsg(e) } }

async function upload(e) { try { for (const f of [...e.target.files].slice(0, 4)) ret.images.push(await uploadImage(f)) } catch (x) { err.value = errMsg(x) } }

const retDone = async () => { retOpen.value = false; toast('Đã gửi yêu cầu đổi trả'); await load() }

async function sendReturn() { err.value = ''; try { await api.post(`/orders/${o.value.code}/returns`, ret); retOpen.value = false; toast('Đã gửi yêu cầu'); await load() } catch (e) { err.value = errMsg(e) } }

onMounted(load)

</script>

<style scoped>

.acts { gap: 8px; flex-wrap: wrap; justify-content: flex-end; }



.track { margin: 18px 0; }

.road { position: relative; height: 10px; margin: 26px 12px 8px; border-radius: 5px; background: #3a3a3e; }

.road::after { content: ''; position: absolute; left: 0; right: 0; top: 4.5px; height: 1px; background: repeating-linear-gradient(90deg, #f5c400 0 10px, transparent 10px 20px); }

.fill { position: absolute; inset: 0 auto 0 0; border-radius: 5px; background: linear-gradient(90deg, var(--bronze), #f5c400); transition: width 1.4s cubic-bezier(.2,.8,.2,1); }

.truck { position: absolute; top: -22px; transform: translateX(-50%) scaleX(-1); font-size: 1.4rem; transition: left 1.4s cubic-bezier(.2,.8,.2,1); }

.tl { list-style: none; display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin-top: 10px; padding: 0; }

.tl li { display: flex; gap: 8px; opacity: .45; } .tl li.done, .tl li.current, .tl li.cancel { opacity: 1; }

.dot { width: 14px; height: 14px; border-radius: 50%; background: #ccc; margin-top: 3px; flex-shrink: 0; } .done .dot { background: var(--success); } .current .dot { background: var(--bronze); box-shadow: 0 0 0 4px var(--bronze-soft); } .cancel .dot { background: var(--danger); }

.tl p { font-size: .8rem; color: var(--muted); } .tl small { font-size: .74rem; color: var(--muted); }

.paybox { display: flex; gap: 20px; align-items: center; margin-bottom: 18px; border: 2px solid var(--bronze); } .qr { width: 170px; border-radius: 8px; background: #fff; }

.paybox p { margin: 4px 0; }

.item { display: flex; gap: 10px; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--border); } .item img { width: 60px; height: 44px; object-fit: cover; border-radius: 6px; }

.total { font-weight: 700; margin-top: 8px; } .ok { color: var(--success); }

.sub { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; padding: 12px; background: var(--bg-2); border-radius: 10px; }

.check { display: flex; gap: 6px; align-items: center; font-size: .88rem; } .up { border: 1px dashed var(--bronze); color: var(--bronze); border-radius: 8px; padding: 5px 10px; cursor: pointer; font-size: .85rem; } .ti { width: 40px; height: 40px; object-fit: cover; border-radius: 6px; }

.retbox { margin-top: 12px; padding: 10px; border-radius: 8px; background: var(--bg-2); }

.rvb { flex-shrink: 0; } @media (max-width: 560px) { .item { flex-wrap: wrap; } }

.nf { text-align: center; padding: 60px 0; display: flex; flex-direction: column; align-items: center; gap: 10px; }

</style>


<style scoped>

.track { margin: 18px 0; }
.track-header { align-items: center; margin-bottom: 14px; }
.cancel-alert-box {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 8px;
  padding: 14px 16px;
  margin-top: 8px;
}
.cab-icon { color: var(--danger); flex-shrink: 0; }
.road-track-container {
  margin-top: 36px;
  padding: 0 4px;
}
.road-highway {
  position: relative;
  height: 12px;
  background: #27272a;
  border-radius: 6px;
  margin: 0 28px;
}
.road-highway-dashes {
  position: absolute;
  left: 0;
  right: 0;
  top: 5px;
  height: 2px;
  background: repeating-linear-gradient(90deg, #facc15 0 10px, transparent 10px 20px);
  pointer-events: none;
}
.road-highway-fill {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  background: linear-gradient(90deg, var(--bronze), #f97316);
  border-radius: 6px;
  transition: width 0.9s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.vehicle-runner {
  position: absolute;
  top: -18px;
  transform: translate(-50%, -50%);
  transition: left 0.9s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 5;
  pointer-events: none;
}
.runner-icon-wrap {
  background: #fff;
  border: 1.5px solid var(--accent);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
}
.road-checkpoints {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  pointer-events: none;
  z-index: 3;
}
.checkpoint-node {
  position: relative;
  display: grid;
  place-items: center;
}
.checkpoint-disc {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e4e4e7;
  border: 2px solid #fff;
  display: grid;
  place-items: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.18);
  color: #fff;
}
.checkpoint-core {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a1a1aa;
}
.checkpoint-node.done .checkpoint-disc {
  background: #16a34a;
  color: #fff;
}
.checkpoint-node.current .checkpoint-disc {
  background: #f97316;
  box-shadow: 0 0 0 4px rgba(249, 115, 22, 0.28);
}
.checkpoint-node.current .checkpoint-core {
  background: #fff;
}
.road-details-row {
  display: flex;
  justify-content: space-between;
  margin-top: 22px;
  gap: 12px;
}
.road-step-col {
  flex: 1;
  text-align: center;
  opacity: 0.5;
  transition: opacity 0.3s ease;
}
.road-step-col.done, .road-step-col.current {
  opacity: 1;
}
.rsc-title {
  display: block;
  font-size: 0.92rem;
  color: var(--text);
  margin-bottom: 2px;
}
.road-step-col.current .rsc-title {
  color: var(--accent);
}
.rsc-desc {
  font-size: 0.78rem;
  color: var(--muted);
  margin: 0;
}
.rsc-time {
  font-size: 0.72rem;
  color: var(--muted);
  display: block;
  margin-top: 3px;
}

</style>
