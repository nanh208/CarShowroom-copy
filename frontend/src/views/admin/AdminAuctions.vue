<template>
  <div>
    <div class="row between"><h1>Quản lý đấu giá</h1><button class="btn" @click="edit()">+ Tạo phiên đấu giá</button></div>
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>ID</th><th>Xe</th><th>Khởi điểm / bước</th><th>Giá hiện tại</th><th>Lượt</th><th>Thời gian</th><th>Trạng thái</th><th>Người thắng</th><th /></tr></thead>
        <tbody><tr v-for="a in list" :key="a.id">
          <td>{{ a.id }}</td>
          <td style="min-width:240px"><div class="row" style="flex-wrap:nowrap"><img v-if="a.images?.[0]" :src="a.images[0]" class="thumb" /><b>{{ a.title }}</b></div></td>
          <td class="small" style="white-space:nowrap">{{ ty(a.startPrice) }}<br />+{{ ty(a.minIncrement) }}</td>
          <td class="price">{{ ty(a.currentPrice) }}</td><td>{{ a.bidCount }}</td>
          <td class="small" style="white-space:nowrap">{{ dt(a.startTime) }}<br />→ {{ dt(a.endTime) }}</td>
          <td><span :class="['badge', 'st-' + a.status]">{{ AUCTION_STATUS[a.status] }}</span></td>
          <td class="small">{{ a.winner?.fullName || '—' }}<br /><router-link v-if="a.orderCode" :to="`/orders/${a.orderCode}`">{{ a.orderCode }}</router-link></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:6px">
            <button class="btn btn-sm btn-outline" @click="showBids(a)">Lượt đặt</button>
            <button v-if="!a.finalized && a.status !== 'CANCELLED'" class="btn btn-sm btn-outline" @click="edit(a)">Sửa</button>
            <button v-if="a.status === 'LIVE'" class="btn btn-sm" @click="act(a, 'close', 'Chốt phiên ngay và tạo đơn cho người thắng?')">Chốt</button>
            <button v-if="!a.finalized && a.status !== 'CANCELLED'" class="btn btn-sm btn-danger" @click="act(a, 'cancel', 'Huỷ phiên đấu giá?')">Huỷ</button>
          </div></td>
        </tr></tbody>
      </table>
    </div>
    <div v-if="err" class="alert alert-error" style="margin-top:12px">{{ err }}</div>

    <div v-if="form" class="modal-bg" @click.self="form = null">
      <div class="modal">
        <h3>{{ form.id ? 'Sửa phiên #' + form.id : 'Tạo phiên đấu giá' }}</h3>
        <div v-if="ferr" class="alert alert-error">{{ ferr }}</div>
        <div class="field" style="margin-top:12px"><label>Tên xe *</label><input v-model="form.title" class="input" placeholder="Ferrari F40 (1990)" /></div>
        <div class="form-grid">
          <div class="field"><label>Hãng</label><input v-model="form.brandName" class="input" /></div>
          <div class="field"><label>Năm</label><input v-model.number="form.year" type="number" class="input" /></div>
          <div class="field"><label>Odo</label><input v-model="form.mileage" class="input" placeholder="12.000 km" /></div>
          <div class="field"><label>Giá khởi điểm (đ) *</label><input v-model.number="form.startPrice" type="number" class="input" :disabled="form.bidCount > 0" /></div>
          <div class="field"><label>Bước giá tối thiểu (đ) *</label><input v-model.number="form.minIncrement" type="number" class="input" /></div>
          <div></div>
          <div class="field"><label>Bắt đầu</label><input v-model="form.startTime" type="datetime-local" class="input" /></div>
          <div class="field"><label>Kết thúc *</label><input v-model="form.endTime" type="datetime-local" class="input" /></div>
        </div>
        <div class="field"><label>Mô tả</label><textarea v-model="form.description" rows="3" class="input" /></div>
        <div class="field"><label>Ảnh (mỗi dòng 1 URL)</label><textarea v-model="form.imagesText" rows="2" class="input" placeholder="/images/cars/f40.jpg" /></div>
        <div class="field"><label>Thông số (mỗi dòng "Tên: Giá trị")</label><textarea v-model="form.specsText" rows="4" class="input" /></div>
        <div class="row"><button class="btn" @click="save">Lưu</button><button class="btn btn-outline" @click="form = null">Huỷ</button></div>
      </div>
    </div>

    <div v-if="bidsOf" class="modal-bg" @click.self="bidsOf = null">
      <div class="modal">
        <h3>Lượt đặt giá — {{ bidsOf.title }}</h3>
        <div v-if="!bidList.length" class="muted" style="margin-top:12px">Chưa có lượt đặt.</div>
        <table v-else class="table" style="margin-top:12px"><tbody>
          <tr v-for="b in bidList" :key="b.id"><td>{{ b.name }}<div class="small muted">{{ b.email }}</div></td><td class="price">{{ vnd(b.amount) }}</td><td class="small">{{ dt(b.createdAt) }}</td></tr>
        </tbody></table>
        <button class="btn btn-outline" style="margin-top:12px" @click="bidsOf = null">Đóng</button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, dt, ty, AUCTION_STATUS } from '../../format'
const list = ref([]), form = ref(null), err = ref(''), ferr = ref(''), bidsOf = ref(null), bidList = ref([])
const local = s => s ? s.slice(0, 16) : ''
async function load() { list.value = (await api.get('/admin/auctions')).data }
function edit(a) {
  ferr.value = ''
  const now = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
  const base = a || { title: '', brandName: '', year: 1990, mileage: '', startPrice: 0, minIncrement: 100000000, images: [], specs: {}, bidCount: 0,
    startTime: now.toISOString(), endTime: new Date(now.getTime() + 3 * 86400000).toISOString() }
  form.value = { ...base, startTime: local(base.startTime), endTime: local(base.endTime), imagesText: (base.images || []).join('\n'),
    specsText: Object.entries(base.specs || {}).map(([k, v]) => `${k}: ${v}`).join('\n') }
}
async function save() {
  const f = form.value, specs = {}
  f.specsText.split('\n').forEach(l => { const i = l.indexOf(':'); if (i > 0) specs[l.slice(0, i).trim()] = l.slice(i + 1).trim() })
  const body = { title: f.title, brandName: f.brandName, year: f.year, mileage: f.mileage, description: f.description, startPrice: f.startPrice,
    minIncrement: f.minIncrement, startTime: f.startTime ? f.startTime + ':00' : null, endTime: f.endTime ? f.endTime + ':00' : null,
    images: f.imagesText.split('\n').map(s => s.trim()).filter(Boolean), specs }
  try { f.id ? await api.put(`/admin/auctions/${f.id}`, body) : await api.post('/admin/auctions', body); form.value = null; load() } catch (e) { ferr.value = errMsg(e) }
}
async function act(a, what, q) { err.value = ''; if (!confirm(q)) return; try { await api.post(`/admin/auctions/${a.id}/${what}`) } catch (e) { err.value = errMsg(e) } load() }
async function showBids(a) { bidsOf.value = a; bidList.value = (await api.get(`/admin/auctions/${a.id}/bids`)).data }
onMounted(load)
</script>
<style scoped>
.thumb { width: 64px; height: 40px; object-fit: cover; border-radius: 6px; }
.st-LIVE { background: var(--danger); color: #fff; } .st-SCHEDULED { background: var(--primary); color: #fff; } .st-ENDED, .st-CANCELLED { background: #475569; }
.modal { max-width: 760px; }
</style>
