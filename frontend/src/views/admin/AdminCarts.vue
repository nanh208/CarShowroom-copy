<template>
  <div>
    <div class="row between hd"><div><h1>Giỏ hàng bị bỏ quên</h1><p class="muted small">Giỏ có hàng nhưng chưa đặt sau {{ minutes }} phút. Gửi email nhắc kèm mã COMEBACK5 (−5%).</p></div>
      <label class="small">Không hoạt động từ <select v-model.number="minutes" class="input" style="width:auto" @change="load"><option :value="0">0 phút</option><option :value="30">30 phút</option><option :value="120">2 giờ</option><option :value="1440">1 ngày</option></select></label></div>
    <div class="kp"><div class="card"><small class="muted">Giỏ bị bỏ</small><b>{{ list.length }}</b></div><div class="card"><small class="muted">Giá trị tiềm năng</small><b>{{ vnd(list.reduce((a, c) => a + c.subtotal, 0)) }}</b></div>
      <div class="card"><small class="muted">Đã nhắc phiên này</small><b>{{ Object.keys(sent).length }}</b></div></div>
    <div v-if="!list.length" class="card empty muted">Không có giỏ hàng bị bỏ quên. 🎉</div>
    <div v-else class="card table-wrap">
      <table class="table">
        <thead><tr><th>Khách</th><th>Sản phẩm</th><th>Giá trị</th><th>Lần cuối</th><th /></tr></thead>
        <tbody><tr v-for="c in list" :key="c.id">
          <td><b>{{ c.name || 'Khách' }}</b><div class="small muted">{{ c.email }}</div></td>
          <td><div class="its"><img v-for="(i, n) in c.items.slice(0, 4)" :key="n" :src="i.image" :title="`${i.name} × ${i.quantity}`" alt="" /><span class="small muted">{{ c.items.reduce((a, i) => a + i.quantity, 0) }} sp</span></div></td>
          <td><b>{{ vnd(c.subtotal) }}</b></td><td class="small">{{ ago(c.lastSeen) }}</td>
          <td><button class="btn btn-sm" :disabled="sent[c.id]" @click="remind(c)">{{ sent[c.id] ? '✓ Đã gửi' : '✉ Nhắc khách' }}</button></td>
        </tr></tbody>
      </table>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, ago } from '../../format'
import { toast } from '../../stores/prefs'
const list = ref([]), minutes = ref(30), sent = reactive({})
const load = async () => { list.value = (await api.get('/admin/abandoned-carts', { params: { minutes: minutes.value } })).data.map(c => ({ ...c, items: JSON.parse(c.itemsJson || '[]') })) }
async function remind(c) { try { const { data } = await api.post(`/admin/abandoned-carts/${c.id}/remind`); sent[c.id] = true; toast(`Đã gửi email nhắc tới ${data.sentTo}`) } catch (e) { toast(errMsg(e), 'error') } }
onMounted(load)
</script>
<style scoped>
.hd { gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.kp { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; margin: 16px 0; } .kp b { display: block; font-size: 1.3rem; margin-top: 4px; }
.its { display: flex; align-items: center; gap: 4px; } .its img { width: 40px; height: 30px; object-fit: cover; }
</style>
