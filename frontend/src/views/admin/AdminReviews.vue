<template>
  <div>
    <div class="row between head"><h1>Kiểm duyệt đánh giá</h1>
      <div class="tabs flat"><button v-for="(v, k) in filters" :key="k" :class="{ active: f === k }" @click="f = k">{{ v }} <small>({{ count(k) }})</small></button></div></div>
    <div class="card">
      <ListTools :p="pg" placeholder="Tìm sản phẩm, khách, nội dung…" />
      <div v-if="!pg.items.value.length" class="empty">Không có đánh giá.</div>
      <div v-else class="table-wrap"><table class="table"><thead><tr><th>Sản phẩm</th><th>Khách hàng</th><th>Đánh giá</th><th>Nội dung</th><th>Ngày</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="r in pg.items.value" :key="r.id">
          <td style="min-width:180px"><router-link :to="`/products/${r.productId}`">{{ r.productName }}</router-link></td>
          <td style="white-space:nowrap">{{ r.authorName }}</td>
          <td><Stars :value="r.rating" size=".9rem" /></td>
          <td class="cmt">{{ r.comment }}</td>
          <td class="small" style="white-space:nowrap">{{ d(r.createdAt) }}</td>
          <td><span :class="['badge', r.approved ? 'badge-COMPLETED' : 'badge-PENDING']">{{ r.approved ? 'Hiển thị' : 'Đã ẩn' }}</span></td>
          <td><div class="row" style="flex-wrap:nowrap;gap:8px">
            <button class="btn btn-sm btn-outline" @click="toggle(r)">{{ r.approved ? 'Ẩn' : 'Duyệt' }}</button>
            <button class="btn btn-sm btn-outline danger" @click="del(r)">Xoá</button></div></td>
        </tr></tbody></table></div>
      <Pager :p="pg" />
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import ListTools from '../../components/ListTools.vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import Stars from '../../components/Stars.vue'
import { d } from '../../format'
import { toast } from '../../stores/prefs'
const list = ref([]), f = ref('all')
const filters = { all: 'Tất cả', on: 'Đang hiển thị', off: 'Đã ẩn', low: '≤ 3 sao' }
const match = (r, k) => k === 'all' || (k === 'on' && r.approved) || (k === 'off' && !r.approved) || (k === 'low' && r.rating <= 3)
const FP = { all: {}, on: { 'f.approved': 'true' }, off: { 'f.approved': 'false' }, low: { 'f.rating': '1,2,3' } }
const counts = ref({}), count = k => counts.value[k] ?? '…'
// Phân trang + tìm kiếm + sắp xếp phía máy chủ
const pg = useServerPaged('/admin/reviews', { filters: () => FP[f.value], sorts: { new: { label: 'Mới nhất', sort: 'createdAt,desc' }, low: { label: 'Sao thấp trước', sort: 'rating' }, high: { label: 'Sao cao trước', sort: 'rating,desc' } } })
const load = async () => { await pg.reload(); for (const k of Object.keys(FP)) api.get('/admin/reviews', { params: { page: 0, size: 1, ...FP[k] } }).then(r => { counts.value[k] = r.data.totalElements }) }
const toggle = async r => { try { await api.patch(`/admin/reviews/${r.id}`, { approved: !r.approved }); r.approved = !r.approved; toast(r.approved ? 'Đã duyệt đánh giá' : 'Đã ẩn đánh giá') } catch (e) { toast(errMsg(e), 'error') } }
const del = async r => { if (!confirm('Xoá đánh giá này?')) return; await api.delete(`/admin/reviews/${r.id}`); await load(); toast('Đã xoá đánh giá') }
onMounted(load)
</script>
<style scoped>
.head { margin-bottom: 24px; } .head h1 { margin: 0 !important; }
.flat { border: none; margin: 0; } .flat small { color: var(--muted); }
.cmt { color: var(--text-2); min-width: 260px; } .danger { color: var(--danger); }
</style>
