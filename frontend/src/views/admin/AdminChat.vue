<template>
  <div>
    <h1>Live chat</h1>
    <div class="chat-layout">
      <div class="card sessions">
        <div v-if="!sessions.length" class="muted small">Chưa có cuộc trò chuyện.</div>
        <button v-for="s in sessions" :key="s.sessionId" :class="['sess', { on: cur === s.sessionId }]" @click="open(s.sessionId)">
          <div class="row between"><b>{{ s.customerName }}</b><span v-if="s.waiting" class="badge badge-PENDING">Chờ</span></div>
          <div class="small muted ellipsis">{{ s.lastMessage }}</div>
        </button>
      </div>
      <div class="card convo">
        <div v-if="!cur" class="empty">Chọn một cuộc trò chuyện</div>
        <template v-else>
          <div ref="box" class="msgs">
            <div v-for="m in msgs" :key="m.id" :class="['msg', m.fromAdmin ? 'me' : 'them']">{{ m.content }}<div class="t">{{ dt(m.createdAt) }}</div></div>
          </div>
          <form class="row composer" @submit.prevent="send"><CannedReplies :name="curName" :current="text" @insert="t => (text = t)" /><input v-model="text" class="input grow" placeholder="Trả lời khách..." aria-label="Trả lời" /><button class="btn">Gửi</button></form>
        </template>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import api from '../../api'
import { computed } from 'vue'
import CannedReplies from '../../components/CannedReplies.vue'
import { dt } from '../../format'
const sessions = ref([]), cur = ref(''), msgs = ref([]), text = ref(''), box = ref()
const curName = computed(() => sessions.value.find(s => s.sessionId === cur.value)?.customerName)
let timer
const scroll = () => nextTick(() => box.value && (box.value.scrollTop = box.value.scrollHeight))
async function refresh() {
  sessions.value = (await api.get('/admin/chat/sessions')).data
  if (cur.value) {
    const after = msgs.value.length ? msgs.value[msgs.value.length - 1].id : 0
    const { data } = await api.get(`/chat/${cur.value}`, { params: { after } })
    if (data.length) { msgs.value.push(...data); scroll() }
  }
}
async function open(s) { cur.value = s; msgs.value = []; await refresh() }
async function send() { if (!text.value.trim()) return; await api.post(`/admin/chat/${cur.value}`, { content: text.value }); text.value = ''; refresh() }
onMounted(() => { refresh(); timer = setInterval(refresh, 4000) })
onUnmounted(() => clearInterval(timer))
</script>
<style scoped>
.chat-layout { display: grid; grid-template-columns: 280px 1fr; gap: 18px; height: calc(100vh - 140px); }
.sessions { overflow-y: auto; padding: 10px; }
.sess { display: block; width: 100%; text-align: left; background: none; border: 1px solid transparent; color: var(--text); padding: 10px; border-radius: 10px; cursor: pointer; font-family: inherit; margin-bottom: 4px; }
.sess:hover { background: rgba(0,0,0,.04); } .sess.on { border-color: var(--accent); background: rgba(168,122,60,.08); }
.ellipsis { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.convo { display: flex; flex-direction: column; gap: 12px; }
.msgs { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.msg { max-width: 70%; padding: 8px 12px; border-radius: 12px; white-space: pre-wrap; }
.me { align-self: flex-end; background: var(--primary); } .them { align-self: flex-start; background: rgba(0,0,0,.08); }
.t { font-size: .68rem; opacity: .6; margin-top: 2px; }
.me { color: #fff; } .composer { flex-wrap: nowrap; gap: 8px; } .composer .input { min-width: 0; }
@media (max-width: 899px) { .chat-layout { grid-template-columns: 1fr; height: auto; } .sessions { max-height: 30vh; } .convo { min-height: 60vh; } .msg { max-width: 88%; } .composer { flex-wrap: wrap; } }
</style>
