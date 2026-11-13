<template>
  <div class="channels-analytics-page">
    <!-- Header -->
    <div class="row between hd">
      <div>
        <h1>Sản lượng & Doanh số Kênh Online và Offline (POS)</h1>
        <p class="muted small">So sánh trực quan sản lượng xe bán ra, doanh thu và hiệu suất giữa kênh Website (Online) và Showroom tại quầy (Offline).</p>
      </div>
      <div class="row ctl">
        <div class="seg" role="tablist">
          <button :class="{ on: quickRange === 'today' }" @click="setQuickRange('today')">Hôm nay</button>
          <button :class="{ on: quickRange === '7d' }" @click="setQuickRange('7d')">7 ngày qua</button>
          <button :class="{ on: quickRange === '30d' }" @click="setQuickRange('30d')">30 ngày qua</button>
          <button :class="{ on: quickRange === 'month' }" @click="setQuickRange('month')">Tháng này</button>
        </div>
        <label class="small date-lbl">Từ <input v-model="fromDate" type="date" class="input" @change="loadData" /></label>
        <label class="small date-lbl">Đến <input v-model="toDate" type="date" class="input" @change="loadData" /></label>
        <button class="btn btn-sm btn-outline" @click="exportCsv">Xuất CSV</button>
        <router-link to="/admin/pos" class="btn btn-sm btn-primary">Vào quầy POS</router-link>
      </div>
    </div>

    <div v-if="loading" class="spinner" />

    <template v-else>
      <!-- Comparison KPI Highlights Grid -->
      <div class="kpi-channels-grid">
        <!-- Doanh thu -->
        <div class="kpi-channel-card">
          <div class="card-title-row">
            <span class="muted small">Tổng doanh thu kỳ này</span>
            <span class="badge badge-accent">{{ vnd(stats.totalRevenue) }}</span>
          </div>
          <div class="channels-split-row">
            <div class="split-col online-col">
              <span class="lbl-channel">Online (Web / App)</span>
              <b class="val-channel">{{ vnd(stats.onlineRevenue) }}</b>
              <small class="muted">{{ stats.onlineRevShare }}% tổng doanh số</small>
            </div>
            <div class="split-col pos-col">
              <span class="lbl-channel">Tại quầy (POS Showroom)</span>
              <b class="val-channel text-accent">{{ vnd(stats.posRevenue) }}</b>
              <small class="muted">{{ stats.posRevShare }}% tổng doanh số</small>
            </div>
          </div>
          <!-- Visual Stacked Bar -->
          <div class="channel-progress-track">
            <div class="channel-progress-bar online-bg" :style="{ width: stats.onlineRevShare + '%' }" :title="`Online: ${stats.onlineRevShare}%`"></div>
            <div class="channel-progress-bar pos-bg" :style="{ width: stats.posRevShare + '%' }" :title="`POS: ${stats.posRevShare}%`"></div>
          </div>
        </div>

        <!-- Sản lượng xe bán ra -->
        <div class="kpi-channel-card">
          <div class="card-title-row">
            <span class="muted small">Sản lượng bán ra (Chiếc)</span>
            <span class="badge">{{ stats.totalItems }} chiếc xe</span>
          </div>
          <div class="channels-split-row">
            <div class="split-col online-col">
              <span class="lbl-channel">Online xuất kho</span>
              <b class="val-channel">{{ stats.onlineItems }} sp</b>
              <small class="muted">{{ calcPercent(stats.onlineItems, stats.totalItems) }}% sản lượng</small>
            </div>
            <div class="split-col pos-col">
              <span class="lbl-channel">Quầy POS bàn giao</span>
              <b class="val-channel text-accent">{{ stats.posItems }} sp</b>
              <small class="muted">{{ calcPercent(stats.posItems, stats.totalItems) }}% sản lượng</small>
            </div>
          </div>
          <div class="channel-progress-track">
            <div class="channel-progress-bar online-bg" :style="{ width: calcPercent(stats.onlineItems, stats.totalItems) + '%' }"></div>
            <div class="channel-progress-bar pos-bg" :style="{ width: calcPercent(stats.posItems, stats.totalItems) + '%' }"></div>
          </div>
        </div>

        <!-- Số lượng đơn hàng & AOV -->
        <div class="kpi-channel-card">
          <div class="card-title-row">
            <span class="muted small">Số đơn & Giá trị trung bình (AOV)</span>
            <span class="badge">{{ stats.totalOrders }} đơn</span>
          </div>
          <div class="channels-split-row">
            <div class="split-col online-col">
              <span class="lbl-channel">Online: {{ stats.onlineOrders }} đơn</span>
              <b class="val-channel">{{ vnd(stats.onlineAov) }}</b>
              <small class="muted">Trung bình / đơn</small>
            </div>
            <div class="split-col pos-col">
              <span class="lbl-channel">Tại quầy: {{ stats.posOrders }} đơn</span>
              <b class="val-channel text-accent">{{ vnd(stats.posAov) }}</b>
              <small class="muted">Trung bình / đơn</small>
            </div>
          </div>
          <div class="channel-progress-track">
            <div class="channel-progress-bar online-bg" :style="{ width: calcPercent(stats.onlineOrders, stats.totalOrders) + '%' }"></div>
            <div class="channel-progress-bar pos-bg" :style="{ width: calcPercent(stats.posOrders, stats.totalOrders) + '%' }"></div>
          </div>
        </div>
      </div>

      <!-- Daily Trend Chart -->
      <div class="card chart-card">
        <div class="row between" style="margin-bottom:14px">
          <div>
            <h3>Diễn biến doanh số & sản lượng theo ngày</h3>
            <p class="small muted">So sánh ngày qua ngày giữa đơn Web Online (màu xanh dương) và hóa đơn quầy POS (màu cam ARES).</p>
          </div>
          <div class="chart-legend">
            <span class="legend-item"><i class="legend-dot online-bg"></i> Online (Web/App)</span>
            <span class="legend-item"><i class="legend-dot pos-bg"></i> Tại quầy (POS)</span>
          </div>
        </div>

        <div v-if="!stats.dailyStats || stats.dailyStats.length === 0" class="muted" style="padding:20px;text-align:center">
          Không có dữ liệu trong khoảng thời gian này.
        </div>
        <div v-else class="bar-chart-container">
          <div v-for="d in stats.dailyStats" :key="d.date" class="chart-bar-col">
            <div class="bars-pair">
              <div
                class="bar-fill online-bg"
                :style="{ height: getBarHeight(d.onlineRevenue) + '%' }"
                :title="`${d.date} - Online: ${vnd(d.onlineRevenue)} (${d.onlineItems} sp)`"
              ></div>
              <div
                class="bar-fill pos-bg"
                :style="{ height: getBarHeight(d.posRevenue) + '%' }"
                :title="`${d.date} - POS: ${vnd(d.posRevenue)} (${d.posItems} sp)`"
              ></div>
            </div>
            <span class="bar-date-label">{{ formatShortDate(d.date) }}</span>
          </div>
        </div>
      </div>

      <!-- Detailed Daily Breakdown Table -->
      <div class="card table-card">
        <div class="row between" style="margin-bottom:12px">
          <h3>Bảng số liệu chi tiết theo từng ngày</h3>
          <span class="small muted">{{ stats.dailyStats?.length || 0 }} ngày ghi nhận</span>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Ngày</th>
                <th style="text-align:right">Doanh thu Online</th>
                <th style="text-align:right">Sản lượng Online</th>
                <th style="text-align:right">Doanh thu POS</th>
                <th style="text-align:right">Sản lượng POS</th>
                <th style="text-align:right">Tổng doanh thu ngày</th>
                <th>Kênh dẫn đầu</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in stats.dailyStats" :key="d.date">
                <td><b>{{ formatDate(d.date) }}</b></td>
                <td style="text-align:right">{{ vnd(d.onlineRevenue) }}</td>
                <td style="text-align:right">{{ d.onlineItems }} sp</td>
                <td style="text-align:right;font-weight:700" class="text-accent">{{ vnd(d.posRevenue) }}</td>
                <td style="text-align:right;font-weight:700" class="text-accent">{{ d.posItems }} sp</td>
                <td style="text-align:right;font-weight:800" class="price">{{ vnd(d.totalRevenue) }}</td>
                <td>
                  <span v-if="d.totalRevenue === 0" class="small muted">—</span>
                  <span v-else-if="d.posRevenue > d.onlineRevenue" class="badge badge-accent">POS Tại quầy</span>
                  <span v-else-if="d.onlineRevenue > d.posRevenue" class="badge">Online Web</span>
                  <span v-else class="badge">Cân bằng</span>
                </td>
              </tr>
              <tr v-if="!stats.dailyStats || stats.dailyStats.length === 0">
                <td colspan="7" class="muted" style="text-align:center;padding:24px">Không có số liệu.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'
