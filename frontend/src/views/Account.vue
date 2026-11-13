<template>
  <div class="container page">
    <h1>Tài khoản của tôi</h1>
    <p class="muted">{{ auth.user.email }} <span class="badge">{{ auth.user.provider === 'LOCAL' ? 'Email' : auth.user.provider }}</span></p>
    <div class="tabs">
      <button v-for="(v, k) in tabs" :key="k" :class="{ active: tab === k }" @click="setTab(k)">{{ v }}</button>
    </div>

    <!-- Hồ sơ -->
    <div v-if="tab === 'profile'" class="grid grid-2">
      <div class="card">
        <h3>Thông tin cá nhân</h3>
        <div v-if="msg.profile" class="alert alert-success">{{ msg.profile }}</div>
        
        <!-- Quản lý Avatar -->
        <div class="avatar-manager">
          <div class="avatar-view-row">
            <div class="avatar-large">
              <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="Avatar" class="avatar-large-img" />
              <div v-else class="avatar-large-placeholder">{{ (profile.fullName || auth.user.fullName || '?').split(' ').pop()[0] }}</div>
              <button type="button" class="avatar-upload-badge" title="Tải ảnh mới từ máy" @click="fileInput?.click()">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
              </button>
              <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/webp,image/gif" style="display:none" @change="uploadAvatar" />
            </div>
            <div class="avatar-meta">
              <b>Ảnh đại diện tài khoản</b>
              <p class="muted small">Tải ảnh từ máy tính (tối đa 5MB) hoặc chọn từ Bộ sưu tập Tay Đua Siêu Xe Ares bên dưới.</p>
              <div class="avatar-btn-group">
                <button type="button" class="btn btn-sm" :disabled="avatarLoading" @click="fileInput?.click()">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg><span>{{ avatarLoading ? 'Đang tải lên...' : 'Chọn ảnh từ máy' }}</span>
                </button>
                <button v-if="profile.avatarUrl" type="button" class="btn btn-sm btn-outline" @click="clearAvatar">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px"><path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                  <span>Xoá ảnh</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Bộ sưu tập Avatar có sẵn -->
          <div class="presets-wrap">
            <span class="small muted">Bộ sưu tập Avatar Tay Đua Siêu Xe ARES:</span>
            <div class="preset-items">
              <button v-for="p in PRESET_AVATARS" :key="p.id" type="button" :class="['preset-item', { active: profile.avatarUrl === p.url }]" :title="p.name" @click="pickPreset(p)">
                <img :src="p.url" class="preset-avatar-img" :alt="p.name" />
                <small>{{ p.name }}</small>
              </button>
            </div>
          </div>
        </div>

        <div class="field"><label>Họ và tên</label><input v-model="profile.fullName" class="input" /></div>
        <div class="field"><label>Số điện thoại</label><input v-model="profile.phone" class="input" /></div>
        <div class="field"><label>Hoặc nhập link ảnh đại diện (URL)</label><input v-model="profile.avatarUrl" class="input" placeholder="https://..." /></div>
        <button class="btn" @click="saveProfile">Lưu thay đổi</button>
      </div>
      <div class="card">
        <h3>Đổi mật khẩu</h3>
        <div v-if="msg.pw" :class="['alert', msg.pwOk ? 'alert-success' : 'alert-error']">{{ msg.pw }}</div>
        <div v-if="auth.user.provider === 'LOCAL'" class="field"><label>Mật khẩu hiện tại</label><input v-model="pw.currentPassword" type="password" class="input" /></div>
        <div class="field"><label>Mật khẩu mới</label><input v-model="pw.newPassword" type="password" class="input" /></div>
        <button class="btn" @click="changePw">Đổi mật khẩu</button>
      </div>
    </div>

    <!-- Sổ địa chỉ -->
    <div v-if="tab === 'addresses'">
      <div class="row between" style="margin-bottom:14px"><span class="muted">Lưu địa chỉ giao nhận để thanh toán nhanh hơn</span>
        <button class="btn btn-sm" @click="editAddr()">+ Thêm địa chỉ</button></div>
      <div v-if="!addresses.length" class="empty card">Chưa có địa chỉ nào.</div>
      <div class="grid grid-4">
        <div v-for="a in addresses" :key="a.id" class="card addr">
          <div class="row between"><b>{{ a.label || 'Địa chỉ' }}</b><span v-if="a.isDefault" class="badge badge-new">Mặc định</span></div>
          <p>{{ a.receiverName }} · {{ a.phone }}</p>
          <p class="muted small">{{ [a.line, a.ward, a.district, a.city].filter(Boolean).join(', ') }}</p>
          <div class="row"><button class="btn btn-sm btn-outline" @click="editAddr(a)">Sửa</button><button class="btn btn-sm btn-outline" @click="delAddr(a)">Xoá</button><button v-if="!a.isDefault" class="btn btn-sm btn-outline" @click="makeDefault(a)">Đặt mặc định</button></div>
        </div>
      </div>
    </div>

    <!-- Lịch sử giao dịch -->
    <div v-if="tab === 'orders'" class="card table-wrap">
      <div v-if="!orders.length" class="empty">Bạn chưa có đơn hàng nào. <router-link to="/products">Mua sắm ngay</router-link></div>
      <table v-else class="table">
        <thead><tr><th>Mã đơn</th><th>Ngày đặt</th><th>Sản phẩm</th><th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="o in orders" :key="o.id">
          <td><b>{{ o.code }}</b></td><td>{{ dt(o.createdAt) }}</td>
          <td class="small">{{ o.items.map(i => i.productName + ' ×' + i.quantity).join(', ') }}</td>
          <td class="price">{{ vnd(o.total) }}</td>
          <td class="small">{{ PAY_METHOD[o.paymentMethod] }}<br /><span :class="['badge', 'badge-' + o.paymentStatus]">{{ PAY_STATUS[o.paymentStatus] }}</span></td>
          <td><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span></td>
          <td><router-link :to="`/orders/${o.code}`" class="btn btn-sm btn-outline">Chi tiết</router-link></td>
        </tr></tbody>
      </table>
    </div>

    <!-- Điểm thưởng & Thẻ hạng thành viên VIP -->
    <div v-if="tab === 'points'">
      <div class="grid grid-2 vip-card-section">
        <!-- 1. Thẻ thành viên VIP Vietcombank Platinum Visa Debit -->
        <div class="vip-card-wrap">
          <VipCard
            :tier="activeCardTier"
            :points="pts.points || 0"
            :user="auth.user"
            :allow-click="true"
            :show-avatar-badge="true"
          />
        </div><!-- 2. Khối THÔNG TIN THẺ THÀNH VIÊN (Cạnh thẻ) -->
        <div class="card card-info-panel">
          <div class="cip-header">
            <div>
              <h3>HỒ SƠ THẺ THÀNH VIÊN ARES VIP</h3>
              <p class="muted small">Định danh đặc quyền & Quản lý điểm thưởng hội viên</p>
            </div>
            <span class="vip-status-pill">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" style="display:inline-block;vertical-align:-2px;margin-right:4px"><polyline points="20 6 9 17 4 12"/></svg>
              ĐANG HOẠT ĐỘNG
            </span>
          </div>

          <div class="cip-grid">
            <div class="cip-item">
              <span class="cip-lbl">Mã số thẻ VIP:</span>
              <div class="cip-val-row">
                <b class="card-code-mono">{{ formatCardNum(auth.user?.vipCardNumber || auth.user?.id, pts.points) }}</b>
                <button class="btn btn-sm btn-outline btn-copy" title="Sao chép số thẻ" @click="copyCardNum">Sao chép</button>
              </div>
            </div>

            <div class="cip-item">
              <span class="cip-lbl">{{ selectedTier ? 'Đang xem mẫu thẻ:' : 'Hạng thẻ hiện tại:' }}</span>
              <div class="cip-tier-row" style="display:flex;align-items:center;gap:8px">
                <b class="cip-tier-val" :class="'tier-text-' + activeCardTier.replace(/\s+/g, '_')">
                  Hạng {{ activeCardTier }} VIP
                </b>
                <button v-if="selectedTier && activeCardTier !== formatTierName(pts.tier || auth.user?.tier || 'Đồng')" type="button" class="btn btn-xs btn-outline" style="font-size:0.72rem;padding:2px 7px" @click="selectedTier = ''">
                  Về hạng của tôi ({{ formatTierName(pts.tier || auth.user?.tier || 'Đồng') }})
                </button>
              </div>
            </div>

            <div class="cip-item">
              <span class="cip-lbl">Điểm thưởng tích luỹ:</span>
              <b class="cip-pts-val">{{ pts.points || 0 }} <small>PTS</small> <span class="pts-cash">(≈ {{ vnd((pts.points || 0) * (pts.pointValue || 100)) }})</span></b>
            </div>

            <div class="cip-item">
              <span class="cip-lbl">Tổng chi tiêu tích luỹ:</span>
              <b class="cip-spent-val" style="font-size:1.02rem;color:var(--text);font-weight:800">
                {{ vnd(pts.totalSpent || 0) }}
                <small class="muted" style="font-weight:normal;font-size:0.75rem">(Đã thanh toán)</small>
              </b>
            </div>

            <div class="cip-item">
              <span class="cip-lbl">Chủ sở hữu định danh:</span>
              <span class="cip-user-val"><b>{{ auth.user?.fullName }}</b> · {{ auth.user?.phone || auth.user?.email }}</span>
            </div>

            <div class="cip-item">
              <span class="cip-lbl">Niên khóa thẻ:</span>
              <span class="cip-sub-val">2024 – 2028 (Gia hạn tự động theo hoạt động)</span>
            </div>

            <div class="cip-item">
              <span class="cip-lbl">Đặc quyền kích hoạt:</span>
              <span class="cip-sub-val">Bảo hành VIP 1 đổi 1, Kiểm định xe miễn phí, Đấu giá ưu tiên</span>
            </div>
          </div>

          <div class="cip-actions">
            <router-link to="/rewards" class="btn btn-sm btn-spin">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><circle cx="12" cy="12" r="10"/><path d="M12 2a10 10 0 0 1 10 10"/><path d="m16 8 4 4-4 4"/></svg>
              Vòng quay & Điểm danh hằng ngày &rarr;
            </router-link>
            <router-link to="/promotions" class="btn btn-sm btn-outline">
              Đổi điểm lấy Voucher
            </router-link>
          </div>
        </div>
      </div>

      <!-- 3. ĐẶC QUYỀN HẠNG & TIẾN TRÌNH THĂNG HẠNG (CHUYỂN XUỐNG DƯỚI TOÀN CHIỀU RỘNG) -->
      <div class="card tier-privileges-card full-width-tiers" style="margin-top:22px">
        <div class="row between" style="margin-bottom:14px;align-items:center">
          <div>
            <h3 style="margin:0;font-size:1.15rem">ĐẶC QUYỀN HỘI VIÊN &bull; TIẾN TRÌNH THĂNG HẠNG</h3>
            <p class="muted small" style="margin:3px 0 0">Tích luỹ điểm chi tiêu từ các đơn mua xe để nâng cấp thẻ và mở khóa ưu đãi độc quyền (Nhấn vào từng hạng để xem mẫu thẻ thực tế)</p>
          </div>
          <span class="tier-current-tag">Hạng hiện tại: <b>{{ formatTierName(pts.tier || 'Đồng') }}</b></span>
        </div>

        <!-- CÔNG CỤ QUẢN TRỊ VIÊN VIP (Dành riêng cho ADMIN) -->
        <div v-if="auth.isAdmin" class="admin-quick-vip-panel card" style="margin-bottom:16px;border:1.5px dashed var(--accent);background:rgba(240,90,26,0.05);padding:14px 18px">
          <div class="row between" style="align-items:center;flex-wrap:wrap;gap:12px">
            <div>
              <b style="color:var(--accent);display:flex;align-items:center;gap:6px">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5m14 3c0 .6-.4 1-1 1H6c-.6 0-1-.4-1-1v-1h14v1z"/></svg>
                CÔNG CỤ ADMIN: CẤP THẺ &amp; NÂNG HẠNG TRỰC TIẾP
              </b>
              <p class="muted small" style="margin:2px 0 0">Quản trị viên có thể đổi hạng thẻ hoặc cấp điểm thưởng thử nghiệm ngay tại đây.</p>
            </div>
            <div class="row" style="gap:8px;align-items:center;flex-wrap:wrap">
              <select v-model="adminTierPick" class="input" style="width:auto;padding:5px 10px;font-size:0.85rem">
                <option value="Đồng">Hạng Đồng (Mặc định)</option>
                <option value="Bạc">Hạng Bạc (Chi từ 3 tr)</option>
                <option value="Vàng">Hạng Vàng (Chi từ 10 tr)</option>
                <option value="Titan">Hạng Titan VIP (Đen tuyền tối cao)</option>
              </select>
              <button type="button" class="btn btn-sm btn-primary" :disabled="adminLoading" @click="adminApplyTier(adminTierPick)">
                {{ adminLoading ? 'Đang lưu...' : 'Nâng hạng ngay' }}
              </button>
              <button type="button" class="btn btn-sm btn-outline" :disabled="adminLoading" title="Cộng thử nghiệm 500 điểm" @click="adminAddPoints(500)">
                +500 PTS
              </button>
              <button type="button" class="btn btn-sm btn-outline" :disabled="adminLoading" title="Cộng thử nghiệm 2000 điểm" @click="adminAddPoints(2000)">
                +2.000 PTS
              </button>
            </div>
          </div>
        </div>

        <div class="tier-progress-box">
          <!-- 1. Tiến trình chi tiêu thực tế (VNĐ) -->
          <div class="row between small" style="margin-bottom:6px">
            <span>Tiêu dùng tích luỹ: <b>{{ vnd(pts.totalSpent || 0) }}</b></span>
            <span v-if="nextTier">Mục tiêu lên <b>{{ nextTier.name }}</b>: {{ vnd(nextTierSpentReq) }} (còn {{ vnd(Math.max(0, nextTierSpentReq - (pts.totalSpent || 0))) }})</span>
            <span v-else class="ok"><b>Đã đạt cấp bậc tối cao Titan!</b></span>
          </div>
          <div class="t-bar" style="margin-bottom:12px">
            <div :style="{ width: nextTier ? Math.min(100, Math.round(((pts.totalSpent || 0) * 100) / (nextTierSpentReq || 1))) + '%' : '100%' }" />
          </div>

          <!-- 2. Tiến trình điểm thưởng (PTS) -->
          <div class="row between small" style="margin-bottom:6px">
            <span>Điểm thưởng hiện có: <b>{{ pts.points || 0 }} PTS</b></span>
            <span v-if="nextTier">Cột mốc điểm <b>{{ nextTier.name }}</b>: {{ nextTier.at }} PTS (còn {{ Math.max(0, nextTier.at - (pts.points || 0)) }} PTS)</span>
          </div>
          <div class="t-bar">
            <div style="background: linear-gradient(90deg, #10b981, #06b6d4)" :style="{ width: nextTier ? Math.min(100, Math.round(((pts.points || 0) * 100) / (nextTier.at || 1))) + '%' : '100%' }" />
          </div>

          <small class="muted" style="margin-top:8px;display:block">
            Tỷ lệ quy đổi: 1 điểm = {{ vnd(pts.pointValue || 100) }} khi trừ tiền đơn hàng · Tích 1 điểm mỗi {{ vnd(pts.earnPer || 10000) }} giá trị đơn hoàn tất / thanh toán
          </small>
        </div>

        <div class="tiers-grid">
          <div v-for="t in TIERS" :key="t.name" :class="['tier-col', 'tier-box-' + t.name.replace(/\s+/g, '_'), { active: t.name === activeCardTier, 'is-user-tier': t.name === formatTierName(pts.tier || auth.user?.tier || 'Đồng') }]" style="cursor:pointer" :title="'Nhấn để xem mẫu thẻ ' + t.name" @click="selectedTier = t.name">
            <div class="tc-header">
              <div class="tc-name">{{ t.name }}</div>
              <span v-if="t.name === formatTierName(pts.tier || auth.user?.tier || 'Đồng')" class="tc-badge-cur">HẠNG CỦA BẠN</span>
              <span v-else-if="t.name === activeCardTier" class="tc-badge-cur" style="background:#f05a1a;color:#fff">ĐANG XEM</span>
              <span v-else class="tc-badge-cur" style="background:transparent;border:1px dashed #777;color:#aaa">BẤM ĐỂ XEM</span>
            </div>
            <div class="tc-req">{{ t.label }}</div>
            <ul class="tc-perks">
              <li v-if="t.name === 'Đồng'"><span class="perk-bullet">&bull;</span> Tích 1% điểm đơn hàng</li>
              <li v-if="t.name === 'Đồng'"><span class="perk-bullet">&bull;</span> Quà sinh nhật 50.000đ</li>
              <li v-if="t.name === 'Đồng'"><span class="perk-bullet">&bull;</span> Miễn phí gói quà tặng</li>
              <li v-if="t.name === 'Bạc'"><span class="perk-bullet">&bull;</span> Tích 2% điểm đơn hàng</li>
              <li v-if="t.name === 'Bạc'"><span class="perk-bullet">&bull;</span> 5 voucher freeship mỗi tháng</li>
              <li v-if="t.name === 'Bạc'"><span class="perk-bullet">&bull;</span> Ưu tiên giao hàng tiêu chuẩn</li>
              <li v-if="t.name === 'Vàng'"><span class="perk-bullet">&bull;</span> Tích 3% điểm đơn hàng</li>
              <li v-if="t.name === 'Vàng'"><span class="perk-bullet">&bull;</span> Miễn phí vận chuyển toàn quốc</li>
              <li v-if="t.name === 'Vàng'"><span class="perk-bullet">&bull;</span> Ưu tiên đấu giá xe mô hình hiếm</li>
              <li v-if="t.name === 'Vàng'"><span class="perk-bullet">&bull;</span> Quà sinh nhật VIP 200.000đ</li>
              <li v-if="t.name === 'Titan' || t.name === 'Bạch kim' || t.name === 'Kim cương'"><span class="perk-bullet">&bull;</span> Tích 5% điểm đơn hàng</li>
              <li v-if="t.name === 'Titan' || t.name === 'Bạch kim' || t.name === 'Kim cương'"><span class="perk-bullet">&bull;</span> Suất đặt trước xe Limited số 1-10</li>
              <li v-if="t.name === 'Titan' || t.name === 'Bạch kim' || t.name === 'Kim cương'"><span class="perk-bullet">&bull;</span> Quà sinh nhật thượng hạng 500.000đ</li>
              <li v-if="t.name === 'Titan' || t.name === 'Bạch kim' || t.name === 'Kim cương'"><span class="perk-bullet">&bull;</span> Trợ lý riêng phục vụ 24/7 & Bảo hành 1 đổi 1 tận nơi</li>
            </ul>
            <button
              v-if="auth.isAdmin && t.name !== formatTierName(pts.tier || auth.user?.tier || 'Đồng')"
              type="button"
              class="btn btn-xs btn-outline"
              style="margin-top:10px;width:100%;font-size:0.75rem;padding:4px 6px"
              :disabled="adminLoading"
              @click.stop="adminApplyTier(t.name)"
            >
              👑 Admin: Đổi sang {{ t.name }}
            </button>
          </div>
        </div>
      </div>

      <!-- Lịch sử điểm -->
      <div class="card table-wrap" style="margin-top:20px">
        <h3>Lịch sử giao dịch điểm thưởng</h3>
        <div v-if="!pts.history?.length" class="empty">Chưa có giao dịch điểm.</div>
        <table v-else class="table">
          <thead><tr><th>Thời gian</th><th>Nội dung</th><th style="text-align:right">Biến động điểm</th></tr></thead>
          <tbody>
            <tr v-for="h in pts.history" :key="h.id">
              <td>{{ dt(h.createdAt) }}</td>
              <td>{{ h.reason }}</td>
              <td :class="['price', h.delta > 0 ? 'ok' : 'err']" style="text-align:right">
                {{ h.delta > 0 ? '+' : '' }}{{ h.delta }} pts
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Đổi trả -->
    <div v-if="tab === 'returns'" class="card table-wrap">
      <div v-if="!returnsList.length" class="empty">Chưa có yêu cầu đổi trả. Bạn có thể gửi yêu cầu trong trang chi tiết đơn đã giao (trong 7 ngày).</div>
      <table v-else class="table">
        <thead><tr><th>Đơn</th><th>Loại</th><th>Lý do</th><th>Trạng thái</th><th>Phản hồi</th><th>Ngày</th></tr></thead>
        <tbody><tr v-for="r in returnsList" :key="r.id"><td><router-link :to="`/orders/${r.orderCode}`">{{ r.orderCode }}</router-link></td><td>{{ r.kind === 'REFUND' ? 'Hoàn tiền' : 'Đổi trả' }}</td>
          <td class="small">{{ r.reason }}</td><td><span :class="['badge', 'badge-' + r.status]">{{ RETURN_STATUS[r.status] }}</span></td><td class="small">{{ r.adminNote }}</td><td>{{ d(r.createdAt) }}</td></tr></tbody>
      </table>
    </div>

    <!-- Phiếu hỗ trợ -->
    <div v-if="tab === 'tickets'" class="card table-wrap">
      <div class="row between" style="margin-bottom:10px"><span class="muted">Phiếu hỗ trợ của bạn</span><router-link to="/support#ticket" class="btn btn-sm">+ Tạo phiếu</router-link></div>
      <div v-if="!ticketsList.length" class="empty">Chưa có phiếu hỗ trợ.</div>
      <table v-else class="table">
        <thead><tr><th>Mã</th><th>Chủ đề</th><th>Loại</th><th>Trạng thái</th><th>Cập nhật</th></tr></thead>
        <tbody><tr v-for="t in ticketsList" :key="t.id"><td><router-link :to="`/support/tickets/${t.code}`">{{ t.code }}</router-link></td><td>{{ t.subject }}</td><td>{{ t.category }}</td>
          <td><span :class="['badge', 'badge-' + t.status]">{{ TICKET_STATUS[t.status] }}</span></td><td>{{ dt(t.updatedAt) }}</td></tr></tbody>
      </table>
    </div>

    <div v-if="tab === 'bids'" class="card table-wrap">
      <div v-if="!bids.length" class="empty">Bạn chưa tham gia phiên đấu giá nào. <router-link to="/auctions">Xem phiên đấu giá</router-link></div>
      <table v-else class="table">
        <thead><tr><th>Phiên</th><th>Giá bạn đặt</th><th>Giá hiện tại</th><th>Thời gian</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="b in bids" :key="b.id">
          <td><router-link :to="`/auctions/${b.auctionId}`">{{ b.auctionTitle }}</router-link></td>
          <td class="price">{{ vnd(b.amount) }}</td><td>{{ vnd(b.currentPrice) }}</td><td class="small">{{ dt(b.createdAt) }}</td>
          <td><span :class="['badge', b.won ? 'badge-COMPLETED' : 'badge-' + (b.status === 'LIVE' ? 'CONFIRMED' : 'PENDING')]">{{ b.won ? 'Thắng' : AUCTION_STATUS[b.status] }}</span></td>
          <td><router-link v-if="b.won && b.orderCode" :to="`/orders/${b.orderCode}`" class="btn btn-sm">Thanh toán</router-link></td>
        </tr></tbody>
      </table>
    </div>

    <div v-if="tab === 'wallet'" class="grid grid-2 wallet">
      <div class="card wcard">
        <small>Ví tín dụng ARES</small><b class="bal">{{ vnd(w.storeCredit || 0) }}</b>
        <p class="small muted">Dùng trừ thẳng vào đơn ở bước thanh toán. Nguồn: thẻ quà, hoàn tiền vào ví.</p>
        <form class="row" @submit.prevent="redeem"><input v-model.trim="gcCode" class="input grow" placeholder="Nhập mã thẻ quà GC…" aria-label="Mã thẻ quà" /><button class="btn btn-sm">Nạp vào ví</button></form>
        <router-link to="/gift-cards" class="small">Mua thẻ quà tặng bạn bè →</router-link>
      </div>
      <div class="card">
        <h3>Giới thiệu bạn bè</h3>
        <p class="small muted">Bạn bè giảm <b>{{ vnd(w.referral?.discount) }}</b> cho đơn đầu tiên; bạn nhận <b>{{ w.referral?.reward }} điểm</b> mỗi lượt.</p>
        <div class="refcode"><code>{{ w.referral?.code }}</code><button class="btn btn-sm" @click="copyRef">Sao chép liên kết</button></div>
        <p class="small muted">Đã có {{ w.referral?.uses || 0 }} người dùng mã của bạn.</p>
      </div>
      <div class="card">
        <h3>Thẻ quà của tôi</h3>
        <p v-if="!w.giftCards?.length" class="muted small">Chưa có thẻ quà.</p>
        <div v-for="g in w.giftCards" :key="g.id" class="row between gc"><code>{{ g.code }}</code><span class="small">{{ vnd(g.balance) }} / {{ vnd(g.initial) }}</span><span class="small muted">HSD {{ d(g.expiresAt) }}</span></div>
      </div>
      <div class="card">
        <h3>Hộp thư (email mô phỏng)</h3>
        <p v-if="!w.inbox?.length" class="muted small">Chưa có email.</p>
        <details v-for="m in w.inbox" :key="m.id" class="mail"><summary><b>{{ m.subject }}</b><small class="muted"> · {{ ago(m.createdAt) }}</small></summary><pre>{{ m.body }}</pre></details>
      </div>
    </div>

    <div v-if="tab === 'settings'" class="grid grid-2">
      <div class="card">
        <h3>Xác thực 2 bước (TOTP)</h3>
        <p class="muted small">Bảo vệ tài khoản bằng mã 6 số từ Google Authenticator / Authy mỗi lần đăng nhập.</p>
        <template v-if="auth.user.totpEnabled">
          <p class="ok">✓ Đang bật</p>
          <form class="row" @submit.prevent="off2fa"><input v-model.trim="otp" class="input grow" inputmode="numeric" maxlength="6" placeholder="Mã 6 số để tắt" aria-label="Mã xác thực" /><button class="btn btn-sm btn-outline">Tắt 2FA</button></form>
        </template>
        <template v-else-if="tf">
          <div class="tf"><img v-if="tfQr" :src="tfQr" alt="Mã QR thiết lập 2FA" width="160" height="160" /><div><p class="small">1. Quét mã QR bằng ứng dụng xác thực<br />2. Hoặc nhập khoá: <code class="key">{{ tf.secret.match(/.{1,4}/g).join(' ') }}</code></p>
            <form class="row" @submit.prevent="on2fa"><input v-model.trim="otp" class="input grow" inputmode="numeric" maxlength="6" placeholder="Mã 6 số" aria-label="Mã xác thực" /><button class="btn btn-sm">Kích hoạt</button></form></div></div>
        </template>
        <button v-else class="btn btn-sm" @click="setup2fa">Bật xác thực 2 bước</button>
        <p v-if="msg.tf" :class="msg.tfOk ? 'ok' : 'err'">{{ msg.tf }}</p>
      </div>
      <div class="card">
        <h3>Thanh toán ưa thích</h3>
        <p class="muted small">Tự chọn sẵn ở bước thanh toán.</p>
        <div class="paypref">
          <label v-for="(v, k) in PAY_METHOD" :key="k" :class="['pp', { on: payPref === k }]"><input v-model="payPref" type="radio" :value="k" @change="savePay" />{{ v }}</label>
          <label :class="['pp', { on: !payPref }]"><input v-model="payPref" type="radio" value="" @change="savePay" />Không đặt</label>
        </div>
      </div>
      <div class="card">
        <h3>Thông báo giá & hàng</h3>
        <p v-if="!myAlerts.length" class="muted small">Chưa theo dõi sản phẩm nào. Bật trong <router-link to="/wishlist">Yêu thích</router-link>.</p>
        <div v-for="a in myAlerts" :key="a.id" class="row between alrow">
          <router-link :to="`/products/${a.productId}`">{{ a.name || 'Sản phẩm #' + a.productId }}</router-link>
          <span class="badge">{{ a.kind === 'PRICE' ? 'Giảm giá' : 'Có hàng' }}</span>
          <button class="btn btn-sm btn-outline" @click="delAlert(a)">Tắt</button>
        </div>
      </div>
    </div>

    <div v-if="addr" class="modal-bg" @click.self="addr = null">
      <div class="modal">
        <h3>{{ addr.id ? 'Sửa địa chỉ' : 'Thêm địa chỉ' }}</h3>
        <div v-if="msg.addr" class="alert alert-error">{{ msg.addr }}</div>
        <div class="form-grid" style="margin-top:12px">
          <div class="field"><label>Tên gợi nhớ</label><input v-model="addr.label" class="input" placeholder="Nhà riêng, Công ty..." /></div>
          <div class="field"><label>Người nhận *</label><input v-model="addr.receiverName" class="input" /></div>
          <div class="field"><label>Số điện thoại *</label><input v-model="addr.phone" class="input" /></div>
        </div>
        <AddressPicker v-model="addrPick" :cities="catalog.options.cities" />
        <label class="check field"><input v-model="addr.isDefault" type="checkbox" /> Đặt làm địa chỉ mặc định</label>
        <div class="row"><button class="btn" @click="saveAddr">Lưu</button><button class="btn btn-outline" @click="addr = null">Huỷ</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import Gauge from '../components/Gauge.vue'
