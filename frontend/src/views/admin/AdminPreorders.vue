<template>
  <div>
    <h1>Đặt trước</h1>
    <p class="muted small">Quản lý mẫu mở đặt trước và danh sách khách đang chờ. “Báo hàng về” gửi email + thông báo cho toàn bộ khách đang chờ mẫu đó.</p>
    <div class="card" style="margin:14px 0">
      <div class="row between th"><h3>Mẫu mở đặt trước</h3>
        <div class="row" style="gap:6px;flex-wrap:wrap"><select v-model="addId" class="input" style="width:auto;max-width:260px" aria-label="Chọn sản phẩm"><option value="">+ Mở đặt trước cho sản phẩm…</option><option v-for="p in products.filter(p => !p.preorder)" :key="p.id" :value="p.id">{{ p.name }}</option></select>
          <button class="btn btn-sm" :disabled="!addId" @click="setPre(products.find(p => p.id === addId), true)">Mở</button></div></div>
      <div class="table-wrap"><table class="table"><thead><tr><th>Sản phẩm</th><th>Ngày phát hành</th><th>Cọc/sp</th><th>Đang chờ</th><th /></tr></thead>
        <tbody><tr v-for="p in products.filter(p => p.preorder)" :key="p.id">
          <td><b>{{ p.name }}</b><div class="small muted">{{ p.sku }} · tồn {{ p.stock }}</div></td>
          <td><input v-model="p.releaseDate" type="date" class="input" style="width:auto" :aria-label="`Ngày phát hành ${p.name}`" /></td>
          <td><input v-model.number="p.preorderDeposit" type="number" step="50000" min="0" class="input" style="width:120px" :aria-label="`Cọc ${p.name}`" /></td>
          <td><b>{{ waiting(p.id) }}</b> khách</td>
          <td><div class="row" style="gap:6px;flex-wrap:nowrap"><button class="btn btn-sm btn-outline" @click="setPre(p, true)">Lưu</button><button class="btn btn-sm" :disabled="!waiting(p.id)" @click="notifyAll(p)">Báo hàng về</button><button class="btn btn-sm btn-outline" @click="setPre(p, false)">Đóng</button></div></td>
        </tr><tr v-if="!products.some(p => p.preorder)"><td colspan="5" class="muted">Chưa có mẫu nào mở đặt trước.</td></tr></tbody></table></div>
    </div>
    <div class="card">
      <div class="row between th"><h3>Đăng ký đặt trước ({{ shown.length }})</h3>
        <div class="row" style="gap:6px;flex-wrap:wrap"><input v-model.trim="q" class="input" placeholder="Tìm tên, email, SĐT…" aria-label="Tìm" style="width:200px" />
          <select v-model="st" class="input" style="width:auto" aria-label="Trạng thái"><option value="">Mọi trạng thái</option><option v-for="(v, k) in ST" :key="k" :value="k">{{ v.l }}</option></select>
          <button class="btn btn-sm btn-outline" @click="csv">⤓ CSV</button></div></div>
      <div v-if="loading" class="sk" style="height:120px" />
      <div v-else class="table-wrap"><table class="table"><thead><tr><th>Khách</th><th>Sản phẩm</th><th>SL</th><th>Cọc</th><th>Trạng thái</th><th>Ngày</th><th /></tr></thead>
        <tbody><tr v-for="o in shown" :key="o.id"><td><b>{{ o.name }}</b><div class="small muted">{{ o.email }} · {{ o.phone }}</div><div v-if="o.note" class="small">“{{ o.note }}”</div></td><td>{{ o.productName }}</td><td>{{ o.qty }}</td><td>{{ vnd(o.deposit) }}</td>
          <td><span :class="['badge', ST[o.status].b]">{{ ST[o.status].l }}</span></td><td class="small">{{ dt(o.createdAt) }}</td>
          <td><select class="input" style="width:auto;padding:5px" :value="o.status" :aria-label="`Đổi trạng thái ${o.name}`" @change="setSt(o, $event.target.value)"><option v-for="(v, k) in ST" :key="k" :value="k">{{ v.l }}</option></select></td></tr>
          <tr v-if="!shown.length"><td colspan="7" class="muted">Không có đăng ký.</td></tr></tbody></table></div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, dt } from '../../format'
import { toast } from '../../stores/prefs'
const ST = { WAITING: { l: 'Đang chờ', b: 'badge-PENDING' }, NOTIFIED: { l: 'Đã báo hàng về', b: 'badge-CONFIRMED' }, CONVERTED: { l: 'Đã mua', b: 'badge-COMPLETED' }, CANCELLED: { l: 'Đã huỷ', b: 'badge-CANCELLED' } }
const list = ref([]), products = ref([]), q = ref(''), st = ref(''), addId = ref(''), loading = ref(true)
const waiting = id => list.value.filter(o => o.productId === id && o.status === 'WAITING').length
const shown = computed(() => list.value.filter(o => (!st.value || o.status === st.value) && (!q.value || [o.name, o.email, o.phone, o.productName].join(' ').toLowerCase().includes(q.value.toLowerCase()))))
const load = async () => { const [a, b] = await Promise.all([api.get('/admin/preorders'), api.get('/admin/products', { params: { size: 500 } })]); list.value = a.data; products.value = b.data.content; loading.value = false }
async function setPre(p, on) { try { await api.put(`/admin/products/${p.id}/preorder`, { preorder: on, releaseDate: p.releaseDate || null, deposit: p.preorderDeposit ?? 0 }); toast(on ? 'Đã lưu' : 'Đã đóng đặt trước'); addId.value = ''; load() } catch (e) { toast(errMsg(e), 'error') } }
async function notifyAll(p) { if (!confirm(`Gửi “hàng về” cho ${waiting(p.id)} khách đang chờ ${p.name}?`)) return; const { data } = await api.post(`/admin/preorders/notify-product/${p.id}`); toast(`Đã báo ${data.notified} khách`); load() }
async function setSt(o, s) { try { await api.post(`/admin/preorders/${o.id}/status`, { status: s }); load() } catch (e) { toast(errMsg(e), 'error') } }
function csv() { const rows = [['Tên', 'Email', 'SĐT', 'Sản phẩm', 'SL', 'Cọc', 'Trạng thái', 'Ngày'], ...shown.value.map(o => [o.name, o.email, o.phone, o.productName, o.qty, o.deposit, ST[o.status].l, o.createdAt])]
  const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob(['\ufeff' + rows.map(r => r.map(v => `"${String(v ?? '').replace(/"/g, '""')}"`).join(',')).join('\r\n')], { type: 'text/csv' })); a.download = 'dat-truoc.csv'; a.click() }
onMounted(load)
</script>
<style scoped>.th { gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }</style>
