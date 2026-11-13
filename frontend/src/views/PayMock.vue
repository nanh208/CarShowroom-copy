<template>
  <div class="paypage" :class="payType">
    <div v-if="!o" class="pay-loading">
      <svg class="spin-icon" viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2.5">
        <circle cx="12" cy="12" r="10" stroke-opacity="0.25" />
        <path d="M12 2a10 10 0 0 1 10 10" />
      </svg>
      <p>{{ loadErr || 'Đang khởi tạo cổng thanh toán...' }}</p>
    </div>

    <div v-else class="gw">
      <!-- Header Bar -->
      <div class="gh">
        <div class="gh-brand">
          <svg v-if="payType === 'BANK_QR'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" />
          </svg>
          <span v-else-if="payType === 'MOMO'" class="gh-tag-momo">MOMO</span>
          <span v-else class="gh-tag-vnpay">VNPAY</span>
          <b>{{ payTitle }}</b>
        </div>
        <span class="gh-sub">Tự động xác nhận khi tiền về · Bảo mật SSL</span>
      </div>

      <!-- Kết quả trả về từ cổng (chỉ hiển thị – đơn chỉ PAID khi server nhận IPN/webhook hợp lệ) -->
      <div v-if="returnMsg" :class="['gw-banner', returnMsg.kind]">{{ returnMsg.text }}</div>

      <div class="gb">
        <!-- QR Code Column -->
        <div class="qr-col">
          <div class="qr-box">
            <div class="qr-frame">
              <img v-if="qrSrc" :src="qrSrc" alt="Mã QR thanh toán" class="qr-img" @error="qrImgErr = true" />
              <div v-else class="qr-empty">{{ qrPlaceholder }}</div>
            </div>
            <div class="qr-caption">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
              <span>{{ payScanHint }}</span>
            </div>
          </div>
        </div>

        <!-- Transfer Information Column -->
        <div class="info-col">
          <div class="info-group">
            <div class="info-row">
              <span class="lbl">Mã đơn hàng</span>
              <div class="copy-val">
                <b class="val code">{{ o.code }}</b>
                <button type="button" class="btn-copy" title="Sao chép mã đơn" @click="copyText(o.code, 'mã đơn hàng')">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                  <span>Sao chép</span>
                </button>
              </div>
            </div>

            <!-- Thông tin chuyển khoản lấy từ cấu hình server (BANK_BIN / BANK_ACCOUNT_NO / BANK_ACCOUNT_NAME) -->
            <template v-if="payType === 'BANK_QR' && bank">
              <div v-if="!bank.configured" class="pay-note warn">Shop chưa cấu hình tài khoản nhận chuyển khoản. Vui lòng chọn phương thức khác hoặc liên hệ shop.</div>
              <template v-else>
                <div class="info-row">
                  <span class="lbl">Ngân hàng</span>
                  <b class="val">{{ bank.bank }}</b>
                </div>
                <div class="info-row">
                  <span class="lbl">Chủ tài khoản</span>
                  <b class="val">{{ bank.holder }}</b>
                </div>
                <div class="info-row">
                  <span class="lbl">Số tài khoản</span>
                  <div class="copy-val">
                    <b class="val account-no">{{ bank.account }}</b>
                    <button type="button" class="btn-copy" title="Sao chép số tài khoản" @click="copyText(bank.account, 'số tài khoản')">
                      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                      <span>Sao chép</span>
                    </button>
                  </div>
                </div>
                <div class="info-row highlight-row">
                  <span class="lbl">Nội dung chuyển tiền</span>
                  <div class="copy-val">
                    <b class="val transfer-note">{{ o.code }}</b>
                    <button type="button" class="btn-copy" title="Sao chép nội dung" @click="copyText(o.code, 'nội dung chuyển khoản')">
                      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                      <span>Sao chép</span>
                    </button>
                  </div>
                </div>
              </template>
            </template>

            <!-- Total Amount -->
            <div class="info-row total-row">
              <span class="lbl">Số tiền cần thanh toán</span>
              <div class="copy-val">
                <b class="amt">{{ vnd(amountDue) }}</b>
                <button type="button" class="btn-copy" title="Sao chép số tiền" @click="copyText(String(amountDue), 'số tiền')">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                  <span>Sao chép</span>
                </button>
              </div>
            </div>
          </div>

          <div v-if="payType === 'BANK_QR'" class="pay-note">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <span v-if="cfg.bankAutoConfirm">Chuyển <b>đủ số tiền</b> với <b>nội dung {{ o.code }}</b>. Đơn sẽ tự động được xác nhận ngay khi ngân hàng báo tiền về.</span>
            <span v-else>Chuyển <b>đủ số tiền</b> với <b>nội dung {{ o.code }}</b>. Shop sẽ đối soát và xác nhận đơn thủ công.</span>
          </div>

          <!-- Nút mở cổng thanh toán thật -->
          <div class="sandbox-direct-panel">
            <template v-if="payType === 'VNPAY'">
              <button type="button" class="btn btn-primary btn-block btn-lg btn-sandbox-main vnpay-color" :disabled="busy || !cfg.vnpay" @click="openVnPay">
                {{ !cfg.vnpay ? 'VNPay chưa được cấu hình' : busy ? 'Đang chuyển tới VNPay...' : 'THANH TOÁN QUA VNPAY' }}
              </button>
              <div v-if="cfg.vnpay" class="testcard-box">
                <div class="tc-title">Thẻ test (chỉ dùng trên môi trường sandbox VNPay):</div>
                <div class="tc-line">Ngân hàng <b>NCB</b> · Số thẻ <b>9704198526191432198</b> · <b>NGUYEN VAN A</b> · <b>07/15</b> · OTP <b>123456</b></div>
              </div>
            </template>

            <template v-else-if="payType === 'MOMO'">
              <button type="button" class="btn btn-primary btn-block btn-lg btn-sandbox-main momo-color" :disabled="busy || !cfg.momo" @click="openMoMo">
                {{ !cfg.momo ? 'MoMo chưa được cấu hình' : busy ? 'Đang tạo giao dịch MoMo...' : momoPayUrl ? 'MỞ TRANG THANH TOÁN MOMO' : 'TẠO MÃ THANH TOÁN MOMO' }}
              </button>
            </template>

            <template v-else-if="cfg.payos">
              <button type="button" class="btn btn-block btn-outline" :disabled="busy" @click="openPayOs">
                {{ busy ? 'Đang tạo link payOS...' : 'Hoặc thanh toán qua trang payOS' }}
              </button>
            </template>
          </div>

          <!-- Trạng thái chờ tiền về -->
          <div class="momo-listening-box">
            <div class="pulse-ring">
              <div class="pulse-dot" />
            </div>
            <div class="mlb-text">
              <b>Đang chờ xác nhận thanh toán...</b>
              <p class="small muted">Trang tự cập nhật khi hệ thống nhận được tiền (thường vài giây sau khi chuyển).</p>
            </div>
          </div>

          <div class="pay-actions-clean">
            <router-link :to="`/orders/${o.code}`" class="btn btn-block btn-outline">
              ← Quay lại xem đơn hàng
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// Trang thanh toán THẬT: không còn nút "xác nhận đã trả". Trạng thái chỉ đổi khi backend nhận webhook/IPN hợp lệ.
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import api, { errMsg } from '../api'
import { vnd } from '../format'
import { toast } from '../stores/prefs'

