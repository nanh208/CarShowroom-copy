<template>
  <div>
    <h1>Tổng quan</h1>
    <div v-if="!s" class="spinner" />
    <template v-else>
      <div class="grid stats">
        <div class="card"><div class="muted small">Doanh thu đã thu</div><div class="val price">{{ vnd(s.revenue) }}</div></div>
        <div class="card"><div class="muted small">Đơn hàng</div><div class="val">{{ s.orders }}</div><div class="small warn">{{ s.pendingOrders }} chờ xác nhận</div></div>
        <div class="card"><div class="muted small">Khách hàng</div><div class="val">{{ s.customers }}</div></div>
        <div class="card"><div class="muted small">Sản phẩm</div><div class="val">{{ s.products }}</div><div class="small warn">{{ s.lowStock }} sắp hết hàng (≤3)</div></div>
        <div class="card"><div class="muted small">Lịch hẹn chờ xử lý</div><div class="val">{{ s.bookingsPending }}</div></div>
      </div>
      <div class="grid grid-2" style="margin-top:22px">
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
import { ref, onMounted } from 'vue'
import api from '../../api'
import { vnd, dt, ORDER_STATUS } from '../../format'
const s = ref(null)
onMounted(async () => { s.value = (await api.get('/admin/stats')).data })
</script>
<style scoped>
.stats { grid-template-columns: repeat(5, 1fr) } @media (max-width: 1100px) { .stats { grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)) }; }
.val { font-size: 1.7rem; font-weight: 800; margin: 4px 0; } .warn { color: #fcd34d; }
.bar-row { display: grid; grid-template-columns: 120px 1fr 30px; gap: 10px; align-items: center; margin-bottom: 10px; font-size: .9rem; }
.bar { background: rgba(255,255,255,.06); border-radius: 6px; height: 10px; overflow: hidden; } .bar div { background: var(--primary); height: 100%; }
.ro { padding: 8px 0; border-bottom: 1px solid var(--border); }
</style>
