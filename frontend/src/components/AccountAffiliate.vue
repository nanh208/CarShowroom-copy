<template>
  <div>
    <div v-if="!data" class="card">Đang tải…</div>
    <div v-else-if="!data.joined" class="card join">
      <h3>Chương trình tiếp thị liên kết</h3>
      <p>Chia sẻ liên kết của bạn — nhận <b>{{ data.defaultPct }}%</b> hoa hồng trên mỗi đơn giao thành công (tính trên giá trị hàng sau giảm giá). Liên kết ghi nhớ người mua trong 30 ngày.</p>
      <button class="btn" :disabled="busy" @click="join">Tham gia ngay</button>
    </div>
    <template v-else>
      <div class="kpis">
        <div class="card k"><small>Lượt nhấp</small><b>{{ data.affiliate.clicks }}</b></div>
        <div class="card k"><small>Đơn ghi nhận</small><b>{{ data.orders }}</b><span class="small muted">{{ data.conversion }}% chuyển đổi</span></div>
        <div class="card k"><small>Chờ giao</small><b>{{ vnd(data.totals.PENDING) }}</b></div>
        <div class="card k"><small>Đã duyệt</small><b>{{ vnd(data.totals.APPROVED) }}</b></div>
        <div class="card k"><small>Đã chi trả</small><b>{{ vnd(data.totals.PAID) }}</b></div>
      </div>
      <div class="grid grid-2" style="margin-top:14px">
        <div class="card">
          <h3>Liên kết của bạn</h3>
          <p class="small muted">Mã <b>{{ data.affiliate.code }}</b> · hoa hồng {{ data.affiliate.commissionPct }}% · <span class="badge">{{ data.affiliate.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm khoá' }}</span></p>
          <div class="row"><input class="input grow" readonly :value="full" aria-label="Liên kết tiếp thị" data-no-i18n /><button class="btn btn-sm" @click="copy">Sao chép</button></div>
          <p class="small muted" style="margin-top:8px">Mẹo: thêm <code>?aff={{ data.affiliate.code }}</code> vào cuối bất kỳ liên kết sản phẩm nào.</p>
        </div>
        <form class="card" @submit.prevent="savePayout">
          <h3>Thông tin nhận tiền</h3>
          <textarea v-model="payout" class="input" rows="3" maxlength="300" placeholder="VD: Vietcombank 0123456789 — Nguyễn Văn A" aria-label="Thông tin nhận tiền" />
          <button class="btn btn-sm" style="margin-top:8px">Lưu</button>
        </form>
      </div>
      <div class="card table-wrap" style="margin-top:14px">
        <table class="table">
          <thead><tr><th>Đơn</th><th>Ngày</th><th>Giá trị tính</th><th>Hoa hồng</th><th>Trạng thái</th></tr></thead>
          <tbody><tr v-for="c in data.commissions" :key="c.id"><td>{{ c.orderCode }}</td><td class="small">{{ d(c.createdAt) }}</td><td>{{ vnd(c.base) }}</td><td class="price">{{ vnd(c.amount) }}</td><td><span class="badge" :class="'c-' + c.status">{{ CS[c.status] }}</span></td></tr></tbody>
        </table>
        <p v-if="!data.commissions.length" class="muted" style="padding:12px">Chưa có đơn nào qua liên kết của bạn.</p>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../api'
import { vnd, d } from '../format'
import { toast } from '../stores/prefs'
const CS = { PENDING: 'Chờ giao', APPROVED: 'Đã duyệt', PAID: 'Đã chi trả', VOID: 'Đã huỷ' }
const data = ref(null), busy = ref(false), payout = ref('')
const full = computed(() => location.origin + (data.value?.link || ''))
async function load() { data.value = (await api.get('/affiliate/me')).data; payout.value = data.value.affiliate?.payoutInfo || '' }
async function join() { busy.value = true; try { data.value = (await api.post('/affiliate/join')).data; toast('Đã tham gia chương trình tiếp thị liên kết') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
async function savePayout() { try { data.value = (await api.put('/affiliate/payout-info', { payoutInfo: payout.value })).data; toast('Đã lưu thông tin nhận tiền') } catch (e) { toast(errMsg(e), 'error') } }
function copy() { navigator.clipboard?.writeText(full.value).then(() => toast('Đã sao chép liên kết'), () => {}) }
onMounted(load)
</script>
<style scoped>
.join p { margin: 8px 0 14px; } .kpis { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
.k { display: flex; flex-direction: column; gap: 2px; padding: 12px 14px; } .k b { font-size: 1.2rem; }
.c-APPROVED { background: #e7f6ec; color: #17803d; } .c-PAID { background: #111; color: #fff; } .c-VOID { opacity: .55; }
@media (max-width: 900px) { .kpis { grid-template-columns: repeat(2, 1fr); } }
</style>
