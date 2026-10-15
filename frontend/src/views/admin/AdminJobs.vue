<template>
  <div>
    <h1>Tác vụ nền</h1>
    <p class="muted small">Các job @Scheduled chạy tự động trên máy chủ. Có thể chạy tay để kiểm tra; mọi lần chạy đều được ghi nhật ký.</p>
    <div v-if="!data" class="sk" style="height:200px;margin-top:14px" />
    <template v-else>
      <div class="jg" v-stagger>
        <div v-for="(desc, k) in data.jobs" :key="k" class="card job">
          <div class="row between"><b>{{ k }}</b><span v-if="last(k)" :class="['badge', last(k).ok ? 'badge-COMPLETED' : 'badge-CANCELLED']">{{ last(k).ok ? 'OK' : 'Lỗi' }}</span></div>
          <small class="muted">{{ desc }}</small>
          <small v-if="last(k)">Lần cuối: {{ ago(last(k).startedAt) }} · {{ last(k).result }}</small><small v-else class="muted">Chưa chạy lần nào</small>
          <button class="btn btn-sm" :disabled="busy === k" @click="run(k)">{{ busy === k ? 'Đang chạy…' : 'Chạy ngay' }}</button>
        </div>
      </div>
      <div class="card" style="margin-top:16px">
        <h3>Nhật ký chạy</h3>
        <div class="table-wrap"><table class="table"><thead><tr><th>Job</th><th>Bắt đầu</th><th>Thời gian</th><th>Kích hoạt</th><th>Kết quả</th></tr></thead>
          <tbody><tr v-for="r in data.runs" :key="r.id"><td><b>{{ r.job }}</b></td><td class="small">{{ dt(r.startedAt) }}</td><td>{{ r.ms }}ms</td><td class="small">{{ r.trigger === 'auto' ? 'Tự động' : r.trigger }}</td><td :class="{ err: !r.ok }">{{ r.result }}</td></tr>
            <tr v-if="!data.runs.length"><td colspan="5" class="muted">Chưa có lần chạy.</td></tr></tbody></table></div>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { dt, ago } from '../../format'
import { toast } from '../../stores/prefs'
const data = ref(null), busy = ref('')
const last = k => data.value.runs.find(r => r.job === k)
const load = async () => { data.value = (await api.get('/admin/jobs')).data }
async function run(k) { busy.value = k; try { const { data: d } = await api.post(`/admin/jobs/${k}/run`); toast(d.result); await load() } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = '' } }
onMounted(load)
</script>
<style scoped>
.jg { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr)); gap: 12px; margin-top: 14px; }
.job { display: flex; flex-direction: column; gap: 6px; } .job .btn { align-self: flex-start; margin-top: auto; } .err { color: var(--danger); }
</style>
