<template>
  <div>
    <div class="row between dh">
      <h1>Tổng quan</h1>
      <div class="row" style="gap:8px;align-items:center">
        <router-link to="/admin/pos" class="btn btn-sm btn-primary">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:-2px"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/><circle cx="7" cy="15" r="1"/></svg>
          Vào quầy POS
        </router-link>
        <router-link to="/admin/sales-channels" class="btn btn-sm btn-outline">
          Sản lượng On & Off
        </router-link>
        <details class="wt">
        <summary class="btn btn-sm btn-outline">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:-2px"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          Tuỳ chỉnh widget
        </summary>
        <div class="wtp card">
          <label v-for="(l, k) in WIDGETS" :key="k" class="small">
            <input v-model="show[k]" type="checkbox" /> {{ l }}
          </label>
        </div>
      </details>
      </div>
    </div>

    <div v-if="!s" class="spinner" />
    <template v-else>
      <!-- Quick Highlights: Today & Yesterday Revenue -->
      <div v-show="show.stats" class="daily-highlights-strip">
        <div class="dh-card">
          <div class="dh-icon">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"/></svg>
          </div>
          <div class="dh-content">
            <span class="dh-label">Doanh thu hôm nay</span>
            <b class="dh-val price">{{ vnd(s.todayRevenue || 0) }}</b>
            <small class="muted">{{ s.todayOrders || 0 }} đơn hàng</small>
          </div>
        </div>
        <div class="dh-card">
          <div class="dh-icon">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
          </div>
          <div class="dh-content">
            <span class="dh-label">Doanh thu hôm qua</span>
            <b class="dh-val">{{ vnd(s.yesterdayRevenue || 0) }}</b>
            <small class="muted">{{ s.yesterdayOrders || 0 }} đơn hàng</small>
          </div>
        </div>
        <div class="dh-card">
          <div class="dh-icon">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h2M18 9h2a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2h-2M4 5h16v6a7 7 0 0 1-14 0V5z"/><path d="M12 17v4M8 21h8"/></svg>
          </div>
          <div class="dh-content">
            <span class="dh-label">Kỷ lục ngày cao nhất</span>
            <b class="dh-val">{{ peakDay ? vnd(peakDay.revenue) : '0đ' }}</b>
            <small class="muted">{{ peakDay ? fmtD(peakDay.date) + ' (' + peakDay.orderCount + ' đơn)' : '—' }}</small>
          </div>
        </div>
        <div class="dh-card">
          <div class="dh-icon">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3v18h18M7 16l4-4 4 4 6-7"/></svg>
          </div>
          <div class="dh-content">
            <span class="dh-label">Bình quân / ngày</span>
            <b class="dh-val">{{ vnd(avgDailyRevenue) }}</b>
            <small class="muted">{{ dailyList.length }} ngày trong kỳ</small>
          </div>
        </div>
      </div>

      <!-- General Statistics Grid -->
      <div v-show="show.stats" class="grid stats">
        <div class="card"><div class="muted small">Doanh thu tích luỹ</div><div class="val price"><CountUp :value="s.revenue" :format="vnd" /></div></div>
        <div class="card"><div class="muted small">Tổng đơn hàng</div><div class="val"><CountUp :value="s.orders" /></div><div class="small warn">{{ s.pendingOrders }} chờ xác nhận</div></div>
        <div class="card"><div class="muted small">Khách hàng</div><div class="val"><CountUp :value="s.customers" /></div></div>
        <div class="card"><div class="muted small">Sản phẩm</div><div class="val">{{ s.products }}</div><div class="small warn">{{ s.lowStock }} sắp hết hàng (≤5)</div></div>
        <div class="card"><div class="muted small">Việc cần xử lý</div>
          <router-link to="/admin/returns" class="todo">Yêu cầu đổi trả ({{ s.returnsPending }})</router-link>
          <router-link to="/admin/tickets" class="todo">Phiếu hỗ trợ mở ({{ s.ticketsOpen }})</router-link>
          <router-link to="/admin/questions" class="todo">Câu hỏi chưa trả lời ({{ s.questionsOpen }})</router-link>
        </div>
      </div>

      <!-- Business Performance & Dual-Axis Combo Chart -->
      <div v-show="show.chart" class="card chart-box">
        <!-- Main Top Bar -->
        <div class="cb-header">
          <div>
            <div class="cb-title-row">
              <h3>Hiệu quả kinh doanh & Doanh thu theo ngày</h3>
              <div class="cb-mode-switch">
                <button :class="['mode-tab', { on: chartMode === 'bars' }]" @click="chartMode = 'bars'">
                  Biểu đồ kết hợp
                </button>
                <button :class="['mode-tab', { on: chartMode === 'table' }]" @click="chartMode = 'table'">
                  Bảng kê ({{ dailyList.length }})
                </button>
              </div>
            </div>
            <p class="cb-sub">{{ fmtD(from) }} – {{ fmtD(to) }} · chu kỳ {{ spanDays }} ngày</p>
          </div>

          <div class="cb-controls">
            <div class="seg seg-presets" role="group">
              <button v-for="p in PRESETS" :key="p.k" :class="{ on: preset === p.k }" @click="setPreset(p.k)">{{ p.l }}</button>
            </div>
            <div class="date-pickers">
              <label class="dt">Từ <input v-model="from" type="date" class="input" :max="to" @change="preset = ''; loadKpi()" /></label>
              <label class="dt">Đến <input v-model="to" type="date" class="input" :min="from" :max="today" @change="preset = ''; loadKpi()" /></label>
            </div>
          </div>
        </div>

        <!-- KPI Strip -->
        <div v-if="k" class="kpis">
          <div v-for="m in KPIS" :key="m.k" class="kpi">
            <small>{{ m.l }}</small>
            <b><CountUp :value="k.current[m.k]" :format="m.f" /></b>
            <span v-if="delta(m.k) !== null" :class="['dl', (m.inv ? -delta(m.k) : delta(m.k)) >= 0 ? 'up' : 'down']">
              {{ delta(m.k) >= 0 ? '▲' : '▼' }} {{ Math.abs(delta(m.k)) }}%
            </span>
            <span v-else class="dl muted">—</span>
          </div>
        </div>

        <!-- Mode 1: Dual-Axis Studio Combo Chart -->
        <div v-if="chartMode === 'bars'" class="combo-stage">
          <!-- Sub-toolbar: Legend Pills & Metric Switcher -->
                    <div class="stage-nav">
            <div class="nav-tools">
              <span class="tool-label">Kiểu nét sức mua:</span>
              <div class="seg seg-compact">
                <button :class="{ on: lineStyle === 'sharp' }" @click="lineStyle = 'sharp'">Góc nhọn</button>
                <button :class="{ on: lineStyle === 'stepped' }" @click="lineStyle = 'stepped'">Bậc thang 90°</button>
              </div>
              <span class="tool-label" style="margin-left:10px">Tiêu chí:</span>
              <div class="seg seg-compact">
                <button :class="{ on: powerMetric === 'orders' }" @click="powerMetric = 'orders'">Đơn hàng</button>
                <button :class="{ on: powerMetric === 'items' }" @click="powerMetric = 'items'">Sản phẩm</button>
              </div>
            </div>
          </div>

          <!-- SVG Canvas -->
          <div class="svg-wrapper" @mouseleave="hoveredIdx = null">
            <svg class="master-svg" viewBox="0 0 1000 320" preserveAspectRatio="none">
              <defs>
                <!-- Revenue Bar Gradient -->
                <linearGradient id="aresBarGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#ff781f" />
                  <stop offset="100%" stop-color="#e24a00" />
                </linearGradient>
                <!-- Line Area Smooth Gradient -->
                <linearGradient id="aresAreaGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.22" />
                  <stop offset="100%" stop-color="#3b82f6" stop-opacity="0.0" />
                </linearGradient>
                <!-- Hover Column Glow -->
                <linearGradient id="hoverColGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="rgba(249, 115, 22, 0.12)" />
                  <stop offset="100%" stop-color="rgba(249, 115, 22, 0.02)" />
                </linearGradient>
              </defs>

              <!-- Grid Horizontal Lines & Dual Y-Axis Labels -->
              <g class="chart-grid">
                <template v-for="(g, idx) in yTicks" :key="'grid-' + idx">
                  <line :x1="padL" :y1="g.y" :x2="1000 - padR" :y2="g.y" stroke="rgba(140, 140, 140, 0.18)" stroke-dasharray="4 4" />
                  <!-- Trục trái: Doanh thu -->
                  <text :x="padL - 12" :y="g.y + 4" text-anchor="end" class="axis-txt rev-txt">{{ g.revLabel }}</text>
                  <!-- Trục phải: Sức mua -->
                  <text :x="1000 - padR + 12" :y="g.y + 4" text-anchor="start" class="axis-txt power-txt">{{ g.powerLabel }}</text>
                </template>
              </g>

              <!-- Hover column highlight -->
              <rect
                v-if="hoveredIdx !== null && chartPoints[hoveredIdx]"
                :x="chartPoints[hoveredIdx].colLeft"
                :y="padT"
                :width="colWidth"
                :height="plotH"
                fill="url(#hoverColGrad)"
                rx="6"
              />

              <!-- Vertical Crosshair line -->
              <line
                v-if="hoveredIdx !== null && chartPoints[hoveredIdx]"
                :x1="chartPoints[hoveredIdx].centerX"
                :y1="padT"
                :x2="chartPoints[hoveredIdx].centerX"
                :y2="padT + plotH"
                stroke="#ff781f"
                stroke-dasharray="3 3"
                opacity="0.8"
                stroke-width="1.2"
              />

                            <!-- Revenue Bars (CỘT DOANH THU: 100% VUÔNG GÓC SẮC CẠNH) -->
              <g class="bars-group">
                <template v-for="(p, i) in chartPoints" :key="'bar-' + p.date">
                  <rect
                    :x="p.barX"
                    :y="p.barY"
                    :width="p.barW"
                    :height="p.barH"
                    rx="0"
                    ry="0"
                    fill="url(#aresBarGrad)"
                    class="bar-item"
                    :class="{ active: hoveredIdx === i }"
                  />
                </template>
              </g>

              <!-- Purchasing Power Angular Stroke (CÁC NÉT GÓC CẠNH SẮC BÉN) -->
              <path
                v-if="activeLinePathD"
                :d="activeLinePathD"
                fill="none"
                stroke="#1d4ed8"
                stroke-width="2.5"
                stroke-miterlimit="10"
                stroke-linejoin="miter"
                stroke-linecap="square"
              />

              <!-- Node Points (ĐIỂM NÚT GÓC CẠNH HÌNH THOI KIM CƯƠNG) -->
              <g class="dots-group">
                <template v-for="(p, i) in chartPoints" :key="'dot-' + p.date">
                  <polygon
                    :points="`${p.centerX},${p.lineY - (hoveredIdx === i ? 6 : 4)} ${p.centerX + (hoveredIdx === i ? 6 : 4)},${p.lineY} ${p.centerX},${p.lineY + (hoveredIdx === i ? 6 : 4)} ${p.centerX - (hoveredIdx === i ? 6 : 4)},${p.lineY}`"
                    fill="#ffffff"
                    stroke="#1d4ed8"
                    :stroke-width="hoveredIdx === i ? 2.5 : 1.8"
                    class="dot-item"
                  />
                </template>
              </g>

              <!-- X-Axis Dates (TRỤC NGÀY THÁNG) -->
              <g class="x-group">
                <template v-for="(p, i) in chartPoints" :key="'x-' + p.date">
                  <text
                    v-if="shouldShowDate(i)"
                    :x="p.centerX"
                    :y="padT + plotH + 24"
                    text-anchor="middle"
                    class="x-date-txt"
                    :class="{ highlighted: hoveredIdx === i }"
                  >
                    {{ p.date.slice(8) }}/{{ p.date.slice(5, 7) }}
                  </text>
                </template>
              </g>

              <!-- Invisible Hover Triggers -->
              <g class="hitbox-group">
                <rect
                  v-for="(p, i) in chartPoints"
                  :key="'hit-' + p.date"
                  :x="p.colLeft"
                  :y="0"
                  :width="colWidth"
                  height="320"
                  fill="transparent"
                  style="cursor:pointer"
                  @mouseenter="hoveredIdx = i"
                  @click="openDayDetail(p.raw)"
                />
              </g>
            </svg>

            <!-- Floating Studio Tooltip -->
            <div
              v-if="hoveredIdx !== null && chartPoints[hoveredIdx]"
              class="studio-tooltip"
              :style="tooltipStyle"
            >
              <div class="st-header">
                <b>{{ fmtD(chartPoints[hoveredIdx].date) }}</b>
                <span class="st-day">{{ getWeekday(chartPoints[hoveredIdx].date) }}</span>
              </div>
              <div class="st-body">
                <div class="st-row">
                  <span class="st-pill rev-bullet"></span>
                  <span class="st-label">Doanh thu:</span>
                  <b class="st-val price">{{ vnd(chartPoints[hoveredIdx].revenue) }}</b>
                </div>
                <div class="st-row">
                  <span class="st-pill power-bullet"></span>
                  <span class="st-label">Sức mua:</span>
                  <b class="st-val">{{ chartPoints[hoveredIdx].orderCount }} đơn hàng · {{ chartPoints[hoveredIdx].itemsSold }} sp</b>
                </div>
                <div class="st-row st-sub">
                  <span>Thành công: <strong style="color:var(--success)">{{ chartPoints[hoveredIdx].okCount }}</strong></span>
                  <span> · Huỷ: <strong :style="{ color: chartPoints[hoveredIdx].cancelledCount ? 'var(--danger)' : 'inherit' }">{{ chartPoints[hoveredIdx].cancelledCount }}</strong></span>
                </div>
              </div>
              <div class="st-footer">
                <small>Bấm vào cột để xem danh sách {{ chartPoints[hoveredIdx].orderCount }} đơn hàng →</small>
              </div>
            </div>
          </div>
        </div>

        <!-- Mode 2: Detailed Daily Revenue Breakdown Table -->
        <div v-else class="daily-table-wrap">
          <table class="table daily-table">
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Thứ</th>
                <th style="min-width:180px">Doanh thu</th>
                <th>Số đơn hàng</th>
                <th>Thành công</th>
                <th>Huỷ / Trả</th>
                <th>Sản phẩm bán</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dailyList" :key="d.date" :class="{ 'row-peak': peakDay && d.date === peakDay.date, 'row-today': d.date === today }">
                <td>
                  <b>{{ fmtD(d.date) }}</b>
                  <span v-if="d.date === today" class="badge badge-new" style="margin-left:6px">Hôm nay</span>
                  <span v-else-if="peakDay && d.date === peakDay.date" class="badge" style="background:var(--accent);color:#fff;margin-left:6px">Kỷ lục</span>
                </td>
                <td class="muted small">{{ getWeekday(d.date) }}</td>
                <td>
                  <div class="rev-cell">
                    <b class="price">{{ vnd(d.revenue) }}</b>
                    <div class="mini-bar">
                      <div :style="{ width: Math.round((d.revenue * 100) / (peakDay?.revenue || 1)) + '%' }" />
                    </div>
                  </div>
                </td>
                <td><b>{{ d.orderCount }}</b></td>
                <td><span class="ok-txt">{{ d.okCount }}</span></td>
                <td><span :class="d.cancelledCount ? 'err-txt' : 'muted'">{{ d.cancelledCount }}</span></td>
                <td>{{ d.itemsSold }} sp</td>
                <td>
                  <button class="btn btn-sm btn-outline" @click="openDayDetail(d)">
                    Chi tiết {{ d.orderCount }} đơn
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Top Selling & Low Stock Grid -->
      <div class="grid grid-2" style="margin-top:24px">
        <div v-show="show.top" class="card">
          <h3>Top sản phẩm bán chạy</h3>
          <div v-for="(p, i) in s.topProducts" :key="p.id" class="row tp">
            <span class="rank">{{ i + 1 }}</span>
            <img loading="lazy" decoding="async" :src="p.image" alt="" />
            <div style="flex:1;min-width:0">
              <b class="ell">{{ p.name }}</b>
              <div class="small muted">{{ p.qty }} đã bán</div>
            </div>
            <b class="price">{{ ty(p.revenue) }}</b>
          </div>
        </div>

        <div v-show="show.low" class="card">
          <div class="row between">
            <h3>Cảnh báo tồn kho thấp</h3>
            <router-link to="/admin/inventory" class="small">Nhập hàng →</router-link>
          </div>
          <div v-if="!s.lowStockItems.length" class="muted">Tồn kho ổn định.</div>
          <div v-for="p in s.lowStockItems" :key="p.id" class="row tp">
            <img loading="lazy" decoding="async" :src="p.images?.[0]" alt="" />
            <div style="flex:1;min-width:0">
              <b class="ell">{{ p.name }}</b>
              <div class="small muted">{{ p.sku }}</div>
            </div>
            <span :class="['badge', p.stock ? 'badge-PENDING' : 'badge-CANCELLED']">
              {{ p.stock ? 'Còn ' + p.stock : 'Hết hàng' }}
            </span>
          </div>
        </div>
      </div>

      <!-- Orders by Status & Recent Orders Grid -->
      <div class="grid grid-2" style="margin-top:24px">
        <div v-show="show.status" class="card">
          <h3>Đơn theo trạng thái</h3>
          <div v-for="(n, k) in s.ordersByStatus" :key="k" class="bar-row">
            <span>{{ ORDER_STATUS[k] }}</span>
            <div class="bar"><div :style="{ width: (s.orders ? n * 100 / s.orders : 0) + '%' }" /></div>
            <b>{{ n }}</b>
          </div>
        </div>

        <div v-show="show.recent" class="card">
          <h3>Đơn mới nhất</h3>
          <div v-if="!s.recentOrders.length" class="muted">Chưa có đơn.</div>
          <div v-for="o in s.recentOrders" :key="o.id" class="row between ro">
            <div>
              <b>{{ o.code }}</b>
              <div class="small muted">{{ o.buyerName }} · {{ dt(o.createdAt) }}</div>
            </div>
            <div style="text-align:right">
              <div class="price">{{ vnd(o.total) }}</div>
              <span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span>
            </div>
          </div>
          <router-link to="/admin/orders" class="small">Quản lý đơn hàng →</router-link>
        </div>
      </div>

      <!-- Modal Chi Tiết Đơn Hàng Của Ngày -->
      <div v-if="selectedDay" class="modal-bg" @click.self="selectedDay = null">
        <div class="modal day-modal">
          <div class="row between" style="border-bottom:1px solid var(--border);padding-bottom:12px;margin-bottom:16px">
            <div>
              <h3 style="margin:0">Chi tiết doanh thu ngày {{ fmtD(selectedDay.date) }}</h3>
              <span class="muted small">{{ getWeekday(selectedDay.date) }}</span>
            </div>
            <button class="btn btn-sm btn-outline" @click="selectedDay = null">Đóng</button>
          </div>

          <!-- Day Summary Stats -->
          <div class="grid grid-3 day-stats" style="margin-bottom:18px">
            <div class="d-box">
              <span class="muted small">Doanh thu ngày</span>
              <b class="price" style="font-size:1.3rem">{{ vnd(selectedDay.revenue) }}</b>
            </div>
            <div class="d-box">
              <span class="muted small">Số đơn thành công</span>
              <b style="font-size:1.3rem;color:var(--success)">{{ selectedDay.okCount || selectedDay.orderCount || 0 }} đơn</b>
            </div>
            <div class="d-box">
              <span class="muted small">Số đơn huỷ</span>
              <b style="font-size:1.3rem;color:var(--danger)">{{ selectedDay.cancelledCount || 0 }} đơn</b>
            </div>
          </div>

          <!-- List of Orders for that day -->
          <h4>Danh sách đơn hàng trong ngày</h4>
          <div v-if="!selectedDay.orders || !selectedDay.orders.length" class="empty">
            Không có đơn hàng nào phát sinh trong ngày này.
          </div>
          <div v-else class="table-wrap" style="max-height:360px;overflow-y:auto">
            <table class="table">
              <thead>
                <tr>
                  <th>Mã đơn</th>
                  <th>Khách hàng</th>
                  <th>Số điện thoại</th>
                  <th>Giờ đặt</th>
                  <th>Trạng thái</th>
                  <th style="text-align:right">Tổng tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in selectedDay.orders" :key="o.id">
                  <td>
                    <router-link :to="`/orders/${o.code}`" class="code-link" target="_blank">
                      <b>{{ o.code }}</b>
                    </router-link>
                  </td>
                  <td>{{ o.buyerName || 'Khách vãng lai' }}</td>
                  <td class="small">{{ o.phone || '—' }}</td>
                  <td class="small muted">{{ o.createdAt.slice(11, 16) }}</td>
                  <td><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] || o.status }}</span></td>
                  <td style="text-align:right"><b class="price">{{ vnd(o.total) }}</b></td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="row between" style="margin-top:18px;border-top:1px solid var(--border);padding-top:14px">
            <router-link to="/admin/orders" class="btn btn-sm btn-outline">Xem tất cả tại Quản lý đơn hàng →</router-link>
            <button class="btn btn-sm" @click="selectedDay = null">Đóng cửa sổ</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed, onMounted } from 'vue'
