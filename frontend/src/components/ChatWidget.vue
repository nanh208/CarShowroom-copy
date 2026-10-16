<template>
  <div class="chat">
    <transition name="cw">
      <section v-if="open" class="box" role="dialog" aria-label="Trợ lý hỗ trợ ARES">
        <header class="head">
          <span class="av" aria-hidden="true">A<i :class="{ on: mode === 'bot' || staffOnline }" /></span>
          <div><b>{{ mode === 'human' ? 'Chat với nhân viên' : 'Trợ lý ARES' }}<span v-if="aiOn && mode !== 'human'" class="ai-tag">AI</span></b><small>{{ mode === 'human' ? 'Thường phản hồi trong vài phút' : 'Trả lời tức thì 24/7 · tìm sản phẩm, tra đơn, mã giảm giá' }}</small></div>
          <button v-if="mode === 'human'" class="hb" @click="mode = 'bot'">Trợ lý</button>
          <button v-else class="hb" title="Xoá lịch sử trò chuyện" @click="reset">Mới</button>
          <button class="x" aria-label="Đóng" @click="open = false">✕</button>
        </header>
        <div ref="list" class="msgs" aria-live="polite">
          <template v-if="mode === 'bot'">
            <transition-group name="msg">
              <div v-for="m in bot" :key="m.k" :class="['msg', m.me ? 'me' : 'them']">
                <span>{{ m.text }}</span>
                <div v-if="m.products?.length" class="plist">
                  <router-link v-for="p in m.products" :key="p.id" :to="`/products/${p.id}`" class="pc" @click="open = false">
                    <img :src="p.image" alt="" loading="lazy" /><span><b>{{ p.name }}</b><small>{{ vnd(p.price) }} · {{ p.stock ? 'Còn hàng' : 'Hết hàng' }}</small></span></router-link>
                </div>
                <div v-if="m.coupons?.length" class="clist">
                  <button v-for="c in m.coupons" :key="c.code" class="cp" @click="copy(c.code)"><b>{{ c.code }}</b><small>{{ c.value }}{{ c.minOrder ? ' · đơn từ ' + vnd(c.minOrder) : '' }}{{ c.endDate ? ' · HSD ' + d(c.endDate) : '' }}</small><em>{{ copied === c.code ? 'Đã chép ✓' : 'Chép' }}</em></button>
                </div>
                <div v-if="m.order" class="ocard">
                  <div class="row between"><b>{{ m.order.code }}</b><span :class="['badge', 'badge-' + m.order.status]">{{ ORDER_STATUS[m.order.status] }}</span></div>
                  <small v-if="m.order.etaFrom">Dự kiến giao {{ d(m.order.etaFrom) }} – {{ d(m.order.etaTo) }}</small>
                  <small v-if="m.order.total">Tổng {{ vnd(m.order.total) }} · {{ PAY_STATUS[m.order.paymentStatus] }}</small>
                  <ul v-if="m.order.items"><li v-for="it in m.order.items" :key="it">{{ it }}</li></ul>
                  <router-link :to="auth.loggedIn ? `/orders/${m.order.code}` : `/track/${m.order.code}`" class="small" @click="open = false">Xem chi tiết →</router-link>
                </div>
                <div v-if="m.orders?.length" class="olist">
                  <button v-for="o in m.orders" :key="o.code" class="ochip" @click="ask(o.code)"><b>{{ o.code }}</b><small>{{ ORDER_STATUS[o.status] }} · {{ vnd(o.total) }}</small></button>
                </div>
                <button v-if="m.handoff" class="btn btn-sm hand" :disabled="busy" @click="handoff">Chuyển nhân viên</button>
                <router-link v-if="m.ticket" :to="`/support/tickets/${m.ticket}`" class="small" @click="open = false">Mở phiếu {{ m.ticket }} →</router-link>
              </div>
            </transition-group>
            <div v-if="typing" class="msg them typing" aria-label="Trợ lý đang soạn"><i /><i /><i /></div>
          </template>
          <template v-else>
            <div v-if="!msgs.length" class="muted small hint">Nhân viên ARES sẽ trả lời ngay. Bạn có thể mô tả vấn đề bên dưới.</div>
            <div v-for="m in msgs" :key="m.id" :class="['msg', m.fromAdmin ? 'them' : 'me']">{{ m.content }}</div>
            <div v-if="staffTyping" class="msg them typing" aria-label="Nhân viên đang gõ"><i /><i /><i /></div>
            <small v-if="rtConnected" class="muted live">● Kết nối trực tiếp</small>
          </template>
        </div>
        <div v-if="mode === 'bot' && !typing" class="chips">
          <button v-for="c in quick" :key="c" class="chip" @click="chip(c)">{{ c }}</button>
        </div>
        <div v-if="mode === 'human' && !auth.loggedIn && !msgs.length" class="namebox"><input v-model="name" class="input" placeholder="Tên của bạn" aria-label="Tên của bạn" /></div>
        <form class="send" @submit.prevent="mode === 'bot' ? ask() : send()">
          <input v-model="text" class="input" maxlength="500" @input="sendTyping" :placeholder="mode === 'bot' ? 'VD: xe RC dưới 3tr, phí ship, mã giảm giá…' : 'Nhập tin nhắn…'" aria-label="Tin nhắn" />
          <button class="btn btn-sm" :disabled="!text.trim() || typing">Gửi</button>
        </form>
        <a class="call" :href="`tel:${HOTLINE.replace(/ /g, '')}`">Gọi hotline {{ HOTLINE }}</a>
      </section>
    </transition>
    <button :class="['fab', { opened: open }]" :aria-label="open ? 'Đóng hỗ trợ' : 'Mở trợ lý hỗ trợ'" @click="toggle">
      <transition name="ic" mode="out-in"><span v-if="open" key="x">✕</span><svg v-else key="c" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z"/></svg></transition>
      <span v-if="unread && !open" class="dot" /></button>
  </div>
