<template>
  <div class="chat">
    <transition name="cw">
      <section v-if="open" class="box" role="dialog" aria-label="Trợ lý hỗ trợ ARES">
        <header class="head">
          <div><b>{{ mode === 'human' ? 'Chat với nhân viên' : 'Trợ lý ARES' }}</b><small>{{ mode === 'human' ? 'Thường phản hồi trong vài phút' : 'Trả lời tự động 24/7 · có thể chuyển nhân viên' }}</small></div>
          <button v-if="mode === 'human'" class="hb" title="Quay lại trợ lý" @click="mode = 'bot'">Trợ lý</button>
          <button class="x" aria-label="Đóng" @click="open = false">✕</button>
        </header>
        <div ref="list" class="msgs" aria-live="polite">
          <template v-if="mode === 'bot'">
            <div v-for="(m, i) in bot" :key="i" :class="['msg', m.me ? 'me' : 'them']">
              <span v-html="m.html || esc(m.text)" />
              <div v-if="m.orders" class="olist">
                <button v-for="o in m.orders" :key="o.code" class="ochip" @click="showOrder(o)"><b>{{ o.code }}</b><small>{{ ORDER_STATUS[o.status] }} · {{ vnd(o.total) }}</small></button>
              </div>
            </div>
            <form v-if="step === 'lookup'" class="lookup" @submit.prevent="lookup">
              <input v-model="lk.code" class="input" placeholder="Mã đơn (VD: TS2610…)" aria-label="Mã đơn" required />
              <input v-model="lk.phone" class="input" placeholder="Số điện thoại đặt hàng" aria-label="Số điện thoại" inputmode="tel" required />
              <button class="btn btn-sm" :disabled="busy">{{ busy ? 'Đang tra…' : 'Tra cứu' }}</button>
            </form>
          </template>
          <template v-else>
            <div v-if="!msgs.length" class="muted small hint">Nhân viên ARES sẽ trả lời ngay. Bạn có thể mô tả vấn đề bên dưới.</div>
            <div v-for="m in msgs" :key="m.id" :class="['msg', m.fromAdmin ? 'them' : 'me']">{{ m.content }}</div>
          </template>
        </div>
        <div v-if="mode === 'bot'" class="chips">
          <button v-for="c in quick" :key="c.k" class="chip" @click="pick(c)">{{ c.label }}</button>
        </div>
        <div v-if="mode === 'human' && !auth.loggedIn && !msgs.length" class="namebox"><input v-model="name" class="input" placeholder="Tên của bạn" aria-label="Tên của bạn" /></div>
        <form class="send" @submit.prevent="mode === 'bot' ? ask() : send()">
          <input v-model="text" class="input" :placeholder="mode === 'bot' ? 'Hỏi về ship, đổi trả, bảo hành…' : 'Nhập tin nhắn…'" aria-label="Tin nhắn" />
          <button class="btn btn-sm">Gửi</button>
        </form>
        <a class="call" :href="`tel:${HOTLINE.replace(/ /g, '')}`">Gọi hotline {{ HOTLINE }}</a>
      </section>
    </transition>
    <button class="fab" :aria-label="open ? 'Đóng hỗ trợ' : 'Mở trợ lý hỗ trợ'" @click="toggle"><span v-if="open">✕</span><svg v-else viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z"/></svg><span v-if="unread && !open" class="dot" /></button>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { useAuth } from '../stores/auth'
import { HOTLINE, ORDER_STATUS, PAY_STATUS, vnd, dt } from '../format'
import { FAQS } from '../faqs'

const auth = useAuth(), router = useRouter()
const open = ref(false), text = ref(''), name = ref(''), msgs = ref([]), unread = ref(false), list = ref(), mode = ref('bot'), step = ref(''), busy = ref(false)
const lk = reactive({ code: '', phone: '' })
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
const bot = ref([{ text: 'Xin chào! Mình là trợ lý ARES. Chọn một chủ đề bên dưới hoặc gõ câu hỏi nhé.' }])
const quick = [
  { k: 'order', label: 'Tra cứu đơn hàng' }, { k: 'ship', label: 'Phí ship & giao hàng' }, { k: 'return', label: 'Đổi trả' },
  { k: 'pay', label: 'Thanh toán' }, { k: 'warranty', label: 'Bảo hành' }, { k: 'human', label: 'Gặp nhân viên' }]
let sid = localStorage.getItem('chatSession')
if (!sid) { sid = 'c' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8); localStorage.setItem('chatSession', sid) }
let timer
const scroll = () => nextTick(() => { if (list.value) list.value.scrollTop = list.value.scrollHeight })
const say = (m) => { bot.value.push(m); scroll() }
const faq = q => FAQS.find(f => f[1] === q)

