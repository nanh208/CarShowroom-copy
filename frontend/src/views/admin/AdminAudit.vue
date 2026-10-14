<template>
  <div>
    <div class="row between"><h1>Nhật ký thao tác</h1>
      <div class="row"><input v-model="q" class="input" placeholder="Lọc theo người, thao tác, đường dẫn…" aria-label="Lọc nhật ký" style="max-width:280px" />
        <select v-model="who" class="input" aria-label="Lọc theo vai trò" style="width:auto"><option value="">Mọi vai trò</option><option>ADMIN</option><option>STAFF</option></select></div></div>
    <p class="muted small" style="margin:-16px 0 16px">Ghi lại mọi thay đổi qua trang quản trị (300 bản ghi gần nhất).</p>
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>Thời gian</th><th>Người thực hiện</th><th>Thao tác</th><th>Đường dẫn</th><th>Kết quả</th></tr></thead>
        <tbody>
          <tr v-for="a in shown" :key="a.id"><td class="small">{{ dt(a.createdAt) }}</td><td>{{ a.actor }} <span class="badge">{{ a.role }}</span></td><td>{{ a.summary }}</td>
            <td class="small mono">{{ a.method }} {{ a.path.replace('/api/admin', '') }}</td><td><span :class="['badge', a.status < 400 ? 'badge-COMPLETED' : 'badge-CANCELLED']">{{ a.status }}</span></td></tr>
          <tr v-if="!shown.length"><td colspan="5" class="muted">Chưa có bản ghi.</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../../api'
import { dt } from '../../format'
const list = ref([]), q = ref(''), who = ref('')
const shown = computed(() => list.value.filter(a => (!who.value || a.role === who.value) && (!q.value || `${a.actor} ${a.summary} ${a.path}`.toLowerCase().includes(q.value.toLowerCase()))))
onMounted(async () => { list.value = (await api.get('/admin/audit')).data })
</script>
<style scoped>.mono { font-family: ui-monospace, monospace; }</style>
