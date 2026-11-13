<template>

  <div class="order-detail-page">

    <div v-if="loading" class="spinner" style="margin:60px auto" />

    <div v-else-if="err && !order" class="alert alert-error">

      {{ err }}

      <div style="margin-top:12px"><router-link to="/admin/orders" class="btn btn-sm btn-outline">← Về danh sách đơn hàng</router-link></div>

    </div>

    <div v-else-if="order" class="detail-container">

      <!-- Breadcrumb & Top Bar -->

      <div class="top-nav">

        <router-link to="/admin/orders" class="back-link">

          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M19 12H5m7 7-7-7 7-7"/></svg>

          Quay lại danh sách đơn hàng

        </router-link>

        <div class="order-id-badge">

          <span>Mã đơn:</span>

          <b>{{ order.code }}</b>

        </div>

      </div>



      <!-- Header Overview Card -->

      <div class="card header-card">

        <div class="row between header-row">

          <div>

            <div class="row" style="gap:10px;align-items:center">

              <h1>Đơn hàng #{{ order.code }}</h1>

              <span :class="['badge badge-lg', 'badge-' + order.status]">{{ ORDER_STATUS[order.status] }}</span>

              <span :class="['badge', 'badge-' + order.paymentStatus]">{{ PAY_STATUS[order.paymentStatus] }}</span>

              <span v-if="order.giftWrap" class="badge gift-badge">Có gói quà</span>

            </div>

            <div class="meta-line">

              <span>Ngày đặt: <b>{{ dt(order.createdAt) }}</b></span>

              <span>· Phương thức: <b>{{ PAY_METHOD[order.paymentMethod] }}</b></span>

              <span v-if="order.paidAt">· Thanh toán lúc: <b>{{ dt(order.paidAt) }}</b></span>

              <span v-if="order.shippingMethod === 'EXPRESS'" class="badge badge-CONFIRMED">Giao nhanh Hỏa tốc</span>

            </div>

          </div>



          <!-- Action buttons -->

          <div class="actions-group">

            <div class="next-btns">

              <button v-for="n in NEXT[order.status]" :key="n" :class="['btn btn-sm', n === 'CANCELLED' ? 'btn-danger' : '']" @click="handleStatus(n)">

                {{ n === 'CANCELLED' ? 'Huỷ đơn hàng' : 'Chuyển sang: ' + LABEL[n] }}

              </button>

            </div>

            <div class="sub-btns">

              <!-- Nếu đơn hàng đã huỷ thì che/khoá nút In HĐ -->

              <router-link v-if="order.status !== 'CANCELLED'" :to="`/orders/${order.code}/invoice`" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-outline">

                In hoá đơn

              </router-link>

              <button v-else class="btn btn-sm btn-outline btn-disabled" disabled title="Đơn hàng đã huỷ — Hoá đơn đã bị vô hiệu hoá">

                HĐ đã vô hiệu

              </button>

              <button class="btn btn-sm btn-outline" @click="openLabels([order.id], 'packing')">Phiếu đóng gói</button>

              <button class="btn btn-sm btn-outline" @click="openLabels([order.id], 'labels')">Nhãn vận chuyển</button>

              <button v-if="order.paymentStatus !== 'PAID' && order.paymentStatus !== 'REFUNDED' && !['CANCELLED','RETURNED'].includes(order.status)" class="btn btn-sm btn-outline" @click="markPaid">

                Đã thu tiền

              </button>

            </div>

          </div>

        </div>

      </div>



      <!-- KHỐI PHÂN TÍCH SỰ CỐ / THẤT BẠI (HIỆN NỔI BẬT NẾU ĐƠN HỦY HOẶC TRẢ HÀNG) -->

      <div v-if="order.status === 'CANCELLED' || order.status === 'RETURNED' || failureInfo.isFailed" class="card failure-card">

        <div class="failure-header">

          <div class="failure-title">

            <span class="f-icon">⚠️</span>

            <div>

              <h3>BIÊN BẢN PHÂN TÍCH SỰ CỐ & TRÁCH NHIỆM CÁC BÊN</h3>

              <p class="muted">Xác định khâu phát sinh lỗi, bên chịu trách nhiệm và phương án khắc phục bồi hoàn</p>

            </div>

          </div>

          <span class="badge badge-CANCELLED failure-tag">{{ order.status === 'RETURNED' ? 'ĐỔI TRẢ HÀNG' : 'ĐƠN HÀNG THẤT BẠI' }}</span>

        </div>



        <div class="failure-grid">

          <div class="fg-item">

            <small>Khâu phát sinh thất bại</small>

            <b class="highlight-stage">{{ failureInfo.stage }}</b>

            <span class="fg-desc">{{ failureInfo.stageDesc }}</span>

          </div>

          <div class="fg-item">

            <small>Bên chịu trách nhiệm chính</small>

            <b class="highlight-party">{{ failureInfo.party }}</b>

            <span class="fg-desc">{{ failureInfo.partyDesc }}</span>

          </div>

          <div class="fg-item">

            <small>Thời gian phát sinh sự cố</small>

            <b>{{ dt(order.timeline?.CANCELLED || order.timeline?.RETURNED || order.createdAt) }}</b>

            <span class="fg-desc">Hệ thống ghi nhận và niêm phong chứng từ</span>

          </div>

          <div class="fg-item">

            <small>Tình trạng hóa đơn điện tử</small>

            <b style="color:var(--danger)">ĐÃ VÔ HIỆU HÓA (VOID)</b>

            <span class="fg-desc">Hóa đơn bán hàng bị che mờ & thu hồi hiệu lực</span>

          </div>

        </div>



        <div class="failure-reason-box">

          <div class="reason-label">Lý do cụ thể / Biên bản sự cố:</div>

          <div class="reason-content">{{ failureInfo.rawReason || 'Không có lý do chi tiết.' }}</div>

        </div>



        <div class="failure-resolution">

          <b>Quy trình xử lý & khắc phục:</b>

          <ul>

            <li><b>Kho hàng:</b> Đã tự động hoàn trả số lượng linh kiện & mô hình xe vào tồn kho hệ thống (Restocked).</li>

            <li><b>Khách hàng:</b> Đã hoàn lại điểm thưởng tích lũy ({{ order.pointsUsed }} điểm) và kích hoạt lại mã ưu đãi nếu có.</li>

            <li><b>Tài chính:</b> {{ order.paidAmount > 0 ? (order.refundedAmount >= order.paidAmount ? 'Đã hoàn tiền đầy đủ (' + vnd(order.refundedAmount) + ')' : 'Chưa hoàn hết — Cần xử lý hoàn tiền bên dưới') : 'Đơn chưa phát sinh thanh toán' }}.</li>

            <li><b>Đối tác vận chuyển:</b> {{ failureInfo.party.includes('vận chuyển') ? 'Ghi nhận lỗi giao nhận, lập biên bản đối soát cấn trừ phí giao hàng.' : 'Đã thông báo dừng hành trình vận chuyển.' }}</li>

          </ul>

        </div>

      </div>



      <!-- QUY TRÌNH CÁC BÊN LIÊN QUAN (STAKEHOLDER WORKFLOW MATRIX) -->

      <div class="card workflow-card">

        <div class="sec-title">

          <h3>Quy trình & Phân công trách nhiệm các bên liên quan</h3>

          <small class="muted">Theo dõi trách nhiệm cụ thể của từng khâu từ Khách hàng đến Kho vận, Shipper và Quản lý</small>

        </div>



        <div class="steps-matrix">

          <!-- 1. KHÁCH HÀNG -->

          <div :class="['step-box', { active: true, done: order.status !== 'PENDING', failed: order.status === 'CANCELLED' && failureInfo.party.includes('Khách') }]">

            <div class="step-num">1</div>

            <div class="step-head">

              <span class="step-party">Khách hàng (Buyer)</span>

              <h4>Đặt hàng & Thanh toán</h4>

            </div>

            <div class="step-body">

              <p><b>{{ order.buyerName }}</b></p>

              <p class="muted">{{ order.phone }}</p>

              <p class="muted text-sm">{{ order.email }}</p>

              <div class="tag-list">

                <span class="badge">{{ PAY_METHOD[order.paymentMethod] }}</span>

                <span :class="['badge', 'badge-' + order.paymentStatus]">{{ PAY_STATUS[order.paymentStatus] }}</span>

              </div>

            </div>

            <div class="step-foot">

              <small>Trách nhiệm: Cung cấp đúng thông tin & nhận cuộc gọi</small>

            </div>

          </div>



          <!-- 2. CSKH / TIẾP NHẬN -->

          <div :class="['step-box', { active: ['CONFIRMED','SHIPPING','COMPLETED'].includes(order.status), done: ['SHIPPING','COMPLETED'].includes(order.status), failed: order.status === 'CANCELLED' && failureInfo.stage.includes('CSKH') }]">

            <div class="step-num">2</div>

            <div class="step-head">

              <span class="step-party">Bộ phận CSKH & Sale</span>

              <h4>Xác nhận & Kiểm duyệt</h4>

            </div>

            <div class="step-body">

              <p><b>Nhân viên phụ trách:</b> {{ order.guest ? 'CSKH Hệ thống' : 'Tổ trực tổng đài ARES' }}</p>

              <p class="text-sm">Trạng thái: {{ ['CONFIRMED','SHIPPING','COMPLETED'].includes(order.status) ? '✓ Đã gọi xác thực đơn' : (order.status === 'CANCELLED' ? '✗ Hủy khi duyệt' : 'Đang chờ liên hệ') }}</p>

              <p v-if="order.timeline?.CONFIRMED" class="text-xs muted">Xác nhận: {{ dt(order.timeline.CONFIRMED) }}</p>

            </div>

            <div class="step-foot">

              <small>Trách nhiệm: Kiểm tra địa chỉ, phụ kiện & tư vấn bảo hành</small>

            </div>

          </div>



          <!-- 3. KHO VẬN & ĐÓNG GÓI -->

          <div :class="['step-box', { active: ['CONFIRMED','SHIPPING','COMPLETED'].includes(order.status), done: ['SHIPPING','COMPLETED'].includes(order.status), failed: order.status === 'CANCELLED' && failureInfo.party.includes('Kho') }]">

            <div class="step-num">3</div>

            <div class="step-head">

              <span class="step-party">Bộ phận Kho vận ARES</span>

              <h4>QC & Đóng gói an toàn</h4>

            </div>

            <div class="step-body">

              <p><b>Kho xuất hàng:</b> Tổng kho ARES Hub</p>

              <p class="text-sm">Tiêu chuẩn: Hộp xốp chống va đập, bọc màng khí mô hình xe</p>

              <p v-if="order.giftWrap" class="text-sm gift">Đóng hộp quà & thiệp chúc mừng</p>

            </div>

            <div class="step-foot">

              <small>Trách nhiệm: Đúng biến thể màu/bản xe, đủ phụ kiện, dán tem vỡ</small>

            </div>

          </div>



          <!-- 4. ĐƠN VỊ VẬN CHUYỂN -->

          <div :class="['step-box', { active: ['SHIPPING','COMPLETED'].includes(order.status), done: order.status === 'COMPLETED', failed: order.status === 'CANCELLED' && failureInfo.party.includes('vận chuyển') }]">

            <div class="step-num">4</div>

            <div class="step-head">

              <span class="step-party">Đơn vị Giao vận & Shipper</span>

              <h4>Vận chuyển & Phát hàng</h4>

            </div>

            <div class="step-body">

              <p><b>Đối tác vận chuyển:</b> {{ order.carrierName || (order.shippingMethod === 'EXPRESS' ? 'GHN Express (Hỏa tốc)' : 'GHTK Tiêu chuẩn') }}</p>

              <p v-if="order.shippingDistanceKm" class="text-sm">Cự ly tính cước: <b>{{ order.shippingDistanceKm }} km</b> (từ Tổng kho ARES)</p>

              <p class="text-sm">Cước vận chuyển: <b>{{ order.shippingFee ? vnd(order.shippingFee) : 'Miễn phí' }}</b></p>

              <p class="text-sm">Mã vận đơn: <code class="track-code">{{ trackingCode }}</code></p>

              <p v-if="order.etaFrom" class="text-xs muted">Dự kiến: {{ d(order.etaFrom) }} – {{ d(order.etaTo) }}</p>

            </div>

            <div class="step-foot">

              <small>Trách nhiệm: Bảo toàn nguyên vẹn hộp xe, phát đúng hạn, thu COD</small>

            </div>

          </div>



          <!-- 5. KẾ TOÁN & QUẢN TRỊ -->

          <div :class="['step-box', { active: order.status === 'COMPLETED', done: order.status === 'COMPLETED' }]">

            <div class="step-num">5</div>

            <div class="step-head">

              <span class="step-party">Kế toán & Quản trị</span>

              <h4>Nghiệm thu & Quyết toán</h4>

            </div>

            <div class="step-body">

              <p><b>Hóa đơn:</b> {{ order.status === 'CANCELLED' ? 'Đã vô hiệu hóa (VOID)' : 'Đã lập chứng từ điện tử' }}</p>

              <p class="text-sm">Đã thu: <b>{{ vnd(order.paidAmount) }}</b></p>

              <p v-if="order.refundedAmount" class="text-sm" style="color:var(--danger)">Đã hoàn: {{ vnd(order.refundedAmount) }}</p>

            </div>

            <div class="step-foot">

              <small>Trách nhiệm: Đối soát dòng tiền, xuất VAT, xử lý hoàn tiền</small>

            </div>

          </div>

        </div>

      </div>



      <!-- MAIN CONTENT: 2 COLUMNS (ITEMS & FINANCIALS) -->

      <div class="grid-layout">

        <!-- Cột trái: Sản phẩm & Biến thể xe -->

        <div class="col-left">

          <div class="card items-card">

            <div class="card-title">

              <h3>Danh sách xe mô hình & biến thể ({{ order.items.length }})</h3>

              <span class="small muted">Chi tiết mã SKU và biến thể đã chọn</span>

            </div>



            <table class="table order-items-table">

              <thead>

                <tr>

                  <th>Sản phẩm / Mẫu xe</th>

                  <th>Phân loại / Biến thể</th>

                  <th>Đơn giá</th>

                  <th>SL</th>

                  <th style="text-align:right">Thành tiền</th>

                </tr>

              </thead>

              <tbody>

                <tr v-for="it in order.items" :key="it.id">

                  <td>

                    <div class="prod-cell">

                      <b>{{ it.productName }}</b>

                      <span v-if="it.sku" class="small muted">SKU: {{ it.sku }}</span>

                    </div>

                  </td>

                  <td>

                    <div v-if="it.variantName" class="variant-pill">

                      <span>{{ it.variantName }}</span>

                      <small v-if="it.flashSale" class="flash-tag">⚡ Flash Sale</small>

                    </div>

                    <span v-else class="muted small">Bản tiêu chuẩn</span>

                  </td>

                  <td class="price">{{ vnd(it.unitPrice) }}</td>

                  <td>× <b>{{ it.quantity }}</b></td>

                  <td class="price" style="text-align:right"><b>{{ vnd(it.unitPrice * it.quantity) }}</b></td>

                </tr>

              </tbody>

            </table>



            <!-- Thông tin giao hàng -->

            <div class="shipping-info-block">

              <h4>Thông tin giao nhận hàng</h4>

              <div class="info-row">

                <span class="label">Người nhận:</span>

                <b>{{ order.buyerName }} · {{ order.phone }}</b>

              </div>

              <div class="info-row">

                <span class="label">Địa chỉ nhận:</span>

                <span>{{ order.shippingAddress }}</span>

              </div>

              <div class="info-row">

                <span class="label">Đơn vị vận chuyển:</span>

                <span><b>{{ order.carrierName || order.shippingMethod || 'Giao Hàng Nhanh' }}</b> (Gói: {{ order.shippingMethod }})</span>

              </div>

              <div v-if="order.shippingDistanceKm" class="info-row">

                <span class="label">Khoảng cách & Cước:</span>

                <span><b>{{ order.shippingDistanceKm }} km</b> từ Tổng kho ARES · Cước: <b>{{ order.shippingFee ? vnd(order.shippingFee) : 'Miễn phí' }}</b></span>

              </div>

              <div v-if="order.note" class="info-row">

                <span class="label">Ghi chú của khách:</span>

                <span class="note-box">“{{ order.note }}”</span>

              </div>

              <div v-if="order.giftWrap" class="info-row">

                <span class="label">Dịch vụ quà tặng:</span>

                <span>Gói quà cao cấp (+{{ vnd(order.giftFee || 0) }}) {{ order.giftMessage ? '· Lời nhắn: “' + order.giftMessage + '”' : '' }}</span>

              </div>

              <div v-if="order.hasHazardous || order.hazardousFee" class="info-row" style="background:#fff7ed;padding:8px 10px;border-radius:6px;border:1px solid #fdba74;color:#9a3412;margin-top:6px">

                <span class="label" style="color:#c2410c;font-weight:700">Quy chuẩn an toàn:</span>

                <span>Kiện hàng chứa Pin LiPo / Hàng dễ cháy nổ (+{{ vnd(order.hazardousFee || 25000) }}). Đóng gói chống cháy & vận chuyển đường bộ an toàn.</span>

              </div>

            </div>

          </div>



          <!-- Nhật ký nội bộ giữa các bộ phận -->

          <div class="card notes-card">

            <div class="card-title">

              <h3>Nhật ký trao đổi nội bộ giữa các bộ phận</h3>

              <small class="muted">Chỉ nhân viên nội bộ thấy — phục vụ bàn giao & xử lý sự cố</small>

            </div>



            <div class="notes-list">

              <div v-for="n in notes" :key="n.id" class="note-item">

                <div class="note-meta">

                  <b>{{ n.author }}</b>

                  <small>{{ dt(n.createdAt) }}</small>

                </div>

                <div class="note-text">{{ n.text }}</div>

              </div>

              <div v-if="!notes.length" class="empty-note muted">Chưa có ghi chú nội bộ cho đơn hàng này.</div>

            </div>



            <div class="add-note-box">

              <input v-model.trim="newNote" class="input" placeholder="Thêm ghi chú giữa các bộ phận (CSKH, Kho, Shipper, Kế toán)..." @keyup.enter="submitNote" />

              <button class="btn btn-sm" :disabled="!newNote || busyNote" @click="submitNote">Gửi ghi chú</button>

            </div>

          </div>

        </div>



        <!-- Cột phải: Tài chính & Hoàn tiền -->

        <div class="col-right">

          <!-- Bảng tổng hợp chi phí -->

          <div class="card summary-card">

            <div class="card-title">

              <h3>Chi tiết thanh toán</h3>

            </div>



            <div class="sum-lines">

              <div class="sum-row"><span>Tạm tính hàng hóa:</span><b>{{ vnd(order.subtotal) }}</b></div>

              <div v-if="order.discount" class="sum-row discount"><span>Giảm giá {{ order.promoCode ? '(' + order.promoCode + ')' : '' }}:</span><span>−{{ vnd(order.discount) }}</span></div>

              <div v-if="order.pointsUsed" class="sum-row discount"><span>Dùng {{ order.pointsUsed }} điểm:</span><span>−{{ vnd(order.pointsDiscount) }}</span></div>

              <div v-if="order.bundleDiscount" class="sum-row discount"><span>Ưu đãi Combo:</span><span>−{{ vnd(order.bundleDiscount) }}</span></div>

              <div v-if="order.creditUsed" class="sum-row discount"><span>Ví / Thẻ quà:</span><span>−{{ vnd(order.creditUsed) }}</span></div>

              <div class="sum-row"><span>Phí vận chuyển:</span><span>{{ vnd(order.shippingFee) }}</span></div>

              <div v-if="order.hazardousFee" class="sum-row hazmat-row" style="color:#c2410c;font-weight:600">

                <span>Phụ phí pin / hàng cháy nổ:</span>

                <span>+{{ vnd(order.hazardousFee) }}</span>

              </div>

              <div v-if="order.giftFee" class="sum-row"><span>Phí gói quà:</span><span>{{ vnd(order.giftFee) }}</span></div>



              <div class="sum-row total-row">

                <span>TỔNG ĐƠN HÀNG:</span>

                <span class="total-price">{{ vnd(order.total) }}</span>

              </div>



              <div class="sum-row paid-row">

                <span>Đã thanh toán:</span>

                <b :class="{ 'paid-ok': order.paidAmount >= order.total }">{{ vnd(order.paidAmount) }}</b>

              </div>



              <div v-if="order.refundedAmount" class="sum-row refunded-row">

                <span>Đã hoàn tiền:</span>

                <b class="text-danger">−{{ vnd(order.refundedAmount) }}</b>

              </div>



              <div v-if="order.paidAmount < order.total && order.status !== 'CANCELLED'" class="sum-row remaining-row">

                <span>Còn phải thu (COD):</span>

                <b>{{ vnd(order.total - order.paidAmount) }}</b>

              </div>

            </div>

          </div>



          <!-- Khối xử lý hoàn tiền -->

          <div class="card refund-card">

            <div class="card-title">

              <h3>Xử lý hoàn tiền</h3>

              <span class="small muted">Hoàn tối đa: {{ vnd(refundable) }}</span>

            </div>



            <div v-if="refundable > 0">

              <div class="field" style="margin-bottom:8px">

                <label class="small">Số tiền hoàn (đ):</label>

                <input v-model.number="rf.amount" type="number" min="1" :max="refundable" class="input" />

              </div>

              <div class="field" style="margin-bottom:8px">

                <label class="small">Lý do hoàn tiền (bắt buộc):</label>

                <input v-model.trim="rf.reason" class="input" placeholder="Lý do hủy/trả hàng..." />

              </div>

              <div v-if="!order.guest" class="field" style="margin-bottom:12px">

                <label class="small row" style="gap:6px;cursor:pointer">

                  <input v-model="rf.toCredit" type="checkbox" /> Hoàn vào ví tín dụng ARES của khách

                </label>

              </div>

              <button class="btn btn-sm btn-danger" style="width:100%" :disabled="!rf.amount || !rf.reason || busyRefund" @click="doRefund">

                {{ busyRefund ? 'Đang hoàn tiền…' : 'Xác nhận hoàn ' + vnd(rf.amount || 0) }}

              </button>

            </div>

            <div v-else class="muted small" style="padding:10px 0">

              {{ order.paidAmount <= 0 ? 'Đơn hàng chưa thu tiền, không cần hoàn.' : 'Đã hoàn tất toàn bộ số tiền đã thu (' + vnd(order.refundedAmount) + ').' }}

            </div>

          </div>



          <!-- Thông tin hóa đơn điện tử -->

          <div class="card invoice-status-card">

            <div class="card-title">

              <h3>Chứng từ & Hóa đơn</h3>

            </div>

            <p v-if="order.status === 'CANCELLED'" class="text-sm" style="color:var(--danger);font-weight:600">

              Hóa đơn điện tử đã bị che mờ và thu hồi hiệu lực do đơn hàng đã bị hủy.

            </p>

            <p v-else class="text-sm">

              Hóa đơn điện tử hợp lệ cho đơn hàng #{{ order.code }}. Có thể xuất in hoặc gửi cho khách hàng tra cứu.

            </p>

            <div style="margin-top:10px">

              <router-link v-if="order.status !== 'CANCELLED'" :to="`/orders/${order.code}/invoice`" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-outline" style="width:100%;text-align:center">

                Mở trang Hóa đơn bán hàng →

              </router-link>

              <router-link v-else :to="`/orders/${order.code}/invoice`" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-outline danger-text" style="width:100%;text-align:center">

                Xem hóa đơn đã che (VOID) →

              </router-link>

            </div>

          </div>

        </div>

      </div>



      <!-- Modal nhập biên bản hủy đơn hàng -->

      <div v-if="cancelModal" class="modal-bg" @click.self="cancelModal = false">

        <div class="modal">

          <h3>HỦY ĐƠN HÀNG #{{ order.code }}</h3>

          <p class="small muted">Vui lòng cung cấp khâu phát sinh lỗi và bên chịu trách nhiệm để ghi nhận vào biên bản sự cố.</p>



          <div class="field" style="margin-top:12px">

            <label>Khâu phát sinh sự cố thất bại *</label>

            <select v-model="cancelForm.stage" class="input">

              <option value="CSKH & Tiếp nhận">1. CSKH & Tiếp nhận (Khách đổi ý, gọi không nghe máy, sai SĐT)</option>

              <option value="Kho vận & Kiểm hàng">2. Kho vận & Kiểm hàng (Hết tồn kho, mô hình lỗi trầy xước không đủ chuẩn)</option>

              <option value="Vận chuyển & Giao nhận">3. Vận chuyển & Giao nhận (Shipper làm hỏng hộp hàng, quá số lần phát)</option>

              <option value="Thanh toán & Cổng tiền">4. Thanh toán & Cổng tiền (Lỗi thanh toán thẻ, nghi vấn giao dịch)</option>

              <option value="Khác">5. Lý do khác</option>

            </select>

          </div>



          <div class="field">

            <label>Bên chịu trách nhiệm chính *</label>

            <select v-model="cancelForm.party" class="input">

              <option value="Khách hàng">Khách hàng (Thay đổi nhu cầu, không nhận hàng)</option>

              <option value="Đơn vị vận chuyển / Shipper">Đơn vị vận chuyển / Shipper (Làm vỡ móp, giao trễ)</option>

              <option value="Bộ phận Kho vận ARES">Bộ phận Kho vận ARES (Sai lệch tồn kho, nhầm mẫu)</option>

              <option value="Bộ phận CSKH / Bán hàng">Bộ phận CSKH / Bán hàng (Tư vấn sai thông số)</option>

              <option value="Kỹ thuật & Hệ thống">Kỹ thuật & Hệ thống</option>

            </select>

          </div>



          <div class="field">

            <label>Chi tiết lý do hủy đơn *</label>

            <textarea v-model="cancelForm.reason" rows="3" class="input" placeholder="Mô tả chi tiết nguyên nhân hủy đơn..." />

          </div>



          <div class="row" style="gap:8px;margin-top:16px">

            <button class="btn btn-danger" :disabled="!cancelForm.reason.trim() || busyCancel" @click="confirmCancel">

              {{ busyCancel ? 'Đang xử lý…' : 'Xác nhận hủy đơn hàng' }}

            </button>

            <button class="btn btn-outline" @click="cancelModal = false">Bỏ qua</button>

          </div>

        </div>

      </div>



    </div>

  </div>

