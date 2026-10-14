<template>
  <nav class="crumb" aria-label="breadcrumb">
    <router-link to="/">Trang chủ</router-link>
    <template v-for="(c, i) in items" :key="i"><span class="sep">/</span>
      <router-link v-if="c.to" :to="c.to">{{ c.label }}</router-link><span v-else class="cur">{{ c.label }}</span></template>
  </nav>
</template>
<script setup>defineProps({ items: { type: Array, default: () => [] } })</script>
<style scoped>
.crumb { display: flex; flex-wrap: wrap; gap: 8px; color: var(--muted); font-size: .9rem; margin-bottom: 24px; }
.crumb a { color: var(--muted); } .crumb a:hover { color: var(--text); } .sep { opacity: .5; } .cur { color: var(--text-2); }
</style>
