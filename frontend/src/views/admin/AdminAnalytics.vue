<template>
  <div>
    <div class="row between hd"><h1>Doanh số theo {{ dim === 'brand' ? 'thương hiệu' : 'danh mục' }}</h1>
      <div class="row ctl"><div class="seg" role="tablist"><button role="tab" :aria-selected="dim === 'category'" :class="{ on: dim === 'category' }" @click="dim = 'category'">Danh mục</button><button role="tab" :aria-selected="dim === 'brand'" :class="{ on: dim === 'brand' }" @click="dim = 'brand'">Thương hiệu</button></div>
        <label class="small">Từ <input v-model="from" type="date" class="input" /></label><label class="small">Đến <input v-model="to" type="date" class="input" /></label>
        <button class="btn btn-sm btn-outline" @click="csv">⤓ CSV</button></div></div>
    <div class="kp"><div class="card"><small class="muted">Doanh thu hàng hoá</small><b>{{ vnd(sum) }}</b></div><div class="card"><small class="muted">Sản phẩm bán ra</small><b>{{ rows.reduce((a, r) => a + r.qty, 0) }}</b></div><div class="card"><small class="muted">Nhóm dẫn đầu</small><b>{{ rows[0]?.name || '—' }}</b></div></div>
    <div class="card">
      <div v-if="!rows.length" class="muted">Không có dữ liệu trong khoảng này.</div>
      <div v-for="(r, i) in rows" :key="r.name" class="br">
        <span class="nm"><i :style="{ background: COLORS[i % COLORS.length] }" />{{ r.name }}</span>
        <div class="bar"><div :style="{ width: (rows[0].revenue ? r.revenue * 100 / rows[0].revenue : 0) + '%', background: COLORS[i % COLORS.length] }" /></div>
        <b>{{ vnd(r.revenue) }}</b><small class="muted">{{ r.share }}% · {{ r.qty }} sp</small>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import api from '../../api'
import { vnd } from '../../format'
const COLORS = ['#ff6a00', '#111', '#a87a3c', '#2e7d32', '#1565c0', '#8e24aa', '#c62828', '#00838f', '#6d4c41']
const iso = d => new Date(d.getTime() - d.getTimezoneOffset() * 6e4).toISOString().slice(0, 10)
const dim = ref('category'), from = ref(iso(new Date(Date.now() - 89 * 864e5))), to = ref(iso(new Date())), rows = ref([])
const sum = computed(() => rows.value.reduce((a, r) => a + r.revenue, 0))
const load = async () => { rows.value = (await api.get('/admin/analytics/sales-by', { params: { dim: dim.value, from: from.value, to: to.value } })).data }
watch([dim, from, to], load); onMounted(load)
function csv() {
  const l = [['Nhóm', 'Doanh thu', 'Số lượng', 'Tỉ trọng %'], ...rows.value.map(r => [r.name, r.revenue, r.qty, r.share])]
  const b = new Blob(['\ufeff' + l.map(r => r.map(v => `"${v}"`).join(',')).join('\r\n')], { type: 'text/csv' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(b); a.download = `doanh-so-${dim.value}-${from.value}_${to.value}.csv`; a.click()
}
</script>
<style scoped>
.hd { gap: 12px; flex-wrap: wrap; margin-bottom: 14px; } .ctl { gap: 8px; flex-wrap: wrap; align-items: center; } .ctl label { display: flex; gap: 6px; align-items: center; margin: 0; } .ctl .input { width: auto; padding: 6px 8px; }
.seg { display: flex; border: 1px solid var(--border); } .seg button { border: none; background: none; padding: 7px 12px; font: 600 .82rem inherit; font-family: inherit; cursor: pointer; color: var(--muted); } .seg button.on { background: #111; color: #fff; }
.kp { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; margin-bottom: 16px; } .kp b { display: block; font-size: 1.3rem; margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.br { display: grid; grid-template-columns: minmax(110px, 180px) 1fr auto auto; gap: 12px; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--border); font-size: .88rem; }
.nm { display: flex; gap: 8px; align-items: center; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .nm i { width: 10px; height: 10px; flex-shrink: 0; }
.bar { height: 14px; background: var(--bg-2); } .bar div { height: 100%; transition: width .4s ease-out; }
@media (max-width: 599px) { .br { grid-template-columns: 1fr auto; } .bar { grid-column: 1 / -1; order: 3; } .br small { display: none; } }
@media (prefers-reduced-motion: reduce) { .bar div { transition: none; } }
</style>
