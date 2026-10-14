<template>
  <div class="container page">
    <div v-if="!a" class="spinner" />
    <template v-else>
      <div class="muted small" style="margin-bottom:16px"><router-link to="/auctions">Đấu giá</router-link> / {{ a.title }}</div>
      <div class="grid grid-2 top">
        <div>
          <div class="hero-img"><img :src="a.images?.[img] || '/images/bg-pagani.jpg'" class="photo" /></div>
          <div v-if="a.images?.length > 1" class="row" style="margin-top:10px">
            <img v-for="(u, i) in a.images" :key="i" :src="u" :class="['th', { on: i === img }]" @click="img = i" /></div>
          <div class="card" style="margin-top:18px">
            <h3>Thông tin xe</h3>
            <p class="muted" style="margin-bottom:12px">{{ a.description }}</p>
            <table class="table"><tbody>
              <tr><th>Hãng</th><td>{{ a.brandName }}</td></tr><tr><th>Năm</th><td>{{ a.year }}</td></tr><tr><th>Odo</th><td>{{ a.mileage }}</td></tr>
              <tr v-for="(v, k) in a.specs" :key="k"><th>{{ k }}</th><td>{{ v }}</td></tr>
            </tbody></table>
          </div>
        </div>
        <div>
          <span :class="['badge', 'st-' + a.status]">{{ AUCTION_STATUS[a.status] }}</span>
          <h1 class="title">{{ a.title }}</h1>
          <div class="card bidbox">
            <div class="row between">
              <div><div class="muted small">{{ a.bidCount ? 'Giá cao nhất' : 'Giá khởi điểm' }}</div><div class="price big">{{ vnd(a.currentPrice) }}</div></div>
              <div style="text-align:right"><div class="muted small">{{ a.status === 'SCHEDULED' ? 'Bắt đầu sau' : a.status === 'LIVE' ? 'Kết thúc sau' : 'Đã kết thúc' }}</div>
                <Countdown v-if="a.status !== 'ENDED' && a.status !== 'CANCELLED'" class="big-cd" :to="a.status === 'SCHEDULED' ? a.startTime : a.endTime" :offset="skew" />
                <div v-else>{{ dt(a.endTime) }}</div></div>
            </div>
            <div class="row between small muted" style="margin:10px 0">
              <span>Bước giá: <b>{{ vnd(a.minIncrement) }}</b></span><span>{{ a.bidCount }} lượt đặt</span>
            </div>
            <template v-if="a.status === 'LIVE'">
              <div v-if="leading" class="alert alert-success">Bạn đang dẫn đầu!</div>
              <template v-if="auth.loggedIn">
                <label>Giá đặt (tối thiểu {{ vnd(a.nextMinBid) }})</label>
                <div class="row" style="flex-wrap:nowrap">
                  <input v-model.number="amount" type="number" :step="a.minIncrement" :min="a.nextMinBid" class="input grow" />
                  <button class="btn" :disabled="busy" @click="bid">Đặt giá</button>
                </div>
                <div class="row" style="margin-top:8px">
                  <button v-for="k in [1, 2, 5]" :key="k" class="btn btn-sm btn-outline" @click="amount = a.nextMinBid + (k - 1) * a.minIncrement">+{{ ty(a.minIncrement * k) }}</button>
                </div>
              </template>
              <router-link v-else :to="{ path: '/login', query: { redirect: $route.fullPath } }" class="btn btn-block">Đăng nhập để đặt giá</router-link>
              <p class="small muted" style="margin-top:10px">Đặt giá trong 2 phút cuối sẽ gia hạn phiên thêm 2 phút. Giá đặt là cam kết mua.</p>
            </template>
            <div v-if="youWon" class="alert alert-success" style="margin-top:12px">Chúc mừng! Bạn đã thắng phiên đấu giá.
              <router-link :to="`/orders/${a.orderCode}`" class="btn btn-sm" style="margin-left:8px">Đặt cọc 10% ngay</router-link></div>
            <div v-else-if="a.status === 'ENDED'" class="alert alert-info" style="margin-top:12px">Phiên đã kết thúc{{ a.winner ? ' — người thắng: ' + mask(a.winner.fullName) : ' — không có lượt đặt giá' }}.</div>
            <div v-if="err" class="alert alert-error" style="margin-top:12px">{{ err }}</div>
            <div v-if="ok" class="alert alert-success" style="margin-top:12px">{{ ok }}</div>
          </div>
          <div class="card" style="margin-top:18px">
            <h3>Lịch sử đặt giá <span class="muted small">(cập nhật trực tiếp)</span></h3>
            <div v-if="!bids.length" class="muted">Chưa có lượt đặt giá.</div>
            <div v-for="(b, i) in bids" :key="b.id" :class="['bidrow', { top: i === 0 }]">
              <span>{{ i === 0 ? '' : '#' + (i + 1) }} {{ b.bidderName }}</span><b>{{ vnd(b.amount) }}</b><span class="small muted">{{ dt(b.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import api, { errMsg } from '../api'
import { useAuth } from '../stores/auth'
import { vnd, dt, ty, AUCTION_STATUS } from '../format'
import Countdown from '../components/Countdown.vue'
const route = useRoute(), auth = useAuth()
const a = ref(null), bids = ref([]), leading = ref(false), youWon = ref(false), amount = ref(0), err = ref(''), ok = ref(''), busy = ref(false), img = ref(0), skew = ref(0)
let t
const mask = n => n ? n.split(' ').pop()[0] + '***' : 'Ẩn danh'
async function load() {
  const { data } = await api.get(`/auctions/${route.params.id}`)
  const first = !a.value
  a.value = data.auction; bids.value = data.bids; leading.value = data.leading; youWon.value = data.youWon
  skew.value = Date.now() - new Date(data.serverTime).getTime()
  if (first || amount.value < a.value.nextMinBid) amount.value = a.value.nextMinBid
}
async function bid() {
  err.value = ''; ok.value = ''; busy.value = true
  try { await api.post(`/auctions/${route.params.id}/bids`, { amount: amount.value }); ok.value = 'Đặt giá thành công!'; await load() }
  catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
onMounted(() => { load(); t = setInterval(load, 3000) })
onUnmounted(() => clearInterval(t))
</script>
<style scoped>
.top { align-items: start; }
.photo { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 12px; }
.th { width: 90px; aspect-ratio: 16/10; object-fit: cover; border-radius: 8px; border: 2px solid var(--border); cursor: pointer; } .th.on { border-color: var(--accent); }
.title { font-size: 2rem; margin: 10px 0 16px; }
.big { font-size: 1.8rem; } .big-cd { font-size: 1.6rem; color: var(--danger); }
.st-LIVE { background: var(--danger); color: var(--text); } .st-SCHEDULED { background: var(--primary); color: var(--text); }
.bidrow { display: grid; grid-template-columns: 1fr auto; gap: 2px 10px; padding: 10px 0; border-bottom: 1px solid var(--border); }
.bidrow .small { grid-column: 1 / -1; } .bidrow.top b { color: var(--accent); }
.table th { width: 40%; text-transform: none; font-size: .88rem; }
.hero-img { border-radius: 20px; overflow: hidden; border: 1px solid var(--border); aspect-ratio: 16/10; } .hero-img .photo { width: 100%; height: 100%; object-fit: cover; border-radius: 0; }
</style>
