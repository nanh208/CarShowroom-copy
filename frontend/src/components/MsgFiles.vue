<template>
  <div v-if="files?.length" class="mf"><a v-for="u in files" :key="u" :href="u" target="_blank" rel="noopener"><img :src="u" alt="Ảnh đính kèm" loading="lazy" /></a></div>
</template>
<script setup>defineProps({ files: Array })</script>
<style scoped>.mf { display: flex; flex-wrap: wrap; gap: 6px; margin: 6px 0 2px; } .mf img { width: 84px; height: 84px; object-fit: cover; border: 1px solid var(--border); }</style>
