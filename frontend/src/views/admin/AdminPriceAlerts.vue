<template>
  <div>
    <h1>Theo dõi giá</h1>
    <p class="muted small">Khách đăng ký báo giảm giá (có thể đặt giá mong muốn). Hệ thống quét mỗi 30 giây và gửi thông báo + email khi giá giảm.</p>
    <div class="card" style="margin-top:14px">
      <ListTools :p="pg" placeholder="Tìm sản phẩm, email…"><select v-model="st" class="input" aria-label="Trạng thái"><option value="">Tất cả</option><option value="false">Đang chờ</option><option value="true">Đã báo</option></select></ListTools>
      <div v-if="pg.loading.value && !pg.items.value.length" class="sk" style="height:160px" />
      <p v-else-if="pg.error.value" class="alert alert-error">{{ pg.error.value }}</p>
      <div v-else-if="!pg.items.value.length" class="empty">Chưa có đăng ký theo dõi giá.</div>
      <div v-else class="table-wrap"><table class="table"><thead><tr><th>Sản phẩm</th><th>Liên hệ</th><th>Giá lúc đăng ký</th><th>Giá mong muốn</th><th>Giá hiện tại</th><th>Trạng thái</th><th>Ngày</th></tr></thead>
        <tbody><tr v-for="a in pg.items.value" :key="a.id">
          <td><router-link :to="`/products/${a.productId}`">{{ a.productName }}</router-link></td><td class="small">{{ a.contact }}</td>
          <td class="money">{{ vnd(a.basePrice) }}</td><td class="money">{{ a.targetPrice ? vnd(a.targetPrice) : '—' }}</td><td class="money">{{ vnd(a.currentPrice) }}</td>
          <td><span :class="['badge', a.fired ? 'badge-COMPLETED' : 'badge-PENDING']">{{ a.fired ? 'Đã báo ' + vnd(a.firedPrice) : 'Đang chờ' }}</span></td><td class="small">{{ d(a.createdAt) }}</td>
        </tr></tbody></table></div>
      <Pager :p="pg" />
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import ListTools from '../../components/ListTools.vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import { vnd, d } from '../../format'
const st = ref('')
const pg = useServerPaged('/admin/price-alerts', { size: 20, filters: () => ({ 'f.fired': st.value }), sorts: { new: { label: 'Mới nhất', sort: 'createdAt,desc' }, gap: { label: 'Giá hiện tại thấp', sort: 'currentPrice' } } })
pg.reload()
</script>
