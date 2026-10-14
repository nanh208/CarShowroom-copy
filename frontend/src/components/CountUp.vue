<template><span ref="el">{{ shown }}</span></template>
<script setup>
// Số đếm tăng dần khi xuất hiện trên màn hình
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { reduced } from '../motion'
const props = defineProps({ value: { type: Number, default: 0 }, format: Function, duration: { type: Number, default: 600 } })
const el = ref(null), cur = ref(0); let io, raf
const shown = computed(() => props.format ? props.format(Math.round(cur.value)) : Math.round(cur.value).toLocaleString('vi-VN'))
const run = () => {
  if (reduced()) { cur.value = props.value; return }
  const from = cur.value, to = props.value, t0 = performance.now()
  cancelAnimationFrame(raf)
  const step = t => { const p = Math.min(1, (t - t0) / props.duration); cur.value = from + (to - from) * (1 - Math.pow(1 - p, 3)); if (p < 1) raf = requestAnimationFrame(step) }
  raf = requestAnimationFrame(step)
}
onMounted(() => { io = new IntersectionObserver(([e]) => { if (e.isIntersecting) { run(); io.disconnect() } }); io.observe(el.value) })
watch(() => props.value, run)
onUnmounted(() => { io?.disconnect(); cancelAnimationFrame(raf) })
</script>