import AddressPicker from '../components/AddressPicker.vue'
import VipCard from '../components/VipCard.vue'
import { toast } from '../stores/prefs'
const TIERS = [{ name: 'Đồng', label: 'mặc định', at: 0 }, { name: 'Bạc', label: 'chi 3 tr', at: 300 }, { name: 'Vàng', label: 'chi 10 tr', at: 1000 }, { name: 'Titan', label: 'chi 20 tr', at: 2000 }]
const selectedTier = ref('')
function formatTierName(t) {
  if (!t) return 'Đồng'
  if (t === 'Bạch kim' || t === 'Bạch Kim' || t === 'Kim cương') return 'Titan'
  return t
}
const activeCardTier = computed(() => {
  const val = selectedTier.value || pts.value?.tier || auth.user?.tier || 'Đồng'
  return formatTierName(val)
})

const adminTierPick = ref('Titan')
const adminLoading = ref(false)

const nextTierSpentReq = computed(() => {
  if (!nextTier.value) return 20000000
  if (nextTier.value.name === 'Bạc') return 3000000
  if (nextTier.value.name === 'Vàng') return 10000000
  if (nextTier.value.name === 'Titan') return 20000000
  return 20000000
})

async function adminApplyTier(tierName) {
  if (!auth.isAdmin) return
  adminLoading.value = true
  try {
    const res = await api.post('/account/admin-upgrade-tier', {
      tier: tierName
    })
    pts.value = res.data
    if (auth.user) {
      auth.user.tier = res.data.tier
      auth.user.points = res.data.points
    }
    selectedTier.value = ''
    toast(`Quản trị viên đã nâng thẻ lên Hạng ${tierName} VIP thành công!`)
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    adminLoading.value = false
  }
}

