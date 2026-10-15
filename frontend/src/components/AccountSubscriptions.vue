<template>
  <div class="subs">
    <p class="muted small">Hệ thống tự tạo đơn theo chu kỳ bạn chọn, giảm {{ opts.discountPct }}% mỗi kỳ. Tạm dừng, bỏ qua một kỳ hoặc huỷ bất cứ lúc nào.</p>
    <div v-if="!list.length" class="card empty">Chưa có đăng ký mua định kỳ. Mở trang sản phẩm và chọn “Mua định kỳ”.</div>
    <div v-for="s in list" :key="s.id" class="card sub" :class="'st-' + s.status">
      <img v-if="s.image" :src="s.image" alt="" loading="lazy" />
      <div class="info">
        <router-link :to="`/products/${s.productId}`"><b>{{ s.productName }}</b></router-link>
        <div class="small muted">{{ s.quantity }} × {{ vnd(s.unitPrice) }} · mỗi {{ s.intervalDays }} ngày · {{ PAY_METHOD[s.paymentMethod] }}</div>
        <div class="small">
          <span class="badge">{{ ST[s.status] }}</span>
          <template v-if="s.status === 'ACTIVE'"> · Kỳ tới: <b>{{ d(s.nextRunAt) }}</b></template>
          · Đã giao {{ s.runs }} kỳ<template v-if="s.lastOrderCode"> · <router-link :to="`/orders/${s.lastOrderCode}`">Đơn gần nhất</router-link></template>
        </div>
        <div v-if="s.lastError" class="small err">Lỗi gần nhất: {{ s.lastError }}</div>
      </div>
      <div v-if="s.status !== 'CANCELLED'" class="act">
        <select class="input sel" :value="s.intervalDays" aria-label="Chu kỳ" @change="patch(s, { intervalDays: +$event.target.value })"><option v-for="i in opts.intervals" :key="i" :value="i">{{ i }} ngày</option></select>
        <button v-if="s.status === 'ACTIVE'" class="btn btn-sm btn-outline" @click="patch(s, { skip: true })">Bỏ qua kỳ tới</button>
        <button v-if="s.status === 'ACTIVE'" class="btn btn-sm btn-outline" @click="patch(s, { status: 'PAUSED' })">Tạm dừng</button>
        <button v-else class="btn btn-sm" @click="patch(s, { status: 'ACTIVE' })">Tiếp tục</button>
        <button class="btn btn-sm btn-danger" @click="cancel(s)">Huỷ</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../api'
import { vnd, d, PAY_METHOD } from '../format'
import { toast } from '../stores/prefs'
const ST = { ACTIVE: 'Đang hoạt động', PAUSED: 'Tạm dừng', CANCELLED: 'Đã huỷ' }
const list = ref([]), opts = ref({ intervals: [7, 14, 30, 60, 90], discountPct: 5 })
async function load() { list.value = (await api.get('/subscriptions')).data; opts.value = (await api.get('/public/subscriptions/options')).data }
async function patch(s, body) { try { await api.patch(`/subscriptions/${s.id}`, body); toast('Đã cập nhật đăng ký'); load() } catch (e) { toast(errMsg(e), 'error') } }
function cancel(s) { if (confirm('Huỷ đăng ký mua định kỳ này?')) patch(s, { status: 'CANCELLED' }) }
onMounted(load)
</script>
<style scoped>
.subs { display: flex; flex-direction: column; gap: 12px; } .empty { padding: 20px; }
.sub { display: grid; grid-template-columns: 64px 1fr auto; gap: 14px; align-items: center; } .sub img { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; background: #f3f3f3; }
.info { display: flex; flex-direction: column; gap: 3px; min-width: 0; } .act { display: flex; flex-wrap: wrap; gap: 6px; justify-content: flex-end; } .sel { width: auto; padding: 5px 8px; }
.st-CANCELLED { opacity: .55; } .err { color: var(--danger); }
@media (max-width: 720px) { .sub { grid-template-columns: 48px 1fr; } .sub img { width: 48px; height: 48px; } .act { grid-column: 1 / -1; justify-content: flex-start; } }
</style>
