<template>
  <transition name="cp">
    <div v-if="open" class="cp-bg" @click.self="close">
      <div class="cp" role="dialog" aria-modal="true" aria-label="Tìm nhanh (Ctrl+K)">
        <div class="cp-in">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
          <input ref="inp" v-model="q" placeholder="Tìm trang, đơn hàng, sản phẩm, khách hàng…" aria-label="Tìm nhanh" role="combobox" aria-expanded="true" aria-controls="cp-list" :aria-activedescendant="'cp-' + hl"
            @keydown.down.prevent="move(1)" @keydown.up.prevent="move(-1)" @keydown.enter.prevent="go(flat[hl])" @keydown.esc="close" @input="search" />
          <kbd>Esc</kbd>
        </div>
        <ul id="cp-list" class="cp-list" role="listbox">
          <template v-for="g in groups" :key="g.name">
            <li v-if="g.items.length" class="cp-g" role="presentation">{{ g.name }}</li>
            <li v-for="it in g.items" :id="'cp-' + it.i" :key="g.name + it.key" role="option" :aria-selected="it.i === hl" :class="['cp-it', { hl: it.i === hl }]" @mousemove="hl = it.i" @click="go(it)">
              <img v-if="it.img" :src="it.img" alt="" /><span v-else class="cp-ic">{{ it.ic }}</span>
              <span class="grow"><b>{{ it.label }}</b><small v-if="it.sub">{{ it.sub }}</small></span><span class="cp-k">{{ it.kind }}</span>
            </li>
          </template>
          <li v-if="!flat.length" class="cp-none">Không có kết quả cho “{{ q }}”</li>
        </ul>
        <div class="cp-foot"><span><kbd>↑</kbd><kbd>↓</kbd> chọn</span><span><kbd>Enter</kbd> mở</span><span><kbd>Ctrl</kbd>+<kbd>K</kbd> bật/tắt</span></div>
      </div>
    </div>
  </transition>
