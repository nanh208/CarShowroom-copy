<template>
  <div v-if="p.pages.value > 1" class="pg" role="navigation" aria-label="Phân trang">
    <button class="btn btn-sm btn-outline" :disabled="p.page.value === 0" @click="go(p.page.value - 1)">← Trước</button>
    <span class="small">Trang {{ p.page.value + 1 }}/{{ p.pages.value }}</span>
    <button class="btn btn-sm btn-outline" :disabled="p.page.value >= p.pages.value - 1" @click="go(p.page.value + 1)">Sau →</button>
  </div>
</template>
<script setup>
const props = defineProps({ p: Object })
const go = n => { props.p.page.value = n; window.scrollTo({ top: 0, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' }) }
</script>
<style scoped>.pg { display: flex; gap: 10px; align-items: center; justify-content: center; margin-top: 14px; }</style>
