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
      <button class="btn btn-sm btn-outline" @click="labels(sel, 'packing')">In phiếu đóng gói</button>
      <button class="btn btn-sm btn-outline" @click="sel = []">Bỏ chọn</button>
    </div>
    <div class="card table-wrap">
      <div v-if="!list.length" class="empty">Không có đơn hàng.</div>
      <table v-else class="table">
        <thead><tr><th class="ck"><input type="checkbox" :checked="sel.length && sel.length === list.length" :indeterminate.prop="sel.length > 0 && sel.length < list.length" aria-label="Chọn tất cả" @change="sel = $event.target.checked ? list.map(o => o.id) : []" /></th><th>Mã đơn</th><th>Khách hàng</th><th>Ngày</th><th>Tổng</th><th>Thanh toán</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="o in pg.items.value" :key="o.id" :class="{ picked: sel.includes(o.id) }">
          <td class="ck"><input v-model="sel" type="checkbox" :value="o.id" :aria-label="`Chọn đơn ${o.code}`" /></td>
          <td><b>{{ o.code }}</b><div v-if="o.giftWrap" class="small gift">Gói quà</div><div class="small"><span v-if="o.guest" class="badge badge-PENDING">Khách vãng lai</span> <span v-if="o.shippingMethod === 'EXPRESS'" class="badge badge-CONFIRMED">Giao nhanh</span></div></td><td>{{ o.buyerName }}<div class="small muted">{{ o.phone }}</div></td><td class="small">{{ dt(o.createdAt) }}</td>
          <td class="price">{{ vnd(o.total) }}</td>
          <td class="small">{{ PAY_METHOD[o.paymentMethod] }}<br /><span :class="['badge', 'badge-' + o.paymentStatus]">{{ PAY_STATUS[o.paymentStatus] }}</span> {{ vnd(o.paidAmount) }}<div v-if="o.refundedAmount" class="small" style="color:var(--danger)">Đã hoàn {{ vnd(o.refundedAmount) }}</div></td>
          <td><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span>
            <div class="row flow"><button v-for="n in NEXT[o.status]" :key="n" :class="['btn btn-sm', n === 'CANCELLED' ? 'btn-danger' : '']" @click="setStatus(o, n)">{{ LABEL[n] }}</button></div></td>
          <td><div class="row flow"><button class="btn btn-sm btn-outline" @click="open(o)">Xem</button>
            <router-link :to="`/orders/${o.code}/invoice`" target="_blank" class="btn btn-sm btn-outline">In HĐ</router-link>
            <button class="btn btn-sm btn-outline" @click="labels([o.id])">Nhãn</button>
            <button class="btn btn-sm btn-outline" @click="labels([o.id], 'packing')">Phiếu ĐG</button>
            <button v-if="o.paymentStatus !== 'PAID' && o.paymentStatus !== 'REFUNDED' && !['CANCELLED','RETURNED'].includes(o.status)" class="btn btn-sm btn-outline" @click="markPaid(o)">Đã thu tiền</button></div></td>
        </tr></tbody>
      </table>
      <p v-if="!list.length" class="muted" style="padding:16px">Không có đơn hàng phù hợp.</p>
    </div>
    <div class="row between" style="margin-top:8px;gap:8px;flex-wrap:wrap"><select v-model="pg.sort.value" class="input" style="width:auto" aria-label="Sắp xếp"><option v-for="(v, k) in pg.sorts" :key="k" :value="k">{{ v.label }}</option></select><span class="small muted">{{ pg.total.value }} đơn</span></div>
    <Pager :p="pg" />
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
        <p class="small">Vận chuyển: <b>{{ view.shippingMethod === 'EXPRESS' ? 'Giao nhanh' : 'Tiêu chuẩn' }}</b><span v-if="view.etaFrom"> · Dự kiến {{ d(view.etaFrom) }} – {{ d(view.etaTo) }}</span><span v-if="view.guest"> · Khách vãng lai</span></p>
        <p v-if="view.bundleDiscount || view.creditUsed" class="small">Combo −{{ vnd(view.bundleDiscount || 0) }} · Ví/thẻ quà −{{ vnd(view.creditUsed || 0) }}</p>
        <div class="rf">
          <div class="row between"><b>Hoàn tiền</b><small class="muted">Đã hoàn {{ vnd(view.refundedAmount || 0) }} / {{ vnd(view.paidAmount) }}</small></div>
          <div v-if="view.paidAmount > (view.refundedAmount || 0)" class="row" style="gap:6px;flex-wrap:wrap;margin-top:6px">
            <input v-model.number="rf.amount" type="number" min="1" :max="view.paidAmount - (view.refundedAmount || 0)" class="input" style="width:130px" aria-label="Số tiền hoàn" />
            <input v-model.trim="rf.reason" class="input" style="flex:1;min-width:140px" placeholder="Lý do (bắt buộc)" aria-label="Lý do hoàn" />
            <label v-if="!view.guest" class="small row" style="gap:4px"><input v-model="rf.toCredit" type="checkbox" /> Vào ví tín dụng</label>
            <button class="btn btn-sm btn-danger" :disabled="!rf.amount || !rf.reason" @click="refund">Hoàn tiền</button></div>
          <p v-else class="small muted">Chưa có khoản thu để hoàn.</p>
        </div>
        <div class="nt"><b>Ghi chú nội bộ</b> <small class="muted">(khách không thấy)</small>
          <ul><li v-for="n in notes" :key="n.id"><small class="muted">{{ n.author }} · {{ dt(n.createdAt) }}</small><div>{{ n.text }}</div></li><li v-if="!notes.length" class="muted small">Chưa có ghi chú.</li></ul>
          <div class="row" style="gap:6px"><input v-model.trim="noteText" class="input" style="flex:1" placeholder="Thêm ghi chú…" aria-label="Ghi chú nội bộ" @keyup.enter="addNote" /><button class="btn btn-sm" :disabled="!noteText" @click="addNote">Thêm</button></div>
        </div>
        <div class="row" style="gap:8px;margin-top:12px"><button class="btn btn-outline" @click="view = null">Đóng</button><button class="btn btn-outline" @click="labels([view.id], 'packing')">In phiếu đóng gói</button></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted, watch } from 'vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import { useRoute, useRouter } from 'vue-router'
