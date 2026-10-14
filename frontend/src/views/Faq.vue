<template>
  <div class="container page narrow">
    <Breadcrumb :items="[{ label: 'Câu hỏi thường gặp' }]" />
    <h1>Câu hỏi thường gặp</h1>
    <p>Chưa tìm thấy câu trả lời? Gọi hotline {{ HOTLINE }} hoặc chat với chuyên viên ở góc phải màn hình.</p>
    <div class="cats"><button v-for="c in cats" :key="c" :class="{ on: cat === c }" @click="cat = c">{{ c }}</button></div>
    <div class="list">
      <details v-for="f in shown" :key="f[1]" class="qa">
        <summary>{{ f[1] }}<span class="plus" /></summary>
        <p>{{ f[2] }}</p>
      </details>
    </div>
  </div>
</template>
<script setup>
import { ref, computed } from 'vue'
import Breadcrumb from '../components/Breadcrumb.vue'
import { HOTLINE } from '../format'
import { FAQS } from '../faqs'
const faqs = FAQS
const cats = ['Tất cả', ...new Set(faqs.map(f => f[0]))]
const cat = ref('Tất cả')
const shown = computed(() => faqs.filter(f => cat.value === 'Tất cả' || f[0] === cat.value))
</script>
<style scoped>
.narrow { max-width: 880px; }
.cats { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 32px; }
.cats button { padding: 8px 18px; border-radius: 999px; border: 1px solid var(--border); background: none; color: var(--text-2); cursor: pointer; font: 500 .9rem inherit; font-family: inherit; }
.cats button.on { background: #fff; color: var(--bg-2); border-color: var(--text); }
.qa { border-bottom: 1px solid var(--border); }
.qa summary { list-style: none; cursor: pointer; padding: 24px 0; font-size: 1.1rem; font-weight: 600; display: flex; justify-content: space-between; gap: 16px; }
.qa summary::-webkit-details-marker { display: none; }
.plus { width: 20px; height: 20px; position: relative; flex-shrink: 0; } .plus::before, .plus::after { content: ''; position: absolute; background: var(--accent); left: 9px; top: 2px; width: 2px; height: 16px; transition: .2s; }
.plus::after { transform: rotate(90deg); } .qa[open] .plus::before { transform: rotate(90deg); }
.qa p { color: var(--text-2); padding: 0 0 24px; line-height: 1.7; max-width: 720px; }
</style>
