<template>
  <button class="theme-switch" type="button" :aria-label="`Đổi giao diện (đang dùng ${theme === 'v2' ? '2.0' : 'Cổ điển'})`" :title="'Giao diện: ' + (theme === 'v2' ? '2.0' : 'Cổ điển') + ' — bấm để đổi'" @click="toggleTheme">
    <span :class="{ on: theme === 'v2' }">2.0</span><span :class="{ on: theme === 'v1' }">1.0</span>
  </button>
</template>
<script setup>
import { theme, toggleTheme } from '../stores/theme'
</script>
<style scoped>
.theme-switch { display: inline-flex; border: 1px solid currentColor; border-radius: 6px; overflow: hidden; background: transparent; color: inherit; cursor: pointer; padding: 0; font: 700 .7rem/1 inherit; font-family: inherit; height: 26px; }
.theme-switch span { padding: 0 7px; display: flex; align-items: center; opacity: .6; transition: background-color .15s ease-out; }
.theme-switch span.on { opacity: 1; background: var(--accent); color: #fff; }
</style>
