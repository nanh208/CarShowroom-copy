<template>
  <div v-if="state !== 'idle'" class="ol" role="dialog" aria-modal="true" aria-labelledby="ol-t" @click.self="$emit('close')">
    <div class="ol-card">
      <button type="button" class="ol-close" aria-label="Đóng" @click="$emit('close')">&times;</button>
      <template v-if="state !== 'ok' || !showDone">
        <h2 id="ol-t" class="ol-h">{{ state === 'err' ? 'Chưa thể đặt hàng' : 'Đang đặt hàng…' }}</h2>
        <div class="road" :class="{ err: state === 'err' }">
          <div class="asphalt"><i class="lane" /></div>
          <div class="done" :style="{ transform: `scaleX(${p})` }" />
          <ol class="steps">
            <li v-for="(s, i) in STEPS" :key="s" :class="{ on: p >= i / 2 - 0.001, cur: Math.round(p * 2) === i }" :style="{ left: i * 50 + '%' }"><i />{{ s }}</li>
          </ol>
          <svg class="car" :class="{ shake: state === 'err' }" :style="{ left: `calc(${p * 100}% - ${p * 46}px)` }" viewBox="0 0 46 24" aria-hidden="true">
            <path d="M3 17h40l-2.6-6a3 3 0 0 0-2.7-1.8H31l-5-5.2H14.5L9.7 9.2H6a3 3 0 0 0-3 3z" fill="#f05a1a" /><path d="M15.5 6h8.8l3.7 3.4H12.3z" fill="#cfe8ff" />
            <circle cx="12" cy="18" r="4" class="w" /><circle cx="34" cy="18" r="4" class="w" /><rect x="41" y="12" width="3" height="2" fill="#ffe08a" />
          </svg>
        </div>
        <p v-if="state === 'run'" class="ol-sub"><Speedometer v-if="slow" label="Đang xác nhận đơn…" /><span v-else>Kiểm tra tồn kho, áp dụng ưu đãi…</span></p>
        <div v-else-if="state === 'err'" class="ol-err">
          <div class="err-box-alert">
            <div class="err-header">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#ef4444" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <b>ĐẶT HÀNG THẤT BẠI</b>
            </div>
            <div class="err-desc">
              <span>Chi tiết nguyên nhân lỗi:</span>
              <p class="err-text-bold">{{ error || 'Hệ thống không thể xử lý đơn hàng lúc này. Vui lòng kiểm tra lại.' }}</p>
            </div>
          </div>
          <button class="btn btn-outline" @click="$emit('close')">Quay lại kiểm tra & sửa giỏ hàng</button>
        </div>
      </template>

      <div v-else ref="win" class="win">
        <div class="flag" aria-hidden="true" />
        <h2 id="ol-t">Đặt hàng thành công!</h2>
        <p class="code">Mã đơn <b>{{ prefix }}<span class="digits">{{ rolled }}</span></b></p>
        <p class="muted">Tổng thanh toán <b class="price"><CountUp :value="order.total" :format="vnd" :duration="700" /></b></p>
        <template v-if="payNext">
          <div class="pay-qr-hint">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
            <span>Mã QR thanh toán đã sẵn sàng</span>
          </div>
          <button class="btn btn-block btn-qr-action" @click="$emit('pay')">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            <span>Mở mã QR quét thanh toán ngay</span>
          </button>
        </template>
        <div v-else class="acts">
          <button class="btn" @click="$emit('view')">Xem đơn hàng</button>
          <button class="btn btn-outline" @click="$emit('shop')">Tiếp tục mua sắm</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, watch, computed, nextTick, onUnmounted } from 'vue'
