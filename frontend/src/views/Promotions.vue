<template>
  <div class="container page">
    <h1>Chương trình ưu đãi</h1>
    <p class="muted">Nhập mã ở bước thanh toán để được giảm giá.</p>
    <div v-if="!list.length" class="empty card">Hiện chưa có chương trình ưu đãi nào.</div>
    <div class="grid promos">
      <div v-for="p in list" :key="p.id" class="promo" :style="{ background: `linear-gradient(160deg, ${p.bannerColor}40, rgba(24,26,31,.88) 55%)` }">
        <div class="pct">−{{ p.discountPercent }}%</div>
        <h3>{{ p.title }}</h3>
        <p>{{ p.description }}</p>
        <div class="small muted">
          <span v-if="p.minOrder">Đơn tối thiểu {{ vnd(p.minOrder) }} · </span><span v-if="p.maxDiscount">Giảm tối đa {{ vnd(p.maxDiscount) }} · </span>
          HSD: {{ d(p.endDate) }}
        </div>
        <div class="cdl">Còn <Countdown :to="p.endDate + 'T23:59:59'" /></div>
        <button class="code" @click="copy(p.code)">{{ copied === p.code ? 'Đã sao chép' : '' + p.code }}</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { vnd, d } from '../format'
import Countdown from '../components/Countdown.vue'
const list = ref([]), copied = ref('')
const copy = c => { navigator.clipboard?.writeText(c); copied.value = c }
onMounted(async () => { list.value = (await api.get('/promotions')).data })
</script>
<style scoped>
.promos { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); }
.promo { border-radius: var(--radius); padding: 32px; border: 1px solid var(--border); display: flex; flex-direction: column; gap: 8px; }
.pct { font-size: 2.6rem; font-weight: 800; }
.code { margin-top: 10px; align-self: flex-start; background: rgba(0,0,0,.35); color: #fff; border: 1px dashed rgba(255,255,255,.5); border-radius: 8px; padding: 8px 14px; font: 700 1rem inherit; font-family: inherit; cursor: pointer; }
.cdl { margin-top: 8px; font-size: .9rem; color: #fca5a5; }
</style>
