<template>
  <div class="container page">
    <h1>Đấu giá xe hiếm</h1>
    <p class="muted">Những mẫu xe sưu tầm hiếm có, đã được ARES thẩm định. Đăng nhập để đặt giá — người trả giá cao nhất khi hết giờ sẽ nhận đơn đặt cọc 10%.</p>
    <div class="tabs">
      <button v-for="(v, k) in filters" :key="k" :class="{ active: filter === k }" @click="filter = k">{{ v }}</button>
    </div>
    <div v-if="loading" class="spinner" />
    <div v-else-if="!shown.length" class="empty card">Chưa có phiên đấu giá.</div>
    <div v-else class="grid agrid">
      <router-link v-for="a in shown" :key="a.id" :to="`/auctions/${a.id}`" class="acard">
        <div class="img"><img :src="a.images?.[0] || '/images/bg-pagani.jpg'" :alt="a.title" />
          <span :class="['badge', 'st-' + a.status]">{{ AUCTION_STATUS[a.status] }}</span></div>
        <div class="body">
          <b class="name">{{ a.title }}</b>
          <div class="muted small">{{ a.brandName }} · {{ a.mileage }}</div>
          <div class="row between">
            <div><div class="muted small">{{ a.bidCount ? 'Giá hiện tại' : 'Giá khởi điểm' }}</div><div class="price">{{ ty(a.currentPrice) }}</div></div>
            <div style="text-align:right"><div class="muted small">{{ a.status === 'SCHEDULED' ? 'Bắt đầu sau' : a.status === 'LIVE' ? 'Còn lại' : 'Kết thúc' }}</div>
              <Countdown v-if="a.status !== 'ENDED'" :to="a.status === 'SCHEDULED' ? a.startTime : a.endTime" /><span v-else>{{ dt(a.endTime) }}</span></div>
          </div>
          <div class="small muted">{{ a.bidCount }} lượt đặt giá</div>
        </div>
      </router-link>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../api'
import { dt, ty, AUCTION_STATUS } from '../format'
import Countdown from '../components/Countdown.vue'
const list = ref([]), loading = ref(true), filter = ref('ALL')
const filters = { ALL: 'Tất cả', LIVE: 'Đang đấu giá', SCHEDULED: 'Sắp diễn ra', ENDED: 'Đã kết thúc' }
const shown = computed(() => list.value.filter(a => filter.value === 'ALL' || a.status === filter.value))
let t
const load = async () => { list.value = (await api.get('/auctions')).data; loading.value = false }
onMounted(() => { load(); t = setInterval(load, 10000) })
onUnmounted(() => clearInterval(t))
</script>
<style scoped>
.acard { background: var(--card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; color: var(--text); transition: .25s; display: flex; flex-direction: column; }
.acard:hover { transform: translateY(-4px); border-color: rgba(168,122,60,.35); color: var(--text); }
.img { position: relative; } .img img { width: 100%; aspect-ratio: 16/10; object-fit: cover; }
.img .badge { position: absolute; top: 10px; left: 10px; }
.st-LIVE { background: var(--danger); color: var(--text); } .st-SCHEDULED { background: var(--primary); color: var(--text); } .st-ENDED { background: #475569; }
.body { padding: 14px 16px; display: flex; flex-direction: column; gap: 8px; }
.name { font-size: 1.02rem; }
.agrid { grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); }
.acard { border-radius: var(--radius) !important; transition: .3s; }
.acard:hover { transform: translateY(-4px); }
.acard .img { aspect-ratio: 16/10; overflow: hidden; } .acard .img img { width: 100%; height: 100%; object-fit: cover; transition: transform .6s; }
.acard:hover .img img { transform: scale(1.05); }
.acard .body { padding: 24px !important; gap: 12px !important; }
.acard .name { font-size: 1.2rem; }
.acard .price { font-size: 1.3rem; }
</style>
