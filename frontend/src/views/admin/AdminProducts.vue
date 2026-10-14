<template>
  <div>
    <div class="row between"><h1>Quản lý sản phẩm</h1><div class="row"><router-link to="/admin/catalog" class="btn btn-outline btn-sm">Danh mục & hãng</router-link><button class="btn" @click="edit()">+ Thêm sản phẩm</button></div></div>
    <div class="row" style="margin-bottom:14px">
      <input v-model="q" class="input" style="max-width:280px" placeholder="Tìm theo tên, SKU..." @keyup.enter="load" />
      <select v-model="type" class="input" style="max-width:200px" @change="load"><option value="">Tất cả danh mục</option><option v-for="c in catalog.categories" :key="c.code" :value="c.code">{{ c.name }}</option></select>
      <button class="btn btn-sm btn-outline" @click="load">Tìm</button>
    </div>
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>Sản phẩm</th><th>Danh mục</th><th>Tỷ lệ / Tuổi</th><th>Giá</th><th>Tồn</th><th>Đã bán</th><th>★</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="p in items" :key="p.id" :class="{ off: !p.active }">
          <td><div class="row" style="flex-wrap:nowrap;gap:12px"><img :src="p.images?.[0]" class="thumbimg" alt="" />
            <div><b>{{ p.name }}</b><div class="small muted">{{ p.sku }} · {{ p.brand?.name }}<span v-if="p.hasVariants"> · có biến thể</span></div></div></div></td>
          <td class="small">{{ TYPES[p.type] }}</td><td class="small">{{ p.scale || '—' }} · {{ p.ageMin }}+</td>
          <td class="price" style="white-space:nowrap">{{ vnd(p.price) }}<div v-if="p.oldPrice" class="small muted"><s>{{ vnd(p.oldPrice) }}</s></div></td>
          <td :class="{ low: p.stock <= 5 }">{{ p.stock }}</td><td>{{ p.sold }}</td><td class="small">{{ p.reviewCount ? p.rating?.toFixed(1) : '—' }}</td>
          <td><span :class="['badge', p.active ? 'badge-COMPLETED' : '']">{{ p.active ? 'Đang bán' : 'Ẩn' }}</span></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:6px"><button class="btn btn-sm btn-outline" @click="edit(p)">Sửa</button>
            <button v-if="p.active" class="btn btn-sm btn-outline danger-text" @click="remove(p)">Ẩn</button>
            <button v-else class="btn btn-sm btn-outline" @click="restore(p)">Hiện</button></div></td>
        </tr></tbody>
      </table>
    </div>

    <div v-if="form" class="modal-bg" @click.self="form = null">
      <div class="modal">
        <h3>{{ form.id ? 'Sửa sản phẩm #' + form.id : 'Thêm sản phẩm' }}</h3>
        <div v-if="err" class="alert alert-error">{{ err }}</div>
        <div class="form-grid" style="margin-top:12px">
          <div class="field" style="grid-column:span 2"><label>Tên sản phẩm *</label><input v-model="form.name" class="input" /></div>
          <div class="field"><label>SKU</label><input v-model="form.sku" class="input" placeholder="AR-..." /></div>
          <div class="field"><label>Thương hiệu</label><select v-model="form.brandId" class="input"><option v-for="b in catalog.brands" :key="b.id" :value="b.id">{{ b.name }}</option></select></div>
          <div class="field"><label>Danh mục *</label><select v-model="form.type" class="input"><option v-for="c in catalog.categories" :key="c.code" :value="c.code">{{ c.name }}</option></select></div>
          <div class="field"><label>Tỷ lệ</label><input v-model="form.scale" class="input" list="scales" placeholder="1:64" /><datalist id="scales"><option v-for="s in catalog.options.scales" :key="s" :value="s" /></datalist></div>
          <div class="field"><label>Độ tuổi tối thiểu</label><select v-model.number="form.ageMin" class="input"><option v-for="a in [3, 6, 8, 14]" :key="a" :value="a">{{ a }}+</option></select></div>
          <div class="field"><label>Chất liệu</label><input v-model="form.material" class="input" placeholder="Hợp kim + nhựa ABS" /></div>
          <div class="field"><label>Bảo hành (tháng)</label><input v-model.number="form.warrantyMonths" type="number" class="input" /></div>
          <div class="field"><label>Giá bán (đ) *</label><input v-model.number="form.price" type="number" class="input" /></div>
          <div class="field"><label>Giá gốc (đ)</label><input v-model.number="form.oldPrice" type="number" class="input" /></div>
          <div class="field"><label>Tồn kho {{ vars.length ? '(tính từ biến thể)' : '' }}</label><input v-model.number="form.stock" type="number" class="input" :disabled="vars.length > 0" /></div>
        </div>
        <div class="field"><label>Mô tả</label><textarea v-model="form.description" rows="3" class="input" /></div>
        <div class="field"><label>Ảnh</label>
          <div class="imgs">
            <div v-for="(u, i) in form.images" :key="u + i" class="im"><img :src="u" /><button title="Xoá" @click="form.images.splice(i, 1)">×</button><button v-if="i" class="l" title="Đặt làm ảnh chính" @click="form.images.unshift(form.images.splice(i, 1)[0])">★</button></div>
            <label class="im add">{{ uploading ? '…' : '+ Tải ảnh' }}<input type="file" accept="image/*" multiple hidden @change="up" /></label>
          </div>
          <div class="row" style="gap:6px;margin-top:6px"><input v-model="imgUrl" class="input" style="flex:1" placeholder="hoặc dán URL ảnh" /><button class="btn btn-sm btn-outline" @click="imgUrl && form.images.push(imgUrl); imgUrl = ''">Thêm</button></div>
        </div>
        <div class="field"><div class="row between"><label>Biến thể (màu / phiên bản) — mỗi biến thể có giá & tồn kho riêng</label><button class="btn btn-sm btn-outline" @click="vars.push({ name: '', color: '#a87a3c', price: form.price, stock: 0, image: '' })">+ Biến thể</button></div>
          <table v-if="vars.length" class="table vt"><thead><tr><th>Tên</th><th>Màu</th><th>Giá</th><th>Giá gốc</th><th>Tồn</th><th>Ảnh (URL)</th><th /></tr></thead><tbody>
            <tr v-for="(v, i) in vars" :key="i"><td><input v-model="v.name" class="input" /></td><td><input v-model="v.color" type="color" class="input clr" /></td>
              <td><input v-model.number="v.price" type="number" class="input" /></td><td><input v-model.number="v.oldPrice" type="number" class="input" /></td>
              <td><input v-model.number="v.stock" type="number" class="input" style="width:70px" /></td><td><input v-model="v.image" class="input" /></td>
              <td><button class="btn btn-sm btn-danger" @click="vars.splice(i, 1)">×</button></td></tr></tbody></table>
        </div>
        <div class="field"><label>Thông số (mỗi dòng "Tên: Giá trị")</label><textarea v-model="form.specsText" rows="4" class="input" /></div>
        <div class="row field">
          <label class="check"><input v-model="form.isNew" type="checkbox" /> Hàng mới</label>
          <label class="check"><input v-model="form.featured" type="checkbox" /> Nổi bật</label>
          <label class="check"><input v-model="form.active" type="checkbox" /> Đang bán</label>
        </div>
        <div class="row"><button class="btn" @click="save">Lưu</button><button class="btn btn-outline" @click="form = null">Huỷ</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg, uploadImage } from '../../api'
