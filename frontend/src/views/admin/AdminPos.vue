<template>
  <div class="pos-container">
    <!-- Top Bar: Shift & Quick Actions -->
    <div class="pos-topbar">
      <div class="shift-info">
        <div class="shift-badge" :class="{ 'shift-open': shift.open, 'shift-closed': !shift.open }">
          <span class="status-dot"></span>
          <span>{{ shift.open ? 'Ca đang mở: ' + shift.shiftCode : 'Chưa mở ca bán hàng' }}</span>
        </div>
        <div v-if="shift.open" class="shift-details">
          <span>Thu ngân: <b>{{ shift.staffName || auth.user?.fullName }}</b></span>
          <span>Két dự kiến: <b>{{ vnd(shift.expectedCash || shift.openingCash || 0) }}</b></span>
          <span>Doanh thu ca: <b class="text-accent">{{ vnd(shift.totalSales || 0) }}</b> ({{ shift.orderCount || 0 }} đơn)</span>
        </div>
      </div>

      <div class="topbar-actions">
        <!-- Multi-cart Tabs (Hold Bills) -->
        <div class="cart-tabs" role="tablist">
          <button
            v-for="(cart, idx) in heldCarts"
            :key="cart.id"
            :class="['cart-tab', { active: currentCartIndex === idx }]"
            @click="currentCartIndex = idx"
          >
            <span>Đơn #{{ idx + 1 }}</span>
            <small v-if="cart.items.length">({{ cart.items.length }})</small>
            <span
              v-if="heldCarts.length > 1"
              class="tab-close"
              title="Đóng đơn này"
              @click.stop="closeHeldCart(idx)"
            >&times;</span>
          </button>
          <button class="cart-tab add-tab" title="Tạo thêm đơn chờ mới" @click="addNewCart">+ Thêm đơn</button>
        </div>

        <button v-if="!shift.open" class="btn btn-sm btn-primary" @click="showOpenShiftModal = true">
          Mở ca làm việc
        </button>
        <button v-else class="btn btn-sm btn-outline" @click="showCloseShiftModal = true">
          Đóng ca / Bàn giao két
        </button>
        <button class="btn btn-sm btn-outline" @click="showRecentOrdersModal = true">
          Hóa đơn gần đây
        </button>
        <button class="btn btn-sm btn-outline staff-perf-btn" @click="openMyStatsModal">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
          <span>Hoa hồng & Hiệu suất</span>
          <span v-if="myStats?.pendingCommission > 0" class="badge-comm">+{{ vnd(myStats.pendingCommission) }}</span>
        </button>
        <button v-if="auth.isAdmin" class="btn btn-sm btn-outline manager-vault-btn" @click="openAllShiftsModal">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><circle cx="12" cy="12" r="3"/><line x1="12" y1="2" x2="12" y2="4"/><line x1="12" y1="20" x2="12" y2="22"/></svg>
          <span>Giám sát két quầy</span>
        </button>
        <button class="btn btn-sm btn-primary customer-disp-btn" @click="openCustomerDisplayModal">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
          <span>Màn hình phục vụ khách</span>
        </button>
      </div>
    </div>

    <!-- Main Workspace: Catalog (Left) + Cart & Checkout (Right) -->
    <div class="pos-main">
      <!-- Left Column: Product Catalog & Fast Search -->
      <div class="pos-catalog">
        <div class="catalog-header">
          <div class="search-box">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            <input
              ref="barcodeInput"
              v-model="searchQuery"
              class="input-search"
              placeholder="Quét mã vạch (Barcode) hoặc gõ SKU / tên xe bấm Enter..."
              @keyup.enter="handleBarcodeScan"
            />
            <button v-if="searchQuery" class="clear-btn" @click="searchQuery = ''; filterProducts()">&times;</button>
          </div>

          <!-- Quick Category Filters -->
          <div class="cat-filters">
            <button
              :class="['cat-btn', { active: selectedCat === '' }]"
              @click="selectedCat = ''; filterProducts()"
            >Tất cả ({{ productList.length }})</button>
            <button
              v-for="cat in categories"
              :key="cat"
              :class="['cat-btn', { active: selectedCat === cat }]"
              @click="selectedCat = cat; filterProducts()"
            >{{ cat }}</button>
          </div>
        </div>

        <!-- Product Grid -->
        <div v-if="loadingCatalog" class="loading-state">Đang tải danh mục hàng...</div>
        <div v-else-if="filteredProducts.length === 0" class="empty-catalog">
          Không tìm thấy sản phẩm phù hợp.
        </div>
        <div v-else class="product-grid">
          <div
            v-for="p in filteredProducts"
            :key="p.id"
            class="pos-card"
            :class="{ 'out-of-stock': p.stock <= 0 }"
            @click="onProductClick(p)"
          >
            <div class="card-img-wrap">
              <img :src="p.image || '/images/toys/default.jpg'" :alt="p.name" loading="lazy" />
              <span v-if="p.scale" class="scale-badge">{{ p.scale }}</span>
              <span v-if="p.variants?.length" class="variant-tag">{{ p.variants.length }} bản</span>
            </div>
            <div class="card-body">
              <div class="card-title" :title="p.name">{{ p.name }}</div>
              <div class="card-meta">
                <span class="sku-text">{{ p.sku }}</span>
                <span :class="['stock-text', { 'low-stock': p.stock <= 3 }]">Kho: {{ p.stock }}</span>
              </div>
              <div class="card-price price">{{ vnd(p.price) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: Active Cart & Cashier Checkout -->
      <div class="pos-cart">
                <!-- Cashier & Customer Info (Sắp xếp tinh gọn, không chiếm diện tích) -->
        <div class="cart-header-box">
          <!-- Top Row: Role Mode Tag & Staff Assignment -->
          <div class="pos-role-staff-row">
            <span v-if="auth.isAdmin" class="mode-tag manager-tag">QUẢN LÝ SHOWROOM</span>
            <span v-else class="mode-tag staff-tag">THU NGÂN & BÁN HÀNG</span>

            <div class="staff-picker-inline">
              <span class="staff-lbl-inline">NV:</span>
              <select v-model="activeCart.staffId" class="staff-select-inline" :disabled="!auth.isAdmin">
                <option v-for="st in staffList" :key="st.id" :value="st.id">
                  {{ st.fullName }} ({{ st.commissionRate || 2 }}%)
                </option>
              </select>
            </div>
          </div>

          <!-- Customer Picker (Gọn gàng trong 1 dòng) -->
          <div class="customer-picker-compact">
            <div v-if="!activeCart.customer" class="guest-compact-row">
              <span class="guest-pill">Khách lẻ</span>
              <input v-model="activeCart.customerName" class="input-pos-compact" placeholder="Tên khách lẻ..." />
              <input v-model="activeCart.customerPhone" class="input-pos-compact" placeholder="SĐT khách..." />
              <button class="btn-vip-lookup" @click="showCustomerSearch = true">+ VIP</button>
            </div>

            <div v-else class="vip-compact-row">
              <div class="vip-info-inline">
                <b>{{ activeCart.customer.fullName }}</b>
                <span class="tier-pill" :class="'tier-' + activeCart.customer.tier?.toLowerCase()">{{ activeCart.customer.tier }} VIP</span>
                <span class="points-tag">{{ activeCart.customer.points }} PTS</span>
              </div>
              <div class="vip-actions-inline">
                <label v-if="activeCart.customer.points > 0" class="pts-chk-label">
                  <input type="checkbox" v-model="activeCart.applyPoints" @change="onTogglePoints" /> Dùng điểm
                </label>
                <button class="btn-change-cust" @click="removeCustomer">Đổi</button>
              </div>
            </div>
          </div>

          <!-- Thẩm quyền Quản lý: Duyệt giảm giá đặc biệt (Accordion thu gọn để không chiếm diện tích) -->
          <details v-if="auth.isAdmin" class="manager-discount-accordion" :open="managerDiscountComputed > 0">
            <summary class="discount-accordion-summary">
              <span>Phê duyệt chiết khấu Quản lý</span>
              <span v-if="managerDiscountComputed > 0" class="badge badge-accent">-{{ vnd(managerDiscountComputed) }}</span>
              <span v-else class="summary-toggle-hint">Chọn % hoặc tiền giảm &darr;</span>
            </summary>
            <div class="discount-accordion-content">
              <div class="manager-preset-row">
                <button
                  v-for="pct in [5, 10, 15, 20, 30]"
                  :key="pct"
                  type="button"
                  :class="['preset-btn', { on: activeCart.managerDiscountPercent === pct }]"
                  @click="setManagerDiscountPercent(pct)"
                >
                  -{{ pct }}%
                </button>
                <button type="button" class="preset-btn reset-btn" @click="resetManagerDiscount">Hủy giảm</button>
              </div>
              <div class="manager-custom-row">
                <input
                  v-model.number="activeCart.managerDiscountAmount"
                  type="number"
                  min="0"
                  class="input-pos-compact"
                  placeholder="Hoặc số tiền giảm (đ)..."
                  @input="activeCart.managerDiscountPercent = 0"
                />
                <input
                  v-model="activeCart.managerDiscountReason"
                  class="input-pos-compact"
                  placeholder="Lý do duyệt giảm (Khách VIP, Đối tác...)"
                />
              </div>
            </div>
          </details>

          <!-- Tùy chọn Xuất hóa đơn VAT Doanh nghiệp (Thu gọn) -->
          <div class="vat-card-compact">
            <label class="vat-chk-label-compact">
              <input type="checkbox" v-model="activeCart.requireVatInvoice" />
              <span>Xuất hóa đơn GTGT / VAT (8%) cho Doanh nghiệp</span>
            </label>
            <div v-if="activeCart.requireVatInvoice" class="vat-fields-grid-compact">
              <input v-model="activeCart.companyName" class="input-pos-compact" placeholder="Tên công ty / Doanh nghiệp *" />
              <input v-model="activeCart.companyTaxCode" class="input-pos-compact" placeholder="Mã số thuế (MST) *" />
              <input v-model="activeCart.companyAddress" class="input-pos-compact" placeholder="Địa chỉ ĐKKD" />
              <input v-model="activeCart.companyEmail" class="input-pos-compact" placeholder="Email nhận HĐĐT" />
            </div>
          </div>
        </div>

<!-- Cart Items List -->
        <div class="cart-items-wrap">
          <div v-if="activeCart.items.length === 0" class="empty-cart-msg">
            Chưa có sản phẩm trong đơn. Quét barcode hoặc click chọn sản phẩm từ danh mục.
          </div>
          <div v-else class="cart-items-list">
            <div v-for="(it, idx) in activeCart.items" :key="idx" class="cart-item-row">
              <img :src="it.image || '/images/toys/default.jpg'" class="item-thumb" />
              <div class="item-info">
                <div class="item-name">{{ it.name }}</div>
                <small v-if="it.variantName" class="item-variant">{{ it.variantName }}</small>
                <div class="item-price-unit">{{ vnd(it.unitPrice) }}</div>
              </div>

              <!-- Quantity Controls -->
              <div class="qty-control">
                <button class="qty-btn" @click="changeQty(idx, -1)">-</button>
                <input
                  v-model.number="it.quantity"
                  type="number"
                  min="1"
                  :max="it.maxStock"
                  class="qty-input"
                  @change="validateQty(idx)"
                />
                <button class="qty-btn" :disabled="it.quantity >= it.maxStock" @click="changeQty(idx, 1)">+</button>
              </div>

              <div class="item-subtotal price">{{ vnd(it.unitPrice * it.quantity) }}</div>
              <button class="item-remove" title="Xóa món" @click="removeItem(idx)">&times;</button>
            </div>
          </div>
        </div>

        <!-- Checkout & Calculation Box -->
        <div class="cart-summary-box">
          <div class="summary-line">
            <span>Tạm tính ({{ totalItemCount }} món):</span>
            <b>{{ vnd(cartSubtotal) }}</b>
          </div>

          <div v-if="activeCart.customer?.tierDiscountPercent > 0" class="summary-line text-accent">
            <span>Chiết khấu VIP ({{ activeCart.customer.tierDiscountPercent }}%):</span>
            <span>-{{ vnd(tierDiscountAmount) }}</span>
          </div>

          <div v-if="activeCart.applyPoints && pointsDiscountAmount > 0" class="summary-line text-accent">
            <span>Dùng điểm ({{ pointsUsedCount }} điểm):</span>
            <span>-{{ vnd(pointsDiscountAmount) }}</span>
          </div>

          <div v-if="managerDiscountComputed > 0" class="summary-line text-accent">
            <span>Quản lý duyệt giảm ({{ activeCart.managerDiscountPercent ? activeCart.managerDiscountPercent + '%' : 'Đặc cách' }}):</span>
            <span>-{{ vnd(managerDiscountComputed) }}</span>
          </div>

          <div class="summary-line">
            <span>Voucher / Giảm thêm:</span>
            <input
              v-model.number="activeCart.manualDiscount"
              type="number"
              min="0"
              class="input-discount"
              placeholder="0đ"
            />
          </div>

          <div v-if="activeCart.requireVatInvoice && vatAmountComputed > 0" class="summary-line">
            <span>Thuế GTGT / VAT (8%):</span>
            <span>+{{ vnd(vatAmountComputed) }}</span>
          </div>

          <div class="summary-line grand-total-line">
            <span class="total-lbl">TỔNG THANH TOÁN:</span>
            <span class="total-val price">{{ vnd(finalTotal) }}</span>
          </div>

          <!-- Payment Methods Selection (Cổng Sandbox chính thức & Tiền mặt) -->
          <div class="payment-methods-grid">
            <button
              :class="['pay-btn', { active: activeCart.paymentMethod === 'VNPAY' }]"
              @click="setPaymentMethod('VNPAY')"
            >
              Cổng VNPay
            </button>
            <button
              :class="['pay-btn', { active: activeCart.paymentMethod === 'MOMO' }]"
              @click="setPaymentMethod('MOMO')"
            >
              Ví MoMo
            </button>
            <button
              :class="['pay-btn', { active: activeCart.paymentMethod === 'BANK_QR' }]"
              @click="setPaymentMethod('BANK_QR')"
            >
              VietQR Napas
            </button>
            <button
              :class="['pay-btn', { active: activeCart.paymentMethod === 'CASH' }]"
              @click="setPaymentMethod('CASH')"
            >
              Tiền mặt
            </button>
            <button
              :class="['pay-btn', { active: activeCart.paymentMethod === 'POS_CARD' }]"
              @click="setPaymentMethod('POS_CARD')"
            >
              Quẹt thẻ POS
            </button>
          </div>

          <!-- Cash Payment Interface -->
          <div v-if="activeCart.paymentMethod === 'CASH'" class="cash-input-section">
            <div class="quick-cash-row">
              <button class="quick-cash-btn" @click="setCashGiven(finalTotal)">Đủ tiền</button>
              <button class="quick-cash-btn" @click="setCashGiven(100000)">100k</button>
              <button class="quick-cash-btn" @click="setCashGiven(200000)">200k</button>
              <button class="quick-cash-btn" @click="setCashGiven(500000)">500k</button>
              <button class="quick-cash-btn" @click="setCashGiven(1000000)">1.000k</button>
              <button class="quick-cash-btn" @click="setCashGiven(2000000)">2.000k</button>
            </div>
            <div class="cash-calc-row">
              <label>Khách đưa:</label>
              <input
                v-model.number="activeCart.cashGiven"
                type="number"
                class="input input-cash"
                placeholder="Nhập số tiền khách đưa..."
              />
            </div>
            <div class="cash-change-row">
              <span>Tiền thừa trả khách:</span>
              <b :class="['change-amount', { 'text-danger': cashChange < 0 }]">
                {{ cashChange >= 0 ? vnd(cashChange) : 'Còn thiếu ' + vnd(Math.abs(cashChange)) }}
              </b>
            </div>
          </div>

<!-- VNPay Gateway Card -->
          <div v-else-if="activeCart.paymentMethod === 'VNPAY'" class="gateway-preview-section vnpay-theme">
            <div class="gw-card-header">
              <b>CỔNG THANH TOÁN VNPAY CHÍNH THỨC</b>
              <span class="badge badge-accent">VNPay-QR</span>
            </div>
            <p class="small muted">Thanh toán qua cổng VNPay bằng thẻ ATM nội địa hoặc quét mã VNPAY-QR.</p>
            <div class="test-card-quick-note">
              <small>Thẻ ATM test: <b>NCB · 9704198526191432198 · NGUYEN VAN A · 07/15 · OTP: 123456</b></small>
            </div>
            <div class="gw-actions-row" style="margin-top:10px">
              <button type="button" class="btn btn-sm btn-outline btn-block" @click="openCustomerDisplayModal">
                Mở màn hình phục vụ khách (Hiện QR VNPay)
              </button>
            </div>
          </div>

          <!-- MoMo Gateway Card -->
          <div v-else-if="activeCart.paymentMethod === 'MOMO'" class="gateway-preview-section momo-theme">
            <div class="gw-card-header">
              <b>CỔNG THANH TOÁN VÍ MOMO</b>
              <span class="badge badge-accent">Ví MoMo</span>
            </div>
            <p class="small muted">Thanh toán qua Ví MoMo trực tiếp tại quầy hoặc trên màn hình phục vụ khách.</p>
            <div class="gw-actions-row" style="margin-top:10px">
              <button type="button" class="btn btn-sm btn-outline btn-block" @click="openCustomerDisplayModal">
                Mở màn hình phục vụ khách (Hiện QR MoMo)
              </button>
            </div>
          </div>

          <!-- Bank QR Display (VietinBank Napas 24/7) -->
          <div v-else-if="activeCart.paymentMethod === 'BANK_QR'" class="qr-pay-section">
            <div class="qr-wrapper">
              <img
                :src="posBankInfo.qrUrl || `https://api.vietqr.io/image/970415-108876543210-compact2.png?amount=${finalTotal}&addInfo=${activeCartCode}&accountName=ARES+TOYS+SHOWROOM`"
                alt="VietQR Quầy"
                class="qr-img"
              />
              <div class="qr-caption">
                <b>Quét VietQR Napas247 tự động</b>
                <small>{{ posBankInfo.bankName || 'VietinBank' }} · {{ posBankInfo.accountNo || '108876543210' }} · {{ posBankInfo.accountHolder || 'ARES TOYS SHOWROOM' }}</small>
                <div class="price">{{ vnd(finalTotal) }}</div>
              </div>
            </div>
            <div style="margin-top:8px;text-align:center">
              <button type="button" class="btn btn-sm btn-outline btn-block" @click="openCustomerDisplayModal">
                Mở màn hình phục vụ khách (Hiện VietQR)
              </button>
            </div>
          </div>

          <!-- Card Swipe Display -->
          <div v-else-if="activeCart.paymentMethod === 'POS_CARD'" class="card-swipe-section">
            <div class="pos-terminal-box">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/><circle cx="7" cy="15" r="1"/></svg>
              <div>
                <b>Máy POS quẹt thẻ ngân hàng sẵn sàng</b>
                <p class="small muted">Vui lòng chạm thẻ chip contactless hoặc cắm thẻ vào máy POS cầm tay.</p>
              </div>
            </div>
          </div>

          <!-- Action Buttons (Single Unified Action Column) -->
          <div class="cart-actions-column">
            <button
              class="btn btn-primary btn-checkout-main"
              :disabled="activeCart.items.length === 0 || isSubmitting || (activeCart.paymentMethod === 'CASH' && cashChange < 0)"
              @click="submitPosOrder"
            >
              {{ isSubmitting ? 'ĐANG XỬ LÝ...' : (activeCart.paymentMethod === 'CASH' ? 'THANH TOÁN TIỀN MẶT & IN HÓA ĐƠN' : (activeCart.paymentMethod === 'VNPAY' ? 'XÁC NHẬN NHẬN TIỀN VNPAY & IN HÓA ĐƠN' : (activeCart.paymentMethod === 'MOMO' ? 'XÁC NHẬN NHẬN TIỀN MOMO & IN HÓA ĐƠN' : (activeCart.paymentMethod === 'BANK_QR' ? 'XÁC NHẬN NHẬN VIETQR & IN HÓA ĐƠN' : 'XÁC NHẬN QUẸT THẺ POS & IN HÓA ĐƠN')))) }}
            </button>
            <div class="cart-sub-actions-row">
              <button type="button" class="btn btn-outline btn-sub-action" title="Lưu tạm đơn hàng này" @click="holdCurrentCart">
                Lưu tạm
              </button>
              <button type="button" class="btn btn-outline text-danger btn-sub-action" title="Xóa toàn bộ đơn" @click="clearCurrentCart">
                Xóa đơn
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Variant Picker -->
    <div v-if="showVariantModal && currentProductForVariant" class="modal-scrim" @click.self="showVariantModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Chọn phiên bản: {{ currentProductForVariant.name }}</h3>
          <button class="close-btn" @click="showVariantModal = false">&times;</button>
        </div>
        <div class="variant-list">
          <div
            v-for="v in currentProductForVariant.variants"
            :key="v.id"
            class="variant-option-card"
            :class="{ disabled: v.stock <= 0 }"
            @click="v.stock > 0 && addVariantToCart(currentProductForVariant, v)"
          >
            <div>
              <b>{{ v.name }}</b>
              <small class="muted" style="display:block">Mã: {{ v.sku }} · Tồn: {{ v.stock }}</small>
            </div>
            <div class="price">{{ vnd(v.price) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Customer Search -->
    <div v-if="showCustomerSearch" class="modal-scrim" @click.self="showCustomerSearch = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Tra cứu khách hàng thành viên VIP</h3>
          <button class="close-btn" @click="showCustomerSearch = false">&times;</button>
        </div>
        <div class="customer-search-input-wrap">
          <input
            v-model="customerQuery"
            class="input"
            placeholder="Nhập SĐT, họ tên hoặc mã thẻ VIP..."
            @input="searchCustomers"
          />
        </div>
        <div class="customer-results-list">
          <div v-if="customerResults.length === 0" class="muted" style="padding:16px;text-align:center">
            {{ customerQuery.length < 2 ? 'Nhập tối thiểu 2 ký tự để tra cứu' : 'Không tìm thấy khách hàng' }}
          </div>
          <div
            v-for="c in customerResults"
            :key="c.id"
            class="customer-result-item"
            @click="selectCustomer(c)"
          >
            <div>
              <b>{{ c.fullName }}</b>
              <span class="tier-pill" :class="'tier-' + c.tier?.toLowerCase()">{{ c.tier }}</span>
              <div class="small muted">{{ c.phone }} · {{ c.email }}</div>
            </div>
            <div style="text-align:right">
              <b>{{ c.points }} điểm</b>
              <div class="small text-accent">Giảm {{ c.tierDiscountPercent }}% VIP</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Open Shift -->
    <div v-if="showOpenShiftModal" class="modal-scrim" @click.self="showOpenShiftModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Mở ca bán hàng tại quầy</h3>
          <button class="close-btn" @click="showOpenShiftModal = false">&times;</button>
        </div>
        <div class="shift-form">
          <label>Tiền mặt ban đầu trong két thu ngân (tiền lẻ đầu ca):</label>
          <input v-model.number="openCashInput" type="number" class="input" placeholder="Ví dụ: 2000000" />
          <div class="quick-cash-row" style="margin-top:8px">
            <button class="quick-cash-btn" @click="openCashInput = 1000000">1.000.000đ</button>
            <button class="quick-cash-btn" @click="openCashInput = 2000000">2.000.000đ</button>
            <button class="quick-cash-btn" @click="openCashInput = 3000000">3.000.000đ</button>
          </div>
          <label style="margin-top:12px">Ghi chú ca làm việc:</label>
          <input v-model="openShiftNotes" class="input" placeholder="Ví dụ: Ca sáng - Quầy thu ngân A" />
          <button class="btn btn-primary" style="margin-top:16px;width:100%" @click="confirmOpenShift">
            XÁC NHẬN MỞ CA
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: Close Shift -->
    <div v-if="showCloseShiftModal" class="modal-scrim" @click.self="showCloseShiftModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Đóng ca bán hàng & Bàn giao két</h3>
          <button class="close-btn" @click="showCloseShiftModal = false">&times;</button>
        </div>
        <div class="shift-summary-box">
          <div class="summary-line"><span>Mã ca:</span><b>{{ shift.shiftCode }}</b></div>
          <div class="summary-line"><span>Tiền đầu ca:</span><b>{{ vnd(shift.openingCash || 0) }}</b></div>
          <div class="summary-line"><span>Doanh thu tiền mặt trong ca:</span><b>{{ vnd(shift.cashSales || 0) }}</b></div>
          <div class="summary-line"><span>Doanh thu chuyển khoản QR:</span><b>{{ vnd(shift.bankQrSales || 0) }}</b></div>
          <div class="summary-line"><span>Doanh thu quẹt thẻ:</span><b>{{ vnd(shift.cardSales || 0) }}</b></div>
          <div class="summary-line grand-total-line">
            <span>Tiền mặt dự kiến có trong két:</span>
            <b class="price">{{ vnd(shift.expectedCash || shift.openingCash || 0) }}</b>
          </div>
        </div>
        <div class="shift-form" style="margin-top:14px">
          <label>Tiền mặt thực tế đếm được trong két cuối ca:</label>
          <input v-model.number="closeCashInput" type="number" class="input" placeholder="Nhập số tiền đếm được..." />
          <div v-if="closeCashInput !== null" class="summary-line" style="margin-top:8px">
            <span>Chênh lệch:</span>
            <b :class="closeCashDifference >= 0 ? 'text-accent' : 'text-danger'">
              {{ closeCashDifference >= 0 ? '+' + vnd(closeCashDifference) + ' (Thừa)' : vnd(closeCashDifference) + ' (Thiếu)' }}
            </b>
          </div>
          <label style="margin-top:10px">Ghi chú bàn giao ca:</label>
          <textarea v-model="closeShiftNotes" class="input" rows="2" placeholder="Ghi chú số tiền bàn giao cho ca sau..."></textarea>
          <button class="btn btn-danger" style="margin-top:14px;width:100%" @click="confirmCloseShift">
            CHỐT CA & BÀN GIAO KÉT
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: Recent POS Orders -->
    <div v-if="showRecentOrdersModal" class="modal-scrim" @click.self="showRecentOrdersModal = false">
      <div class="modal-card modal-large">
        <div class="modal-header">
          <h3>Hóa đơn bán tại quầy hôm nay</h3>
          <button class="close-btn" @click="showRecentOrdersModal = false">&times;</button>
        </div>
        <div class="table-wrap" style="max-height:420px;overflow-y:auto">
          <table class="table">
            <thead>
              <tr>
                <th>Mã HĐ</th>
                <th>Giờ</th>
                <th>Khách hàng</th>
                <th>Thu ngân</th>
                <th>Thanh toán</th>
                <th>Tổng tiền</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ro in recentPosOrders" :key="ro.id">
                <td><b>{{ ro.code }}</b></td>
                <td>{{ fmtTime(ro.createdAt) }}</td>
                <td>{{ ro.buyerName }}</td>
                <td>{{ ro.staffName }}</td>
                <td><span class="badge">{{ ro.posPaymentMethod }}</span></td>
                <td class="price">{{ vnd(ro.total) }}</td>
                <td>
                  <button class="btn btn-sm btn-outline" @click="reprintReceipt(ro)">In lại</button>
                </td>
              </tr>
              <tr v-if="recentPosOrders.length === 0">
                <td colspan="7" class="muted" style="text-align:center;padding:20px">Chưa có hóa đơn nào trong hôm nay.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Modal: Printable K80 Receipt -->
    <div v-if="showReceiptModal && receiptData" class="modal-scrim" @click.self="showReceiptModal = false">
      <div class="modal-card receipt-modal-box">
        <div class="modal-header no-print">
          <h3>Phiếu in hóa đơn bán lẻ K80</h3>
          <button class="close-btn" @click="showReceiptModal = false">&times;</button>
        </div>

        <!-- Thermal Receipt K80 Template (80mm width printable) -->
        <div id="pos-thermal-receipt" class="k80-receipt">
          <div class="receipt-brand">
            <h2 class="store-name">ARES TOYS SHOWROOM</h2>
            <p class="store-tagline">MÔ HÌNH XE CHÍNH HÃNG CAO CẤP</p>
            <p class="store-address">Tầng 1, Hoàng Huy Commerce, Võ Nguyên Giáp, Lê Chân, Hải Phòng</p>
            <p class="store-phone">Hotline: 0988.888.888 · Website: ares.vn</p>
          </div>

          <div class="receipt-divider">================================</div>

          <div class="receipt-meta">
            <div>Số HĐ: <b>{{ receiptData.code }}</b></div>
            <div>Ngày: {{ fmtFullDate(receiptData.createdAt) }}</div>
            <div>Thu ngân: {{ receiptData.staffName }}</div>
            <div>Khách hàng: {{ receiptData.buyerName }}</div>
            <div v-if="receiptData.phone">SĐT: {{ receiptData.phone }}</div>
          </div>

          <div class="receipt-divider">--------------------------------</div>

          <!-- Items Table -->
          <table class="receipt-table">
            <thead>
              <tr>
                <th style="text-align:left">Tên món</th>
                <th style="text-align:center">SL</th>
                <th style="text-align:right">Đơn giá</th>
                <th style="text-align:right">T.Tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(it, i) in receiptData.items" :key="i">
                <td class="item-cell">
                  <div>{{ it.productName }}</div>
                  <small v-if="it.variantName">({{ it.variantName }})</small>
                </td>
                <td style="text-align:center">{{ it.quantity }}</td>
                <td style="text-align:right">{{ vnd(it.unitPrice) }}</td>
                <td style="text-align:right"><b>{{ vnd(it.unitPrice * it.quantity) }}</b></td>
              </tr>
            </tbody>
          </table>

          <div class="receipt-divider">--------------------------------</div>

          <div class="receipt-calc">
            <div class="calc-row"><span>Tạm tính:</span><b>{{ vnd(receiptData.subtotal) }}</b></div>
            <div v-if="receiptData.discount > 0" class="calc-row"><span>Giảm giá:</span><b>-{{ vnd(receiptData.discount) }}</b></div>
            <div v-if="receiptData.pointsDiscount > 0" class="calc-row"><span>Điểm thưởng:</span><b>-{{ vnd(receiptData.pointsDiscount) }}</b></div>
            <div v-if="receiptData.managerDiscountAmount > 0" class="calc-row"><span>CK Quản lý duyệt:</span><b>-{{ vnd(receiptData.managerDiscountAmount) }}</b></div>
            <div v-if="receiptData.managerDiscountReason" class="calc-row receipt-subnote"><span>Lý do duyệt CK:</span><i>{{ receiptData.managerDiscountReason }}</i></div>
            <div v-if="receiptData.vatAmount > 0" class="calc-row"><span>Thuế GTGT (VAT 8%):</span><b>+{{ vnd(receiptData.vatAmount) }}</b></div>
            <div class="calc-row total-highlight"><span>TỔNG CỘNG:</span><b>{{ vnd(receiptData.total) }}</b></div>
            <div class="calc-row"><span>Hình thức:</span><b>{{ receiptData.posPaymentMethod }}</b></div>
            <div v-if="receiptData.posPaymentMethod === 'CASH'" class="calc-row"><span>Tiền khách đưa:</span><b>{{ vnd(receiptData.cashGiven) }}</b></div>
            <div v-if="receiptData.posPaymentMethod === 'CASH'" class="calc-row"><span>Tiền thừa:</span><b>{{ vnd(receiptData.cashChange) }}</b></div>
          </div>

          <div v-if="receiptData.requireVatInvoice" class="receipt-vat-block">
            <div class="receipt-divider">--------------------------------</div>
            <div style="font-weight:700;margin-bottom:3px">HÓA ĐƠN ĐIỆN TỬ GTGT (VAT)</div>
            <div>Đơn vị: <b>{{ receiptData.companyName }}</b></div>
            <div>MST: <b>{{ receiptData.companyTaxCode }}</b></div>
            <div v-if="receiptData.companyAddress">Đ/c: {{ receiptData.companyAddress }}</div>
            <div v-if="receiptData.companyEmail">Email nhận HĐ: {{ receiptData.companyEmail }}</div>
          </div>

          <div class="receipt-divider">================================</div>

          <div class="receipt-footer">
            <p>Cảm ơn Quý khách đã mua sắm tại ARES Toys!</p>
            <p class="small">Đổi trả sản phẩm trong vòng 7 ngày (giữ nguyên hộp và hóa đơn).</p>
            <p class="small">Quét mã QR để theo dõi bảo hành điện tử</p>
          </div>
        </div>

        <div class="receipt-modal-actions no-print">
          <button class="btn btn-primary" @click="printReceipt">IN HÓA ĐƠN K80</button>
          <button class="btn btn-outline" @click="showReceiptModal = false">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Modal: Staff Performance & Commission -->
    <div v-if="showMyStatsModal" class="modal-scrim" @click.self="showMyStatsModal = false">
      <div class="modal-card modal-large">
        <div class="modal-header">
          <div>
            <h3>Hiệu suất & Hoa hồng cá nhân</h3>
            <p class="muted small" style="margin:4px 0 0">
              {{ myStats?.fullName || auth.user?.fullName }} (Mã: {{ myStats?.staffCode || 'NV' }}) &middot; {{ myStats?.department || 'Tư vấn bán hàng trực tiếp' }}
            </p>
          </div>
          <button class="close-btn" @click="showMyStatsModal = false">&times;</button>
        </div>

        <!-- Personal Metrics Grid -->
        <div class="kpi-grid-modal">
          <div class="kpi-box">
            <span class="kpi-lbl">Hôm nay tại quầy</span>
            <b class="kpi-val text-accent">{{ vnd(myStats?.todaySales || 0) }}</b>
            <small class="muted">{{ myStats?.todayOrderCount || 0 }} đơn &middot; {{ myStats?.todayItemCount || 0 }} xe</small>
          </div>
          <div class="kpi-box">
            <span class="kpi-lbl">Doanh số tháng này</span>
            <b class="kpi-val">{{ vnd(myStats?.monthSales || 0) }}</b>
            <small class="muted">Mục tiêu: {{ vnd(myStats?.monthlySalesTarget || 50000000) }} (Đạt {{ myStats?.targetProgress || 0 }}%)</small>
            <div class="progress-bar-wrap">
              <div class="progress-bar-fill" :style="{ width: Math.min(100, myStats?.targetProgress || 0) + '%' }"></div>
            </div>
          </div>
          <div class="kpi-box">
            <span class="kpi-lbl">Tỷ lệ hoa hồng</span>
            <b class="kpi-val highlight-val">{{ myStats?.commissionRate || 2 }}%</b>
            <small class="muted">Cộng tự động mỗi đơn POS</small>
          </div>
          <div class="kpi-box highlight-kpi">
            <span class="kpi-lbl">Hoa hồng chờ quyết toán</span>
            <b class="kpi-val text-accent">{{ vnd(myStats?.pendingCommission || 0) }}</b>
            <small class="muted">Đã nhận: {{ vnd(myStats?.totalPaidCommission || 0) }} / Lũy kế: {{ vnd(myStats?.totalEarnedCommission || 0) }}</small>
          </div>
        </div>

        <!-- Commission History Table -->
        <h4 style="margin:16px 0 8px">Lịch sử tích lũy hoa hồng đơn bán tại quầy</h4>
        <div class="table-wrap" style="max-height:280px;overflow-y:auto">
          <table class="table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Mã đơn</th>
                <th>Loại giao dịch</th>
                <th>Tỷ lệ</th>
                <th>Tiền hoa hồng</th>
                <th>Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in myStats?.recentTransactions || []" :key="t.id">
                <td>{{ fmtFullDate(t.createdAt) }}</td>
                <td><b>{{ t.orderCode || 'N/A' }}</b></td>
                <td>
                  <span :class="['badge', t.txType === 'EARN' ? 'badge-earn' : 'badge-payout']">
                    {{ t.txType === 'EARN' ? 'Thưởng đơn' : (t.txType === 'PAYOUT' ? 'Quyết toán chi trả' : t.txType) }}
                  </span>
                </td>
                <td>{{ t.rate ? t.rate + '%' : '--' }}</td>
                <td :class="t.txType === 'EARN' ? 'price text-accent' : 'price text-danger'">
                  {{ t.txType === 'EARN' ? '+' : '-' }}{{ vnd(t.amount) }}
                </td>
                <td class="muted small">{{ t.note }}</td>
              </tr>
              <tr v-if="!myStats?.recentTransactions?.length">
                <td colspan="6" class="muted" style="text-align:center;padding:16px">Chưa có giao dịch hoa hồng nào được ghi nhận.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div style="margin-top:16px;display:flex;justify-content:flex-end">
          <button class="btn btn-outline" @click="showMyStatsModal = false">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Modal: Customer-Facing Display (Màn hình phục vụ khách hàng) -->
    <div v-if="showCustomerDisplayModal" class="modal-scrim" @click.self="closeCustomerDisplayModal">
      <div class="modal-card modal-customer-display">
        <div class="modal-header customer-display-header">
          <div class="cd-title-box">
            <span class="badge badge-accent">SHOWROOM DISPLAY</span>
            <h3>MÀN HÌNH PHỤC VỤ KHÁCH HÀNG TẠI QUẦY</h3>
            <p class="small muted">Màn hình hiển thị đối diện khách: Theo dõi đơn hàng & Thanh toán trực tiếp</p>
          </div>
          <div class="cd-header-actions">
            <span v-if="customerPaymentPaid" class="badge badge-success">THANH TOÁN THÀNH CÔNG</span>
            <span v-else class="badge badge-polling">ĐANG CHỜ THANH TOÁN</span>
            <button class="close-btn" @click="closeCustomerDisplayModal">&times;</button>
          </div>
        </div>

        <div class="customer-display-body">
          <!-- Left: Order Summary -->
          <div class="cd-order-summary">
            <div class="cd-panel-title">Chi tiết đơn hàng đang phục vụ</div>
            <div class="cd-items-list">
              <div v-for="(it, idx) in activeCart.items" :key="idx" class="cd-item-row">
                <div class="cd-item-img-wrap">
                  <img :src="it.image || '/src/assets/placeholder.png'" class="cd-item-thumb" />
                </div>
                <div class="cd-item-info">
                  <div class="cd-item-name">{{ it.name }}</div>
                  <small v-if="it.variantName" class="muted">{{ it.variantName }}</small>
                  <div class="cd-item-price-qty">{{ vnd(it.unitPrice) }} &times; <b>{{ it.quantity }}</b></div>
                </div>
                <div class="cd-item-total price">{{ vnd(it.unitPrice * it.quantity) }}</div>
              </div>
              <div v-if="!activeCart.items.length" class="muted cd-empty">Chưa có sản phẩm nào trong giỏ hàng.</div>
            </div>

            <div class="cd-calc-box">
              <div class="calc-row"><span>Tạm tính:</span><b>{{ vnd(cartSubtotal) }}</b></div>
              <div v-if="tierDiscountAmount > 0" class="calc-row">
                <span>Ưu đãi VIP ({{ activeCart.customer?.tier }}):</span>
                <b class="text-accent">-{{ vnd(tierDiscountAmount) }}</b>
              </div>
              <div v-if="pointsDiscountAmount > 0" class="calc-row">
                <span>Dùng điểm tích lũy:</span>
                <b class="text-accent">-{{ vnd(pointsDiscountAmount) }}</b>
              </div>
              <div v-if="managerDiscountComputed > 0" class="calc-row">
                <span>Chiết khấu Quản lý duyệt:</span>
                <b class="text-accent">-{{ vnd(managerDiscountComputed) }}</b>
              </div>
              <div v-if="activeCart.requireVatInvoice && vatAmountComputed > 0" class="calc-row">
                <span>Thuế GTGT / VAT (8%):</span>
                <b>+{{ vnd(vatAmountComputed) }}</b>
              </div>
              <div class="calc-row cd-grand-total">
                <span>TỔNG TIỀN THANH TOÁN:</span>
                <span class="price cd-total-val">{{ vnd(finalTotal) }}</span>
              </div>
            </div>

            <div v-if="activeCart.customer" class="cd-customer-card">
              <div class="cd-cust-tag">Khách hàng thành viên</div>
              <div class="cd-cust-name">{{ activeCart.customer.fullName }} · <span class="text-accent">{{ activeCart.customer.tier }}</span></div>
              <div class="small muted">SĐT: {{ activeCart.customer.phone || 'N/A' }} · Tích luỹ: {{ activeCart.customer.points }} điểm</div>
            </div>
          </div>

          <!-- Right: Official Sandbox Gateway / Payment Section -->
          <div class="cd-payment-area">
            <!-- Payment Success Overlay -->
            <div v-if="customerPaymentPaid" class="cd-success-screen">
              <div class="cd-success-icon">
                <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
              </div>
              <h2>GIAO DỊCH THÀNH CÔNG!</h2>
              <p>Hệ thống đã nhận đủ <b>{{ vnd(finalTotal) }}</b> qua {{ activeCart.paymentMethod }}.</p>
              <p class="small muted">Hóa đơn bán lẻ đang được in tại quầy. Cảm ơn Quý khách!</p>
              <button class="btn btn-primary" style="margin-top:20px" @click="closeCustomerDisplayModal">Đóng màn hình</button>
            </div>

            <!-- Active Gateway Display -->
            <div v-else class="cd-gateway-wrapper">
              <!-- VNPay Sandbox -->
              <div v-if="activeCart.paymentMethod === 'VNPAY'" class="cd-vnpay-box">
                <div class="cd-gw-header vnpay-header">
                  <span class="cd-gw-badge">CỔNG THANH TOÁN VNPAY</span>
                  <h4>Cổng thanh toán VNPay</h4>
                  <p class="small">Quét mã VNPAY-QR bằng app ngân hàng hoặc thanh toán bằng thẻ ATM test</p>
                </div>

                <div class="cd-gw-interactive">
                  <div class="cd-test-card-panel" style="margin-bottom:14px">
                    <div class="cd-card-title">THÔNG TIN THẺ TEST ATM NCB</div>
                    <div class="test-card-grid">
                      <div><span>Ngân hàng:</span><b>NCB (Quốc Dân)</b></div>
                      <div><span>Số thẻ:</span><b>9704198526191432198</b></div>
                      <div><span>Tên chủ thẻ:</span><b>NGUYEN VAN A</b></div>
                      <div><span>Ngày phát hành:</span><b>07/15</b></div>
                      <div><span>Mật khẩu OTP:</span><b>123456</b></div>
                      <div><span>Số tiền:</span><b class="price">{{ vnd(finalTotal) }}</b></div>
                    </div>
                  </div>

                  <div class="cd-vnpay-actions">
                    <button class="btn btn-primary btn-gateway-open" :disabled="activeCart.items.length === 0 || isSubmitting" @click="confirmPosPaidAndPrint">
                      XÁC NHẬN THANH TOÁN VNPAY (NHẬN ĐỦ TIỀN) &rarr;
                    </button>
                  </div>
                </div>
              </div>

              <!-- MoMo Sandbox -->
              <div v-else-if="activeCart.paymentMethod === 'MOMO'" class="cd-momo-box">
                <div class="cd-gw-header momo-header">
                  <span class="cd-gw-badge">CỔNG THANH TOÁN VÍ MOMO</span>
                  <h4>Cổng thanh toán Ví MoMo</h4>
                  <p class="small">Mở ứng dụng MoMo quét mã QR bên dưới hoặc mở cổng thanh toán MoMo</p>
                </div>

                <div class="cd-gw-interactive">
                  <div class="cd-qr-center" v-if="customerDisplayQrUrl">
                    <img :src="customerDisplayQrUrl" class="cd-momo-qr" alt="Mã QR Ví MoMo" />
                    <div class="small muted">Quét bằng ứng dụng Ví MoMo để thanh toán</div>
                  </div>
                  <div class="cd-vnpay-actions" style="margin-top:16px">
                    <button class="btn btn-primary btn-gateway-open" :disabled="activeCart.items.length === 0 || isSubmitting" @click="confirmPosPaidAndPrint">
                      XÁC NHẬN THANH TOÁN VÍ MOMO (NHẬN ĐỦ TIỀN) &rarr;
                    </button>
                  </div>
                </div>
              </div>

              <!-- VietQR Napas247 (VietinBank Auto-Confirm) -->
              <div v-else-if="activeCart.paymentMethod === 'BANK_QR'" class="cd-vietqr-box">
                <div class="cd-gw-header qr-header">
                  <span class="cd-gw-badge">VIETQR NAPAS247</span>
                  <h4>Quét mã VietQR chuyển khoản 24/7</h4>
                  <p class="small">Mở ứng dụng VietinBank hoặc bất kỳ app ngân hàng nào để quét mã tự động xác nhận</p>
                </div>
                <div class="cd-qr-center">
                  <img
                    :src="posBankInfo.qrUrl || `https://api.vietqr.io/image/970415-108876543210-compact2.png?amount=${finalTotal}&addInfo=${activeCartCode}&accountName=ARES+TOYS+SHOWROOM`"
                    class="cd-vietqr-img"
                    alt="VietQR"
                  />
                  <div class="vietqr-details">
                    <div>Ngân hàng: <b>{{ posBankInfo.bankName || 'VietinBank' }} (BIN: {{ posBankInfo.bankBin || '970415' }})</b></div>
                    <div>Số tài khoản: <b>{{ posBankInfo.accountNo || '108876543210' }}</b></div>
                    <div>Chủ tài khoản: <b>{{ posBankInfo.accountHolder || 'ARES TOYS SHOWROOM' }}</b></div>
                    <div>Số tiền: <b class="price">{{ vnd(finalTotal) }}</b></div>
                    <div>Nội dung CK: <b>{{ activeCartCode }}</b></div>
                  </div>
                  <div class="dev-simulate-bar" style="margin-top:12px;width:100%">
                    <button type="button" class="btn btn-sm btn-outline btn-block text-accent" :disabled="isSimulatingTransfer" @click="simulateBankTransferPaid">
                      {{ isSimulatingTransfer ? 'Đang gửi thông báo ngân hàng...' : 'Mô phỏng ngân hàng VietinBank báo Có (Test tự động)' }}
                    </button>
                  </div>
                  <button class="btn btn-primary btn-block" style="margin-top:10px" @click="confirmPosPaidAndPrint">
                    XÁC NHẬN ĐÃ NHẬN CHUYỂN KHOẢN & IN HÓA ĐƠN
                  </button>
                </div>
              </div>

              <!-- CASH -->
              <div v-else-if="activeCart.paymentMethod === 'CASH'" class="cd-cash-box">
                <div class="cd-gw-header cash-header">
                  <span class="cd-gw-badge">TIỀN MẶT TẠI QUẦY</span>
                  <h4>Thanh toán tiền mặt cho thu ngân</h4>
                </div>
                <div class="cd-cash-summary">
                  <div class="calc-row"><span>Tổng tiền:</span><b class="price">{{ vnd(finalTotal) }}</b></div>
                  <div class="calc-row"><span>Tiền khách đưa:</span><b>{{ vnd(activeCart.cashGiven || finalTotal) }}</b></div>
                  <div class="calc-row"><span>Tiền thừa trả khách:</span><b class="text-accent">{{ vnd(Math.max(0, (activeCart.cashGiven || finalTotal) - finalTotal)) }}</b></div>
                </div>
                <button class="btn btn-primary" style="margin-top:20px;width:100%" @click="confirmPosPaidAndPrint">
                  HOÀN TẤT THU TIỀN MẶT & IN HÓA ĐƠN K80
                </button>
              </div>

              <!-- POS Card -->
              <div v-else class="cd-poscard-box">
                <div class="cd-gw-header card-header">
                  <span class="cd-gw-badge">QUẸT THẺ POS</span>
                  <h4>Thanh toán quẹt thẻ máy POS</h4>
                </div>
                <p>Vui lòng chạm hoặc cắm thẻ vào thiết bị quẹt thẻ cầm tay tại quầy.</p>
                <div class="calc-row"><span>Số tiền quẹt:</span><b class="price">{{ vnd(finalTotal) }}</b></div>
                <button class="btn btn-primary" style="margin-top:20px;width:100%" @click="confirmPosPaidAndPrint">
                  XÁC NHẬN ĐÃ QUẸT THẺ THÀNH CÔNG & IN HÓA ĐƠN
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal: Manager All Shifts & Vault Monitoring (Chỉ dành cho Quản lý Showroom) -->
    <div v-if="showAllShiftsModal && auth.isAdmin" class="modal-scrim" @click.self="showAllShiftsModal = false">
      <div class="modal-card modal-xlarge">
        <div class="modal-header">
          <div>
            <h3>Giám sát két tiền & Lịch sử ca làm việc toàn Showroom</h3>
            <p class="muted small" style="margin:4px 0 0">Quyền Quản lý cấp cao: Theo dõi tiền két, chênh lệch bàn giao và toàn bộ thu ngân</p>
          </div>
          <button class="close-btn" @click="showAllShiftsModal = false">&times;</button>
        </div>

        <!-- Vault KPI Row -->
        <div class="kpi-grid-modal" style="margin-bottom:18px">
          <div class="kpi-box">
            <span class="kpi-lbl">Tổng ca đã mở</span>
            <b class="kpi-val">{{ allShiftsList.length }} ca</b>
            <small class="muted">Toàn bộ nhân sự thu ngân</small>
          </div>
          <div class="kpi-box highlight-kpi">
            <span class="kpi-lbl">Tiền mặt dự kiến có trong két</span>
            <b class="kpi-val text-accent">{{ vnd(totalVaultExpectedCash) }}</b>
            <small class="muted">Tổng cộng các ca đang mở & đã chốt</small>
          </div>
          <div class="kpi-box">
            <span class="kpi-lbl">Tổng doanh thu bán tại quầy</span>
            <b class="kpi-val">{{ vnd(totalVaultAllSales) }}</b>
            <small class="muted">Tất cả phương thức thanh toán</small>
          </div>
        </div>

        <!-- All Shifts Table -->
        <div class="table-wrap" style="max-height:400px;overflow-y:auto">
          <table class="table">
            <thead>
              <tr>
                <th>Mã ca</th>
                <th>Thu ngân phụ trách</th>
                <th>Bắt đầu</th>
                <th>Kết thúc</th>
                <th>Tiền đầu ca</th>
                <th>Doanh thu TM</th>
                <th>Doanh thu QR/Ví</th>
                <th>Doanh thu Thẻ</th>
                <th>Két dự kiến</th>
                <th>Két thực đếm</th>
                <th>Chênh lệch</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in allShiftsList" :key="s.id">
                <td><b>{{ s.shiftCode }}</b></td>
                <td>{{ s.staffName || 'Thu ngân' }}</td>
                <td>{{ fmtFullDate(s.startTime) }}</td>
                <td>{{ s.endTime ? fmtFullDate(s.endTime) : 'Đang mở...' }}</td>
                <td>{{ vnd(s.openingCash || 0) }}</td>
                <td class="price">{{ vnd(s.cashSales || 0) }}</td>
                <td>{{ vnd(s.bankQrSales || 0) }}</td>
                <td>{{ vnd(s.cardSales || 0) }}</td>
                <td><b>{{ vnd(s.expectedCash || 0) }}</b></td>
                <td>{{ s.closingCash !== null && s.closingCash !== undefined ? vnd(s.closingCash) : '--' }}</td>
                <td>
                  <span v-if="s.cashDifference !== null && s.cashDifference !== undefined" :class="s.cashDifference >= 0 ? 'text-accent' : 'text-danger'">
                    {{ s.cashDifference >= 0 ? '+' + vnd(s.cashDifference) : vnd(s.cashDifference) }}
                  </span>
                  <span v-else class="muted">--</span>
                </td>
                <td>
                  <span :class="['badge', s.status === 'OPEN' ? 'badge-open' : 'badge-closed']">
                    {{ s.status === 'OPEN' ? 'Đang mở ca' : 'Đã chốt ca' }}
                  </span>
                </td>
              </tr>
              <tr v-if="!allShiftsList.length">
                <td colspan="12" class="muted" style="text-align:center;padding:20px">Chưa có dữ liệu ca làm việc nào.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div style="margin-top:16px;display:flex;justify-content:flex-end">
          <button class="btn btn-outline" @click="showAllShiftsModal = false">Đóng</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useAuth } from '../../stores/auth'
import api from '../../api'
import { vnd } from '../../format'

const auth = useAuth()
const barcodeInput = ref(null)

// Data state
const productList = ref([])
const filteredProducts = ref([])
const categories = ref([])
const staffList = ref([])
const selectedCat = ref('')
const searchQuery = ref('')
const loadingCatalog = ref(false)
const isSubmitting = ref(false)

// Shifts
const shift = ref({ open: false })
const showOpenShiftModal = ref(false)
const showCloseShiftModal = ref(false)
const openCashInput = ref(2000000)
const openShiftNotes = ref('')
const closeCashInput = ref(null)
const closeShiftNotes = ref('')

const closeCashDifference = computed(() => {
  if (closeCashInput.value === null) return 0
  const expected = shift.value.expectedCash || shift.value.openingCash || 0
  return closeCashInput.value - expected
})

// Multi-cart (Held bills)
const currentCartIndex = ref(0)
const heldCarts = ref([createNewCartInstance()])
const activeCart = computed(() => heldCarts.value[currentCartIndex.value] || heldCarts.value[0])

function createNewCartInstance() {
  return {
    id: Date.now() + Math.random(),
    cartCode: 'POS' + Math.floor(100000 + Math.random() * 900000),
    staffId: auth.user?.id || null,
    customer: null,
    customerName: '',
    customerPhone: '',
    items: [],
    applyPoints: false,
    manualDiscount: 0,
    managerDiscountMode: 'percent',
    managerDiscountPercent: 0,
    managerDiscountCustomAmount: 0,
    managerDiscountReason: '',
    requireVatInvoice: false,
    companyName: '',
    companyTaxCode: '',
    companyAddress: '',
    companyEmail: '',
    paymentMethod: 'CASH',
    cashGiven: 0,
    note: ''
  }
}

function addNewCart() {
  heldCarts.value.push(createNewCartInstance())
  currentCartIndex.value = heldCarts.value.length - 1
  focusBarcode()
}

function closeHeldCart(idx) {
  if (heldCarts.value.length <= 1) return
  heldCarts.value.splice(idx, 1)
  if (currentCartIndex.value >= heldCarts.value.length) {
    currentCartIndex.value = heldCarts.value.length - 1
  }
}

function holdCurrentCart() {
  addNewCart()
}

function clearCurrentCart() {
  heldCarts.value[currentCartIndex.value] = createNewCartInstance()
  focusBarcode()
}

// Modals
const showVariantModal = ref(false)
const currentProductForVariant = ref(null)
const showCustomerSearch = ref(false)
const customerQuery = ref('')
const customerResults = ref([])
const showRecentOrdersModal = ref(false)
const recentPosOrders = ref([])
const showReceiptModal = ref(false)
const receiptData = ref(null)
const showMyStatsModal = ref(false)
const myStats = ref(null)

// Customer-facing display & Sandbox Gateway
const showCustomerDisplayModal = ref(false)
const customerPaymentPaid = ref(false)
const customerDisplayPayUrl = ref('')
const posBankInfo = ref({
  bankName: 'VietinBank',
  bankBin: '970415',
  accountNo: '108876543210',
  accountHolder: 'ARES TOYS SHOWROOM',
  qrUrl: 'https://api.vietqr.io/image/970415-108876543210-compact2.png'
})
const isSimulatingTransfer = ref(false)
const customerDisplayQrUrl = ref('')
const isCheckingStatus = ref(false)
let pollPaymentTimer = null

// Manager All Shifts
const showAllShiftsModal = ref(false)
const allShiftsList = ref([])

const totalVaultExpectedCash = computed(() => {
  return allShiftsList.value.reduce((s, it) => s + (it.expectedCash || it.openingCash || 0), 0)
})

const totalVaultAllSales = computed(() => {
  return allShiftsList.value.reduce((s, it) => s + (it.totalSales || 0), 0)
})

// Cart Computations
const activeCartCode = computed(() => {
  return activeCart.value?.cartCode || 'POS100001'
})

const totalItemCount = computed(() => {
  return activeCart.value.items.reduce((s, it) => s + it.quantity, 0)
})

const cartSubtotal = computed(() => {
  return activeCart.value.items.reduce((s, it) => s + (it.unitPrice * it.quantity), 0)
})

const tierDiscountAmount = computed(() => {
  if (!activeCart.value.customer || !activeCart.value.customer.tierDiscountPercent) return 0
  return Math.round((cartSubtotal.value * activeCart.value.customer.tierDiscountPercent) / 100.0)
})

const managerDiscountComputed = computed(() => {
  if (!activeCart.value) return 0
  if (activeCart.value.managerDiscountMode === 'percent') {
    const pct = Number(activeCart.value.managerDiscountPercent) || 0
    if (pct <= 0) return 0
    return Math.round((cartSubtotal.value * pct) / 100.0)
  } else {
    return Math.max(0, Number(activeCart.value.managerDiscountCustomAmount) || 0)
  }
})

const maxPointsCanUse = computed(() => {
  if (!activeCart.value.customer || !activeCart.value.customer.points) return 0
  const afterTier = Math.max(0, cartSubtotal.value - tierDiscountAmount.value - (activeCart.value.manualDiscount || 0) - managerDiscountComputed.value)
  const maxByAmount = Math.floor(afterTier / 1000)
  return Math.min(activeCart.value.customer.points, maxByAmount)
})

const pointsUsedCount = computed(() => {
  if (!activeCart.value.applyPoints) return 0
  return maxPointsCanUse.value
})

const pointsDiscountAmount = computed(() => {
  return pointsUsedCount.value * 1000
})

const vatAmountComputed = computed(() => {
  if (!activeCart.value?.requireVatInvoice) return 0
  const after = Math.max(0, cartSubtotal.value - tierDiscountAmount.value - pointsDiscountAmount.value - (activeCart.value.manualDiscount || 0) - managerDiscountComputed.value)
  return Math.round(after * 0.08)
})

const finalTotal = computed(() => {
  const t = cartSubtotal.value - tierDiscountAmount.value - pointsDiscountAmount.value - (activeCart.value.manualDiscount || 0) - managerDiscountComputed.value + vatAmountComputed.value
  return Math.max(0, t)
})

const cashChange = computed(() => {
  const given = activeCart.value.cashGiven || 0
  return given - finalTotal.value
})

function setCashGiven(amt) {
  activeCart.value.cashGiven = amt
}

function setManagerDiscountPercent(pct) {
  activeCart.value.managerDiscountMode = 'percent'
  activeCart.value.managerDiscountPercent = pct
}

function resetManagerDiscount() {
  activeCart.value.managerDiscountPercent = 0
  activeCart.value.managerDiscountCustomAmount = 0
  activeCart.value.managerDiscountReason = ''
}

function setPaymentMethod(method) {
  activeCart.value.paymentMethod = method
  if (method === 'VNPAY' || method === 'MOMO' || method === 'BANK_QR') {
    generateSandboxPayment()
  }
}

async function generateSandboxPayment() {
  try {
    const method = activeCart.value.paymentMethod
    const res = await api.post('/admin/pos/generate-sandbox-pay', {
      method: method,
      amount: finalTotal.value,
      code: activeCartCode.value
    })
    customerDisplayPayUrl.value = res.data.paymentUrl || ''
    customerDisplayQrUrl.value = res.data.qrCodeUrl || ''
    if (res.data.bankName) {
      posBankInfo.value = {
        bankName: res.data.bankName || 'VietinBank',
        bankBin: res.data.bankBin || '970415',
        accountNo: res.data.accountNo || '108876543210',
        accountHolder: res.data.accountHolder || 'ARES TOYS SHOWROOM',
        qrUrl: res.data.vietinbankQr || res.data.qrCodeUrl || ''
      }
    }
  } catch (err) {
    console.error('Lỗi tạo URL thanh toán sandbox:', err)
  }
}

async function simulateBankTransferPaid() {
  if (isSimulatingTransfer.value) return
  isSimulatingTransfer.value = true
  try {
    await api.post('/payment/dev/simulate-transfer', {
      orderCode: activeCartCode.value,
      amount: finalTotal.value
    })
    toast('Đã mô phỏng chuyển khoản thành công vào tài khoản VietinBank! Đang tự động đối soát...')
    await checkPaymentNow()
  } catch (err) {
    console.error('Lỗi mô phỏng chuyển khoản:', err)
    toast('Lỗi mô phỏng chuyển khoản: ' + errMsg(err))
  } finally {
    isSimulatingTransfer.value = false
  }
}

async function openCustomerDisplayModal() {
  customerPaymentPaid.value = false
  if (activeCart.value.paymentMethod === 'VNPAY' || activeCart.value.paymentMethod === 'MOMO') {
    await generateSandboxPayment()
  }
  showCustomerDisplayModal.value = true
  startPaymentPolling()
}

function closeCustomerDisplayModal() {
  showCustomerDisplayModal.value = false
  stopPaymentPolling()
}

function startPaymentPolling() {
  stopPaymentPolling()
  pollPaymentTimer = setInterval(async () => {
    await checkPaymentNow()
  }, 2500)
}

function stopPaymentPolling() {
  if (pollPaymentTimer) {
    clearInterval(pollPaymentTimer)
    pollPaymentTimer = null
  }
}

async function checkPaymentNow() {
  if (isCheckingStatus.value) return
  isCheckingStatus.value = true
  try {
    const code = activeCartCode.value
    const res = await api.get(`/payment/status/${code}`)
    if (res.data?.paymentStatus === 'PAID') {
      customerPaymentPaid.value = true
      stopPaymentPolling()
      setTimeout(async () => {
        await submitPosOrder()
        closeCustomerDisplayModal()
      }, 1200)
    }
  } catch (e) {
    // chưa thanh toán hoặc chưa ghi nhận
  } finally {
    isCheckingStatus.value = false
  }
}

function onCheckoutButtonClick() {
  if (activeCart.value.paymentMethod === 'CASH') {
    submitPosOrder()
  } else {
    openCustomerDisplayModal()
  }
}

async function confirmPosPaidAndPrint() {
  customerPaymentPaid.value = true
  await submitPosOrder()
  closeCustomerDisplayModal()
}

async function openAllShiftsModal() {
  showAllShiftsModal.value = true
  await loadAllShifts()
}

async function loadAllShifts() {
  try {
    const res = await api.get('/admin/pos/shifts')
    allShiftsList.value = res.data || []
  } catch (err) {
    console.error('Lỗi tải danh sách ca toàn showroom:', err)
  }
}

// Lifecycle & Loaders
onMounted(async () => {
  await loadCatalog()
  await loadStaff()
  await loadCurrentShift()
  await loadRecentOrders()
  await loadMyStats()
  focusBarcode()
})

function focusBarcode() {
  nextTick(() => {
    barcodeInput.value?.focus()
  })
}

async function loadMyStats() {
  try {
    const res = await api.get('/admin/pos/my-stats')
    myStats.value = res.data || null
  } catch (err) {
    console.error('Lỗi tải hiệu suất cá nhân:', err)
  }
}

function openMyStatsModal() {
  loadMyStats()
  showMyStatsModal.value = true
}

async function loadCatalog() {
  loadingCatalog.value = true
  try {
    const res = await api.get('/admin/pos/catalog')
    productList.value = res.data || []
    filteredProducts.value = productList.value

    // Extract categories
    const cats = new Set()
    productList.value.forEach(p => {
      if (p.category) cats.add(p.category)
    })
    categories.value = Array.from(cats)
  } catch (err) {
    console.error('Lỗi tải danh mục POS:', err)
  } finally {
    loadingCatalog.value = false
  }
}

async function loadStaff() {
  try {
    const res = await api.get('/admin/pos/staff-list')
    staffList.value = res.data || []
    if (staffList.value.length) {
      const me = staffList.value.find(s => s.id === auth.user?.id)
      if (me) {
        activeCart.value.staffId = me.id
      } else if (!activeCart.value.staffId) {
        activeCart.value.staffId = staffList.value[0].id
      }
    }
  } catch (err) {
    console.error('Lỗi tải nhân viên:', err)
  }
}

async function loadCurrentShift() {
  try {
    const res = await api.get('/admin/pos/shifts/current')
    shift.value = res.data || { open: false }
  } catch (err) {
    console.error('Lỗi tải ca:', err)
  }
}

async function loadRecentOrders() {
  try {
    const res = await api.get('/admin/pos/orders')
    recentPosOrders.value = res.data || []
  } catch (err) {
    console.error('Lỗi tải hóa đơn gần đây:', err)
  }
}

// Product Filtering & Search
function filterProducts() {
  let list = productList.value
  if (selectedCat.value) {
    list = list.filter(p => p.category === selectedCat.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(p => {
      return (p.name + ' ' + (p.sku || '') + ' ' + (p.barcode || '')).toLowerCase().includes(q)
    })
  }
  filteredProducts.value = list
}

function handleBarcodeScan() {
  const code = searchQuery.value.trim()
  if (!code) return

  // Exact barcode or SKU match
  const exact = productList.value.find(p =>
    (p.sku && p.sku.toLowerCase() === code.toLowerCase()) ||
    (p.barcode && p.barcode.toLowerCase() === code.toLowerCase())
  )

  if (exact) {
    onProductClick(exact)
    searchQuery.value = ''
    filterProducts()
    return
  }

  // Filter list by keyword
  filterProducts()
  if (filteredProducts.value.length === 1) {
    onProductClick(filteredProducts.value[0])
    searchQuery.value = ''
    filterProducts()
  }
}

function onProductClick(p) {
  if (p.stock <= 0) {
    alert('Sản phẩm đã hết hàng trong kho showroom.')
    return
  }
  if (p.variants && p.variants.length > 0) {
    currentProductForVariant.value = p
    showVariantModal.value = true
    return
  }
  addProductToCart(p, null)
}

function addProductToCart(p, variant) {
  const vid = variant ? variant.id : null
  const existing = activeCart.value.items.find(i => i.productId === p.id && i.variantId === vid)

  const price = variant && variant.price > 0 ? variant.price : p.price
  const maxStock = variant ? variant.stock : p.stock

  if (existing) {
    if (existing.quantity < maxStock) {
      existing.quantity++
    } else {
      alert(`Đã đạt số lượng tồn kho tối đa (${maxStock} chiếc) của sản phẩm này.`)
    }
  } else {
    activeCart.value.items.push({
      productId: p.id,
      variantId: vid,
      name: p.name,
      variantName: variant ? variant.name : '',
      image: p.image,
      unitPrice: price,
      quantity: 1,
      maxStock: maxStock
    })
  }
  focusBarcode()
}

function addVariantToCart(product, variant) {
  addProductToCart(product, variant)
  showVariantModal.value = false
}

// Cart Item Actions
function changeQty(idx, delta) {
  const it = activeCart.value.items[idx]
  if (!it) return
  const newQty = it.quantity + delta
  if (newQty <= 0) {
    removeItem(idx)
    return
  }
  if (newQty > it.maxStock) {
    alert(`Kho chỉ còn ${it.maxStock} chiếc.`)
    return
  }
  it.quantity = newQty
}

function validateQty(idx) {
  const it = activeCart.value.items[idx]
  if (!it) return
  if (it.quantity <= 0) it.quantity = 1
  if (it.quantity > it.maxStock) {
    alert(`Kho chỉ còn ${it.maxStock} chiếc.`)
    it.quantity = it.maxStock
  }
}

function removeItem(idx) {
  activeCart.value.items.splice(idx, 1)
}

// Customer Operations
async function searchCustomers() {
  if (customerQuery.value.trim().length < 2) {
    customerResults.value = []
    return
  }
  try {
    const res = await api.get('/admin/pos/customers', { params: { q: customerQuery.value.trim() } })
    customerResults.value = res.data || []
  } catch (err) {
    console.error('Lỗi tìm khách:', err)
  }
}

function selectCustomer(c) {
  activeCart.value.customer = c
  activeCart.value.customerName = c.fullName
  activeCart.value.customerPhone = c.phone || ''
  showCustomerSearch.value = false
  customerQuery.value = ''
  customerResults.value = []
}

function removeCustomer() {
  activeCart.value.customer = null
  activeCart.value.customerName = ''
  activeCart.value.customerPhone = ''
  activeCart.value.applyPoints = false
}

function onTogglePoints() {
  // auto recalculate via computed
}

// Shifts Handling
async function confirmOpenShift() {
  try {
    const res = await api.post('/admin/pos/shifts/open', {
      openingCash: openCashInput.value || 0,
      notes: openShiftNotes.value || ''
    })
    shift.value = { ...res.data, open: true }
    showOpenShiftModal.value = false
    alert('Đã mở ca làm việc thành công.')
  } catch (err) {
    alert('Lỗi mở ca: ' + (err.response?.data?.message || err.message))
  }
}

async function confirmCloseShift() {
  try {
    const res = await api.post('/admin/pos/shifts/close', {
      closingCash: closeCashInput.value || 0,
      notes: closeShiftNotes.value || ''
    })
    shift.value = { open: false }
    showCloseShiftModal.value = false
    alert('Đã chốt ca bán hàng và lưu biên bản bàn giao két.')
  } catch (err) {
    alert('Lỗi đóng ca: ' + (err.response?.data?.message || err.message))
  }
}

// Submit Order
async function submitPosOrder() {
  if (activeCart.value.items.length === 0) return

  if (activeCart.value.paymentMethod === 'CASH' && cashChange.value < 0) {
    alert('Số tiền khách đưa chưa đủ để thanh toán.')
    return
  }

  isSubmitting.value = true
  try {
    const payload = {
      items: activeCart.value.items.map(i => ({
        productId: i.productId,
        variantId: i.variantId,
        quantity: i.quantity,
        unitPrice: i.unitPrice
      })),
      staffId: activeCart.value.staffId,
      customerId: activeCart.value.customer ? activeCart.value.customer.id : null,
      customerName: activeCart.value.customerName,
      customerPhone: activeCart.value.customerPhone,
      discountAmount: tierDiscountAmount.value + (activeCart.value.manualDiscount || 0),
      managerDiscountAmount: managerDiscountComputed.value,
      managerDiscountReason: activeCart.value.managerDiscountReason || '',
      requireVatInvoice: !!activeCart.value.requireVatInvoice,
      companyName: activeCart.value.companyName || '',
      companyTaxCode: activeCart.value.companyTaxCode || '',
      companyAddress: activeCart.value.companyAddress || '',
      companyEmail: activeCart.value.companyEmail || '',
      vatAmount: vatAmountComputed.value,
      usePoints: activeCart.value.applyPoints ? pointsUsedCount.value : 0,
      paymentMethod: activeCart.value.paymentMethod,
      cashGiven: activeCart.value.paymentMethod === 'CASH' ? (activeCart.value.cashGiven || finalTotal.value) : finalTotal.value,
      note: activeCart.value.note || 'Bán tại quầy'
    }

    const res = await api.post('/admin/pos/orders', payload)
    receiptData.value = res.data

    // Refresh catalog stock, shift & stats
    await loadCatalog()
    await loadCurrentShift()
    await loadRecentOrders()
    await loadMyStats()

    // Reset current cart
    clearCurrentCart()

    // Open receipt modal
    showReceiptModal.value = true
  } catch (err) {
    alert('Lỗi thanh toán tại quầy: ' + (err.response?.data?.message || err.message))
  } finally {
    isSubmitting.value = false
  }
}

function reprintReceipt(ro) {
  receiptData.value = ro
  showReceiptModal.value = true
}

function printReceipt() {
  window.print()
}

// Format Helpers
function fmtTime(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function fmtFullDate(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  return d.toLocaleString('vi-VN')
}
</script>

<style scoped>
.pos-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 70px);
  background: var(--bg);
  overflow: hidden;
  font-family: inherit;
}

/* Topbar */
.pos-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 14px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  gap: 10px;
  flex-shrink: 0;
}
.shift-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  white-space: nowrap;
}
.shift-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 700;
}
.shift-open {
  background: rgba(46, 125, 50, 0.15);
  color: #2e7d32;
  border: 1px solid #2e7d32;
}
.shift-closed {
  background: rgba(198, 40, 40, 0.15);
  color: #c62828;
  border: 1px solid #c62828;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}
.shift-details {
  display: flex;
  gap: 10px;
  font-size: 0.82rem;
  color: var(--muted);
  white-space: nowrap;
  align-items: center;
  flex-shrink: 0;
}
.shift-details b {
  color: var(--text);
}
.text-accent {
  color: var(--accent, #ff6a00);
}
.text-danger {
  color: #c62828;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 1;
  overflow-x: auto;
}
.topbar-actions .btn {
  padding: 5px 9px;
  font-size: 0.78rem;
  white-space: nowrap;
}
.cart-tabs {
  display: flex;
  gap: 4px;
  background: var(--bg-2);
  padding: 3px;
  border-radius: 6px;
}
.cart-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  background: transparent;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--muted);
  cursor: pointer;
  border-radius: 4px;
}
.cart-tab.active {
  background: var(--surface);
  color: var(--accent);
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.tab-close {
  font-size: 1rem;
  line-height: 1;
  color: var(--muted);
}
.tab-close:hover {
  color: #c62828;
}
.add-tab {
  color: var(--accent);
}

/* Main Layout */
.pos-main {
  display: grid;
  grid-template-columns: 1fr 440px;
  flex: 1;
  overflow: hidden;
  min-height: 0;
}
@media (max-width: 1100px) {
  .pos-main {
    grid-template-columns: 1fr 380px;
  }
}

/* Left Column: Catalog */
.pos-catalog {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
  overflow: hidden;
  background: var(--bg);
  min-height: 0;
}
.catalog-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.search-box {
  position: relative;
  display: flex;
  align-items: center;
}
.search-box svg {
  position: absolute;
  left: 12px;
  color: var(--muted);
}
.input-search {
  width: 100%;
  padding: 10px 36px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 0.95rem;
}
.clear-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  font-size: 1.2rem;
  color: var(--muted);
  cursor: pointer;
}

