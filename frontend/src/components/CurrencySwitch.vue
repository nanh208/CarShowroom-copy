<template>
  <div class="cs" role="group" :aria-label="'Tiền tệ hiển thị'">
    <button v-for="c in ['VND', 'USD']" :key="c" type="button" :class="{ on: currency.code === c }" :aria-pressed="currency.code === c" :title="c === 'USD' ? `1 USD = ${rate} — thanh toán bằng VND` : 'Việt Nam đồng'" @click="setCurrency(c)">{{ c === 'VND' ? '₫' : '$' }}</button>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { currency, setCurrency, vndRaw } from '../format'
const rate = computed(() => vndRaw(currency.rates.USD))
</script>
<style scoped>
.cs { display: inline-flex; border: 1px solid var(--border); border-radius: 99px; overflow: hidden; flex: none; }
.cs button { background: none; border: 0; padding: 3px 9px; font: 700 .74rem/1.4 inherit; color: inherit; cursor: pointer; opacity: .65; }
.cs button.on { background: var(--text, #111); color: var(--bg, #fff); opacity: 1; }
</style>
