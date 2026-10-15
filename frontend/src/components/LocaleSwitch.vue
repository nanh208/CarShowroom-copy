<template>
  <div class="ls" role="group" :aria-label="$t('Ngôn ngữ')">
    <button v-for="l in ['vi', 'en']" :key="l" type="button" :class="{ on: locale === l }" :aria-pressed="locale === l" @click="setLocale(l)">{{ l.toUpperCase() }}</button>
  </div>
</template>
<script setup>import { locale, setLocale } from '../i18n'</script>
<style scoped>
.ls { display: inline-flex; border: 1px solid var(--border); border-radius: 99px; overflow: hidden; flex: none; }
.ls button { background: none; border: 0; padding: 3px 8px; font: 600 .72rem/1.4 inherit; color: inherit; cursor: pointer; opacity: .65; }
.ls button.on { background: var(--accent); color: #fff; opacity: 1; }
</style>
