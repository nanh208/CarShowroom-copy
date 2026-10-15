<template>
  <div>
    <h1>Phân khúc khách hàng</h1>
    <p class="muted small">Tự động theo hành vi mua: chi tiêu (đơn hoàn tất), số đơn và lần mua gần nhất.</p>
    <div v-if="!data" class="spinner" />
    <template v-else>
      <div class="segs">
        <button v-for="(m, k) in META" :key="k" :class="['seg card', { on: cur === k }]" :style="{ '--c': m.c }" @click="cur = k">
          <span class="dot" /><b>{{ m.l }}</b><strong>{{ data[k].count }}</strong><small class="muted">{{ m.d }}</small><small>{{ vnd(data[k].spent) }}</small>
        </button>
      </div>
      <div class="card">
        <div class="row between th"><h3>{{ META[cur].l }} · {{ data[cur].count }} khách</h3>
          <div class="row" style="gap:8px"><span class="small muted">Gợi ý: {{ META[cur].tip }}</span><button class="btn btn-sm btn-outline" @click="csv">⤓ Xuất CSV</button></div></div>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Khách</th><th>Hạng</th><th>Đơn</th><th>Chi tiêu</th><th>Đơn gần nhất</th></tr></thead>
            <tbody><tr v-for="m in data[cur].members" :key="m.id"><td><b>{{ m.name }}</b><div class="small muted">{{ m.email }}</div></td><td>{{ m.tier }}</td><td>{{ m.orders }}</td><td>{{ vnd(m.spent) }}</td><td class="small">{{ m.lastOrder ? ago(m.lastOrder) : '—' }}</td></tr>
              <tr v-if="!data[cur].members.length"><td colspan="5" class="muted">Không có khách trong nhóm này.</td></tr></tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'
import { vnd, ago } from '../../format'
const META = { VIP: { l: 'VIP', d: 'Chi ≥ 10 triệu', c: '#c9a227', tip: 'quà tri ân, ưu tiên hàng hiếm' }, LOYAL: { l: 'Trung thành', d: '≥ 3 đơn', c: '#2e7d32', tip: 'mời tham gia đấu giá' },
  NEW: { l: 'Khách mới', d: 'Mua gần đây', c: '#1565c0', tip: 'gửi combo phụ kiện' }, AT_RISK: { l: 'Có nguy cơ rời bỏ', d: '> 60 ngày chưa mua', c: '#c62828', tip: 'voucher quay lại' },
  NO_ORDER: { l: 'Chưa mua', d: 'Đã đăng ký', c: '#757575', tip: 'mã CHAOBAN đơn đầu' } }
const data = ref(null), cur = ref('VIP')
onMounted(async () => { data.value = (await api.get('/admin/segments')).data; cur.value = Object.keys(META).find(k => data.value[k].count) || 'VIP' })
function csv() {
  const rows = [['Tên', 'Email', 'Hạng', 'Số đơn', 'Chi tiêu', 'Đơn gần nhất'], ...data.value[cur.value].members.map(m => [m.name, m.email, m.tier, m.orders, m.spent, m.lastOrder || ''])]
  const b = new Blob(['\ufeff' + rows.map(r => r.map(v => `"${String(v ?? '').replace(/"/g, '""')}"`).join(',')).join('\r\n')], { type: 'text/csv' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(b); a.download = `phan-khuc-${cur.value.toLowerCase()}.csv`; a.click()
}
</script>
<style scoped>
.segs { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; margin: 16px 0; }
.seg { text-align: left; display: flex; flex-direction: column; gap: 2px; cursor: pointer; border: 1.5px solid var(--border); font-family: inherit; color: var(--text); position: relative; }
.seg.on { border-color: var(--c); box-shadow: inset 0 -3px 0 var(--c); } .seg strong { font-size: 1.6rem; } .dot { width: 10px; height: 10px; border-radius: 50%; background: var(--c); position: absolute; right: 12px; top: 14px; }
.th { gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
</style>