import { vnd } from '../../format'

const quickRange = ref('30d')
const fromDate = ref('')
const toDate = ref('')
const loading = ref(false)

const stats = ref({
  totalRevenue: 0,
  onlineRevenue: 0,
  posRevenue: 0,
  onlineRevShare: 0,
  posRevShare: 0,
  totalOrders: 0,
  onlineOrders: 0,
  posOrders: 0,
  totalItems: 0,
  onlineItems: 0,
  posItems: 0,
  onlineAov: 0,
  posAov: 0,
  dailyStats: []
})

const maxDayRevenue = ref(1)

function iso(d) {
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

function setQuickRange(r) {
  quickRange.value = r
  const now = new Date()
  toDate.value = iso(now)

  if (r === 'today') {
    fromDate.value = iso(now)
  } else if (r === '7d') {
    fromDate.value = iso(new Date(now.getTime() - 6 * 86400000))
  } else if (r === '30d') {
    fromDate.value = iso(new Date(now.getTime() - 29 * 86400000))
  } else if (r === 'month') {
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
    fromDate.value = iso(startOfMonth)
  }
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await api.get('/admin/analytics/channels', {
      params: {
        from: fromDate.value,
        to: toDate.value
      }
    })
    stats.value = res.data || {}

    // Compute max for chart scale
    let max = 1
    if (stats.value.dailyStats) {
      stats.value.dailyStats.forEach(d => {
        if (d.onlineRevenue > max) max = d.onlineRevenue
        if (d.posRevenue > max) max = d.posRevenue
      })
    }
    maxDayRevenue.value = max
  } catch (err) {
    console.error('Lỗi tải thống kê On/Off:', err)
  } finally {
    loading.value = false
  }
}

