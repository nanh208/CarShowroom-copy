<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Yêu thích' }]" />
    <div class="row between whead"><h1>Danh sách yêu thích</h1>
      <div v-if="prefs.wish.length" class="row shr"><button class="btn btn-sm" @click="shareList">🔗 Chia sẻ danh sách</button><a v-if="shareUrl" class="btn btn-sm btn-outline" :href="zalo" target="_blank" rel="noopener">Gửi qua Zalo</a></div></div>
    <p v-if="shareUrl" class="shbox small"><input :value="shareUrl" readonly class="input" aria-label="Liên kết chia sẻ" @focus="$event.target.select()" /> Liên kết đã được sao chép — người nhận xem được mà không cần đăng nhập.</p>
    <p>{{ prefs.wish.length ? `${prefs.wish.length} mẫu xe bạn đã lưu. Bật thông báo để biết khi giảm giá hoặc có hàng lại.` : 'Bấm biểu tượng trái tim trên ảnh xe để lưu lại mẫu bạn thích.' }}</p>
    <div v-if="!prefs.wish.length" class="empty card">Chưa có mẫu xe yêu thích. <router-link to="/products">Khám phá mẫu xe →</router-link></div>
    <div v-else class="grid grid-w">
      <div v-for="p in prefs.wish" :key="p.id" class="wi">
        <ProductCard :p="p" />
        <div class="alerts" role="group" :aria-label="`Thông báo cho ${p.name}`">
          <button :class="['al', { on: has(p.id, 'PRICE') }]" :aria-pressed="has(p.id, 'PRICE')" @click="toggle(p, 'PRICE')">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 12 12 20l-8-8V4h8z"/><circle cx="8" cy="8" r="1.5"/></svg>{{ has(p.id, 'PRICE') ? 'Đang theo dõi giá' : 'Báo khi giảm giá' }}</button>
          <button v-if="p.stock <= 0" :class="['al', { on: has(p.id, 'STOCK') }]" :aria-pressed="has(p.id, 'STOCK')" @click="toggle(p, 'STOCK')">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"/></svg>{{ has(p.id, 'STOCK') ? 'Sẽ báo khi có hàng' : 'Báo khi có hàng' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Breadcrumb from '../components/Breadcrumb.vue'
import ProductCard from '../components/ProductCard.vue'
import api, { errMsg } from '../api'
import { usePrefs, toast } from '../stores/prefs'
import { useAuth } from '../stores/auth'
const prefs = usePrefs(), auth = useAuth(), router = useRouter(), alerts = ref([]), shareUrl = ref('')
const zalo = computed(() => 'https://zalo.me/share?url=' + encodeURIComponent(shareUrl.value))
async function shareList() {
  const q = new URLSearchParams({ ids: prefs.wish.map(p => p.id).join(',') }); if (auth.user?.fullName) q.set('name', auth.user.fullName.split(' ').pop())
  shareUrl.value = `${location.origin}/wishlist/shared?${q}`
  if (navigator.share) { try { await navigator.share({ title: 'Danh sách yêu thích ARES Toys', url: shareUrl.value }); return } catch {} }
  try { await navigator.clipboard.writeText(shareUrl.value); toast('Đã sao chép liên kết chia sẻ') } catch {}
}
const has = (id, k) => alerts.value.some(a => a.productId === id && a.kind === k)
async function toggle(p, kind) {
  if (!auth.loggedIn) return router.push({ path: '/login', query: { redirect: '/wishlist' } })
  try {
    const ex = alerts.value.find(a => a.productId === p.id && a.kind === kind)
    if (ex) { await api.delete(`/alerts/${ex.id}`); alerts.value = alerts.value.filter(a => a !== ex); toast('Đã tắt thông báo') }
    else { const { data } = await api.post('/alerts', { productId: p.id, kind }); alerts.value.push(data); toast(kind === 'PRICE' ? 'Sẽ báo bạn khi sản phẩm giảm giá' : 'Sẽ báo bạn khi có hàng lại') }
  } catch (e) { toast(errMsg(e), 'error') }
}
onMounted(async () => { if (auth.loggedIn) try { alerts.value = (await api.get('/alerts')).data } catch {} })
</script>
<style scoped>
.whead { gap: 10px; flex-wrap: wrap; } .shr { gap: 8px; }
.shbox { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin: 8px 0 14px; } .shbox .input { flex: 1 1 280px; min-width: 0; font-size: .8rem; }

.grid-w { grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr)); }
.wi { display: flex; flex-direction: column; gap: 8px; }
.alerts { display: flex; flex-wrap: wrap; gap: 6px; }
.al { display: inline-flex; align-items: center; gap: 6px; min-height: 36px; padding: 0 10px; border: 1.5px solid var(--border); background: var(--surface); color: var(--text); font: 600 .78rem inherit; font-family: inherit; cursor: pointer; }
.al svg { width: 15px; height: 15px; fill: none; stroke: currentColor; stroke-width: 2; }
.al.on { border-color: var(--accent); color: var(--accent); background: var(--bronze-soft); }
@media (max-width: 599px) { .grid-w { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 10px; } .al { font-size: .7rem; padding: 0 6px; } }
</style>