.cat-filters {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.cat-btn {
  padding: 6px 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--muted);
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.cat-btn.active {
  background: #111;
  color: #fff;
  border-color: #111;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  grid-auto-rows: max-content;
  align-content: start;
  gap: 12px;
  padding: 12px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}
.pos-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
  min-height: 250px;
  height: auto;
}
.pos-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.pos-card.out-of-stock {
  opacity: 0.45;
  cursor: not-allowed;
}
.card-img-wrap {
  position: relative;
  aspect-ratio: 4/3;
  background: #0d0d0d;
}
.card-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.scale-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  background: rgba(0,0,0,0.75);
  color: #fff;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.7rem;
  font-weight: 700;
}
.variant-tag {
  position: absolute;
  bottom: 6px;
  right: 6px;
  background: var(--accent);
  color: #fff;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.68rem;
  font-weight: 700;
}
.card-body {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}
.card-title {
  font-size: 0.85rem;
  font-weight: 700;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--muted);
}
.low-stock {
  color: #c62828;
  font-weight: 700;
}
.card-price {
  font-size: 0.95rem;
  font-weight: 800;
  margin-top: auto;
  color: var(--accent);
}

/* Right Column: Cart */
.pos-cart {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  height: 100%;
  overflow: hidden;
  box-sizing: border-box;
}
.cart-header-box {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-2);
  flex-shrink: 0;
}
.field-lbl {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--muted);
  display: block;
  margin-bottom: 3px;
}
.customer-picker {
  margin-top: 8px;
  background: var(--surface);
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
}
.guest-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.quick-guest-inputs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.vip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tier-pill {
  font-size: 0.72rem;
  font-weight: 800;
  padding: 2px 7px;
  border-radius: 10px;
  margin-left: 6px;
  text-transform: uppercase;
}
.tier-đồng { background: #d78b54; color: #fff; }
.tier-bạc { background: #b0bec5; color: #111; }
.tier-vàng { background: #fbc02d; color: #111; }
.tier-bạch_kim, .tier-bạch-kim { background: #263238; color: #eceff1; border: 1px solid #78909c; }
.vip-details {
  display: flex;
  gap: 12px;
  font-size: 0.78rem;
  color: var(--muted);
  margin-top: 4px;
}
.use-points-row {
  margin-top: 6px;
  font-size: 0.8rem;
  font-weight: 600;
}

/* Cart Items */
.cart-items-wrap {
  flex: 1 1 0px;
  min-height: 80px;
  max-height: none;
  overflow-y: auto;
  padding: 6px 12px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.empty-cart-msg {
  padding: 40px 20px;
  text-align: center;
  color: var(--muted);
  font-size: 0.9rem;
}
.cart-item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.item-thumb {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  object-fit: cover;
  background: #111;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 0.84rem;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-variant {
  color: var(--muted);
  font-size: 0.75rem;
}
.item-price-unit {
  font-size: 0.78rem;
  color: var(--muted);
}
.qty-control {
  display: flex;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 4px;
}
.qty-btn {
  width: 24px;
  height: 26px;
  border: none;
  background: var(--bg-2);
  cursor: pointer;
  font-weight: 800;
}
.qty-input {
  width: 34px;
  height: 26px;
  border: none;
  text-align: center;
  font-size: 0.85rem;
  font-weight: 700;
  background: transparent;
  color: var(--text);
}
.item-subtotal {
  font-size: 0.88rem;
  font-weight: 800;
  min-width: 75px;
  text-align: right;
}
.item-remove {
  background: none;
  border: none;
  color: var(--muted);
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0 4px;
}
.item-remove:hover {
  color: #c62828;
}

/* Calculation & Payment */
.cart-summary-box {
  padding: 8px 12px;
  border-top: 1px solid var(--border);
  background: var(--surface);
  box-shadow: 0 -4px 16px rgba(0,0,0,0.05);
  flex-shrink: 0;
  max-height: 520px;
  overflow-y: auto;
}
.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.82rem;
  margin-bottom: 2px;
}
.input-discount {
  width: 120px;
  padding: 4px 8px;
  text-align: right;
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 0.85rem;
  background: var(--bg);
  color: var(--text);
}
.grand-total-line {
  border-top: 1px dashed var(--border);
  padding-top: 8px;
  margin-top: 8px;
  margin-bottom: 10px;
}
.total-lbl {
  font-family: 'Oswald', 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 1rem;
  font-weight: 900;
  letter-spacing: -0.01em;
}
.total-val {
  font-family: 'Oswald', 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 1.45rem;
  font-weight: 900;
  color: var(--accent);
}

.payment-methods-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 4px;
  margin-bottom: 6px;
}
.pay-btn {
  font-family: 'Oswald', 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  padding: 6px 2px;
  border: 1px solid var(--border);
  background: var(--bg-2);
  color: var(--text);
  font-size: 0.76rem;
  font-weight: 700;
  border-radius: 4px;
  cursor: pointer;
  text-align: center;
}
.pay-btn.active {
  background: #111;
  color: #fff;
  border-color: #111;
}

/* Cash Section */
.cash-input-section {
  background: var(--bg-2);
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 10px;
}
.quick-cash-row {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.quick-cash-btn {
  padding: 4px 8px;
  border: 1px solid var(--border);
  background: var(--surface);
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 4px;
  cursor: pointer;
}
.cash-calc-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.input-cash {
  flex: 1;
  padding: 7px 10px;
  font-size: 1rem;
  font-weight: 800;
  text-align: right;
}
.cash-change-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
}
.change-amount {
  font-size: 1.15rem;
  font-weight: 900;
  color: #2e7d32;
}

/* QR & Terminal sections */
.qr-pay-section, .card-swipe-section, .momo-pay-section {
  background: var(--bg-2);
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 10px;
}
.qr-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}
.qr-img {
  width: 90px;
  height: 90px;
  border-radius: 4px;
  background: #fff;
}
.pos-terminal-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-actions-row {
  display: flex;
  gap: 8px;
}
.btn-checkout {
  font-family: 'Oswald', 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  flex: 1;
  padding: 12px 16px;
  font-size: 0.95rem;
  font-weight: 900;
  letter-spacing: 0.02em;
}

/* Modals */
.modal-scrim {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.modal-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  width: min(520px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  padding: 20px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.3);
}
.modal-large {
  width: min(840px, 100%);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
}
.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--muted);
  cursor: pointer;
}
.variant-option-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
}
.variant-option-card:hover {
  border-color: var(--accent);
  background: var(--bg-2);
}
.customer-result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
}
.customer-result-item:hover {
  background: var(--bg-2);
}