const route = useRoute(), router = useRouter()
const o = ref(null), busy = ref(false), qrImgErr = ref(false), loadErr = ref('')
const cfg = ref({}), bank = ref(null), status = ref(null)
const momoPayUrl = ref(''), gatewayQr = ref('')

const payType = computed(() => {
  const m = o.value?.paymentMethod
  if (m === 'MOMO') return 'MOMO'
  if (m === 'VNPAY') return 'VNPAY'
  return 'BANK_QR'
})

const amountDue = computed(() => status.value?.amountDue ?? bank.value?.amount ?? Math.max(0, (o.value?.total || 0) - (o.value?.paidAmount || 0)))

const payTitle = computed(() => {
  if (payType.value === 'MOMO') return 'Thanh toán Ví MoMo'
  if (payType.value === 'VNPAY') return 'Thanh toán VNPAY'
  return `Chuyển khoản VietQR 24/7${bank.value?.bank ? ' (' + bank.value.bank + ')' : ''}`
})

const payScanHint = computed(() => {
  if (payType.value === 'MOMO') return 'Mở app MoMo quét mã QR'
  if (payType.value === 'VNPAY') return 'Bấm nút bên cạnh để sang cổng VNPay'
  return 'Mở app ngân hàng bất kỳ để quét mã VietQR'
})

