<template>
  <div>
    <h1>Tiền tệ & tỷ giá</h1>
    <p class="muted small">Khách có thể xem giá bằng USD (nút ₫/$ trên thanh đầu trang). Chỉ để tham khảo — đơn hàng, cổng thanh toán và trang quản trị luôn dùng VND.</p>
    <div class="g2">
      <form class="card" @submit.prevent="save">
        <h3>Tỷ giá</h3>
        <div class="field"><label>1 USD = ? VND</label><input v-model.number="usd" type="number" min="1000" max="100000" step="10" class="input" required /></div>
        <p class="small muted">Cập nhật lần cuối: {{ cur.updatedAt ? dt(cur.updatedAt) : 'mặc định hệ thống' }}</p>
        <button class="btn" :disabled="busy">Lưu tỷ giá</button>
      </form>
      <div class="card">
        <h3>Xem trước</h3>
        <table class="table"><thead><tr><th>VND</th><th>USD</th></tr></thead>
          <tbody><tr v-for="v in [199000, 500000, 1250000, 3490000, 12900000]" :key="v"><td class="money">{{ vndRaw(v) }}</td><td class="money">${{ (v / (usd || 1)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</td></tr></tbody></table>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vndRaw, dt, loadCurrency } from '../../format'
import { toast } from '../../stores/prefs'
const cur = ref({}), usd = ref(25400), busy = ref(false)
async function load() { cur.value = (await api.get('/public/currency')).data; usd.value = cur.value.rates.USD }
async function save() { busy.value = true; try { cur.value = (await api.put('/admin/currency', { rates: { USD: usd.value } })).data; loadCurrency(); toast('Đã lưu tỷ giá') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
onMounted(load)
</script>
<style scoped>.g2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; } @media (max-width: 800px) { .g2 { grid-template-columns: 1fr; } }</style>