import { vnd, TYPES, catalog, loadCatalog } from '../../format'
const items = ref([]), q = ref(''), type = ref(''), form = ref(null), err = ref(''), vars = ref([]), imgUrl = ref(''), uploading = ref(false)

async function load() {
  items.value = (await api.get('/admin/products', { params: { q: q.value || undefined, type: type.value || undefined, size: 300 } })).data.content
}
async function edit(p) {
  err.value = ''; vars.value = []
  let base = p
  if (p) { const r = (await api.get(`/admin/products/${p.id}`)).data; base = r.product; vars.value = r.variants.filter(v => v.active).map(v => ({ ...v })) }
  base = base || { name: '', sku: '', type: catalog.categories[0]?.code || 'MODEL', scale: '1:64', ageMin: 3, warrantyMonths: 0, material: '', price: 0, stock: 10, images: [], specs: {}, isNew: true, featured: false, active: true, brand: catalog.brands[0] }
  form.value = { ...base, images: [...(base.images || [])], brandId: base.brand?.id, specsText: Object.entries(base.specs || {}).map(([k, v]) => `${k}: ${v}`).join('\n') }
}
async function up(e) {
  uploading.value = true
  try { for (const f of e.target.files) form.value.images.push(await uploadImage(f)) } catch (x) { err.value = errMsg(x) } finally { uploading.value = false }
}
async function save() {
  const f = form.value, specs = {}
  f.specsText.split('\n').forEach(l => { const i = l.indexOf(':'); if (i > 0) specs[l.slice(0, i).trim()] = l.slice(i + 1).trim() })
  const product = { ...f, brand: f.brandId ? { id: f.brandId } : null, specs, oldPrice: f.oldPrice || null }
  delete product.specsText; delete product.brandId
  const body = { product, variants: vars.value }
  try { f.id ? await api.put(`/admin/products/${f.id}`, body) : await api.post('/admin/products', body); form.value = null; load() }
  catch (e) { err.value = errMsg(e) }
}
async function remove(p) { if (confirm(`Ẩn sản phẩm "${p.name}"? (giữ lại lịch sử đơn hàng)`)) { await api.delete(`/admin/products/${p.id}`); load() } }
async function restore(p) { await api.post(`/admin/products/${p.id}/restore`); load() }
onMounted(() => { loadCatalog(); load() })
</script>
<style scoped>
.thumbimg { width: 56px; height: 42px; object-fit: cover; border-radius: 8px; flex-shrink: 0; background: #f6f1ea; }
.danger-text { color: var(--danger); }
.low { color: var(--danger); font-weight: 700; } .off { opacity: .5; }
.modal { max-width: 900px; }
.imgs { display: flex; flex-wrap: wrap; gap: 8px; }
.im { position: relative; width: 90px; height: 70px; border-radius: 8px; overflow: hidden; border: 1px solid var(--border); } .im img { width: 100%; height: 100%; object-fit: cover; }
.im button { position: absolute; top: 2px; right: 2px; width: 20px; height: 20px; border-radius: 50%; border: none; background: rgba(255,255,255,.9); cursor: pointer; line-height: 1; padding: 0; }
.im button.l { right: auto; left: 2px; color: var(--accent); }
.add { display: flex; align-items: center; justify-content: center; border-style: dashed; cursor: pointer; font-size: .8rem; color: var(--muted); }
.vt .input { padding: 6px 8px; } .clr { width: 44px; height: 34px; padding: 2px; }
</style>