async function adminAddPoints(delta) {
  if (!auth.isAdmin) return
  adminLoading.value = true
  try {
    const res = await api.post('/account/admin-upgrade-tier', {
      pointsDelta: delta
    })
    pts.value = res.data
    if (auth.user) {
      auth.user.points = res.data.points
      auth.user.tier = res.data.tier
    }
    toast(`Đã cộng ${delta} điểm VIP thành công!`)
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    adminLoading.value = false
  }
}
import { useAuth } from '../stores/auth'
import { ago, catalog, loadCatalog, vnd, dt, d, ORDER_STATUS, PAY_METHOD, PAY_STATUS, AUCTION_STATUS, RETURN_STATUS, TICKET_STATUS } from '../format'

const auth = useAuth(), route = useRoute(), router = useRouter()
const tabs = { profile: 'Hồ sơ', addresses: 'Sổ địa chỉ', orders: 'Đơn hàng', points: 'Điểm thưởng', wallet: 'Ví & quà tặng', returns: 'Đổi trả', tickets: 'Hỗ trợ', bids: 'Đấu giá của tôi', settings: 'Cài đặt' }
const nextTier = computed(() => {
  const currentTierName = formatTierName(pts.value?.tier || auth.user?.tier || 'Đồng')
  if (currentTierName === 'Titan') return null
  const higher = TIERS.find(t => t.at > (pts.value.points || 0))
  return higher || null
})

