<template>
  <div class="container page track-page">
    <Breadcrumb :items="[{ label: 'Tra cứu đơn hàng' }]" />
    <h1>Tra cứu đơn hàng</h1>
    <p class="muted">Nhập mã đơn (VD: TS2610…) để xem trạng thái — không cần đăng nhập.</p>
    <form class="row q" @submit.prevent="go"><input v-model.trim="code" class="input grow" placeholder="Mã đơn hàng" aria-label="Mã đơn hàng" autocapitalize="characters" /><button class="btn">Tra cứu</button></form>
    <div v-if="err" class="alert alert-error">{{ err }}</div>
    <div v-if="loading" class="spinner" />
    <div v-else-if="o" class="card res">
      <div class="row between"><div><h2>Đơn {{ o.code }}</h2><span class="muted small">Đặt {{ dt(o.createdAt) }} · {{ o.buyer }} · {{ o.city }}</span></div>
        <span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span></div>
      <div class="road" aria-hidden="true"><div class="fill" :style="{ width: pct + '%' }" /><span class="car" :style="{ left: pct + '%' }">🚚</span></div>
      <ol class="steps">
        <li v-for="s in steps" :key="s.k" :class="{ done: o.timeline[s.k], now: cur === s.k }"><span class="dot" /><b>{{ s.l }}</b><small>{{ o.timeline[s.k] ? dt(o.timeline[s.k]) : '' }}</small></li>
      </ol>
      <div v-if="o.etaFrom && !['COMPLETED', 'CANCELLED'].includes(o.status)" class="eta">📦 Dự kiến giao <b>{{ dd(o.etaFrom) }}{{ o.etaTo !== o.etaFrom ? ' – ' + dd(o.etaTo) : '' }}</b> · {{ o.shippingMethod === 'EXPRESS' ? 'Hoả tốc' : 'Tiêu chuẩn' }}</div>
      <div class="items"><div v-for="(i, n) in o.items" :key="n" class="it"><img v-if="i.image" :src="i.image" alt="" /><span>{{ i.name }} × {{ i.qty }}</span></div></div>
      <p class="small muted">Cần hỗ trợ? <router-link to="/support">Trung tâm trợ giúp</router-link> · Hotline 1900 6868</p>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { dt, ORDER_STATUS } from '../format'
import Breadcrumb from '../components/Breadcrumb.vue'
const route = useRoute(), router = useRouter()
const code = ref(route.params.code || ''), o = ref(null), err = ref(''), loading = ref(false)
const steps = [{ k: 'PENDING', l: 'Đã đặt hàng' }, { k: 'CONFIRMED', l: 'Đã xác nhận' }, { k: 'SHIPPING', l: 'Đang giao' }, { k: 'COMPLETED', l: 'Đã nhận hàng' }]
const cur = computed(() => [...steps].reverse().find(s => o.value?.timeline[s.k])?.k)
const pct = computed(() => o.value?.status === 'CANCELLED' ? 0 : Math.max(0, steps.findIndex(s => s.k === cur.value)) * 100 / 3)
const dd = s => new Date(s + 'T00:00:00').toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit' })
const go = () => { if (code.value) router.replace('/track/' + code.value.toUpperCase()) }
async function load(c) {
  o.value = null; err.value = ''; if (!c) return; loading.value = true
  try { o.value = (await api.get('/public/track/' + encodeURIComponent(c))).data } catch (e) { err.value = errMsg(e) } finally { loading.value = false }
}
watch(() => route.params.code, c => { code.value = c || ''; load(c) }, { immediate: true })
</script>
<style scoped>
.track-page { max-width: 820px; } .q { gap: 8px; margin: 16px 0; flex-wrap: nowrap; } .q .input { min-width: 0; }
.res { display: flex; flex-direction: column; gap: 18px; } .res h2 { font-size: clamp(1.1rem, 1rem + 1vw, 1.5rem); }
.road { position: relative; height: 10px; background: repeating-linear-gradient(90deg, #333 0 18px, #555 18px 30px); margin: 18px 18px 0; }
.fill { position: absolute; inset: 0 auto 0 0; background: var(--accent); transition: width .5s ease-out; }
.car { position: absolute; top: -22px; transform: translateX(-50%) scaleX(-1); font-size: 1.3rem; transition: left .5s ease-out; }
.steps { list-style: none; display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.steps li { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 2px; color: var(--muted); font-size: .85rem; }
.steps .dot { width: 14px; height: 14px; border-radius: 50%; border: 2px solid var(--border); background: var(--surface); }
.steps li.done { color: var(--text); } .steps li.done .dot { background: var(--accent); border-color: var(--accent); } .steps li.now b { color: var(--accent); }
.steps small { font-size: .7rem; color: var(--muted); }
.eta { background: var(--bg-2); padding: 10px 12px; }
.items { display: flex; flex-direction: column; gap: 8px; } .it { display: flex; gap: 10px; align-items: center; } .it img { width: 48px; height: 36px; object-fit: cover; }
@media (max-width: 520px) { .steps { grid-template-columns: 1fr 1fr; row-gap: 14px; } }
@media (prefers-reduced-motion: reduce) { .fill, .car { transition: none; } }
</style>
