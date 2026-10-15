<template>
  <div class="ret">
    <div class="card box">
      <div v-if="!r" class="spinner" />
      <template v-else>
        <div :class="['ic', r.success ? 'ok' : 'bad']" aria-hidden="true">{{ r.success ? '✓' : '✕' }}</div>
        <h1>{{ r.success ? 'Thanh toán thành công' : 'Thanh toán chưa thành công' }}</h1>
        <p class="muted">{{ r.message }}<template v-if="r.provider"> · {{ r.provider }}</template></p>
        <p v-if="r.amount" class="amt money">{{ vnd(r.amount) }}</p>
        <p v-if="!r.valid" class="alert alert-error small">Chữ ký phản hồi không hợp lệ — giao dịch không được ghi nhận.</p>
        <div class="row" style="gap:8px;justify-content:center;flex-wrap:wrap">
          <router-link v-if="r.orderCode" :to="`/orders/${r.orderCode}${r.success ? '?new=1' : ''}`" class="btn">Xem đơn hàng</router-link>
          <router-link v-if="r.orderCode && !r.success" :to="`/pay/${r.orderCode}`" class="btn btn-outline">Thử thanh toán lại</router-link>
          <router-link to="/products" class="btn btn-outline">Tiếp tục mua sắm</router-link>
        </div>
      </template>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { vnd } from '../format'
const route = useRoute(), r = ref(null)
onMounted(async () => { try { r.value = (await api.get('/public/payments/return', { params: route.query })).data } catch { r.value = { success: false, valid: false, message: 'Không xác minh được giao dịch' } } })
</script>
<style scoped>
.ret { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
.box { width: min(460px, 100%); text-align: center; padding: 32px 24px; display: flex; flex-direction: column; gap: 10px; align-items: center; animation: pop .35s ease-out; }
.ic { width: 64px; height: 64px; border-radius: 50%; display: grid; place-items: center; font-size: 2rem; color: #fff; } .ic.ok { background: #2e7d32; } .ic.bad { background: #c62828; }
.amt { font-size: 1.6rem; font-weight: 700; }
@keyframes pop { from { opacity: 0; transform: translateY(8px) scale(.98); } }
@media (prefers-reduced-motion: reduce) { .box { animation: none; } }
</style>
