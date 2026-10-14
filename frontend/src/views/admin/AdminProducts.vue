<template>
  <div>
    <div class="row between"><h1>Quản lý sản phẩm</h1><div class="row"><button class="btn btn-outline btn-sm" @click="brandModal = true">Thương hiệu</button><button class="btn" @click="edit()">+ Thêm sản phẩm</button></div></div>
    <div class="row" style="margin-bottom:14px">
      <input v-model="q" class="input" style="max-width:280px" placeholder="Tìm theo tên..." @keyup.enter="load" />
      <select v-model="type" class="input" style="max-width:180px" @change="load"><option value="">Tất cả loại</option><option v-for="(v, k) in TYPES" :key="k" :value="k">{{ v }}</option></select>
      <button class="btn btn-sm btn-outline" @click="load">Tìm</button>
    </div>
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>ID</th><th>Sản phẩm</th><th>Hãng</th><th>Loại / Nhiên liệu</th><th>Giá</th><th>Tồn kho</th><th>Đã bán</th><th>Hiển thị</th><th /></tr></thead>
        <tbody><tr v-for="p in items" :key="p.id" :class="{ off: !p.active }">
          <td>{{ p.id }}</td>
          <td><div class="row" style="flex-wrap:nowrap;gap:12px"><img v-if="p.images?.[0]" :src="p.images[0]" class="thumbimg" /><span v-else class="dot" :style="{ background: p.baseColor }" /><b style="min-width:160px">{{ p.name }}</b><span v-if="p.isNew" class="badge badge-new">Mới</span></div></td>
          <td>{{ p.brand?.name }}</td><td class="small">{{ TYPES[p.type] }}<template v-if="p.type !== 'ACCESSORY'"><br />{{ p.fuel }} · {{ p.year }}</template></td>
          <td class="price" style="white-space:nowrap">{{ vnd(p.price) }}</td>
          <td :class="{ low: p.stock <= 3 }">{{ p.stock }}</td><td>{{ p.sold }}</td>
          <td><span :class="['badge', p.active ? 'badge-COMPLETED' : '']">{{ p.active ? 'Hiển thị' : 'Ẩn' }}</span></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:6px"><button class="btn btn-sm btn-outline" @click="edit(p)">Sửa</button>
            <button v-if="p.active" class="btn btn-sm btn-outline danger-text" @click="remove(p)">Ẩn</button></div></td>
        </tr></tbody>
      </table>
    </div>

    <div v-if="form" class="modal-bg" @click.self="form = null">
      <div class="modal">
        <h3>{{ form.id ? 'Sửa sản phẩm #' + form.id : 'Thêm sản phẩm' }}</h3>
        <div v-if="err" class="alert alert-error">{{ err }}</div>
        <div class="field" style="margin-top:12px"><label>Tên sản phẩm *</label><input v-model="form.name" class="input" /></div>
        <div class="form-grid">
          <div class="field"><label>Thương hiệu</label><select v-model="form.brandId" class="input"><option v-for="b in brands" :key="b.id" :value="b.id">{{ b.name }}</option></select></div>
          <div class="field"><label>Loại</label><select v-model="form.type" class="input"><option v-for="(v, k) in TYPES" :key="k" :value="k">{{ v }}</option></select></div>
          <div class="field"><label>Nhiên liệu</label><select v-model="form.fuel" class="input"><option v-for="x in [...FUELS, 'Phụ kiện']" :key="x">{{ x }}</option></select></div>
          <div class="field"><label>Năm sản xuất</label><input v-model.number="form.year" type="number" class="input" /></div>
          <div class="field"><label>Hộp số / ghi chú</label><input v-model="form.material" class="input" /></div>
          <div class="field"><label>Giá bán (đ) *</label><input v-model.number="form.price" type="number" class="input" /></div>
          <div class="field"><label>Giá gốc (đ, để trống nếu không giảm)</label><input v-model.number="form.oldPrice" type="number" class="input" /></div>
          <div class="field"><label>Tồn kho</label><input v-model.number="form.stock" type="number" class="input" /></div>
          <div class="field"><label>Màu gốc</label><input v-model="form.baseColor" type="color" class="input" style="height:44px;padding:4px" /></div>
        </div>
        <div class="field"><label>Mô tả</label><textarea v-model="form.description" rows="3" class="input" /></div>
        <div class="field"><label>Ảnh (mỗi dòng 1 URL)</label><textarea v-model="form.imagesText" rows="2" class="input" /></div>
        <div class="field"><label>Thông số (mỗi dòng "Tên: Giá trị")</label><textarea v-model="form.specsText" rows="4" class="input" /></div>
        <div class="row field">
          <label class="check"><input v-model="form.isNew" type="checkbox" /> Hàng mới</label>
          <label class="check"><input v-model="form.featured" type="checkbox" /> Nổi bật</label>
          <label class="check"><input v-model="form.customizable" type="checkbox" /> Cho phép thiết kế</label>
          <label class="check"><input v-model="form.active" type="checkbox" /> Hiển thị</label>
        </div>
        <div class="row"><button class="btn" @click="save">Lưu</button><button class="btn btn-outline" @click="form = null">Huỷ</button></div>
      </div>
    </div>

    <div v-if="brandModal" class="modal-bg" @click.self="brandModal = false">
      <div class="modal">
        <h3>Thương hiệu</h3>
        <div v-if="berr" class="alert alert-error">{{ berr }}</div>
        <table class="table"><tbody><tr v-for="b in brands" :key="b.id">
          <td><input v-model="b.name" class="input" /></td><td><input v-model="b.country" class="input" placeholder="Quốc gia" /></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:6px"><button class="btn btn-sm btn-outline" @click="saveBrand(b)">Lưu</button><button class="btn btn-sm btn-danger" @click="delBrand(b)">Xoá</button></div></td>
        </tr></tbody></table>
        <div class="row" style="margin-top:12px"><input v-model="newBrand.name" class="input grow" placeholder="Tên thương hiệu mới" /><input v-model="newBrand.country" class="input grow" placeholder="Quốc gia" />
          <button class="btn btn-sm" @click="saveBrand(newBrand)">Thêm</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, TYPES, FUELS } from '../../format'