import api from '../../api'
import CountUp from '../../components/CountUp.vue'
import { vnd, dt, ty, ORDER_STATUS } from '../../format'

const WIDGETS = { stats: 'Chỉ số tổng', chart: 'Biểu đồ hiệu quả', top: 'Top bán chạy', low: 'Tồn kho thấp', status: 'Đơn theo trạng thái', recent: 'Đơn mới nhất' }
const show = reactive(Object.fromEntries(Object.keys(WIDGETS).map(k => [k, true])))
let wReady = false, wT
api.get('/account/prefs').then(r => { if (r.data.dashWidgets) Object.assign(show, r.data.dashWidgets) }).finally(() => { wReady = true })
watch(show, v => { if (!wReady) return; clearTimeout(wT); wT = setTimeout(() => api.put('/account/prefs/dashWidgets', { ...v }).catch(() => {}), 400) })

const s = ref(null), k = ref(null)
const chartMode = ref('bars') // 'bars' | 'table'
const selectedDay = ref(null)

const iso = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const today = iso(new Date()), daysAgo = n => iso(new Date(Date.now() - n * 864e5))
const PRESETS = [{ k: '7', l: '7 ngày' }, { k: '14', l: '14 ngày' }, { k: '30', l: '30 ngày' }, { k: 'month', l: 'Tháng này' }]
const preset = ref('14'), from = ref(daysAgo(13)), to = ref(today)
const setPreset = p => { preset.value = p; to.value = today; from.value = p === 'month' ? today.slice(0, 8) + '01' : daysAgo(+p - 1); loadKpi() }

