<template>
  <div>
    <div class="row between hd"><div><h1>Nhập hàng & gợi ý bổ sung</h1><p class="muted small">Gợi ý dựa trên tốc độ bán {{ days }} ngày gần nhất, đủ hàng cho {{ cover }} ngày tới (tối thiểu 6 sp, tồn ≤ 5 luôn được gợi ý).</p></div>
      <div class="row" style="gap:8px"><label class="small">Đủ hàng cho <select v-model.number="cover" class="input" style="width:auto" @change="loadSug"><option :value="14">14 ngày</option><option :value="21">21 ngày</option><option :value="30">30 ngày</option><option :value="60">60 ngày</option></select></label></div></div>

    <div class="card">
      <div class="row between th"><h3>Gợi ý nhập hàng ({{ sug.length }})</h3>
        <div class="row" style="gap:8px"><input v-model.trim="supplier" class="input" placeholder="Nhà cung cấp" aria-label="Nhà cung cấp" style="width:200px" />
          <button class="btn btn-sm" :disabled="!picked.length" @click="createPo">Tạo đơn nhập ({{ picked.length }}) · {{ vnd(pickedCost) }}</button></div></div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th><input type="checkbox" :checked="picked.length === sug.length && sug.length" aria-label="Chọn tất cả" @change="e => sug.forEach(s => (s.on = e.target.checked))" /></th><th>Sản phẩm</th><th>Tồn</th><th>Bán/ngày</th><th>Còn đủ</th><th>SL nhập</th><th>Giá vốn</th><th>Thành tiền</th></tr></thead>
          <tbody><tr v-for="s in sug" :key="s.productId" :class="{ crit: s.stock <= 2 }">
            <td><input v-model="s.on" type="checkbox" :aria-label="`Chọn ${s.name}`" /></td>
            <td><div class="pn"><img :src="s.image" alt="" /><div><b>{{ s.name }}</b><div class="small muted">{{ s.sku }}</div></div></div></td>
            <td><span :class="['badge', s.stock ? 'badge-PENDING' : 'badge-CANCELLED']">{{ s.stock }}</span></td>
            <td>{{ s.perDay }}</td><td class="small">{{ s.daysLeft == null ? 'chưa bán' : s.daysLeft + ' ngày' }}</td>
            <td><input v-model.number="s.qty" type="number" min="1" class="input qi" :aria-label="`Số lượng nhập ${s.name}`" /></td>
            <td><input v-model.number="s.unitCost" type="number" min="0" step="1000" class="input ci" :aria-label="`Giá vốn ${s.name}`" /></td>
            <td>{{ vnd(s.qty * s.unitCost) }}</td>
          </tr><tr v-if="!sug.length"><td colspan="8" class="muted">Tồn kho đang ổn — chưa cần nhập thêm.</td></tr></tbody>
        </table>
      </div>
    </div>

    <div class="card" style="margin-top:16px">
      <h3>Đơn nhập hàng</h3>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Mã</th><th>Nhà cung cấp</th><th>Mặt hàng</th><th>Tổng vốn</th><th>Trạng thái</th><th>Ngày</th><th /></tr></thead>
          <tbody><tr v-for="po in pos" :key="po.id">
            <td><b>PO#{{ po.id }}</b></td><td>{{ po.supplier }}</td>
            <td class="small">{{ po.items.map(i => `${i.name} ×${i.qty}`).join(', ') }}</td><td>{{ vnd(po.totalCost) }}</td>
            <td><span :class="['badge', ST[po.status].b]">{{ ST[po.status].l }}</span></td><td class="small">{{ dt(po.receivedAt || po.createdAt) }}</td>
            <td><div class="row" style="gap:6px;flex-wrap:nowrap">
              <button v-if="po.status === 'DRAFT'" class="btn btn-sm" @click="setSt(po, 'ORDERED')">Gửi NCC</button>
              <button v-if="po.status === 'ORDERED'" class="btn btn-sm" @click="setSt(po, 'RECEIVED')">Nhận hàng → kho</button>
              <button v-if="['DRAFT', 'ORDERED'].includes(po.status)" class="btn btn-sm btn-outline" @click="setSt(po, 'CANCELLED')">Huỷ</button></div></td>
          </tr><tr v-if="!pos.length"><td colspan="7" class="muted">Chưa có đơn nhập.</td></tr></tbody>
        </table>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, dt } from '../../format'
import { toast } from '../../stores/prefs'
const ST = { DRAFT: { l: 'Nháp', b: 'badge-PENDING' }, ORDERED: { l: 'Đã đặt NCC', b: 'badge-CONFIRMED' }, RECEIVED: { l: 'Đã nhập kho', b: 'badge-COMPLETED' }, CANCELLED: { l: 'Đã huỷ', b: 'badge-CANCELLED' } }
const days = 30, cover = ref(21), sug = ref([]), pos = ref([]), supplier = ref('')
const picked = computed(() => sug.value.filter(s => s.on && s.qty > 0))
const pickedCost = computed(() => picked.value.reduce((a, s) => a + s.qty * s.unitCost, 0))
const loadSug = async () => { sug.value = (await api.get('/admin/restock-suggestions', { params: { days, coverDays: cover.value } })).data.map(s => ({ ...s, qty: s.suggestQty, on: s.stock <= 2 })) }
const loadPo = async () => { pos.value = (await api.get('/admin/purchase-orders')).data.map(p => ({ ...p, items: JSON.parse(p.itemsJson || '[]') })) }
async function createPo() {
  try { await api.post('/admin/purchase-orders', { supplier: supplier.value, items: picked.value.map(s => ({ productId: s.productId, qty: s.qty, unitCost: s.unitCost })) }); toast('Đã tạo đơn nhập (nháp)'); picked.value.forEach(s => (s.on = false)); loadPo() }
  catch (e) { toast(errMsg(e), 'error') }
}
async function setSt(po, s) {
  if (s === 'CANCELLED' && !confirm('Huỷ đơn nhập này?')) return
  try { await api.post(`/admin/purchase-orders/${po.id}/status`, { status: s }); toast(s === 'RECEIVED' ? 'Đã cộng tồn kho' : 'Đã cập nhật'); loadPo(); if (s === 'RECEIVED') loadSug() } catch (e) { toast(errMsg(e), 'error') }
}
onMounted(() => { loadSug(); loadPo() })
</script>
<style scoped>
.hd { gap: 12px; flex-wrap: wrap; align-items: flex-end; margin-bottom: 16px; } .th { gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
.pn { display: flex; gap: 8px; align-items: center; min-width: 220px; } .pn img { width: 44px; height: 34px; object-fit: cover; }
.qi { width: 76px; padding: 6px; } .ci { width: 110px; padding: 6px; } tr.crit td:first-child { box-shadow: inset 3px 0 0 var(--danger); }
@media (max-width: 599px) { .th .input { width: 100% !important; } }
</style>
