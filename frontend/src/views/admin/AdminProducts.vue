<template>

  <div>

    <div class="row between">

      <h1>Quản lý sản phẩm & Biến thể xe</h1>

      <div class="row">

        <button v-if="hasTestItems" class="btn btn-sm" style="background:#dc2626;color:#fff;font-weight:700" @click="clearTestItems">🗑 Xoá toàn bộ hàng mẫu test ({{ testCount }})</button>

        

        <router-link to="/admin/catalog" class="btn btn-outline btn-sm">Danh mục & hãng</router-link>

        <button class="btn btn-outline btn-sm" @click="imp = { text: '', result: null }">Nhập CSV</button>

        <button class="btn" @click="edit()">+ Thêm sản phẩm</button>

      </div>

    </div>

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

        <thead><tr><th class="ck"><input type="checkbox" :checked="sel.length && sel.length === items.length" aria-label="Chọn tất cả" @change="sel = $event.target.checked ? items.map(p => p.id) : []" /></th><th>Sản phẩm & Biến thể</th><th>Danh mục</th><th>Tỷ lệ / Tuổi</th><th>Giá bán theo biến thể</th><th>Tồn</th><th>Đã bán</th><th>★</th><th>Trạng thái</th><th /></tr></thead>

        <tbody><tr v-for="p in items" :key="p.id" :class="{ off: !p.active, picked: sel.includes(p.id) }">

          <td class="ck"><input v-model="sel" type="checkbox" :value="p.id" :aria-label="`Chọn ${p.name}`" /></td>

          <td><div class="row" style="flex-wrap:nowrap;gap:12px"><img loading="lazy" decoding="async" :src="p.images?.[0]" class="thumbimg" alt="" />

            <div><b>{{ p.name }}</b><div class="small muted">{{ p.sku }} · {{ p.brand?.name }}<span v-if="p.hasVariants" class="variant-tag"> ✨ Đa biến thể</span><span v-if="p.hazardous || (p.name && (p.name.toLowerCase().includes('pin') || p.name.toLowerCase().includes('lipo')))" class="badge" style="background:#fff7ed;color:#c2410c;border:1px solid #fed7aa;font-size:.7rem;margin-left:6px;padding:2px 6px">PIN / CHÁY NỔ</span><span v-if="p.sku?.startsWith('TEST-')" class="badge" style="background:#fee2e2;color:#dc2626;border:1px solid #f87171;font-size:.7rem;margin-left:6px;padding:2px 6px">MẪU TEST</span></div></div></div></td>

          <td class="small">{{ TYPES[p.type] }}</td><td class="small">{{ p.scale || '—' }} · {{ p.ageMin }}+</td>

          <td class="price" style="white-space:nowrap">

            <template v-if="p.hasVariants"><span class="small muted">từ </span></template>

            <b>{{ vnd(p.price) }}</b>

            <div v-if="p.oldPrice" class="small muted"><s>{{ vnd(p.oldPrice) }}</s></div>

          </td>

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

      <div class="modal modal-lg">

        <h3>{{ form.id ? 'Sửa sản phẩm #' + form.id : 'Thêm sản phẩm mới' }}</h3>

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

          <div class="field"><label>Giá bán tiêu chuẩn (đ) *</label><input v-model.number="form.price" type="number" class="input" /></div>

          <div class="field"><label>Giá gốc niêm yết (đ)</label><input v-model.number="form.oldPrice" type="number" class="input" /></div>

          <div class="field"><label>Tồn kho {{ vars.length ? '(tự động tổng hợp từ các biến thể)' : '' }}</label><input v-model.number="form.stock" type="number" class="input" :disabled="vars.length > 0" /></div>

        </div>

        <div class="field"><label>Mô tả sản phẩm</label><textarea v-model="form.description" rows="3" class="input" /></div>

        <div class="field"><label>Hình ảnh sản phẩm</label>

          <div class="imgs">

            <div v-for="(u, i) in form.images" :key="u + i" class="im"><img loading="lazy" decoding="async" :src="u" /><button title="Xoá" @click="form.images.splice(i, 1)">×</button><button v-if="i" class="l" title="Đặt làm ảnh chính" @click="form.images.unshift(form.images.splice(i, 1)[0])">★</button></div>

            <label class="im add">{{ uploading ? '…' : '+ Tải ảnh' }}<input type="file" accept="image/*" multiple hidden @change="up" /></label>

          </div>

          <div class="row" style="gap:6px;margin-top:6px"><input v-model="imgUrl" class="input" style="flex:1" placeholder="hoặc dán URL ảnh" /><button class="btn btn-sm btn-outline" @click="imgUrl && form.images.push(imgUrl); imgUrl = ''">Thêm</button></div>

        </div>



        <!-- HỆ THỐNG BIẾN THỂ XE VÀ GIÁ THEO TỪNG BIẾN THỂ -->

        <div class="field variant-block">

          <div class="row between" style="align-items:center;margin-bottom:6px">

            <div>

              <label style="font-weight:700;font-size:1rem;color:var(--text)">Hệ thống Biến thể & Giá theo biến thể (Variants)</label>

              <div class="small muted">Hỗ trợ các biến thể theo màu sắc xe hoặc cấu hình (Bản tiêu chuẩn, kèm Pin 3S, kèm sạc, độ đèn LED). Mỗi biến thể có giá bán & tồn kho riêng biệt.</div>

            </div>

            <button type="button" class="btn btn-sm btn-outline" @click="addEmptyVar">+ Thêm biến thể mới</button>

          </div>



          <!-- Thanh chọn nhanh màu sắc -->

          <div class="preset-box">

            <span class="small preset-label">Thêm nhanh màu xe:</span>

            <div class="preset-chips">

              <button type="button" class="chip" @click="addPresetVar('Bayside Blue', '#1e5aa8')"><i style="background:#1e5aa8" /> Bayside Blue</button>

              <button type="button" class="chip" @click="addPresetVar('Trắng Pearl', '#f5f5f5')"><i style="background:#f5f5f5;border:1px solid #ccc" /> Trắng Pearl</button>

              <button type="button" class="chip" @click="addPresetVar('Đỏ Midnight', '#c1121f')"><i style="background:#c1121f" /> Đỏ Midnight</button>

              <button type="button" class="chip" @click="addPresetVar('Đen Satin', '#1b1b1f')"><i style="background:#1b1b1f" /> Đen Satin</button>

              <button type="button" class="chip" @click="addPresetVar('Vàng Racing', '#f5c400')"><i style="background:#f5c400" /> Vàng Racing</button>

              <button type="button" class="chip" @click="addPresetVar('Xám Nardo', '#8a8d8f')"><i style="background:#8a8d8f" /> Xám Nardo</button>

            </div>

          </div>



          <!-- Thanh chọn nhanh phiên bản cấu hình -->

          <div class="preset-box" style="margin-top:6px">

            <span class="small preset-label">Thêm nhanh cấu hình:</span>

            <div class="preset-chips">

              <button type="button" class="chip" @click="addPresetVar('Bản tiêu chuẩn (Standard)', null, form.price)">Bản Standard</button>

              <button type="button" class="chip" @click="addPresetVar('Bản Full Option (Mở full cửa + Đèn LED)', null, (form.price || 0) + 150000)">Full Option + Đèn</button>

              <button type="button" class="chip" @click="addPresetVar('Bản RC Kèm Pin 2S + Tay khiển 2.4GHz', null, (form.price || 0) + 250000)">Kèm Pin 2S + Tay Khiển</button>

              <button type="button" class="chip" @click="addPresetVar('Bản RC Nâng Cấp Pin 3S + Sạc Nhanh', null, (form.price || 0) + 500000)">Kèm Pin 3S + Sạc Nhanh</button>

            </div>

          </div>



          <!-- Bảng chi tiết biến thể -->

          <table v-if="vars.length" class="table vt" style="margin-top:12px">

            <thead>

              <tr>

                <th>Tên biến thể / Phiên bản *</th>

                <th style="width:58px">Màu</th>

                <th>Giá bán (đ) *</th>

                <th>Giá gốc (đ)</th>

                <th style="width:75px">Tồn kho</th>

                <th>Hình ảnh biến thể</th>

                <th style="width:36px" />

              </tr>

            </thead>

            <tbody>

              <tr v-for="(v, i) in vars" :key="i">

                <td><input v-model="v.name" class="input" placeholder="VD: Bayside Blue / Kèm Pin 3S" /></td>

                <td><input v-model="v.color" type="color" class="input clr" title="Mã màu xe" /></td>

                <td><input v-model.number="v.price" type="number" class="input" placeholder="Giá bán" /></td>

                <td><input v-model.number="v.oldPrice" type="number" class="input" placeholder="Giá gốc" /></td>

                <td><input v-model.number="v.stock" type="number" class="input" min="0" style="width:70px" /></td>

                <td>
                  <div class="var-img-col">
                    <div v-if="v.image" class="var-img-preview">
                      <img :src="v.image" alt="Variant" />
                      <button type="button" class="del-var-img" title="Xoá ảnh" @click="v.image = ''">×</button>
                    </div>
                    <label class="btn btn-sm btn-outline var-up-btn">
                      <input type="file" accept="image/*" style="display:none" @change="uploadVarImage(i, $event)" />
                      <span>{{ uploadingVar === i ? 'Đang tải...' : (v.image ? 'Đổi ảnh' : 'Tải ảnh lên') }}</span>
                    </label>
                    <input v-model="v.image" class="input var-url-input" placeholder="URL ảnh..." />
                  </div>
                </td>

                <td><button type="button" class="btn btn-sm btn-danger" title="Xoá biến thể" @click="vars.splice(i, 1)">×</button></td>

              </tr>

            </tbody>

          </table>



          <div v-if="vars.length" class="var-summary">

            <span>Tổng tồn kho: <b>{{ totalVarStock }}</b> chiếc</span>

            <span>Khoảng giá theo biến thể: <b>{{ varPriceRange }}</b></span>

          </div>

          <p v-else class="small muted" style="margin-top:8px">Chưa có biến thể nào. Sản phẩm sẽ được bán theo giá và tồn kho tiêu chuẩn ở trên.</p>

        </div>



        <div class="field"><label>Thông số kỹ thuật (mỗi dòng "Tên: Giá trị")</label><textarea v-model="form.specsText" rows="4" class="input" /></div>

        <div class="row field">

          <label class="check"><input v-model="form.isNew" type="checkbox" /> Hàng mới</label>

          <label class="check"><input v-model="form.featured" type="checkbox" /> Nổi bật</label>

          <label class="check"><input v-model="form.active" type="checkbox" /> Đang bán</label>

        </div>

        <div class="row field" style="align-items:center;background:#fff7ed;border:1px solid #fdba74;border-radius:6px;padding:10px 14px;gap:16px;margin:12px 0">

          <label class="check" style="font-weight:700;color:#c2410c">

            <input v-model="form.hazardous" type="checkbox" />

            Sản phẩm dễ cháy nổ / Pin LiPo (Tự động cộng phụ phí vận chuyển an toàn)

          </label>

          <div v-if="form.hazardous" class="row" style="align-items:center;gap:6px">

            <span class="small muted">Phụ phí an toàn (đ):</span>

            <input v-model.number="form.hazardousFee" type="number" class="input" style="width:120px" placeholder="25000" />

          </div>

        </div>

        <div class="row" style="margin-top:16px"><button class="btn" @click="save">Lưu sản phẩm & Biến thể</button><button class="btn btn-outline" @click="form = null">Huỷ</button></div>

      </div>

    </div>

  </div>

