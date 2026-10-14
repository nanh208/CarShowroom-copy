<template>
  <div>
    <div class="row between"><h1>Quản lý sản phẩm</h1><div class="row"><router-link to="/admin/catalog" class="btn btn-outline btn-sm">Danh mục & hãng</router-link><button class="btn btn-outline btn-sm" @click="imp = { text: '', result: null }">Nhập CSV</button><button class="btn" @click="edit()">+ Thêm sản phẩm</button></div></div>
    <div class="row" style="margin-bottom:14px">
      <input v-model="q" class="input" style="max-width:280px" placeholder="Tìm theo tên, SKU..." @keyup.enter="load" />
      <select v-model="type" class="input" style="max-width:200px" @change="load"><option value="">Tất cả danh mục</option><option v-for="c in catalog.categories" :key="c.code" :value="c.code">{{ c.name }}</option></select>
      <button class="btn btn-sm btn-outline" @click="load">Tìm</button>
    </div>
    <div v-if="sel.length" class="bulk" role="region" aria-label="Sửa hàng loạt">
      <b>Đã chọn {{ sel.length }}</b>
      <select v-model="bk.mode" class="input" aria-label="Kiểu sửa"><option value="PERCENT">Giá ± %</option><option value="SET">Đặt giá (đ)</option><option value="STOCK_ADD">Tồn kho + / −</option><option value="STOCK_SET">Đặt tồn kho</option></select>
      <input v-model.number="bk.value" type="number" class="input num" :placeholder="bk.mode === 'PERCENT' ? 'VD: -10' : 'Giá trị'" aria-label="Giá trị" />
      <button class="btn btn-sm" :disabled="bk.value === '' || bk.value === null || busy" @click="bulk()">Áp dụng</button>
      <button class="btn btn-sm btn-outline" @click="bulk(false)">Ẩn</button><button class="btn btn-sm btn-outline" @click="bulk(true)">Hiện</button>
      <button class="btn btn-sm btn-outline" @click="sel = []">Bỏ chọn</button>
    </div>
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th class="ck"><input type="checkbox" :checked="sel.length && sel.length === items.length" aria-label="Chọn tất cả" @change="sel = $event.target.checked ? items.map(p => p.id) : []" /></th><th>Sản phẩm</th><th>Danh mục</th><th>Tỷ lệ / Tuổi</th><th>Giá</th><th>Tồn</th><th>Đã bán</th><th>★</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="p in items" :key="p.id" :class="{ off: !p.active, picked: sel.includes(p.id) }">
          <td class="ck"><input v-model="sel" type="checkbox" :value="p.id" :aria-label="`Chọn ${p.name}`" /></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:12px"><img loading="lazy" decoding="async" :src="p.images?.[0]" class="thumbimg" alt="" />
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

    <div v-if="imp" class="modal-bg" @click.self="imp = null">
      <div class="modal">
        <h3>Nhập sản phẩm từ CSV</h3>
        <p class="small muted">Cột: <code>sku,name,brand,type,grp,scale,price,oldPrice,stock,image,description</code> — dòng đầu là tiêu đề, bắt buộc <b>sku, name, price</b>. Trùng SKU sẽ cập nhật.</p>
        <div class="row" style="margin:10px 0"><label class="btn btn-sm btn-outline">Chọn file .csv<input type="file" accept=".csv,text/csv" hidden @change="readCsv" /></label><button class="btn btn-sm btn-outline" @click="imp.text = SAMPLE">Dán mẫu</button></div>
        <textarea v-model="imp.text" class="input mono" rows="8" placeholder="sku,name,brand,type,grp,scale,price,stock" />
        <p v-if="imp.text" class="small muted">{{ imp.text.trim().split('\n').length - 1 }} dòng dữ liệu</p>
        <div v-if="imp.result" :class="['alert', imp.result.errors.length ? 'alert-error' : 'alert-success']" style="margin-top:10px">
          Tạo mới {{ imp.result.created }} · cập nhật {{ imp.result.updated }}<div v-for="e in imp.result.errors" :key="e" class="small">{{ e }}</div></div>
        <div class="row" style="margin-top:12px"><button class="btn" :disabled="!imp.text.trim() || busy" @click="doImport">{{ busy ? 'Đang nhập…' : 'Nhập' }}</button><button class="btn btn-outline" @click="imp = null">Đóng</button></div>
      </div>
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
            <div v-for="(u, i) in form.images" :key="u + i" class="im"><img loading="lazy" decoding="async" :src="u" /><button title="Xoá" @click="form.images.splice(i, 1)">×</button><button v-if="i" class="l" title="Đặt làm ảnh chính" @click="form.images.unshift(form.images.splice(i, 1)[0])">★</button></div>
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
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from '../../stores/prefs'
import api, { errMsg, uploadImage } from '../../api'
import { vnd, TYPES, catalog, loadCatalog } from '../../format'
const route = useRoute()
const sel = ref([]), bk = reactive({ mode: 'PERCENT', value: null }), busy = ref(false), imp = ref(route.query.import ? { text: '', result: null } : null)
const SAMPLE = 'sku,name,brand,type,grp,scale,price,oldPrice,stock,image\nAR-HW-DEMO-01,"Hot Wheels Demo, Nissan Z",Hot Wheels,MODEL,TOY,1:64,129000,159000,24,/images/toys/hw-r34-0.jpg'
async function bulk(active) {
  busy.value = true
  try { const body = typeof active === 'boolean' ? { ids: sel.value, active } : { ids: sel.value, mode: bk.mode, value: bk.value }
    const { data } = await api.post('/admin/products/bulk', body); toast(`Đã cập nhật ${data.updated} sản phẩm`); sel.value = []; bk.value = null; load() }
  catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
const readCsv = e => { const f = e.target.files[0]; if (!f) return; const rd = new FileReader(); rd.onload = () => (imp.value.text = rd.result); rd.readAsText(f, 'utf-8') }
async function doImport() {
  busy.value = true
  try { imp.value.result = (await api.post('/admin/products/import', imp.value.text, { headers: { 'Content-Type': 'text/plain' } })).data; load() }
  catch (e) { imp.value.result = { created: 0, updated: 0, errors: [errMsg(e)] } } finally { busy.value = false }
}
const items = ref([]), q = ref(route.query.q || ''), type = ref(''), form = ref(null), err = ref(''), vars = ref([]), imgUrl = ref(''), uploading = ref(false)

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
onMounted(() => { loadCatalog(); load(); if (route.query.new) edit() })
</script>
<style scoped>
.bulk { position: sticky; top: 64px; z-index: 5; display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding: 10px 14px; margin-bottom: 12px; background: #111; color: #fff; }
.bulk .input { width: auto; padding: 6px 10px; } .bulk .num { width: 120px; } .bulk .btn-outline { color: #fff; border-color: #fff; background: transparent; }
.ck { width: 36px; } .ck input { width: 17px; height: 17px; accent-color: var(--accent); } tr.picked { background: var(--bronze-soft); } .mono { font-family: ui-monospace, monospace; font-size: .8rem; }
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
