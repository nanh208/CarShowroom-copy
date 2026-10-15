<template>
  <div>
    <div class="row between"><h1>Xuất kho &amp; giao vận</h1><button class="btn btn-sm btn-outline" @click="load">Làm mới</button></div>
    <p class="muted small">Quy trình: <b>Nhặt hàng</b> (quét SKU hoặc đánh dấu) → <b>Đóng gói</b> (cân, hộp) → <b>Bàn giao</b> GHN/GHTK (báo giá, tạo vận đơn, đơn chuyển sang “Đang giao”).</p>
    <div class="tabs flat" style="margin:12px 0"><button v-for="(v, k) in STAGES" :key="k" :class="{ active: tab === k }" @click="tab = k">{{ v }} <small>({{ count(k) }})</small></button></div>
    <div v-if="loading" class="sk" style="height:220px" />
    <p v-else-if="err" class="alert alert-error">{{ err }} <button class="btn btn-sm btn-outline" @click="load">Thử lại</button></p>
    <div v-else class="layout">
      <div class="list">
        <div v-if="!shown.length" class="empty">Không có đơn ở bước này.</div>
        <button v-for="o in shown" :key="o.orderId" :class="['oc', { on: cur?.orderId === o.orderId }]" @click="pick(o)">
          <div class="row between"><b class="mono">{{ o.code }}</b><span :class="['badge', 'st-' + o.stage]">{{ STAGES[o.stage] || o.stage }}</span></div>
          <small>{{ o.buyerName }} · {{ o.city }}</small>
          <div class="bar"><i :style="{ width: (o.stage === 'NEW' ? 0 : ['PICKING'].includes(o.stage) ? o.pickProgress / 3 : o.stage === 'PICKED' ? 40 : o.stage === 'PACKED' ? 70 : 100) + '%' }" /></div>
          <small class="muted">{{ o.items.length }} dòng · ~{{ (o.estWeight / 1000).toFixed(1) }} kg<template v-if="o.cod"> · COD <span class="money">{{ vnd(o.cod) }}</span></template></small>
        </button>
      </div>
      <div class="card panel">
        <div v-if="!cur" class="empty">Chọn một đơn để xử lý</div>
        <template v-else>
          <div class="row between"><h3 class="mono">{{ cur.code }}</h3><router-link :to="`/admin/orders?q=${cur.code}`" class="small">Xem đơn →</router-link></div>
          <p class="small">{{ cur.buyerName }} · {{ cur.phone }}<br />{{ cur.address }}</p>
          <p v-if="cur.giftWrap" class="small gift">🎁 Gói quà</p>
          <ol class="steps"><li v-for="(s, i) in ['Nhặt hàng', 'Đóng gói', 'Bàn giao']" :key="s" :class="{ done: stepIdx > i, now: stepIdx === i }">{{ s }}</li></ol>

          <button v-if="cur.stage === 'NEW'" class="btn" :disabled="busy" @click="act('start')">Bắt đầu nhặt hàng</button>

          <template v-if="['PICKING', 'PICKED'].includes(cur.stage)">
            <form class="row scan" @submit.prevent="scan"><input ref="scanIn" v-model.trim="sku" class="input grow mono" placeholder="Quét / nhập SKU rồi Enter" aria-label="Quét SKU" /><button class="btn btn-sm">Nhặt</button></form>
            <ul class="items">
              <li v-for="it in cur.items" :key="it.id" :class="{ ok: it.picked >= it.qty }">
                <img :src="it.image" alt="" /><div class="grow"><b>{{ it.name }}</b><small>{{ it.variant }} · SKU <span class="mono">{{ it.sku || '—' }}</span></small></div>
                <span class="q">{{ it.picked }}/{{ it.qty }}</span>
                <button class="btn btn-sm btn-outline" :disabled="busy || it.picked >= it.qty" @click="act('pick', { itemId: it.id, qty: 1 })">+1</button>
                <button class="btn btn-sm btn-outline" :disabled="busy || !it.picked" aria-label="Bỏ 1" @click="act('pick', { itemId: it.id, qty: -1 })">−</button>
              </li>
            </ul>
          </template>

          <form v-if="cur.stage === 'PICKED'" class="pack" @submit.prevent="act('pack', pk)">
            <b>Đóng gói</b>
            <label>Khối lượng (g)<input v-model.number="pk.weightGrams" type="number" min="50" max="30000" class="input" required /></label>
            <label>Cỡ hộp<select v-model="pk.boxSize" class="input"><option v-for="b in ['S', 'M', 'L', 'XL']" :key="b">{{ b }}</option></select></label>
            <label>Số kiện<input v-model.number="pk.packages" type="number" min="1" max="10" class="input" /></label>
            <button class="btn" :disabled="busy">Xác nhận đóng gói</button>
          </form>

          <div v-if="cur.stage === 'PACKED'" class="rates">
            <b>Chọn hãng vận chuyển</b> <small class="muted">({{ cur.shipment.weightGrams }} g · hộp {{ cur.shipment.boxSize }}<template v-if="cur.carrierPref"> · khách chọn {{ cur.carrierPref }}</template>)</small>
            <div v-if="!rates.length" class="sk" style="height:80px" />
            <label v-for="r in rates" :key="r.carrier + r.service" :class="['rate', { on: sel === r.carrier + '|' + r.service }]">
              <input v-model="sel" type="radio" :value="r.carrier + '|' + r.service" /><div class="grow"><b>{{ r.label }}</b><small>Giao {{ d(r.etaFrom) }}–{{ d(r.etaTo) }}<template v-if="r.codFee"> · phí COD {{ vnd(r.codFee) }}</template></small></div><b class="money">{{ vnd(r.fee) }}</b>
            </label>
            <button class="btn" :disabled="busy || !sel" @click="ship">Tạo vận đơn &amp; bàn giao</button>
          </div>

          <div v-if="['SHIPPED', 'DELIVERED'].includes(cur.stage)" class="done-box">
            <p>Đã bàn giao <b>{{ cur.shipment.carrier }}</b> · vận đơn <b class="mono">{{ cur.shipment.trackingNo }}</b> · phí <span class="money">{{ vnd(cur.shipment.fee) }}</span></p>
            <p class="small muted">Nhặt: {{ cur.shipment.pickedBy }} · Đóng gói: {{ cur.shipment.packedBy }} ({{ dt(cur.shipment.packedAt) }}) · Bàn giao: {{ cur.shipment.shippedBy }} ({{ dt(cur.shipment.shippedAt) }})</p>
            <div class="row" style="gap:8px;flex-wrap:wrap">
              <router-link :to="`/track/${cur.code}`" class="btn btn-sm btn-outline" target="_blank">Trang theo dõi</router-link>
              <button v-if="cur.stage === 'SHIPPED'" class="btn btn-sm" :disabled="busy" @click="act('simulate-delivered')">Giả lập hãng báo “đã giao”</button>
              <span v-else class="badge badge-COMPLETED">Đã giao thành công</span>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, d, dt } from '../../format'
