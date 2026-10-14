<template>
  <div>
    <div class="row between"><h1>Chương trình ưu đãi</h1><button class="btn" @click="edit()">+ Tạo ưu đãi</button></div>
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>Mã</th><th>Tiêu đề</th><th>Giảm</th><th>Tối đa</th><th>Đơn tối thiểu</th><th>Thời gian</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="p in list" :key="p.id">
          <td><b>{{ p.code }}</b></td><td>{{ p.title }}<div class="small muted">{{ p.description }}</div></td><td>{{ p.discountPercent }}%</td>
          <td>{{ p.maxDiscount ? vnd(p.maxDiscount) : '—' }}</td><td>{{ vnd(p.minOrder) }}</td>
          <td class="small">{{ d(p.startDate) }} → {{ d(p.endDate) }}</td>
          <td><span :class="['badge', p.validNow ? 'badge-COMPLETED' : 'badge-CANCELLED']">{{ p.validNow ? 'Đang chạy' : (p.active ? 'Ngoài thời hạn' : 'Tắt') }}</span></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:6px"><button class="btn btn-sm btn-outline" @click="edit(p)">Sửa</button><button class="btn btn-sm btn-danger" @click="remove(p)">Xoá</button></div></td>
        </tr></tbody>
      </table>
    </div>
    <div v-if="form" class="modal-bg" @click.self="form = null">
      <div class="modal">
        <h3>{{ form.id ? 'Sửa ưu đãi' : 'Tạo ưu đãi' }}</h3>
        <div v-if="err" class="alert alert-error">{{ err }}</div>
        <div class="form-grid" style="margin-top:12px">
          <div class="field"><label>Mã *</label><input v-model="form.code" class="input" /></div>
          <div class="field"><label>Tiêu đề</label><input v-model="form.title" class="input" /></div>
          <div class="field"><label>% giảm</label><input v-model.number="form.discountPercent" type="number" class="input" /></div>
          <div class="field"><label>Giảm tối đa (đ, 0 = không giới hạn)</label><input v-model.number="form.maxDiscount" type="number" class="input" /></div>
          <div class="field"><label>Đơn tối thiểu (đ)</label><input v-model.number="form.minOrder" type="number" class="input" /></div>
          <div class="field"><label>Màu banner</label><input v-model="form.bannerColor" type="color" class="input" style="height:44px;padding:4px" /></div>
          <div class="field"><label>Bắt đầu</label><input v-model="form.startDate" type="date" class="input" /></div>
          <div class="field"><label>Kết thúc</label><input v-model="form.endDate" type="date" class="input" /></div>
        </div>
        <div class="field"><label>Mô tả</label><textarea v-model="form.description" rows="2" class="input" /></div>
        <label class="check field"><input v-model="form.active" type="checkbox" /> Kích hoạt</label>
        <div class="row"><button class="btn" @click="save">Lưu</button><button class="btn btn-outline" @click="form = null">Huỷ</button></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, d } from '../../format'
const list = ref([]), form = ref(null), err = ref('')
async function load() { list.value = (await api.get('/admin/promotions')).data }
const edit = p => { err.value = ''; form.value = p ? { ...p } : { code: '', title: '', discountPercent: 10, maxDiscount: 0, minOrder: 0, active: true, bannerColor: '#0284c7', startDate: new Date().toISOString().slice(0, 10), endDate: '' } }
async function save() {
  const f = { ...form.value }; delete f.validNow
  if (!f.endDate) f.endDate = null
  try { f.id ? await api.put(`/admin/promotions/${f.id}`, f) : await api.post('/admin/promotions', f); form.value = null; load() } catch (e) { err.value = errMsg(e) }
}
async function remove(p) { if (confirm(`Xoá mã ${p.code}?`)) { await api.delete(`/admin/promotions/${p.id}`); load() } }
onMounted(load)
</script>
