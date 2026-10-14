<template>
  <div>
    <div class="row between"><h1>Quản lý đơn hàng</h1><input v-model="q" class="input" style="max-width:280px" placeholder="Tìm mã đơn, tên, SĐT…" @keyup.enter="load" /></div>
    <div class="tabs">
      <button :class="{ active: !status }" @click="status = ''; load()">Tất cả</button>
      <button v-for="(v, k) in ORDER_STATUS" :key="k" :class="{ active: status === k }" @click="status = k; load()">{{ v }}</button>
    </div>
    <div class="card table-wrap">
      <div v-if="!list.length" class="empty">Không có đơn hàng.</div>
      <table v-else class="table">
        <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Ngày</th><th>Tổng</th><th>Thanh toán</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="o in list" :key="o.id">
          <td><b>{{ o.code }}</b></td><td>{{ o.buyerName }}<div class="small muted">{{ o.phone }}</div></td><td class="small">{{ dt(o.createdAt) }}</td>
          <td class="price">{{ vnd(o.total) }}</td>
          <td class="small">{{ PAY_METHOD[o.paymentMethod] }}<br /><span :class="['badge', 'badge-' + o.paymentStatus]">{{ PAY_STATUS[o.paymentStatus] }}</span> {{ vnd(o.paidAmount) }}</td>
          <td><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span>
            <div class="row flow"><button v-for="n in NEXT[o.status]" :key="n" :class="['btn btn-sm', n === 'CANCELLED' ? 'btn-danger' : '']" @click="setStatus(o, n)">{{ LABEL[n] }}</button></div></td>
          <td><div class="row flow"><button class="btn btn-sm btn-outline" @click="view = o">Xem</button>
            <router-link :to="`/orders/${o.code}/invoice`" target="_blank" class="btn btn-sm btn-outline">In HĐ</router-link>
            <button v-if="o.paymentStatus !== 'PAID' && o.paymentStatus !== 'REFUNDED' && !['CANCELLED','RETURNED'].includes(o.status)" class="btn btn-sm btn-outline" @click="markPaid(o)">Đã thu tiền</button></div></td>
        </tr></tbody>
      </table>
    </div>
    <div v-if="err" class="alert alert-error" style="margin-top:12px">{{ err }}</div>

    <div v-if="view" class="modal-bg" @click.self="view = null">
      <div class="modal">
        <div class="row between"><h3>Đơn {{ view.code }}</h3><span :class="['badge', 'badge-' + view.status]">{{ ORDER_STATUS[view.status] }}</span></div>
        <p><b>{{ view.buyerName }}</b> · {{ view.phone }} · {{ view.email }}</p>
        <p class="muted">{{ view.shippingAddress }}</p><p v-if="view.note" class="small">Ghi chú: {{ view.note }}</p>
        <table class="table" style="margin:12px 0"><tbody><tr v-for="i in view.items" :key="i.id">
          <td>{{ i.productName }}<div class="small muted">{{ i.variantName }} {{ i.sku ? '· ' + i.sku : '' }}{{ i.flashSale ? ' · ⚡ flash' : '' }}</div></td><td>× {{ i.quantity }}</td><td>{{ vnd(i.unitPrice * i.quantity) }}</td></tr></tbody></table>
        <p>Tạm tính {{ vnd(view.subtotal) }} · Giảm {{ vnd(view.discount) }} {{ view.promoCode ? '(' + view.promoCode + ')' : '' }} · Ship {{ vnd(view.shippingFee) }}<span v-if="view.pointsUsed"> · Dùng {{ view.pointsUsed }} điểm (−{{ vnd(view.pointsDiscount) }})</span></p>
        <p v-if="view.cancelReason" class="small" style="color:var(--danger)">Lý do huỷ: {{ view.cancelReason }}</p>
        <p><b>Tổng: {{ vnd(view.total) }}</b> — Đã thu {{ vnd(view.paidAmount) }} ({{ PAY_METHOD[view.paymentMethod] }}<span v-if="view.installmentMonths">, {{ view.installmentMonths }} tháng</span>)</p>
        <button class="btn btn-outline" style="margin-top:12px" @click="view = null">Đóng</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, dt, ORDER_STATUS, PAY_METHOD, PAY_STATUS } from '../../format'
const list = ref([]), status = ref(''), view = ref(null), err = ref(''), q = ref('')
const NEXT = { PENDING: ['CONFIRMED', 'CANCELLED'], CONFIRMED: ['SHIPPING', 'CANCELLED'], SHIPPING: ['COMPLETED'] }
const LABEL = { CONFIRMED: 'Xác nhận', SHIPPING: 'Giao hàng', COMPLETED: 'Hoàn tất', CANCELLED: 'Huỷ' }
async function load() { list.value = (await api.get('/admin/orders', { params: { status: status.value || undefined, q: q.value || undefined } })).data }
async function markPaid(o) { try { await api.post(`/admin/orders/${o.id}/mark-paid`) } catch (e) { err.value = errMsg(e) } load() }
async function setStatus(o, s) {
  err.value = ''
  let note
  if (s === 'CANCELLED') { note = prompt('Lý do huỷ đơn (hoàn kho & điểm)?', 'Hết hàng'); if (note === null) return }
  try { await api.patch(`/admin/orders/${o.id}/status`, { status: s, note }) } catch (e) { err.value = errMsg(e) }
  load()
}
onMounted(load)
</script>
<style scoped>.flow { gap: 4px; flex-wrap: nowrap; margin-top: 4px; } .modal p { margin-bottom: 6px; }</style>
