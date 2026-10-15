<template>
  <div class="a11y" role="group" aria-label="Hiển thị và trợ năng">
    <div class="row-a">
      <span class="lb">Chế độ</span>
      <div class="seg" role="radiogroup" aria-label="Chế độ sáng tối">
        <button type="button" role="radio" :aria-checked="mode === 'light'" :class="{ on: mode === 'light' }" @click="mode = 'light'">☀ Sáng</button>
        <button type="button" role="radio" :aria-checked="mode === 'dark'" :class="{ on: mode === 'dark' }" @click="mode = 'dark'">☾ Tối</button>
      </div>
    </div>
    <div class="row-a">
      <span class="lb">Cỡ chữ</span>
      <div class="seg" role="radiogroup" aria-label="Cỡ chữ">
        <button v-for="(s, i) in FONT_STEPS" :key="s" type="button" role="radio" :aria-checked="fontScale === s" :class="{ on: fontScale === s }" :style="{ fontSize: (0.78 + i * 0.1) + 'rem' }" :aria-label="`Cỡ chữ ${Math.round(s * 100)}%`" @click="fontScale = s">A</button>
      </div>
    </div>
    <label class="row-a sw"><span class="lb">Hiển thị gọn (compact)</span><input type="checkbox" role="switch" :checked="density === 'compact'" @change="density = $event.target.checked ? 'compact' : 'comfy'" /><i aria-hidden="true" /></label>
    <label class="row-a sw"><span class="lb">Tương phản cao</span><input v-model="contrast" type="checkbox" role="switch" /><i aria-hidden="true" /></label>
    <div v-if="showTheme" class="row-a"><span class="lb">Giao diện</span><ThemeSwitch /></div>
  </div>
</template>
<script setup>
import { mode, fontScale, contrast, density, FONT_STEPS } from '../stores/theme'
import ThemeSwitch from './ThemeSwitch.vue'
defineProps({ showTheme: { type: Boolean, default: false } })
</script>
<style scoped>
.a11y { display: flex; flex-direction: column; gap: 12px; min-width: 240px; }
.row-a { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.lb { font-size: .85rem; font-weight: 600; color: var(--text); }
.seg { display: inline-flex; border: 1px solid var(--border); }
.seg button { min-width: 34px; height: 32px; padding: 0 10px; border: none; background: var(--surface); color: var(--text); font: 600 .8rem/1 inherit; font-family: inherit; cursor: pointer; }
.seg button + button { border-left: 1px solid var(--border); }
.seg button.on { background: var(--accent); color: #fff; }
.sw { cursor: pointer; margin: 0; } .sw input { position: absolute; opacity: 0; width: 1px; height: 1px; }
.sw i { width: 40px; height: 22px; background: var(--tile-2); position: relative; transition: background-color .15s ease-out; flex-shrink: 0; }
.sw i::after { content: ''; position: absolute; top: 3px; left: 3px; width: 16px; height: 16px; background: #fff; transition: transform .15s ease-out; }
.sw input:checked + i { background: var(--accent); } .sw input:checked + i::after { transform: translateX(18px); }
.sw input:focus-visible + i { outline: 2px solid var(--accent); outline-offset: 2px; }
</style>
