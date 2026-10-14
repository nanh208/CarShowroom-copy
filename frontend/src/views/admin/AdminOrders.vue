<template>
  <div>
    <div class="row between"><h1>Quản lý đơn hàng</h1><input v-model="q" class="input" style="max-width:280px" placeholder="Tìm mã đơn, tên, SĐT…" @keyup.enter="load" /></div>
    <div class="tabs">
      <button :class="{ active: !status }" @click="status = ''; load()">Tất cả</button>
      <button v-for="(v, k) in ORDER_STATUS" :key="k" :class="{ active: status === k }" @click="status = k; load()">{{ v }}</button>
    </div>
    <div v-if="sel.length" class="bulk" role="region" aria-label="Thao tác hàng loạt">
      <b>Đã chọn {{ sel.length }} đơn</b>
      <select v-model="bulkTo" class="input" aria-label="Chuyển trạng thái"><option value="">Chuyển trạng thái…</option><option v-for="(v, k) in LABEL" :key="k" :value="k">{{ v }}</option></select>
      <button class="btn btn-sm" :disabled="!bulkTo || busy" @click="bulk">Áp dụng</button>
      <button class="btn btn-sm btn-outline" @click="labels(sel)">In nhãn vận chuyển</button>
      <button class="btn btn-sm btn-outline" @click="sel = []">Bỏ chọn</button>
    </div>
    <div class="card table-wrap">
      <div v-if="!list.length" class="empty">Không có đơn hàng.</div>
      <table v-else class="table">
        <thead><tr><th class="ck"><input type="checkbox" :checked="sel.length && sel.length === list.length" :indeterminate.prop="sel.length > 0 && sel.length < list.length" aria-label="Chọn tất cả" @change="sel = $event.target.checked ? list.map(o => o.id) : []" /></th><th>Mã đơn</th><th>Khách hàng</th><th>Ngày</th><th>Tổng</th><th>Thanh toán</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="o in list" :key="o.id" :class="{ picked: sel.includes(o.id) }">
          <td class="ck"><input v-model="sel" type="checkbox" :value="o.id" :aria-label="`Chọn đơn ${o.code}`" /></td>
          <td><b>{{ o.code }}</b><div v-if="o.giftWrap" class="small gift">Gói quà</div></td><td>{{ o.buyerName }}<div class="small muted">{{ o.phone }}</div></td><td class="small">{{ dt(o.createdAt) }}</td>
          <td class="price">{{ vnd(o.total) }}</td>
          <td class="small">{{ PAY_METHOD[o.paymentMethod] }}<br /><span :class="['badge', 'badge-' + o.paymentStatus]">{{ PAY_STATUS[o.paymentStatus] }}</span> {{ vnd(o.paidAmount) }}</td>
          <td><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span>
            <div class="row flow"><button v-for="n in NEXT[o.status]" :key="n" :class="['btn btn-sm', n === 'CANCELLED' ? 'btn-danger' : '']" @click="setStatus(o, n)">{{ LABEL[n] }}</button></div></td>
          <td><div class="row flow"><button class="btn btn-sm btn-outline" @click="view = o">Xem</button>
            <router-link :to="`/orders/${o.code}/invoice`" target="_blank" class="btn btn-sm btn-outline">In HĐ</router-link>
            <button class="btn btn-sm btn-outline" @click="labels([o.id])">Nhãn</button>
            <button v-if="o.paymentStatus !== 'PAID' && o.paymentStatus !== 'REFUNDED' && !['CANCELLED','RETURNED'].includes(o.status)" class="btn btn-sm btn-outline" @click="markPaid(o)">Đã thu tiền</button></div></td>
        </tr></tbody>
      </table>
    </div>
    <div v-if="err" class="alert alert-error" style="margin-top:12px">{{ err }}</div>

    <div v-if="view" class="modal-bg" @click.self="view = null">
      <div class="modal">
        <div class="row between"><h3>Đơn {{ view.code }}</h3><span :class="['badge', 'badge-' + view.status]">{{ ORDER_STATUS[view.status] }}</span></div>
        <p><b>{{ view.buyerName }}</b> · {{ view.phone }} · {{ view.email }}</p>
        <p class="muted">{{ view.shippingAddress }}</p><p v-if="view.note" class="small">Ghi chú: {{ view.note }}</p><p v-if="view.giftWrap" class="small"><b>Gói quà</b> (+{{ vnd(view.giftFee) }}){{ view.giftMessage ? ' · Thiệp: “' + view.giftMessage + '”' : '' }}</p>
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
import { useRoute, useRouter } from 'vue-router'
import { toast } from '../../stores/prefs'
import api, { errMsg } from '../../api'
import { vnd, dt, ORDER_STATUS, PAY_METHOD, PAY_STATUS } from '../../format'
const route = useRoute(), router = useRouter()
const list = ref([]), status = ref(route.query.status || ''), view = ref(null), err = ref(''), q = ref(route.query.q || ''), sel = ref([]), bulkTo = ref(''), busy = ref(false)
const labels = ids => { const u = router.resolve({ path: '/admin/labels', query: { ids: ids.join(','), auto: 1 } }).href; window.open(u, '_blank') }
async function bulk() {
  if (bulkTo.value === 'CANCELLED' && !confirm(`Huỷ ${sel.value.length} đơn? Hàng sẽ được hoàn kho.`)) return
  busy.value = true; err.value = ''
  try { const { data } = await api.post('/admin/orders/bulk-status', { ids: sel.value, status: bulkTo.value, note: bulkTo.value === 'CANCELLED' ? 'Shop huỷ hàng loạt' : null })
    toast(`Đã cập nhật ${data.updated}/${sel.value.length} đơn`); if (data.errors.length) err.value = 'Không cập nhật được: ' + data.errors.join(' · ')
    sel.value = []; bulkTo.value = ''; await load() } catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
const NEXT = { PENDING: ['CONFIRMED', 'CANCELLED'], CONFIRMED: ['SHIPPING', 'CANCELLED'], SHIPPING: ['COMPLETED'] }
const LABEL = { CONFIRMED: 'Xác nhận', SHIPPING: 'Giao hàng', COMPLETED: 'Hoàn tất', CANCELLED: 'Huỷ' }
async function load() { list.value = (await api.get('/admin/orders', { params: { status: status.value || undefined, q: q.value || undefined } })).data; sel.value = sel.value.filter(id => list.value.some(o => o.id === id)) }
async function markPaid(o) { try { await api.post(`/admin/orders/${o.id}/mark-paid`) } catch (e) { err.value = errMsg(e) } load() }
async function setStatus(o, s) {
  err.value = ''
  let note
  if (s === 'CANCELLED') { note = prompt('Lý do huỷ đơn (hoàn kho & điểm)?', 'Hết hàng'); if (note === null) return }
  try { await api.patch(`/admin/orders/${o.id}/status`, { status: s, note }) } catch (e) { err.value = errMsg(e) }
  load()
}
onMounted(async () => { await load(); if (route.query.labels && list.value.length) { sel.value = list.value.map(o => o.id) } })
</script>
<style scoped>
.bulk { position: sticky; top: 64px; z-index: 5; display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding: 10px 14px; margin-bottom: 12px; background: #111; color: #fff; }
.bulk .input { width: auto; min-width: 180px; padding: 6px 10px; } .bulk .btn-outline { color: #fff; border-color: #fff; background: transparent; }
.ck { width: 36px; } .ck input { width: 17px; height: 17px; accent-color: var(--accent); } tr.picked { background: var(--bronze-soft); } .gift { color: var(--accent); font-weight: 600; }.flow { gap: 4px; flex-wrap: nowrap; margin-top: 4px; } .modal p { margin-bottom: 6px; }</style>
