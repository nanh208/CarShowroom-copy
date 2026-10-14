<template>
  <span class="stars" :title="`${value || 0}/5`">
    <span v-for="i in 5" :key="i" :class="['s', { on: i <= Math.round(value || 0), pick: pick }]" :style="{ fontSize: size }"
      @click="pick && $emit('update:value', i)">★</span>
  </span>
</template>
<script setup>defineProps({ value: Number, size: { type: String, default: '1rem' }, pick: Boolean }); defineEmits(['update:value'])</script>
<style scoped>
.stars { display: inline-flex; gap: 2px; line-height: 1; } .s { color: rgba(255,255,255,.18); } .s.on { color: #fbbf24; }
.s.pick { cursor: pointer; transition: transform .1s; } .s.pick:hover { transform: scale(1.2); }
</style>
