<template>
  <div class="lt">
    <input v-model="p.q.value" class="input s" type="search" :placeholder="placeholder" :aria-label="placeholder" />
    <slot />
    <select v-if="Object.keys(p.sorts).length" v-model="p.sort.value" class="input so" aria-label="Sắp xếp"><option v-for="(v, k) in p.sorts" :key="k" :value="k">{{ v.label }}</option></select>
    <span class="muted small c">{{ p.total.value }} mục</span>
  </div>
</template>
<script setup>defineProps({ p: Object, placeholder: { type: String, default: 'Tìm…' } })</script>
<style scoped>
.lt { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 12px; } .s { flex: 1 1 200px; max-width: 320px; } .so, .lt :slotted(select) { width: auto; } .c { margin-left: auto; }
</style>
