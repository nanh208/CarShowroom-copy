<template>
  <img v-if="src && !failed" :src="src" :alt="product.name" class="pimg" loading="lazy" @error="failed = true" />
  <div v-else class="pimg ph"><svg viewBox="0 0 64 40" width="64"><path d="M6 28h52l-4-10a4 4 0 0 0-4-3H20a4 4 0 0 0-3.6 2.3L12 22l-6 2z" fill="#cfcfcf"/><circle cx="18" cy="30" r="5" fill="#9a9a9a"/><circle cx="46" cy="30" r="5" fill="#9a9a9a"/></svg></div>
</template>
<script setup>
import { ref, computed } from 'vue'
const props = defineProps({ product: Object, index: { type: Number, default: 0 } })
const failed = ref(false)
const src = computed(() => props.product?.images?.[props.index])
</script>
<style scoped>.pimg { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; } .ph { display: flex; align-items: center; justify-content: center; background: var(--tile); }</style>
