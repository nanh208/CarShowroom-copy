<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Ưu đãi' }]" />
    <h1>Ưu đãi & Flash sale</h1>
    <section v-if="flash.length" class="sec">
      <div class="fh"><h2>⚡ Flash sale</h2><span class="muted small">Số lượng có hạn — giá áp dụng tự động khi thêm vào giỏ</span></div>
      <div class="grid fgrid">
        <router-link v-for="f in flash" :key="f.flash.id" v-reveal :to="`/products/${f.product.id}`" class="fcard card">
          <img :src="f.product.images?.[0]" alt="" />
          <div class="fb">
            <b>{{ f.product.name }}</b>
            <div><span class="price">{{ vnd(f.flash.salePrice) }}</span> <s class="muted small">{{ vnd(f.product.price) }}</s></div>
            <div class="bar"><i :style="{ width: Math.min(100, f.flash.sold / (f.flash.quota || 1) * 100) + '%' }" /></div>
            <div class="small" :class="f.flash.live ? 'red' : 'muted'">
              <template v-if="f.flash.live">Đã bán {{ f.flash.sold }}/{{ f.flash.quota }} · còn <Countdown :to="f.flash.endTime" /></template>
              <template v-else>Bắt đầu sau <Countdown :to="f.flash.startTime" /></template>
            </div>
          </div>
        </router-link>
      </div>
    </section>
    <section class="sec">
      <div class="fh"><h2>Mã giảm giá</h2><span class="muted small">Nhập mã ở bước thanh toán · miễn phí ship cho đơn từ {{ vnd(catalog.options.freeShipFrom) }}</span></div>
      <div v-if="!list.length" class="empty card">Hiện chưa có mã giảm giá nào.</div>
      <div class="grid promos">
        <div v-for="p in list" :key="p.id" v-reveal class="promo" :style="{ background: `linear-gradient(160deg, ${p.bannerColor || '#a87a3c'}30, #fff 60%)` }">
          <span class="tag">{{ VOUCHER_KIND[p.kind] || p.kind }}</span>
          <div class="pct">{{ p.kind === 'FIXED' ? '−' + k(p.amount) : p.kind === 'FREESHIP' ? 'Freeship' : '−' + p.discountPercent + '%' }}</div>
          <h3>{{ p.title }}</h3>
          <p>{{ p.description }}</p>
          <div class="small muted">
            <span v-if="p.minOrder">Đơn tối thiểu {{ vnd(p.minOrder) }} · </span><span v-if="p.maxDiscount">Giảm tối đa {{ vnd(p.maxDiscount) }} · </span>
            <span v-if="p.usageLimit">Còn {{ p.usageLimit - p.usedCount }} lượt · </span>HSD: {{ d(p.endDate) }}
          </div>
          <button class="code" @click="copy(p.code)">{{ copied === p.code ? '✓ Đã sao chép' : p.code }}</button>
        </div>
      </div>
    </section>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { vnd, d, k, VOUCHER_KIND, catalog } from '../format'
import Countdown from '../components/Countdown.vue'
import Breadcrumb from '../components/Breadcrumb.vue'
const list = ref([]), flash = ref([]), copied = ref('')
const copy = c => { navigator.clipboard?.writeText(c); copied.value = c }
onMounted(async () => {
  const [a, b] = await Promise.all([api.get('/promotions'), api.get('/flash-sales')])
  list.value = a.data; flash.value = b.data
})
</script>
<style scoped>
.sec { margin-bottom: 40px; } .fh { display: flex; align-items: baseline; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; } .fh h2 { margin: 0; }
.fgrid { grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); }
.fcard { padding: 0; overflow: hidden; color: var(--text); transition: transform .25s, box-shadow .25s; } .fcard:hover { transform: translateY(-4px); box-shadow: 0 10px 24px rgba(120,80,30,.15); }
.fcard img { width: 100%; aspect-ratio: 4/3; object-fit: cover; } .fb { padding: 12px 14px; display: flex; flex-direction: column; gap: 6px; }
.bar { height: 6px; background: #f1e6d6; border-radius: 3px; overflow: hidden; } .bar i { display: block; height: 100%; background: linear-gradient(90deg, #e2583e, #f0a35b); }
.red { color: var(--danger); }
.promos { grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); }
.promo { position: relative; border-radius: var(--radius); padding: 28px; border: 1px solid var(--border); display: flex; flex-direction: column; gap: 8px; }
.tag { position: absolute; top: 14px; right: 14px; font-size: .72rem; padding: 3px 8px; border-radius: 99px; background: #fff; border: 1px solid var(--border); color: var(--bronze); }
.pct { font-size: 2.2rem; font-weight: 800; color: var(--bronze); }
.code { margin-top: 10px; align-self: flex-start; background: #fff; color: var(--text); border: 1px dashed var(--bronze); border-radius: 8px; padding: 8px 14px; font-weight: 700; font-family: inherit; cursor: pointer; letter-spacing: .05em; }
</style>
