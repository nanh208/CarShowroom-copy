<template>
  <div>
    <h1>Hộp thư đi <small class="muted" style="font-size:.9rem">(email mô phỏng — không gửi thật)</small></h1>
    <div class="chips"><button v-for="k in kinds" :key="k" :class="['chip', { on: kind === k }]" @click="kind = k">{{ KIND[k] || 'Tất cả' }} <small>{{ k ? list.filter(m => m.kind === k).length : list.length }}</small></button></div>
    <div class="split">
      <div class="card ml">
        <button v-for="m in shown" :key="m.id" :class="['mi', { on: cur?.id === m.id }]" @click="cur = m">
          <span class="k">{{ KIND[m.kind] || m.kind }}</span><b>{{ m.subject }}</b><small class="muted">{{ m.toAddr }} · {{ ago(m.createdAt) }}</small></button>
        <p v-if="!shown.length" class="muted">Không có email.</p>
      </div>
      <div class="card mv">
        <template v-if="cur"><h3>{{ cur.subject }}</h3><p class="small muted">Tới: {{ cur.toAddr }} · {{ dt(cur.createdAt) }}</p><pre>{{ cur.body }}</pre></template>
        <p v-else class="muted">Chọn một email để xem nội dung.</p>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../../api'
import { dt, ago } from '../../format'
const KIND = { INVOICE: 'Hoá đơn', CART_REMINDER: 'Nhắc giỏ hàng', LOW_STOCK: 'Tồn kho thấp', BACK_IN_STOCK: 'Có hàng lại', GIFT_CARD: 'Thẻ quà', REFUND: 'Hoàn tiền' }
const list = ref([]), kind = ref(''), cur = ref(null)
const kinds = computed(() => ['', ...new Set(list.value.map(m => m.kind))])
const shown = computed(() => list.value.filter(m => !kind.value || m.kind === kind.value))
onMounted(async () => { list.value = (await api.get('/admin/emails')).data; cur.value = list.value[0] || null })
</script>
<style scoped>
.chips { display: flex; gap: 6px; flex-wrap: wrap; margin: 12px 0; } .chip { border: 1px solid var(--border); background: var(--surface); padding: 6px 12px; font: 600 .8rem inherit; font-family: inherit; cursor: pointer; color: var(--text); } .chip.on { background: #111; color: #fff; border-color: #111; }
.split { display: grid; grid-template-columns: minmax(260px, 380px) 1fr; gap: 16px; align-items: start; } .split > * { min-width: 0; }
.ml { max-height: 70vh; overflow-y: auto; padding: 6px; } .mi { display: flex; flex-direction: column; gap: 2px; width: 100%; text-align: left; background: none; border: none; border-bottom: 1px solid var(--border); padding: 10px; cursor: pointer; font-family: inherit; color: var(--text); }
.mi.on { background: var(--bronze-soft); } .mi b { font-size: .86rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .k { font: 600 .65rem 'Oswald', sans-serif; letter-spacing: .1em; text-transform: uppercase; color: var(--accent); }
.mv pre { white-space: pre-wrap; font: .9rem/1.6 inherit; font-family: inherit; background: var(--bg-2); padding: 14px; margin-top: 10px; }
@media (max-width: 899px) { .split { grid-template-columns: 1fr; } .ml { max-height: 40vh; } }
</style>
