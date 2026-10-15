<template>
  <div>
    <h1>Combo & thẻ quà tặng</h1>
    <div class="grid2">
      <div class="card">
        <div class="row between"><h3>Combo / Mua X tặng Y</h3><button class="btn btn-sm" @click="edit()">+ Combo mới</button></div>
        <div v-for="b in bundles" :key="b.id" class="bi">
          <div><b>{{ b.title }}</b> <span :class="['badge', b.active ? 'badge-COMPLETED' : 'badge-CANCELLED']">{{ b.active ? 'Đang bật' : 'Tắt' }}</span>
            <div class="small muted">{{ b.kind === 'FIXED' ? 'Giá combo ' + vnd(b.bundlePrice) : `Mua ${b.buyQty} tặng ${b.getQty}` }} · {{ b.productIds.length }} sản phẩm</div></div>
          <div class="row" style="gap:6px"><button class="btn btn-sm btn-outline" @click="edit(b)">Sửa</button><button class="btn btn-sm btn-outline" @click="del(b)">Xoá</button></div>
        </div>
        <p v-if="!bundles.length" class="muted">Chưa có combo.</p>
      </div>
      <div class="card">
        <h3>Phát hành thẻ quà tặng</h3>
        <div class="row" style="gap:8px;flex-wrap:wrap;margin:8px 0"><input v-model.number="gc.amount" type="number" step="50000" min="50000" class="input" style="width:150px" aria-label="Mệnh giá" /><input v-model.trim="gc.message" class="input" style="flex:1;min-width:160px" placeholder="Lời nhắn / lý do" aria-label="Lời nhắn" /><button class="btn btn-sm" @click="issue">Phát hành</button></div>
        <div class="table-wrap"><table class="table"><thead><tr><th>Mã</th><th>Mệnh giá</th><th>Còn lại</th><th>Trạng thái</th></tr></thead>
          <tbody><tr v-for="g in cards" :key="g.id"><td><code>{{ g.code }}</code></td><td>{{ vnd(g.initial) }}</td><td>{{ vnd(g.balance) }}</td><td class="small">{{ g.redeemedByUserId ? 'Đã nạp ví' : g.balance ? 'Khả dụng' : 'Đã dùng hết' }}</td></tr></tbody></table></div>
      </div>
    </div>
    <div v-if="f" class="modal-back" @click.self="f = null">
      <div class="modal card" role="dialog" aria-label="Combo">
        <h3>{{ f.id ? 'Sửa combo' : 'Combo mới' }}</h3>
        <label>Tiêu đề<input v-model="f.title" class="input" /></label>
        <label>Mô tả<input v-model="f.description" class="input" /></label>
        <div class="row" style="gap:8px"><label style="flex:1">Loại<select v-model="f.kind" class="input"><option value="BUY_X_GET_Y">Mua X tặng Y</option><option value="FIXED">Giá combo cố định</option></select></label>
          <template v-if="f.kind === 'FIXED'"><label style="flex:1">Giá combo<input v-model.number="f.bundlePrice" type="number" class="input" /></label></template>
          <template v-else><label>Mua<input v-model.number="f.buyQty" type="number" min="1" class="input" style="width:70px" /></label><label>Tặng<input v-model.number="f.getQty" type="number" min="1" class="input" style="width:70px" /></label></template></div>
        <label>Sản phẩm ({{ f.productIds.length }})<input v-model="pq" class="input" placeholder="Lọc sản phẩm…" /></label>
        <div class="pl"><label v-for="p in products.filter(p => !pq || p.name.toLowerCase().includes(pq.toLowerCase()))" :key="p.id" class="pi"><input v-model="f.productIds" type="checkbox" :value="p.id" /> {{ p.name }} <small class="muted">{{ vnd(p.price) }}</small></label></div>
        <label class="row" style="gap:6px"><input v-model="f.active" type="checkbox" /> Đang bật</label>
        <div class="row" style="gap:8px;justify-content:flex-end"><button class="btn btn-outline btn-sm" @click="f = null">Huỷ</button><button class="btn btn-sm" @click="save">Lưu</button></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd } from '../../format'
import { toast } from '../../stores/prefs'
const bundles = ref([]), cards = ref([]), products = ref([]), f = ref(null), pq = ref(''), gc = ref({ amount: 200000, message: '' })
const load = async () => { bundles.value = (await api.get('/admin/bundles')).data; cards.value = (await api.get('/admin/giftcards')).data }
const edit = b => { f.value = b ? { ...b, productIds: [...b.productIds] } : { title: '', description: '', kind: 'BUY_X_GET_Y', buyQty: 2, getQty: 1, bundlePrice: 0, productIds: [], active: true, sortOrder: 0 } }
async function save() { try { f.value.id ? await api.put(`/admin/bundles/${f.value.id}`, f.value) : await api.post('/admin/bundles', f.value); f.value = null; toast('Đã lưu combo'); load() } catch (e) { toast(errMsg(e), 'error') } }
async function del(b) { if (!confirm('Xoá combo ' + b.title + '?')) return; await api.delete(`/admin/bundles/${b.id}`); load() }
async function issue() { try { const r = (await api.post('/admin/giftcards', gc.value)).data; toast('Đã phát hành ' + r.code); load() } catch (e) { toast(errMsg(e), 'error') } }
onMounted(async () => { products.value = (await api.get('/admin/products', { params: { size: 500 } })).data.content; load() })
</script>
<style scoped>
.grid2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 380px), 1fr)); gap: 16px; margin-top: 12px; } .grid2 > * { min-width: 0; }
.bi { display: flex; justify-content: space-between; gap: 10px; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
.modal-back { position: fixed; inset: 0; background: rgba(0,0,0,.45); display: grid; place-items: center; z-index: 200; padding: 12px; }
.modal { width: min(560px, 100%); max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; } .modal label { display: flex; flex-direction: column; gap: 4px; font-size: .85rem; }
.pl { max-height: 200px; overflow-y: auto; border: 1px solid var(--border); padding: 6px; } .pl .pi { flex-direction: row; align-items: center; gap: 6px; padding: 3px 0; }
</style>