const items = ref([]), brands = ref([]), q = ref(''), type = ref(''), form = ref(null), err = ref('')
const brandModal = ref(false), berr = ref(''), newBrand = ref({ name: '', country: '' })

async function load() {
  items.value = (await api.get('/admin/products', { params: { q: q.value || undefined, type: type.value || undefined, size: 200 } })).data.content
  brands.value = (await api.get('/brands')).data
}
function edit(p) {
  err.value = ''
  const base = p || { name: '', type: 'SEDAN', fuel: 'Xăng', year: new Date().getFullYear(), price: 0, stock: 5, material: 'AT 8 cấp', baseColor: '#c1121f', images: [], specs: {}, isNew: true, featured: false, customizable: true, active: true, brand: brands.value[0] }
  form.value = { ...base, brandId: base.brand?.id, imagesText: (base.images || []).join('\n'),
    specsText: Object.entries(base.specs || {}).map(([k, v]) => `${k}: ${v}`).join('\n') }
}
async function save() {
  const f = form.value
  const specs = {}
  f.specsText.split('\n').forEach(l => { const i = l.indexOf(':'); if (i > 0) specs[l.slice(0, i).trim()] = l.slice(i + 1).trim() })
  const body = { ...f, brand: f.brandId ? { id: f.brandId } : null, images: f.imagesText.split('\n').map(s => s.trim()).filter(Boolean), specs, oldPrice: f.oldPrice || null }
  delete body.imagesText; delete body.specsText; delete body.brandId
  try { f.id ? await api.put(`/admin/products/${f.id}`, body) : await api.post('/admin/products', body); form.value = null; load() }
  catch (e) { err.value = errMsg(e) }
}
async function remove(p) { if (confirm(`Ẩn sản phẩm "${p.name}"? (giữ lại lịch sử đơn hàng)`)) { await api.delete(`/admin/products/${p.id}`); load() } }
async function saveBrand(b) {
  berr.value = ''
  try { b.id ? await api.put(`/admin/brands/${b.id}`, b) : await api.post('/admin/brands', b); newBrand.value = { name: '', country: '' }; load() } catch (e) { berr.value = errMsg(e) }
}
async function delBrand(b) { berr.value = ''; if (confirm('Xoá thương hiệu?')) try { await api.delete(`/admin/brands/${b.id}`); load() } catch (e) { berr.value = errMsg(e) } }
onMounted(load)
</script>
<style scoped>
.thumbimg { width: 72px; height: 45px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.danger-text { color: var(--danger); }
.dot { width: 14px; height: 14px; border-radius: 50%; flex-shrink: 0; }
.low { color: var(--danger); font-weight: 700; } .off { opacity: .5; }
.modal { max-width: 760px; }
</style>
