<template>
  <div class="paypage" :class="o?.paymentMethod">
    <div v-if="!o" class="spinner" />
    <div v-else class="gw">
      <div class="gh"><b>{{ o.paymentMethod === 'MOMO' ? 'MoMo' : 'VNPAY' }}</b><span>Cổng thanh toán giả lập · Sandbox</span></div>
      <div class="gb">
        <div class="qr"><svg viewBox="0 0 29 29" width="180" height="180"><rect width="29" height="29" fill="#fff"/><path :d="qrPath" fill="#111"/></svg><small>Quét mã bằng app {{ o.paymentMethod === 'MOMO' ? 'MoMo' : 'ngân hàng / VNPay' }}</small></div>
        <div class="info">
          <p>Nhà cung cấp<br /><b>ARES Toys</b></p><p>Mã đơn hàng<br /><b>{{ o.code }}</b></p>
          <p>Số tiền<br /><b class="amt">{{ vnd(o.total - o.paidAmount) }}</b></p>
          <p class="muted small">Đây là môi trường demo, không có giao dịch thật.</p>
          <button class="btn btn-block" :disabled="busy" @click="pay">{{ busy ? 'Đang xác nhận…' : 'Xác nhận thanh toán' }}</button>
          <router-link :to="`/orders/${o.code}`" class="cancel">Huỷ, quay lại đơn hàng</router-link>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { vnd } from '../format'
import { toast } from '../stores/prefs'
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
onMounted(async () => { o.value = (await api.get(`/orders/${route.params.code}`)).data.order })
async function pay() {
  busy.value = true
  try { await new Promise(r => setTimeout(r, 900)); await api.post(`/orders/${o.value.code}/pay`); toast('Thanh toán thành công'); router.push(`/orders/${o.value.code}?new=1`) }
  catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
</script>
<style scoped>
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
</style>
