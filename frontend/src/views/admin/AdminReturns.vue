<template>
  <div>
    <h1>Đổi trả & hoàn tiền</h1>
    <div class="card">
      <ListTools :p="pg" placeholder="Tìm mã đơn, khách, lý do…"><select v-model="st" class="input" aria-label="Trạng thái"><option value="">Mọi trạng thái</option><option v-for="(v, k) in RETURN_STATUS" :key="k" :value="k">{{ v }}</option></select></ListTools>
      <div class="table-wrap"><table class="table">
        <thead><tr><th>Ngày</th><th>Đơn</th><th>Khách</th><th>Lý do</th><th>Ảnh</th><th>Hoàn</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="r in pg.items.value" :key="r.id">
          <td class="small">{{ d(r.createdAt) }}</td>
          <td><router-link :to="`/orders/${r.orderCode}`" target="_blank">{{ r.orderCode }}</router-link><div class="small muted">{{ PAY_METHOD[r.order?.paymentMethod] }}</div></td>
          <td>{{ r.customerName }}</td><td class="small" style="max-width:260px">{{ r.reason }}<div v-if="r.adminNote" class="muted">↳ {{ r.adminNote }}</div></td>
          <td><a v-for="i in r.images" :key="i" :href="i" target="_blank"><img loading="lazy" decoding="async" :src="i" class="th" /></a></td>
          <td><b>{{ vnd(r.refundAmount) }}</b></td>
          <td><span :class="['badge', 'badge-' + (r.status === 'REFUNDED' ? 'COMPLETED' : r.status === 'REJECTED' ? 'CANCELLED' : r.status === 'APPROVED' ? 'SHIPPING' : 'PENDING')]">{{ RETURN_STATUS[r.status] }}</span></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:6px">
            <template v-if="r.status === 'PENDING'"><button class="btn btn-sm" @click="act(r, 'APPROVED')">Duyệt</button><button class="btn btn-sm btn-danger" @click="act(r, 'REJECTED', true)">Từ chối</button></template>
            <button v-if="r.status === 'APPROVED'" class="btn btn-sm" @click="act(r, 'REFUNDED')">Đã nhận hàng · Hoàn tiền</button>
          </div></td>
        </tr>
        <tr v-if="!pg.items.value.length"><td colspan="8" class="muted">Không có yêu cầu nào.</td></tr></tbody>
      </table></div>
      <Pager :p="pg" />
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import ListTools from '../../components/ListTools.vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import api, { errMsg } from '../../api'
import { vnd, d, RETURN_STATUS, PAY_METHOD } from '../../format'
import { toast } from '../../stores/prefs'
const list = ref([]), st = ref('')
const pg = useServerPaged('/admin/returns', { filters: () => ({ 'f.status': st.value }), sorts: { new: { label: 'Mới nhất', sort: 'createdAt,desc' }, amt: { label: 'Số tiền hoàn lớn', sort: 'refundAmount,desc' } } })
const load = async () => { await pg.reload(); list.value = (await api.get('/admin/returns')).data }
const act = async (r, status, ask) => {
  const note = ask ? prompt('Lý do từ chối?') : null
  if (ask && note === null) return
  try { await api.patch(`/admin/returns/${r.id}`, { status, note }); toast('Đã cập nhật yêu cầu'); load() } catch (e) { toast(errMsg(e), 'error') }
}
onMounted(load)
</script>
<style scoped>.th { width: 40px; height: 40px; object-fit: cover; border-radius: 6px; margin-right: 4px; }</style>
