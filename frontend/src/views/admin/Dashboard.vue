<template>
  <div>
    <h1>Tổng quan</h1>
    <div v-if="!s" class="spinner" />
    <template v-else>
      <div class="grid stats">
        <div class="card"><div class="muted small">Doanh thu đã thu</div><div class="val price"><CountUp :value="s.revenue" :format="vnd" /></div></div>
        <div class="card"><div class="muted small">Đơn hàng</div><div class="val"><CountUp :value="s.orders" /></div><div class="small warn">{{ s.pendingOrders }} chờ xác nhận</div></div>
        <div class="card"><div class="muted small">Khách hàng</div><div class="val"><CountUp :value="s.customers" /></div></div>
        <div class="card"><div class="muted small">Sản phẩm</div><div class="val">{{ s.products }}</div><div class="small warn">{{ s.lowStock }} sắp hết hàng (≤5)</div></div>
        <div class="card"><div class="muted small">Việc cần xử lý</div>
          <router-link to="/admin/returns" class="todo">↩ {{ s.returnsPending }} yêu cầu đổi trả</router-link>
          <router-link to="/admin/tickets" class="todo">🎫 {{ s.ticketsOpen }} phiếu hỗ trợ mở</router-link>
          <router-link to="/admin/questions" class="todo">❓ {{ s.questionsOpen }} câu hỏi chưa trả lời</router-link></div>
      </div>
      <div class="card chart">
        <div class="row between"><div><h3>Doanh thu đã thu</h3><span class="muted small">{{ days }} ngày gần nhất · tổng {{ ty(revTotal) }}</span></div>
          <div class="seg"><button v-for="n in [7, 14, 30]" :key="n" :class="{ on: days === n }" @click="days = n; loadRev()">{{ n }} ngày</button></div></div>
        <div class="bars">
          <div v-for="r in rev" :key="r.date" class="col" :title="`${r.date}: ${vnd(r.revenue)} · ${r.orders} đơn`">
            <span class="tip">{{ r.revenue ? ty(r.revenue) : '' }}</span>
            <div class="b" :style="{ height: Math.max(r.revenue ? 4 : 1, r.revenue * 100 / revMax) + '%' }" />
            <small>{{ r.date.slice(8) }}/{{ r.date.slice(5, 7) }}</small>
          </div>
        </div>
      </div>
      <div class="grid grid-2" style="margin-top:24px">
        <div class="card">
          <h3>Top sản phẩm bán chạy</h3>
          <div v-for="(p, i) in s.topProducts" :key="p.id" class="row tp"><span class="rank">{{ i + 1 }}</span><img loading="lazy" decoding="async" :src="p.image" alt="" />
            <div style="flex:1;min-width:0"><b class="ell">{{ p.name }}</b><div class="small muted">{{ p.qty }} đã bán</div></div><b class="price">{{ ty(p.revenue) }}</b></div>
        </div>
        <div class="card">
          <div class="row between"><h3>⚠ Cảnh báo tồn kho thấp</h3><router-link to="/admin/inventory" class="small">Nhập hàng →</router-link></div>
          <div v-if="!s.lowStockItems.length" class="muted">Tồn kho ổn định.</div>
          <div v-for="p in s.lowStockItems" :key="p.id" class="row tp"><img loading="lazy" decoding="async" :src="p.images?.[0]" alt="" /><div style="flex:1;min-width:0"><b class="ell">{{ p.name }}</b><div class="small muted">{{ p.sku }}</div></div>
            <span :class="['badge', p.stock ? 'badge-PENDING' : 'badge-CANCELLED']">{{ p.stock ? 'Còn ' + p.stock : 'Hết hàng' }}</span></div>
        </div>
      </div>
      <div class="grid grid-2" style="margin-top:24px">
        <div class="card">
          <h3>Đơn theo trạng thái</h3>
          <div v-for="(n, k) in s.ordersByStatus" :key="k" class="bar-row">
            <span>{{ ORDER_STATUS[k] }}</span>
            <div class="bar"><div :style="{ width: (s.orders ? n * 100 / s.orders : 0) + '%' }" /></div><b>{{ n }}</b>
          </div>
        </div>
        <div class="card">
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
import { ref, computed, onMounted } from 'vue'
import api from '../../api'
import CountUp from '../../components/CountUp.vue'
import { vnd, dt, ty, ORDER_STATUS } from '../../format'
const s = ref(null), rev = ref([]), days = ref(14)
const revMax = computed(() => Math.max(1, ...rev.value.map(r => r.revenue)))
const revTotal = computed(() => rev.value.reduce((a, r) => a + r.revenue, 0))
const loadRev = async () => { rev.value = (await api.get('/admin/revenue', { params: { days: days.value } })).data }
onMounted(async () => { s.value = (await api.get('/admin/stats')).data; loadRev() })
</script>
<style scoped>
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
</style>
