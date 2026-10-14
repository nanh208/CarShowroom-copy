<template>
  <div class="gauge" :style="{ width: size + 'px' }">
    <svg :viewBox="`0 0 120 72`">
      <path d="M12 64a48 48 0 0 1 96 0" class="track" />
      <path d="M12 64a48 48 0 0 1 96 0" class="val" :style="{ strokeDashoffset: 151 - 151 * pct }" />
      <g v-for="i in 9" :key="i" :transform="`rotate(${-90 + (i - 1) * 22.5} 60 64)`"><line x1="60" y1="20" x2="60" :y2="i % 2 ? 26 : 24" class="tick" /></g>
      <g class="needle" :style="{ transform: `rotate(${-90 + 180 * pct}deg)` }"><path d="M58.5 64 60 22l1.5 42z" /><circle cx="60" cy="64" r="5" /></g>
    </svg>
    <div class="lbl"><b>{{ label }}</b><small>{{ sub }}</small></div>
  </div>
</template>
<script setup>
// Đồng hồ tốc độ: dùng cho thống kê và đếm ngược
import { computed } from 'vue'
const props = defineProps({ value: Number, max: { type: Number, default: 100 }, label: String, sub: String, size: { type: Number, default: 140 } })
const pct = computed(() => Math.max(0, Math.min(1, (props.value || 0) / (props.max || 1))))
</script>
<style scoped>
.gauge { position: relative; text-align: center; }
svg { width: 100%; overflow: visible; }
.track, .val { fill: none; stroke-width: 9; stroke-linecap: round; } .track { stroke: #ececec; }
.val { stroke: url(#g) var(--bronze); stroke: var(--bronze); stroke-dasharray: 151; transition: stroke-dashoffset 1.2s cubic-bezier(.3,1.3,.5,1); }
.tick { stroke: #bbb; stroke-width: 1.5; }
.needle { transform-origin: 60px 64px; transition: transform 1.2s cubic-bezier(.3,1.4,.5,1); fill: #c62828; } .needle circle { fill: #333; }
.lbl { margin-top: -6px; } .lbl b { display: block; font-family: var(--head); font-size: 1.1rem; } .lbl small { color: var(--muted); font-size: .72rem; }
@media (prefers-reduced-motion: reduce) { .val, .needle { transition: none; } }
</style>