</template>



<script setup>

import { ref, computed, reactive, onMounted } from 'vue'

import { useRoute, useRouter } from 'vue-router'

import api, { errMsg } from '../../api'

import { toast } from '../../stores/prefs'

import { vnd, dt, d, ORDER_STATUS, PAY_METHOD, PAY_STATUS } from '../../format'



const route = useRoute(), router = useRouter()

const order = ref(null), loading = ref(true), err = ref('')

const notes = ref([]), newNote = ref(''), busyNote = ref(false)

const cancelModal = ref(false), busyCancel = ref(false)

const cancelForm = reactive({ stage: 'CSKH & Tiếp nhận', party: 'Khách hàng', reason: 'Khách yêu cầu hủy đơn' })

const rf = reactive({ amount: 0, reason: '', toCredit: false }), busyRefund = ref(false)



const NEXT = { PENDING: ['CONFIRMED', 'CANCELLED'], CONFIRMED: ['SHIPPING', 'CANCELLED'], SHIPPING: ['COMPLETED'] }

const LABEL = { CONFIRMED: 'Xác nhận', SHIPPING: 'Giao hàng', COMPLETED: 'Hoàn tất', CANCELLED: 'Huỷ' }



const refundable = computed(() => {

  if (!order.value) return 0

  return Math.max(0, (order.value.paidAmount || 0) - (order.value.refundedAmount || 0))

})



