<template>
  <div class="container page">
    <div v-if="!o" class="spinner" />
    <template v-else>
      <Breadcrumb :items="[{ label: 'Tài khoản', to: '/account?tab=orders' }, { label: 'Đơn ' + o.code }]" />
      <div v-if="$route.query.new" class="alert alert-success">Đặt hàng thành công! Mã đơn của bạn là <b>{{ o.code }}</b>.</div>
      <div v-if="o.giftWrap" class="alert gift-note"><b>Gói quà</b> (+{{ vnd(o.giftFee) }})<span v-if="o.giftMessage"> · Thiệp: “{{ o.giftMessage }}”</span></div>
      <div class="row between"><h1>Đơn hàng {{ o.code }}</h1>
        <div class="row acts"><button class="btn btn-sm" :disabled="busy" @click="reorder">↻ Mua lại</button><button class="btn btn-sm btn-outline" :disabled="mailed" @click="mailInvoice">{{ mailed ? '✓ Đã gửi email' : '✉ Gửi hoá đơn' }}</button><router-link :to="`/orders/${o.code}/invoice`" class="btn btn-sm btn-outline">🧾 Hoá đơn</router-link><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span></div></div>
      <p class="muted">Đặt lúc {{ dt(o.createdAt) }}<template v-if="o.etaFrom && !['COMPLETED', 'CANCELLED'].includes(o.status)"> · 📦 Dự kiến giao <b>{{ dshort(o.etaFrom) }}{{ o.etaTo !== o.etaFrom ? ' – ' + dshort(o.etaTo) : '' }}</b> ({{ o.shippingMethod === 'EXPRESS' ? 'hoả tốc' : 'tiêu chuẩn' }})</template></p>

      <div v-reveal class="card track">
        <h3>Theo dõi đơn hàng</h3>
        <div class="road">
          <div class="fill" :style="{ width: progress + '%' }" />
          <span class="truck" :style="{ left: progress + '%' }">🚚</span>
        </div>
        <ol class="tl">
          <li v-for="st in steps" :key="st.key" :class="st.state">
            <span class="dot" /><div><b>{{ st.label }}</b><p>{{ st.desc }}</p><small v-if="st.time">{{ dt(st.time) }}</small></div>
          </li>
        </ol>
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
              <div class="small muted">{{ i.variantName }}<span v-if="i.flashSale"> · ⚡ Flash sale</span><span v-if="i.warrantyMonths"> · BH {{ i.warrantyMonths }} tháng</span></div></div>
            <b>{{ vnd(i.unitPrice * i.quantity) }}</b>
          </div>
          <div class="row between"><span>Tạm tính</span><span>{{ vnd(o.subtotal) }}</span></div>
          <div v-if="o.bundleDiscount" class="row between"><span>Combo ưu đãi</span><span>−{{ vnd(o.bundleDiscount) }}</span></div>
          <div v-if="o.discount - (o.bundleDiscount || 0) > 0" class="row between"><span>Giảm giá<template v-if="o.promoCode"> ({{ o.promoCode }})</template></span><span>−{{ vnd(o.discount - (o.bundleDiscount || 0)) }}</span></div>
          <div v-if="o.pointsDiscount" class="row between"><span>Dùng {{ o.pointsUsed }} điểm</span><span>−{{ vnd(o.pointsDiscount) }}</span></div>
          <div class="row between"><span>Phí vận chuyển<small v-if="o.shippingMethod === 'EXPRESS'" class="muted"> (hoả tốc)</small></span><span>{{ o.shippingFee ? vnd(o.shippingFee) : 'Miễn phí' }}</span></div>
          <div v-if="o.giftFee" class="row between"><span>Gói quà</span><span>{{ vnd(o.giftFee) }}</span></div>
          <div v-if="o.creditUsed" class="row between"><span>Thẻ quà / ví tín dụng</span><span>−{{ vnd(o.creditUsed) }}</span></div>
          <div class="row between total"><span>Tổng cộng</span><span class="price">{{ vnd(o.total) }}</span></div>
          <p v-if="o.pointsEarned" class="small ok">+{{ o.pointsEarned }} điểm thưởng đã được cộng</p>
        </div>
        <div>
          <div class="card" style="margin-bottom:18px">
            <h3>Giao nhận</h3>
            <p><b>{{ o.buyerName }}</b> · {{ o.phone }}</p><p class="muted">{{ o.email }}</p><p>{{ o.shippingAddress }}</p>
            <p v-if="o.note" class="muted small">Ghi chú: {{ o.note }}</p>
          </div>
          <div class="card">
            <h3>Thanh toán</h3>
            <p>{{ PAY_METHOD[o.paymentMethod] }} <span :class="['badge', 'badge-' + o.paymentStatus]">{{ PAY_STATUS[o.paymentStatus] }}</span></p>
            <div class="row between"><span>Đã thanh toán</span><b class="ok">{{ vnd(o.paidAmount) }}</b></div>
            <p v-if="o.transactionRef" class="small muted">Mã giao dịch: {{ o.transactionRef }}</p>
            <div v-if="o.refundedAmount" class="row between"><span>Đã hoàn tiền</span><b>{{ vnd(o.refundedAmount) }}</b></div>
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
const progress = computed(() => { const i = KEYS.indexOf(o.value.status); return i < 0 ? 0 : i / 3 * 100 })
const needPay = computed(() => o.value.status !== 'CANCELLED' && o.value.paymentMethod !== 'COD' && o.value.paidAmount < o.value.total)
const canReturn = computed(() => o.value.status === 'COMPLETED' && !returns.value.some(r => ['PENDING', 'APPROVED'].includes(r.status)))
const load = async () => { const { data } = await api.get(`/orders/${route.params.code}`); o.value = data.order; bank.value = data.bank; returns.value = data.returns }
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
</style>