</template>



<script setup>

import { ref, reactive, computed, onMounted } from 'vue'

import { useRoute } from 'vue-router'

import { toast } from '../../stores/prefs'

import api, { errMsg, uploadImage } from '../../api'

import { vnd, TYPES, catalog, loadCatalog } from '../../format'



const route = useRoute()

const sel = ref([]), bk = reactive({ mode: 'PERCENT', value: null }), busy = ref(false), imp = ref(route.query.import ? { text: '', result: null } : null)

const SAMPLE = 'sku,name,brand,type,grp,scale,price,oldPrice,stock,image\nAR-HW-DEMO-01,"Hot Wheels Demo, Nissan Z",Hot Wheels,MODEL,TOY,1:64,129000,159000,24,/images/toys/hw-r34-0.jpg'



async function bulk(active) {

  busy.value = true

  try {

    const body = typeof active === 'boolean' ? { ids: sel.value, active } : { ids: sel.value, mode: bk.mode, value: bk.value }

    const { data } = await api.post('/admin/products/bulk', body)

    toast(`Đã cập nhật ${data.updated} sản phẩm`)

    sel.value = []; bk.value = null; load()

  } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }

}



const readCsv = e => { const f = e.target.files[0]; if (!f) return; const rd = new FileReader(); rd.onload = () => (imp.value.text = rd.result); rd.readAsText(f, 'utf-8') }