const KPIS = [
  { k: 'revenue', l: 'Doanh thu', f: v => vnd(v) },
  { k: 'orders', l: 'Đơn hàng' },
  { k: 'aov', l: 'Giá trị TB/đơn', f: v => vnd(v) },
  { k: 'items', l: 'Sản phẩm bán ra' },
  { k: 'newCustomers', l: 'Khách mới' },
  { k: 'cancelRate', l: 'Tỉ lệ huỷ', f: v => v.toFixed?.(1) + '%', inv: true }
]

const delta = key => { const c = k.value?.current[key], p = k.value?.previous[key]; if (!p) return c ? 100 : null; return Math.round((c - p) * 100 / p) }
const spanDays = computed(() => Math.round((new Date(to.value) - new Date(from.value)) / 864e5) + 1)
const fmtD = s => s ? s.split('-').reverse().join('/') : ''

const WEEKDAYS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7']
function getWeekday(dateStr) {
  if (!dateStr) return ''
  const dt = new Date(dateStr)
  return WEEKDAYS[dt.getDay()]
}

// Chart parameters
const powerMetric = ref('orders') // 'orders' | 'items'
const hoveredIdx = ref(null)

const padL = 75
const padR = 75
const padT = 36
const padB = 44
const plotW = 1000 - padL - padR // 850
const plotH = 320 - padT - padB // 240

