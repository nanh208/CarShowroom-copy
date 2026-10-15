<template>
  <div>
    <div class="row between dh"><h1>Tổng quan</h1>
      <details class="wt"><summary class="btn btn-sm btn-outline">⚙ Tuỳ chỉnh widget</summary>
        <div class="wtp card"><label v-for="(l, k) in WIDGETS" :key="k" class="small"><input v-model="show[k]" type="checkbox" /> {{ l }}</label></div></details></div>
    <div v-if="!s" class="spinner" />
    <template v-else>
      <div v-show="show.stats" class="grid stats">
        <div class="card"><div class="muted small">Doanh thu đã thu</div><div class="val price"><CountUp :value="s.revenue" :format="vnd" /></div></div>
        <div class="card"><div class="muted small">Đơn hàng</div><div class="val"><CountUp :value="s.orders" /></div><div class="small warn">{{ s.pendingOrders }} chờ xác nhận</div></div>
        <div class="card"><div class="muted small">Khách hàng</div><div class="val"><CountUp :value="s.customers" /></div></div>
        <div class="card"><div class="muted small">Sản phẩm</div><div class="val">{{ s.products }}</div><div class="small warn">{{ s.lowStock }} sắp hết hàng (≤5)</div></div>
        <div class="card"><div class="muted small">Việc cần xử lý</div>
          <router-link to="/admin/returns" class="todo">↩ {{ s.returnsPending }} yêu cầu đổi trả</router-link>
          <router-link to="/admin/tickets" class="todo">🎫 {{ s.ticketsOpen }} phiếu hỗ trợ mở</router-link>
          <router-link to="/admin/questions" class="todo">❓ {{ s.questionsOpen }} câu hỏi chưa trả lời</router-link></div>
      </div>
      <div v-show="show.chart" class="card chart">
        <div class="rangebar">
          <div><h3>Hiệu quả kinh doanh</h3><span class="muted small">{{ fmtD(from) }} – {{ fmtD(to) }} · so với {{ spanDays }} ngày trước đó</span></div>
          <div class="range">
            <div class="seg" role="group" aria-label="Khoảng thời gian"><button v-for="p in PRESETS" :key="p.k" :class="{ on: preset === p.k }" @click="setPreset(p.k)">{{ p.l }}</button></div>
            <label class="dt">Từ <input v-model="from" type="date" class="input" :max="to" @change="preset = ''; loadKpi()" /></label>
            <label class="dt">Đến <input v-model="to" type="date" class="input" :min="from" :max="today" @change="preset = ''; loadKpi()" /></label>
          </div>
        </div>
        <div v-if="k" class="kpis">
          <div v-for="m in KPIS" :key="m.k" class="kpi">
            <small>{{ m.l }}</small>
            <b><CountUp :value="k.current[m.k]" :format="m.f" /></b>
            <span v-if="delta(m.k) !== null" :class="['dl', (m.inv ? -delta(m.k) : delta(m.k)) >= 0 ? 'up' : 'down']">{{ delta(m.k) >= 0 ? '▲' : '▼' }} {{ Math.abs(delta(m.k)) }}%</span>
            <span v-else class="dl muted">—</span>
          </div>
        </div>
        <div class="bars">
          <div v-for="r in rev" :key="r.date" class="col" :title="`${r.date}: ${vnd(r.revenue)}`">
            <span class="tip">{{ r.revenue && rev.length <= 31 ? ty(r.revenue) : '' }}</span>
            <div class="b" :style="{ height: Math.max(r.revenue ? 4 : 1, r.revenue * 100 / revMax) + '%' }" />
            <small v-if="rev.length <= 16 || i0(r) % Math.ceil(rev.length / 12) === 0">{{ r.date.slice(8) }}/{{ r.date.slice(5, 7) }}</small>
          </div>
        </div>
      </div>
      <div class="grid grid-2" style="margin-top:24px">
        <div v-show="show.top" class="card">
          <h3>Top sản phẩm bán chạy</h3>
          <div v-for="(p, i) in s.topProducts" :key="p.id" class="row tp"><span class="rank">{{ i + 1 }}</span><img loading="lazy" decoding="async" :src="p.image" alt="" />
            <div style="flex:1;min-width:0"><b class="ell">{{ p.name }}</b><div class="small muted">{{ p.qty }} đã bán</div></div><b class="price">{{ ty(p.revenue) }}</b></div>
        </div>
        <div v-show="show.low" class="card">
          <div class="row between"><h3>⚠ Cảnh báo tồn kho thấp</h3><router-link to="/admin/inventory" class="small">Nhập hàng →</router-link></div>
          <div v-if="!s.lowStockItems.length" class="muted">Tồn kho ổn định.</div>
          <div v-for="p in s.lowStockItems" :key="p.id" class="row tp"><img loading="lazy" decoding="async" :src="p.images?.[0]" alt="" /><div style="flex:1;min-width:0"><b class="ell">{{ p.name }}</b><div class="small muted">{{ p.sku }}</div></div>
            <span :class="['badge', p.stock ? 'badge-PENDING' : 'badge-CANCELLED']">{{ p.stock ? 'Còn ' + p.stock : 'Hết hàng' }}</span></div>
        </div>
      </div>
      <div class="grid grid-2" style="margin-top:24px">
        <div v-show="show.status" class="card">
          <h3>Đơn theo trạng thái</h3>
          <div v-for="(n, k) in s.ordersByStatus" :key="k" class="bar-row">
            <span>{{ ORDER_STATUS[k] }}</span>
            <div class="bar"><div :style="{ width: (s.orders ? n * 100 / s.orders : 0) + '%' }" /></div><b>{{ n }}</b>
          </div>
        </div>
        <div v-show="show.recent" class="card">
          <h3>Đơn mới nhất</h3>
          <div v-if="!s.recentOrders.length" class="muted">Chưa có đơn.</div>
          <div v-for="o in s.recentOrders" :key="o.id" class="row between ro">
            <div><b>{{ o.code }}</b><div class="small muted">{{ o.buyerName }} · {{ dt(o.createdAt) }}</div></div>
            <div style="text-align:right"><div class="price">{{ vnd(o.total) }}</div><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span></div>
          </div>
          <router-link to="/admin/orders" class="small">Quản lý đơn hàng →</router-link>
        </div>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, reactive, watch, computed, onMounted } from 'vue'
