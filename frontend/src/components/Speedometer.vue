<template>
  <div class="spd" role="status" :aria-label="label">
    <svg viewBox="0 0 120 74" aria-hidden="true">
      <path d="M12 66a48 48 0 0 1 96 0" class="trk" />
      <path d="M12 66a48 48 0 0 1 96 0" class="red" />
      <g v-for="i in 11" :key="i" :transform="`rotate(${-90 + (i - 1) * 18} 60 66)`"><line x1="60" y1="22" x2="60" :y2="i % 2 ? 29 : 26" /></g>
      <g class="ndl"><path d="M58.4 66 60 26l1.6 40z" /><circle cx="60" cy="66" r="5.5" /></g>
    </svg>
    <b v-if="label">{{ label }}</b>
  </div>
</template>
<script setup>
// Loader đồng hồ tốc độ: kim quét qua lại (dừng ở giữa khi reduced-motion)
defineProps({ label: { type: String, default: 'Đang xử lý thanh toán…' } })
</script>
<style scoped>
.spd { display: inline-flex; flex-direction: column; align-items: center; gap: 6px; }
svg { width: 120px; overflow: visible; }
.trk { fill: none; stroke: #2a2a2a; stroke-width: 7; } .red { fill: none; stroke: var(--accent, #f05a1a); stroke-width: 7; stroke-dasharray: 151; stroke-dashoffset: 120; transform-origin: 60px 66px; transform: rotate(0deg); }
line { stroke: #888; stroke-width: 1.6; }
.ndl { transform-origin: 60px 66px; animation: sweep 1.1s cubic-bezier(.45,0,.55,1) infinite alternate; } .ndl path { fill: var(--accent, #f05a1a); } .ndl circle { fill: #111; stroke: #fff; stroke-width: 1.5; }
b { font: 600 .9rem 'Oswald', 'Be Vietnam Pro', sans-serif; letter-spacing: .03em; }
@keyframes sweep { from { transform: rotate(-80deg); } 70% { transform: rotate(62deg); } to { transform: rotate(48deg); } }
@media (prefers-reduced-motion: reduce) { .ndl { animation: none; transform: rotate(0deg); } }
</style>
