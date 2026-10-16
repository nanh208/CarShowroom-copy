<template>
  <div>
    <div class="row between wrap"><h1>Dự báo tồn kho</h1><div class="row" style="gap:6px"><button class="btn btn-sm btn-outline" :disabled="!d" @click="csv">Xuất CSV</button><router-link to="/admin/purchasing" class="btn btn-sm">Tạo đơn nhập →</router-link></div></div>
    <p class="muted small">Dự báo từ lịch sử bán (đơn không huỷ/hoàn) bằng làm trơn mũ Holt có giảm chấn xu hướng. Tồn an toàn = z·σ·√(thời gian chờ hàng). Gợi ý nhập = nhu cầu (chờ hàng + kỳ dự báo) + tồn an toàn − tồn hiện tại.</p>
    <div class="card ctl">
      <label>Lịch sử<select v-model.number="p.days" class="input"><option :value="30">30 ngày</option><option :value="60">60 ngày</option><option :value="90">90 ngày</option><option :value="180">180 ngày</option></select></label>
      <label>Kỳ dự báo<select v-model.number="p.horizon" class="input"><option :value="14">14 ngày</option><option :value="30">30 ngày</option><option :value="60">60 ngày</option></select></label>
      <label>Chờ hàng (ngày)<input v-model.number="p.lead" type="number" min="1" max="60" class="input" /></label>
      <label>Mức phục vụ<select v-model.number="p.serviceLevel" class="input"><option :value="0.9">90%</option><option :value="0.95">95%</option><option :value="0.99">99%</option></select></label>
      <label class="q">Tìm<input v-model="q" class="input" placeholder="Tên / SKU" /></label>
    </div>
    <div v-if="d" class="sum">
      <button v-for="r in RISKS" :key="r[0]" :class="['card', 'stat', r[0], { on: risk === r[0] }]" @click="risk = risk === r[0] ? '' : r[0]"><b>{{ d.counts[r[0]] || 0 }}</b><span>{{ r[1] }}</span></button>
    </div>
    <div v-if="loading && !d" class="sk" style="height:320px" />
    <div v-else-if="d" class="table-wrap card"><table class="table">
      <thead><tr><th>Sản phẩm</th><th>Tồn</th><th>Đã bán</th><th>12 tuần</th><th>TB/ngày</th><th>Dự báo {{ d.horizon }}n</th><th>Đủ bán</th><th>Tồn an toàn</th><th>Điểm đặt</th><th>Gợi ý nhập</th><th>Mức</th></tr></thead>
      <tbody>
        <tr v-for="r in rows" :key="r.productId">
          <td><div class="pn"><img v-if="r.image" :src="r.image" alt="" loading="lazy" decoding="async" /><div><b>{{ r.name }}</b><div class="small muted">{{ r.sku }}</div></div></div></td>
          <td>{{ r.stock }}</td><td>{{ r.sold }}</td>
          <td><svg class="spark" viewBox="0 0 60 18" preserveAspectRatio="none" aria-hidden="true"><polyline :points="spark(r.weekly)" fill="none" stroke="currentColor" stroke-width="1.5" /></svg></td>
          <td>{{ r.avgDaily }}<span v-if="r.trendPerDay" :class="['small', r.trendPerDay > 0 ? 'up' : 'down']"> {{ r.trendPerDay > 0 ? '↑' : '↓' }}</span></td>
          <td>{{ r.forecast }}</td>
          <td>{{ r.daysOfCover == null ? '—' : r.daysOfCover >= 365 ? '> 1 năm' : Math.floor(r.daysOfCover) + ' ngày' }}<div v-if="r.stockoutDate && r.daysOfCover < 365" class="small muted">hết ~{{ dd(r.stockoutDate) }}</div></td>
          <td>{{ r.safetyStock }}</td><td>{{ r.reorderPoint }}</td>
          <td><b :class="{ hot: r.reorderQty > 0 }">{{ r.reorderQty || '—' }}</b></td>
          <td><span :class="['badge', 'r-' + r.risk]">{{ RL[r.risk] }}</span></td>
        </tr>
        <tr v-if="!rows.length"><td colspan="11" class="muted">Không có sản phẩm phù hợp.</td></tr>
      </tbody></table></div>
  </div>
