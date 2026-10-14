<template>
  <div>
    <h1>Báo cáo & xuất CSV</h1>
    <p class="muted">File CSV UTF-8 (mở trực tiếp bằng Excel). Chỉ tài khoản ADMIN được xuất báo cáo.</p>
    <div class="grid rgrid">
      <div v-for="r in reports" :key="r.kind" class="card rep">
        <div class="ico">{{ r.icon }}</div><b>{{ r.title }}</b><div class="small muted">{{ r.desc }}</div>
        <button class="btn btn-sm" :disabled="busy === r.kind" @click="dl(r.kind)">{{ busy === r.kind ? 'Đang xuất…' : '⬇ Tải CSV' }}</button>
      </div>
    </div>
    <div v-if="err" class="alert alert-error" style="margin-top:16px">{{ err }}</div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import api, { errMsg } from '../../api'
const busy = ref(''), err = ref('')
const reports = [
  { kind: 'orders', icon: '🧾', title: 'Đơn hàng', desc: 'Mã đơn, khách, trạng thái, thanh toán, tổng tiền, voucher, điểm.' },
  { kind: 'products', icon: '🚗', title: 'Sản phẩm', desc: 'SKU, danh mục, hãng, giá, tồn kho, đã bán, đánh giá.' },
  { kind: 'customers', icon: '👥', title: 'Khách hàng', desc: 'Thông tin, hạng thành viên, điểm, số đơn, tổng chi tiêu.' },
  { kind: 'inventory', icon: '📦', title: 'Xuất nhập kho', desc: 'Lịch sử thay đổi tồn kho gần nhất.' }]
const dl = async kind => {
  busy.value = kind; err.value = ''
  try {
    const r = await api.get(`/admin/reports/${kind}.csv`, { responseType: 'blob' })
    const a = document.createElement('a'); a.href = URL.createObjectURL(r.data); a.download = `ares-${kind}-${new Date().toISOString().slice(0, 10)}.csv`; a.click(); URL.revokeObjectURL(a.href)
  } catch (e) { err.value = e.response?.status === 403 ? 'Bạn không có quyền xuất báo cáo.' : errMsg(e) } finally { busy.value = '' }
}
</script>
<style scoped>
.rgrid { grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.rep { display: flex; flex-direction: column; gap: 8px; align-items: flex-start; } .ico { font-size: 2rem; }
</style>
