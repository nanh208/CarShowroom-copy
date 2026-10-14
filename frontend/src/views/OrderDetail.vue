<template>
  <div class="container page">
    <div v-if="!o" class="spinner" />
    <template v-else>
      <div v-if="$route.query.new" class="alert alert-success">🎉 Đặt hàng thành công! Mã đơn của bạn là <b>{{ o.code }}</b>.</div>
      <div class="row between"><h1>Đơn hàng {{ o.code }}</h1><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span></div>
      <p class="muted">Đặt lúc {{ dt(o.createdAt) }}</p>
      <div class="steps">
        <div v-for="(s, i) in flow" :key="s" :class="['step', { done: i <= flow.indexOf(o.status) }]">{{ ORDER_STATUS[s] }}</div>
      </div>
      <div class="grid grid-2">
        <div class="card">
          <h3>Sản phẩm</h3>
          <div v-for="i in o.items" :key="i.id" class="row between item">
            <div class="grow"><router-link :to="`/products/${i.productId}`">{{ i.productName }}</router-link> × {{ i.quantity }}
              <div class="small muted">{{ i.scale }}<span v-if="i.options"> · {{ i.options }}</span></div></div>
            <b>{{ vnd(i.unitPrice * i.quantity) }}</b>
          </div>
          <div class="row between"><span>Tạm tính</span><span>{{ vnd(o.subtotal) }}</span></div>
          <div v-if="o.discount" class="row between"><span>Giảm giá ({{ o.promoCode }})</span><span>−{{ vnd(o.discount) }}</span></div>
          <div class="row between"><span>Vận chuyển</span><span>{{ o.shippingFee ? vnd(o.shippingFee) : 'Miễn phí' }}</span></div>
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api, { errMsg } from '../api'
import { vnd, dt, ORDER_STATUS, PAY_METHOD, PAY_STATUS } from '../format'
const route = useRoute(), o = ref(null), err = ref('')
const flow = ['PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED']
async function act(a) { err.value = ''; try { o.value = (await api.post(`/orders/${o.value.code}/${a}`)).data } catch (e) { err.value = errMsg(e) } }
const cancel = () => confirm('Bạn chắc chắn muốn huỷ đơn hàng này?') && act('cancel')
onMounted(async () => { o.value = (await api.get(`/orders/${route.params.code}`)).data })
</script>
<style scoped>
.steps { display: flex; gap: 6px; margin: 18px 0 24px; }
.step { flex: 1; text-align: center; padding: 10px; border-radius: 2px; background: #eee; color: var(--muted); font-size: .85rem; font-weight: 600; }
.step.done { background: var(--bronze); color: #fff; }
.item { padding: 10px 0; border-bottom: 1px solid var(--border); }
.card .row.between { margin-bottom: 6px; } .card p { margin-bottom: 6px; }
.total { font-size: 1.1rem; font-weight: 700; } .ok { color: #1e7a3a; }
</style>