const qrSrc = computed(() => {
  if (gatewayQr.value) return gatewayQr.value
  if (payType.value === 'BANK_QR' && bank.value?.qr && !qrImgErr.value) return bank.value.qr
  return ''
})
const qrPlaceholder = computed(() => payType.value === 'VNPAY' ? 'VNPAY' : payType.value === 'MOMO' ? 'Bấm "Tạo mã thanh toán MoMo"' : 'Không tải được mã QR')

const returnMsg = computed(() => {
  const r = route.query.result, gw = String(route.query.gw || '').toUpperCase()
  if (!r && !gw) return null
  if (r === 'success') return { kind: 'info', text: `${gw}: giao dịch đã hoàn tất phía cổng. Đang chờ cổng gửi xác nhận (IPN) về hệ thống...` }
  if (r === 'cancel') return { kind: 'warn', text: 'Bạn đã huỷ thanh toán. Có thể thử lại bên dưới.' }
  if (r === 'failed') return { kind: 'warn', text: `${gw}: giao dịch không thành công hoặc đã huỷ.` }
  if (r === 'invalid') return { kind: 'error', text: 'Dữ liệu trả về không hợp lệ (sai chữ ký). Đơn chưa được thanh toán.' }
  return { kind: 'info', text: 'Đang kiểm tra trạng thái thanh toán...' }
})

function copyText(val, label) {
  if (navigator.clipboard) navigator.clipboard.writeText(val)
  toast(`Đã sao chép ${label}: ${val}`)
}