</template>
<script setup>
import { ref, reactive, computed, watch } from 'vue'
import api from '../../api'
import { d as dd } from '../../format'
const RISKS = [['OUT', 'Hết hàng'], ['HIGH', 'Sắp hết (< chờ hàng)'], ['MEDIUM', 'Cần theo dõi'], ['LOW', 'Đủ hàng'], ['NONE', 'Chưa có bán']]
const RL = Object.fromEntries(RISKS.map(r => [r[0], r[1].split(' (')[0]]))
const p = reactive({ days: 90, horizon: 30, lead: 7, serviceLevel: 0.95 }), d = ref(null), loading = ref(false), risk = ref(''), q = ref('')
const rows = computed(() => (d.value?.items || []).filter(r => (!risk.value || r.risk === risk.value) && (!q.value || (r.name + ' ' + r.sku).toLowerCase().includes(q.value.toLowerCase()))))
const spark = w => { const m = Math.max(1, ...w); return w.map((v, i) => `${(i / Math.max(1, w.length - 1)) * 60},${17 - (v / m) * 16}`).join(' ') }
let t
watch(p, () => { clearTimeout(t); t = setTimeout(load, 250) })
async function load() { loading.value = true; try { d.value = (await api.get('/admin/forecast', { params: { ...p, lead: Math.min(60, Math.max(1, p.lead || 7)) } })).data } finally { loading.value = false } }
function csv() {
  const h = ['productId', 'sku', 'name', 'stock', 'sold', 'avgDaily', 'forecast', 'daysOfCover', 'safetyStock', 'reorderPoint', 'reorderQty', 'risk']
  const body = rows.value.map(r => h.map(k => `"${String(r[k] ?? '').replace(/"/g, '""')}"`).join(',')).join('\n')
  const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob(['\ufeff' + h.join(',') + '\n' + body], { type: 'text/csv' })); a.download = 'du-bao-ton-kho.csv'; a.click()
}
load()
</script>
<style scoped>
.wrap { flex-wrap: wrap; gap: 8px }
.ctl { display: flex; flex-wrap: wrap; gap: 12px; padding: 12px; margin: 10px 0 }
.ctl label { display: flex; flex-direction: column; gap: 4px; font-size: 12px; color: var(--muted); min-width: 120px }
.ctl .q { flex: 1; min-width: 160px }
.sum { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 10px; margin-bottom: 12px }
.stat { display: flex; flex-direction: column; align-items: flex-start; gap: 2px; padding: 12px; border: 1px solid var(--border); text-align: left; font: inherit; color: inherit }
.stat b { font-size: 1.5rem } .stat span { font-size: 12px; color: var(--muted) }
.stat.on { outline: 2px solid var(--accent, #f15a24) }
.stat.OUT b, .stat.HIGH b { color: #b3261e } .stat.MEDIUM b { color: #8a6100 } .stat.LOW b { color: #1b7f3b }
.pn { display: flex; gap: 8px; align-items: center; min-width: 200px } .pn img { width: 40px; height: 40px; object-fit: cover; border-radius: 4px }
.spark { width: 60px; height: 18px; color: var(--accent, #f15a24) }
.up { color: #1b7f3b } .down { color: #b3261e } .hot { color: #b3261e }
.r-OUT, .r-HIGH { background: #fdecea; color: #b3261e } .r-MEDIUM { background: #fff3cd; color: #8a6100 } .r-LOW { background: #e6f4ea; color: #1b7f3b } .r-NONE { background: #eee; color: #666 }
</style>
