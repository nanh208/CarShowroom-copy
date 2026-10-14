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
          <form class="row" @submit.prevent="send"><input v-model="text" class="input grow" placeholder="Trả lời khách..." /><button class="btn">Gửi</button></form>
        </template>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import api from '../../api'
import { dt } from '../../format'
const sessions = ref([]), cur = ref(''), msgs = ref([]), text = ref(''), box = ref()
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
.sess:hover { background: #f5f5f5; } .sess.on { border-color: var(--bronze); background: rgba(168,122,60,.08); }
.ellipsis { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.convo { display: flex; flex-direction: column; gap: 12px; }
.msgs { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.msg { max-width: 70%; padding: 8px 12px; border-radius: 12px; white-space: pre-wrap; }
.me { align-self: flex-end; background: var(--bronze); color: #fff; } .them { align-self: flex-start; background: #f0f0f0; }
.t { font-size: .68rem; opacity: .6; margin-top: 2px; }
</style>
