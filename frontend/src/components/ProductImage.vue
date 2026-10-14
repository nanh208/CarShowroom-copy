<template>
  <img v-if="src && !failed" :src="src" :alt="product.name" class="pimg" @error="failed = true" />
  <CarPreview v-else :body="product.baseColor" :type="product.type" :scale="product.year ? String(product.year) : ''" />
</template>
<script setup>
import { ref, computed } from 'vue'
import CarPreview from './CarPreview.vue'
const props = defineProps({ product: Object, index: { type: Number, default: 0 } })
const failed = ref(false)
const src = computed(() => props.product?.images?.[props.index])
</script>
<style scoped>.pimg { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; }</style>