const trackingCode = computed(() => {

  if (!order.value) return ''

  return 'ARES-' + (order.value.shippingMethod === 'EXPRESS' ? 'GHN-' : 'GHTK-') + order.value.code.slice(-6)

})



// Phân tích thông tin sự cố / bên chịu trách nhiệm

const failureInfo = computed(() => {

  if (!order.value) return { isFailed: false, stage: '', stageDesc: '', party: '', partyDesc: '', rawReason: '' }

  const isFailed = order.value.status === 'CANCELLED' || order.value.status === 'RETURNED'

  const raw = order.value.cancelReason || ''



  let stage = 'CSKH & Tiếp nhận'

  let party = 'Khách hàng'

  let stageDesc = 'Khách hàng thay đổi nhu cầu hoặc không liên lạc được'

  let partyDesc = 'Người mua từ chối nhận hoặc yêu cầu hủy trước khi giao'



  if (raw.includes('[Khâu:') || raw.includes('[Bên:')) {

    const stageMatch = raw.match(/\[Khâu:\s*([^\]|]+)\]/)

    const partyMatch = raw.match(/\[Bên:\s*([^\]|]+)\]/)

    if (stageMatch) stage = stageMatch[1].trim()

    if (partyMatch) party = partyMatch[1].trim()

  } else if (/hết hàng|tồn kho|hỏng|vỡ|móp/i.test(raw)) {

    stage = 'Kho vận & Đóng gói'

    party = 'Bộ phận Kho vận ARES'

    stageDesc = 'Sản phẩm không đạt chuẩn QC hoặc lệch tồn kho'

    partyDesc = 'Nhân viên kho kiểm tra phát hiện lỗi trước khi xuất'

  } else if (/shipper|vận chuyển|không giao được|hỏng hộp/i.test(raw)) {

    stage = 'Vận chuyển & Giao nhận'

    party = 'Đơn vị vận chuyển / Shipper'

    stageDesc = 'Quá hạn phát hàng hoặc xảy ra sự cố trong quá trình giao'

    partyDesc = 'Đơn vị logistics phụ trách tuyến giao hàng'

  }



  return { isFailed, stage, stageDesc, party, partyDesc, rawReason: raw }

})



