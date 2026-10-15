<template>
  <div class="sb">
    <div class="row between top">
      <div><b>Mua định kỳ</b> <small class="muted">· giảm {{ opts.discountPct }}% mỗi kỳ, tự giao tận nhà</small></div>
      <button class="btn btn-sm btn-outline" @click="toggle">{{ open ? 'Đóng' : '↻ Đăng ký' }}</button>
    </div>
    <Transition name="fade">
      <form v-if="open" class="f" @submit.prevent="submit">
        <label class="small">Chu kỳ<select v-model.number="f.intervalDays" class="input"><option v-for="i in opts.intervals" :key="i" :value="i">Mỗi {{ i }} ngày</option></select></label>
        <label class="small">Thanh toán<select v-model="f.paymentMethod" class="input"><option value="COD">Thanh toán khi nhận hàng (COD)</option><option value="BANK_QR">Chuyển khoản QR</option></select></label>
        <label class="small">Địa chỉ giao
          <select v-if="addrs.length" v-model="addrId" class="input"><option v-for="a in addrs" :key="a.id" :value="a.id">{{ a.receiverName }} · {{ a.line }}, {{ a.city }}</option><option :value="0">Nhập địa chỉ khác</option></select>
        </label>
        <template v-if="!addrs.length || addrId === 0">
          <input v-model.trim="f.buyerName" class="input" required placeholder="Họ và tên *" aria-label="Họ và tên" />
          <input v-model.trim="f.phone" class="input" required placeholder="Số điện thoại *" aria-label="Số điện thoại" />
          <input v-model.trim="f.shippingAddress" class="input full" required placeholder="Số nhà, tên đường *" aria-label="Địa chỉ" />
          <input v-model.trim="f.city" class="input" placeholder="Tỉnh / Thành phố" aria-label="Tỉnh / Thành phố" />
        </template>
        <p class="small muted full">Mỗi kỳ: {{ qty }} × {{ vnd(price) }} − {{ opts.discountPct }}% ≈ <b>{{ vnd(Math.round(price * qty * (100 - opts.discountPct) / 100)) }}</b> + phí ship. Kỳ đầu tiên sau {{ f.intervalDays }} ngày.</p>
        <button class="btn btn-sm full" :disabled="busy || disabled">{{ busy ? 'Đang lưu…' : 'Xác nhận đăng ký' }}</button>
      </form>
    </Transition>
  </div>
</template>
<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { vnd } from '../format'
import { toast } from '../stores/prefs'
import { useAuth } from '../stores/auth'
const props = defineProps({ productId: Number, variantId: Number, qty: { type: Number, default: 1 }, price: Number, disabled: Boolean })
const auth = useAuth(), router = useRouter()
const open = ref(false), busy = ref(false), addrs = ref([]), addrId = ref(0), opts = ref({ intervals: [7, 14, 30, 60, 90], discountPct: 5 })
const f = reactive({ intervalDays: 30, paymentMethod: 'COD', buyerName: auth.user?.fullName || '', phone: auth.user?.phone || '', shippingAddress: '', city: '' })
api.get('/public/subscriptions/options').then(r => { opts.value = r.data }).catch(() => {})
async function toggle() {
  if (!auth.loggedIn) return router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  open.value = !open.value
  if (open.value && !addrs.value.length) try { addrs.value = (await api.get('/account/addresses')).data; addrId.value = (addrs.value.find(a => a.isDefault) || addrs.value[0])?.id || 0 } catch { /* bỏ qua */ }
}
async function submit() {
  const a = addrs.value.find(x => x.id === addrId.value)
  const body = { productId: props.productId, variantId: props.variantId || null, quantity: props.qty, intervalDays: f.intervalDays, paymentMethod: f.paymentMethod,
    buyerName: a ? a.receiverName : f.buyerName, phone: a ? a.phone : f.phone, shippingAddress: a ? [a.line, a.ward, a.district].filter(Boolean).join(', ') : f.shippingAddress, city: a ? a.city : f.city }
  busy.value = true
  try { await api.post('/subscriptions', body); open.value = false; toast('Đã đăng ký mua định kỳ — quản lý trong Tài khoản › Mua định kỳ') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
</script>
<style scoped>
.sb { border: 1px solid var(--border); border-radius: 10px; padding: 12px 14px; margin-top: 12px; }
.f { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 10px; } .f label { display: flex; flex-direction: column; gap: 4px; } .full { grid-column: 1 / -1; }
@media (max-width: 560px) { .f { grid-template-columns: 1fr; } }
</style>
