<template>
  <div>
    <h1>Giao dịch cổng thanh toán</h1>
    <p class="muted small">VNPay / MoMo (sandbox). Trạng thái được ghi nhận qua IPN có chữ ký HMAC-SHA512; giao dịch chờ quá 15 phút tự hết hạn.</p>
    <div class="card" style="margin-top:14px">
      <ListTools :p="pg" placeholder="Tìm mã đơn, mã giao dịch…"><select v-model="st" class="input" aria-label="Trạng thái"><option value="">Mọi trạng thái</option><option v-for="s in ['PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED']" :key="s">{{ s }}</option></select></ListTools>
      <div v-if="pg.loading.value && !pg.items.value.length" class="sk" style="height:160px" />
      <div v-else-if="!pg.items.value.length" class="empty">Chưa có giao dịch.</div>
      <div v-else class="table-wrap"><table class="table"><thead><tr><th>Mã GD</th><th>Đơn</th><th>Cổng</th><th>Số tiền</th><th>Trạng thái</th><th>Mã PH</th><th>Ngân hàng</th><th>IPN</th><th>Tạo lúc</th></tr></thead>
        <tbody><tr v-for="t in pg.items.value" :key="t.id"><td class="mono small">{{ t.txnRef }}</td><td><router-link :to="`/admin/orders?q=${t.orderCode}`">{{ t.orderCode }}</router-link></td><td>{{ t.provider }}</td>
          <td class="money">{{ vnd(t.amount) }}</td><td><span :class="['badge', { SUCCESS: 'badge-COMPLETED', PENDING: 'badge-PENDING' }[t.status] || 'badge-CANCELLED']">{{ t.status }}</span></td>
          <td>{{ t.responseCode || '—' }}</td><td>{{ t.bankCode || '—' }}</td><td>{{ t.ipnCount }}</td><td class="small">{{ dt(t.createdAt) }}</td></tr></tbody></table></div>
      <Pager :p="pg" />
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import ListTools from '../../components/ListTools.vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import { vnd, dt } from '../../format'
const st = ref('')
const pg = useServerPaged('/admin/payments', { filters: () => ({ 'f.status': st.value }), sorts: { new: { label: 'Mới nhất', sort: 'id,desc' }, amt: { label: 'Số tiền lớn', sort: 'amount,desc' } } })
pg.reload()
</script>
