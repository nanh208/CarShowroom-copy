<template>
  <div class="labels-page">
    <div class="bar no-print">
      <b>{{ orders.length }} nhãn vận chuyển</b> <span class="muted">· khổ A6 (105×148mm), 1 nhãn/trang</span>
      <button class="btn btn-sm" @click="print">In nhãn</button><router-link to="/admin/orders" class="btn btn-sm btn-outline">← Đơn hàng</router-link>
    </div>
    <div v-if="!orders.length" class="muted no-print" style="padding:24px">Không có đơn nào được chọn.</div>
    <section v-for="o in orders" :key="o.id" class="label">
      <header><div class="brand"><b>ARES</b> TOYS<small>Kho HCM · 1900 6868</small></div><div class="code"><svg :viewBox="`0 0 ${bars(o.code).w} 40`" preserveAspectRatio="none"><rect v-for="(b, i) in bars(o.code).r" :key="i" :x="b[0]" y="0" :width="b[1]" height="40" /></svg><span>{{ o.code }}</span></div></header>
      <div class="to"><small>NGƯỜI NHẬN</small><b>{{ o.buyerName }}</b><span>{{ o.phone }}</span><p>{{ o.shippingAddress }}</p></div>
      <div class="meta">
        <div><small>THU HỘ (COD)</small><b class="cod">{{ o.paymentMethod === 'COD' && o.paymentStatus !== 'PAID' ? vnd(o.total - (o.paidAmount || 0)) : '0đ' }}</b></div>
        <div><small>SỐ KIỆN</small><b>{{ o.items.reduce((a, i) => a + i.quantity, 0) }} sp</b></div>
        <div><small>NGÀY</small><b>{{ d(o.createdAt) }}</b></div>
      </div>
      <ul class="items"><li v-for="i in o.items" :key="i.id">{{ i.quantity }} × {{ i.productName }}<span v-if="i.variantName"> ({{ i.variantName }})</span></li></ul>
      <p v-if="o.giftWrap" class="gift">GÓI QUÀ{{ o.giftMessage ? ' · Thiệp: “' + o.giftMessage + '”' : '' }}</p>
      <p v-if="o.note" class="note">Ghi chú: {{ o.note }}</p>
      <footer>Cho xem hàng, không cho thử · Hàng dễ vỡ, nhẹ tay</footer>
    </section>
  </div>
</template>
<script setup>
// Nhãn vận chuyển in hàng loạt: /admin/labels?ids=1,2,3
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../api'
import { vnd, d } from '../../format'
const route = useRoute(), orders = ref([])
// Mã vạch minh hoạ (Code-39-like) sinh từ mã đơn
const bars = code => { const r = []; let x = 0; for (const ch of '*' + code + '*') { let c = ch.charCodeAt(0); for (let k = 0; k < 5; k++) { const w = (c >> k) & 1 ? 3 : 1; r.push([x, w]); x += w + ((c >> (k + 2)) & 1 ? 2 : 1) } x += 2 } return { r, w: x } }
const print = () => window.print()
onMounted(async () => {
  const ids = String(route.query.ids || '').split(',').map(Number).filter(Boolean)
  const all = (await api.get('/admin/orders')).data
  orders.value = all.filter(o => ids.includes(o.id))
  if (route.query.auto && orders.value.length) setTimeout(print, 300)
})
</script>
<style scoped>
.labels-page { background: #e8e8e8; min-height: 100vh; padding: 70px 12px 24px; display: flex; flex-wrap: wrap; gap: 16px; justify-content: center; align-content: flex-start; color: #000; }
.bar { position: fixed; top: 0; left: 0; right: 0; z-index: 5; display: flex; gap: 12px; align-items: center; padding: 10px 16px; background: #fff; border-bottom: 1px solid #ccc; flex-wrap: wrap; }
.label { width: 105mm; min-height: 148mm; background: #fff; padding: 5mm; display: flex; flex-direction: column; gap: 3mm; font: 10pt/1.35 'Be Vietnam Pro', Arial, sans-serif; border: 1px solid #000; }
header { display: flex; justify-content: space-between; gap: 4mm; border-bottom: 2px solid #000; padding-bottom: 2mm; }
.brand { font: 700 13pt 'Oswald', sans-serif; letter-spacing: .08em; } .brand small { display: block; font: 400 7pt Arial; letter-spacing: 0; }
.code { text-align: center; } .code svg { width: 46mm; height: 11mm; display: block; } .code rect { fill: #000; } .code span { font: 700 8pt ui-monospace, monospace; letter-spacing: .1em; }
.to small, .meta small { display: block; font-size: 6.5pt; letter-spacing: .12em; color: #333; }
.to b { font-size: 13pt; display: block; } .to span { font-weight: 700; } .to p { margin-top: 1mm; font-size: 10.5pt; }
.meta { display: grid; grid-template-columns: 1.3fr 1fr 1fr; border: 1.5px solid #000; } .meta > div { padding: 1.5mm 2mm; } .meta > div + div { border-left: 1.5px solid #000; }
.cod { font-size: 12pt; }
.items { list-style: none; font-size: 8.5pt; border-top: 1px dashed #000; padding-top: 2mm; flex: 1; }
.gift { font-weight: 700; border: 1.5px solid #000; padding: 1mm 2mm; font-size: 8.5pt; } .note { font-size: 8.5pt; }
footer { font-size: 7pt; text-align: center; border-top: 1px solid #000; padding-top: 1.5mm; }
@media print {
  @page { size: 105mm 148mm; margin: 0; }
  .no-print { display: none !important; }
  .labels-page { background: none; padding: 0; display: block; }
  .label { border: none; page-break-after: always; break-after: page; width: 105mm; height: 148mm; }
}
</style>