const chartData = computed(() => {
  if (!k.value) return []
  if (k.value.dailyDetails && k.value.dailyDetails.length) {
    return k.value.dailyDetails
  }
  return Object.entries(k.value.daily || {}).map(([date, revenue]) => ({
    date,
    revenue,
    orderCount: revenue ? 1 : 0,
    okCount: revenue ? 1 : 0,
    cancelledCount: 0,
    itemsSold: revenue ? 1 : 0,
    orders: []
  }))
})

const totalPeriodRev = computed(() => chartData.value.reduce((acc, d) => acc + (d.revenue || 0), 0))
const totalPeriodPower = computed(() => chartData.value.reduce((acc, d) => acc + (powerMetric.value === 'orders' ? (d.orderCount || 0) : (d.itemsSold || 0)), 0))

const maxRev = computed(() => {
  const max = Math.max(0, ...chartData.value.map(d => d.revenue || 0))
  if (max === 0) return 1000000
  const mag = Math.pow(10, Math.floor(Math.log10(max)))
  const norm = max / mag
  let niceNorm = 10
  if (norm <= 1) niceNorm = 1
  else if (norm <= 1.5) niceNorm = 1.5
  else if (norm <= 2) niceNorm = 2
  else if (norm <= 3) niceNorm = 3
  else if (norm <= 5) niceNorm = 5
  else if (norm <= 7.5) niceNorm = 7.5
  return Math.ceil(niceNorm * mag)
})