async function toQrImage(text) {
  if (!text) return ''
  if (/^https?:\/\//.test(text) && /\.(png|jpe?g|svg)(\?|$)/i.test(text)) return text
  try { return await QRCode.toDataURL(text, { margin: 1, width: 320 }) } catch { return '' }
}

async function openVnPay() {
  busy.value = true
  try {
    const { data } = await api.post('/payment/vnpay/create-url', { orderCode: o.value.code })
    location.href = data.paymentUrl
  } catch (e) { toast(errMsg(e), 'error'); busy.value = false }
}

async function openMoMo() {
  if (momoPayUrl.value) { location.href = momoPayUrl.value; return }
  busy.value = true
  try {
    const { data } = await api.post('/payment/momo/create-url', { orderCode: o.value.code })
    momoPayUrl.value = data.payUrl || ''
    gatewayQr.value = await toQrImage(data.qrCodeUrl)
    if (!gatewayQr.value && momoPayUrl.value) location.href = momoPayUrl.value
  } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}

async function openPayOs() {
  busy.value = true
  try {
    const { data } = await api.post('/payment/payos/create-link', { orderCode: o.value.code })
    if (data.checkoutUrl) location.href = data.checkoutUrl
  } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}

let pollTimer = null
async function poll() {
  try {
    const { data } = await api.get(`/payment/status/${o.value.code}`)
    status.value = data
    if (data.paymentStatus === 'PAID') {
      clearInterval(pollTimer); pollTimer = null
      toast('Đã nhận được tiền. Thanh toán thành công!')
      router.push(`/orders/${data.code}?new=1`)
    }
  } catch {}
}

onMounted(async () => {
  try {
    const { data } = await api.get(`/orders/${route.params.code}`)
    o.value = data.order
    if (o.value.paymentStatus === 'PAID') return router.replace(`/orders/${o.value.code}`)
    if (o.value.status === 'CANCELLED') { toast('Đơn hàng đã huỷ', 'error'); return router.replace(`/orders/${o.value.code}`) }
    cfg.value = (await api.get('/payment/config')).data
    if (payType.value === 'BANK_QR') bank.value = (await api.get(`/payment/vietqr/${o.value.code}`)).data
  } catch (e) {
    loadErr.value = errMsg(e)
    toast(loadErr.value, 'error')
    return
  }
  poll()
  pollTimer = setInterval(poll, 3000)
})

onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.gw-banner { padding: 10px 24px; font-size: .9rem; }
.gw-banner.info { background: #eef6ff; color: #0b4a8b; }
.gw-banner.warn { background: #fff6e5; color: #8a5300; }
.gw-banner.error { background: #fdecec; color: #9b1c1c; }
.pay-note.warn { background: #fff6e5; color: #8a5300; }
.qr-empty { width: 220px; height: 220px; display: flex; align-items: center; justify-content: center; text-align: center; color: #777; font-size: .85rem; padding: 12px; }
.paypage {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg, #f3efe6);
  padding: 24px 16px;
  font-family: var(--head, 'Oswald', 'Be Vietnam Pro', sans-serif);
}

.pay-loading {
  text-align: center;
  color: var(--muted, #666);
}

.spin-icon {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.gw {
  width: min(840px, 100%);
  background: var(--card, #ffffff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 24px 60px -15px rgba(0, 0, 0, 0.2);
  border: 1px solid var(--border, #ddd5c6);
}

.gh {
  padding: 18px 24px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #005baa;
}

.paypage.MOMO .gh {
  background: #a50064;
}

.paypage.BANK_QR .gh {
  background: #111827;
  border-bottom: 3px solid #f05a1a;
}

.gh-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.gh-brand b {
  font-size: 1.25rem;
  letter-spacing: .02em;
}

.gh-sub {
  font-size: 0.8rem;
  opacity: 0.85;
}

.gh-tag-momo, .gh-tag-vnpay {
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
}

.gb {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 28px;
  padding: 30px 28px;
}

@media (max-width: 768px) {
  .gb {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 20px 16px;
  }
}

.qr-col {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-box {
  background: #ffffff;
  padding: 16px;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 280px;
}

.qr-frame {
  width: 240px;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.qr-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-svg {
  width: 210px;
  height: 210px;
}

.qr-caption {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.78rem;
  color: #475569;
  text-align: center;
  line-height: 1.3;
}

.qr-timer {
  margin-top: 10px;
  font-size: 0.82rem;
  color: var(--muted, #64748b);
}

.info-col {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.info-row.highlight-row {
  background: #fff7ed;
  border-color: #fed7aa;
}

.info-row.total-row {
  background: #f1f5f9;
  border-color: #cbd5e1;
  padding: 12px;
}

.info-row .lbl {
  font-size: 0.84rem;
  color: #475569;
  font-weight: 500;
}

.info-row .val {
  font-size: 0.95rem;
  color: #0f172a;
}

.copy-val {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-copy {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 4px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-copy:hover {
  background: #f05a1a;
  color: #fff;
  border-color: #f05a1a;
}

.amt {
  font-size: 1.45rem !important;
  color: #f05a1a !important;
  font-weight: 700;
}

.paypage.MOMO .amt {
  color: #a50064 !important;
}

.paypage.VNPAY .amt {
  color: #005baa !important;
}

.pay-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 16px 0;
  padding: 10px 12px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  color: #1e40af;
  font-size: 0.82rem;
  line-height: 1.45;
}

.pay-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-confirm {
  background: #f05a1a;
  color: #fff;
  padding: 14px 20px;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: .02em;
}

.btn-confirm:hover {
  background: #d94a0e;
}

.btn-fail {
  background: #fff;
  color: #dc2626;
  border: 1.5px solid #dc2626;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 700;
  letter-spacing: .04em;
  padding: 12px;
  transition: all .2s;
  cursor: pointer;
}
.btn-fail:hover {
  background: #fef2f2;
}
.cancel-link {
  text-align: center;
  font-size: 0.86rem;
  color: #64748b;
  text-decoration: none;
  transition: color 0.15s;
}

.cancel-link:hover {
  color: #f05a1a;
}
.sandbox-action-box {
  margin: 14px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.btn-sandbox-vnpay {
  background: #005baa;
  color: #fff;
  text-decoration: none;
  font-weight: 800;
  padding: 12px;
  text-align: center;
  border-radius: 6px;
}
.btn-sandbox-vnpay:hover {
  background: #004480;
}
.btn-sandbox-momo {
  background: #a50064;
  color: #fff;
  text-decoration: none;
  font-weight: 800;
  padding: 12px;
  text-align: center;
  border-radius: 6px;
}
.btn-sandbox-momo:hover {
  background: #80004e;
}
.testcard-box {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 0.82rem;
  line-height: 1.5;
  color: #1e293b;
}
.tc-title {
  font-weight: 800;
  margin-bottom: 4px;
  color: #0f172a;
}
.tc-line {
  margin: 2px 0;
}

.sandbox-direct-panel {
  margin: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.btn-sandbox-main {
  padding: 14px 18px;
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  color: #fff;
  transition: filter 0.2s;
}
.btn-sandbox-main:hover {
  filter: brightness(1.1);
}
.btn-sandbox-main.vnpay-color {
  background: #005baa;
}
.btn-sandbox-main.momo-color {
  background: #a50064;
}
.btn-sandbox-main.qr-color {
  background: #111827;
}
.sandbox-instant-note {
  font-size: 0.78rem;
  color: var(--muted);
  text-align: center;
  margin-top: 4px;
}

</style>