// Trả lời tự do: so khớp từ khoá với FAQ (bỏ dấu), không khớp thì đề nghị chuyển nhân viên
const norm = s => s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')
const KEYS = [[/don|ma don|trang thai|giao toi dau|kiem tra/, 'order'], [/ship|van chuyen|giao hang|bao lau|phi/, 'ship'], [/doi|tra hang|hoan tien|loi/, 'return'],
  [/thanh toan|momo|vnpay|tra gop|chuyen khoan|cod/, 'pay'], [/bao hanh/, 'warranty'], [/nhan vien|nguoi that|tu van|gap/, 'human']]
function ask() {
  const q = text.value.trim(); if (!q) return; text.value = ''; say({ me: true, text: q })
  const n = norm(q)
  const hit = KEYS.find(([re]) => re.test(n))
  if (hit) return pick(quick.find(c => c.k === hit[0]), true)
  const words = n.split(/\s+/).filter(w => w.length > 2)
  const best = FAQS.map(f => [f, words.filter(w => norm(f[1] + ' ' + f[2]).includes(w)).length]).sort((a, b) => b[1] - a[1])[0]
  if (best && best[1] >= 2) say({ html: `<b>${esc(best[0][1])}</b><br>${esc(best[0][2])}` })
  else say({ html: 'Mình chưa chắc câu trả lời. Bạn có muốn <b>gặp nhân viên</b> không? Bấm “Gặp nhân viên” bên dưới, hoặc <a href="/support">tạo phiếu hỗ trợ</a>.' })
}
async function pick(c, silent) {
  if (!silent) say({ me: true, text: c.label })
  step.value = ''
  if (c.k === 'ship') { const f = faq('Phí ship bao nhiêu?'), g = faq('Bao lâu nhận được hàng?'); say({ html: `${esc(f[2])}<br><br>${esc(g[2])}` }) }
  else if (c.k === 'return') say({ html: `${esc(faq('Chính sách đổi trả?')[2])}<br><br>Mở đơn hàng trong <a href="/account?tab=orders">Tài khoản › Đơn hàng</a> và bấm <b>Yêu cầu đổi trả</b> để dùng trình hướng dẫn 4 bước.` })
  else if (c.k === 'pay') say({ text: faq('Có những hình thức thanh toán nào?')[2] })
  else if (c.k === 'warranty') say({ text: faq('Tra cứu bảo hành ở đâu?')[2] })
  else if (c.k === 'human') handoff()
  else if (c.k === 'order') {
    if (auth.loggedIn) {
      try {
        const { data } = await api.get('/account/orders')
        if (data.length) return say({ text: 'Đây là các đơn gần nhất của bạn — chọn một đơn để xem trạng thái:', orders: data.slice(0, 4) })
      } catch { /* rơi xuống tra cứu thủ công */ }
    }
    step.value = 'lookup'; say({ text: 'Nhập mã đơn và số điện thoại đặt hàng để tra cứu:' })
  }
}
function showOrder(o) {
  const tl = Object.entries(o.timeline || {}).map(([k, v]) => `${ORDER_STATUS[k] || k}: ${dt(v)}`).join('<br>')
  say({ html: `Đơn <b>${esc(o.code)}</b>: <b>${ORDER_STATUS[o.status]}</b> · ${PAY_STATUS[o.paymentStatus] || ''} · ${vnd(o.total)}${tl ? '<br><small>' + tl + '</small>' : ''}<br><a href="/orders/${encodeURIComponent(o.code)}">Xem chi tiết đơn →</a>` })
}
async function lookup() {
  busy.value = true
  try { const { data } = await api.get('/public/order-status', { params: { code: lk.code.trim(), phone: lk.phone.trim() } }); step.value = ''; showOrder(data) }
  catch (e) { say({ text: errMsg(e) + '. Kiểm tra lại mã đơn/số điện thoại, hoặc bấm “Gặp nhân viên”.' }) }
  finally { busy.value = false }
}
async function handoff() {
  mode.value = 'human'
  const ctx = bot.value.filter(m => m.me).map(m => m.text).slice(-4).join(' / ')
  if (ctx && !msgs.value.length) { try { await api.post(`/chat/${sid}`, { content: `[Từ trợ lý ảo] Khách cần hỗ trợ: ${ctx}`, customerName: name.value }); await poll() } catch {} }
  scroll()
}

