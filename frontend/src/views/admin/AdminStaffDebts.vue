<template>
  <div class="admin-debts">
    <div class="row between hd">
      <div>
        <h1>Quản lý Công nợ Nhân viên</h1>
        <p class="muted small">Theo dõi tạm ứng lương, tiền COD chưa nộp, mua hàng nội bộ và các khoản bồi thường tài sản.</p>
      </div>
      <div class="row" style="gap:8px">
        <button class="btn" @click="openCreateModal">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" style="display:inline-block;vertical-align:-2px;margin-right:4px"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Tạo phiếu công nợ
        </button>
      </div>
    </div>

    <!-- KPI Summary Cards -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <span class="kpi-lbl">Tổng nợ phát sinh</span>
        <b class="kpi-val">{{ vnd(stats.totalDebt || 0) }}</b>
        <small class="muted">{{ stats.totalRecords || 0 }} phiếu ghi nợ</small>
      </div>
      <div class="kpi-card">
        <span class="kpi-lbl">Đã thu hồi / cấn trừ</span>
        <b class="kpi-val success-val">{{ vnd(stats.totalPaid || 0) }}</b>
        <small class="muted">{{ stats.totalDebt ? Math.round(((stats.totalPaid || 0) * 100) / stats.totalDebt) : 0 }}% tổng số nợ</small>
      </div>
      <div class="kpi-card highlight">
        <span class="kpi-lbl">Dư nợ còn phải thu</span>
        <b class="kpi-val danger-val">{{ vnd(stats.totalRemaining || 0) }}</b>
        <small class="muted">Cần thu hồi & quyết toán</small>
      </div>
      <div class="kpi-card">
        <span class="kpi-lbl">Nhân sự đang có nợ</span>
        <b class="kpi-val">{{ stats.activeStaffCount || 0 }}</b>
        <small class="muted">Nhân viên chưa tất toán</small>
      </div>
    </div>

    <!-- Search & Filter Bar -->
    <div class="card filter-bar">
      <div class="row filter-row">
        <div class="search-box">
          <input
            v-model.trim="q"
            class="input"
            placeholder="Tìm theo tên nhân viên, email hoặc lý do..."
            @keyup.enter="load"
          />
        </div>
        <select v-model="filterStatus" class="input filter-select" @change="load">
          <option value="ALL">Tất cả trạng thái</option>
          <option value="UNPAID">Chưa thanh toán</option>
          <option value="PARTIAL">Đang trả một phần</option>
          <option value="PAID">Đã hoàn tất</option>
        </select>
        <button class="btn btn-outline" @click="load">Lọc</button>
      </div>
    </div>

    <!-- Table of Debts -->
    <div class="card" style="margin-top:16px">
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th style="width:85px">Mã</th>
              <th>Nhân viên</th>
              <th>Phân loại</th>
              <th>Số tiền</th>
              <th>Đã trả</th>
              <th>Còn lại</th>
              <th style="width:110px">Hạn trả</th>
              <th style="width:120px">Trạng thái</th>
              <th style="text-align:right;width:170px">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in items" :key="d.id">
              <td><b>#DBT-{{ d.id }}</b></td>
              <td>
                <div class="staff-cell">
                  <div class="s-avatar">{{ (d.staffName || 'NV').charAt(0).toUpperCase() }}</div>
                  <div>
                    <b>{{ d.staffName }}</b>
                    <small class="muted d-block">{{ d.staffEmail }}</small>
                  </div>
                </div>
              </td>
              <td>
                <span class="type-pill" :class="'type-' + d.debtType">
                  {{ DEBT_TYPES[d.debtType] || d.debtType }}
                </span>
                <p v-if="d.reason" class="small muted note-line">{{ d.reason }}</p>
              </td>
              <td><b>{{ vnd(d.amount) }}</b></td>
              <td class="success-text">{{ vnd(d.paidAmount) }}</td>
              <td class="danger-text"><b>{{ vnd(d.remainingAmount) }}</b></td>
              <td>
                <span :class="['small', isOverdue(d) ? 'overdue' : 'muted']">
                  {{ d.dueDate || 'Không thời hạn' }}
                  <span v-if="isOverdue(d)" class="badge badge-CANCELLED" style="display:block;margin-top:2px">Quá hạn</span>
                </span>
              </td>
              <td>
                <span :class="['badge', STATUS_BADGES[d.status]?.cls]">
                  {{ STATUS_BADGES[d.status]?.lbl || d.status }}
                </span>
              </td>
              <td style="text-align:right">
                <div class="row row-right" style="gap:4px">
                  <button
                    v-if="d.status !== 'PAID'"
                    class="btn btn-sm"
                    title="Ghi nhận trả nợ"
                    @click="openRepayModal(d)"
                  >
                    Thu nợ
                  </button>
                  <button
                    v-if="d.paymentHistoryJson"
                    class="btn btn-sm btn-outline"
                    title="Xem lịch sử thanh toán"
                    @click="viewHistory(d)"
                  >
                    Lịch sử
                  </button>
                  <button
                    class="btn btn-sm btn-danger-outline"
                    title="Xoá bản ghi"
                    @click="removeDebt(d)"
                  >
                    Xoá
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!items.length">
              <td colspan="10" class="muted text-center" style="padding:32px">
                Chưa có dữ liệu công nợ phù hợp.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal: Tạo phiếu công nợ mới -->
    <div v-if="createModal" class="modal-scrim" @click.self="createModal = false">
      <div class="modal card">
        <div class="modal-hd">
          <h3>Tạo phiếu ghi nợ nhân viên</h3>
          <button class="close-btn" @click="createModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label>Nhân viên tiếp nhận nợ *</label>
            <select v-model="form.staffId" class="input">
              <option value="" disabled>-- Chọn nhân viên --</option>
              <option v-for="s in staffList" :key="s.id" :value="s.id">
                {{ s.fullName }} ({{ s.email }}) — {{ s.role }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>Phân loại khoản nợ *</label>
            <select v-model="form.debtType" class="input">
              <option v-for="(v, k) in DEBT_TYPES" :key="k" :value="k">{{ v }}</option>
            </select>
          </div>
          <div class="field">
            <label>Số tiền nợ (VNĐ) *</label>
            <input
              v-model.number="form.amount"
              type="number"
              min="1000"
              step="10000"
              class="input"
              placeholder="VD: 1500000"
            />
          </div>
          <div class="field">
            <label>Hạn chót thanh toán / Khấu trừ</label>
            <input v-model="form.dueDate" type="date" class="input" />
          </div>
          <div class="field">
            <label>Lý do / Diễn giải cụ thể</label>
            <textarea
              v-model="form.reason"
              rows="3"
              class="input"
              placeholder="VD: Ứng lương đợt 1 tháng 11, hoặc thiếu tiền bàn giao ca thu COD đơn TS..."
            />
          </div>
        </div>
        <div class="modal-ft">
          <button class="btn btn-outline" @click="createModal = false">Huỷ</button>
          <button class="btn" :disabled="saving" @click="saveDebt">
            {{ saving ? 'Đang tạo...' : 'Xác nhận tạo phiếu' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: Ghi nhận trả nợ / Cấn trừ -->
    <div v-if="repayModal" class="modal-scrim" @click.self="repayModal = false">
      <div class="modal card">
        <div class="modal-hd">
          <h3>Ghi nhận thu nợ / Cấn trừ lương</h3>
          <button class="close-btn" @click="repayModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="repay-summary">
            <p>Nhân viên: <b>{{ activeDebt?.staffName }}</b></p>
            <p>Khoản nợ: <b>{{ DEBT_TYPES[activeDebt?.debtType] }}</b></p>
            <p>Dư nợ còn lại: <b class="danger-text">{{ vnd(activeDebt?.remainingAmount || 0) }}</b></p>
          </div>

          <div class="field">
            <label>Số tiền thanh toán / Trừ nợ (VNĐ) *</label>
            <div class="row" style="gap:8px">
              <input
                v-model.number="repayForm.payAmount"
                type="number"
                min="1000"
                :max="activeDebt?.remainingAmount"
                class="input"
                style="flex:1"
              />
              <button
                type="button"
                class="btn btn-sm btn-outline"
                @click="repayForm.payAmount = activeDebt?.remainingAmount"
              >
                Trả toàn bộ
              </button>
            </div>
          </div>

          <div class="field">
            <label>Hình thức cấn trừ / thanh toán</label>
            <select v-model="repayForm.method" class="input">
              <option value="TRU_LUONG">Khấu trừ vào kỳ lương</option>
              <option value="CHUYEN_KHOAN">Chuyển khoản ngân hàng</option>
              <option value="TIEN_MAT">Nộp tiền mặt tại showroom</option>
              <option value="KHAC">Khác</option>
            </select>
          </div>

          <div class="field">
            <label>Ghi chú thanh toán</label>
            <input
              v-model="repayForm.note"
              class="input"
              placeholder="VD: Đã trừ lương tháng 10 / Chuyển qua STK công ty"
            />
          </div>
        </div>
        <div class="modal-ft">
          <button class="btn btn-outline" @click="repayModal = false">Huỷ</button>
          <button class="btn" :disabled="saving" @click="submitRepay">
            {{ saving ? 'Đang lưu...' : 'Xác nhận thu nợ' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: Xem lịch sử thanh toán -->
    <div v-if="historyModal" class="modal-scrim" @click.self="historyModal = false">
      <div class="modal card">
        <div class="modal-hd">
          <h3>Lịch sử thanh toán phiếu #DBT-{{ activeDebt?.id }}</h3>
          <button class="close-btn" @click="historyModal = false">×</button>
        </div>
        <div class="modal-body">
          <table class="table small-table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Số tiền</th>
                <th>Hình thức</th>
                <th>Ghi chú</th>
                <th>Người ghi</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(h, idx) in debtHistory" :key="idx">
                <td>{{ h.paidAt }}</td>
                <td class="success-text"><b>{{ vnd(h.amount) }}</b></td>
                <td>{{ h.method }}</td>
                <td>{{ h.note || '—' }}</td>
                <td>{{ h.recordedBy || 'Admin' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="modal-ft">
          <button class="btn btn-outline" @click="historyModal = false">Đóng</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd } from '../../format'
import { toast } from '../../stores/prefs'

const DEBT_TYPES = {
  TAM_UNG: 'Tạm ứng lương',
  COD_THIEU: 'Thiếu tiền COD',
  MUA_HANG: 'Mua hàng nội bộ',
  DEN_BU: 'Đền bù tài sản / làm hỏng xe',
  KHAC: 'Khác'
}

const STATUS_BADGES = {
  UNPAID: { lbl: 'Chưa thanh toán', cls: 'badge-CANCELLED' },
  PARTIAL: { lbl: 'Trả một phần', cls: 'badge-PENDING' },
  PAID: { lbl: 'Đã hoàn tất', cls: 'badge-COMPLETED' }
}

const items = ref([])
const stats = ref({})
const staffList = ref([])
const q = ref('')
const filterStatus = ref('ALL')
const saving = ref(false)

const createModal = ref(false)
const form = reactive({
  staffId: '',
  debtType: 'TAM_UNG',
  amount: 0,
  dueDate: '',
  reason: ''
})

const repayModal = ref(false)
const activeDebt = ref(null)
const repayForm = reactive({
  payAmount: 0,
  method: 'TRU_LUONG',
  note: ''
})

const historyModal = ref(false)
const debtHistory = ref([])

function isOverdue(d) {
  if (!d.dueDate || d.status === 'PAID') return false
  return new Date(d.dueDate) < new Date()
}

async function load() {
  try {
    const params = {}
    if (q.value) params.q = q.value
    if (filterStatus.value !== 'ALL') params.status = filterStatus.value
    const res = await api.get('/admin/staff-debts', { params })
    items.value = res.data.items || []
    stats.value = res.data.stats || {}
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

async function loadStaff() {
  try {
    const res = await api.get('/admin/staff-debts/staff-list')
    staffList.value = res.data || []
  } catch (e) {
    console.error('Cannot load staff list', e)
  }
}

function openCreateModal() {
  form.staffId = staffList.value[0]?.id || ''
  form.debtType = 'TAM_UNG'
  form.amount = 1000000
  form.dueDate = ''
  form.reason = ''
  createModal.value = true
}

async function saveDebt() {
  if (!form.staffId) return toast('Vui lòng chọn nhân viên', 'error')
  if (!form.amount || form.amount <= 0) return toast('Số tiền nợ phải lớn hơn 0', 'error')
  saving.value = true
  try {
    await api.post('/admin/staff-debts', form)
    toast('Đã lập phiếu công nợ nhân viên thành công')
    createModal.value = false
    await load()
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    saving.value = false
  }
}

function openRepayModal(d) {
  activeDebt.value = d
  repayForm.payAmount = d.remainingAmount
  repayForm.method = 'TRU_LUONG'
  repayForm.note = ''
  repayModal.value = true
}

async function submitRepay() {
  if (!repayForm.payAmount || repayForm.payAmount <= 0) return toast('Nhập số tiền hợp lệ', 'error')
  saving.value = true
  try {
    await api.post(`/admin/staff-debts/${activeDebt.value.id}/repay`, repayForm)
    toast('Đã ghi nhận thanh toán công nợ thành công')
    repayModal.value = false
    await load()
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    saving.value = false
  }
}

function viewHistory(d) {
  activeDebt.value = d
  try {
    debtHistory.value = JSON.parse(d.paymentHistoryJson || '[]')
  } catch {
    debtHistory.value = []
  }
  historyModal.value = true
}

async function removeDebt(d) {
  if (!confirm(`Xoá phiếu nợ #DBT-${d.id} của ${d.staffName}?`)) return
  try {
    await api.delete(`/admin/staff-debts/${d.id}`)
    toast('Đã xoá phiếu nợ')
    await load()
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

onMounted(async () => {
  await Promise.all([load(), loadStaff()])
})
</script>

<style scoped>
.admin-debts {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.kpi-card {
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-card.highlight {
  border-color: var(--accent);
  background: var(--bronze-soft);
}

.kpi-lbl {
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--muted);
}

.kpi-val {
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--text);
  line-height: 1.2;
}

.success-val { color: var(--success); }
.danger-val { color: var(--danger); }

.filter-bar {
  padding: 12px 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 240px;
}

.filter-select {
  width: auto;
  min-width: 170px;
}

.staff-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.s-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 0.88rem;
}

.d-block { display: block; }

.type-pill {
  display: inline-block;
  font-size: 0.76rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--bg);
  border: 1px solid var(--border);
}

.note-line {
  margin: 2px 0 0;
  font-size: 0.74rem;
  max-width: 160px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.success-text { color: var(--success); }
.danger-text { color: var(--danger); }

.overdue {
  color: var(--danger);
  font-weight: 700;
}

.row-right {
  display: flex;
  justify-content: flex-end;
  white-space: nowrap;
  flex-wrap: nowrap;
  gap: 4px;
}

.admin-debts .table {
  width: 100%;
}

.admin-debts .table th,
.admin-debts .table td {
  padding: 8px 6px;
  font-size: 0.82rem;
  vertical-align: middle;
}

.admin-debts .table td .btn-sm {
  padding: 4px 7px;
  font-size: 0.76rem;
}

.btn-danger-outline {
  border: 1px solid var(--danger);
  color: var(--danger);
  background: transparent;
}
.btn-danger-outline:hover {
  background: var(--danger);
  color: #fff;
}

/* Modals */
.modal-scrim {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  display: grid;
  place-items: center;
  z-index: 1000;
  padding: 16px;
}

.modal {
  width: min(520px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--surface);
  border: 1px solid var(--border);
}

.modal-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border);
  padding-bottom: 12px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--muted);
  cursor: pointer;
  line-height: 1;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.modal-ft {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}

.repay-summary {
  background: var(--bg);
  padding: 12px 14px;
  border-radius: 6px;
  border: 1px solid var(--border);
  font-size: 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.small-table {
  font-size: 0.85rem;
}
</style>
