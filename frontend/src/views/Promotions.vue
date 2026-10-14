<template>
  <div class="container page">
    <h1 class="page-title">Chương trình ưu đãi</h1>
    <p class="page-sub">Nhập mã ở bước thanh toán để được giảm giá.</p>
    <div v-if="!list.length" class="empty card">Hiện chưa có chương trình ưu đãi nào.</div>
    <div class="grid promos">
      <div v-for="p in list" :key="p.id" class="promo" :style="{ background: `linear-gradient(135deg, ${p.bannerColor}, #0b0c10)` }">
        <div class="pct">-{{ p.discountPercent }}%</div>
        <h3>{{ p.title }}</h3>
        <p>{{ p.description }}</p>
        <div class="small muted">
          <span v-if="p.minOrder">Đơn tối thiểu {{ vnd(p.minOrder) }} · </span><span v-if="p.maxDiscount">Giảm tối đa {{ vnd(p.maxDiscount) }} · </span>
          HSD: {{ d(p.endDate) }}
        </div>
        <button class="code" @click="copy(p.code)">{{ copied === p.code ? '✔ Đã sao chép' : '📋 ' + p.code }}</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { vnd, d } from '../format'
const list = ref([]), copied = ref('')
const copy = c => { navigator.clipboard?.writeText(c); copied.value = c }
onMounted(async () => { list.value = (await api.get('/promotions')).data })
</script>
<style scoped>
.promos { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); }
.promo { border-radius: 2px; padding: 26px; border: 1px solid var(--line); display: flex; flex-direction: column; gap: 8px; color: #fff; }
.pct { font-size: 2.6rem; font-weight: 800; }
.code { margin-top: 10px; align-self: flex-start; background: rgba(0,0,0,.35); color: #fff; border: 1px dashed rgba(255,255,255,.5); border-radius: 2px; padding: 8px 14px; font: 700 1rem Montserrat,sans-serif; cursor: pointer; }
</style>
