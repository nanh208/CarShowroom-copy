<template>
  <div>
    <div class="row between"><h1>Chương trình ưu đãi</h1><div class="row" style="gap:8px"><div class="seg" role="tablist"><button role="tab" :aria-selected="view === 'list'" :class="{ on: view === 'list' }" @click="view = 'list'">Danh sách</button><button role="tab" :aria-selected="view === 'stats'" :class="{ on: view === 'stats' }" @click="view = 'stats'; loadStats()">Hiệu quả mã</button></div><button class="btn" @click="edit()">+ Tạo ưu đãi</button></div></div>
    <div v-if="view === 'stats'">
      <div class="kp">
        <div class="card"><small class="muted">Lượt dùng</small><b>{{ stats.reduce((a, x) => a + x.orders, 0) }}</b></div>
        <div class="card"><small class="muted">Doanh thu có mã</small><b>{{ vnd(stats.reduce((a, x) => a + x.revenue, 0)) }}</b></div>
        <div class="card"><small class="muted">Tổng tiền đã giảm</small><b>{{ vnd(stats.reduce((a, x) => a + x.discountGiven, 0)) }}</b></div>
        <div class="card"><small class="muted">ROI (doanh thu / giảm)</small><b>{{ roi }}×</b></div>
      </div>
      <div class="card table-wrap">
        <table class="table">
          <thead><tr><th>Mã</th><th>Đơn</th><th>Khách</th><th>Doanh thu</th><th>Đã giảm</th><th>AOV</th><th>Tỉ trọng</th><th>Lượt</th></tr></thead>
          <tbody><tr v-for="x in stats" :key="x.id">
            <td><b>{{ x.code }}</b><div class="small muted">{{ x.title }}</div></td><td>{{ x.orders }}</td><td>{{ x.customers }}</td><td>{{ vnd(x.revenue) }}</td><td>{{ vnd(x.discountGiven) }}</td><td>{{ vnd(x.aov) }}</td>
            <td style="min-width:120px"><div class="sb"><i :style="{ width: (maxRev ? x.revenue * 100 / maxRev : 0) + '%' }" /></div></td>
            <td class="small">{{ x.usedCount }}{{ x.usageLimit ? '/' + x.usageLimit : '' }} <span :class="['badge', x.active ? 'badge-COMPLETED' : 'badge-CANCELLED']">{{ x.active ? 'Chạy' : 'Dừng' }}</span></td>
          </tr></tbody>
        </table>
      </div>
    </div>
    <div v-else class="card table-wrap">
      <table class="table">
        <thead><tr><th>Mã</th><th>Tiêu đề</th><th>Loại</th><th>Giảm</th><th>Tối đa</th><th>Đơn tối thiểu</th><th>Thời gian</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="p in list" :key="p.id">
          <td><b>{{ p.code }}</b></td><td>{{ p.title }}<div class="small muted">{{ p.description }}</div></td><td class="small">{{ VOUCHER_KIND[p.kind] }}</td><td>{{ p.kind === 'FIXED' ? vnd(p.amount) : p.kind === 'FREESHIP' ? 'Phí ship' : p.discountPercent + '%' }}<div v-if="p.usageLimit" class="small muted">{{ p.usedCount }}/{{ p.usageLimit }} lượt</div></td>
          <td>{{ p.maxDiscount ? vnd(p.maxDiscount) : '—' }}</td><td>{{ vnd(p.minOrder) }}</td>
          <td class="small">{{ d(p.startDate) }} → {{ d(p.endDate) }}</td>
          <td><span :class="['badge', p.validNow ? 'badge-COMPLETED' : 'badge-CANCELLED']">{{ p.validNow ? 'Đang chạy' : (p.active ? 'Ngoài thời hạn' : 'Tắt') }}</span></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:6px"><button class="btn btn-sm btn-outline" @click="edit(p)">Sửa</button><button class="btn btn-sm btn-danger" @click="remove(p)">Xoá</button></div></td>
        </tr></tbody>
      </table>
    </div>
    <div v-if="form" class="modal-bg" @click.self="form = null">
      <div class="modal">
        <h3>{{ form.id ? 'Sửa ưu đãi' : 'Tạo ưu đãi' }}</h3>
        <div v-if="err" class="alert alert-error">{{ err }}</div>
        <div class="form-grid" style="margin-top:12px">
          <div class="field"><label>Mã *</label><input v-model="form.code" class="input" /></div>
          <div class="field"><label>Tiêu đề</label><input v-model="form.title" class="input" /></div>
          <div class="field"><label>Loại</label><select v-model="form.kind" class="input"><option v-for="(v, k) in VOUCHER_KIND" :key="k" :value="k">{{ v }}</option></select></div>
          <div v-if="form.kind === 'PERCENT'" class="field"><label>% giảm</label><input v-model.number="form.discountPercent" type="number" class="input" /></div>
          <div v-if="form.kind === 'FIXED'" class="field"><label>Số tiền giảm (đ)</label><input v-model.number="form.amount" type="number" class="input" /></div>
          <div class="field"><label>Giới hạn lượt (0 = không)</label><input v-model.number="form.usageLimit" type="number" class="input" /></div>
          <div class="field"><label class="check"><input v-model="form.isPublic" type="checkbox" /> Hiển thị công khai</label><label class="check"><input v-model="form.active" type="checkbox" /> Kích hoạt</label></div>
          <div class="field"><label>Giảm tối đa (đ, 0 = không giới hạn)</label><input v-model.number="form.maxDiscount" type="number" class="input" /></div>
          <div class="field"><label>Đơn tối thiểu (đ)</label><input v-model.number="form.minOrder" type="number" class="input" /></div>
          <div class="field"><label>Màu banner</label><input v-model="form.bannerColor" type="color" class="input" style="height:44px;padding:4px" /></div>
          <div class="field"><label>Bắt đầu</label><input v-model="form.startDate" type="date" class="input" /></div>
          <div class="field"><label>Kết thúc</label><input v-model="form.endDate" type="date" class="input" /></div>
        </div>
        <div class="field"><label>Mô tả</label><textarea v-model="form.description" rows="2" class="input" /></div>
        <label class="check field"><input v-model="form.active" type="checkbox" /> Kích hoạt</label>
        <div class="row"><button class="btn" @click="save">Lưu</button><button class="btn btn-outline" @click="form = null">Huỷ</button></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, d, VOUCHER_KIND } from '../../format'