function formatCardNum(numOrId, points) {
  if (numOrId && typeof numOrId === 'string' && numOrId.length >= 10) return numOrId
  const idStr = String(numOrId || auth.user?.id || 1).padStart(4, '0')
  const ptsSuffix = String((points || 0) + 1200).padStart(4, '0')
  return `8899 • 2026 • ${idStr} • ${ptsSuffix}`
}
function copyCardNum() {
  const code = formatCardNum(auth.user?.vipCardNumber || auth.user?.id, pts.value?.points)
  if (navigator.clipboard) {
    navigator.clipboard.writeText(code)
  }
  toast('Đã sao chép mã số thẻ VIP!')
}

const tab = ref(tabs[route.query.tab] ? route.query.tab : 'profile')
const profile = reactive({ fullName: auth.user.fullName, phone: auth.user.phone, avatarUrl: auth.user.avatarUrl })
const fileInput = ref(null)
const avatarLoading = ref(false)

function makeAvatarSvg(bg1, bg2, pathD) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100"><defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="${bg1}"/><stop offset="100%" stop-color="${bg2}"/></linearGradient></defs><rect width="100" height="100" rx="50" fill="url(#g)"/><circle cx="50" cy="50" r="42" fill="none" stroke="rgba(255,255,255,0.2)" stroke-width="2"/><g fill="none" stroke="#ffffff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" transform="translate(26, 26) scale(2)">${pathD}</g></svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

const PRESET_AVATARS = [
  { id: 'f1', name: 'F1 Racer', url: makeAvatarSvg('#e10600', '#ff4d4d', '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3"/><line x1="3" y1="12" x2="9" y2="12"/><line x1="15" y1="12" x2="21" y2="12"/><line x1="12" y1="3" x2="12" y2="9"/>') },
  { id: 'drift', name: 'Drift King', url: makeAvatarSvg('#1f2937', '#111827', '<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1zM4 22v-7"/>') },
  { id: 'hyper', name: 'Hyper Pilot', url: makeAvatarSvg('#ea580c', '#f97316', '<path d="M2 17h20M5 17l1.5-5h11l1.5 5M8 12l2-6h4l2 6"/>') },
  { id: 'crawler', name: 'Off-Road 4x4', url: makeAvatarSvg('#15803d', '#22c55e', '<polygon points="12 2 2 22 22 22"/><line x1="12" y1="2" x2="12" y2="22"/>') },
  { id: 'cyber', name: 'Cyber GT', url: makeAvatarSvg('#7c3aed', '#06b6d4', '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>') },
  { id: 'pit', name: 'Pit Crew', url: makeAvatarSvg('#b45309', '#f59e0b', '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>') },
  { id: 'trophy', name: 'Champion', url: makeAvatarSvg('#ca8a04', '#facc15', '<path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6M18 9h1.5a2.5 2.5 0 0 0 0-5H18M4 22h16M12 15V22M8 4h8v7a4 4 0 0 1-8 0V4z"/>') },
  { id: 'pilot', name: 'Stealth Driver', url: makeAvatarSvg('#0f172a', '#334155', '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><circle cx="12" cy="11" r="3"/>') }
]

function pickPreset(p) {
  profile.avatarUrl = p.url
  toast(`Đã chọn avatar "${p.name}". Bấm "Lưu thay đổi" để áp dụng!`)
}

function clearAvatar() {
  profile.avatarUrl = ''
  toast('Đã xoá ảnh đại diện. Bấm "Lưu thay đổi" để áp dụng.')
}

async function uploadAvatar(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { toast('Ảnh tối đa 5MB', 'error'); return }
  const fd = new FormData()
  fd.append('file', file)
  avatarLoading.value = true
  try {
    const { data } = await api.post('/uploads', fd)
    profile.avatarUrl = data.url
    toast('Đã tải ảnh lên thành công! Bấm "Lưu thay đổi" để áp dụng.')
  } catch (err) {
    toast(errMsg(err), 'error')
  } finally {
    avatarLoading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}
const pw = reactive({ currentPassword: '', newPassword: '' })
const bids = ref([])
const msg = reactive({}), addresses = ref([]), orders = ref([]), pts = ref({}), returnsList = ref([]), ticketsList = ref([]), addr = ref(null)

const payPref = ref(auth.user.preferredPayment || ''), myAlerts = ref([])
const savePay = async () => { try { await api.put('/account/payment-preference', { method: payPref.value }); auth.user.preferredPayment = payPref.value || null; toast('Đã lưu phương thức ưa thích') } catch (e) { toast(errMsg(e), 'error') } }
const delAlert = async a => { await api.delete(`/alerts/${a.id}`); myAlerts.value = myAlerts.value.filter(x => x !== a) }
const loadAlerts = async () => { const list = (await api.get('/alerts')).data; await Promise.all(list.map(async a => { try { a.name = (await api.get(`/products/${a.productId}`)).data.product.name } catch {} })); myAlerts.value = list }
const setTab = k => router.replace({ query: { tab: k } })
const w = ref({}), gcCode = ref(''), tf = ref(null), tfQr = ref(''), otp = ref('')
async function redeem() { try { const { data } = await api.post('/account/giftcards/redeem', { code: gcCode.value }); gcCode.value = ''; toast(`Đã nạp ${vnd(data.added)} vào ví`); load() } catch (e) { toast(errMsg(e), 'error') } }
const copyRef = () => { const u = `${location.origin}/register?ref=${w.value.referral.code}`; navigator.clipboard?.writeText(u); toast('Đã sao chép liên kết giới thiệu') }
async function setup2fa() {
  msg.tf = ''; tf.value = (await api.post('/account/2fa/setup')).data
  try { const QR = await import('qrcode'); tfQr.value = await QR.toDataURL(tf.value.otpauth, { margin: 1, width: 320 }) } catch { tfQr.value = '' }
}
async function on2fa() { try { await api.post('/account/2fa/enable', { code: otp.value }); auth.user.totpEnabled = true; localStorage.setItem('user', JSON.stringify(auth.user)); tf.value = null; otp.value = ''; msg.tf = 'Đã bật xác thực 2 bước'; msg.tfOk = true } catch (e) { msg.tf = errMsg(e); msg.tfOk = false } }
async function off2fa() { try { await api.post('/account/2fa/disable', { code: otp.value }); auth.user.totpEnabled = false; localStorage.setItem('user', JSON.stringify(auth.user)); otp.value = ''; msg.tf = 'Đã tắt xác thực 2 bước'; msg.tfOk = true } catch (e) { msg.tf = errMsg(e); msg.tfOk = false } }
async function makeDefault(a) { await api.put(`/account/addresses/${a.id}`, { ...a, isDefault: true }); toast('Đã đặt làm địa chỉ mặc định'); load() }
watch(() => route.query.tab, t => { tab.value = tabs[t] ? t : 'profile'; load() })

async function load() {
  if (tab.value === 'settings') loadAlerts()
  if (tab.value === 'wallet') w.value = (await api.get('/account/wallet')).data
  if (tab.value === 'addresses') { loadCatalog() }
  if (tab.value === 'addresses') addresses.value = (await api.get('/account/addresses')).data
  if (tab.value === 'orders') orders.value = (await api.get('/account/orders')).data
  if (tab.value === 'bids') bids.value = (await api.get('/account/bids')).data
  if (tab.value === 'points') pts.value = (await api.get('/account/points')).data
  if (tab.value === 'returns') returnsList.value = (await api.get('/account/returns')).data
  if (tab.value === 'tickets') ticketsList.value = (await api.get('/tickets')).data
}
async function saveProfile() {
  try {
    const res = await api.put('/account/profile', profile)
    auth.updateUser(res.data)
    msg.profile = 'Đã lưu thông tin và ảnh đại diện thành công!'
    toast('Đã cập nhật hồ sơ & ảnh đại diện!')
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}
async function changePw() {
  try { await api.put('/account/password', pw); msg.pw = 'Đã đổi mật khẩu'; msg.pwOk = true; pw.currentPassword = pw.newPassword = '' }
  catch (e) { msg.pw = errMsg(e); msg.pwOk = false }
}
const addrPick = ref({})
const editAddr = a => { msg.addr = ''; addr.value = a ? { ...a } : { receiverName: auth.user.fullName, phone: auth.user.phone, isDefault: false }; addrPick.value = { province: addr.value.city || '', district: addr.value.district || '', ward: addr.value.ward || '', line: addr.value.line || '' } }
watch(addrPick, v => { if (addr.value) Object.assign(addr.value, { city: v.province, district: v.district, ward: v.ward, line: v.line }) })
async function saveAddr() {
  const a = addr.value
  if (!a.receiverName || !a.phone || !a.line || !a.city) { msg.addr = 'Vui lòng nhập đủ các trường bắt buộc'; return }
  a.id ? await api.put(`/account/addresses/${a.id}`, a) : await api.post('/account/addresses', a)
  addr.value = null; load()
}
async function delAddr(a) { if (confirm('Xoá địa chỉ này?')) { await api.delete(`/account/addresses/${a.id}`); load() } }
onMounted(load)
</script>
<style scoped>
.wcard { display: flex; flex-direction: column; gap: 8px; background: linear-gradient(135deg, #111 0 62%, var(--accent) 62%); color: #fff; }
.wcard small { letter-spacing: .2em; text-transform: uppercase; font-size: .7rem; opacity: .8; } .wcard .bal { font: 700 2rem 'Oswald', sans-serif; } .wcard .muted { color: #ddd; } .wcard a { color: #fff; }
.wallet form { gap: 8px; flex-wrap: nowrap; } .wallet .input { min-width: 0; }
.refcode { display: flex; gap: 10px; align-items: center; margin: 10px 0; flex-wrap: wrap; } .refcode code { font: 700 1.3rem 'Oswald', sans-serif; letter-spacing: .12em; border: 2px dashed var(--accent); padding: 4px 12px; }
.gc { padding: 6px 0; border-bottom: 1px solid var(--border); gap: 8px; flex-wrap: wrap; }
.mail { border-bottom: 1px solid var(--border); padding: 6px 0; } .mail summary { cursor: pointer; font-size: .86rem; } .mail pre { white-space: pre-wrap; font: .82rem/1.5 inherit; font-family: inherit; background: var(--bg-2); padding: 10px; margin-top: 6px; }
.tf { display: flex; gap: 14px; align-items: flex-start; flex-wrap: wrap; } .tf img { background: #fff; padding: 6px; border: 1px solid var(--border); } .tf > div { flex: 1 1 200px; min-width: 0; }
.key { font-size: .85rem; word-break: break-all; background: var(--bg-2); padding: 2px 6px; } .tf form { gap: 8px; flex-wrap: nowrap; margin-top: 10px; } .tf .input { min-width: 0; }

.addr p { margin: 8px 0; }
.pcard { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.tiers { display: flex; gap: 6px; } .tiers span { padding: 6px 10px; border-radius: 8px; background: var(--bg-2); font-size: .82rem; text-align: center; } .tiers span.on { background: var(--bronze); color: #fff; } .tiers small { display: block; font-size: .68rem; opacity: .8; }
.ok { color: var(--success); font-weight: 700; } .err { color: var(--danger); font-weight: 700; }
.paypref { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-top: 10px; }
.pp { display: flex; align-items: center; gap: 8px; padding: 10px; border: 1.5px solid var(--border); margin: 0; cursor: pointer; } .pp.on { border-color: var(--accent); background: var(--bronze-soft); }
.alrow { padding: 8px 0; border-bottom: 1px solid var(--border); gap: 8px; } .alrow a { flex: 1; min-width: 0; }
.tabs { overflow-x: auto; flex-wrap: nowrap; scrollbar-width: none; } .tabs button { flex-shrink: 0; }

.avatar-manager { background: var(--bg-2); border-radius: 12px; padding: 14px; margin-bottom: 16px; border: 1px solid var(--border); }
.avatar-view-row { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.avatar-large { position: relative; width: 72px; height: 72px; border-radius: 50%; background: var(--accent); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 1.8rem; font-weight: 800; border: 3px solid var(--border); overflow: visible; flex-shrink: 0; box-shadow: 0 4px 12px rgba(0,0,0,.15); }
.avatar-large-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.avatar-large-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.avatar-upload-badge { position: absolute; right: -4px; bottom: -4px; width: 28px; height: 28px; border-radius: 50%; background: var(--text); color: var(--bg); border: 2px solid var(--surface); display: flex; align-items: center; justify-content: center; cursor: pointer; transition: transform .2s, background .2s; }
.avatar-upload-badge:hover { transform: scale(1.15); background: var(--accent); color: #fff; }
.avatar-meta { flex: 1; min-width: 200px; display: flex; flex-direction: column; gap: 4px; }
.avatar-meta b { font-size: .95rem; }
.avatar-btn-group { display: flex; gap: 8px; margin-top: 6px; flex-wrap: wrap; }
.presets-wrap { margin-top: 14px; padding-top: 12px; border-top: 1px dashed var(--border); }
.preset-items { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 8px; }
@media (max-width: 600px) { .preset-items { grid-template-columns: repeat(2, 1fr); } }
.preset-avatar-img { width: 34px; height: 34px; border-radius: 50%; display: block; }
.preset-item { display: flex; align-items: center; gap: 8px; padding: 6px 10px; border-radius: 8px; border: 1.5px solid var(--border); background: var(--surface); cursor: pointer; text-align: left; transition: all .2s; font-family: inherit; }
.preset-item:hover { border-color: var(--accent); transform: translateY(-1px); }
.preset-item.active { border-color: var(--accent); background: var(--bronze-soft); box-shadow: 0 0 0 1px var(--accent); }
.preset-emoji { font-size: 1.3rem; line-height: 1; }
.preset-item small { font-size: .75rem; font-weight: 600; color: var(--text); }


/* Thẻ hạng thành viên VIP ARES Ultra-Luxury Widescreen */
.vip-card-section {
  display: grid;
  grid-template-columns: 1.28fr 1fr;
  gap: 24px;
  align-items: stretch;
  margin-bottom: 14px;
}
@media (max-width: 990px) {
  .vip-card-section { grid-template-columns: 1fr; }
}

.vip-card-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.vip-member-card {
  width: 100%;
  max-width: 560px;
  aspect-ratio: 1.62 / 1;
  min-height: 315px;
  border-radius: 22px !important;
  padding: 26px 30px;
  color: #fff;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 28px 56px -12px rgba(0, 0, 0, 0.8), 0 0 0 1px rgba(255, 255, 255, 0.18);
  overflow: hidden;
  transition: transform .3s ease, box-shadow .3s ease;
  user-select: none;
}
.vip-member-card:hover {
  transform: translateY(-4px) scale(1.012);
  box-shadow: 0 36px 70px -12px rgba(0, 0, 0, 0.9), 0 0 0 1.5px rgba(255, 255, 255, 0.4);
}

/* Dynamic Tier Themes */
.card-tier-Đồng {
  background: radial-gradient(circle at 85% 15%, rgba(245, 158, 11, 0.3) 0%, transparent 55%),
              linear-gradient(135deg, #180b05 0%, #45200c 35%, #7c3a16 65%, #2a1307 100%);
  border: 1.8px solid #b45309;
  box-shadow: 0 24px 55px -10px rgba(120, 56, 20, 0.6), 0 0 0 1px rgba(245, 158, 11, 0.35);
}
.card-tier-Bạc {
  background: radial-gradient(circle at 85% 15%, rgba(241, 245, 249, 0.3) 0%, transparent 55%),
              linear-gradient(135deg, #090d16 0%, #334155 35%, #64748b 65%, #1e293b 100%);
  border: 1.8px solid #cbd5e1;
  box-shadow: 0 24px 55px -10px rgba(71, 85, 105, 0.6), 0 0 0 1px rgba(203, 213, 225, 0.4);
}
.card-tier-Vàng {
  background: radial-gradient(circle at 85% 15%, rgba(253, 224, 71, 0.38) 0%, transparent 55%),
              linear-gradient(135deg, #181002 0%, #78350f 30%, #b45309 55%, #d97706 75%, #1c1303 100%);
  border: 1.8px solid #fde047;
  box-shadow: 0 26px 60px -10px rgba(217, 119, 6, 0.7), 0 0 0 1.2px rgba(253, 224, 71, 0.5);
}
.card-tier-Bạch_kim, .card-tier-Bạch-kim, .card-tier-Kim_cương, .card-tier-Kim-cương {
  background: radial-gradient(circle at 85% 15%, rgba(255, 255, 255, 0.18) 0%, transparent 45%),
              radial-gradient(circle at 15% 85%, rgba(148, 163, 184, 0.15) 0%, transparent 50%),
              linear-gradient(135deg, #07080b 0%, #121319 32%, #1b1c25 58%, #111218 80%, #040507 100%);
  border: 1.8px solid #e2e8f0;
  box-shadow: 0 32px 70px -12px rgba(0, 0, 0, 0.95), 0 0 0 1.2px rgba(255, 255, 255, 0.35), inset 0 1px 2px rgba(255, 255, 255, 0.3);
}
.card-tier-Bạch_kim .thb-text, .card-tier-Kim_cương .thb-text {
  color: #f8fafc;
  text-shadow: 0 0 8px rgba(255,255,255,0.6);
}

.card-sheen {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 40%, rgba(255, 255, 255, 0.16) 50%, transparent 60%);
  pointer-events: none;
  animation: cardShimmer 7s infinite linear;
}
@keyframes cardShimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.card-mesh {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.07) 1.2px, transparent 1.2px);
  background-size: 8px 8px;
  pointer-events: none;
}

/* Card Top Row */
.card-top { display: flex; justify-content: space-between; align-items: center; position: relative; z-index: 2; }
.club-brand { display: flex; align-items: center; gap: 10px; }
.crest-icon { color: #facc15; filter: drop-shadow(0 2px 4px rgba(0,0,0,.7)); }
.club-title { font: 800 1.08rem 'Oswald', sans-serif; letter-spacing: .18em; display: block; line-height: 1.1; color: #fff; text-shadow: 0 2px 4px rgba(0,0,0,.8); }
.club-sub { font-size: .62rem; letter-spacing: .22em; color: rgba(255,255,255,.8); font-family: monospace; display: block; margin-top: 2px; }
.card-top-right { display: flex; align-items: center; gap: 12px; }
.tier-holo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 4px 14px;
  border-radius: 999px !important;
  backdrop-filter: blur(6px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  white-space: nowrap;
}
.thb-text { font: 800 .75rem 'Oswald', sans-serif; letter-spacing: .12em; color: #ffd54f; white-space: nowrap; }
.rfid-wave { opacity: .88; color: #fff; }

/* Card Mid Layout (Chip on Left, Large Avatar on Right) */
.card-mid-layout {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 2;
  margin: 10px 0;
  padding: 0 2px;
}
.card-mid-left { display: flex; flex-direction: column; gap: 4px; }
.emv-chip-wrap { display: flex; flex-direction: column; align-items: flex-start; }
.emv-chip {
  width: 56px;
  height: 42px;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #fef08a 0%, #eab308 45%, #ca8a04 100%);
  border: 1.2px solid #a16207;
  box-shadow: inset 0 1px 3px rgba(255,255,255,.9), 0 3px 8px rgba(0,0,0,.6);
  position: relative;
  overflow: hidden;
}
.chip-line { position: absolute; background: rgba(0,0,0,.28); }
.c1 { top: 50%; left: 0; right: 0; height: 1px; }
.c2 { top: 0; bottom: 0; left: 36%; width: 1px; }
.c3 { top: 0; bottom: 0; right: 36%; width: 1px; }
.chip-core {
  position: absolute;
  top: 25%;
  bottom: 25%;
  left: 36%;
  right: 36%;
  border: 1px solid rgba(0,0,0,.22);
  border-radius: 2px !important;
  background: rgba(254, 240, 138, 0.4);
}
.chip-meta { margin-top: 5px; }
.cm-label { font-size: .56rem; letter-spacing: .15em; color: rgba(255,255,255,.7); font-family: monospace; display: block; }

/* Enlarged Luxury Avatar Medallion */
.card-mid-right { display: flex; align-items: center; justify-content: center; }
.avatar-luxury-container { position: relative; display: flex; align-items: center; justify-content: center; }
.avatar-ring-outer {
  width: 92px;
  height: 92px;
  border-radius: 50% !important;
  padding: 3.5px;
  background: linear-gradient(135deg, #fef08a 0%, #ca8a04 50%, #fef08a 100%);
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.7), 0 10px 28px rgba(0, 0, 0, 0.85), inset 0 1px 2px rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-ring-inner {
  width: 100%;
  height: 100%;
  border-radius: 50% !important;
  overflow: hidden;
  background: #111;
  border: 2px solid rgba(0, 0, 0, 0.65);
  position: relative;
}
.avatar-photo {
  width: 100%;
  height: 100%;
  border-radius: 50% !important;
  object-fit: cover;
  display: block;
}
.avatar-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50% !important;
  background: linear-gradient(135deg, #374151, #111827);
  color: #facc15;
  display: grid;
  place-items: center;
  font: 800 2rem 'Oswald', sans-serif;
}
.avatar-vip-seal {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: linear-gradient(135deg, #fde047 0%, #ca8a04 100%);
  color: #111;
  font: 900 .65rem 'Oswald', sans-serif;
  letter-spacing: .08em;
  padding: 2.5px 8px;
  border-radius: 999px !important;
  border: 1.5px solid #fff;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.75);
}

/* Card Number Embossed */
.card-number-embossed {
  font-family: 'Share Tech Mono', 'Farrington 7B', 'OCR A Extended', 'Cinzel', monospace;
  font-size: clamp(1.05rem, 2.15vw, 1.32rem);
  font-weight: 700;
  letter-spacing: 0.20em;
  color: #f8fafc;
  text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.95), -1px -1px 0 rgba(255, 255, 255, 0.38), 0 0 14px rgba(255, 255, 255, 0.22);
  position: relative;
  z-index: 2;
  margin: 6px 0 8px;
  white-space: nowrap;
  overflow: visible;
  width: 100%;
}
.card-dragon-phoenix {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}
.dp-watermark {
  width: 100%;
  height: 100%;
  color: #f1f5f9;
}
.card-tier-Bạch_kim .dp-watermark, .card-tier-Kim_cương .dp-watermark {
  color: #e2e8f0;
  filter: drop-shadow(0 0 2px rgba(255,255,255,0.25));
}

/* Card Bottom Row */
.card-bottom { display: flex; justify-content: space-between; align-items: flex-end; position: relative; z-index: 2; padding: 0 2px; }
.cb-holder { flex: 1.2; }
.cb-label { font-size: .56rem; letter-spacing: .15em; color: rgba(255,255,255,.75); display: block; text-transform: uppercase; margin-bottom: 2px; }
.cb-name { font-size: .98rem; letter-spacing: .1em; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px; display: block; color: #fff; text-shadow: 0 1px 3px rgba(0,0,0,.8); }
.cb-since { text-align: center; margin: 0 14px; }
.cb-expiry { text-align: center; margin-right: 14px; }
.cb-val-text, .cb-val-exp { font-size: .94rem; letter-spacing: .1em; font-family: monospace; color: #fff; display: block; }
.cb-seal {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.28);
  padding: 3px 9px;
  border-radius: 6px !important;
  backdrop-filter: blur(4px);
}
.cb-seal-crest { font: 900 .72rem 'Oswald', sans-serif; letter-spacing: .15em; color: #fff; }
.cb-seal-sub { font-size: .52rem; letter-spacing: .18em; color: #ffd54f; font-family: monospace; }

/* Card Info Panel (Right side) */
.card-info-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 22px 24px;
}
.cip-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid var(--border);
  padding-bottom: 12px;
  margin-bottom: 14px;
}
.cip-header h3 { margin: 0; font-size: 1.15rem; }
.vip-status-pill {
  background: #dcfce7;
  color: #15803d;
  border: 1px solid #86efac;
  font-size: .72rem;
  font-weight: 800;
  padding: 3px 10px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}
.cip-grid { display: flex; flex-direction: column; gap: 10px; }
.cip-item { display: flex; justify-content: space-between; align-items: center; font-size: .88rem; }
.cip-lbl { color: var(--muted); font-weight: 600; font-size: .82rem; }
.cip-val-row { display: flex; align-items: center; gap: 8px; }
.card-code-mono { font-family: monospace; font-size: 1.02rem; font-weight: 800; letter-spacing: .06em; color: var(--text); }
.btn-copy { padding: 3px 8px; font-size: .75rem; font-weight: 600; cursor: pointer; }
.cip-tier-val { font-weight: 800; font-size: .95rem; }
.tier-text-Đồng { color: #b45309; }
.tier-text-Bạc { color: #475569; }
.tier-text-Vàng { color: #d97706; }
.tier-text-Titan, .tier-text-Bạch_kim, .tier-text-Bạch-kim { color: #18181b; font-weight: 800; }
.tier-text-Kim_cương, .tier-text-Kim-cương { color: #0284c7; }
.cip-pts-val { font-weight: 800; font-size: 1.15rem; color: var(--accent); }
.pts-cash { font-weight: 600; font-size: .82rem; color: var(--muted); margin-left: 4px; }
.cip-user-val, .cip-sub-val { font-size: .85rem; color: var(--text); text-align: right; max-width: 260px; }
.cip-actions { display: flex; gap: 10px; margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--border); }
.btn-spin { background: var(--accent); color: #fff; font-weight: 700; }
.btn-spin:hover { filter: brightness(1.08); }

/* Full-width Tier Privileges Section */
.full-width-tiers { padding: 22px 24px; }
.tier-current-tag {
  background: var(--bronze-soft, #fef3c7);
  border: 1px solid var(--bronze, #d97706);
  padding: 4px 12px;
  border-radius: 999px;
  font-size: .82rem;
  color: var(--text);
}
.tier-progress-box { background: var(--bg-2); border-radius: 10px; padding: 12px 14px; border: 1px solid var(--border); margin-bottom: 16px; }
.t-bar { height: 8px; background: rgba(0,0,0,.08); border-radius: 999px; overflow: hidden; }
.t-bar div { height: 100%; background: linear-gradient(90deg, var(--bronze), var(--accent)); transition: width .4s ease; }
.tiers-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
@media (max-width: 800px) { .tiers-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 500px) { .tiers-grid { grid-template-columns: 1fr; } }
.tier-col { background: var(--bg-2); border: 1px solid var(--border); border-radius: 12px; padding: 14px 12px; text-align: left; transition: all .2s; }
.tier-col.active { border: 2px solid var(--accent); background: var(--bronze-soft); box-shadow: 0 8px 24px rgba(240,90,26,.15); }
.tc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.tc-name { font-weight: 800; font-size: .95rem; }
.tc-badge-cur { background: var(--accent); color: #fff; font: 800 .62rem 'Oswald', sans-serif; letter-spacing: .06em; padding: 2px 6px; border-radius: 4px; }
.tc-req { color: var(--muted); font-size: .75rem; font-weight: 600; display: block; margin-bottom: 10px; border-bottom: 1px dashed var(--border); padding-bottom: 6px; }
.tc-perks { list-style: none; padding: 0; margin: 0; font-size: .8rem; display: flex; flex-direction: column; gap: 6px; line-height: 1.35; }
.tc-perks li { color: var(--text-2); display: flex; align-items: flex-start; gap: 4px; }
.perk-bullet { color: var(--accent); font-weight: 900; }

</style>
