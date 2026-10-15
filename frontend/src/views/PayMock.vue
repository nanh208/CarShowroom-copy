<template>
  <div class="paypage" :class="o?.paymentMethod">
    <div v-if="!o" class="spinner" />
    <div v-else class="gw">
      <div class="gh"><b>{{ o.paymentMethod === 'MOMO' ? 'MoMo' : 'VNPAY' }}</b><span>Cổng thanh toán giả lập · Sandbox</span></div>
      <div class="gb">
        <div class="qr"><svg viewBox="0 0 29 29" width="180" height="180"><rect width="29" height="29" fill="#fff"/><path :d="qrPath" fill="#111"/></svg><small>Quét mã bằng app {{ o.paymentMethod === 'MOMO' ? 'MoMo' : 'ngân hàng / VNPay' }}</small></div>
        <div class="info">
          <p>Nhà cung cấp<br /><b>ARES Toys</b></p><p>Mã đơn hàng<br /><b>{{ o.code }}</b></p>
          <p>Số tiền<br /><b class="amt">{{ vnd(tx?.amount ?? (o.total - o.paidAmount)) }}</b></p>
          <p v-if="tx" class="small">Mã giao dịch <b class="mono">{{ tx.txnRef }}</b><br />Hết hạn sau <b :class="{ late: left < 60 }">{{ mmss }}</b></p>
          <p v-if="err" class="alert alert-error small">{{ err }}</p>
          <div v-if="tx && banks.length > 1" class="banks" role="radiogroup" aria-label="Chọn ngân hàng">
            <label v-for="b in banks" :key="b" :class="{ on: bank === b }"><input v-model="bank" type="radio" :value="b" />{{ BANKS[b] || b }}</label>
          </div>
          <p class="muted small">Môi trường sandbox — không có giao dịch thật. Tham số được ký HMAC-SHA512 như VNPay.</p>
          <Speedometer v-if="busy" class="spd-pay" label="Đang xử lý thanh toán…" />
          <template v-else-if="tx">
            <button class="btn btn-block" :disabled="left <= 0" @click="finish('success')">Xác nhận thanh toán</button>
            <button class="link-btn" :disabled="left <= 0" @click="finish('insufficient')">Mô phỏng lỗi: không đủ số dư</button>
            <button class="cancel" @click="finish('cancel')">Huỷ giao dịch, quay lại cửa hàng</button>
          </template>
          <router-link v-else :to="`/orders/${o.code}`" class="cancel">Quay lại đơn hàng</router-link>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { vnd } from '../format'
import { toast } from '../stores/prefs'
import Speedometer from '../components/Speedometer.vue'
const route = useRoute(), router = useRouter(), o = ref(null), busy = ref(false)
// Mã QR giả (họa tiết ngẫu nhiên theo mã đơn) — chỉ minh hoạ
const qrPath = computed(() => {
  let s = 0; for (const c of route.params.code) s = (s * 31 + c.charCodeAt(0)) >>> 0
  const rnd = () => (s = (s * 1103515245 + 12345) >>> 0) / 2 ** 32
  let d = ''; const fin = (x, y) => { d += `M${x} ${y}h7v7h-7zM${x + 1} ${y + 1}v5h5v-5zM${x + 2} ${y + 2}h3v3h-3z` }
  fin(1, 1); fin(21, 1); fin(1, 21)
  for (let y = 1; y < 28; y++) for (let x = 1; x < 28; x++) { if ((x < 9 && y < 9) || (x > 19 && y < 9) || (x < 9 && y > 19)) continue; if (rnd() > .55) d += `M${x} ${y}h1v1h-1z` }
  return d
})
const tx = ref(null), banks = ref([]), bank = ref(''), err = ref(''), now = ref(Date.now())
const BANKS = { NCB: 'NCB', VCB: 'Vietcombank', TCB: 'Techcombank', BIDV: 'BIDV', INTCARD: 'Thẻ quốc tế', MOMO: 'Ví MoMo' }
const left = computed(() => tx.value ? Math.max(0, Math.floor((new Date(tx.value.expiresAt) - now.value) / 1000)) : 0)
const mmss = computed(() => `${String(Math.floor(left.value / 60)).padStart(2, '0')}:${String(left.value % 60).padStart(2, '0')}`)
let tick
onMounted(async () => {
  try {
    o.value = (await api.get(`/orders/${route.params.code}`)).data.order
    if (o.value.paymentStatus === 'PAID') { toast('Đơn đã được thanh toán'); return router.replace(`/orders/${o.value.code}`) }
    // 1) shop tạo giao dịch + tham số ký → 2) "cổng" kiểm chữ ký và hiển thị
    const { data } = await api.post(`/payments/${o.value.code}/create`, { provider: o.value.paymentMethod })
    if (!data.sandbox) { err.value = 'Cổng thanh toán thử nghiệm đã tắt trên máy chủ này.'; return }
    const info = (await api.post('/public/payments/sandbox/inspect', data.params)).data
    tx.value = { ...data, ...info }; banks.value = info.banks; bank.value = info.banks[0]
    tick = setInterval(() => (now.value = Date.now()), 1000)
  } catch (e) { err.value = errMsg(e) }
})
onUnmounted(() => clearInterval(tick))
async function finish(result) {
  busy.value = true
  try {
    await new Promise(r => setTimeout(r, result === 'cancel' ? 200 : 900))
    const { data } = await api.post(`/public/payments/sandbox/${tx.value.txnRef}/complete`, { result, bankCode: bank.value, token: tx.value.sandboxToken })
    router.replace({ path: '/pay/return', query: data.returnParams })
  } catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
</script>
<style scoped>
.banks { display: grid; grid-template-columns: repeat(auto-fill, minmax(110px, 1fr)); gap: 6px; margin: 8px 0; } .banks label { border: 1px solid #ddd; border-radius: 8px; padding: 8px; font-size: .82rem; cursor: pointer; display: flex; gap: 6px; align-items: center; color: #111; } .banks label.on { border-color: #005baa; background: #eef5ff; } .MOMO .banks label.on { border-color: #a50064; background: #fdf0f7; }
.link-btn { background: none; border: 0; color: #777; font-size: .8rem; text-decoration: underline; display: block; margin: 8px auto 0; cursor: pointer; } .cancel { background: none; border: 0; width: 100%; cursor: pointer; font-family: inherit; } .late { color: #c62828; }
.paypage { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f2f2f2; padding: 24px; }
.paypage.MOMO { background: linear-gradient(135deg, #fde2f1, #f7f7f7); } .paypage.VNPAY { background: linear-gradient(135deg, #dbeafe, #f7f7f7); }
.gw { width: min(720px, 100%); background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 60px -20px rgba(0,0,0,.3); }
.gh { padding: 16px 22px; color: #fff; display: flex; justify-content: space-between; align-items: center; background: #005baa; } .MOMO .gh { background: #a50064; }
.gh b { font-size: 1.4rem; } .gh span { font-size: .8rem; opacity: .85; }
.gb { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; padding: 24px; }
.qr { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 14px; border: 1px solid #eee; border-radius: 12px; } .qr small { color: #777; }
.info p { margin-bottom: 10px; color: #666; font-size: .88rem; } .info b { color: #111; font-size: 1rem; } .amt { font-size: 1.5rem !important; color: #a50064 !important; }
.VNPAY .amt { color: #005baa !important; }
.cancel { display: block; text-align: center; margin-top: 10px; font-size: .85rem; color: #777; }
@media (max-width: 600px) { .gb { grid-template-columns: 1fr; } }
.spd-pay { display: flex; margin: 6px auto; }
</style>
