<template>
  <div class="container page">
    <div v-if="!t" class="spinner" />
    <template v-else>
      <Breadcrumb :items="[{ label: 'Hỗ trợ', to: '/support' }, { label: t.code }]" />
      <div class="row between"><h1>{{ t.subject }}</h1><span :class="['badge', 'badge-' + t.status]">{{ TICKET_STATUS[t.status] }}</span></div>
      <p class="muted">{{ t.code }} · {{ t.category }}<span v-if="t.orderCode"> · Đơn <router-link :to="`/orders/${t.orderCode}`">{{ t.orderCode }}</router-link></span> · tạo {{ dt(t.createdAt) }}</p>
      <div class="thread card">
        <div v-for="m in t.messages" :key="m.id" :class="['msg', { staff: m.fromStaff }]"><b>{{ m.author }}</b><p>{{ m.content }}</p><small>{{ dt(m.createdAt) }}</small></div>
      </div>
      <div v-if="t.status !== 'CLOSED'" class="card reply">
        <textarea v-model="text" class="input" rows="3" placeholder="Nhập phản hồi…" />
        <div class="row between"><button class="btn btn-sm btn-outline" @click="send('CLOSED')">Đóng phiếu (đã giải quyết)</button><button class="btn btn-sm" :disabled="!text.trim()" @click="send()">Gửi</button></div>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Breadcrumb from '../components/Breadcrumb.vue'
import api, { errMsg } from '../api'
import { dt, TICKET_STATUS } from '../format'
import { toast } from '../stores/prefs'
const route = useRoute(), t = ref(null), text = ref('')
const load = async () => { t.value = (await api.get(`/tickets/${route.params.code}`)).data }
async function send(status) { try { t.value = (await api.post(`/tickets/${t.value.code}/reply`, { content: text.value, status })).data; text.value = '' } catch (e) { toast(errMsg(e), 'error') } }
onMounted(load)
</script>
<style scoped>
.thread { display: flex; flex-direction: column; gap: 10px; margin: 16px 0; }
.msg { max-width: 75%; padding: 10px 14px; border-radius: 12px; background: var(--bg-2); } .msg.staff { align-self: flex-end; background: var(--bronze-soft); }
.msg p { margin: 4px 0; white-space: pre-line; } .msg small { color: var(--muted); font-size: .74rem; }
.reply { display: flex; flex-direction: column; gap: 10px; }
</style>