const maxPower = computed(() => {
  const vals = chartData.value.map(d => powerMetric.value === 'orders' ? (d.orderCount || 0) : (d.itemsSold || 0))
  const max = Math.max(0, ...vals)
  if (max === 0) return 5
  if (max <= 5) return 5
  if (max <= 10) return 10
  if (max <= 15) return 15
  if (max <= 20) return 20
  if (max <= 30) return 30
  if (max <= 50) return 50
  return Math.ceil(max * 1.2)
})

const yTicks = computed(() => {
  const ticks = []
  const steps = 4
  for (let i = 0; i <= steps; i++) {
    const ratio = i / steps
    const y = padT + plotH - (ratio * plotH)
    const revVal = Math.round(maxRev.value * ratio)
    const powerVal = Math.round(maxPower.value * ratio)
    ticks.push({
      y,
      revVal,
      revLabel: revVal === 0 ? '0đ' : ty(revVal),
      powerVal,
      powerLabel: powerVal + (powerMetric.value === 'orders' ? ' đơn' : ' sp')
    })
  }
  return ticks
})

const colWidth = computed(() => {
  const n = chartData.value.length
  return n > 0 ? plotW / n : plotW
})

const chartPoints = computed(() => {
  const n = chartData.value.length
  if (!n) return []
  const cw = colWidth.value
  return chartData.value.map((d, i) => {
    const colLeft = padL + i * cw
    const centerX = colLeft + cw / 2
    const barW = Math.max(12, Math.min(26, cw * 0.50))
    const barX = centerX - barW / 2
    const revRatio = Math.min(1, Math.max(0, (d.revenue || 0) / maxRev.value))
    const barH = d.revenue > 0 ? Math.max(4, revRatio * plotH) : 2
    const barY = padT + plotH - barH

    const powerVal = powerMetric.value === 'orders' ? (d.orderCount || 0) : (d.itemsSold || 0)
    const powerRatio = Math.min(1, Math.max(0, powerVal / maxPower.value))
    const lineY = padT + plotH - (powerRatio * plotH)

    return {
      date: d.date,
      revenue: d.revenue || 0,
      orderCount: d.orderCount || 0,
      okCount: d.okCount || 0,
      cancelledCount: d.cancelledCount || 0,
      itemsSold: d.itemsSold || 0,
      powerVal,
      colLeft,
      centerX,
      barW,
      barX,
      barY,
      barH,
      lineY,
      raw: d
    }
  })
})