const WIDGETS = { stats: 'Chỉ số tổng', chart: 'Biểu đồ hiệu quả', top: 'Top bán chạy', low: 'Tồn kho thấp', status: 'Đơn theo trạng thái', recent: 'Đơn mới nhất' }
// Tuỳ chọn widget lưu theo tài khoản ở backend (/api/account/prefs/dashWidgets)
const show = reactive(Object.fromEntries(Object.keys(WIDGETS).map(k => [k, true])))
let wReady = false, wT
api.get('/account/prefs').then(r => { if (r.data.dashWidgets) Object.assign(show, r.data.dashWidgets) }).finally(() => { wReady = true })
watch(show, v => { if (!wReady) return; clearTimeout(wT); wT = setTimeout(() => api.put('/account/prefs/dashWidgets', { ...v }).catch(() => {}), 400) })
import api from '../../api'
import CountUp from '../../components/CountUp.vue'
import { vnd, dt, ty, ORDER_STATUS } from '../../format'
const s = ref(null), k = ref(null)
const iso = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const today = iso(new Date()), daysAgo = n => iso(new Date(Date.now() - n * 864e5))
const PRESETS = [{ k: '7', l: '7 ngày' }, { k: '14', l: '14 ngày' }, { k: '30', l: '30 ngày' }, { k: 'month', l: 'Tháng này' }]
const preset = ref('14'), from = ref(daysAgo(13)), to = ref(today)
const setPreset = p => { preset.value = p; to.value = today; from.value = p === 'month' ? today.slice(0, 8) + '01' : daysAgo(+p - 1); loadKpi() }
const KPIS = [{ k: 'revenue', l: 'Doanh thu', f: v => vnd(v) }, { k: 'orders', l: 'Đơn hàng' }, { k: 'aov', l: 'Giá trị TB/đơn', f: v => vnd(v) },
  { k: 'items', l: 'Sản phẩm bán ra' }, { k: 'newCustomers', l: 'Khách mới' }, { k: 'cancelRate', l: 'Tỉ lệ huỷ', f: v => v.toFixed?.(1) + '%', inv: true }]