function getBarHeight(val) {
  if (!val || maxDayRevenue.value <= 0) return 3
  const pct = Math.round((val / maxDayRevenue.value) * 100)
  return Math.max(4, Math.min(100, pct))
}

function calcPercent(part, total) {
  if (!total || total <= 0) return 0
  return Math.round((part * 1000.0) / total) / 10.0
}

function formatShortDate(dt) {
  if (!dt) return ''
  const parts = dt.split('-')
  return `${parts[2]}/${parts[1]}`
}

function formatDate(dt) {
  if (!dt) return ''
  const parts = dt.split('-')
  return `${parts[2]}/${parts[1]}/${parts[0]}`
}

function exportCsv() {
  if (!stats.value.dailyStats || stats.value.dailyStats.length === 0) return
  const headers = ['Ngày', 'Doanh thu Online', 'Sản lượng Online (sp)', 'Doanh thu POS', 'Sản lượng POS (sp)', 'Tổng doanh thu']
  const rows = stats.value.dailyStats.map(d => [
    d.date,
    d.onlineRevenue,
    d.onlineItems,
    d.posRevenue,
    d.posItems,
    d.totalRevenue
  ])
  const csvContent = '\ufeff' + [headers, ...rows].map(e => e.join(',')).join('\r\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `san-luong-on-off-${fromDate.value}_${toDate.value}.csv`
  a.click()
}

onMounted(() => {
  setQuickRange('30d')
})
</script>

<style scoped>
.channels-analytics-page {
  padding-bottom: 30px;
}
.hd {
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.ctl {
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.date-lbl {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}
.date-lbl .input {
  width: auto;
  padding: 6px 10px;
}
.seg {
  display: flex;
  border: 1px solid var(--border);
  border-radius: 4px;
  overflow: hidden;
}
.seg button {
  border: none;
  background: none;
  padding: 6px 12px;
  font: 600 0.8rem inherit;
  cursor: pointer;
  color: var(--muted);
}
.seg button.on {
  background: #111;
  color: #fff;
}

/* KPI Cards */
.kpi-channels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.kpi-channel-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.badge-accent {
  background: rgba(255, 106, 0, 0.12);
  color: var(--accent);
  border: 1px solid var(--accent);
  font-weight: 800;
}
.channels-split-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.split-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.lbl-channel {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--muted);
}
.val-channel {
  font-size: 1.25rem;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.text-accent {
  color: var(--accent, #ff6a00);
}

.channel-progress-track {
  height: 8px;
  border-radius: 4px;
  background: var(--bg-2);
  display: flex;
  overflow: hidden;
}
.channel-progress-bar {
  height: 100%;
  transition: width 0.3s ease-out;
}
.online-bg {
  background: #1976d2;
}
.pos-bg {
  background: var(--accent, #ff6a00);
}

/* Daily Chart */
.chart-card {
  margin-bottom: 24px;
  padding: 18px;
}
.chart-legend {
  display: flex;
  gap: 16px;
  font-size: 0.8rem;
  font-weight: 700;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}
.bar-chart-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 220px;
  padding-top: 10px;
  overflow-x: auto;
  border-bottom: 1px solid var(--border);
  padding-bottom: 4px;
}
.chart-bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 28px;
  flex: 1;
  height: 100%;
  justify-content: flex-end;
}
.bars-pair {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  width: 100%;
  height: 180px;
  justify-content: center;
}
.bar-fill {
  width: 10px;
  border-radius: 3px 3px 0 0;
  transition: height 0.3s ease;
  cursor: pointer;
}
.bar-fill:hover {
  opacity: 0.8;
}
.bar-date-label {
  font-size: 0.68rem;
  color: var(--muted);
  white-space: nowrap;
}

/* Table */
.table-card {
  padding: 18px;
}
</style>