async function load() {

  loading.value = true

  err.value = ''

  try {

    const id = route.params.id

    // Thử load bằng id hoặc fallback bằng orders list

    let o = null

    try {

      o = (await api.get(`/admin/orders/${id}`)).data

    } catch {

      const all = (await api.get('/admin/orders')).data

      o = all.find(x => String(x.id) === String(id) || x.code === id)

    }

    if (!o) throw new Error('Không tìm thấy đơn hàng #' + id)

    order.value = o

    rf.amount = Math.max(0, (o.paidAmount || 0) - (o.refundedAmount || 0))

    await loadNotes()

  } catch (e) {

    err.value = errMsg(e)

  } finally {

    loading.value = false

  }

}



async function loadNotes() {

  if (!order.value) return

  try {

    notes.value = (await api.get(`/admin/orders/${order.value.id}/notes`)).data

  } catch {}

}



async function submitNote() {

  if (!newNote.value.trim() || busyNote.value) return

  busyNote.value = true

  try {

    const res = (await api.post(`/admin/orders/${order.value.id}/notes`, { text: newNote.value.trim() })).data

    notes.value.push(res)

    newNote.value = ''

    toast('Đã thêm ghi chú nội bộ')

  } catch (e) {

    toast(errMsg(e), 'error')

  } finally {

    busyNote.value = false

  }

}



