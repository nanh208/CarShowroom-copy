<template>
  <div class="admin-staff">
    <!-- Header -->
    <div class="row between hd">
      <div>
        <h1>Quản lý Nhân sự & Nhân viên</h1>
        <p class="muted small">Danh sách tài khoản nhân viên, vai trò hệ thống, trạng thái tài khoản và theo dõi công nợ nội bộ.</p>
      </div>
      <div class="row header-actions">
        <router-link to="/admin/staff-debts" class="btn btn-outline">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
          Sổ công nợ nhân viên
        </router-link>
        <router-link to="/admin/permissions" class="btn btn-outline">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2 4 5v6c0 5 3.5 9 8 11 4.5-2 8-6 8-11V5z"/><path d="m9 12 2 2 4-4"/></svg>
          Phân quyền
        </router-link>
        <button class="btn btn-primary" @click="openCreateModal">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Thêm nhân viên mới
        </button>
      </div>
    </div>

    <!-- Tabs: Nhân sự vs Hoa hồng -->
    <div class="staff-tabs-strip">
      <button :class="['staff-tab-btn', { active: activeTab === 'staff' }]" @click="activeTab = 'staff'">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        Danh sách nhân sự & Phân công
      </button>
      <button :class="['staff-tab-btn', { active: activeTab === 'commission' }]" @click="activeTab = 'commission'">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8"/><path d="M12 6v2m0 8v2"/></svg>
        Thưởng hoa hồng & Hiệu suất bán hàng
      </button>
    </div>

    <!-- TAB 1: NHÂN SỰ & PHÂN CÔNG -->
    <div v-show="activeTab === 'staff'">
    <!-- KPI Summary Grid -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <span class="kpi-lbl">Tổng nhân sự</span>
        <b class="kpi-val">{{ staffList.length }}</b>
        <small class="muted">Tài khoản quản trị & nhân viên</small>
      </div>
      <div class="kpi-card">
        <span class="kpi-lbl">Quản trị viên (ADMIN)</span>
        <b class="kpi-val highlight-val">{{ adminCount }}</b>
        <small class="muted">Toàn quyền hệ thống</small>
      </div>
      <div class="kpi-card">
        <span class="kpi-lbl">Nhân viên (STAFF)</span>
        <b class="kpi-val">{{ staffCount }}</b>
        <small class="muted">{{ activeStaffCount }} đang hoạt động</small>
      </div>
      <div class="kpi-card">
        <span class="kpi-lbl">Tài khoản tạm khoá</span>
        <b class="kpi-val" :class="{ 'danger-val': lockedCount > 0 }">{{ lockedCount }}</b>
        <small class="muted">Bị vô hiệu hoá đăng nhập</small>
      </div>
      <div class="kpi-card highlight">
        <span class="kpi-lbl">Tổng dư nợ nội bộ</span>
        <b class="kpi-val danger-val">{{ vnd(totalRemainingDebt) }}</b>
        <small class="muted">{{ staffWithDebtCount }} nhân viên còn dư nợ</small>
      </div>
    </div>

    <!-- Search & Filter Bar -->
    <div class="card filter-bar">
      <div class="row filter-row">
        <div class="search-box">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input
            v-model.trim="q"
            class="input"
            placeholder="Tìm theo họ tên, email, số điện thoại, vị trí..."
          />
        </div>
        <select v-model="filterRole" class="input filter-select">
          <option value="ALL">Tất cả vai trò</option>
          <option value="ADMIN">Quản trị viên (ADMIN)</option>
          <option value="STAFF">Nhân viên (STAFF)</option>
        </select>
        <select v-model="filterStatus" class="input filter-select">
          <option value="ALL">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang hoạt động</option>
          <option value="LOCKED">Đã tạm khoá</option>
        </select>
        <select v-model="filterDebt" class="input filter-select">
          <option value="ALL">Tất cả công nợ</option>
          <option value="HAS_DEBT">Đang có dư nợ (> 0đ)</option>
          <option value="NO_DEBT">Không có dư nợ (0đ)</option>
        </select>
        <button class="btn btn-outline" @click="loadStaff">Làm mới</button>
      </div>
    </div>

    <!-- Staff Table -->
    <div class="card" style="margin-top:16px">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <span>Đang tải danh sách nhân sự...</span>
      </div>

      <div v-else-if="!filteredStaff.length" class="empty-state">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        <p>Không tìm thấy nhân viên nào phù hợp bộ lọc.</p>
        <button v-if="q || filterRole !== 'ALL' || filterStatus !== 'ALL' || filterDebt !== 'ALL'" class="btn btn-outline" @click="resetFilters">Xoá bộ lọc</button>
      </div>

      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th style="width:70px">ID</th>
              <th>Nhân viên</th>
              <th style="width:130px">Điện thoại</th>
              <th style="width:140px">Vai trò</th>
              <th style="width:130px">Trạng thái</th>
              <th style="width:170px">Dư nợ công nợ</th>
              <th>Bộ phận / Ghi chú</th>
              <th style="text-align:right;width:220px">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in filteredStaff" :key="s.id" :class="{ 'row-locked': s.locked }">
              <td><b>#{{ s.id }}</b></td>
              <td>
                <div class="user-cell">
                  <div class="avatar-wrap">
                    <img v-if="s.avatarUrl" :src="s.avatarUrl" :alt="s.fullName" class="avatar-img" />
                    <div v-else class="avatar-ph">{{ getInitials(s.fullName || s.email) }}</div>
                  </div>
                  <div>
                    <div class="user-name">
                      <b>{{ s.fullName || 'Chưa đặt tên' }}</b>
                      <span v-if="s.id === currentUser?.id" class="badge-me">Bạn</span>
                    </div>
                    <div class="user-email muted">{{ s.email }}</div>
                    <div class="user-date muted">Tham gia: {{ formatDate(s.createdAt) }}</div>
                  </div>
                </div>
              </td>
              <td>
                <span v-if="s.phone" class="phone-text">{{ s.phone }}</span>
                <span v-else class="muted italic">Chưa cập nhật</span>
              </td>
              <td>
                <span :class="['role-badge', s.role === 'ADMIN' ? 'role-admin' : 'role-staff']">
                  {{ s.role === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên' }}
                </span>
              </td>
              <td>
                <span :class="['status-badge', s.locked ? 'status-locked' : 'status-active']">
                  {{ s.locked ? 'Đã tạm khoá' : 'Đang làm việc' }}
                </span>
              </td>
              <td>
                <div v-if="s.remainingDebt > 0" class="debt-box">
                  <b class="debt-amount">{{ vnd(s.remainingDebt) }}</b>
                  <router-link :to="`/admin/staff-debts?staffId=${s.id}`" class="debt-link">
                    {{ s.debtCount }} phiếu nợ &rarr;
                  </router-link>
                </div>
                <div v-else class="muted small">
                  0 đ (Không nợ)
                </div>
              </td>
              <td>
                <span v-if="s.staffNote" class="note-text">{{ s.staffNote }}</span>
                <span v-else class="muted italic">--</span>
              </td>
              <td style="text-align:right">
                <div class="row actions-row" style="justify-content:flex-end">
                  <button class="btn btn-sm btn-outline" title="Cấp thẻ & Đổi hạng VIP" @click="openVipModal(s)">
                    Thẻ VIP
                  </button>
                  <button class="btn btn-sm btn-outline" title="Chỉnh sửa thông tin" @click="openEditModal(s)">
                    Sửa
                  </button>
                  <button
                    v-if="s.id !== currentUser?.id"
                    :class="['btn btn-sm', s.locked ? 'btn-unlock' : 'btn-lock']"
                    :title="s.locked ? 'Mở khoá tài khoản' : 'Tạm khoá tài khoản'"
                    @click="toggleLock(s)"
                  >
                    {{ s.locked ? 'Mở khoá' : 'Khoá' }}
                  </button>
                  <button
                    class="btn btn-sm btn-outline"
                    title="Tạo phiếu nợ cho nhân viên này"
                    @click="openDebtModal(s)"
                  >
                    + Nợ
                  </button>
                  <button
                    v-if="s.id !== currentUser?.id && s.remainingDebt === 0"
                    class="btn btn-sm btn-danger-ghost"
                    title="Xoá tài khoản nhân viên"
                    @click="confirmDelete(s)"
                  >
                    Xoá
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    </div>
    <!-- END TAB 1 -->

    <!-- TAB 2: THƯỞNG HOA HỒNG & HIỆU SUẤT BÁN HÀNG -->
    <div v-show="activeTab === 'commission'">
      <!-- Commission KPIs -->
      <div class="kpi-grid">
        <div class="kpi-card highlight">
          <span class="kpi-lbl">Tổng hoa hồng tích luỹ</span>
          <b class="kpi-val highlight-val">{{ vnd(totalEarnedCommission) }}</b>
          <small class="muted">Toàn bộ nhân viên từ trước đến nay</small>
        </div>
        <div class="kpi-card">
          <span class="kpi-lbl">Đã quyết toán / chi trả</span>
          <b class="kpi-val">{{ vnd(totalPaidCommission) }}</b>
          <small class="muted">Hoa hồng đã giải ngân cho nhân viên</small>
        </div>
        <div class="kpi-card highlight">
          <span class="kpi-lbl">Chờ quyết toán (còn nợ NV)</span>
          <b class="kpi-val danger-val">{{ vnd(totalPendingCommission) }}</b>
          <small class="muted">{{ staffWithPendingCommissionCount }} nhân viên có hoa hồng chưa nhận</small>
        </div>
        <div class="kpi-card">
          <span class="kpi-lbl">Tổng doanh thu nhân viên tạo ra</span>
          <b class="kpi-val price">{{ vnd(totalStaffSales) }}</b>
          <small class="muted">{{ totalStaffOrders }} đơn hàng thành công</small>
        </div>
      </div>

      <!-- Commission Table -->
      <div class="card" style="margin-top:16px">
        <div class="row between" style="margin-bottom:12px;padding-bottom:10px;border-bottom:1px solid var(--border)">
          <div>
            <h3>Bảng theo dõi hoa hồng & Doanh số theo nhân viên</h3>
            <p class="muted small">Tự động tính hoa hồng theo tỷ lệ % từng đơn hàng tại quầy POS và đơn online phụ trách.</p>
          </div>
          <button class="btn btn-sm btn-outline" @click="loadStaff">Làm mới dữ liệu</button>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Mã & Nhân viên</th>
                <th>Bộ phận</th>
                <th style="text-align:center">Tỷ lệ HH</th>
                <th style="text-align:right">Doanh số POS quầy</th>
                <th style="text-align:right">Doanh số Online</th>
                <th style="text-align:right">Tổng doanh số</th>
                <th style="text-align:right">Hoa hồng tích lũy</th>
                <th style="text-align:right">Đã thanh toán</th>
                <th style="text-align:right">Còn lại phải chi</th>
                <th style="text-align:right">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in staffList" :key="s.id">
                <td>
                  <b>{{ s.staffCode || ('NV-' + s.id) }}</b> - {{ s.fullName }}
                  <div class="small muted">{{ s.email }}</div>
                </td>
                <td>{{ s.department || 'Bán hàng' }}</td>
                <td style="text-align:center"><span class="badge badge-accent">{{ s.commissionRate || 2 }}%</span></td>
                <td style="text-align:right;font-weight:600">{{ vnd(s.posSales || 0) }}</td>
                <td style="text-align:right">{{ vnd(s.onlineSales || 0) }}</td>
                <td style="text-align:right;font-weight:700">{{ vnd(s.totalSales || 0) }}</td>
                <td style="text-align:right">{{ vnd(s.earnedCommission || 0) }}</td>
                <td style="text-align:right">{{ vnd(s.paidCommission || 0) }}</td>
                <td style="text-align:right">
                  <b :class="['commission-amount', (s.remainingCommission || 0) > 0 ? 'text-accent' : 'muted']">
                    {{ vnd(s.remainingCommission || 0) }}
                  </b>
                </td>
                <td style="text-align:right">
                  <div class="row flow" style="justify-content:flex-end">
                    <button
                      class="btn btn-sm btn-primary"
                      :disabled="(s.remainingCommission || 0) <= 0"
                      @click="openPayoutModal(s)"
                    >
                      Chi trả
                    </button>
                    <button class="btn btn-sm btn-outline" @click="openCommissionHistoryModal(s)">
                      Lịch sử
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!staffList.length">
                <td colspan="10" class="muted" style="text-align:center;padding:24px">Không có nhân viên nào.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <!-- END TAB 2 -->

    <!-- Modal Quyết toán hoa hồng nhân viên -->
    <div v-if="showPayoutModal" class="modal-backdrop" @click.self="showPayoutModal = false">
      <div class="modal card">
        <div class="row between modal-hd">
          <h3>Quyết toán hoa hồng cho {{ payoutTargetStaff?.fullName }}</h3>
          <button class="btn-close" @click="showPayoutModal = false">&times;</button>
        </div>
        <form @submit.prevent="submitCommissionPayout" class="modal-form">
          <div class="payout-summary-box">
            <div class="summary-line"><span>Mã NV:</span><b>{{ payoutTargetStaff?.staffCode }}</b></div>
            <div class="summary-line"><span>Tổng hoa hồng tích lũy:</span><b>{{ vnd(payoutTargetStaff?.earnedCommission || 0) }}</b></div>
            <div class="summary-line"><span>Đã chi trả trước đó:</span><b>{{ vnd(payoutTargetStaff?.paidCommission || 0) }}</b></div>
            <div class="summary-line grand-total-line">
              <span>Hoa hồng còn lại chờ chi:</span>
              <b class="price text-accent">{{ vnd(payoutTargetStaff?.remainingCommission || 0) }}</b>
            </div>
          </div>

          <div class="form-group" style="margin-top:14px">
            <label>Số tiền quyết toán đợt này (VNĐ) <span class="req">*</span></label>
            <input
              v-model.number="payoutForm.amount"
              type="number"
              min="1000"
              :max="payoutTargetStaff?.remainingCommission || 100000000"
              class="input"
              required
            />
            <div class="quick-cash-row" style="margin-top:6px">
              <button type="button" class="quick-cash-btn" @click="payoutForm.amount = payoutTargetStaff?.remainingCommission">Toàn bộ còn lại</button>
              <button type="button" class="quick-cash-btn" @click="payoutForm.amount = 1000000">1.000.000đ</button>
              <button type="button" class="quick-cash-btn" @click="payoutForm.amount = 2000000">2.000.000đ</button>
            </div>
          </div>

          <div class="form-group">
            <label>Ghi chú / Chứng từ chi <span class="req">*</span></label>
            <input v-model="payoutForm.note" class="input" placeholder="Ví dụ: Quyết toán hoa hồng bán xe tháng này..." required />
          </div>

          <div v-if="payoutError" class="form-error">{{ payoutError }}</div>

          <div class="row modal-actions" style="justify-content:flex-end;margin-top:20px;gap:10px">
            <button type="button" class="btn btn-outline" @click="showPayoutModal = false">Huỷ</button>
            <button type="submit" class="btn btn-primary" :disabled="submittingPayout">
              {{ submittingPayout ? 'Đang lưu...' : 'Xác nhận chi trả hoa hồng' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal Lịch sử hoa hồng nhân viên -->
    <div v-if="showHistoryModal" class="modal-backdrop" @click.self="showHistoryModal = false">
      <div class="modal card modal-large">
        <div class="row between modal-hd">
          <h3>Lịch sử hoa hồng: {{ historyTargetStaff?.fullName }}</h3>
          <button class="btn-close" @click="showHistoryModal = false">&times;</button>
        </div>
        <div class="table-wrap" style="max-height:380px;overflow-y:auto">
          <table class="table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Loại giao dịch</th>
                <th>Mã đơn liên quan</th>
                <th style="text-align:right">Số tiền</th>
                <th>Nội dung / Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="h in staffCommissionHistory" :key="h.id">
                <td class="small">{{ formatDateTime(h.createdAt) }}</td>
                <td>
                  <span :class="['badge', h.txType === 'PAYOUT' ? 'badge-payout' : 'badge-earn']">
                    {{ h.txType === 'PAYOUT' ? 'Chi trả hoa hồng' : 'Cộng thưởng hoa hồng' }}
                  </span>
                </td>
                <td><b>{{ h.orderCode || '—' }}</b></td>
                <td style="text-align:right;font-weight:700" :class="h.txType === 'PAYOUT' ? 'text-danger' : 'text-accent'">
                  {{ h.txType === 'PAYOUT' ? '-' : '+' }}{{ vnd(h.amount) }}
                </td>
                <td class="small">{{ h.note }}</td>
              </tr>
              <tr v-if="!staffCommissionHistory.length">
                <td colspan="5" class="muted" style="text-align:center;padding:20px">Chưa có giao dịch hoa hồng nào.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    <!-- Modal Thêm / Chỉnh sửa nhân viên -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal card">
        <div class="row between modal-hd">
          <h3>{{ editingStaff ? 'Chỉnh sửa nhân viên #' + editingStaff.id : 'Thêm nhân viên mới' }}</h3>
          <button class="btn-close" @click="showModal = false">&times;</button>
        </div>

        <form @submit.prevent="saveStaff" class="modal-form">
          <div class="form-group">
            <label>Họ và tên <span class="req">*</span></label>
            <input v-model.trim="form.fullName" class="input" placeholder="Ví dụ: Nguyễn Văn A" required />
          </div>

          <div class="form-group">
            <label>Email đăng nhập <span class="req">*</span></label>
            <input
              v-model.trim="form.email"
              type="email"
              class="input"
              placeholder="nhanvien@ares.vn"
              :disabled="!!editingStaff"
              required
            />
            <small v-if="editingStaff" class="muted">Email không thể thay đổi sau khi tạo.</small>
          </div>

          <div class="form-group">
            <label>
              {{ editingStaff ? 'Mật khẩu mới (để trống nếu không đổi)' : 'Mật khẩu đăng nhập' }}
              <span v-if="!editingStaff" class="req">*</span>
            </label>
            <input
              v-model="form.password"
              type="password"
              class="input"
              :placeholder="editingStaff ? 'Nhập mật khẩu mới nếu muốn đổi...' : 'Tối thiểu 6 ký tự'"
              :required="!editingStaff"
              minlength="6"
            />
          </div>

          <div class="form-group">
            <label>Mã nhân viên (Staff Code)</label>
            <input v-model.trim="form.staffCode" class="input" placeholder="Ví dụ: NV-001, QL-002" />
          </div>

          <div class="form-group">
            <label>Số điện thoại liên hệ</label>
            <input v-model.trim="form.phone" class="input" placeholder="0901234567" />
          </div>

          <div class="form-group">
            <label>Bộ phận / Vị trí quầy</label>
            <input v-model.trim="form.department" class="input" placeholder="Ví dụ: Thu ngân quầy A, Tư vấn xe RC, Trưởng showroom..." />
          </div>

          <div class="form-group">
            <label>Tỷ lệ thưởng hoa hồng (%)</label>
            <input v-model.number="form.commissionRate" type="number" step="0.1" min="0" max="50" class="input" placeholder="Ví dụ: 2.5" />
            <small class="muted">Tỷ lệ % tính hoa hồng trên mỗi đơn hàng tại quầy và online phụ trách.</small>
          </div>

          <div class="form-group">
            <label>Chỉ tiêu doanh số tháng (KPI VNĐ)</label>
            <input v-model.number="form.monthlySalesTarget" type="number" min="0" step="1000000" class="input" placeholder="Ví dụ: 50000000" />
          </div>

          <div class="form-group">
            <label>Vai trò hệ thống <span class="req">*</span></label>
            <select v-model="form.role" class="input" :disabled="editingStaff && editingStaff.id === currentUser?.id" required>
              <option value="STAFF">Nhân viên (STAFF) - Theo phân quyền chức năng</option>
              <option value="ADMIN">Quản trị viên (ADMIN) - Toàn quyền quản trị</option>
            </select>
            <small v-if="editingStaff && editingStaff.id === currentUser?.id" class="muted">Bạn không thể tự đổi vai trò của chính mình.</small>
          </div>

          <div class="form-group">
            <label>Bộ phận / Ghi chú nội bộ</label>
            <input v-model.trim="form.staffNote" class="input" placeholder="Ví dụ: Thu ngân, Bán hàng, Thủ kho, CSKH..." />
          </div>

          <div v-if="formError" class="form-error">
            {{ formError }}
          </div>

          <div class="row modal-actions" style="justify-content:flex-end;margin-top:20px;gap:10px">
            <button type="button" class="btn btn-outline" @click="showModal = false">Huỷ</button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? 'Đang lưu...' : (editingStaff ? 'Cập nhật' : 'Tạo nhân viên') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal Tạo phiếu công nợ trực tiếp cho nhân viên -->
    <div v-if="showDebtModal" class="modal-backdrop" @click.self="showDebtModal = false">
      <div class="modal card">
        <div class="row between modal-hd">
          <h3>Tạo phiếu công nợ cho {{ debtTargetStaff?.fullName || debtTargetStaff?.email }}</h3>
          <button class="btn-close" @click="showDebtModal = false">&times;</button>
        </div>

        <form @submit.prevent="submitCreateDebt" class="modal-form">
          <div class="form-group">
            <label>Số tiền ghi nợ (VNĐ) <span class="req">*</span></label>
            <input
              v-model.number="debtForm.amount"
              type="number"
              min="1000"
              step="1000"
              class="input"
              placeholder="Ví dụ: 500000"
              required
            />
            <small v-if="debtForm.amount" class="muted">Tương đương: <b>{{ vnd(debtForm.amount) }}</b></small>
          </div>

          <div class="form-group">
            <label>Phân loại khoản nợ <span class="req">*</span></label>
            <select v-model="debtForm.type" class="input" required>
              <option value="SALARY_ADVANCE">Tạm ứng lương</option>
              <option value="INTERNAL_PURCHASE">Mua hàng nội bộ (trừ lương)</option>
              <option value="COD_COLLECTED">Tiền COD thu hộ chưa nộp quỹ</option>
              <option value="COMPENSATION">Bồi thường tài sản / làm hỏng hàng</option>
              <option value="OTHER">Khoản nợ khác</option>
            </select>
          </div>

          <div class="form-group">
            <label>Lý do chi tiết <span class="req">*</span></label>
            <textarea
              v-model.trim="debtForm.reason"
              class="input"
              rows="3"
              placeholder="Ghi rõ lý do tạm ứng hoặc chi tiết khoản nợ..."
              required
            ></textarea>
          </div>

          <div class="form-group">
            <label>Hạn thanh toán / khấu trừ lương</label>
            <input v-model="debtForm.dueDate" type="date" class="input" />
          </div>

          <div v-if="debtError" class="form-error">
            {{ debtError }}
          </div>

          <div class="row modal-actions" style="justify-content:flex-end;margin-top:20px;gap:10px">
            <button type="button" class="btn btn-outline" @click="showDebtModal = false">Huỷ</button>
            <button type="submit" class="btn btn-primary" :disabled="submittingDebt">
              {{ submittingDebt ? 'Đang tạo...' : 'Xác nhận tạo phiếu nợ' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal Admin cấp thẻ & đổi hạng VIP cho nhân viên -->
    <div v-if="vipModal" class="modal-backdrop" @click.self="vipModal = null">
      <div class="modal card" style="max-width:520px">
        <div class="row between modal-hd" style="margin-bottom:14px;border-bottom:1px solid var(--border);padding-bottom:10px">
          <div>
            <h3 style="margin:0">CẤP THẺ &amp; NÂNG HẠNG VIP NHÂN VIÊN</h3>
            <p class="small muted" style="margin:2px 0 0">{{ vipModal.staff.fullName }} &bull; {{ vipModal.staff.email }}</p>
          </div>
          <button class="btn-close" @click="vipModal = null">&times;</button>
        </div>

        <div class="form-group" style="margin-bottom:12px">
          <label><b>Hạng thành viên VIP</b></label>
          <select v-model="vipModal.tier" class="input">
            <option value="Đồng">Hạng Đồng (Mặc định)</option>
            <option value="Bạc">Hạng Bạc (Chi từ 3 tr)</option>
            <option value="Vàng">Hạng Vàng (Chi từ 10 tr)</option>
            <option value="Titan">Hạng Titan VIP (Màu đen tuyền tối cao)</option>
          </select>
        </div>

        <div class="form-group" style="margin-bottom:12px">
          <label><b>Mã số thẻ VIP tùy biến</b></label>
          <input
            v-model="vipModal.vipCardNumber"
            class="input"
            placeholder="VD: 8899 • 2026 • 0002 • 1200"
          />
        </div>

        <div class="row" style="gap:12px;margin-bottom:12px">
          <div class="form-group" style="flex:1">
            <label><b>Biến động điểm thưởng (+/-)</b></label>
            <input
              v-model.number="vipModal.pointsDelta"
              type="number"
              class="input"
              placeholder="+500 hoặc -100"
            />
            <small class="muted">Hiện có: {{ vipModal.staff.points || 0 }} pts</small>
          </div>
          <div class="form-group" style="flex:1">
            <label><b>Tặng tiền vào Ví (VNĐ)</b></label>
            <input
              v-model.number="vipModal.creditDelta"
              type="number"
              class="input"
              placeholder="+200000"
            />
          </div>
        </div>

        <div class="form-group" style="margin-bottom:16px">
          <label><b>Lý do cấp thẻ / điều chỉnh</b></label>
          <input
            v-model="vipModal.reason"
            class="input"
            placeholder="VD: Tri ân nhân viên xuất sắc / Thử nghiệm hạng thẻ"
          />
        </div>

        <div class="row modal-actions" style="justify-content:flex-end;gap:10px">
          <button type="button" class="btn btn-outline" @click="vipModal = null">Huỷ</button>
          <button type="button" class="btn btn-primary" :disabled="submittingVip" @click="submitVipModal">
            {{ submittingVip ? 'Đang lưu...' : 'Xác nhận & Cấp thẻ' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../../stores/auth'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'

const auth = useAuth()
const currentUser = computed(() => auth.user)

const activeTab = ref('staff')
const staffList = ref([])

// Commission KPIs for Tab 2
const totalEarnedCommission = computed(() => staffList.value.reduce((sum, s) => sum + (s.earnedCommission || 0), 0))
const totalPaidCommission = computed(() => staffList.value.reduce((sum, s) => sum + (s.paidCommission || 0), 0))
const totalPendingCommission = computed(() => staffList.value.reduce((sum, s) => sum + (s.remainingCommission || 0), 0))
const staffWithPendingCommissionCount = computed(() => staffList.value.filter(s => (s.remainingCommission || 0) > 0).length)
const totalStaffSales = computed(() => staffList.value.reduce((sum, s) => sum + (s.totalSales || 0), 0))
const totalStaffOrders = computed(() => staffList.value.reduce((sum, s) => sum + (s.orderCount || 0), 0))
const loading = ref(true)
const q = ref('')
const filterRole = ref('ALL')
const filterStatus = ref('ALL')
const filterDebt = ref('ALL')

// KPIs
const adminCount = computed(() => staffList.value.filter(s => s.role === 'ADMIN').length)
const staffCount = computed(() => staffList.value.filter(s => s.role === 'STAFF').length)
const activeStaffCount = computed(() => staffList.value.filter(s => s.role === 'STAFF' && !s.locked).length)
const lockedCount = computed(() => staffList.value.filter(s => s.locked).length)
const totalRemainingDebt = computed(() => staffList.value.reduce((sum, s) => sum + (s.remainingDebt || 0), 0))
const staffWithDebtCount = computed(() => staffList.value.filter(s => (s.remainingDebt || 0) > 0).length)

// Filtered Staff
const filteredStaff = computed(() => {
  return staffList.value.filter(s => {
    if (filterRole.value !== 'ALL' && s.role !== filterRole.value) return false
    if (filterStatus.value === 'ACTIVE' && s.locked) return false
    if (filterStatus.value === 'LOCKED' && !s.locked) return false
    if (filterDebt.value === 'HAS_DEBT' && (s.remainingDebt || 0) <= 0) return false
    if (filterDebt.value === 'NO_DEBT' && (s.remainingDebt || 0) > 0) return false

    if (q.value) {
      const needle = q.value.toLowerCase()
      const matchName = (s.fullName || '').toLowerCase().includes(needle)
      const matchEmail = (s.email || '').toLowerCase().includes(needle)
      const matchPhone = (s.phone || '').toLowerCase().includes(needle)
      const matchNote = (s.staffNote || '').toLowerCase().includes(needle)
      if (!matchName && !matchEmail && !matchPhone && !matchNote) return false
    }
    return true
  })
})

const resetFilters = () => {
  q.value = ''
  filterRole.value = 'ALL'
  filterStatus.value = 'ALL'
  filterDebt.value = 'ALL'
}

const loadStaff = async () => {
  loading.value = true
  try {
    const res = await api.get('/admin/staff')
    staffList.value = res.data || []
  } catch (err) {
    console.error('Lỗi khi tải danh sách nhân viên:', err)
  } finally {
    loading.value = false
  }
}

// Modal State
const showModal = ref(false)
const editingStaff = ref(null)
const form = ref({
  fullName: '',
  email: '',
  password: '',
  phone: '',
  role: 'STAFF',
  staffCode: '',
  department: '',
  commissionRate: 2.5,
  monthlySalesTarget: 50000000,
  staffNote: ''
})
const formError = ref('')
const submitting = ref(false)

const openCreateModal = () => {
  editingStaff.value = null
  form.value = {
    fullName: '',
    email: '',
    password: '',
    phone: '',
    role: 'STAFF',
    staffNote: ''
  }
  formError.value = ''
  showModal.value = true
}

const openEditModal = (staff) => {
  editingStaff.value = staff
  form.value = {
    fullName: staff.fullName || '',
    email: staff.email || '',
    password: '',
    phone: staff.phone || '',
    role: staff.role || 'STAFF',
    staffNote: staff.staffNote || ''
  }
  formError.value = ''
  showModal.value = true
}

const saveStaff = async () => {
  formError.value = ''
  submitting.value = true
  try {
    if (editingStaff.value) {
      // Update
      const payload = {
        fullName: form.value.fullName,
        phone: form.value.phone,
        role: form.value.role,
        staffNote: form.value.staffNote
      }
      if (form.value.password && form.value.password.trim().length >= 6) {
        payload.password = form.value.password.trim()
      }
      await api.put(`/admin/staff/${editingStaff.value.id}`, payload)
    } else {
      // Create
      await api.post('/admin/staff', {
        fullName: form.value.fullName,
        email: form.value.email,
        password: form.value.password,
        phone: form.value.phone,
        role: form.value.role,
        staffNote: form.value.staffNote
      })
    }
    showModal.value = false
    await loadStaff()
  } catch (err) {
    formError.value = err.response?.data?.message || err.message || 'Lỗi khi lưu nhân viên'
  } finally {
    submitting.value = false
  }
}

const toggleLock = async (staff) => {
  const confirmMsg = staff.locked
    ? `Mở khoá tài khoản cho nhân viên "${staff.fullName || staff.email}"?`
    : `Tạm khoá tài khoản "${staff.fullName || staff.email}"? Nhân viên này sẽ không thể đăng nhập.`
  if (!confirm(confirmMsg)) return

  try {
    await api.patch(`/admin/staff/${staff.id}/toggle-lock`)
    staff.locked = !staff.locked
  } catch (err) {
    alert(err.response?.data?.message || 'Không thể đổi trạng thái tài khoản')
  }
}

const confirmDelete = async (staff) => {
  if (!confirm(`Bạn có chắc chắn muốn xoá tài khoản nhân viên "${staff.fullName || staff.email}"? Hành động này không thể hoàn tác.`)) {
    return
  }
  try {
    await api.delete(`/admin/staff/${staff.id}`)
    await loadStaff()
  } catch (err) {
    alert(err.response?.data?.message || 'Không thể xoá nhân viên này')
  }
}

// Modal Quyết toán hoa hồng
const showPayoutModal = ref(false)
const payoutTargetStaff = ref(null)
const payoutForm = ref({ amount: null, note: '' })
const payoutError = ref('')
const submittingPayout = ref(false)

const openPayoutModal = (staff) => {
  payoutTargetStaff.value = staff
  payoutForm.value = {
    amount: staff.remainingCommission || 0,
    note: `Quyết toán hoa hồng cho ${staff.fullName || staff.email}`
  }
  payoutError.value = ''
  showPayoutModal.value = true
}

const submitCommissionPayout = async () => {
  if (!payoutTargetStaff.value || !payoutForm.value.amount) return
  payoutError.value = ''
  submittingPayout.value = true
  try {
    await api.post('/admin/staff/commission/payout', {
      staffId: payoutTargetStaff.value.id,
      amount: payoutForm.value.amount,
      note: payoutForm.value.note
    })
    showPayoutModal.value = false
    await loadStaff()
  } catch (err) {
    payoutError.value = err.response?.data?.message || err.message || 'Lỗi khi quyết toán hoa hồng'
  } finally {
    submittingPayout.value = false
  }
}

// Modal Lịch sử hoa hồng
const showHistoryModal = ref(false)
const historyTargetStaff = ref(null)
const staffCommissionHistory = ref([])

const openCommissionHistoryModal = async (staff) => {
  historyTargetStaff.value = staff
  showHistoryModal.value = true
  try {
    const res = await api.get(`/admin/staff/${staff.id}/commissions`)
    staffCommissionHistory.value = res.data || []
  } catch (err) {
    staffCommissionHistory.value = []
  }
}

const formatDateTime = (iso) => {
  if (!iso) return '--'
  try {
    return new Date(iso).toLocaleString('vi-VN')
  } catch {
    return iso
  }
}

// Modal Tạo nợ nhanh
const showDebtModal = ref(false)
const debtTargetStaff = ref(null)
const debtForm = ref({
  amount: null,
  type: 'SALARY_ADVANCE',
  reason: '',
  dueDate: ''
})
const debtError = ref('')
const submittingDebt = ref(false)

const openDebtModal = (staff) => {
  debtTargetStaff.value = staff
  debtForm.value = {
    amount: null,
    type: 'SALARY_ADVANCE',
    reason: `Tạm ứng lương nhân viên ${staff.fullName || staff.email}`,
    dueDate: ''
  }
  debtError.value = ''
  showDebtModal.value = true
}

const submitCreateDebt = async () => {
  if (!debtTargetStaff.value || !debtForm.value.amount) return
  debtError.value = ''
  submittingDebt.value = true
  try {
    await api.post('/admin/staff-debts', {
      staffId: debtTargetStaff.value.id,
      staffName: debtTargetStaff.value.fullName,
      staffEmail: debtTargetStaff.value.email,
      staffPhone: debtTargetStaff.value.phone,
      amount: debtForm.value.amount,
      type: debtForm.value.type,
      reason: debtForm.value.reason,
      dueDate: debtForm.value.dueDate || null
    })
    showDebtModal.value = false
    await loadStaff()
  } catch (err) {
    debtError.value = err.response?.data?.message || err.message || 'Lỗi khi tạo phiếu nợ'
  } finally {
    submittingDebt.value = false
  }
}

// Utilities
const vnd = (n) => (n || 0).toLocaleString('vi-VN') + ' đ'

const formatDate = (iso) => {
  if (!iso) return '--'
  try {
    const d = new Date(iso)
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
  } catch {
    return iso
  }
}

const getInitials = (str) => {
  if (!str) return 'U'
  const parts = str.trim().split(/\s+/)
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  }
  return str.slice(0, 2).toUpperCase()
}

const vipModal = ref(null)
const submittingVip = ref(false)

function openVipModal(staff) {
  vipModal.value = {
    staff,
    tier: staff.tier || 'Đồng',
    vipCardNumber: staff.vipCardNumber || '',
    pointsDelta: 0,
    creditDelta: 0,
    reason: ''
  }
}

async function submitVipModal() {
  if (!vipModal.value) return
  submittingVip.value = true
  try {
    const vm = vipModal.value
    const body = {
      tier: vm.tier,
      pointsDelta: vm.pointsDelta ? +vm.pointsDelta : undefined,
      reason: vm.reason || (vm.pointsDelta > 0 ? 'Admin cấp điểm thưởng VIP' : 'Admin điều chỉnh điểm'),
      creditDelta: vm.creditDelta ? +vm.creditDelta : undefined,
      vipCardNumber: vm.vipCardNumber?.trim() || undefined
    }
    await api.patch(`/admin/customers/${vm.staff.id}`, body)
    toast(`Đã cập nhật đặc quyền & hạng VIP cho ${vm.staff.fullName}`)
    vipModal.value = null
    await loadStaff()
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    submittingVip.value = false
  }
}

onMounted(() => {
  loadStaff()
})
</script>

<style scoped>

/* Tabs: Nhân sự vs Hoa hồng */
.staff-tabs-strip {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 2px solid var(--border);
  padding-bottom: 0;
}

.staff-tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: -2px;
}

.staff-tab-btn:hover {
  color: var(--text);
  background: rgba(0, 0, 0, 0.03);
}

.staff-tab-btn.active {
  color: var(--accent, #e65100);
  border-bottom-color: var(--accent, #e65100);
  font-weight: 700;
  background: transparent;
}

.admin-staff {
  max-width: 1300px;
  margin: 0 auto;
}

.hd {
  margin-bottom: 20px;
  align-items: flex-start;
}

.hd h1 {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif !important;
  font-style: normal !important;
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: -0.01em;
  margin: 0 0 4px;
}

.header-actions {
  gap: 10px;
  flex-wrap: wrap;
}

.header-actions svg {
  margin-right: 4px;
  vertical-align: -2px;
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.kpi-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius, 8px);
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.kpi-card.highlight {
  border-color: rgba(239, 68, 68, 0.4);
  background: linear-gradient(180deg, rgba(239, 68, 68, 0.06), var(--surface));
}

.kpi-lbl {
  font-size: 0.8rem;
  color: var(--text-2);
  text-transform: uppercase;
  letter-spacing: .05em;
  margin-bottom: 6px;
}

.kpi-val {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--text);
  line-height: 1.2;
}

.highlight-val {
  color: var(--accent, #e65100);
}

.danger-val {
  color: #ef4444;
}

/* Filter Bar */
.filter-bar {
  padding: 12px 16px;
}

.filter-row {
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.search-box {
  flex: 1;
  min-width: 240px;
  position: relative;
  display: flex;
  align-items: center;
}

.search-box svg {
  position: absolute;
  left: 12px;
  color: var(--text-2);
  pointer-events: none;
}

.search-box input {
  width: 100%;
  padding-left: 36px;
}

.filter-select {
  width: auto;
  min-width: 160px;
}

/* Table */
.table-wrap {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.table th {
  text-align: left;
  padding: 12px 14px;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: .05em;
  color: var(--text-2);
  border-bottom: 1px solid var(--border);
  background: var(--surface-2, rgba(255, 255, 255, 0.02));
}

.table td {
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

.row-locked {
  opacity: 0.7;
  background: rgba(239, 68, 68, 0.03);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-wrap {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-2, #222);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-ph {
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--accent);
}

.user-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.badge-me {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 1px 6px;
  background: var(--accent);
  color: #fff;
  border-radius: 4px;
  text-transform: uppercase;
}

.user-email {
  font-size: 0.8rem;
  line-height: 1.3;
}

.user-date {
  font-size: 0.72rem;
}

.phone-text {
  font-family: monospace;
  font-size: 0.88rem;
}

/* Badges */
.role-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: .02em;
}

.role-admin {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.35);
}

.role-staff {
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.status-badge {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-active {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.status-locked {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.debt-box {
  display: flex;
  flex-direction: column;
}

.debt-amount {
  color: #ef4444;
  font-weight: 700;
  font-size: 0.92rem;
}

.debt-link {
  font-size: 0.75rem;
  color: var(--accent);
  text-decoration: underline;
  margin-top: 2px;
}

.note-text {
  font-size: 0.82rem;
  color: var(--text-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Actions */
.actions-row {
  gap: 6px;
}

.btn-sm {
  padding: 4px 9px;
  font-size: 0.8rem;
  height: 28px;
  line-height: 1;
}

.btn-lock {
  border-color: rgba(239, 68, 68, 0.4);
  color: #ef4444;
  background: transparent;
}

.btn-lock:hover {
  background: rgba(239, 68, 68, 0.15);
}

.btn-unlock {
  border-color: rgba(34, 197, 94, 0.4);
  color: #22c55e;
  background: transparent;
}

.btn-unlock:hover {
  background: rgba(34, 197, 94, 0.15);
}

.btn-danger-ghost {
  border: none;
  background: transparent;
  color: #888;
}

.btn-danger-ghost:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}

.modal {
  width: 100%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 24px;
}

.modal-hd {
  margin-bottom: 18px;
  align-items: center;
}

.modal-hd h3 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--text-2);
  cursor: pointer;
  line-height: 1;
}

.btn-close:hover {
  color: var(--text);
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--text-2);
}

.form-group .req {
  color: #ef4444;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
}

.form-error {
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
  font-size: 0.82rem;
  margin-top: 10px;
}

/* States */
.loading-state,
.empty-state {
  text-align: center;
  padding: 48px 16px;
  color: var(--text-2);
}

.empty-state svg {
  margin-bottom: 12px;
  opacity: 0.4;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .hd {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
