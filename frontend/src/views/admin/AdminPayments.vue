<template>
  <div>
    <h1>Giao dịch cổng thanh toán</h1>
    <p class="muted small">VNPay 2.1.0 (HMAC-SHA512) / MoMo v2 (HMAC-SHA256). Tiền chỉ được ghi nhận qua IPN có chữ ký hoặc truy vấn đối soát; giao dịch chờ quá 15 phút tự hết hạn.</p>
    <div class="gw-grid" v-if="cfg">
      <div v-for="g in [['VNPay', cfg.vnpay], ['MoMo', cfg.momo]]" :key="g[0]" class="card gw">
        <div class="gw-h"><strong>{{ g[0] }}</strong><span :class="['badge', g[1].mode === 'REAL' ? 'badge-COMPLETED' : g[1].mode === 'MOCK' ? 'badge-PENDING' : 'badge-CANCELLED']">{{ g[1].mode }}</span></div>
        <dl class="small">
          <template v-if="g[0] === 'VNPay'"><dt>TmnCode</dt><dd>{{ g[1].tmnCodeSet ? g[1].tmnCode : 'Chưa đặt' }}</dd><dt>Hash secret</dt><dd>{{ g[1].hashSecretSet ? 'Đã đặt' : 'Chưa đặt' }}</dd><dt>Endpoint</dt><dd class="mono">{{ g[1].payUrl }}</dd></template>
          <template v-else><dt>Partner</dt><dd>{{ g[1].partnerCode || 'Chưa đặt' }}<span v-if="g[1].publicTestKeys"> · khoá TEST công khai</span></dd><dt>Access/Secret</dt><dd>{{ g[1].accessKeySet && g[1].secretKeySet ? 'Đã đặt' : 'Chưa đặt' }}</dd><dt>Endpoint</dt><dd class="mono">{{ g[1].endpoint }}</dd></template>
          <dt>Môi trường</dt><dd>{{ g[1].sandboxEndpoint ? 'Sandbox / test' : 'Production' }}</dd>
          <dt>IPN URL</dt><dd class="mono">{{ g[1].ipnUrl }}</dd><dt>Return URL</dt><dd class="mono">{{ g[1].returnUrl }}</dd>
        </dl>
      </div>
    </div>
    <p v-if="cfg && !cfg.publicBaseReachable" class="warn small">PUBLIC_BASE_URL = {{ cfg.publicBaseUrl }} chưa phải địa chỉ HTTPS công khai → cổng không gọi được IPN; hệ thống sẽ đối soát bằng API truy vấn khi khách quay về.</p>
    <div class="card" style="margin-top:14px">
      <ListTools :p="pg" placeholder="Tìm mã đơn, mã giao dịch…"><select v-model="st" class="input" aria-label="Trạng thái"><option value="">Mọi trạng thái</option><option v-for="s in ['PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED']" :key="s">{{ s }}</option></select></ListTools>
      <div v-if="pg.loading.value && !pg.items.value.length" class="sk" style="height:160px" />
      <div v-else-if="!pg.items.value.length" class="empty">Chưa có giao dịch.</div>
      <div v-else class="table-wrap"><table class="table"><thead><tr><th>Mã GD</th><th>Đơn</th><th>Cổng</th><th>Số tiền</th><th>Trạng thái</th><th>Mã PH</th><th>Ngân hàng</th><th>IPN</th><th>Tạo lúc</th><th></th></tr></thead>
        <tbody><tr v-for="t in pg.items.value" :key="t.id"><td class="mono small">{{ t.txnRef }}</td><td><router-link :to="`/admin/orders?q=${t.orderCode}`">{{ t.orderCode }}</router-link></td><td>{{ t.provider }}</td>
          <td class="money">{{ vnd(t.amount) }}</td><td><span :class="['badge', { SUCCESS: 'badge-COMPLETED', PENDING: 'badge-PENDING' }[t.status] || 'badge-CANCELLED']">{{ t.status }}</span></td>
          <td>{{ t.responseCode || '—' }}</td><td>{{ t.bankCode || '—' }}</td><td>{{ t.ipnCount }}</td><td class="small">{{ dt(t.createdAt) }}</td>
          <td><template v-if="t.mode === 'REAL'"><button class="btn btn-sm" :disabled="busy" @click="query(t)">Đối soát</button> <button v-if="t.status === 'SUCCESS' && t.refundedAmount < t.amount" class="btn btn-sm" :disabled="busy" @click="refund(t)">Hoàn tiền</button><span v-if="t.refundedAmount" class="small muted"> Đã hoàn {{ vnd(t.refundedAmount) }}</span></template><span v-else class="small muted">{{ t.mode === 'MOCK' ? 'Giả lập' : '' }}</span></td></tr></tbody></table></div>
      <Pager :p="pg" />
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'
import ListTools from '../../components/ListTools.vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import { vnd, dt } from '../../format'
const st = ref('')
const pg = useServerPaged('/admin/payments', { filters: () => ({ 'f.status': st.value }), sorts: { new: { label: 'Mới nhất', sort: 'id,desc' }, amt: { label: 'Số tiền lớn', sort: 'amount,desc' } } })
pg.reload()
const cfg = ref(null), busy = ref(false)
api.get('/admin/payments/gateways').then(r => (cfg.value = r.data)).catch(() => {})
async function query(t) { busy.value = true; try { const { data } = await api.post(`/admin/payments/${t.txnRef}/query`); toast(data.error || `Trạng thái: ${data.localStatus}`); pg.reload() } catch (e) { toast(errMsg(e)) } finally { busy.value = false } }
async function refund(t) {
  const v = prompt('Số tiền hoàn (VND)', String(t.amount - t.refundedAmount)); if (!v) return
  busy.value = true; try { const { data } = await api.post(`/admin/payments/${t.txnRef}/refund`, { amount: Number(v) }); toast(data.refunded ? 'Đã gửi hoàn tiền' : `Cổng từ chối: ${data.vnp_Message || data.message || ''}`); pg.reload() } catch (e) { toast(errMsg(e)) } finally { busy.value = false }
}
</script>
<style scoped>
.gw-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 14px; margin-top: 14px }
.gw-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px }
.gw dl { display: grid; grid-template-columns: 110px 1fr; gap: 4px 10px; margin: 0 }
.gw dt { color: var(--muted, #666) } .gw dd { margin: 0; overflow-wrap: anywhere }
.warn { margin-top: 10px; color: #a15c00 }
</style>
