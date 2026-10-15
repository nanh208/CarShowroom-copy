<template>
  <div class="slips">
    <div class="bar no-print"><b>{{ orders.length }} phiếu đóng gói</b> <span class="muted">· khổ A4</span>
      <button class="btn btn-sm" @click="print">In phiếu</button><router-link to="/admin/orders" class="btn btn-sm btn-outline">← Đơn hàng</router-link></div>
    <div v-if="!orders.length" class="muted no-print" style="padding:24px">Không có đơn nào được chọn.</div>
    <section v-for="o in orders" :key="o.id" class="slip">
      <header><div><b class="br">ARES TOYS</b><small>Phiếu đóng gói · Packing slip</small></div><div class="r"><b>{{ o.code }}</b><small>Đặt ngày {{ d(o.createdAt) }}</small><small v-if="o.shippingMethod">{{ o.shippingMethod === 'EXPRESS' ? 'GIAO NHANH' : 'Giao tiêu chuẩn' }}</small></div></header>
      <div class="to"><small>GIAO TỚI</small><b>{{ o.buyerName }}</b> · {{ o.phone }}<p>{{ o.shippingAddress }}</p></div>
      <table><thead><tr><th>✓</th><th>Sản phẩm</th><th>Phiên bản</th><th>SL</th></tr></thead>
        <tbody><tr v-for="i in o.items" :key="i.id"><td class="ck">☐</td><td>{{ i.productName }}</td><td>{{ i.variantName || '—' }}</td><td><b>{{ i.quantity }}</b></td></tr></tbody></table>
      <p class="tot">Tổng số sản phẩm: <b>{{ o.items.reduce((a, i) => a + i.quantity, 0) }}</b></p>
      <p v-if="o.giftWrap" class="gift">🎁 GÓI QUÀ — KHÔNG KÈM HOÁ ĐƠN GIÁ{{ o.giftMessage ? ' · Thiệp: “' + o.giftMessage + '”' : '' }}</p>
      <p v-if="o.note" class="small">Ghi chú khách: {{ o.note }}</p>
      <footer><span>Người đóng gói: ____________</span><span>Kiểm tra: ____________</span></footer>
    </section>
  </div>
</template>
<script setup>
// Phiếu đóng gói: /admin/packing?ids=1,2&auto=1
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../api'
import { d } from '../../format'
const route = useRoute(), orders = ref([]), print = () => window.print()
onMounted(async () => {
  const ids = String(route.query.ids || '').split(',').map(Number).filter(Boolean)
  orders.value = (await api.get('/admin/orders')).data.filter(o => ids.includes(o.id))
  if (route.query.auto && orders.value.length) setTimeout(print, 300)
})
</script>
<style scoped>
.slips { background: #eee; min-height: 100vh; padding: 16px; color: #111; } .bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
.slip { background: #fff; max-width: 210mm; margin: 0 auto 16px; padding: 14mm; box-sizing: border-box; font-size: 13px; }
header { display: flex; justify-content: space-between; gap: 12px; border-bottom: 2px solid #111; padding-bottom: 10px; } header small { display: block; color: #555; } .br { font: 700 1.4rem 'Oswald', sans-serif; letter-spacing: .08em; } .r { text-align: right; }
.to { margin: 14px 0; } .to small { display: block; font-weight: 700; color: #555; letter-spacing: .1em; } .to p { margin: 2px 0 0; }
table { width: 100%; border-collapse: collapse; } th, td { border: 1px solid #ccc; padding: 7px; text-align: left; } th { background: #f3f3f3; } .ck { width: 28px; text-align: center; font-size: 1.1rem; }
.tot { text-align: right; } .gift { border: 1px dashed #111; padding: 8px; font-weight: 700; } footer { display: flex; justify-content: space-between; gap: 12px; margin-top: 30px; flex-wrap: wrap; }
@media print { .no-print { display: none !important; } .slips { background: #fff; padding: 0; } .slip { margin: 0; page-break-after: always; } }
</style>
