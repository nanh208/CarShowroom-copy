<template>
  <div>
    <div class="row between"><h1>Phiếu hỗ trợ</h1>
      <div class="row" style="gap:6px"><button v-for="(v, k) in { '': 'Tất cả', ...TICKET_STATUS }" :key="k" :class="['btn btn-sm', st === k ? '' : 'btn-outline']" @click="st = k; load()">{{ v }}</button></div></div>
    <div class="split">
      <div class="card list">
        <button v-for="t in list" :key="t.id" :class="['it', { on: cur?.id === t.id }]" @click="cur = t">
          <div class="row between"><b>{{ t.code }}</b><span :class="['badge', 'badge-' + (t.status === 'OPEN' ? 'PENDING' : t.status === 'ANSWERED' ? 'SHIPPING' : 'COMPLETED')]">{{ TICKET_STATUS[t.status] }}</span></div>
          <div>{{ t.subject }}</div><div class="small muted">{{ t.user?.fullName }} · {{ t.category }} · {{ d(t.updatedAt) }}</div>
        </button>
        <div v-if="!list.length" class="muted" style="padding:16px">Không có phiếu.</div>
      </div>
      <div v-if="cur" class="card thread">
        <div class="row between"><div><h3 style="margin:0">{{ cur.subject }}</h3><div class="small muted">{{ cur.user?.fullName }} · {{ cur.user?.email }}<span v-if="cur.orderCode"> · Đơn <router-link :to="`/orders/${cur.orderCode}`" target="_blank">{{ cur.orderCode }}</router-link></span></div></div>
          <button v-if="cur.status !== 'CLOSED'" class="btn btn-sm btn-outline" @click="reply({ status: 'CLOSED' })">Đóng phiếu</button></div>
        <div class="msgs">
          <div v-for="m in cur.messages" :key="m.id" :class="['msg', { staff: m.fromStaff }]"><div class="small muted">{{ m.author }} · {{ dt(m.createdAt) }}</div>{{ m.content }}<MsgFiles :files="m.attachments" /></div>
        </div>
        <div class="composer"><textarea v-model="text" rows="3" class="input" placeholder="Phản hồi khách hàng…" aria-label="Phản hồi" />
          <div class="row between"><CannedReplies :name="cur.user?.fullName" :order="cur.orderCode" :current="text" @insert="t => (text = text ? text + '\n' + t : t)" /><button class="btn" :disabled="!text.trim()" @click="reply({ content: text })">Gửi</button></div></div>
      </div>
      <div v-else class="card muted">Chọn một phiếu để xem.</div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'
import CannedReplies from '../../components/CannedReplies.vue'
import MsgFiles from '../../components/MsgFiles.vue'
import { d, TICKET_STATUS } from '../../format'
const list = ref([]), cur = ref(null), st = ref('OPEN'), text = ref('')
const dt = s => s ? new Date(s).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : ''
const load = async () => { list.value = (await api.get('/admin/tickets', { params: { status: st.value } })).data; if (cur.value) cur.value = list.value.find(t => t.id === cur.value.id) || cur.value }
const reply = async body => { cur.value = (await api.post(`/admin/tickets/${cur.value.id}/reply`, body)).data; text.value = ''; load() }
onMounted(load)
</script>
<style scoped>
.split { display: grid; grid-template-columns: minmax(260px, 340px) 1fr; gap: 20px; align-items: start; }
.composer { display: flex; flex-direction: column; gap: 8px; }
@media (max-width: 899px) { .split { grid-template-columns: 1fr; } .list { max-height: 40vh; } }
.list { padding: 8px !important; max-height: 70vh; overflow: auto; }
.it { display: block; width: 100%; text-align: left; background: none; border: none; border-radius: 10px; padding: 12px; cursor: pointer; font: inherit; color: inherit; }
.it:hover { background: rgba(0,0,0,.04); } .it.on { background: rgba(168,122,60,.14); }
.msgs { display: flex; flex-direction: column; gap: 10px; margin: 16px 0; max-height: 50vh; overflow: auto; }
.msg { background: var(--bg-2); padding: 10px 14px; border-radius: 12px; max-width: 80%; white-space: pre-wrap; } .msg.staff { align-self: flex-end; background: rgba(168,122,60,.18); }
</style>
