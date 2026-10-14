<template>
  <div>
    <h1>Quản lý đơn hàng</h1>
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
          <td><select class="input sel" :value="o.status" :disabled="o.status === 'CANCELLED'" @change="setStatus(o, $event.target.value)">
            <option v-for="(v, k) in ORDER_STATUS" :key="k" :value="k">{{ v }}</option></select></td>
          <td><button class="btn btn-sm btn-outline" @click="view = o">Xem</button></td>
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
          <td>{{ i.productName }}<div class="small muted">{{ i.options }}</div></td><td>× {{ i.quantity }}</td><td>{{ vnd(i.unitPrice * i.quantity) }}</td></tr></tbody></table>
        <p>Tạm tính {{ vnd(view.subtotal) }} · Giảm {{ vnd(view.discount) }} {{ view.promoCode ? '(' + view.promoCode + ')' : '' }} · Ship {{ vnd(view.shippingFee) }}</p>
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
const list = ref([]), status = ref(''), view = ref(null), err = ref('')
async function load() { list.value = (await api.get('/admin/orders', { params: { status: status.value || undefined } })).data }
async function setStatus(o, s) {
  err.value = ''
  if (s === 'CANCELLED' && !confirm('Huỷ đơn và hoàn kho?')) return load()
  try { await api.patch(`/admin/orders/${o.id}/status`, { status: s }) } catch (e) { err.value = errMsg(e) }
  load()
}
onMounted(load)
</script>
<style scoped>.sel { padding: 6px 10px; width: 150px; } .modal p { margin-bottom: 6px; }</style>
