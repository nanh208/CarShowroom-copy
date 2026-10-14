<template>
  <div>
    <h1>Kho hàng</h1>
    <div class="grid two">
      <div class="card">
        <h3>Điều chỉnh tồn kho</h3>
        <div v-if="err" class="alert alert-error">{{ err }}</div>
        <div v-if="ok" class="alert alert-success">{{ ok }}</div>
        <div class="field"><label>Sản phẩm</label>
          <select v-model="f.productId" class="input"><option v-for="p in products" :key="p.id" :value="p.id">{{ p.sku }} · {{ p.name }} (tồn {{ p.stock }})</option></select></div>
        <div v-if="vars.length" class="field"><label>Biến thể</label>
          <select v-model="f.variantId" class="input"><option v-for="v in vars" :key="v.id" :value="v.id">{{ v.name }} (tồn {{ v.stock }})</option></select></div>
        <div class="form-grid">
          <div class="field"><label>Số lượng (+ nhập / − xuất)</label><input v-model.number="f.delta" type="number" class="input" /></div>
          <div class="field"><label>Lý do</label><select v-model="f.reason" class="input"><option>Nhập hàng</option><option>Kiểm kê</option><option>Hàng lỗi / hỏng</option><option>Điều chỉnh kho</option></select></div>
        </div>
        <button class="btn" :disabled="!f.productId || !f.delta" @click="save">Lưu điều chỉnh</button>
      </div>
      <div class="card">
        <h3>Sắp hết hàng (≤ 5)</h3>
        <table class="table"><tbody>
          <tr v-for="p in low" :key="p.id"><td>{{ p.name }}<div class="small muted">{{ p.sku }}</div></td>
            <td><span :class="['badge', p.stock ? 'badge-PENDING' : 'badge-CANCELLED']">{{ p.stock }}</span></td>
            <td><button class="btn btn-sm btn-outline" @click="pick(p)">Nhập</button></td></tr>
          <tr v-if="!low.length"><td class="muted">Không có sản phẩm nào sắp hết.</td></tr>
        </tbody></table>
      </div>
    </div>
    <div class="card table-wrap" style="margin-top:24px">
      <h3>Lịch sử xuất nhập</h3>
      <table class="table">
        <thead><tr><th>Thời gian</th><th>Sản phẩm</th><th>Thay đổi</th><th>Tồn sau</th><th>Lý do</th><th>Người thực hiện</th></tr></thead>
        <tbody><tr v-for="m in moves" :key="m.id">
          <td class="small">{{ dt(m.createdAt) }}</td><td>{{ m.productName }}</td>
          <td :style="{ color: m.delta > 0 ? 'var(--success, #2e7d32)' : 'var(--danger)', fontWeight: 700 }">{{ m.delta > 0 ? '+' : '' }}{{ m.delta }}</td>
          <td>{{ m.after }}</td><td class="small">{{ m.reason }}</td><td class="small muted">{{ m.byUser }}</td></tr></tbody>
      </table>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api, { errMsg } from '../../api'
const products = ref([]), allVars = ref([]), moves = ref([]), err = ref(''), ok = ref('')
const f = ref({ productId: null, variantId: null, delta: 10, reason: 'Nhập hàng' })
const vars = computed(() => allVars.value.filter(v => v.productId === f.value.productId))
const low = computed(() => products.value.filter(p => p.active !== false && p.stock <= 5).sort((a, b) => a.stock - b.stock))
watch(() => f.value.productId, () => { f.value.variantId = vars.value[0]?.id || null })
const dt = s => s ? new Date(s).toLocaleString('vi-VN') : ''
const pick = p => { f.value.productId = p.id; f.value.delta = 10; f.value.reason = 'Nhập hàng'; window.scrollTo({ top: 0, behavior: 'smooth' }) }
const load = async () => {
  const [a, b, c] = await Promise.all([api.get('/admin/products', { params: { size: 500 } }), api.get('/admin/inventory/variants'), api.get('/admin/inventory/movements')])
  products.value = a.data.content || a.data; allVars.value = b.data; moves.value = c.data
}
const save = async () => {
  err.value = ok.value = ''
  try { const r = (await api.post('/admin/inventory/adjust', f.value)).data; ok.value = `Đã cập nhật. Tồn mới: ${r.stock}`; load() } catch (e) { err.value = errMsg(e) }
}
onMounted(load)
</script>
<style scoped>.two { grid-template-columns: 1.2fr 1fr; gap: 24px; } h3 { margin-bottom: 12px; }</style>
