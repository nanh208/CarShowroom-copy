<template>
  <div class="container page">
    <div v-if="!o" class="spinner" />
    <template v-else>
      <Breadcrumb :items="[{ label: 'Tài khoản', to: '/account' }, { label: 'Đơn ' + o.code }]" />
      <div v-if="$route.query.new" class="alert alert-success">Đặt hàng thành công! Mã đơn của bạn là <b>{{ o.code }}</b>.</div>
      <div class="row between"><h1>Đơn hàng {{ o.code }}</h1><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span></div>
      <p class="muted">Đặt lúc {{ dt(o.createdAt) }}</p>
      <div class="card track">
        <h3>Theo dõi đơn hàng</h3>
        <ol class="tl">
          <li v-for="st in steps" :key="st.key" :class="st.state">
            <span class="dot" /><div><b>{{ st.label }}</b><p>{{ st.desc }}</p><small v-if="st.time">{{ dt(st.time) }}</small></div>
          </li>
        </ol>
      </div>
      <div class="grid grid-2">
        <div class="card">
          <h3>Sản phẩm</h3>
          <div v-for="i in o.items" :key="i.id" class="row between item">
            <div class="grow"><router-link :to="`/products/${i.productId}`">{{ i.productName }}</router-link> × {{ i.quantity }}
              <div class="small muted">{{ i.fuel }}<span v-if="i.options"> · {{ i.options }}</span></div></div>
            <b>{{ vnd(i.unitPrice * i.quantity) }}</b>
          </div>
          <div class="row between"><span>Tạm tính</span><span>{{ vnd(o.subtotal) }}</span></div>
          <div v-if="o.discount" class="row between"><span>Giảm giá ({{ o.promoCode }})</span><span>−{{ vnd(o.discount) }}</span></div>
          <div class="row between"><span>Giao xe</span><span>{{ o.shippingFee ? vnd(o.shippingFee) : 'Miễn phí' }}</span></div>
          <div class="row between total"><span>Tổng cộng</span><span class="price">{{ vnd(o.total) }}</span></div>
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
            <p v-if="o.paymentMethod === 'INSTALLMENT'" class="small muted">{{ vnd(o.monthlyInstallment) }}/tháng × {{ o.installmentMonths }} tháng</p>
            <div class="row between"><span>Đã thanh toán</span><b class="ok">{{ vnd(o.paidAmount) }}</b></div>
            <div class="row between"><span>Còn lại</span><b>{{ vnd(o.total - o.paidAmount) }}</b></div>
            <p v-if="o.transactionRef" class="small muted">Mã giao dịch: {{ o.transactionRef }}</p>
            <div class="row" style="margin-top:12px">
              <button v-if="o.paidAmount < o.total && o.status !== 'CANCELLED'" class="btn btn-sm" @click="act('pay')">
                {{ o.paymentMethod === 'INSTALLMENT' ? 'Thanh toán kỳ tiếp theo' : 'Thanh toán phần còn lại' }}</button>
              <button v-if="['PENDING', 'CONFIRMED'].includes(o.status)" class="btn btn-sm btn-danger" @click="cancel">Huỷ đơn</button>
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
import Breadcrumb from '../components/Breadcrumb.vue'
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api, { errMsg } from '../api'
import { vnd, dt, ORDER_STATUS, PAY_METHOD, PAY_STATUS } from '../format'
const route = useRoute(), o = ref(null), err = ref('')
const flow = ['PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED']
async function act(a) { err.value = ''; try { o.value = (await api.post(`/orders/${o.value.code}/${a}`)).data } catch (e) { err.value = errMsg(e) } }
const cancel = () => confirm('Bạn chắc chắn muốn huỷ đơn hàng này?') && act('cancel')
onMounted(async () => { o.value = (await api.get(`/orders/${route.params.code}`)).data })
const STEP_DESC = { PENDING: 'Đơn hàng đã được tạo, chờ ARES xác nhận.', CONFIRMED: 'Đã xác nhận, chuẩn bị xe & giấy tờ.', SHIPPING: 'Xe đang được vận chuyển tới bạn.', COMPLETED: 'Đã bàn giao xe. Cảm ơn bạn!', CANCELLED: 'Đơn hàng đã bị huỷ.' }
const steps = computed(() => {
  if (!o.value) return []
  const tl = o.value.timeline || {}
  const keys = o.value.status === 'CANCELLED' ? [...['PENDING', 'CONFIRMED', 'SHIPPING'].filter(k => tl[k] || k === 'PENDING'), 'CANCELLED'] : ['PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED']
  const cur = keys.indexOf(o.value.status)
  return keys.map((k, i) => ({ key: k, label: ORDER_STATUS[k], desc: STEP_DESC[k], time: tl[k] || (k === 'PENDING' ? o.value.createdAt : null),
    state: k === 'CANCELLED' ? 'cancel' : i < cur ? 'done' : i === cur ? 'now' : '' }))
})
</script>
<style scoped>
.steps { display: flex; gap: 6px; margin: 18px 0 24px; }
.step { flex: 1; text-align: center; padding: 10px; border-radius: 8px; background: rgba(0,0,0,.05); color: var(--muted); font-size: .85rem; font-weight: 600; }
.step.done { background: var(--primary); color: var(--text); }
.item { padding: 10px 0; border-bottom: 1px solid var(--border); }
.card .row.between { margin-bottom: 6px; } .card p { margin-bottom: 6px; }
.total { font-size: 1.1rem; font-weight: 700; } .ok { color: var(--success); }
.track { margin: 24px 0; }
.tl { list-style: none; display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-top: 16px; }
.tl li { position: relative; padding-top: 32px; color: var(--muted); }
.tl li::before { content: ''; position: absolute; top: 9px; left: 20px; right: -16px; height: 2px; background: rgba(0,0,0,.1); }
.tl li:last-child::before { display: none; }
.tl .dot { position: absolute; top: 0; left: 0; width: 20px; height: 20px; border-radius: 50%; border: 2px solid rgba(0,0,0,.2); background: var(--bg-2); z-index: 1; }
.tl li.done .dot, .tl li.now .dot { background: var(--primary); border-color: var(--primary); }
.tl li.done::before { background: var(--primary); }
.tl li.now .dot { box-shadow: 0 0 0 6px rgba(168,122,60,.25); }
.tl li.cancel .dot { background: var(--danger); border-color: var(--danger); }
.tl b { display: block; color: var(--text); margin-bottom: 4px; } .tl li:not(.done):not(.now):not(.cancel) b { color: var(--muted); }
.tl p { font-size: .85rem; } .tl small { font-size: .78rem; color: var(--accent); }
@media (max-width: 800px) { .tl { grid-template-columns: 1fr 1fr; } }
</style>
