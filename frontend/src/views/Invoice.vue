<template>
  <div class="inv-wrap">
    <div v-if="loading" class="spinner" />
    <div v-else-if="err" class="inv-err-box">
      <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="#ef4444" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      <h2>Không thể tải hoá đơn</h2>
      <p>{{ err }}</p>
      <router-link to="/account?tab=orders" class="btn btn-sm">Quay lại danh sách đơn hàng</router-link>
    </div>
    <div v-else-if="o" class="inv">
      <div class="noprint bar">
        <router-link :to="backLink" class="btn btn-sm btn-outline back-btn">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
          Quay lại đơn hàng
        </router-link>
        <div class="row" style="gap:8px;align-items:center">
          <span v-if="o.status === 'CANCELLED'" class="status-pill-cancel">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:3px"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>
            ĐÃ HỦY ĐƠN
          </span>
          <button type="button" class="btn btn-sm btn-outline noprint" @click="closeWindow" style="margin-right:6px">Đóng tab</button>
          <button v-if="o.status !== 'CANCELLED'" class="btn btn-sm btn-print" @click="printInvoice">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>
            In hoá đơn / PDF
          </button>
          <button v-else class="btn btn-sm btn-disabled" disabled title="Đơn hàng đã hủy, không thể in hóa đơn">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            Không thể in (Đã hủy)
          </button>
        </div>
      </div>

      <!-- Banner thông báo khi đơn hàng bị hủy -->
      <div v-if="o.status === 'CANCELLED'" class="cancel-alert">
        <div class="ca-icon"><svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="#b3261e" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg></div>
        <div class="ca-content">
          <h3>HÓA ĐƠN ĐÃ VÔ HIỆU HÓA — ĐƠN HÀNG ĐÃ BỊ HỦY</h3>
          <p v-if="o.cancelReason"><b>Lý do hủy:</b> {{ o.cancelReason }}</p>
          <small>Hóa đơn điện tử này đã được thu hồi và che mờ, không còn giá trị thanh toán, đối soát tài chính hoặc tra cứu bảo hành.</small>
        </div>
      </div>

      <div :class="['inv-container', { 'is-cancelled': o.status === 'CANCELLED' }]">
        <!-- Dấu đóng dấu VOID chéo lên hóa đơn bị hủy -->
        <div v-if="o.status === 'CANCELLED'" class="void-seal">
          <div class="void-box">
            <b>ĐÃ HỦY</b>
            <span>VOID / VÔ HIỆU HÓA</span>
            <small>{{ d(o.timeline?.CANCELLED || o.createdAt) }}</small>
          </div>
        </div>

        <header>
          <div>
            <img loading="lazy" decoding="async" src="/images/logo-mark.png" alt="" />
            <b>ARES Toys</b>
            <small>Chung cư Hoàng Huy Commerce, Võ Nguyên Giáp, Lê Chân, Hải Phòng · Hotline 1900 6868<br />MST 0312345678</small>
          </div>
          <div class="r">
            <h1 :class="{ 'strike': o.status === 'CANCELLED' }">HOÁ ĐƠN BÁN HÀNG</h1>
            <p>Số: <b>{{ o.code }}</b></p>
            <p>Ngày: {{ d(o.createdAt) }}</p>
            <p v-if="o.status === 'CANCELLED'" class="cancel-badge-text">TRẠNG THÁI: ĐÃ HỦY</p>
          </div>
        </header>

        <section class="who">
          <div>
            <small>Khách hàng</small>
            <b>{{ o.buyerName }}</b>
            <p>{{ o.phone }} · {{ o.email }}</p>
            <p>{{ o.shippingAddress }}</p>
          </div>
          <div>
            <small>Thanh toán & Vận chuyển</small>
            <b>{{ PAY_METHOD[o.paymentMethod] || o.paymentMethod }}</b>
            <p>{{ PAY_STATUS[o.paymentStatus] || o.paymentStatus }}<span v-if="o.transactionRef"> · {{ o.transactionRef }}</span></p>
            <p v-if="o.carrierName" class="carrier-line">{{ o.carrierName }}</p>
            <p v-if="o.refundedAmount" style="color:var(--danger,#c62828);font-weight:700">Đã hoàn: {{ vnd(o.refundedAmount) }}</p>
          </div>
        </section>

        <table>
          <thead>
            <tr><th>#</th><th>Sản phẩm</th><th>SKU</th><th>SL</th><th>Đơn giá</th><th>Thành tiền</th></tr>
          </thead>
          <tbody>
            <tr v-for="(i, n) in o.items" :key="i.id">
              <td>{{ n + 1 }}</td>
              <td>
                <div class="item-name-cell">
                  <span>{{ i.productName }}</span>
                  <small v-if="i.variantName" class="muted"> — {{ i.variantName }}</small>
                  <span v-if="isHazardItem(i.productName)" class="hazmat-pill">
                    <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 2c0 0-6 4-6 10a6 6 0 0 0 12 0c0-6-6-10-6-10z"/><path d="M12 18a3 3 0 0 0 3-3c0-2-3-4-3-4s-3 2-3 4a3 3 0 0 0 3 3z"/></svg>
                    Pin LiPo / Hàng cháy nổ
                  </span>
                </div>
              </td>
              <td>{{ i.sku }}</td>
              <td>{{ i.quantity }}</td>
              <td>{{ vnd(i.unitPrice) }}</td>
              <td>{{ vnd(i.unitPrice * i.quantity) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="sum">
          <p><span>Tạm tính</span><span>{{ vnd(o.subtotal) }}</span></p>
          <p v-if="o.discount"><span>Voucher {{ o.promoCode }}</span><span>−{{ vnd(o.discount) }}</span></p>
          <p v-if="o.pointsDiscount"><span>Điểm thưởng</span><span>−{{ vnd(o.pointsDiscount) }}</span></p>
          <p><span>Phí vận chuyển</span><span>{{ o.shippingFee ? vnd(o.shippingFee) : 'Miễn phí' }}</span></p>
          <p v-if="o.hazardousFee" class="hazmat-sum-row">
            <span>
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="#ea580c" stroke-width="2.2" style="display:inline-block;vertical-align:-2px;margin-right:4px"><path d="M12 2c0 0-6 4-6 10a6 6 0 0 0 12 0c0-6-6-10-6-10z"/><path d="M12 18a3 3 0 0 0 3-3c0-2-3-4-3-4s-3 2-3 4a3 3 0 0 0 3 3z"/></svg>
              Phụ phí pin & hàng cháy nổ
            </span>
            <span class="hazmat-val">+{{ vnd(o.hazardousFee) }}</span>
          </p>
          <p v-if="o.giftFee"><span>Gói quà thiệp chúc</span><span>+{{ vnd(o.giftFee) }}</span></p>
          <p class="t"><span>Tổng thanh toán</span><span>{{ vnd(o.total) }}</span></p>
          <p><span>Đã thanh toán</span><span>{{ vnd(o.paidAmount) }}</span></p>
          <p v-if="o.refundedAmount" style="color:#c62828"><span>Đã hoàn tiền</span><span>−{{ vnd(o.refundedAmount) }}</span></p>
        </div>

        <!-- Cảnh báo an toàn nếu đơn hàng có pin/cháy nổ -->
        <div v-if="o.hasHazardous || o.hazardousFee" class="inv-hazmat-alert">
          <div class="iha-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#c2410c" stroke-width="2.2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          </div>
          <div class="iha-txt">
            <b>Quy chuẩn an toàn hàng cháy nổ & Pin LiPo:</b>
            <span>Kiện hàng chứa pin LiPo / linh kiện pin sạc được đóng gói trong túi cách nhiệt chống cháy, chèn xốp giảm sốc đạt tiêu chuẩn an toàn PCCC & vận chuyển đường bộ theo quy định Bộ GTVT.</span>
          </div>
        </div>

        <footer>
          <template v-if="o.status !== 'CANCELLED'">
            Cảm ơn quý khách đã mua sắm tại ARES Toys! Hoá đơn điện tử có giá trị tra cứu bảo hành tại aresstore.vn/support
          </template>
          <template v-else>
            <span style="color:#c62828;font-weight:600">HÓA ĐƠN NÀY ĐÃ BỊ HỦY CÙNG ĐƠN HÀNG — KHÔNG CÒN HIỆU LỰC SỬ DỤNG.</span>
          </template>
        </footer>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { vnd, d, PAY_METHOD, PAY_STATUS } from '../format'

const route = useRoute()
const o = ref(null)
const loading = ref(true)
const err = ref('')

const backLink = computed(() => {
  return o.value?.code ? `/orders/${o.value.code}` : '/account?tab=orders'
})

function printInvoice() {
  setTimeout(() => {
    window.print()
  }, 60)
}

function closeWindow() {
  try {
    window.close()
  } catch (e) {}
  if (!window.closed) {
    window.history.back()
  }
}

function isHazardItem(name) {
  if (!name) return false
  const lower = name.toLowerCase()
  return lower.includes('pin ') || lower.startsWith('pin') || lower.includes('lipo') || lower.includes('battery') || lower.includes('ắc quy') || lower.includes('ac quy') || lower.includes('cháy nổ')
}

onMounted(async () => {
  loading.value = true
  err.value = ''
  try {
    const res = await api.get(`/orders/${route.params.code}`)
    o.value = res.data.order
  } catch (e) {
    err.value = e?.response?.data?.message || e.message || 'Không thể tải đơn hàng'
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.inv-wrap { background: #eee; min-height: 100vh; padding: 30px 12px; }
.inv { background: #fff; max-width: 820px; margin: 0 auto; padding: 36px 44px; border-radius: 6px; font-size: .9rem; position: relative; }
.inv-err-box { max-width: 500px; margin: 60px auto; background: #fff; padding: 32px; border-radius: 8px; text-align: center; border: 1px solid #fee2e2; }
.inv-err-box h2 { margin: 12px 0 6px; font-size: 1.2rem; color: #991b1b; }
.inv-err-box p { color: #6b7280; margin-bottom: 18px; font-size: .9rem; }
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.back-btn { text-decoration: none; display: inline-flex; align-items: center; }
.status-pill-cancel { background: #fee2e2; color: #b91c1c; font-weight: 700; padding: 4px 10px; border-radius: 6px; font-size: .8rem; border: 1px solid #f87171; display: inline-flex; align-items: center; }
.btn-print { background: #ea580c; color: #fff; border: 1px solid #c2410c; font-weight: 600; cursor: pointer; transition: background .15s; }
.btn-print:hover { background: #c2410c; }
.btn-disabled { opacity: .55; cursor: not-allowed; background: #e5e7eb; color: #6b7280; border: 1px solid #d1d5db; display: inline-flex; align-items: center; }

/* Cancel alert banner */
.cancel-alert { display: flex; gap: 14px; align-items: flex-start; background: #fef2f2; border: 2px dashed #ef4444; border-radius: 8px; padding: 14px 18px; margin-bottom: 24px; color: #991b1b; }
.ca-icon { font-size: 2rem; line-height: 1; flex-shrink: 0; }
.ca-content h3 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 800; color: #b91c1c; }
.ca-content p { margin: 2px 0 4px; font-size: .88rem; }
.ca-content small { display: block; color: #7f1d1d; opacity: .85; font-size: .78rem; }

/* Container & VOID seal */
.inv-container { position: relative; transition: filter .2s; }
.inv-container.is-cancelled { filter: grayscale(0.5); }
.inv-container.is-cancelled table,
.inv-container.is-cancelled .sum,
.inv-container.is-cancelled .who { opacity: .45; }

.void-seal { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; z-index: 10; pointer-events: none; }
.void-box { border: 5px solid #dc2626; color: #dc2626; padding: 18px 36px; border-radius: 12px; transform: rotate(-18deg); text-align: center; background: rgba(255, 255, 255, 0.88); box-shadow: 0 8px 32px rgba(220, 38, 38, 0.2); backdrop-filter: blur(2px); }
.void-box b { display: block; font-size: 2.6rem; font-weight: 900; letter-spacing: .2em; line-height: 1.1; }
.void-box span { display: block; font-size: 1rem; font-weight: 800; letter-spacing: .15em; border-top: 2px solid #dc2626; margin-top: 6px; padding-top: 6px; }
.void-box small { display: block; font-size: .8rem; margin-top: 4px; opacity: .85; }

.strike { text-decoration: line-through; color: #991b1b !important; }
.cancel-badge-text { color: #dc2626; font-weight: 800; font-size: .82rem; }

header { display: flex; justify-content: space-between; border-bottom: 3px solid var(--bronze, #ca8a04); padding-bottom: 16px; }
header img { height: 40px; vertical-align: middle; } header b { font-family: var(--head, sans-serif); font-size: 1.4rem; margin-left: 8px; } header small { display: block; color: #777; margin-top: 6px; }
.r { text-align: right; } .r h1 { font-size: 1.3rem; color: var(--bronze-3, #854d0e); }
.who { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 18px 0; } .who small { color: #888; text-transform: uppercase; font-size: .72rem; } .who b { display: block; }
.carrier-line { font-size: .82rem; color: #4b5563; margin-top: 2px; }

table { width: 100%; border-collapse: collapse; } th, td { padding: 8px; border-bottom: 1px solid #e5e5e5; text-align: left; } th { background: #f6f1e8; }
td:nth-child(n+4), th:nth-child(n+4) { text-align: right; }

.item-name-cell { display: flex; flex-direction: column; gap: 2px; align-items: flex-start; }
.hazmat-pill { display: inline-flex; align-items: center; gap: 4px; background: #fff7ed; color: #c2410c; border: 1px solid #ffedd5; padding: 2px 6px; border-radius: 4px; font-size: .72rem; font-weight: 700; margin-top: 2px; }

.sum { margin-left: auto; width: 340px; margin-top: 14px; } .sum p { display: flex; justify-content: space-between; padding: 4px 0; } .sum .t { font-weight: 800; font-size: 1.1rem; border-top: 2px solid #333; margin-top: 4px; padding-top: 8px; }
.hazmat-sum-row { color: #c2410c; font-weight: 600; }
.hazmat-val { color: #c2410c; font-weight: 700; }

.inv-hazmat-alert { display: flex; gap: 10px; align-items: flex-start; background: #fff7ed; border: 1px solid #fdba74; border-radius: 6px; padding: 10px 14px; margin-top: 20px; font-size: .8rem; color: #9a3412; }
.iha-icon { flex-shrink: 0; margin-top: 1px; }
.iha-txt b { display: block; font-size: .82rem; margin-bottom: 2px; color: #c2410c; }
.iha-txt span { line-height: 1.35; display: block; }

footer { margin-top: 30px; text-align: center; color: #888; font-size: .8rem; }
@media print {
  .inv-wrap { background: #fff; padding: 0; }
  .inv { box-shadow: none; padding: 0; }
  .noprint { display: none; }
  .is-cancelled { filter: none; }
  .void-box { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
}
</style>