async function doImport() {

  busy.value = true

  try {

    imp.value.result = (await api.post('/admin/products/import', imp.value.text, { headers: { 'Content-Type': 'text/plain' } })).data

    load()

  } catch (e) { imp.value.result = { created: 0, updated: 0, errors: [errMsg(e)] } } finally { busy.value = false }

}



const items = ref([]), q = ref(route.query.q || ''), type = ref(''), form = ref(null), err = ref(''), vars = ref([]), imgUrl = ref(''), uploading = ref(false)



const totalVarStock = computed(() => vars.value.reduce((s, v) => s + (Number(v.stock) || 0), 0))

const varPriceRange = computed(() => {

  if (!vars.value.length) return '—'

  const prices = vars.value.map(v => Number(v.price) || 0).filter(p => p > 0)

  if (!prices.length) return vnd(form.value?.price || 0)

  const min = Math.min(...prices), max = Math.max(...prices)

  return min === max ? vnd(min) : `${vnd(min)} – ${vnd(max)}`

})



function addEmptyVar() {

  vars.value.push({

    name: '',

    color: '#1e5aa8',

    price: form.value?.price || 0,

    oldPrice: form.value?.oldPrice || null,

    stock: 5,

    image: ''

  })

}



function addPresetVar(name, color, customPrice) {

  vars.value.push({

    name,

    color,

    price: customPrice || form.value?.price || 0,

    oldPrice: form.value?.oldPrice || null,

    stock: 5,

    image: ''

  })

}