const lineStyle = ref('sharp') // 'sharp' (Góc nhọn dứt khoát) | 'stepped' (Bậc thang 90 độ)

// Đường nét thẳng góc nhọn (Linear sharp segments)
const sharpPathD = computed(() => {
  const pts = chartPoints.value
  if (!pts || !pts.length) return ''
  return pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.centerX.toFixed(1)} ${p.lineY.toFixed(1)}`).join(' ')
})

// Đường nét bậc thang vuông góc 90 độ (Stepped 90° line)
const steppedPathD = computed(() => {
  const pts = chartPoints.value
  if (!pts || !pts.length) return ''
  let d = `M ${pts[0].centerX.toFixed(1)} ${pts[0].lineY.toFixed(1)}`
  for (let i = 0; i < pts.length - 1; i++) {
    const cur = pts[i]
    const next = pts[i + 1]
    const midX = ((cur.centerX + next.centerX) / 2).toFixed(1)
    d += ` L ${midX} ${cur.lineY.toFixed(1)} L ${midX} ${next.lineY.toFixed(1)} L ${next.centerX.toFixed(1)} ${next.lineY.toFixed(1)}`
  }
  return d
})

const activeLinePathD = computed(() => lineStyle.value === 'stepped' ? steppedPathD.value : sharpPathD.value)

function shouldShowDate(idx) {
  const n = chartPoints.value.length
  if (n <= 16) return true
  if (n <= 31) return idx === 0 || idx === n - 1 || idx % 2 === 0
  return idx === 0 || idx === n - 1 || idx % Math.ceil(n / 10) === 0
}

const tooltipStyle = computed(() => {
  if (hoveredIdx.value === null || !chartPoints.value[hoveredIdx.value]) return { display: 'none' }
  const pt = chartPoints.value[hoveredIdx.value]
  const pct = (pt.centerX / 1000) * 100
  if (pct > 62) {
    return {
      right: `calc(${100 - pct}% + 28px)`,
      left: 'auto',
      top: '12px'
    }
  }
  return {
    left: `calc(${pct}% + 28px)`,
    right: 'auto',
    top: '12px'
  }
})

const dailyList = computed(() => {
  if (!k.value) return []
  if (k.value.dailyDetails && k.value.dailyDetails.length) {
    return [...k.value.dailyDetails].reverse()
  }
  return Object.entries(k.value.daily || {}).map(([date, revenue]) => ({
    date,
    revenue,
    orderCount: revenue ? 1 : 0,
    okCount: revenue ? 1 : 0,
    cancelledCount: 0,
    itemsSold: revenue ? 1 : 0,
    orders: []
  })).reverse()
})

const peakDay = computed(() => {
  if (!dailyList.value.length) return null
  let max = dailyList.value[0]
  for (const d of dailyList.value) {
    if (d.revenue > max.revenue) max = d
  }
  return max.revenue > 0 ? max : null
})

const avgDailyRevenue = computed(() => {
  if (!dailyList.value.length) return 0
  const total = dailyList.value.reduce((sum, d) => sum + (d.revenue || 0), 0)
  return Math.round(total / dailyList.value.length)
})

function openDayDetail(item) {
  const dStr = item.date || item
  if (k.value?.dailyDetails) {
    const found = k.value.dailyDetails.find(x => x.date === dStr)
    selectedDay.value = found || { date: dStr, revenue: item.revenue || 0, orderCount: 0, orders: [] }
  } else {
    selectedDay.value = { date: dStr, revenue: item.revenue || 0, orderCount: 0, orders: [] }
  }
}

const loadKpi = async () => {
  if (from.value > to.value) return
  k.value = (await api.get('/admin/kpi', { params: { from: from.value, to: to.value } })).data
}

onMounted(async () => {
  s.value = (await api.get('/admin/stats')).data
  loadKpi()
})
</script>

<style scoped>
.dh { gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.wt { position: relative; }
.wt summary { list-style: none; }
.wt summary::-webkit-details-marker { display: none; }
.wtp { position: absolute; right: 0; top: 110%; z-index: 20; display: flex; flex-direction: column; gap: 6px; min-width: 210px; box-shadow: 0 8px 24px rgba(0,0,0,.12); }
.wtp label { display: flex; gap: 6px; align-items: center; }

/* Daily Highlights Strip */
.daily-highlights-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}
@media (max-width: 900px) { .daily-highlights-strip { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 500px) { .daily-highlights-strip { grid-template-columns: 1fr; } }

.dh-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  box-shadow: var(--shadow);
}
.dh-icon {
  color: var(--accent);
  background: var(--bg-2);
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.dh-content { display: flex; flex-direction: column; min-width: 0; }
.dh-label { font-size: .8rem; color: var(--muted); }
.dh-val { font-size: 1.35rem; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.stats { grid-template-columns: repeat(5, 1fr); }
@media (max-width: 1100px) { .stats { grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); } }
.val { font-size: 1.7rem; font-weight: 800; margin: 4px 0; }
.warn { color: #f5b800; }
.bar-row { display: grid; grid-template-columns: 120px 1fr 30px; gap: 10px; align-items: center; margin-bottom: 10px; font-size: .9rem; }
.bar { background: rgba(0,0,0,.06); border-radius: 6px; height: 10px; overflow: hidden; }
.bar div { background: var(--primary); height: 100%; }
.todo { display: block; font-size: .85rem; margin-top: 6px; color: var(--text-2); }
.todo:hover { color: var(--accent); }
.tp { gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border); flex-wrap: nowrap; }
.tp img { width: 44px; height: 44px; object-fit: cover; border-radius: 8px; }
.rank { width: 22px; font-weight: 800; color: var(--accent); }
.ell { display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ro { padding: 8px 0; border-bottom: 1px solid var(--border); }

/* Chart Container & Header */
.chart-box {
  margin-top: 24px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px 24px 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,.04);
}
.cb-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.cb-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.cb-title-row h3 { margin: 0; font-size: 1.22rem; }
.cb-sub { margin: 4px 0 0; color: var(--muted); font-size: .82rem; }

.cb-mode-switch {
  display: inline-flex;
  background: var(--bg-2);
  border-radius: 8px;
  padding: 2px;
  border: 1px solid var(--border);
}
.mode-tab {
  border: none;
  background: none;
  font: 600 .78rem inherit;
  color: var(--muted);
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all .15s ease;
}
.mode-tab.on {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 2px 6px rgba(249,115,22,.35);
}

.cb-controls {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.seg { display: flex; background: rgba(0,0,0,.05); border-radius: 999px; padding: 3px; }
.seg button { border: none; background: none; color: var(--muted); padding: 5px 12px; border-radius: 999px; cursor: pointer; font: 600 .78rem inherit; transition: all .15s ease; }
.seg button.on { background: var(--accent); color: #fff; }
.date-pickers { display: flex; gap: 8px; align-items: center; }
.dt { display: flex; gap: 5px; align-items: center; margin: 0; font-size: .82rem; }
.dt .input { width: auto; padding: 5px 8px; min-height: 0; font-size: .82rem; }

/* KPIs Strip */
.kpis {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0;
  margin-bottom: 20px;
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
}
.kpi { padding: 12px 14px; display: flex; flex-direction: column; gap: 2px; min-width: 0; background: var(--bg-1); }
.kpi + .kpi { border-left: 1px solid var(--border); }
.kpi small { color: var(--muted); font-size: .76rem; }
.kpi b { font-size: clamp(1rem, .8rem + .8vw, 1.35rem); font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dl { font-size: .74rem; font-weight: 700; }
.dl.up { color: var(--success); }
.dl.down { color: var(--danger); }

/* Combo Stage */
.combo-stage {
  background: var(--bg-1);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px 20px 22px;
}
.stage-nav {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px solid var(--border);
}
.nav-legends {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}
.nav-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: .84rem;
  font-weight: 500;
}
.pill-rev { color: #f97316; }
.pill-rev b { font-size: .95rem; }
.pill-power { color: #2563eb; }
.pill-power b { font-size: .95rem; }

.pill-bar {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  background: linear-gradient(180deg, #ff781f, #e24a00);
  display: inline-block;
}
.pill-line {
  width: 22px;
  height: 3px;
  background: #2563eb;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  border-radius: 2px;
}
.pill-line i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ffffff;
  border: 2px solid #2563eb;
  position: absolute;
}

.nav-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tool-label { font-size: .8rem; color: var(--muted); }
.seg-compact button { padding: 4px 10px; font-size: .76rem; }

/* SVG Canvas */
.svg-wrapper {
  position: relative;
  width: 100%;
  overflow: visible;
}
.master-svg {
  width: 100%;
  height: 310px;
  display: block;
}

/* SVG Elements */
.axis-txt {
  font-size: 11px;
  font-family: inherit;
  user-select: none;
}
.rev-txt {
  fill: var(--muted);
  font-weight: 500;
}
.power-txt {
  fill: #2563eb;
  font-weight: 600;
}
.x-date-txt {
  fill: var(--muted);
  font-size: 11px;
  user-select: none;
  transition: fill .15s ease;
}
.x-date-txt.highlighted {
  fill: var(--accent);
  font-weight: 700;
}
.bar-item {
  transition: opacity .15s, filter .15s;
}
.bar-item.active {
  filter: brightness(1.2) drop-shadow(0 4px 8px rgba(249, 115, 22, 0.4));
}
.dot-item {
  transition: r .15s, stroke-width .15s;
}

/* Floating Studio Tooltip */
.studio-tooltip {
  position: absolute;
  z-index: 25;
  background: rgba(18, 18, 22, 0.96);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 10px;
  padding: 12px 16px;
  box-shadow: 0 16px 36px rgba(0,0,0,0.45);
  pointer-events: none;
  min-width: 235px;
  backdrop-filter: blur(10px);
  animation: toolFade .15s ease-out;
}
@keyframes toolFade { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }

.st-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  padding-bottom: 6px;
  margin-bottom: 8px;
}
.st-header b { font-size: .95rem; color: #fff; }
.st-day { font-size: .78rem; color: #a1a1aa; }
.st-body { display: flex; flex-direction: column; gap: 5px; }
.st-row { display: flex; align-items: center; gap: 7px; font-size: .84rem; }
.st-pill { width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0; }
.rev-bullet { background: #ff781f; }
.power-bullet { background: #2563eb; border-radius: 50%; }
.st-label { color: #a1a1aa; flex: 1; }
.st-val { font-weight: 700; color: #fff; }
.st-sub { margin-top: 4px; padding-top: 6px; border-top: 1px dashed rgba(255, 255, 255, 0.12); font-size: .78rem; color: #a1a1aa; }
.st-footer { margin-top: 8px; font-size: .75rem; color: #ff781f; }

/* Table View */
.daily-table-wrap { margin-top: 20px; overflow-x: auto; }
.daily-table th { background: var(--bg-2); font-size: .82rem; }
.daily-table td { padding: 10px 12px; font-size: .88rem; vertical-align: middle; }
.row-peak { background: rgba(249, 115, 22, 0.05); }
.row-today { background: rgba(59, 130, 246, 0.05); }
.rev-cell { display: flex; flex-direction: column; gap: 4px; }
.mini-bar { width: 100%; height: 6px; background: rgba(0,0,0,.08); border-radius: 4px; overflow: hidden; }
.mini-bar div { height: 100%; background: var(--accent); }
.ok-txt { color: var(--success); font-weight: 700; }
.err-txt { color: var(--danger); font-weight: 700; }

/* Day Detail Modal */
.day-modal { max-width: 780px !important; width: 92%; }
.day-stats .d-box { background: var(--bg-2); padding: 12px 16px; border-radius: 8px; border: 1px solid var(--border); display: flex; flex-direction: column; gap: 4px; }
.code-link { color: var(--accent); text-decoration: none; font-family: monospace; }
.code-link:hover { text-decoration: underline; }

@media (max-width: 1100px) {
  .kpis { grid-template-columns: repeat(3, 1fr); }
  .kpi:nth-child(4) { border-left: none; }
  .kpi:nth-child(n+4) { border-top: 1px solid var(--border); }
}
@media (max-width: 599px) {
  .kpis { grid-template-columns: repeat(2, 1fr); }
  .kpi { border-left: none !important; border-top: 1px solid var(--border); }
  .kpi:nth-child(even) { border-left: 1px solid var(--border) !important; }
  .bar-row { grid-template-columns: 90px 1fr 26px; }
  .master-svg { height: 260px; }
}
</style>