import { toast } from '../../stores/prefs'
const STAGES = { NEW: 'Chờ xử lý', PICKING: 'Đang nhặt', PICKED: 'Chờ đóng gói', PACKED: 'Chờ bàn giao', SHIPPED: 'Đã bàn giao', DELIVERED: 'Đã giao' }
const list = ref([]), cur = ref(null), tab = ref('NEW'), loading = ref(true), err = ref(''), busy = ref(false), sku = ref(''), scanIn = ref()
const pk = ref({ weightGrams: 500, boxSize: 'M', packages: 1 }), rates = ref([]), sel = ref('')
const count = k => list.value.filter(o => o.stage === k).length
const shown = computed(() => list.value.filter(o => o.stage === tab.value))
const stepIdx = computed(() => ({ NEW: 0, PICKING: 0, PICKED: 1, PACKED: 2, SHIPPED: 3, DELIVERED: 3 })[cur.value?.stage] ?? 0)
async function load() {
  loading.value = true; err.value = ''
  try { list.value = (await api.get('/admin/fulfillment')).data; if (cur.value) cur.value = list.value.find(o => o.orderId === cur.value.orderId) || cur.value; if (!count(tab.value)) tab.value = Object.keys(STAGES).find(k => count(k)) || 'NEW' }
  catch (e) { err.value = errMsg(e) } finally { loading.value = false }
}
function set(o) {
  cur.value = o; const i = list.value.findIndex(x => x.orderId === o.orderId); if (i >= 0) list.value[i] = o
  if (o.stage === 'PICKED') pk.value.weightGrams = o.estWeight
  if (o.stage === 'PACKED') { rates.value = []; sel.value = ''; api.get(`/admin/fulfillment/${o.orderId}/rates`).then(r => { rates.value = r.data; const pref = r.data.find(x => x.carrier === o.carrierPref && x.service === o.shippingMethod) || r.data[0]; sel.value = pref ? pref.carrier + '|' + pref.service : '' }) }
  if (['PICKING', 'PICKED'].includes(o.stage)) nextTick(() => scanIn.value?.focus())
}
const pick = o => set(o)
async function act(path, body = {}) {
  busy.value = true
  try { const { data } = await api.post(`/admin/fulfillment/${cur.value.orderId}/${path}`, body); if (path === 'simulate-delivered') { toast('Hãng báo giao thành công — đơn đã hoàn tất'); await load(); set(list.value.find(o => o.orderId === cur.value.orderId) || cur.value) } else { set(data); tab.value = data.stage } }
  catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
async function scan() { if (!sku.value) return; await act('pick', { sku: sku.value }); sku.value = '' }
async function ship() { const [carrier, service] = sel.value.split('|'); await act('ship', { carrier, service }); toast('Đã tạo vận đơn ' + (cur.value.shipment?.trackingNo || '')) }
onMounted(load)
</script>
<style scoped>
.layout { display: grid; grid-template-columns: 320px 1fr; gap: 16px; align-items: start; }
.list { display: flex; flex-direction: column; gap: 8px; max-height: calc(100vh - 220px); overflow-y: auto; }
.oc { text-align: left; background: var(--card); border: 1px solid var(--border); padding: 10px 12px; display: flex; flex-direction: column; gap: 4px; color: var(--text); font-family: inherit; cursor: pointer; }
.oc.on { border-color: var(--accent); box-shadow: inset 3px 0 0 var(--accent); }
.bar { height: 4px; background: var(--border); } .bar i { display: block; height: 100%; background: var(--accent); transition: width .3s ease-out; }
.panel { display: flex; flex-direction: column; gap: 12px; } .gift { color: var(--accent); }
.steps { display: flex; gap: 6px; list-style: none; padding: 0; margin: 0; counter-reset: s; } .steps li { flex: 1; padding: 6px 8px; border: 1px solid var(--border); font-size: .82rem; counter-increment: s; }
.steps li::before { content: counter(s) '. '; } .steps li.now { border-color: var(--accent); color: var(--accent); font-weight: 600; } .steps li.done { background: var(--bronze-soft); }
.scan { gap: 8px; flex-wrap: nowrap; } .items { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.items li { display: flex; align-items: center; gap: 8px; padding: 6px; border: 1px solid var(--border); } .items li.ok { background: color-mix(in srgb, var(--success, #2e7d32) 8%, transparent); }
.items img { width: 44px; height: 44px; object-fit: cover; } .items small { display: block; color: var(--muted); } .q { font-weight: 700; min-width: 40px; text-align: center; }
.pack { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; align-items: end; } .pack b, .pack .btn { grid-column: 1 / -1; } .pack label { display: flex; flex-direction: column; gap: 4px; font-size: .85rem; }
.rates { display: flex; flex-direction: column; gap: 6px; } .rate { display: flex; align-items: center; gap: 10px; border: 1px solid var(--border); padding: 8px 10px; cursor: pointer; } .rate.on { border-color: var(--accent); } .rate small { display: block; color: var(--muted); }
.st-NEW { background: var(--bronze-soft); } .st-SHIPPED, .st-DELIVERED { background: color-mix(in srgb, var(--success, #2e7d32) 15%, transparent); }
@media (max-width: 860px) { .layout { grid-template-columns: 1fr; } .list { max-height: none; } .pack { grid-template-columns: 1fr; } .items li { flex-wrap: wrap; } }
</style>
