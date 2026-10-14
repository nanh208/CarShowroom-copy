<template>
  <div>
    <h1>Quản lý khách hàng</h1>
    <input v-model="q" class="input" style="max-width:320px;margin-bottom:14px" placeholder="Tìm theo tên, email, SĐT..." />
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>ID</th><th>Họ tên</th><th>Email</th><th>SĐT</th><th>Đăng nhập</th><th>Vai trò</th><th>Số đơn</th><th>Tổng chi tiêu</th><th>Ngày tạo</th><th /></tr></thead>
        <tbody><tr v-for="c in filtered" :key="c.user.id" :class="{ off: c.user.locked }">
          <td>{{ c.user.id }}</td><td><b>{{ c.user.fullName }}</b></td><td>{{ c.user.email }}</td><td>{{ c.user.phone }}</td>
          <td class="small">{{ c.user.provider }}</td>
          <td><select class="input sel" :value="c.user.role" @change="patch(c.user, { role: $event.target.value })"><option>USER</option><option>ADMIN</option></select></td>
          <td>{{ c.orderCount }}</td><td class="price">{{ vnd(c.totalSpent) }}</td><td class="small">{{ d(c.user.createdAt) }}</td>
          <td><button :class="['btn btn-sm', c.user.locked ? '' : 'btn-danger']" @click="patch(c.user, { locked: !c.user.locked })">{{ c.user.locked ? 'Mở khoá' : 'Khoá' }}</button></td>
        </tr></tbody>
      </table>
    </div>
    <div v-if="err" class="alert alert-error" style="margin-top:12px">{{ err }}</div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, d } from '../../format'
const list = ref([]), q = ref(''), err = ref('')
const filtered = computed(() => list.value.filter(c => !q.value || [c.user.fullName, c.user.email, c.user.phone].join(' ').toLowerCase().includes(q.value.toLowerCase())))
async function load() { list.value = (await api.get('/admin/customers')).data }
async function patch(u, body) { err.value = ''; try { await api.patch(`/admin/customers/${u.id}`, body) } catch (e) { err.value = errMsg(e) } load() }
onMounted(load)
</script>
<style scoped>.sel { padding: 5px 8px; width: 100px; } .off { opacity: .55; }</style>