</template>

<script setup>
import { subscribe, publish, rtConnected } from '../realtime'
import { ref, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { useAuth } from '../stores/auth'
import { HOTLINE, ORDER_STATUS, PAY_STATUS, vnd, d } from '../format'

const auth = useAuth(), router = useRouter()
const open = ref(false), text = ref(''), name = ref(''), msgs = ref([]), unread = ref(false), list = ref(), mode = ref('bot'), busy = ref(false), typing = ref(false), copied = ref('')
const staffOnline = ref(true)
const GREET = { k: 'g', text: 'Xin chào! Mình là trợ lý ARES 👋 Hỏi mình về đơn hàng, mã giảm giá, phí ship — hoặc gõ tên mẫu xe/ngân sách để tìm sản phẩm.' }
const START = ['Tra cứu đơn hàng', 'Mã giảm giá', 'Phí ship', 'Đổi trả', 'Gợi ý quà tặng', 'Gặp nhân viên']
const bot = ref([]), quick = ref(START)
const newSid = () => 'c' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
let sid = localStorage.getItem('chatSession') || newSid(); localStorage.setItem('chatSession', sid)
let timer, seq = 0
const KEY = 'botHistory'
const save = () => localStorage.setItem(KEY, JSON.stringify({ sid, bot: bot.value.slice(-40), quick: quick.value }))
try { const h = JSON.parse(localStorage.getItem(KEY) || 'null'); if (h?.sid === sid && h.bot?.length) { bot.value = h.bot; quick.value = h.quick || START; seq = h.bot.length } } catch { /* lịch sử hỏng */ }
if (!bot.value.length) bot.value = [GREET]
// Lịch sử lưu ở server (BotLog): khôi phục khi máy này chưa có bản lưu
if (bot.value.length <= 1) api.get(`/public/bot/${sid}`).then(r => { if (r.data.length) bot.value = [GREET, ...r.data.flatMap(l => [{ k: 'q' + l.id, me: true, text: l.question }, { k: 'a' + l.id, text: l.answer }])]; seq = bot.value.length }).catch(() => {})
watch(bot, save, { deep: true })
const scroll = () => nextTick(() => { if (list.value) list.value.scrollTo({ top: list.value.scrollHeight, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' }) })
const push = m => { bot.value.push({ k: ++seq + '-' + Date.now(), ...m }); scroll() }
const wait = ms => new Promise(r => setTimeout(r, ms))

async function ask(q) {
  q = (q ?? text.value).trim(); if (!q || typing.value) return; text.value = ''; push({ me: true, text: q })
  typing.value = true; scroll(); const t0 = Date.now()
  try {
    const { data } = await api.post(`/public/bot/${sid}`, { text: q })
    await wait(Math.max(0, Math.min(900, 350 + data.text.length * 4) - (Date.now() - t0)))
    const m = { text: data.text, products: data.products, coupons: data.coupons, order: data.order, handoff: data.handoff }
    if (data.askOrder && auth.loggedIn) { try { m.orders = (await api.get('/account/orders')).data.slice(0, 4) } catch { /* bỏ qua */ } }
    push(m); quick.value = data.quick?.length ? data.quick : START
  } catch (e) { push({ text: 'Có lỗi kết nối (' + errMsg(e) + '). Thử lại hoặc gọi hotline nhé.', handoff: true }) }
  finally { typing.value = false; scroll() }
}
const LINKS = { 'Thẻ quà tặng': '/gift-cards', 'Bộ sưu tập': '/collections', 'Sắp ra mắt': '/upcoming' }
function chip(c) {
  if (c === 'Gặp nhân viên') { push({ me: true, text: c }); return handoff() }
  if (LINKS[c]) { open.value = false; return router.push(LINKS[c]) }
  ask(c)
}
async function copy(code) { try { await navigator.clipboard.writeText(code) } catch { /* trình duyệt chặn */ } copied.value = code; setTimeout(() => (copied.value = ''), 1500) }
async function handoff() {
  busy.value = true
  try {
    const { data } = await api.post(`/public/bot/${sid}/handoff`, { name: name.value || auth.user?.fullName })
    if (data.mode === 'ticket') push({ text: `Đã tạo phiếu hỗ trợ ${data.ticket} kèm nội dung trò chuyện. Nhân viên sẽ phản hồi trong giờ làm việc (8:00–22:00).`, ticket: data.ticket })
    else { mode.value = 'human'; await poll() }
  } catch (e) { push({ text: errMsg(e) }) } finally { busy.value = false; scroll() }
}
function reset() { bot.value = [GREET]; quick.value = START; sid = newSid(); localStorage.setItem('chatSession', sid); msgs.value = []; save(); live() }

const lastId = () => msgs.value.length ? msgs.value[msgs.value.length - 1].id : 0
function addMsgs(data) {
  const fresh = data.filter(m => !msgs.value.some(x => x.id === m.id))
  if (!fresh.length) return
  msgs.value.push(...fresh); msgs.value.sort((a, b) => a.id - b.id)
  if (fresh.some(m => m.fromAdmin)) { staffTyping.value = false; if (!open.value) unread.value = true; mode.value = 'human' }
  scroll()
}
async function poll() {
  try { const { data } = await api.get(`/chat/${sid}`, { params: { after: lastId() } }); addMsgs(data) } catch { /* bỏ qua */ }
}
// Thời gian thực qua STOMP: tin nhắn mới + "nhân viên đang gõ…"
const staffTyping = ref(false)
let unsubs = []
function live() {
  unsubs.forEach(u => u()); unsubs = [
    subscribe('/topic/chat/' + sid, m => addMsgs([m])),
    subscribe('/topic/chat/' + sid + '/typing', t => { if (t.who === 'staff') { staffTyping.value = t.on; scroll() } })
  ]
}
let typT
function sendTyping() { if (mode.value !== 'human') return; publish(`/app/chat/${sid}/typing`, { who: 'customer', on: true }); clearTimeout(typT); typT = setTimeout(() => publish(`/app/chat/${sid}/typing`, { who: 'customer', on: false }), 2500) }
async function send() {
  if (!text.value.trim()) return
  try { const { data } = await api.post(`/chat/${sid}`, { content: text.value, customerName: name.value }); text.value = ''; addMsgs([data]); publish(`/app/chat/${sid}/typing`, { who: 'customer', on: false }); if (!rtConnected.value) await poll() } catch (e) { msgs.value.push({ id: Date.now(), fromAdmin: true, content: 'Không gửi được: ' + errMsg(e) }) }
}
const toggle = () => { open.value = !open.value; unread.value = false; scroll() }
const openEv = e => { if (!open.value) toggle(); if (e?.detail === 'human') handoff() }
const aiOn = ref(false)
onMounted(() => { api.get('/public/ai/status').then(r => (aiOn.value = r.data.enabled)).catch(() => {}); poll(); live(); timer = setInterval(() => { if (!rtConnected.value && (open.value || mode.value === 'human')) poll() }, 5000); addEventListener('open-chat', openEv) })
onUnmounted(() => { clearInterval(timer); unsubs.forEach(u => u()); removeEventListener('open-chat', openEv) })
</script>

<style scoped>
.live { align-self: center; font-size: .7rem; color: var(--success, #2e7d32); }
.chat { position: fixed; right: 20px; bottom: 20px; z-index: 110; }
.fab { width: 56px; height: 56px; border-radius: 50%; border: none; background: var(--primary); color: #fff; font-size: 1.3rem; cursor: pointer; box-shadow: 0 8px 24px rgba(0,0,0,.25); position: relative; display: grid; place-items: center; transition: transform .2s ease-out, background .2s ease-out; }
.fab:active { transform: scale(.9); } .fab.opened { background: #111; }
.dot { position: absolute; top: 6px; right: 6px; width: 12px; height: 12px; border-radius: 50%; background: var(--danger); animation: pop .3s ease-out; }
.box { position: absolute; right: 0; bottom: 70px; width: min(380px, calc(100vw - 24px)); height: min(620px, calc(100vh - 120px)); background: var(--surface); color: var(--text); border: 1px solid var(--border); overflow: hidden; box-shadow: 0 24px 60px -16px rgba(0,0,0,.4); display: flex; flex-direction: column; transform-origin: bottom right; }
.head { display: flex; align-items: center; gap: 10px; background: #111; color: #fff; padding: 12px 14px; border-bottom: 3px solid var(--primary); }
.av { position: relative; width: 34px; height: 34px; flex-shrink: 0; display: grid; place-items: center; background: var(--primary); font: 700 1rem 'Oswald', sans-serif; }
.av i { position: absolute; right: -2px; bottom: -2px; width: 10px; height: 10px; border-radius: 50%; background: #777; border: 2px solid #111; } .av i.on { background: #2ecc71; }
.head > div { flex: 1; min-width: 0; } .head b { display: block; font-family: 'Oswald', sans-serif; letter-spacing: .03em; } .head small { color: #bbb; font-size: .72rem; display: block; }
.hb { background: none; border: 1px solid #555; color: #fff; padding: 4px 8px; font: 600 .74rem inherit; font-family: inherit; cursor: pointer; }
.x { background: none; border: none; color: #fff; font-size: 1.1rem; cursor: pointer; width: 36px; height: 36px; }
.msgs { flex: 1; min-height: 0; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 8px; overscroll-behavior: contain; }
.msgs > span { display: contents; }
.hint { text-align: center; margin-top: 30px; }
.msg { max-width: 88%; padding: 9px 12px; font-size: .9rem; line-height: 1.5; white-space: pre-wrap; display: flex; flex-direction: column; gap: 6px; }
.me { align-self: flex-end; background: var(--primary); color: #fff; } .them { align-self: flex-start; background: var(--bg-2); }
.plist, .clist, .olist { display: flex; flex-direction: column; gap: 6px; white-space: normal; }
.pc { display: flex; gap: 8px; align-items: center; background: var(--surface); padding: 6px; border: 1px solid var(--border); color: var(--text); text-decoration: none; }
.pc img { width: 54px; height: 40px; object-fit: cover; flex-shrink: 0; } .pc span { min-width: 0; display: flex; flex-direction: column; } .pc b { font-size: .82rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .pc small, .cp small, .ochip small, .ocard small { color: var(--muted); font-size: .74rem; }
.cp { display: grid; grid-template-columns: 1fr auto; text-align: left; gap: 0 8px; padding: 8px 10px; border: 1px dashed var(--primary); background: var(--surface); color: var(--text); cursor: pointer; font-family: inherit; }
.cp b { font-family: 'Be Vietnam Pro', monospace; letter-spacing: .05em; } .cp em { grid-row: 1 / 3; grid-column: 2; align-self: center; font-style: normal; font-size: .74rem; color: var(--primary); font-weight: 700; }
.ocard { background: var(--surface); border: 1px solid var(--border); padding: 8px 10px; white-space: normal; display: flex; flex-direction: column; gap: 3px; } .ocard ul { margin: 2px 0; padding-left: 16px; font-size: .8rem; }
.ochip { display: flex; flex-direction: column; text-align: left; padding: 8px 10px; border: 1px solid var(--border); background: var(--surface); color: var(--text); cursor: pointer; font-family: inherit; }
.hand { align-self: flex-start; }
.typing { flex-direction: row; gap: 4px; padding: 12px 14px; } .typing i { width: 7px; height: 7px; border-radius: 50%; background: var(--muted); animation: blink 1s infinite ease-in-out; } .typing i:nth-child(2) { animation-delay: .15s; } .typing i:nth-child(3) { animation-delay: .3s; }
.chips { display: flex; gap: 6px; overflow-x: auto; padding: 8px 12px; border-top: 1px solid var(--border); scrollbar-width: none; }
.chip { flex-shrink: 0; border: 1px solid var(--primary); background: transparent; color: var(--text); padding: 6px 10px; font: 600 .78rem inherit; font-family: inherit; cursor: pointer; white-space: nowrap; transition: transform .12s ease-out; }
.chip:active { transform: scale(.94); background: var(--primary); color: #fff; }
.namebox { padding: 0 12px 8px; }
.send { display: flex; gap: 6px; padding: 8px 12px 10px; } .send .input { flex: 1; min-width: 0; padding: 8px 12px; }
.call { text-align: center; padding: 8px; font-size: .85rem; border-top: 1px solid var(--border); }
.cw-enter-active { transition: opacity .22s ease-out, transform .22s cubic-bezier(.2,.8,.2,1); } .cw-leave-active { transition: opacity .15s ease-out, transform .15s ease-out; }
.cw-enter-from, .cw-leave-to { opacity: 0; transform: translateY(14px) scale(.96); }
.msg-enter-active { transition: opacity .2s ease-out, transform .2s ease-out; } .msg-enter-from { opacity: 0; transform: translateY(8px); }
.ic-enter-active, .ic-leave-active { transition: opacity .12s, transform .12s ease-out; } .ic-enter-from { opacity: 0; transform: rotate(-60deg) scale(.6); } .ic-leave-to { opacity: 0; transform: rotate(60deg) scale(.6); }
@keyframes blink { 0%, 80%, 100% { opacity: .3; transform: translateY(0); } 40% { opacity: 1; transform: translateY(-3px); } }
@keyframes pop { from { transform: scale(0); } }
@media (max-width: 899px) { :global(body:has(.mbar, .buybar)) .chat { bottom: calc(140px + env(safe-area-inset-bottom)); } .chat { right: 14px; bottom: calc(72px + env(safe-area-inset-bottom)); } .fab { width: 50px; height: 50px; } .box { position: fixed; left: 8px; right: 8px; width: auto; bottom: calc(132px + env(safe-area-inset-bottom)); height: calc(100vh - 210px); max-height: 620px; } }
@media (prefers-reduced-motion: reduce) { .cw-enter-active, .cw-leave-active, .msg-enter-active, .ic-enter-active, .ic-leave-active { transition: none; } .typing i, .dot { animation: none; } }
.ai-tag { margin-left: 6px; font-size: 10px; padding: 1px 5px; border-radius: 4px; background: var(--accent, #f15a24); color: #fff; vertical-align: middle }
</style>