async function handleStatus(s) {

  if (s === 'CANCELLED') {

    cancelModal.value = true

    return

  }

  try {

    await api.patch(`/admin/orders/${order.value.id}/status`, { status: s })

    toast(`Đã chuyển trạng thái: ${LABEL[s]}`)

    await load()

  } catch (e) {

    toast(errMsg(e), 'error')

  }

}



async function confirmCancel() {

  busyCancel.value = true

  const combinedReason = `[Khâu: ${cancelForm.stage}] [Bên: ${cancelForm.party}] Lý do: ${cancelForm.reason.trim()}`

  try {

    await api.patch(`/admin/orders/${order.value.id}/status`, { status: 'CANCELLED', note: combinedReason })

    toast('Đã hủy đơn hàng & hoàn kho')

    cancelModal.value = false

    await load()

  } catch (e) {

    toast(errMsg(e), 'error')

  } finally {

    busyCancel.value = false

  }

}



async function markPaid() {

  try {

    await api.post(`/admin/orders/${order.value.id}/mark-paid`)

    toast('Đã đánh dấu thu tiền')

    await load()

  } catch (e) {

    toast(errMsg(e), 'error')

  }

}



async function doRefund() {

  if (!confirm(`Hoàn ${vnd(rf.amount)} cho đơn ${order.value.code}?`)) return

  busyRefund.value = true

  try {

    await api.post(`/admin/orders/${order.value.id}/refund`, rf)

    toast('Đã hoàn tiền thành công')

    await load()

  } catch (e) {

    toast(errMsg(e), 'error')

  } finally {

    busyRefund.value = false

  }

}



