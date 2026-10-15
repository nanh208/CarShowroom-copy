<template>
  <div>
    <div class="row between"><h1>Bộ sưu tập</h1><button class="btn" @click="edit()">+ Bộ sưu tập</button></div>
    <div v-if="loading" class="cg"><div v-for="i in 3" :key="i" class="sk" style="height:220px" /></div>
    <div v-else class="cg" v-stagger>
      <div v-for="c in list" :key="c.id" :class="['card cc', { off: !c.active }]">
        <div class="im" :style="{ borderColor: c.accent || 'var(--primary)' }"><img v-if="c.cover" :src="c.cover" alt="" loading="lazy" /></div>
        <b>{{ c.title }}</b><small class="muted">/collections/{{ c.slug }} · {{ c.productIds.length }} sp · #{{ c.sortOrder }}</small>
        <div class="row" style="gap:6px;margin-top:8px"><button class="btn btn-sm btn-outline" @click="edit(c)">Sửa</button><a :href="`/collections/${c.slug}`" target="_blank" class="btn btn-sm btn-outline">Xem</a><button class="btn btn-sm btn-danger" @click="del(c)">Xoá</button></div>
      </div>
      <p v-if="!list.length" class="muted">Chưa có bộ sưu tập.</p>
    </div>
    <div v-if="f" class="modal-bg" @click.self="f = null">
      <form class="modal" @submit.prevent="save">
        <h3>{{ f.id ? 'Sửa bộ sưu tập' : 'Bộ sưu tập mới' }}</h3>
        <div class="form-grid"><div class="field"><label>Tên *</label><input v-model.trim="f.title" class="input" required /></div><div class="field"><label>Slug</label><input v-model.trim="f.slug" class="input" placeholder="tự tạo từ tên" /></div>
          <div class="field"><label>Dòng phụ</label><input v-model="f.subtitle" class="input" /></div><div class="field"><label>Màu nhấn</label><input v-model="f.accent" type="color" class="input" style="height:40px;padding:2px" /></div></div>
        <div class="field"><label>Mô tả</label><textarea v-model="f.description" class="input" rows="3" maxlength="3000" /></div>
        <div class="field"><label>Ảnh bìa</label><div class="row" style="gap:8px"><input v-model="f.cover" class="input" style="flex:1" /><label class="btn btn-sm btn-outline">Tải lên<input type="file" accept="image/*" hidden @change="up" /></label></div></div>
        <div class="field"><label>Sản phẩm ({{ f.productIds.length }}) — chạm để thêm/bỏ, thứ tự theo lúc chọn</label><input v-model="pq" class="input" placeholder="Lọc sản phẩm…" />
          <div class="pl"><label v-for="p in products.filter(p => !pq || p.name.toLowerCase().includes(pq.toLowerCase()))" :key="p.id"><input v-model="f.productIds" type="checkbox" :value="p.id" /> {{ p.name }}<small v-if="f.productIds.includes(p.id)" class="ord">#{{ f.productIds.indexOf(p.id) + 1 }}</small></label></div></div>
        <div class="row" style="gap:12px"><label class="check"><input v-model="f.active" type="checkbox" /> Hiển thị</label><label class="small">Thứ tự <input v-model.number="f.sortOrder" type="number" class="input" style="width:80px" /></label></div>
        <p v-if="err" class="alert alert-error">{{ err }}</p>
        <div class="row" style="justify-content:flex-end;gap:8px;margin-top:10px"><button type="button" class="btn btn-outline" @click="f = null">Huỷ</button><button class="btn">Lưu</button></div>
      </form>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg, uploadImage } from '../../api'
import { toast } from '../../stores/prefs'
const list = ref([]), products = ref([]), f = ref(null), err = ref(''), pq = ref(''), loading = ref(true)
const load = async () => { list.value = (await api.get('/admin/collections')).data; loading.value = false }
const edit = c => { err.value = ''; f.value = c ? { ...c, productIds: [...c.productIds] } : { title: '', slug: '', subtitle: '', description: '', cover: '', accent: '#ff6a00', productIds: [], active: true, sortOrder: list.value.length } }
const up = async e => { try { f.value.cover = await uploadImage(e.target.files[0]) } catch (x) { err.value = errMsg(x) } }
async function save() { if (!f.value.cover && f.value.productIds.length) f.value.cover = products.value.find(p => p.id === f.value.productIds[0])?.images?.[0]
  try { f.value.id ? await api.put(`/admin/collections/${f.value.id}`, f.value) : await api.post('/admin/collections', f.value); f.value = null; toast('Đã lưu'); load() } catch (e) { err.value = errMsg(e) } }
async function del(c) { if (!confirm('Xoá bộ sưu tập ' + c.title + '?')) return; await api.delete(`/admin/collections/${c.id}`); load() }
onMounted(async () => { products.value = (await api.get('/admin/products', { params: { size: 500 } })).data.content; load() })
</script>
<style scoped>
.cg { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr)); gap: 16px; margin-top: 14px; }
.cc { display: flex; flex-direction: column; gap: 4px; } .cc.off { opacity: .5; } .im { aspect-ratio: 16/9; background: #111; border-left: 5px solid; margin-bottom: 6px; overflow: hidden; } .im img { width: 100%; height: 100%; object-fit: cover; }
.pl { max-height: 220px; overflow-y: auto; border: 1px solid var(--border); padding: 6px; margin-top: 6px; } .pl label { display: flex; gap: 6px; align-items: center; padding: 3px 0; font-size: .86rem; } .ord { margin-left: auto; color: var(--primary); font-weight: 700; }
</style>
