<template>
  <div>
    <div class="row between"><h1>Banner trang chủ</h1><button class="btn" @click="edit()">+ Thêm banner</button></div>
    <div class="grid bgrid">
      <div v-for="b in list" :key="b.id" class="card bcard" :class="{ off: !b.active }">
        <div class="img"><img :src="b.image" alt="" /><span class="pos">{{ b.position }} · #{{ b.sortOrder }}</span></div>
        <div class="small muted">{{ b.kicker }}</div><b>{{ b.title }}</b><div class="small">{{ b.subtitle }}</div>
        <div class="small muted">→ {{ b.link }}</div>
        <div class="row" style="gap:6px;margin-top:8px"><button class="btn btn-sm btn-outline" @click="edit(b)">Sửa</button><button class="btn btn-sm btn-danger" @click="remove(b)">Xoá</button></div>
      </div>
    </div>
    <div v-if="form" class="modal-bg" @click.self="form = null">
      <div class="modal">
        <h3>{{ form.id ? 'Sửa banner' : 'Thêm banner' }}</h3>
        <div v-if="err" class="alert alert-error">{{ err }}</div>
        <div class="form-grid">
          <div class="field"><label>Vị trí</label><select v-model="form.position" class="input"><option>HERO</option><option>PROMO</option></select></div>
          <div class="field"><label>Thứ tự</label><input v-model.number="form.sortOrder" type="number" class="input" /></div>
          <div class="field"><label>Dòng nhỏ</label><input v-model="form.kicker" class="input" /></div>
          <div class="field"><label>Tiêu đề</label><input v-model="form.title" class="input" /></div>
        </div>
        <div class="field"><label>Mô tả</label><input v-model="form.subtitle" class="input" /></div>
        <div class="field"><label>Liên kết</label><input v-model="form.link" class="input" placeholder="/products?type=RC" /></div>
        <div class="field"><label>Ảnh</label><div class="row" style="gap:8px"><input v-model="form.image" class="input" style="flex:1" /><label class="btn btn-sm btn-outline">Tải lên<input type="file" accept="image/*" hidden @change="up" /></label></div></div>
        <img v-if="form.image" :src="form.image" class="prev" />
        <label class="check"><input v-model="form.active" type="checkbox" /> Hiển thị</label>
        <div class="row" style="justify-content:flex-end;margin-top:16px"><button class="btn btn-outline" @click="form = null">Huỷ</button><button class="btn" @click="save">Lưu</button></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg, uploadImage } from '../../api'
const list = ref([]), form = ref(null), err = ref('')
const load = async () => { list.value = (await api.get('/admin/banners')).data }
const edit = b => { err.value = ''; form.value = b ? { ...b } : { position: 'HERO', sortOrder: list.value.length + 1, title: '', kicker: '', subtitle: '', image: '', link: '/products', active: true } }
const up = async e => { try { form.value.image = await uploadImage(e.target.files[0]) } catch (x) { err.value = errMsg(x) } }
const save = async () => { const f = form.value; try { f.id ? await api.put(`/admin/banners/${f.id}`, f) : await api.post('/admin/banners', f); form.value = null; load() } catch (e) { err.value = errMsg(e) } }
const remove = async b => { if (confirm('Xoá banner?')) { await api.delete(`/admin/banners/${b.id}`); load() } }
onMounted(load)
</script>
<style scoped>
.bgrid { grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.bcard { display: flex; flex-direction: column; gap: 4px; } .bcard.off { opacity: .5; }
.img { position: relative; aspect-ratio: 16/8; border-radius: 10px; overflow: hidden; margin-bottom: 8px; } .img img { width: 100%; height: 100%; object-fit: cover; }
.pos { position: absolute; left: 8px; top: 8px; background: rgba(255,255,255,.9); font-size: .72rem; padding: 2px 8px; border-radius: 99px; }
.prev { width: 100%; max-height: 160px; object-fit: cover; border-radius: 8px; margin-bottom: 10px; }
</style>
