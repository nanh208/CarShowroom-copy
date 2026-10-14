<template>
  <div class="inv-wrap">
    <div v-if="!o" class="spinner" />
    <div v-else class="inv">
      <div class="noprint bar"><router-link :to="`/orders/${o.code}`">← Quay lại đơn hàng</router-link><button class="btn btn-sm" onclick="window.print()">🖨 In hoá đơn</button></div>
      <header><div><img src="/images/logo-mark.png" alt="" /><b>ARES Toys</b><small>123 Nguyễn Văn Linh, Q.7, TP.HCM · Hotline 1900 6868<br />MST 0312345678</small></div>
        <div class="r"><h1>HOÁ ĐƠN BÁN HÀNG</h1><p>Số: <b>{{ o.code }}</b></p><p>Ngày: {{ d(o.createdAt) }}</p></div></header>
      <section class="who"><div><small>Khách hàng</small><b>{{ o.buyerName }}</b><p>{{ o.phone }} · {{ o.email }}</p><p>{{ o.shippingAddress }}</p></div>
        <div><small>Thanh toán</small><b>{{ PAY_METHOD[o.paymentMethod] }}</b><p>{{ PAY_STATUS[o.paymentStatus] }}<span v-if="o.transactionRef"> · {{ o.transactionRef }}</span></p></div></section>
      <table><thead><tr><th>#</th><th>Sản phẩm</th><th>SKU</th><th>SL</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead>
        <tbody><tr v-for="(i, n) in o.items" :key="i.id"><td>{{ n + 1 }}</td><td>{{ i.productName }}<small v-if="i.variantName"> — {{ i.variantName }}</small></td><td>{{ i.sku }}</td><td>{{ i.quantity }}</td><td>{{ vnd(i.unitPrice) }}</td><td>{{ vnd(i.unitPrice * i.quantity) }}</td></tr></tbody></table>
      <div class="sum">
        <p><span>Tạm tính</span><span>{{ vnd(o.subtotal) }}</span></p>
        <p v-if="o.discount"><span>Voucher {{ o.promoCode }}</span><span>−{{ vnd(o.discount) }}</span></p>
        <p v-if="o.pointsDiscount"><span>Điểm thưởng</span><span>−{{ vnd(o.pointsDiscount) }}</span></p>
        <p><span>Phí vận chuyển</span><span>{{ vnd(o.shippingFee) }}</span></p>
        <p class="t"><span>Tổng thanh toán</span><span>{{ vnd(o.total) }}</span></p>
        <p><span>Đã thanh toán</span><span>{{ vnd(o.paidAmount) }}</span></p>
      </div>
      <footer>Cảm ơn quý khách đã mua sắm tại ARES Toys! Hoá đơn điện tử có giá trị tra cứu bảo hành tại aresstore.vn/support</footer>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { vnd, d, PAY_METHOD, PAY_STATUS } from '../format'
const route = useRoute(), o = ref(null)
onMounted(async () => { o.value = (await api.get(`/orders/${route.params.code}`)).data.order })
</script>
<style scoped>
.inv-wrap { background: #eee; min-height: 100vh; padding: 30px 12px; }
.inv { background: #fff; max-width: 820px; margin: 0 auto; padding: 36px 44px; border-radius: 6px; font-size: .9rem; }
.bar { display: flex; justify-content: space-between; margin-bottom: 20px; }
header { display: flex; justify-content: space-between; border-bottom: 3px solid var(--bronze); padding-bottom: 16px; }
header img { height: 40px; vertical-align: middle; } header b { font-family: var(--head); font-size: 1.4rem; margin-left: 8px; } header small { display: block; color: #777; margin-top: 6px; }
.r { text-align: right; } .r h1 { font-size: 1.3rem; color: var(--bronze-3); }
.who { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 18px 0; } .who small { color: #888; text-transform: uppercase; font-size: .72rem; } .who b { display: block; }
table { width: 100%; border-collapse: collapse; } th, td { padding: 8px; border-bottom: 1px solid #e5e5e5; text-align: left; } th { background: #f6f1e8; }
td:nth-child(n+4), th:nth-child(n+4) { text-align: right; }
.sum { margin-left: auto; width: 320px; margin-top: 14px; } .sum p { display: flex; justify-content: space-between; padding: 4px 0; } .sum .t { font-weight: 800; font-size: 1.1rem; border-top: 2px solid #333; margin-top: 4px; padding-top: 8px; }
footer { margin-top: 30px; text-align: center; color: #888; font-size: .8rem; }
@media print { .inv-wrap { background: #fff; padding: 0; } .inv { box-shadow: none; padding: 0; } .noprint { display: none; } }
</style>