import CountUp from './CountUp.vue'
import Speedometer from './Speedometer.vue'
import { reduced, checkeredConfetti } from '../motion'
import { vnd } from '../format'
const props = defineProps({ state: { type: String, default: 'idle' }, order: Object, error: String, payNext: Boolean })
defineEmits(['close', 'view', 'shop', 'pay'])
const STEPS = ['Giỏ hàng', 'Thanh toán', 'Xác nhận']
const p = ref(0), showDone = ref(false), slow = ref(false), win = ref(), rolled = ref('')
let raf, slowT
const tween = (to, ms) => new Promise(res => {
  if (reduced()) { p.value = to; return res() }
  const from = p.value, t0 = performance.now(); cancelAnimationFrame(raf)
  const step = t => { const k = Math.min(1, (t - t0) / ms); p.value = from + (to - from) * (1 - Math.pow(1 - k, 2)); k < 1 ? (raf = requestAnimationFrame(step)) : res() }
  raf = requestAnimationFrame(step)
})
const prefix = computed(() => (props.order?.code || '').replace(/\d.*$/, ''))
function roll() {
  const target = (props.order?.code || '').slice(prefix.value.length)
  if (reduced()) { rolled.value = target; return }
  const t0 = performance.now(), D = 700
  const step = t => { const k = Math.min(1, (t - t0) / D)
    rolled.value = [...target].map((d, i) => (k * target.length > i + 1 ? d : String(Math.floor(Math.random() * 10)))).join('')
    if (k < 1) raf = requestAnimationFrame(step); else rolled.value = target }
  raf = requestAnimationFrame(step)
}
watch(() => props.state, async s => {
  clearTimeout(slowT)
  if (s === 'run') { p.value = 0; showDone.value = false; slow.value = false; await tween(0.5, 520); slowT = setTimeout(() => (slow.value = true), 600) }
  else if (s === 'ok') { await tween(1, 380); showDone.value = true; await nextTick(); roll(); checkeredConfetti(win.value) }
}, { immediate: true })
onUnmounted(() => { cancelAnimationFrame(raf); clearTimeout(slowT) })
</script>
<style scoped>
.ol { position: fixed; inset: 0; z-index: 300; display: grid; place-items: center; padding: 16px; background: rgba(17,17,17,.6); animation: ol-in .18s ease-out; }
.ol-card { position: relative; }
.ol-close { position: absolute; top: 12px; right: 14px; background: none; border: none; font-size: 1.6rem; line-height: 1; cursor: pointer; color: var(--muted); z-index: 10; }
.ol-close:hover { color: var(--text); }
.ol-card-base { width: min(560px, 100%); background: var(--surface); color: var(--text); padding: 28px 28px 24px; border-top: 6px solid var(--accent); box-shadow: 0 30px 80px rgba(0,0,0,.4); text-align: center; }
.ol-h { font-size: clamp(1.3rem, 1rem + 1.5vw, 1.8rem); margin-bottom: 28px; }
.road { position: relative; height: 92px; margin: 0 6px; }
.asphalt { position: absolute; left: 0; right: 0; top: 30px; height: 14px; background: #2b2b2e; }
.lane { position: absolute; left: 0; right: 0; top: 6px; height: 2px; background: repeating-linear-gradient(90deg, #f5c400 0 12px, transparent 12px 24px); }
.done { position: absolute; left: 0; right: 0; top: 30px; height: 4px; background: var(--accent); transform-origin: left; }
.road.err .done { background: var(--danger); }
.steps { list-style: none; position: absolute; left: 0; right: 0; top: 52px; height: 30px; }
.steps li { position: absolute; transform: translateX(-50%); font: 600 .78rem/1.2 'Oswald', 'Be Vietnam Pro', sans-serif; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); white-space: nowrap; }
.steps li:first-child { transform: none; } .steps li:last-child { transform: translateX(-100%); }
.steps li i { position: absolute; top: -26px; left: 50%; width: 12px; height: 12px; margin-left: -6px; background: #fff; border: 3px solid #555; border-radius: 50%; }
.steps li:first-child i { left: 6px; } .steps li:last-child i { left: calc(100% - 6px); }
.steps li.on { color: var(--text); } .steps li.on i { border-color: var(--accent); background: var(--accent); } .steps li.cur { color: var(--accent); }
.car { position: absolute; top: 6px; width: 46px; height: 24px; filter: drop-shadow(0 3px 2px rgba(0,0,0,.35)); }
.w { fill: #111; stroke: #999; stroke-width: 1.2; stroke-dasharray: 3 2; animation: spin .35s linear infinite; transform-box: fill-box; transform-origin: center; }
.shake { animation: shake .3s ease-out 2; } .shake .w { animation: none; }
.ol-sub { margin-top: 10px; color: var(--muted); min-height: 96px; display: grid; place-items: center; }

.ol-err { margin-top: 16px; display: flex; flex-direction: column; align-items: center; gap: 14px; }
.err-box-alert { width: 100%; background: #fef2f2; border: 1.5px solid #fecaca; border-radius: 8px; padding: 14px 16px; text-align: left; }
.err-header { display: flex; align-items: center; gap: 8px; color: #b91c1c; font-size: 0.95rem; margin-bottom: 6px; }
.err-desc span { font-size: 0.8rem; color: #64748b; }
.err-text-bold { margin: 4px 0 0; color: #dc2626; font-weight: 600; font-size: 0.94rem; }

.win { position: relative; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.flag { width: 84px; height: 56px; background: conic-gradient(#111 25%, #fff 0 50%, #111 0 75%, #fff 0) 0 0/14px 14px; border: 2px solid #111; transform: skewX(-12deg); animation: wave 1.2s ease-in-out 2; }
.win h2 { font-size: clamp(1.5rem, 1.1rem + 2vw, 2.2rem); }
.code { font-size: 1rem; } .code b { font: 700 1.5rem/1.2 'Oswald', 'Be Vietnam Pro', sans-serif; letter-spacing: .04em; color: var(--accent); }
.digits { font-variant-numeric: tabular-nums; }

.pay-qr-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 999px;
  margin: 6px 0;
}

.btn-qr-action {
  background: #f05a1a;
  color: #fff;
  padding: 12px 20px;
  font-size: 1rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
}
.btn-qr-action:hover { background: #d94a0e; }

.acts { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; margin-top: 8px; }
@keyframes ol-in { from { opacity: 0; } }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes shake { 25% { transform: translateX(-4px); } 75% { transform: translateX(4px); } }
</style>