/* Printable K80 Receipt Styles */
.k80-receipt {
  width: 78mm;
  margin: 0 auto;
  padding: 6mm 4mm;
  background: #fff;
  color: #000;
  font-family: 'Courier New', Courier, monospace, sans-serif;
  font-size: 11px;
  line-height: 1.35;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}
.receipt-brand {
  text-align: center;
}
.store-name {
  font-size: 15px;
  font-weight: 900;
  margin: 0 0 2px 0;
}
.store-tagline {
  font-size: 9px;
  font-weight: 700;
  margin: 0 0 4px 0;
}
.store-address, .store-phone {
  font-size: 9px;
  margin: 1px 0;
}
.receipt-divider {
  text-align: center;
  font-size: 10px;
  letter-spacing: -1px;
  margin: 4px 0;
  overflow: hidden;
  white-space: nowrap;
}
.receipt-meta {
  font-size: 10px;
}
.receipt-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 10.5px;
}
.receipt-table th {
  padding: 3px 0;
  font-size: 10px;
  border-bottom: 1px dashed #666;
}
.receipt-table td {
  padding: 4px 0;
  vertical-align: top;
}
.item-cell {
  max-width: 38mm;
  word-break: break-word;
}
.receipt-calc {
  font-size: 11px;
}
.calc-row {
  display: flex;
  justify-content: space-between;
  margin: 2px 0;
}
.total-highlight {
  font-size: 13px;
  font-weight: 900;
  border-top: 1px solid #000;
  border-bottom: 1px solid #000;
  padding: 4px 0;
  margin: 4px 0;
}
.receipt-footer {
  text-align: center;
  font-size: 10px;
  margin-top: 8px;
}
.receipt-modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.staff-perf-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--surface);
  border-color: var(--accent);
  color: var(--accent);
  font-weight: 600;
}
.staff-perf-btn:hover {
  background: var(--bronze-soft);
}
.badge-comm {
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
}
.kpi-grid-modal {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 14px;
}
.kpi-box {
  background: var(--bg);
  border: 1px solid var(--border);
  padding: 12px;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kpi-box.highlight-kpi {
  border-color: var(--accent);
  background: var(--surface);
}
.progress-bar-wrap {
  width: 100%;
  height: 6px;
  background: var(--border);
  border-radius: 3px;
  overflow: hidden;
  margin-top: 4px;
}
.progress-bar-fill {
  height: 100%;
  background: var(--accent);
  transition: width 0.3s ease;
}
.badge-earn {
  background: #e6f9ed;
  color: #15803d;
}
.badge-payout {
  background: #fef2f2;
  color: #b91c1c;
}

@media (max-width: 768px) {
  .kpi-grid-modal {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media print {
  body * {
    visibility: hidden;
  }
  #pos-thermal-receipt, #pos-thermal-receipt * {
    visibility: visible;
  }
  #pos-thermal-receipt {
    position: absolute;
    left: 0;
    top: 0;
    width: 80mm;
    margin: 0;
    padding: 2mm;
    box-shadow: none;
  }
  .no-print {
    display: none !important;
  }
}

/* Role Banners */
.role-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 18px;
  font-size: 0.85rem;
  border-bottom: 1px solid var(--border);
}
.manager-banner {
  background: linear-gradient(90deg, #1e293b, #0f172a);
  color: #f8fafc;
}
.staff-banner {
  background: #f1f5f9;
  color: #334155;
}
.role-banner-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.banner-badge {
  font-size: 0.75rem;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 4px;
  letter-spacing: 0.05em;
}
.manager-banner .banner-badge {
  background: #d97706;
  color: #fff;
}
.staff-banner .banner-badge {
  background: #2563eb;
  color: #fff;
}

/* Manager Discount Card */
.manager-discount-card {
  background: var(--bg-2, #fafafa);
  border: 1px dashed var(--accent, #d97706);
  border-radius: 6px;
  padding: 10px 12px;
  margin-top: 10px;
}
.md-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.md-mode-toggles {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}
.md-mode-toggles button {
  flex: 1;
  padding: 4px;
  font-size: 0.75rem;
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  border-radius: 4px;
}
.md-mode-toggles button.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.discount-quick-chips {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.chip-btn {
  padding: 3px 8px;
  font-size: 0.75rem;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 3px;
  cursor: pointer;
}
.chip-btn.active {
  background: #b45309;
  color: #fff;
  border-color: #b45309;
}
.btn-reset-chip {
  color: #dc2626;
}

/* VAT Card */
.vat-card {
  background: var(--bg-2, #fafafa);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 10px 12px;
  margin-top: 10px;
}
.vat-toggle-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 0.85rem;
}
.vat-fields-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 10px;
}

/* Gateway Preview Sections */
.gateway-preview-section {
  padding: 12px;
  border-radius: 6px;
  margin-top: 10px;
  border: 1px solid var(--border);
}
.vnpay-theme {
  background: rgba(0, 91, 170, 0.06);
  border-color: #005baa;
}
.momo-theme {
  background: rgba(165, 0, 100, 0.06);
  border-color: #a50064;
}
.gw-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.gw-actions-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.test-card-quick-note {
  margin-top: 8px;
  padding: 6px 8px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 4px;
  font-size: 0.75rem;
}

/* Customer-Facing Display Modal */
.modal-customer-display {
  width: min(1140px, 96vw);
  max-height: 94vh;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.customer-display-header {
  padding: 14px 20px;
  margin-bottom: 0;
  background: #0f172a;
  color: #fff;
  border-bottom: 1px solid #334155;
}
.customer-display-header h3 {
  margin: 4px 0 2px 0;
  font-size: 1.15rem;
}
.cd-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.badge-polling {
  background: #f59e0b;
  color: #fff;
  font-size: 0.75rem;
  padding: 3px 8px;
  border-radius: 4px;
}
.badge-open {
  background: #10b981;
  color: #fff;
}
.badge-closed {
  background: #64748b;
  color: #fff;
}
.customer-display-body {
  display: grid;
  grid-template-columns: 440px 1fr;
  flex: 1;
  overflow-y: auto;
}
.cd-order-summary {
  background: var(--bg);
  padding: 18px;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.cd-panel-title {
  font-weight: 800;
  font-size: 0.95rem;
  text-transform: uppercase;
  color: var(--muted);
  border-bottom: 1px solid var(--border);
  padding-bottom: 6px;
}
.cd-items-list {
  max-height: 240px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cd-item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--surface);
  padding: 8px;
  border-radius: 4px;
  border: 1px solid var(--border);
}
.cd-item-thumb {
  width: 44px;
  height: 44px;
  object-fit: cover;
  border-radius: 4px;
}
.cd-item-info {
  flex: 1;
}
.cd-item-name {
  font-weight: 700;
  font-size: 0.85rem;
}
.cd-calc-box {
  background: var(--surface);
  padding: 12px;
  border-radius: 6px;
  border: 1px solid var(--border);
}
.cd-grand-total {
  font-size: 1.15rem;
  font-weight: 900;
  border-top: 2px solid var(--border);
  padding-top: 8px;
  margin-top: 6px;
}
.cd-total-val {
  color: var(--accent);
  font-size: 1.3rem;
}
.cd-customer-card {
  background: var(--surface);
  border: 1px solid var(--accent);
  border-radius: 4px;
  padding: 10px;
}
.cd-cust-tag {
  font-size: 0.7rem;
  text-transform: uppercase;
  color: var(--muted);
}
.cd-cust-name {
  font-weight: 800;
}

/* Payment Gateway Right Column in Customer Display */
.cd-payment-area {
  padding: 20px;
  background: var(--surface);
  display: flex;
  flex-direction: column;
}
.cd-gw-header {
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 14px;
}
.cd-gw-header.vnpay-header {
  background: #005baa;
  color: #fff;
}
.cd-gw-header.momo-header {
  background: #a50064;
  color: #fff;
}
.cd-gw-header.qr-header {
  background: #111827;
  color: #fff;
}
.cd-gw-header.cash-header {
  background: #059669;
  color: #fff;
}
.cd-gw-header.card-header {
  background: #3b82f6;
  color: #fff;
}
.cd-gw-badge {
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  opacity: 0.9;
}
.cd-gw-header h4 {
  margin: 4px 0 2px 0;
  font-size: 1.1rem;
}
.cd-vnpay-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.btn-gateway-open {
  flex: 1;
  text-align: center;
  padding: 10px 16px;
  font-weight: 700;
}
.cd-iframe-wrap {
  width: 100%;
  height: 380px;
  border: 1px solid var(--border);
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 12px;
}
.cd-gateway-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
.cd-test-card-panel {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 0.8rem;
}
.cd-card-title {
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 6px;
}
.test-card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.cd-qr-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin: 10px 0;
}
.cd-momo-qr, .cd-vietqr-img {
  width: 200px;
  height: 200px;
  object-fit: contain;
  background: #fff;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
}
.vietqr-details {
  text-align: center;
  line-height: 1.5;
  font-size: 0.85rem;
}
.cd-cash-summary {
  background: var(--bg);
  padding: 16px;
  border-radius: 6px;
  margin: 12px 0;
}

/* Success Overlay in Customer Display */
.cd-success-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
}
.cd-success-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #10b981;
  color: #fff;
  font-size: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

/* Modal XLarge */
.modal-xlarge {
  width: min(1200px, 96vw);
}

@media (max-width: 900px) {
  .customer-display-body {
    grid-template-columns: 1fr;
  }
}

/* Compact POS Cart Header & Controls */
.pos-role-staff-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.mode-tag {
  font-family: 'Oswald', 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-weight: 700;
  font-size: 0.82rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text);
}
.manager-tag {
  color: var(--accent);
}
.staff-tag {
  color: var(--text);
}
.staff-picker-inline {
  display: flex;
  align-items: center;
  gap: 4px;
}
.staff-lbl-inline {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--muted);
}
.staff-select-inline {
  padding: 3px 6px;
  font-size: 0.78rem;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 4px;
  max-width: 190px;
  color: var(--text);
}
.customer-picker-compact {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 4px;
  padding: 4px 8px;
}
.guest-compact-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.guest-pill {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 6px;
  background: var(--bg-2);
  border: 1px solid var(--border);
  border-radius: 3px;
  white-space: nowrap;
}
.input-pos-compact {
  flex: 1;
  min-width: 0;
  padding: 4px 6px;
  font-size: 0.78rem;
  border: 1px solid var(--border);
  background: var(--bg);
  border-radius: 3px;
  color: var(--text);
}
.btn-vip-lookup {
  padding: 3px 8px;
  font-size: 0.75rem;
  font-weight: 700;
  border: 1px solid var(--border);
  background: var(--bg-2);
  border-radius: 3px;
  cursor: pointer;
  white-space: nowrap;
}
.vip-compact-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
}
.vip-info-inline {
  display: flex;
  align-items: center;
  gap: 6px;
}
.points-tag {
  font-size: 0.72rem;
  color: var(--accent);
  font-weight: 700;
}
.vip-actions-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pts-chk-label {
  font-size: 0.75rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
.btn-change-cust {
  background: none;
  border: none;
  color: #c62828;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  padding: 0 4px;
}
.manager-discount-accordion {
  background: var(--surface);
  border: 1px dashed var(--accent, #d97706);
  border-radius: 4px;
  padding: 4px 8px;
}
.discount-accordion-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text);
  user-select: none;
}
.summary-toggle-hint {
  font-size: 0.72rem;
  color: var(--muted);
  font-weight: normal;
}
.discount-accordion-content {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--border);
}
.manager-preset-row {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.preset-btn {
  padding: 2px 6px;
  font-size: 0.74rem;
  font-weight: 700;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 3px;
  cursor: pointer;
}
.preset-btn.on {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.preset-btn.reset-btn {
  color: #c62828;
}
.manager-custom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.vat-card-compact {
  padding: 3px 0;
}
.vat-chk-label-compact {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.76rem;
  cursor: pointer;
  color: var(--muted);
}
.vat-chk-label-compact input {
  margin: 0;
}
.vat-fields-grid-compact {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-top: 4px;
}


/* Layout & Anti-Clipping CSS */
.pos-main {
  grid-template-columns: 1fr 480px !important;
}
.pos-cart {
  padding-right: 8px !important;
  box-sizing: border-box !important;
}
.radar-ping {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #16a34a;
  box-shadow: 0 0 0 0 rgba(22, 163, 74, 0.7);
  animation: radar-pulse 1.8s infinite cubic-bezier(0.66, 0, 0, 1);
  margin-right: 6px;
  flex-shrink: 0;
}
@keyframes radar-pulse {
  to {
    box-shadow: 0 0 0 10px rgba(22, 163, 74, 0);
  }
}
.realtime-status-pill {
  display: flex;
  align-items: center;
  font-size: 0.75rem;
  font-weight: 600;
  color: #166534;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 5px 8px;
  border-radius: 4px;
  margin: 6px 0;
}
.btn-simulate {
  display: block;
  width: 100%;
  padding: 5px 8px;
  font-size: 0.75rem;
  font-weight: 700;
  border: 1px dashed #ea580c;
  background: rgba(234, 88, 12, 0.08);
  color: #ea580c;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 6px;
  text-align: center;
}
.btn-simulate:hover {
  background: #ea580c;
  color: #fff;
}
.cart-actions-column {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  box-sizing: border-box;
  margin-top: 6px;
}
.btn-checkout-main {
  font-family: 'Oswald', 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  width: 100%;
  padding: 10px 12px;
  font-size: 0.88rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  box-sizing: border-box;
}
.cart-sub-actions-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  width: 100%;
  box-sizing: border-box;
}
.btn-sub-action {
  font-family: 'Oswald', 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  padding: 7px 10px;
  font-size: 0.8rem;
  font-weight: 700;
  text-align: center;
  box-sizing: border-box;
}
.vietin-theme {
  background: rgba(0, 85, 166, 0.05);
  border: 1px solid #0055a6;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 8px;
}
.momo-acc-info {
  font-size: 0.78rem;
  color: #a50064;
  margin: 4px 0;
}

</style>
