<template>
  <div class="pw">
    <div class="row between top">
      <div><b>Lịch sử giá</b>
        <small v-if="h" class="muted"> · thấp nhất 30 ngày <span class="money">{{ vnd(h.min30) }}</span><em v-if="h.lowest" class="lo">Giá tốt nhất 30 ngày</em></small></div>
      <button class="btn btn-sm btn-outline" @click="open = !open">{{ open ? 'Đóng' : '🔔 Báo khi giảm giá' }}</button>
    </div>
    <svg v-if="h && h.points.length > 1" class="spark" viewBox="0 0 300 48" preserveAspectRatio="none" role="img" :aria-label="`Biểu đồ giá, thấp nhất ${vnd(h.min30)}`">
      <polyline :points="line" fill="none" stroke="var(--accent)" stroke-width="2" vector-effect="non-scaling-stroke" />
    </svg>
    <Transition name="fade">
      <form v-if="open" class="f" @submit.prevent="submit">
        <input v-if="!auth.loggedIn" v-model.trim="email" type="email" required class="input" placeholder="Email nhận thông báo" aria-label="Email" />
        <input v-model.number="target" type="number" min="1000" step="1000" class="input" :placeholder="`Giá mong muốn (tuỳ chọn, < ${vnd(price)})`" aria-label="Giá mong muốn" />
        <button class="btn btn-sm" :disabled="busy">{{ busy ? 'Đang lưu…' : 'Theo dõi giá' }}</button>
        <small v-if="h?.watchers" class="muted">{{ h.watchers }} người đang theo dõi giá mẫu này</small>
      </form>
    </Transition>
  </div>
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import api, { errMsg } from '../api'
import { vnd } from '../format'
import { toast } from '../stores/prefs'
import { useAuth } from '../stores/auth'
const props = defineProps({ productId: Number, price: Number })
const auth = useAuth(), h = ref(null), open = ref(false), email = ref(''), target = ref(null), busy = ref(false)
const load = () => api.get(`/products/${props.productId}/price-history`).then(r => { h.value = r.data }).catch(() => {})
watch(() => props.productId, load, { immediate: true })
const line = computed(() => {
  const pts = [...h.value.points.map(p => p.price), h.value.current], max = Math.max(...pts), min = Math.min(...pts), span = max - min || 1
  return pts.map((v, i) => `${(i / (pts.length - 1)) * 300},${44 - ((v - min) / span) * 40}`).join(' ')
})
async function submit() {
  busy.value = true
  try { const { data } = await api.post('/public/price-alerts', { productId: props.productId, email: email.value || null, targetPrice: target.value || null }); toast(data.message); open.value = false; load() }
  catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
</script>
<style scoped>
.pw { border: 1px solid var(--border); padding: 10px 12px; margin: 12px 0; display: flex; flex-direction: column; gap: 8px; }
.top { gap: 8px; flex-wrap: wrap; } .lo { font-style: normal; margin-left: 6px; color: var(--success, #2e7d32); font-weight: 600; }
.spark { width: 100%; height: 48px; } .f { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; } .f .input { flex: 1 1 180px; min-width: 0; }
</style>
