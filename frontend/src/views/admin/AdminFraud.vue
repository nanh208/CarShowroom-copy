<template>
  <div>
    <div class="row between wrap"><h1>Rủi ro gian lận</h1><button class="btn btn-sm btn-outline" :disabled="busy" @click="rescan">Chấm điểm lại đơn đang mở</button></div>
    <p class="muted small">Mỗi đơn mới được chấm điểm 0–100 theo các dấu hiệu (giá trị, COD lớn, khách mới, tần suất theo SĐT/IP, email dùng một lần, số lượng lớn…). Điểm ≥ {{ d?.reviewAt ?? 60 }} vào hàng chờ duyệt: hệ thống chặn xác nhận, giao hàng và in nhãn cho tới khi duyệt. Điểm ≥ {{ d?.watchAt ?? 30 }} được đánh dấu theo dõi.</p>
    <div class="tabs" role="tablist">
      <button v-for="t in TABS" :key="t[0]" role="tab" :aria-selected="tab === t[0]" :class="{ on: tab === t[0] }" @click="tab = t[0]">{{ t[1] }} <small v-if="t[0] !== 'ALL'">{{ d?.counts?.[t[0]] || 0 }}</small></button>
    </div>
    <div v-if="loading && !d" class="sk" style="height:240px" />
    <div v-else-if="!d?.items.length" class="empty card">Không có đơn nào ở mục này.</div>
    <div v-else class="list">
      <article v-for="o in d.items" :key="o.id" class="card fo">
        <div class="hd">
          <div class="score" :class="lvl(o.fraudScore)"><b>{{ o.fraudScore }}</b><small>/100</small></div>
          <div class="who"><b>{{ o.code }}</b> <span :class="['badge', 'f-' + o.fraudStatus]">{{ SL[o.fraudStatus] }}</span>
            <div class="small">{{ o.buyerName }} · {{ o.phone }} · {{ o.email || '—' }}<span v-if="o.guest"> · khách vãng lai</span></div>
            <div class="small muted">{{ vnd(o.total) }} · {{ o.paymentMethod }} · {{ o.items }} mặt hàng · {{ dt(o.createdAt) }} · {{ o.status }}<span v-if="o.clientIp"> · IP {{ o.clientIp }}</span></div>
          </div>
        </div>
        <ul class="sig"><li v-for="s in o.signals" :key="s.code"><span class="pt">+{{ s.points }}</span>{{ s.reason }}</li><li v-if="!o.signals.length" class="muted">Không có dấu hiệu.</li></ul>
        <div v-if="o.fraudReviewedBy" class="small muted">Duyệt bởi {{ o.fraudReviewedBy }} · {{ dt(o.fraudReviewedAt) }}</div>
        <div v-if="o.fraudStatus === 'REVIEW' || o.fraudStatus === 'WATCH'" class="act">
          <button class="btn btn-sm" :disabled="busy" @click="decide(o, 'approve')">Duyệt — cho xử lý</button>
          <button class="btn btn-sm btn-danger" :disabled="busy || !['PENDING', 'CONFIRMED'].includes(o.status)" @click="decide(o, 'reject')">Từ chối &amp; huỷ đơn</button>
          <router-link :to="{ path: '/admin/orders', query: { q: o.code } }" class="btn btn-sm btn-outline">Xem đơn</router-link>
        </div>
      </article>
    </div>
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, dt } from '../../format'
import { toast } from '../../stores/prefs'
const TABS = [['REVIEW', 'Chờ duyệt'], ['WATCH', 'Theo dõi'], ['APPROVED', 'Đã duyệt'], ['REJECTED', 'Đã từ chối'], ['ALL', 'Tất cả']]
const SL = { REVIEW: 'Chờ duyệt', WATCH: 'Theo dõi', APPROVED: 'Đã duyệt', REJECTED: 'Đã từ chối', NONE: 'Bình thường' }
const tab = ref('REVIEW'), d = ref(null), loading = ref(false), busy = ref(false)
const lvl = s => s >= 60 ? 'hi' : s >= 30 ? 'md' : 'lo'
async function load() { loading.value = true; try { d.value = (await api.get('/admin/fraud/queue', { params: { status: tab.value } })).data } finally { loading.value = false } }
async function decide(o, act) {
  let note = ''
  if (act === 'reject') { note = prompt('Lý do huỷ (gửi kèm thông báo huỷ đơn):', 'Nghi ngờ đơn ảo'); if (note === null) return }
  busy.value = true
  try { await api.post(`/admin/fraud/orders/${o.id}/${act}`, { note }); toast(act === 'approve' ? 'Đã duyệt đơn ' + o.code : 'Đã từ chối và huỷ đơn ' + o.code); load() } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
async function rescan() { busy.value = true; try { const r = (await api.post('/admin/fraud/rescan')).data; toast(`Đã chấm ${r.scanned} đơn · ${r.review} chờ duyệt`); load() } finally { busy.value = false } }
watch(tab, load); load()
</script>
<style scoped>
.wrap { flex-wrap: wrap; gap: 8px }
.tabs { display: flex; gap: 4px; flex-wrap: wrap; margin: 10px 0 14px }
.tabs button { border: 1px solid var(--border); background: var(--surface, #fff); color: inherit; padding: 6px 12px; border-radius: 6px; font-size: 13px }
.tabs button.on { background: var(--accent, #f15a24); border-color: var(--accent, #f15a24); color: #fff }
.list { display: grid; gap: 12px }
.fo { padding: 14px }
.hd { display: flex; gap: 12px; align-items: flex-start }
.who { min-width: 0; overflow-wrap: anywhere }
.score { flex: none; width: 58px; height: 58px; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; line-height: 1 }
.score b { font-size: 1.4rem } .score small { font-size: 10px }
.score.hi { background: #fdecea; color: #b3261e } .score.md { background: #fff3cd; color: #8a6100 } .score.lo { background: #e6f4ea; color: #1b7f3b }
.sig { list-style: none; padding: 0; margin: 10px 0; display: grid; gap: 4px; font-size: 13px }
.pt { display: inline-block; min-width: 34px; font-weight: 700; color: #b3261e }
.act { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px }
.f-REVIEW { background: #fdecea; color: #b3261e } .f-WATCH { background: #fff3cd; color: #8a6100 } .f-APPROVED { background: #e6f4ea; color: #1b7f3b } .f-REJECTED { background: #eee; color: #555 }
</style>
