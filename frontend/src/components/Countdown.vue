<template><span class="cd">{{ text }}</span></template>
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
const props = defineProps({ to: String, offset: { type: Number, default: 0 } })
const now = ref(Date.now()); let t
const text = computed(() => {
  let s = Math.max(0, Math.floor((new Date(props.to).getTime() - now.value - props.offset) / 1000))
  if (!s) return '00:00:00'
  const d = Math.floor(s / 86400); s %= 86400
  const p = n => String(n).padStart(2, '0')
  return (d ? d + ' ngày ' : '') + `${p(Math.floor(s / 3600))}:${p(Math.floor(s % 3600 / 60))}:${p(s % 60)}`
})
onMounted(() => { t = setInterval(() => now.value = Date.now(), 1000) })
onUnmounted(() => clearInterval(t))
</script>
<style scoped>.cd { font-variant-numeric: tabular-nums; font-weight: 800; }</style>