async function load() {

  items.value = (await api.get('/admin/products', { params: { q: q.value || undefined, type: type.value || undefined, size: 300 } })).data.content

}



async function edit(p) {

  err.value = ''; vars.value = []

  let base = p

  if (p) {

    const r = (await api.get(`/admin/products/${p.id}`)).data

    base = r.product

    vars.value = r.variants.filter(v => v.active).map(v => ({ ...v }))

  }

  base = base || { name: '', sku: '', type: catalog.categories[0]?.code || 'MODEL', scale: '1:64', ageMin: 3, warrantyMonths: 0, material: '', price: 0, stock: 10, images: [], specs: {}, isNew: true, featured: false, active: true, brand: catalog.brands[0] }

  form.value = { ...base, images: [...(base.images || [])], brandId: base.brand?.id, specsText: Object.entries(base.specs || {}).map(([k, v]) => `${k}: ${v}`).join('\n') }

}




const uploadingVar = ref(null)
async function uploadVarImage(index, e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploadingVar.value = index
  try {
    const url = await uploadImage(file)
    vars.value[index].image = url
    toast('Đã tải ảnh biến thể thành công')
  } catch (x) {
    toast(errMsg(x), 'error')
  } finally {
    uploadingVar.value = null
    e.target.value = ''
  }
}

async function up(e) {

  uploading.value = true

  try { for (const f of e.target.files) form.value.images.push(await uploadImage(f)) }

  catch (x) { err.value = errMsg(x) } finally { uploading.value = false }

}



async function save() {

  const f = form.value, specs = {}

  f.specsText.split('\n').forEach(l => { const i = l.indexOf(':'); if (i > 0) specs[l.slice(0, i).trim()] = l.slice(i + 1).trim() })

  const product = { ...f, brand: f.brandId ? { id: f.brandId } : null, specs, oldPrice: f.oldPrice || null }

  delete product.specsText; delete product.brandId



  // Đồng bộ giá thấp nhất và tồn kho từ biến thể nếu có biến thể

  if (vars.value.length) {

    const validPrices = vars.value.map(v => v.price).filter(p => p > 0)

    if (validPrices.length) product.price = Math.min(...validPrices)

    product.stock = totalVarStock.value

  }



  const body = { product, variants: vars.value }

  try {

    f.id ? await api.put(`/admin/products/${f.id}`, body) : await api.post('/admin/products', body)

    form.value = null

    load()

    toast('Đã lưu sản phẩm & cập nhật biến thể thành công')

  } catch (e) {

    err.value = errMsg(e)

  }

}



