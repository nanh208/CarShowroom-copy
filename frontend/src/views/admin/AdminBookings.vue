<template>
  <div>
    <h1>Lịch hẹn</h1>
    <div class="tabs">
      <button :class="{ active: !type }" @click="type = ''; load()">Tất cả</button>
      <button v-for="(v, k) in BOOKING_TYPE" :key="k" :class="{ active: type === k }" @click="type = k; load()">{{ v }}</button>
    </div>
    <div class="card table-wrap">
      <div v-if="!list.length" class="empty">Chưa có lịch hẹn.</div>
      <table v-else class="table">
        <thead><tr><th>#</th><th>Loại</th><th>Khách</th><th>Nội dung</th><th>Ngày / giờ</th><th>Gửi lúc</th><th>Trạng thái</th></tr></thead>
        <tbody><tr v-for="b in list" :key="b.id">
          <td>{{ b.id }}</td><td class="small">{{ BOOKING_TYPE[b.type] }}</td>
          <td>{{ b.name }}<div class="small muted">{{ b.phone }} {{ b.email }}</div></td>
          <td class="small">{{ b.productName || b.serviceType }}<div class="muted">{{ b.note }}</div></td>
          <td>{{ d(b.date) }}<div class="small muted">{{ b.timeSlot }}</div></td><td class="small">{{ dt(b.createdAt) }}</td>
          <td><select class="input sel" :value="b.status" @change="setStatus(b, $event.target.value)"><option v-for="(v, k) in BOOKING_STATUS" :key="k" :value="k">{{ v }}</option></select></td>
        </tr></tbody>
      </table>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'
import { d, dt, BOOKING_TYPE, BOOKING_STATUS } from '../../format'
const list = ref([]), type = ref('')
async function load() { list.value = (await api.get('/admin/bookings', { params: { type: type.value || undefined } })).data }
async function setStatus(b, s) { await api.patch(`/admin/bookings/${b.id}/status`, { status: s }); load() }
onMounted(load)
</script>
<style scoped>.sel { padding: 6px 10px; width: 140px; }</style>
