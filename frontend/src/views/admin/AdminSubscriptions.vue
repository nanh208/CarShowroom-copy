<template>
  <div>
    <h1>Mua định kỳ</h1>
    <p class="muted small">Khách đăng ký nhận hàng theo chu kỳ. Tác vụ nền chạy mỗi phút: đến hạn → tạo đơn (giảm {{ pct }}%); lỗi 3 lần liên tiếp (hết hàng, ngừng bán…) → tự tạm dừng và báo khách.</p>
    <div class="row" style="gap:8px;margin:10px 0;flex-wrap:wrap">
      <label class="small row" style="gap:6px">Giảm mỗi kỳ <input v-model.number="pct" type="number" min="0" max="30" class="input" style="width:80px" /> %</label>
      <button class="btn btn-sm btn-outline" @click="savePct">Lưu</button>
      <button class="btn btn-sm" :disabled="busy" @click="runDue">Chạy các kỳ đến hạn ngay</button>
    </div>
    <div class="card">
      <ListTools :p="pg" placeholder="Tìm sản phẩm, khách, mã đơn…"><select v-model="st" class="input" aria-label="Trạng thái"><option value="">Tất cả</option><option value="ACTIVE">Đang hoạt động</option><option value="PAUSED">Tạm dừng</option><option value="CANCELLED">Đã huỷ</option></select></ListTools>
      <div v-if="!pg.items.value.length && !pg.loading.value" class="empty">Chưa có đăng ký mua định kỳ.</div>
      <div v-else class="table-wrap"><table class="table"><thead><tr><th>#</th><th>Khách</th><th>Sản phẩm</th><th>Chu kỳ</th><th>Kỳ tới</th><th>Đã chạy</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="s in pg.items.value" :key="s.id">
          <td>{{ s.id }}</td><td><b>{{ s.user?.fullName }}</b><div class="small muted">{{ s.phone }} · {{ s.city }}</div></td>
          <td><router-link :to="`/products/${s.productId}`">{{ s.productName }}</router-link><div class="small muted">{{ s.quantity }} × {{ vnd(s.unitPrice) }} · {{ s.paymentMethod }}</div></td>
          <td>{{ s.intervalDays }} ngày</td><td class="small">{{ s.status === 'ACTIVE' ? dt(s.nextRunAt) : '—' }}</td>
          <td>{{ s.runs }}<div v-if="s.lastOrderCode" class="small"><router-link :to="`/admin/orders?q=${s.lastOrderCode}`">{{ s.lastOrderCode }}</router-link></div><div v-if="s.lastError" class="small err" :title="s.lastError">⚠ {{ s.failCount }} lỗi</div></td>
          <td><span :class="['badge', s.status === 'ACTIVE' ? 'badge-COMPLETED' : s.status === 'PAUSED' ? 'badge-PENDING' : 'badge-CANCELLED']">{{ ST[s.status] }}</span></td>
          <td><button v-if="s.status === 'ACTIVE'" class="btn btn-sm btn-outline" :disabled="busy" @click="runOne(s)">Tạo đơn ngay</button></td>
        </tr></tbody></table></div>
      <Pager :p="pg" />
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import ListTools from '../../components/ListTools.vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import api, { errMsg } from '../../api'
import { vnd, dt } from '../../format'
import { toast } from '../../stores/prefs'
const ST = { ACTIVE: 'Đang hoạt động', PAUSED: 'Tạm dừng', CANCELLED: 'Đã huỷ' }
const st = ref(''), busy = ref(false), pct = ref(5)
const pg = useServerPaged('/admin/subscriptions', { size: 20, filters: () => ({ 'f.status': st.value }), sorts: { next: { label: 'Kỳ tới sớm nhất', sort: 'nextRunAt' }, new: { label: 'Mới nhất', sort: 'id,desc' } } })
pg.reload(); api.get('/public/subscriptions/options').then(r => { pct.value = r.data.discountPct })
async function runDue() { busy.value = true; try { const { data } = await api.post('/admin/subscriptions/run-due'); toast(`Đã tạo ${data.created} đơn, ${data.failed} lỗi`); pg.reload() } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
async function runOne(s) { busy.value = true; try { const { data } = await api.post(`/admin/subscriptions/${s.id}/run`); toast(data.ok ? `Đã tạo đơn ${data.orderCode}` : data.error, data.ok ? 'ok' : 'error'); pg.reload() } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
async function savePct() { try { await api.put('/admin/subscriptions/settings', { discountPct: pct.value }); toast('Đã lưu mức giảm') } catch (e) { toast(errMsg(e), 'error') } }
</script>
<style scoped>.err { color: var(--danger); }</style>