const delta = key => { const c = k.value?.current[key], p = k.value?.previous[key]; if (!p) return c ? 100 : null; return Math.round((c - p) * 100 / p) }
const rev = computed(() => k.value ? Object.entries(k.value.daily).map(([date, revenue]) => ({ date, revenue })) : [])
const i0 = r => rev.value.indexOf(r)
const revMax = computed(() => Math.max(1, ...rev.value.map(r => r.revenue)))
const spanDays = computed(() => Math.round((new Date(to.value) - new Date(from.value)) / 864e5) + 1)
const fmtD = s => s.split('-').reverse().join('/')
const loadKpi = async () => { if (from.value > to.value) return; k.value = (await api.get('/admin/kpi', { params: { from: from.value, to: to.value } })).data }
onMounted(async () => { s.value = (await api.get('/admin/stats')).data; loadKpi() })
</script>
<style scoped>
.dh { gap: 10px; flex-wrap: wrap; } .wt { position: relative; } .wt summary { list-style: none; } .wt summary::-webkit-details-marker { display: none; } .wtp { position: absolute; right: 0; top: 110%; z-index: 20; display: flex; flex-direction: column; gap: 6px; min-width: 210px; box-shadow: 0 8px 24px rgba(0,0,0,.12); } .wtp label { display: flex; gap: 6px; align-items: center; }
.stats { grid-template-columns: repeat(5, 1fr) } @media (max-width: 1100px) { .stats { grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)) }; }
.val { font-size: 1.7rem; font-weight: 800; margin: 4px 0; } .warn { color: #f5b800; }
.bar-row { display: grid; grid-template-columns: 120px 1fr 30px; gap: 10px; align-items: center; margin-bottom: 10px; font-size: .9rem; }
.bar { background: rgba(0,0,0,.06); border-radius: 6px; height: 10px; overflow: hidden; } .bar div { background: var(--primary); height: 100%; }
.todo { display: block; font-size: .85rem; margin-top: 6px; color: var(--text-2); } .todo:hover { color: var(--accent); }
.tp { gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border); flex-wrap: nowrap; } .tp img { width: 44px; height: 44px; object-fit: cover; border-radius: 8px; }
.rank { width: 22px; font-weight: 800; color: var(--accent); } .ell { display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ro { padding: 8px 0; border-bottom: 1px solid var(--border); }
.chart { margin-top: 24px; }
.seg { display: flex; background: rgba(0,0,0,.05); border-radius: 999px; padding: 4px; }
.seg button { border: none; background: none; color: var(--muted); padding: 6px 14px; border-radius: 999px; cursor: pointer; font: 600 .8rem inherit; font-family: inherit; }
.seg button.on { background: var(--accent); color: #fff; }
.bars { display: flex; align-items: flex-end; gap: 6px; height: 240px; margin-top: 24px; padding-top: 24px; border-bottom: 1px solid var(--border); }
.col { flex: 1; height: 100%; display: flex; flex-direction: column; justify-content: flex-end; align-items: center; position: relative; }
.b { width: 100%; max-width: 40px; border-radius: 6px 6px 0 0; background: linear-gradient(180deg, var(--accent), var(--primary)); transition: height .4s; }
.col:hover .b { filter: brightness(1.3); }
.col small { position: absolute; bottom: -24px; color: var(--muted); font-size: .7rem; }
.tip { font-size: .7rem; color: var(--text-2); margin-bottom: 4px; white-space: nowrap; }
.bars { margin-bottom: 28px; }
.rangebar { display: flex; justify-content: space-between; align-items: flex-start; gap: 14px; flex-wrap: wrap; }
.range { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; } .dt { display: flex; gap: 6px; align-items: center; margin: 0; font-size: .82rem; } .dt .input { width: auto; padding: 6px 8px; min-height: 0; }
.kpis { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 0; margin-top: 18px; border: 1px solid var(--border); }
.kpi { padding: 12px 14px; display: flex; flex-direction: column; gap: 2px; min-width: 0; } .kpi + .kpi { border-left: 1px solid var(--border); }
.kpi small { color: var(--muted); font-size: .76rem; } .kpi b { font-size: clamp(1rem, .8rem + .8vw, 1.35rem); font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dl { font-size: .74rem; font-weight: 700; } .dl.up { color: var(--success); } .dl.down { color: var(--danger); }
@media (max-width: 1100px) { .kpis { grid-template-columns: repeat(3, 1fr); } .kpi:nth-child(4) { border-left: none; } .kpi:nth-child(n+4) { border-top: 1px solid var(--border); } }
@media (max-width: 599px) { .kpis { grid-template-columns: repeat(2, 1fr); } .kpi { border-left: none !important; border-top: 1px solid var(--border); } .kpi:nth-child(even) { border-left: 1px solid var(--border) !important; } .bar-row { grid-template-columns: 90px 1fr 26px; } .bars { gap: 2px; height: 180px; } }
</style>
