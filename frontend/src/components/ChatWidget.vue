<template>
  <div class="chat">
    <div v-if="open" class="box">
      <div class="head row between">
        <div><b>Tư vấn trực tuyến</b><div class="small">Hotline: {{ HOTLINE }}</div></div>
        <button class="x" @click="open = false">✕</button>
      </div>
      <div ref="list" class="msgs">
        <div v-if="!msgs.length" class="muted small hint">Xin chào! Hãy để lại câu hỏi.</div>
        <div v-for="m in msgs" :key="m.id" :class="['msg', m.fromAdmin ? 'them' : 'me']">{{ m.content }}</div>
      </div>
      <div v-if="!auth.loggedIn && !msgs.length" class="namebox"><input v-model="name" class="input" placeholder="Tên của bạn" /></div>
      <form class="row send" @submit.prevent="send">
        <input v-model="text" class="input grow" placeholder="Nhập tin nhắn..." /><button class="btn btn-sm">Gửi</button>
      </form>
      <a class="call" :href="`tel:${HOTLINE.replace(/ /g,'')}`">📞 {{ HOTLINE }}</a>
    </div>
    <button class="fab" @click="toggle">{{ open ? '✕' : '💬' }}<span v-if="unread && !open" class="dot" /></button>
  </div>
</template>
<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import api from '../api'
import { useAuth } from '../stores/auth'
import { HOTLINE } from '../format'
const auth = useAuth()
const open = ref(false), text = ref(''), name = ref(''), msgs = ref([]), unread = ref(false), list = ref()
let sid = localStorage.getItem('chatSession')
if (!sid) { sid = 'c' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8); localStorage.setItem('chatSession', sid) }
let timer
const lastId = () => msgs.value.length ? msgs.value[msgs.value.length - 1].id : 0
const scroll = () => nextTick(() => { if (list.value) list.value.scrollTop = list.value.scrollHeight })
async function poll() {
  try {
    const { data } = await api.get(`/chat/${sid}`, { params: { after: lastId() } })
    if (data.length) { msgs.value.push(...data); if (!open.value && data.some(m => m.fromAdmin)) unread.value = true; scroll() }
  } catch {}
}
async function send() {
  if (!text.value.trim()) return
  await api.post(`/chat/${sid}`, { content: text.value, customerName: name.value })
  text.value = ''; await poll()
}
const toggle = () => { open.value = !open.value; unread.value = false; scroll() }
onMounted(() => { poll(); timer = setInterval(poll, 4000) })
onUnmounted(() => clearInterval(timer))
</script>
<style scoped>
.chat { position: fixed; right: 20px; bottom: 20px; z-index: 90; }
.fab { width: 52px; height: 52px; border-radius: 50%; border: none; background: var(--bronze); color: #fff; font-size: 1.3rem; cursor: pointer; box-shadow: 0 6px 18px rgba(168,122,60,.45); position: relative; }
.dot { position: absolute; top: 4px; right: 4px; width: 10px; height: 10px; border-radius: 50%; background: var(--red); }
.box { position: absolute; right: 0; bottom: 64px; width: 320px; background: #fff; border: 1px solid var(--line); border-radius: 4px; overflow: hidden; box-shadow: var(--shadow); display: flex; flex-direction: column; color: var(--text); }
.head { background: var(--bronze); color: #fff; padding: 11px 12px; }
.x { background: none; border: none; color: #fff; font-size: 1.1rem; cursor: pointer; }
.msgs { height: 260px; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.hint { text-align: center; margin-top: 30px; }
.msg { max-width: 80%; padding: 8px 11px; border-radius: 4px; font-size: .88rem; white-space: pre-wrap; }
.me { align-self: flex-end; background: var(--bronze); color: #fff; }
.them { align-self: flex-start; background: var(--bg-soft); }
.namebox { padding: 0 12px 8px; }
.send { padding: 0 12px 10px; flex-wrap: nowrap; }
.send .input, .namebox .input { padding: 8px 10px; }
.call { text-align: center; padding: 8px; font-size: .82rem; border-top: 1px solid var(--line); color: var(--bronze-dark); }
</style>