async function remove(p) {

  if (confirm(`Ẩn sản phẩm "${p.name}"? (giữ lại lịch sử đơn hàng)`)) {

    await api.delete(`/admin/products/${p.id}`)

    load()

  }

}



async function restore(p) {

  await api.post(`/admin/products/${p.id}/restore`)

  load()

}



onMounted(() => { loadCatalog(); load(); if (route.query.new) edit() })

</script>



<style scoped>

.bulk { position: sticky; top: 64px; z-index: 5; display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding: 10px 14px; margin-bottom: 12px; background: #111; color: #fff; }

.bulk .input { width: auto; padding: 6px 10px; } .bulk .num { width: 120px; } .bulk .btn-outline { color: #fff; border-color: #fff; background: transparent; }

.ck { width: 36px; } .ck input { width: 17px; height: 17px; accent-color: var(--accent); } tr.picked { background: var(--bronze-soft); } .mono { font-family: ui-monospace, monospace; font-size: .8rem; }

.thumbimg { width: 56px; height: 42px; object-fit: cover; border-radius: 8px; flex-shrink: 0; background: #f6f1ea; }

.danger-text { color: var(--danger); }

.low { color: var(--danger); font-weight: 700; } .off { opacity: .5; }

.variant-tag { color: var(--accent); font-weight: 700; font-size: .8rem; }



.modal-lg { max-width: 960px; }

.imgs { display: flex; flex-wrap: wrap; gap: 8px; }

.im { position: relative; width: 90px; height: 70px; border-radius: 8px; overflow: hidden; border: 1px solid var(--border); } .im img { width: 100%; height: 100%; object-fit: cover; }

.im button { position: absolute; top: 2px; right: 2px; width: 20px; height: 20px; border-radius: 50%; border: none; background: rgba(255,255,255,.9); cursor: pointer; line-height: 1; padding: 0; }

.im button.l { right: auto; left: 2px; color: var(--accent); }

.add { display: flex; align-items: center; justify-content: center; border-style: dashed; cursor: pointer; font-size: .8rem; color: var(--muted); }




.var-img-col { display: flex; align-items: center; gap: 6px; }
.var-img-preview { position: relative; width: 36px; height: 36px; border-radius: 6px; overflow: hidden; border: 1px solid var(--border); flex-shrink: 0; background: var(--bg); }
.var-img-preview img { width: 100%; height: 100%; object-fit: cover; }
.del-var-img { position: absolute; top: 1px; right: 1px; width: 15px; height: 15px; border-radius: 50%; background: rgba(0,0,0,0.7); color: #fff; border: none; font-size: 11px; line-height: 1; display: grid; place-items: center; cursor: pointer; padding: 0; }
.var-up-btn { cursor: pointer; white-space: nowrap; font-size: .75rem; padding: 4px 8px; height: auto; margin: 0; }
.var-url-input { font-size: .78rem; padding: 4px 8px; min-width: 110px; width: 100%; }

/* Variant block */

.variant-block { background: var(--bg); border: 1px solid var(--border); padding: 16px; border-radius: 8px; margin: 14px 0; }

.preset-box { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.preset-label { font-weight: 600; color: var(--muted); font-size: .82rem; }

.preset-chips { display: flex; gap: 6px; flex-wrap: wrap; }

.chip { display: inline-flex; align-items: center; gap: 5px; border: 1px solid var(--border); background: var(--surface); padding: 3px 8px; border-radius: 12px; font-size: .78rem; cursor: pointer; color: var(--text); }

.chip:hover { border-color: var(--accent); color: var(--accent); }

.chip i { width: 11px; height: 11px; border-radius: 50%; display: inline-block; }



.vt { background: var(--surface); }

.vt th { font-size: .8rem; padding: 6px 8px; }

.vt td { padding: 4px 6px; }

.vt .input { padding: 5px 8px; font-size: .85rem; }

.clr { width: 44px; height: 32px; padding: 1px; cursor: pointer; }



.var-summary { display: flex; gap: 20px; justify-content: flex-end; margin-top: 10px; font-size: .86rem; color: var(--text-2); background: var(--surface); padding: 8px 12px; border-radius: 6px; border: 1px dashed var(--border); }

.var-summary b { color: var(--accent); }

</style>

