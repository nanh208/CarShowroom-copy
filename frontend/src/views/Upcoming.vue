<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Sắp ra mắt' }]" />
    <h1>Sắp ra mắt</h1>
    <p class="muted" style="margin:6px 0 20px">Đặt trước để giữ suất — chỉ thanh toán cọc khi hàng về, được báo qua email & thông báo.</p>
    <div v-if="loading" class="ug"><div v-for="i in 2" :key="i" class="sk" style="height:260px" /></div>
    <p v-else-if="!list.length" class="muted">Hiện chưa có mẫu nào mở đặt trước.</p>
    <div v-else class="ug" v-stagger>
      <article v-for="p in list" :key="p.id" class="card up">
        <img :src="p.images[0]" alt="" loading="lazy" />
        <div class="bd">
          <span class="tag">Đặt trước</span><h3>{{ p.name }}</h3>
          <p class="small muted">{{ p.brand?.name }} · tỉ lệ {{ p.scale }}</p>
          <div class="cd"><div><b>{{ daysLeft(p) }}</b><small>ngày nữa</small></div><div><b>{{ d(p.releaseDate) }}</b><small>dự kiến</small></div><div><b class="money">{{ vnd(p.price) }}</b><small>giá dự kiến</small></div></div>
          <p v-if="p.preorderDeposit" class="small">Cọc <b class="money">{{ vnd(p.preorderDeposit) }}</b>/sp khi hàng về</p>
          <p v-if="mine.some(o => o.productId === p.id && ['WAITING', 'NOTIFIED'].includes(o.status))" class="ok small">✓ Bạn đã đặt trước mẫu này</p>
          <button v-else class="btn" @click="openForm(p)">Đặt trước</button>
        </div>
      </article>
    </div>
    <section v-if="mine.length" class="card" style="margin-top:24px">
      <h3>Đặt trước của tôi</h3>
      <div class="table-wrap"><table class="table"><thead><tr><th>Sản phẩm</th><th>SL</th><th>Trạng thái</th><th>Ngày</th><th /></tr></thead>
        <tbody><tr v-for="o in mine" :key="o.id"><td>{{ o.productName }}</td><td>{{ o.qty }}</td><td><span :class="['badge', ST[o.status].b]">{{ ST[o.status].l }}</span></td><td class="small">{{ d(o.createdAt) }}</td>
          <td><button v-if="['WAITING', 'NOTIFIED'].includes(o.status)" class="btn btn-sm btn-outline" @click="cancel(o)">Huỷ</button><router-link v-if="o.status === 'NOTIFIED'" :to="`/products/${o.productId}`" class="btn btn-sm">Mua ngay</router-link></td></tr></tbody></table></div>
    </section>
    <div v-if="form" class="modal-bg" @click.self="form = null">
      <form class="modal" @submit.prevent="submit">
        <h3>Đặt trước: {{ form.p.name }}</h3>
        <template v-if="!auth.loggedIn">
          <div class="field"><label>Họ tên *</label><input v-model.trim="form.name" class="input" required /></div>
          <div class="field"><label>Email *</label><input v-model.trim="form.email" type="email" class="input" required /></div>
          <div class="field"><label>Số điện thoại</label><input v-model.trim="form.phone" class="input" inputmode="tel" pattern="0\d{9}" title="10 số, bắt đầu bằng 0" /></div>
        </template>
        <div class="field"><label>Số lượng (tối đa 5)</label><input v-model.number="form.qty" type="number" min="1" max="5" class="input" /></div>
        <div class="field"><label>Ghi chú</label><input v-model="form.note" class="input" maxlength="200" /></div>
        <p v-if="err" class="alert alert-error">{{ err }}</p>
        <div class="row" style="justify-content:flex-end;gap:8px"><button type="button" class="btn btn-outline" @click="form = null">Huỷ</button><button class="btn" :disabled="busy">{{ busy ? 'Đang gửi…' : 'Xác nhận đặt trước' }}</button></div>
      </form>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../api'
import { vnd, d } from '../format'
import { useAuth } from '../stores/auth'
import { toast } from '../stores/prefs'
import Breadcrumb from '../components/Breadcrumb.vue'
const ST = { WAITING: { l: 'Đang chờ hàng', b: 'badge-PENDING' }, NOTIFIED: { l: 'Hàng đã về', b: 'badge-CONFIRMED' }, CONVERTED: { l: 'Đã mua', b: 'badge-COMPLETED' }, CANCELLED: { l: 'Đã huỷ', b: 'badge-CANCELLED' } }
const auth = useAuth(), list = ref([]), mine = ref([]), loading = ref(true), form = ref(null), busy = ref(false), err = ref('')
const daysLeft = p => p.releaseDate ? Math.max(0, Math.ceil((new Date(p.releaseDate) - Date.now()) / 864e5)) : '—'
const openForm = p => { err.value = ''; form.value = { p, name: '', email: '', phone: '', qty: 1, note: '' } }
const loadMine = async () => { if (auth.loggedIn) mine.value = (await api.get('/account/preorders')).data }
async function submit() {
  busy.value = true; err.value = ''
  try { const f = form.value; await api.post('/public/preorders', { productId: f.p.id, name: f.name, email: f.email, phone: f.phone, qty: f.qty, note: f.note }); toast('Đã đặt trước — kiểm tra email xác nhận'); form.value = null; loadMine() }
  catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
async function cancel(o) { if (!confirm('Huỷ đặt trước ' + o.productName + '?')) return; try { await api.post(`/account/preorders/${o.id}/cancel`); loadMine() } catch (e) { toast(errMsg(e), 'error') } }
onMounted(async () => { try { list.value = (await api.get('/public/upcoming')).data; await loadMine() } finally { loading.value = false } })
</script>
<style scoped>
.ug { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 460px), 1fr)); gap: 16px; }
.up { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr); gap: 16px; padding: 0; overflow: hidden; }
.up img { width: 100%; height: 100%; min-height: 220px; object-fit: cover; } .bd { padding: 16px 16px 16px 0; display: flex; flex-direction: column; gap: 6px; align-items: flex-start; }
.tag { background: #111; color: #fff; font: 700 .68rem 'Oswald', sans-serif; letter-spacing: .12em; text-transform: uppercase; padding: 3px 8px; }
.cd { display: grid; grid-template-columns: repeat(3, auto); gap: 14px; margin: 6px 0; } .cd b { display: block; font: 700 1.05rem 'Oswald', sans-serif; } .cd small { font-size: .7rem; color: var(--muted); }
.ok { color: var(--success, #2e7d32); font-weight: 600; }
@media (max-width: 560px) { .up { grid-template-columns: 1fr; } .up img { min-height: 0; aspect-ratio: 16/10; } .bd { padding: 0 16px 16px; } }
</style>