const lastId = () => msgs.value.length ? msgs.value[msgs.value.length - 1].id : 0
async function poll() {
  try {
    const { data } = await api.get(`/chat/${sid}`, { params: { after: lastId() } })
    if (data.length) { msgs.value.push(...data); if (data.some(m => m.fromAdmin)) { if (!open.value) unread.value = true; if (mode.value === 'bot' && msgs.value.length > 1) mode.value = 'human' } scroll() }
  } catch { /* bỏ qua */ }
}
async function send() {
  if (!text.value.trim()) return
  await api.post(`/chat/${sid}`, { content: text.value, customerName: name.value })
  text.value = ''; await poll()
}
const toggle = () => { open.value = !open.value; unread.value = false; scroll() }
const openEv = e => { if (!open.value) toggle(); if (e?.detail === 'human') handoff() }
// chặn link nội bộ trong câu trả lời: điều hướng SPA
const clickLink = e => { const a = e.target.closest?.('.msgs a[href^="/"]'); if (a) { e.preventDefault(); router.push(a.getAttribute('href')) } }
onMounted(() => { poll(); timer = setInterval(poll, 5000); addEventListener('open-chat', openEv); document.addEventListener('click', clickLink) })
onUnmounted(() => { clearInterval(timer); removeEventListener('open-chat', openEv); document.removeEventListener('click', clickLink) })
</script>

<style scoped>
.chat { position: fixed; right: 20px; bottom: 20px; z-index: 110; }
.fab { width: 56px; height: 56px; border-radius: 50%; border: none; background: var(--primary); color: #fff; font-size: 1.3rem; cursor: pointer; box-shadow: 0 8px 24px rgba(0,0,0,.25); position: relative; display: grid; place-items: center; }
.dot { position: absolute; top: 6px; right: 6px; width: 12px; height: 12px; border-radius: 50%; background: var(--danger); }
.box { position: absolute; right: 0; bottom: 70px; width: min(370px, calc(100vw - 24px)); max-height: min(620px, calc(100vh - 120px)); background: var(--surface); color: var(--text); border: 1px solid var(--border); overflow: hidden; box-shadow: 0 24px 60px -16px rgba(0,0,0,.4); display: flex; flex-direction: column; }
.head { display: flex; align-items: center; gap: 8px; background: #111; color: #fff; padding: 12px 14px; border-bottom: 3px solid var(--primary); }
.head > div { flex: 1; min-width: 0; } .head b { display: block; font-family: 'Oswald', sans-serif; letter-spacing: .03em; } .head small { color: #bbb; font-size: .74rem; }
.hb { background: none; border: 1px solid #555; color: #fff; padding: 4px 8px; font: 600 .74rem inherit; font-family: inherit; cursor: pointer; }
.x { background: none; border: none; color: #fff; font-size: 1.1rem; cursor: pointer; width: 36px; height: 36px; }
.msgs { flex: 1; min-height: 220px; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.hint { text-align: center; margin-top: 30px; }
.msg { max-width: 86%; padding: 9px 12px; font-size: .9rem; line-height: 1.5; white-space: pre-wrap; }
.msg :deep(a) { color: var(--primary); font-weight: 600; } .msg :deep(small) { color: var(--muted); }
.me { align-self: flex-end; background: var(--primary); color: #fff; }
.them { align-self: flex-start; background: var(--bg-2); }
.olist { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; white-space: normal; }
.ochip { display: flex; flex-direction: column; text-align: left; padding: 8px 10px; border: 1px solid var(--border); background: var(--surface); color: var(--text); cursor: pointer; font-family: inherit; }
.ochip small { color: var(--muted); } .ochip:hover { border-color: var(--primary); }
.lookup { display: flex; flex-direction: column; gap: 6px; align-self: stretch; }
.chips { display: flex; gap: 6px; overflow-x: auto; padding: 8px 12px; border-top: 1px solid var(--border); scrollbar-width: none; }
.chip { flex-shrink: 0; border: 1px solid var(--primary); background: transparent; color: var(--text); padding: 6px 10px; font: 600 .78rem inherit; font-family: inherit; cursor: pointer; white-space: nowrap; }
.chip:hover { background: var(--primary); color: #fff; }
.namebox { padding: 0 12px 8px; }
.send { display: flex; gap: 6px; padding: 8px 12px 10px; } .send .input { flex: 1; min-width: 0; padding: 8px 12px; }
.call { text-align: center; padding: 8px; font-size: .85rem; border-top: 1px solid var(--border); }
.cw-enter-active, .cw-leave-active { transition: opacity .18s ease-out, transform .18s ease-out; } .cw-enter-from, .cw-leave-to { opacity: 0; transform: translateY(10px); }
@media (max-width: 899px) { :global(body:has(.mbar, .buybar)) .chat { bottom: calc(140px + env(safe-area-inset-bottom)); } .chat { right: 14px; bottom: calc(72px + env(safe-area-inset-bottom)); } .fab { width: 50px; height: 50px; } .box { position: fixed; left: 8px; right: 8px; width: auto; bottom: calc(132px + env(safe-area-inset-bottom)); max-height: calc(100vh - 210px); } }
@media (prefers-reduced-motion: reduce) { .cw-enter-active, .cw-leave-active { transition: none; } }
</style>
