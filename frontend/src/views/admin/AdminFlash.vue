<template>
  <div>
    <div class="row between"><h1>Flash sale</h1><button class="btn" @click="edit()">+ Tạo flash sale</button></div>
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>Sản phẩm</th><th>Giá flash</th><th>Đã bán</th><th>Thời gian</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="f in list" :key="f.id">
          <td>{{ f.productName }}</td><td><b>{{ vnd(f.salePrice) }}</b></td>
          <td>{{ f.sold }}/{{ f.quota }}<div class="bar"><i :style="{ width: Math.min(100, f.sold / (f.quota || 1) * 100) + '%' }" /></div></td>
          <td class="small">{{ dt(f.startTime) }} → {{ dt(f.endTime) }}</td>
          <td><span :class="['badge', f.live ? 'badge-COMPLETED' : 'badge-CANCELLED']">{{ f.live ? 'Đang chạy' : f.active ? (new Date(f.startTime) > new Date() ? 'Sắp diễn ra' : 'Kết thúc') : 'Tắt' }}</span></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:6px"><button class="btn btn-sm btn-outline" @click="edit(f)">Sửa</button><button class="btn btn-sm btn-danger" @click="remove(f)">Xoá</button></div></td>
        </tr></tbody>
      </table>
    </div>
    <div v-if="form" class="modal-bg" @click.self="form = null">
      <div class="modal">
        <h3>{{ form.id ? 'Sửa flash sale' : 'Tạo flash sale' }}</h3>
        <div v-if="err" class="alert alert-error">{{ err }}</div>
        <div class="field"><label>Sản phẩm</label>
          <select v-model="form.productId" class="input"><option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }} — {{ vnd(p.price) }}</option></select></div>
        <div class="form-grid">
          <div class="field"><label>Giá flash (đ)</label><input v-model.number="form.salePrice" type="number" class="input" /></div>
          <div class="field"><label>Số suất</label><input v-model.number="form.quota" type="number" class="input" /></div>
          <div class="field"><label>Bắt đầu</label><input v-model="form.startTime" type="datetime-local" class="input" /></div>
          <div class="field"><label>Kết thúc</label><input v-model="form.endTime" type="datetime-local" class="input" /></div>
        </div>
        <label class="check"><input v-model="form.active" type="checkbox" /> Kích hoạt</label>
        <div class="row" style="justify-content:flex-end;margin-top:16px"><button class="btn btn-outline" @click="form = null">Huỷ</button><button class="btn" @click="save">Lưu</button></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd } from '../../format'
const list = ref([]), products = ref([]), form = ref(null), err = ref('')
const dt = s => s ? new Date(s).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : ''
const local = d => new Date(d.getTime() - d.getTimezoneOffset() * 6e4).toISOString().slice(0, 16)
const load = async () => { list.value = (await api.get('/admin/flash-sales')).data }
const edit = f => {
  err.value = ''
  form.value = f ? { ...f, startTime: f.startTime?.slice(0, 16), endTime: f.endTime?.slice(0, 16) }
    : { productId: products.value[0]?.id, salePrice: 0, quota: 10, sold: 0, active: true, startTime: local(new Date()), endTime: local(new Date(Date.now() + 864e5)) }
}
const save = async () => {
  const f = { ...form.value }; f.productName = products.value.find(p => p.id === f.productId)?.name
  try { f.id ? await api.put(`/admin/flash-sales/${f.id}`, f) : await api.post('/admin/flash-sales', f); form.value = null; load() } catch (e) { err.value = errMsg(e) }
}
const remove = async f => { if (confirm('Xoá flash sale này?')) { await api.delete(`/admin/flash-sales/${f.id}`); load() } }
onMounted(async () => { products.value = (await api.get('/admin/products', { params: { size: 500 } })).data.content; load() })
</script>
<style scoped>.bar { height: 4px; background: #f1e6d6; border-radius: 2px; margin-top: 4px; width: 80px; } .bar i { display: block; height: 100%; background: var(--accent); border-radius: 2px; }</style>