const list = ref([]), form = ref(null), err = ref(''), view = ref('list'), stats = ref([])
const loadStats = async () => { stats.value = (await api.get('/admin/promotions/stats')).data }
const maxRev = computed(() => Math.max(0, ...stats.value.map(x => x.revenue)))
const roi = computed(() => { const g = stats.value.reduce((a, x) => a + x.discountGiven, 0); return g ? (stats.value.reduce((a, x) => a + x.revenue, 0) / g).toFixed(1) : '—' })
async function load() { list.value = (await api.get('/admin/promotions')).data }
const edit = p => { err.value = ''; form.value = p ? { ...p } : { code: '', title: '', kind: 'PERCENT', discountPercent: 10, amount: 0, usageLimit: 0, isPublic: true, maxDiscount: 0, minOrder: 0, active: true, bannerColor: '#a87a3c', startDate: new Date().toISOString().slice(0, 10), endDate: '' } }
async function save() {
  const f = { ...form.value }; delete f.validNow
  if (!f.endDate) f.endDate = null
  try { f.id ? await api.put(`/admin/promotions/${f.id}`, f) : await api.post('/admin/promotions', f); form.value = null; load() } catch (e) { err.value = errMsg(e) }
}
async function remove(p) { if (confirm(`Xoá mã ${p.code}?`)) { await api.delete(`/admin/promotions/${p.id}`); load() } }
onMounted(load)
</script>

<style scoped>
.seg { display: flex; border: 1px solid var(--border); } .seg button { border: none; background: none; padding: 7px 12px; font: 600 .82rem inherit; font-family: inherit; cursor: pointer; color: var(--muted); } .seg button.on { background: #111; color: #fff; }
.kp { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; margin: 16px 0; } .kp b { display: block; font-size: 1.3rem; margin-top: 4px; }
.sb { height: 8px; background: var(--bg-2); } .sb i { display: block; height: 100%; background: var(--accent); }
</style>
