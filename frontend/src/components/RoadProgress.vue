<template>
  <div class="road" aria-hidden="true">
    <div class="lane" />
    <div class="done" :style="{ transform: `scaleX(${p})` }" />
    <svg class="car" :style="{ left: `calc(${p * 100}% - ${p * 26}px)` }" viewBox="0 0 26 14" width="26" height="14">
      <path d="M2 10h22l-1.6-3.6a2 2 0 0 0-1.8-1.2H17l-3-3.2H8.6L5.8 5.2H4a2 2 0 0 0-2 2z" fill="#a87a3c" />
      <path d="M9 3h4.6l2.2 2.2H7.2z" fill="#dff1ff" /><circle cx="7" cy="10.6" r="2.4" class="w" /><circle cx="19.5" cy="10.6" r="2.4" class="w" />
      <circle cx="24" cy="7.6" r="1" fill="#ffe08a" />
    </svg>
  </div>
</template>
<script setup>
// Thanh tiến trình cuộn dạng con đường, có xe chạy theo
import { ref, onMounted, onUnmounted } from 'vue'
const p = ref(0); let raf
const on = () => { cancelAnimationFrame(raf); raf = requestAnimationFrame(() => { const h = document.documentElement.scrollHeight - innerHeight; p.value = h > 0 ? Math.min(1, scrollY / h) : 0 }) }
onMounted(() => { addEventListener('scroll', on, { passive: true }); addEventListener('resize', on); on() })
onUnmounted(() => { removeEventListener('scroll', on); removeEventListener('resize', on) })
</script>
<style scoped>
.road { position: fixed; top: 0; left: 0; right: 0; height: 4px; z-index: 200; background: #2b2b2e; pointer-events: none; }
.lane { position: absolute; inset: 1.5px 0 auto; height: 1px; background: repeating-linear-gradient(90deg, #f5c400 0 10px, transparent 10px 20px); opacity: .7; }
.done { position: absolute; inset: 0; background: linear-gradient(90deg, #8f672f, #a87a3c, #f5c400); transform-origin: left; }
.car { position: absolute; top: -1px; filter: drop-shadow(0 1px 1px rgba(0,0,0,.35)); }
.w { fill: #222; stroke: #888; stroke-width: .8; stroke-dasharray: 2 1.2; }
</style>
