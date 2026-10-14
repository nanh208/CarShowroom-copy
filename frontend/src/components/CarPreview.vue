<template>
  <div class="car-preview" :style="{ background: bg }">
    <svg v-if="!accessory" viewBox="0 0 400 180" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient :id="gid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" :stop-color="light" />
          <stop offset=".55" :stop-color="body" />
          <stop offset="1" :stop-color="dark" />
        </linearGradient>
      </defs>
      <ellipse cx="200" cy="158" rx="170" ry="9" fill="rgba(0,0,0,.45)" />
      <path v-if="tall" :d="suvBody" :fill="`url(#${gid})`" stroke="rgba(0,0,0,.35)" stroke-width="1.5" />
      <path v-else :d="sportBody" :fill="`url(#${gid})`" stroke="rgba(0,0,0,.35)" stroke-width="1.5" />
      <path :d="tall ? suvGlass : sportGlass" fill="#0f172a" opacity=".88" />
      <path :d="tall ? suvSeat : sportSeat" :fill="interior" opacity=".95" />
      <rect :x="tall ? 330 : 345" :y="tall ? 92 : 104" width="14" height="6" rx="3" fill="#fde68a" />
      <rect :x="tall ? 52 : 40" :y="tall ? 92 : 104" width="12" height="6" rx="3" fill="#ef4444" />
      <g v-for="cx in [tall ? 112 : 108, tall ? 292 : 300]" :key="cx">
        <circle :cx="cx" cy="134" r="27" fill="#0b0b0b" />
        <circle :cx="cx" cy="134" r="17" :fill="wheel" stroke="rgba(0,0,0,.4)" stroke-width="2" />
        <g :stroke="wheelDark" stroke-width="3">
          <line v-for="a in [0, 72, 144, 216, 288]" :key="a" :x1="cx" y1="134"
                :x2="cx + 15 * Math.cos(a * Math.PI / 180)" :y2="134 + 15 * Math.sin(a * Math.PI / 180)" />
        </g>
        <circle :cx="cx" cy="134" r="4" fill="#111" />
      </g>
    </svg>
    <div v-else class="acc">📦</div>
    <span v-if="scale" class="scale-tag">{{ scale }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  body: { type: String, default: '#c1121f' }, wheel: { type: String, default: '#cbd5e1' },
  interior: { type: String, default: '#0f172a' }, type: String, scale: String, height: { type: String, default: '100%' }
})
const gid = 'g' + Math.random().toString(36).slice(2, 8)
const tall = computed(() => ['SUV', 'TRUCK'].includes(props.type))
const accessory = computed(() => props.type === 'ACCESSORY')
const shade = (hex, f) => {
  const h = (hex || '#888888').replace('#', ''); const n = parseInt(h.length === 3 ? h.split('').map(c => c + c).join('') : h, 16)
  const ch = s => Math.max(0, Math.min(255, Math.round(((n >> s) & 255) * f)))
  return `rgb(${ch(16)},${ch(8)},${ch(0)})`
}
const light = computed(() => shade(props.body, 1.35))
const dark = computed(() => shade(props.body, 0.55))
const wheelDark = computed(() => shade(props.wheel, 0.6))
const bg = computed(() => `radial-gradient(ellipse at 50% 35%, ${shade(props.body, 0.45)}55, transparent 70%), linear-gradient(180deg,#1b1e26,#0e1015)`)
const sportBody = 'M30 128 C30 112 38 104 60 100 L130 92 C160 70 190 58 235 60 C268 62 292 78 318 92 L352 100 C368 104 372 116 370 128 L338 130 A32 32 0 0 0 270 130 L138 130 A32 32 0 0 0 78 130 Z'
const sportGlass = 'M150 92 C175 72 200 66 232 67 C256 68 276 80 296 93 Z'
const sportSeat = 'M200 92 L206 76 L216 76 L214 92 Z'
const suvBody = 'M40 130 L40 98 C40 90 46 86 56 85 L96 82 L130 52 C138 46 146 44 160 44 L282 44 C296 44 304 50 312 60 L334 86 L350 90 C360 93 362 104 362 130 L322 130 A30 30 0 0 0 262 130 L142 130 A30 30 0 0 0 82 130 Z'
const suvGlass = 'M112 82 L142 56 L282 56 L306 82 Z'
const suvSeat = 'M196 82 L200 64 L212 64 L210 82 Z'
</script>

<style scoped>
.car-preview { position: relative; width: 100%; aspect-ratio: 16 / 10; display: flex; align-items: center; justify-content: center; border-radius: 12px; overflow: hidden; }
svg { width: 92%; height: auto; filter: drop-shadow(0 10px 18px rgba(0,0,0,.45)); transition: .3s; }
.acc { font-size: 4.5rem; }
.scale-tag { position: absolute; right: 10px; bottom: 8px; font-size: .75rem; font-weight: 700; color: #cbd5e1; background: rgba(0,0,0,.45); padding: 2px 8px; border-radius: 6px; }
</style>