import { toast } from '../../stores/prefs'
import api, { errMsg } from '../../api'
import { vnd, dt, d, ORDER_STATUS, PAY_METHOD, PAY_STATUS } from '../../format'
const route = useRoute(), router = useRouter()
const list = ref([]), status = ref(route.query.status || ''), view = ref(null), err = ref(''), q = ref(route.query.q || ''), sel = ref([]), bulkTo = ref(''), busy = ref(false)
// Phân trang + lọc + sắp xếp phía máy chủ
const pg = useServerPaged('/admin/orders', { size: 25, filters: () => ({ 'f.status': status.value }), sorts: { new: { label: 'Mới nhất', sort: 'createdAt,desc' }, old: { label: 'Cũ nhất', sort: 'createdAt' }, big: { label: 'Giá trị cao', sort: 'total,desc' } } })
watch(q, v => { pg.q.value = v })
const labels = (ids, kind = 'labels') => { const u = router.resolve({ path: '/admin/' + kind, query: { ids: ids.join(','), auto: 1 } }).href; window.open(u, '_blank') }
const rf = ref({ amount: 0, reason: '', toCredit: false }), notes = ref([]), noteText = ref('')
async function open(o) { view.value = o; rf.value = { amount: o.paidAmount - (o.refundedAmount || 0), reason: '', toCredit: false }; notes.value = (await api.get(`/admin/orders/${o.id}/notes`)).data }
async function refund() {
  if (!confirm(`Hoàn ${vnd(rf.value.amount)} cho đơn ${view.value.code}?`)) return
  try { const { data } = await api.post(`/admin/orders/${view.value.id}/refund`, rf.value); toast('Đã hoàn tiền'); await load(); open(list.value.find(o => o.id === data.id) || data) } catch (e) { toast(errMsg(e), 'error') }
}
async function addNote() { try { notes.value.push((await api.post(`/admin/orders/${view.value.id}/notes`, { text: noteText.value })).data); noteText.value = '' } catch (e) { toast(errMsg(e), 'error') } }
async function bulk() {
  if (bulkTo.value === 'CANCELLED' && !confirm(`Huỷ ${sel.value.length} đơn? Hàng sẽ được hoàn kho.`)) return
  busy.value = true; err.value = ''
  try { const { data } = await api.post('/admin/orders/bulk-status', { ids: sel.value, status: bulkTo.value, note: bulkTo.value === 'CANCELLED' ? 'Shop huỷ hàng loạt' : null })
    toast(`Đã cập nhật ${data.updated}/${sel.value.length} đơn`); if (data.errors.length) err.value = 'Không cập nhật được: ' + data.errors.join(' · ')
    sel.value = []; bulkTo.value = ''; await load() } catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
const NEXT = { PENDING: ['CONFIRMED', 'CANCELLED'], CONFIRMED: ['SHIPPING', 'CANCELLED'], SHIPPING: ['COMPLETED'] }
const LABEL = { CONFIRMED: 'Xác nhận', SHIPPING: 'Giao hàng', COMPLETED: 'Hoàn tất', CANCELLED: 'Huỷ' }
async function load() { pg.q.value = q.value; await pg.reload(); list.value = pg.items.value; sel.value = sel.value.filter(id => list.value.some(o => o.id === id)) }
watch(pg.items, v => { list.value = v })
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
.ck { width: 36px; } .ck input { width: 17px; height: 17px; accent-color: var(--accent); } tr.picked { background: var(--bronze-soft); } .gift { color: var(--accent); font-weight: 600; }.flow { gap: 4px; flex-wrap: nowrap; margin-top: 4px; } .modal p { margin-bottom: 6px; } .rf, .nt { border-top: 1px solid var(--border); padding-top: 10px; margin-top: 10px; } .nt ul { list-style: none; padding: 0; margin: 6px 0; max-height: 180px; overflow-y: auto; } .nt li { padding: 6px 0; border-bottom: 1px dashed var(--border); font-size: .88rem; }</style>
