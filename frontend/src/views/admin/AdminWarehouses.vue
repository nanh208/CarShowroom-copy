<template>
  <div>
    <h1>Kho theo chi nhánh</h1>
    <p class="muted small">Tổng tồn khớp với tồn sản phẩm; bán hàng & nhập hàng điều chỉnh kho chính HCM. Điều chuyển giữa các kho không đổi tổng.</p>
    <div v-if="data" class="kp" v-stagger><div v-for="w in data.warehouses" :key="w.code" class="card"><small class="muted">{{ w.name }}</small><b><CountUp :value="data.totals[w.code]" /></b><small>sản phẩm tồn</small></div></div>
    <div class="card">
      <div class="row between th"><input v-model.trim="q" class="input" placeholder="Tìm sản phẩm / SKU…" aria-label="Tìm" style="max-width:260px" />
        <label class="small row" style="gap:6px"><input v-model="low" type="checkbox" /> Chỉ hiện kho có ≤ 2</label></div>
      <div v-if="!data" class="sk" style="height:240px" />
      <div v-else class="table-wrap"><table class="table"><thead><tr><th>Sản phẩm</th><th v-for="w in data.warehouses" :key="w.code">{{ w.code }}</th><th>Tổng</th><th /></tr></thead>
        <tbody><tr v-for="r in shown" :key="r.productId"><td><div class="pn"><img :src="r.image" alt="" /><div><b>{{ r.name }}</b><div class="small muted">{{ r.sku }}</div></div></div></td>
          <td v-for="w in data.warehouses" :key="w.code"><span :class="{ lw: r.by[w.code] <= 2 }">{{ r.by[w.code] }}</span></td><td><b>{{ r.total }}</b></td>
          <td><button class="btn btn-sm btn-outline" :disabled="!r.total" @click="tf = { r, from: 'HCM', to: 'HN', qty: 1 }">Điều chuyển</button></td></tr>
          <tr v-if="!shown.length"><td colspan="6" class="muted">Không có sản phẩm.</td></tr></tbody></table></div>
    </div>
    <div v-if="tf" class="modal-bg" @click.self="tf = null">
      <form class="modal" @submit.prevent="transfer">
        <h3>Điều chuyển: {{ tf.r.name }}</h3>
        <div class="form-grid"><div class="field"><label>Từ kho</label><select v-model="tf.from" class="input"><option v-for="w in data.warehouses" :key="w.code" :value="w.code">{{ w.code }} (còn {{ tf.r.by[w.code] }})</option></select></div>
          <div class="field"><label>Đến kho</label><select v-model="tf.to" class="input"><option v-for="w in data.warehouses.filter(w => w.code !== tf.from)" :key="w.code" :value="w.code">{{ w.code }}</option></select></div></div>
        <div class="field"><label>Số lượng</label><input v-model.number="tf.qty" type="number" min="1" :max="tf.r.by[tf.from]" class="input" required /></div>
        <p v-if="err" class="alert alert-error">{{ err }}</p>
        <div class="row" style="justify-content:flex-end;gap:8px"><button type="button" class="btn btn-outline" @click="tf = null">Huỷ</button><button class="btn" :disabled="tf.from === tf.to || tf.qty < 1 || tf.qty > tf.r.by[tf.from]">Điều chuyển</button></div>
      </form>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'
import CountUp from '../../components/CountUp.vue'
const data = ref(null), q = ref(''), low = ref(false), tf = ref(null), err = ref('')
const shown = computed(() => (data.value?.rows || []).filter(r => (!q.value || (r.name + r.sku).toLowerCase().includes(q.value.toLowerCase())) && (!low.value || Object.values(r.by).some(v => v <= 2))))
const load = async () => { data.value = (await api.get('/admin/warehouses')).data }
async function transfer() { err.value = ''; try { await api.post('/admin/warehouses/transfer', { productId: tf.value.r.productId, from: tf.value.from, to: tf.value.to, qty: tf.value.qty }); toast('Đã điều chuyển'); tf.value = null; load() } catch (e) { err.value = errMsg(e) } }
onMounted(load)
</script>
<style scoped>
.kp { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; margin: 14px 0; } .kp b { display: block; font: 700 1.6rem 'Oswald', sans-serif; }
.th { gap: 10px; flex-wrap: wrap; margin-bottom: 10px; } .pn { display: flex; gap: 8px; align-items: center; min-width: 200px; } .pn img { width: 44px; height: 34px; object-fit: cover; } .lw { color: var(--danger); font-weight: 700; }
</style>