</template>
<script setup>
// Bảng lệnh Ctrl+K cho trang quản trị: điều hướng + tìm đơn/sản phẩm/khách
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { vnd, ORDER_STATUS } from '../format'
const props = defineProps({ pages: { type: Array, default: () => [] } })
const router = useRouter()
const open = ref(false), q = ref(''), hl = ref(0), inp = ref(), res = ref({ orders: [], products: [], customers: [] })
const norm = s => (s || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')
const ACTIONS = [{ label: 'In nhãn vận chuyển đơn chờ giao', to: '/admin/orders?status=CONFIRMED&labels=1', ic: '⎙' }, { label: 'Nhập sản phẩm từ CSV', to: '/admin/products?import=1', ic: '⇪' }, { label: 'Tạo sản phẩm mới', to: '/admin/products?new=1', ic: '+' }]
const groups = computed(() => {
  const t = norm(q.value.trim()); let i = 0
  const pg = props.pages.filter(p => !t || norm(p.label).includes(t)).slice(0, t ? 6 : 8).map(p => ({ key: p.to, label: p.label, to: p.to, ic: '→', kind: 'Trang' }))
  const ac = ACTIONS.filter(a => !t || norm(a.label).includes(t)).map(a => ({ key: a.to, ...a, kind: 'Lệnh' }))
  const out = [{ name: 'Trang', items: pg }, { name: 'Lệnh nhanh', items: ac },
    { name: 'Đơn hàng', items: res.value.orders.map(o => ({ key: o.id, label: o.code, sub: `${o.buyerName} · ${vnd(o.total)} · ${ORDER_STATUS[o.status]}`, to: `/admin/orders?q=${o.code}`, ic: '#', kind: 'Đơn' })) },
    { name: 'Sản phẩm', items: res.value.products.map(p => ({ key: p.id, label: p.name, sub: `${p.sku} · tồn ${p.stock} · ${vnd(p.price)}`, img: p.images?.[0], to: `/admin/products?q=${encodeURIComponent(p.sku)}`, kind: 'SP' })) },
    { name: 'Khách hàng', items: res.value.customers.map(c => ({ key: c.user.id, label: c.user.fullName, sub: `${c.user.email} · ${c.orderCount} đơn`, to: `/admin/customers?q=${encodeURIComponent(c.user.email)}`, ic: '@', kind: 'Khách' })) }]
  out.forEach(g => g.items.forEach(it => (it.i = i++)))
  return out
})
const flat = computed(() => groups.value.flatMap(g => g.items))
const move = d => { const n = flat.value.length; if (n) hl.value = (hl.value + d + n) % n }
let timer, customers
const search = () => {
  hl.value = 0; clearTimeout(timer); const t = q.value.trim()
  if (t.length < 2) { res.value = { orders: [], products: [], customers: [] }; return }
  timer = setTimeout(async () => {
    const [o, p] = await Promise.all([api.get('/admin/orders', { params: { q: t } }).catch(() => ({ data: [] })), api.get('/admin/products', { params: { q: t, size: 5 } }).catch(() => ({ data: { content: [] } }))])
    customers ||= (await api.get('/admin/customers').catch(() => ({ data: [] }))).data
    const n = norm(t)
    if (q.value.trim() === t) res.value = { orders: o.data.slice(0, 5), products: p.data.content.slice(0, 5), customers: customers.filter(c => norm(c.user.fullName + ' ' + c.user.email + ' ' + (c.user.phone || '')).includes(n)).slice(0, 5) }
  }, 160)
}
const go = it => { if (!it) return; close(); router.push(it.to) }
const close = () => { open.value = false; q.value = ''; res.value = { orders: [], products: [], customers: [] } }
const toggle = async () => { open.value = !open.value; if (open.value) { hl.value = 0; await nextTick(); inp.value?.focus() } }
const key = e => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); toggle() } }
onMounted(() => document.addEventListener('keydown', key))
onUnmounted(() => document.removeEventListener('keydown', key))
watch(hl, () => nextTick(() => document.getElementById('cp-' + hl.value)?.scrollIntoView({ block: 'nearest' })))
defineExpose({ toggle })
</script>
<style scoped>
.cp-bg { position: fixed; inset: 0; z-index: 400; background: rgba(10,10,10,.45); display: flex; justify-content: center; align-items: flex-start; padding: 12vh 12px 12px; }
.cp { width: min(640px, 100%); background: var(--surface); color: var(--text); box-shadow: 0 30px 80px rgba(0,0,0,.4); border-top: 4px solid var(--accent); display: flex; flex-direction: column; max-height: 70vh; }
.cp-in { display: flex; align-items: center; gap: 10px; padding: 0 14px; border-bottom: 1px solid var(--border); }
.cp-in input { flex: 1; min-width: 0; height: 54px; border: none; outline: none; background: none; color: var(--text); font: 500 1.02rem inherit; font-family: inherit; }
kbd { font: 600 .7rem/1 ui-monospace, monospace; padding: 3px 6px; border: 1px solid var(--border); border-bottom-width: 2px; background: var(--bg-2); color: var(--text-2); }
.cp-list { list-style: none; overflow-y: auto; padding: 6px; flex: 1; }
.cp-g { padding: 10px 10px 4px; font: 600 .68rem/1 'Oswald', sans-serif; letter-spacing: .14em; text-transform: uppercase; color: var(--muted); }
.cp-it { display: flex; align-items: center; gap: 10px; padding: 8px 10px; cursor: pointer; }
.cp-it.hl { background: var(--bronze-soft); box-shadow: inset 3px 0 var(--accent); }
.cp-it img { width: 40px; height: 30px; object-fit: cover; } .cp-ic { width: 30px; height: 30px; display: grid; place-items: center; background: var(--bg-2); font-weight: 700; flex-shrink: 0; }
.cp-it b { display: block; font-size: .9rem; font-weight: 600; } .cp-it small { color: var(--muted); font-size: .78rem; }
.grow { flex: 1; min-width: 0; } .grow b, .grow small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.cp-k { font-size: .7rem; color: var(--muted); }
.cp-none { padding: 18px; color: var(--muted); text-align: center; }
.cp-foot { display: flex; gap: 16px; padding: 8px 14px; border-top: 1px solid var(--border); font-size: .74rem; color: var(--muted); }
.cp-enter-active, .cp-leave-active { transition: opacity .15s ease-out; } .cp-enter-from, .cp-leave-to { opacity: 0; }
@media (max-width: 600px) { .cp-foot { display: none; } .cp-bg { padding-top: 8px; } }
</style>