function openLabels(ids, kind = 'labels') {

  const u = router.resolve({ path: '/admin/' + kind, query: { ids: ids.join(','), auto: 1 } }).href

  window.open(u, '_blank')

}



onMounted(load)

</script>



<style scoped>

.order-detail-page { max-width: 1240px; margin: 0 auto; padding-bottom: 40px; }

.top-nav { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }

.back-link { display: flex; align-items: center; gap: 6px; font-weight: 600; font-size: .92rem; color: var(--accent); }

.order-id-badge { font-size: .88rem; color: var(--muted); } .order-id-badge b { color: var(--text); margin-left: 4px; font-family: ui-monospace, monospace; }



.header-card { padding: 22px 26px; margin-bottom: 18px; border-left: 5px solid var(--accent); }

.header-row { align-items: flex-start; gap: 20px; }

.header-row h1 { font-size: 1.6rem; margin: 0; }

.meta-line { margin-top: 8px; font-size: .88rem; color: var(--muted); display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

.actions-group { display: flex; flex-direction: column; gap: 8px; align-items: flex-end; }

.next-btns, .sub-btns { display: flex; gap: 6px; flex-wrap: wrap; }

.gift-badge { background: #fef08a; color: #854d0e; }

.btn-disabled { opacity: .55; cursor: not-allowed; }



/* Failure & Incident Analysis Card */

.failure-card { background: #fff5f5; border: 2px solid #ef4444; padding: 20px 24px; margin-bottom: 20px; }

.failure-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; border-bottom: 1px dashed #fca5a5; padding-bottom: 12px; }

.failure-title { display: flex; align-items: center; gap: 12px; }

.failure-title h3 { margin: 0; color: #991b1b; font-size: 1.15rem; font-weight: 800; }

.f-icon { font-size: 2rem; line-height: 1; }

.failure-tag { font-size: .82rem; padding: 4px 10px; }



.failure-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; margin-bottom: 16px; }

.fg-item { background: #fff; padding: 12px 14px; border-radius: 6px; border: 1px solid #fee2e2; }

.fg-item small { display: block; color: #7f1d1d; text-transform: uppercase; font-size: .72rem; font-weight: 700; margin-bottom: 4px; }

.highlight-stage { color: #b91c1c; font-size: 1rem; display: block; }

.highlight-party { color: #1e3a8a; font-size: 1rem; display: block; }

.fg-desc { display: block; font-size: .78rem; color: #6b7280; margin-top: 4px; }



.failure-reason-box { background: #fff; border: 1px solid #fca5a5; padding: 12px 16px; border-radius: 6px; margin-bottom: 14px; }

.reason-label { font-weight: 700; font-size: .84rem; color: #991b1b; margin-bottom: 4px; }

.reason-content { font-size: .94rem; color: #1f2937; }



.failure-resolution { font-size: .88rem; color: #374151; }

.failure-resolution ul { margin: 6px 0 0 18px; padding: 0; }

.failure-resolution li { margin-bottom: 4px; }



/* Workflow Matrix */

.workflow-card { padding: 22px 26px; margin-bottom: 22px; }

.sec-title h3 { margin: 0 0 4px; font-size: 1.2rem; }

.steps-matrix { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-top: 18px; }

.step-box { background: var(--bg); border: 1px solid var(--border); border-radius: 8px; padding: 14px 12px; display: flex; flex-direction: column; position: relative; opacity: .75; }

.step-box.active { opacity: 1; border-color: var(--accent); }

.step-box.done { border-color: #22c55e; }

.step-box.failed { border-color: #ef4444; background: #fef2f2; }

.step-num { width: 22px; height: 22px; border-radius: 50%; background: var(--border); color: var(--text); font-size: .75rem; font-weight: 800; display: flex; align-items: center; justify-content: center; margin-bottom: 8px; }

.step-box.done .step-num { background: #22c55e; color: #fff; }

.step-box.failed .step-num { background: #ef4444; color: #fff; }

.step-party { font-size: .68rem; font-weight: 700; text-transform: uppercase; color: var(--accent); display: block; margin-bottom: 2px; }

.step-head h4 { margin: 0 0 8px; font-size: .88rem; line-height: 1.3; }

.step-body { font-size: .82rem; margin-bottom: 10px; flex: 1; }

.step-body p { margin: 2px 0; }

.step-foot { border-top: 1px dashed var(--border); padding-top: 6px; font-size: .74rem; color: var(--muted); }

.tag-list { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 6px; }

.track-code { font-family: ui-monospace, monospace; background: rgba(0,0,0,.06); padding: 2px 4px; border-radius: 4px; }



/* Grid Layout */

.grid-layout { display: grid; grid-template-columns: 1.55fr 1fr; gap: 20px; align-items: flex-start; }

.col-left, .col-right { display: flex; flex-direction: column; gap: 20px; }



.card-title { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 14px; border-bottom: 1px solid var(--border); padding-bottom: 10px; }

.card-title h3 { margin: 0; font-size: 1.1rem; }



/* Items table */

.order-items-table th { background: var(--bg); font-size: .82rem; }

.prod-cell b { display: block; font-size: .92rem; }

.variant-pill { display: inline-flex; align-items: center; gap: 6px; background: var(--bronze-soft); padding: 2px 8px; border-radius: 4px; font-size: .82rem; font-weight: 600; color: var(--accent); }

.flash-tag { color: #dc2626; font-weight: 800; font-size: .72rem; }



.shipping-info-block { margin-top: 20px; padding: 14px 16px; background: var(--bg); border-radius: 6px; }

.shipping-info-block h4 { margin: 0 0 10px; font-size: .95rem; }

.info-row { display: flex; gap: 10px; margin-bottom: 6px; font-size: .88rem; }

.info-row .label { width: 130px; color: var(--muted); flex-shrink: 0; }

.note-box { font-style: italic; color: var(--accent); }



/* Notes Card */

.notes-list { max-height: 240px; overflow-y: auto; margin-bottom: 12px; }

.note-item { padding: 8px 10px; border-bottom: 1px dashed var(--border); font-size: .86rem; }

.note-meta { display: flex; justify-content: space-between; margin-bottom: 4px; }

.note-meta b { color: var(--accent); }

.note-meta small { color: var(--muted); }

.add-note-box { display: flex; gap: 8px; }



/* Summary card */

.sum-lines { display: flex; flex-direction: column; gap: 8px; font-size: .9rem; }

.sum-row { display: flex; justify-content: space-between; align-items: center; }

.sum-row.discount { color: var(--accent); }

.total-row { border-top: 2px solid var(--text); border-bottom: 1px dashed var(--border); padding: 10px 0; font-weight: 800; font-size: 1.15rem; margin-top: 6px; }

.total-price { color: var(--accent); font-size: 1.35rem; }

.paid-row { font-size: .95rem; }

.paid-ok { color: #16a34a; }

.text-danger { color: #dc2626; }



@media (max-width: 1080px) {

  .steps-matrix { grid-template-columns: repeat(2, 1fr); }

  .grid-layout { grid-template-columns: 1fr; }

}

@media (max-width: 640px) {

  .steps-matrix { grid-template-columns: 1fr; }

  .header-row { flex-direction: column; }

  .actions-group { align-items: flex-start; }

}

</style>

