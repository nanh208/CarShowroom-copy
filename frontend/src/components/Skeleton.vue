<template>
  <div :class="['grid', `grid-${cols}`]" aria-busy="true">
    <div v-for="i in count" :key="i" class="sk-card"><div class="sk sk-img" /><div class="sk sk-line" /><div class="sk sk-line short" /><div class="sk sk-line half" /></div>
  </div>
</template>
<script setup>defineProps({ count: { type: Number, default: 8 }, cols: { type: Number, default: 4 } })</script>
<style scoped>
.sk-card { display: flex; flex-direction: column; gap: 8px; }
.sk-img { aspect-ratio: 4/3; border-radius: 10px; } .sk-line { height: 12px; border-radius: 6px; } .short { width: 60%; } .half { width: 40%; height: 18px; }
</style>
