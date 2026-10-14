<template>
  <div>
    <h1>Danh mục & thương hiệu</h1>
    <div class="grid two">
      <div class="card">
        <div class="row between"><h3>Danh mục</h3><button class="btn btn-sm" @click="cat = { code: '', name: '', grp: 'TOY', image: '', sortOrder: cats.length + 1 }">+ Thêm</button></div>
        <table class="table"><thead><tr><th>Mã</th><th>Tên</th><th>Nhóm</th><th /></tr></thead><tbody>
          <tr v-for="c in cats" :key="c.id"><td><code>{{ c.code }}</code></td><td>{{ c.name }}</td><td class="small">{{ c.grp === 'TOY' ? 'Xe đồ chơi' : 'Phụ kiện' }}</td>
            <td><div class="row" style="flex-wrap:nowrap;gap:6px"><button class="btn btn-sm btn-outline" @click="cat = { ...c }">Sửa</button><button class="btn btn-sm btn-danger" @click="del('categories', c)">Xoá</button></div></td></tr>
        </tbody></table>
      </div>
      <div class="card">
        <div class="row between"><h3>Thương hiệu</h3><button class="btn btn-sm" @click="brand = { name: '', country: '', logoUrl: '', description: '' }">+ Thêm</button></div>
        <table class="table"><thead><tr><th /><th>Tên</th><th>Quốc gia</th><th /></tr></thead><tbody>
          <tr v-for="b in brands" :key="b.id"><td><img loading="lazy" decoding="async" :src="b.logoUrl" class="logo" alt="" /></td><td>{{ b.name }}</td><td class="small">{{ b.country }}</td>
            <td><div class="row" style="flex-wrap:nowrap;gap:6px"><button class="btn btn-sm btn-outline" @click="brand = { ...b }">Sửa</button><button class="btn btn-sm btn-danger" @click="del('brands', b)">Xoá</button></div></td></tr>
        </tbody></table>
      </div>
    </div>
    <div v-if="cat" class="modal-bg" @click.self="cat = null"><div class="modal">
      <h3>{{ cat.id ? 'Sửa' : 'Thêm' }} danh mục</h3>
      <div v-if="err" class="alert alert-error">{{ err }}</div>
      <div class="form-grid">
        <div class="field"><label>Mã</label><input v-model="cat.code" class="input" :disabled="!!cat.id" /></div>
        <div class="field"><label>Tên</label><input v-model="cat.name" class="input" /></div>
        <div class="field"><label>Nhóm</label><select v-model="cat.grp" class="input"><option value="TOY">Xe đồ chơi</option><option value="ACCESSORY">Phụ kiện</option></select></div>
        <div class="field"><label>Thứ tự</label><input v-model.number="cat.sortOrder" type="number" class="input" /></div>
      </div>
      <div class="field"><label>Ảnh</label><input v-model="cat.image" class="input" /></div>
      <div class="row" style="justify-content:flex-end"><button class="btn btn-outline" @click="cat = null">Huỷ</button><button class="btn" @click="save('categories', cat)">Lưu</button></div>
    </div></div>
    <div v-if="brand" class="modal-bg" @click.self="brand = null"><div class="modal">
      <h3>{{ brand.id ? 'Sửa' : 'Thêm' }} thương hiệu</h3>
      <div v-if="err" class="alert alert-error">{{ err }}</div>
      <div class="form-grid">
        <div class="field"><label>Tên</label><input v-model="brand.name" class="input" /></div>
        <div class="field"><label>Quốc gia</label><input v-model="brand.country" class="input" /></div>
      </div>
      <div class="field"><label>Logo</label><div class="row" style="gap:8px"><input v-model="brand.logoUrl" class="input" style="flex:1" /><label class="btn btn-sm btn-outline">Tải lên<input type="file" accept="image/*" hidden @change="up" /></label></div></div>
      <div class="field"><label>Mô tả</label><textarea v-model="brand.description" rows="2" class="input" /></div>
      <div class="row" style="justify-content:flex-end"><button class="btn btn-outline" @click="brand = null">Huỷ</button><button class="btn" @click="save('brands', brand)">Lưu</button></div>
    </div></div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg, uploadImage } from '../../api'
import { loadCatalog } from '../../format'
const cats = ref([]), brands = ref([]), cat = ref(null), brand = ref(null), err = ref('')
const load = async () => {
  const [a, b] = await Promise.all([api.get('/categories'), api.get('/brands')]); cats.value = a.data; brands.value = b.data
}
const up = async e => { try { brand.value.logoUrl = await uploadImage(e.target.files[0]) } catch (x) { err.value = errMsg(x) } }
const save = async (kind, x) => {
  err.value = ''
  try { x.id ? await api.put(`/admin/${kind}/${x.id}`, x) : await api.post(`/admin/${kind}`, x); cat.value = brand.value = null; load(); loadCatalog?.(true) } catch (e) { err.value = errMsg(e) }
}
const del = async (kind, x) => {
  if (!confirm(`Xoá “${x.name}”?`)) return
  try { await api.delete(`/admin/${kind}/${x.id}`); load() } catch (e) { alert(errMsg(e)) }
}
onMounted(load)
</script>
<style scoped>.two { grid-template-columns: 1fr 1fr; gap: 24px; align-items: start; } .logo { width: 48px; height: 28px; object-fit: contain; } h3 { margin: 0 0 12px; }</style>
