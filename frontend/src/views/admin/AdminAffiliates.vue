<template>
  <div>
    <h1>Tiếp thị liên kết</h1>
    <p class="muted small">Khách tham gia trong Tài khoản › Tiếp thị liên kết. Hoa hồng ghi nhận khi đặt đơn qua liên kết (30 ngày), tự duyệt khi đơn giao thành công, tự huỷ khi đơn huỷ/trả. Không tính đơn tự giới thiệu.</p>
    <div class="row" style="gap:8px;margin:10px 0;flex-wrap:wrap">
      <label class="small row" style="gap:6px">Hoa hồng mặc định <input v-model.number="defPct" type="number" min="0" max="30" step="0.5" class="input" style="width:90px" /> %</label>
      <button class="btn btn-sm btn-outline" @click="saveDef">Lưu</button>
    </div>
    <div class="card table-wrap">
      <table class="table"><thead><tr><th>Đối tác</th><th>Mã</th><th>% HH</th><th>Nhấp</th><th>Đơn</th><th>Chờ giao</th><th>Đã duyệt</th><th>Đã trả</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="a in list" :key="a.affiliate.id">
          <td><b>{{ a.userName }}</b><div class="small muted">{{ a.email }}</div><div v-if="a.affiliate.payoutInfo" class="small" :title="a.affiliate.payoutInfo">💳 {{ a.affiliate.payoutInfo.slice(0, 40) }}</div></td>
          <td><code>{{ a.affiliate.code }}</code></td>
          <td><input :value="a.affiliate.commissionPct" type="number" min="0" max="30" step="0.5" class="input" style="width:70px;padding:4px 6px" :aria-label="`Hoa hồng ${a.affiliate.code}`" @change="patch(a, { commissionPct: +$event.target.value })" /></td>
          <td>{{ a.affiliate.clicks }}</td><td>{{ a.orders }}<div class="small muted">{{ a.conversion }}%</div></td>
          <td class="money">{{ vnd(a.totals.PENDING) }}</td><td class="money">{{ vnd(a.totals.APPROVED) }}</td><td class="money">{{ vnd(a.totals.PAID) }}</td>
          <td><button :class="['btn btn-sm', a.affiliate.status === 'ACTIVE' ? 'btn-outline' : 'btn-danger']" @click="patch(a, { status: a.affiliate.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE' })">{{ a.affiliate.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm khoá' }}</button></td>
          <td><button class="btn btn-sm" :disabled="!a.totals.APPROVED" @click="payout(a)">Chi trả</button></td>
        </tr></tbody></table>
      <p v-if="!list.length" class="empty">Chưa có đối tác nào.</p>
    </div>
    <h3 style="margin-top:20px">Hoa hồng gần đây</h3>
    <div class="card">
      <ListTools :p="pg" placeholder="Tìm mã đơn, mã đối tác…"><select v-model="st" class="input" aria-label="Trạng thái"><option value="">Tất cả</option><option v-for="(v, k) in CS" :key="k" :value="k">{{ v }}</option></select></ListTools>
      <div class="table-wrap"><table class="table"><thead><tr><th>Đơn</th><th>Đối tác</th><th>Giá trị tính</th><th>%</th><th>Hoa hồng</th><th>Trạng thái</th><th>Ngày</th></tr></thead>
        <tbody><tr v-for="c in pg.items.value" :key="c.id"><td>{{ c.orderCode }}</td><td><code>{{ c.affiliateCode }}</code></td><td class="money">{{ vnd(c.base) }}</td><td>{{ c.pct }}%</td><td class="money">{{ vnd(c.amount) }}</td>
          <td><span :class="['badge', { PENDING: 'badge-PENDING', APPROVED: 'badge-CONFIRMED', PAID: 'badge-COMPLETED', VOID: 'badge-CANCELLED' }[c.status]]">{{ CS[c.status] }}</span></td><td class="small">{{ d(c.createdAt) }}</td></tr></tbody></table></div>
      <Pager :p="pg" />
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import ListTools from '../../components/ListTools.vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import api, { errMsg } from '../../api'
import { vnd, d } from '../../format'
import { toast } from '../../stores/prefs'
const CS = { PENDING: 'Chờ giao', APPROVED: 'Đã duyệt', PAID: 'Đã chi trả', VOID: 'Đã huỷ' }
const list = ref([]), st = ref(''), defPct = ref(5)
const pg = useServerPaged('/admin/affiliate-commissions', { size: 15, filters: () => ({ 'f.status': st.value }), sorts: { new: { label: 'Mới nhất', sort: 'id,desc' }, amt: { label: 'Hoa hồng cao', sort: 'amount,desc' } } })
async function load() { list.value = (await api.get('/admin/affiliates')).data; pg.reload() }
async function patch(a, body) { try { await api.patch(`/admin/affiliates/${a.affiliate.id}`, body); load() } catch (e) { toast(errMsg(e), 'error'); load() } }
async function payout(a) { if (!confirm(`Đánh dấu đã chi trả ${vnd(a.totals.APPROVED)} cho ${a.userName}?`)) return; try { const { data } = await api.post(`/admin/affiliates/${a.affiliate.id}/payout`); toast(`Đã chi trả ${vnd(data.paid)} (${data.count} đơn)`); load() } catch (e) { toast(errMsg(e), 'error') } }
async function saveDef() { try { await api.put('/admin/affiliates/settings', { defaultPct: defPct.value }); toast('Đã lưu — áp dụng cho đối tác mới') } catch (e) { toast(errMsg(e), 'error') } }
onMounted(async () => { load(); try { defPct.value = (await api.get('/affiliate/me')).data.defaultPct ?? 5 } catch { /* bỏ qua */ } })
</script>
