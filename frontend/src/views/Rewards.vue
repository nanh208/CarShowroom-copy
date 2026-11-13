<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Tài khoản', to: '/account' }, { label: 'Phần thưởng' }]" />
    <div v-if="!r" class="rsk"><div class="sk" style="height:180px" /><div class="sk" style="height:260px" /></div>
    <template v-else>
      <section class="hero" v-reveal>
        <!-- Thẻ hạng thành viên VIP Vietcombank Platinum Visa Debit -->
        <div class="vip-card-wrap">
          <VipCard
            :tier="activeTierName"
            :points="r.points"
            :user="auth.user"
            :allow-click="true"
            :show-avatar-badge="true"
          />
        </div><div class="hl"><small>{{ selectedTier && selectedTier !== (r?.tier || 'Đồng') ? 'Đang xem mẫu thẻ' : 'Hạng thành viên' }}</small><h1>{{ activeTierName }}</h1>
          <div v-if="selectedTier && selectedTier !== (r?.tier || 'Đồng')" style="margin:4px 0 8px">
            <button type="button" class="btn btn-xs btn-outline" style="font-size:0.75rem;padding:3px 9px;background:rgba(255,255,255,0.1);color:#fff" @click="selectedTier = ''">
              Về hạng của tôi ({{ r?.tier || 'Đồng' }})
            </button>
          </div>
          <p class="pts"><CountUp :value="r.points" /> <span>điểm</span></p>
          <div v-if="r.nextTier" class="prog">
            <div class="bar"><i :style="{ width: pct + '%' }" /></div>
            <small>Chi tiêu thêm <b class="money">{{ vnd(r.nextTier.need) }}</b> để lên hạng <b>{{ r.nextTier.name }}</b></small>
          </div>
          <div v-else class="prog prog-max">
            <div class="bar"><i style="width: 100%; background: linear-gradient(90deg, #e4e4e7, #71717a)" /></div>
            <small style="color: #a1a1aa; font-weight: 600">Bạn đã đạt cấp bậc cao nhất &bull; Tận hưởng toàn bộ đặc quyền Titan tối cao!</small>
          </div>
          </div>
        <div class="tiers" v-stagger><div v-for="t in r.tiers" :key="t.name" :class="['tier', { on: t.name === activeTierName, 'is-cur': t.name === (r?.tier || 'Đồng') }]" style="cursor:pointer" :title="'Nhấn để xem mẫu thẻ ' + t.name" @click="selectedTier = t.name"><b>{{ t.name }}</b><small>{{ t.min ? 'từ ' + short(t.min) : 'mặc định' }}</small><p>{{ t.perk }}</p></div></div>
      </section>

      <div class="g2">
        <section class="card" v-reveal>
          <div class="row between"><h3>Điểm danh mỗi ngày</h3><span class="muted small">Chuỗi {{ r.streak }} ngày</span></div>
          <ol class="days"><li v-for="(p, i) in r.checkinRewards" :key="i" :class="{ done: i < doneDays, today: i === doneDays && !r.checkedToday }"><small>Ngày {{ i + 1 }}</small><b>+{{ p }}</b></li></ol>
          <button class="btn" :disabled="r.checkedToday || busy" @click="checkin">{{ r.checkedToday ? 'Đã điểm danh hôm nay' : 'Điểm danh nhận điểm' }}</button>
        </section>

        <section class="card spin" v-reveal:60>
          <h3>Vòng quay may mắn</h3><p class="muted small">1 lượt miễn phí mỗi ngày</p>
          <div class="wheelwrap"><span class="ptr" aria-hidden="true">▼</span>
            <svg class="wheel" viewBox="-100 -100 200 200" :style="{ transform: `rotate(${rot}deg)`, transitionDuration: spinning ? '3.2s' : '0s' }" role="img" aria-label="Vòng quay">
              <g v-for="(lab, i) in r.spinPrizes" :key="i"><path :d="slice(i)" :fill="i % 2 ? '#111' : 'var(--primary)'" stroke="#fff" stroke-width="1" />
                <text :transform="`rotate(${(i + .5) * seg - 90}) translate(58 0)`" text-anchor="middle" dominant-baseline="middle" fill="#fff" font-size="9" font-weight="700">{{ lab.length > 12 ? lab.slice(0, 11) + '…' : lab }}</text></g>
              <circle r="16" fill="#fff" /><text text-anchor="middle" dominant-baseline="middle" font-size="9" font-weight="800">ARES</text></svg></div>
          <p v-if="prize" class="prize" aria-live="polite">Chúc mừng! {{ prize.label }}<span v-if="prize.code"> — mã <b class="money">{{ prize.code }}</b> (7 ngày)</span></p>
          <button class="btn" :disabled="r.spunToday || spinning" @click="spin">{{ spinning ? 'Đang quay…' : r.spunToday ? 'Hẹn bạn ngày mai' : 'Quay ngay' }}</button>
        </section>
      </div>

      <div class="g2">
        <section class="card" v-reveal>
          <h3>Quà sinh nhật</h3>
          <template v-if="!r.birthday">
            <p class="muted small">Lưu ngày sinh để nhận 200–300 điểm + mã giảm 15% trong tháng sinh nhật. Chỉ lưu được 1 lần.</p>
            <div class="row" style="gap:8px;flex-wrap:wrap"><input v-model="bd" type="date" class="input" style="width:auto" :max="today" aria-label="Ngày sinh" /><button class="btn btn-sm" :disabled="!bd || busy" @click="saveBd">Lưu ngày sinh</button></div>
          </template>
          <template v-else>
            <p>Sinh nhật: <b>{{ d(r.birthday) }}</b></p>
            <button v-if="r.birthdayMonth && !r.birthdayClaimed" class="btn" :disabled="busy" @click="claim">Nhận quà sinh nhật</button>
            <p v-else-if="r.birthdayClaimed" class="muted small">Bạn đã nhận quà năm nay. Chúc mừng sinh nhật!</p>
            <p v-else class="muted small">Quà sẽ mở vào tháng sinh nhật của bạn.</p>
            <p v-if="bdCode" class="prize">Mã của bạn: <b class="money">{{ bdCode }}</b></p>
          </template>
        </section>
        <section class="card" v-reveal:60>
          <div class="row between"><h3>Huy hiệu</h3><span class="muted small">{{ r.badges.filter(b => b.earned).length }}/{{ r.badges.length }}</span></div>
          <div class="badges" v-stagger><div v-for="b in r.badges" :key="b.key" :class="['bd', { on: b.earned }]"><span class="bd-icon" v-html="BADGE_SVGS[b.key] || BADGE_SVGS.first"></span><b>{{ b.name }}</b><small>{{ b.desc }}</small></div></div>
        </section>
      </div>
      <p class="muted small">Lịch sử điểm chi tiết xem tại <router-link to="/account?tab=points">Tài khoản › Điểm thưởng</router-link>. 100 điểm = 10.000đ khi thanh toán.</p>
    </template>
  </div>
</template>
<script setup>
function formatCardNum(numOrId, points) {
  if (numOrId && typeof numOrId === 'string' && numOrId.length >= 12) {
    return numOrId.replace(/[-]/g, ' • ')
  }
  const idStr = String(numOrId || auth.user?.id || 1).padStart(4, '0')
  const ptsSuffix = String((points || 0) + 1200).padStart(4, '0')
  return `8899 • 2026 • ${idStr} • ${ptsSuffix}`
}

import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../api'
import { vnd, d } from '../format'
import { toast } from '../stores/prefs'
import { reduced } from '../motion'
import { useAuth } from '../stores/auth'

const auth = useAuth()
import VipCard from '../components/VipCard.vue'
import Breadcrumb from '../components/Breadcrumb.vue'
import CountUp from '../components/CountUp.vue'
const BADGE_SVGS = {
  first: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="m9 12 2 2 4-4"/></svg>',
  collector: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6M18 9h1.5a2.5 2.5 0 0 0 0-5H18M4 22h16M12 15V22M8 4h8v7a4 4 0 0 1-8 0V4z"/></svg>',
  critic: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>',
  streak7: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"/></svg>',
  bigspender: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 3h12l4 6-10 12L2 9l4-6z"/></svg>',
  rc: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5" width="20" height="14" rx="2"/><circle cx="7" cy="15" r="2"/><circle cx="17" cy="15" r="2"/></svg>',
  birthday: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="8" width="18" height="13" rx="2"/><path d="M12 8v13M3 12h18"/><path d="M12 8a3 3 0 0 0-3-3 3 3 0 0 0-3 3M12 8a3 3 0 0 1 3-3 3 3 0 0 1 3 3"/></svg>'
}
const r = ref(null), busy = ref(false), rot = ref(0), spinning = ref(false), prize = ref(null), bd = ref(''), bdCode = ref('')
const selectedTier = ref('')
function formatTierName(t) {
  if (!t) return 'Đồng'
  if (t === 'Bạch kim' || t === 'Bạch Kim' || t === 'Kim cương') return 'Titan'
  return t
}
const activeTierName = computed(() => {
  const val = selectedTier.value || r.value?.tier || 'Đồng'
  return formatTierName(val)
})
const today = new Date().toISOString().slice(0, 10)
const seg = computed(() => 360 / (r.value?.spinPrizes.length || 1))
const doneDays = computed(() => r.value ? (r.value.checkedToday ? ((r.value.streak - 1) % 7) + 1 : r.value.streak % 7) : 0)
const pct = computed(() => { const x = r.value; if (!x?.nextTier) return 100; const cur = x.tiers.filter(t => t.min <= x.spent).pop()?.min || 0; const nx = x.spent + x.nextTier.need; return Math.round((x.spent - cur) * 100 / (nx - cur)) })
const short = n => n >= 1e6 ? n / 1e6 + ' triệu' : vnd(n)
const slice = i => { const a0 = (i * seg.value - 90) * Math.PI / 180, a1 = ((i + 1) * seg.value - 90) * Math.PI / 180; return `M0 0 L${95 * Math.cos(a0)} ${95 * Math.sin(a0)} A95 95 0 0 1 ${95 * Math.cos(a1)} ${95 * Math.sin(a1)} Z` }
const load = async () => {
  const res = (await api.get('/account/rewards')).data
  if (res?.tier) res.tier = formatTierName(res.tier)
  if (res?.tiers) {
    res.tiers = res.tiers.map(t => ({ ...t, name: formatTierName(t.name) }))
  }
  if (res?.nextTier) {
    res.nextTier.name = formatTierName(res.nextTier.name)
  }
  r.value = res
}
async function checkin() { busy.value = true; try { const { data } = await api.post('/account/checkin'); toast(`+${data.points} điểm · chuỗi ${data.streak} ngày`); await load() } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
async function spin() {
  spinning.value = true; prize.value = null
  try {
    const { data } = await api.post('/account/spin')
    const target = 360 - (data.index + .5) * seg.value
    rot.value = rot.value - (rot.value % 360) + 360 * 5 + target
    setTimeout(async () => { prize.value = data; spinning.value = false; await load() }, reduced() ? 0 : 3300)
  } catch (e) { toast(errMsg(e), 'error'); spinning.value = false }
}
async function saveBd() { busy.value = true; try { r.value = (await api.put('/account/birthday', { birthday: bd.value })).data; toast('Đã lưu ngày sinh') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
async function claim() { busy.value = true; try { const { data } = await api.post('/account/birthday/claim'); bdCode.value = data.code; toast(`+${data.points} điểm sinh nhật`); await load() } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
onMounted(load)
</script>
<style scoped>
.rsk { display: grid; gap: 16px; margin-top: 16px; }
.hero { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr); gap: 24px; background: #111; color: #fff; padding: clamp(20px, 4vw, 36px); margin: 12px 0 20px; position: relative; overflow: hidden; }
.hero::after { content: ''; position: absolute; right: -40px; top: -40px; width: 220px; height: 220px; background: repeating-conic-gradient(#fff 0 25%, transparent 0 50%) 0 0 / 28px 28px; opacity: .06; transform: rotate(12deg); }
.hl small { color: #aaa; text-transform: uppercase; letter-spacing: .12em; font-size: .72rem; } .hl h1 { color: #fff; margin: 4px 0; font-size: clamp(2.2rem, 6vw, 3.4rem); }
.pts { font: 700 1.6rem 'Oswald', sans-serif; color: var(--primary); margin: 0 0 12px; } .pts span { font-size: .9rem; color: #ccc; }
.prog .bar { height: 8px; background: #333; margin-bottom: 6px; } .prog .bar i { display: block; height: 100%; background: var(--primary); transition: width .8s cubic-bezier(.2,.8,.2,1); } .prog small { color: #ccc; }
.tiers { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 8px; position: relative; z-index: 1; }
.tier { border: 1px solid #333; padding: 10px; } .tier b { display: block; font-family: 'Oswald', sans-serif; } .tier small { color: #999; font-size: .72rem; } .tier p { margin: 6px 0 0; font-size: .78rem; color: #ccc; }
.tier.on { border-color: var(--primary); background: rgba(255,106,0,.12); }
.g2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 340px), 1fr)); gap: 16px; margin-bottom: 16px; } .g2 > * { min-width: 0; }
.days { list-style: none; padding: 0; margin: 14px 0; display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
.days li { border: 1px solid var(--border); text-align: center; padding: 8px 2px; display: flex; flex-direction: column; } .days small { font-size: .62rem; color: var(--muted); } .days b { font-family: 'Oswald', sans-serif; }
.days li.done { background: var(--primary); color: #fff; border-color: var(--primary); } .days li.done small { color: #fff; } .days li.today { border: 2px solid var(--primary); animation: pulse 1.6s ease-out infinite; }
@keyframes pulse { 50% { box-shadow: 0 0 0 4px rgba(255,106,0,.18); } }
.spin { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 6px; }
.wheelwrap { position: relative; width: min(240px, 70vw); aspect-ratio: 1; margin: 8px 0; } .ptr { position: absolute; left: 50%; top: -12px; transform: translateX(-50%); color: var(--danger); font-size: 1.4rem; z-index: 1; }
.wheel { width: 100%; height: 100%; transition-property: transform; transition-timing-function: cubic-bezier(.15,.85,.25,1); }
.prize { font-weight: 600; animation: pop .35s ease-out; } @keyframes pop { from { transform: scale(.8); opacity: 0; } }
.badges { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 8px; margin-top: 10px; }
.bd { border: 1px solid var(--border); padding: 10px; display: flex; flex-direction: column; gap: 2px; filter: grayscale(1); opacity: .5; } .bd span { font-size: 1.5rem; } .bd small { font-size: .72rem; color: var(--muted); }
.bd.on { filter: none; opacity: 1; border-color: var(--primary); }
@media (max-width: 760px) { .hero { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) { .days li.today, .prize { animation: none; } .wheel { transition: none !important; } }

/* VIP Member Card Styles in Rewards Ultra-Luxury Widescreen */
.vip-card-section {
  display: grid;
  grid-template-columns: 1.28fr 1fr;
  gap: 24px;
  align-items: stretch;
  margin-bottom: 14px;
}
@media (max-width: 990px) {
  .vip-card-section { grid-template-columns: 1fr; }
}

.vip-card-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.vip-member-card {
  width: 100%;
  max-width: 560px;
  aspect-ratio: 1.62 / 1;
  min-height: 315px;
  border-radius: 22px !important;
  padding: 26px 30px;
  color: #fff;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 28px 56px -12px rgba(0, 0, 0, 0.8), 0 0 0 1px rgba(255, 255, 255, 0.18);
  overflow: hidden;
  transition: transform .3s ease, box-shadow .3s ease;
  user-select: none;
}
.vip-member-card:hover {
  transform: translateY(-4px) scale(1.012);
  box-shadow: 0 36px 70px -12px rgba(0, 0, 0, 0.9), 0 0 0 1.5px rgba(255, 255, 255, 0.4);
}

/* Dynamic Tier Themes */
.card-tier-Đồng {
  background: radial-gradient(circle at 85% 15%, rgba(245, 158, 11, 0.3) 0%, transparent 55%),
              linear-gradient(135deg, #180b05 0%, #45200c 35%, #7c3a16 65%, #2a1307 100%);
  border: 1.8px solid #b45309;
  box-shadow: 0 24px 55px -10px rgba(120, 56, 20, 0.6), 0 0 0 1px rgba(245, 158, 11, 0.35);
}
.card-tier-Bạc {
  background: radial-gradient(circle at 85% 15%, rgba(241, 245, 249, 0.3) 0%, transparent 55%),
              linear-gradient(135deg, #090d16 0%, #334155 35%, #64748b 65%, #1e293b 100%);
  border: 1.8px solid #cbd5e1;
  box-shadow: 0 24px 55px -10px rgba(71, 85, 105, 0.6), 0 0 0 1px rgba(203, 213, 225, 0.4);
}
.card-tier-Vàng {
  background: radial-gradient(circle at 85% 15%, rgba(253, 224, 71, 0.38) 0%, transparent 55%),
              linear-gradient(135deg, #181002 0%, #78350f 30%, #b45309 55%, #d97706 75%, #1c1303 100%);
  border: 1.8px solid #fde047;
  box-shadow: 0 26px 60px -10px rgba(217, 119, 6, 0.7), 0 0 0 1.2px rgba(253, 224, 71, 0.5);
}
.card-tier-Bạch_kim, .card-tier-Bạch-kim, .card-tier-Kim_cương, .card-tier-Kim-cương {
  background: radial-gradient(circle at 85% 15%, rgba(255, 255, 255, 0.18) 0%, transparent 45%),
              radial-gradient(circle at 15% 85%, rgba(148, 163, 184, 0.15) 0%, transparent 50%),
              linear-gradient(135deg, #07080b 0%, #121319 32%, #1b1c25 58%, #111218 80%, #040507 100%);
  border: 1.8px solid #e2e8f0;
  box-shadow: 0 32px 70px -12px rgba(0, 0, 0, 0.95), 0 0 0 1.2px rgba(255, 255, 255, 0.35), inset 0 1px 2px rgba(255, 255, 255, 0.3);
}
.card-tier-Bạch_kim .thb-text, .card-tier-Kim_cương .thb-text {
  color: #f8fafc;
  text-shadow: 0 0 8px rgba(255,255,255,0.6);
}

.card-sheen {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 40%, rgba(255, 255, 255, 0.16) 50%, transparent 60%);
  pointer-events: none;
  animation: cardShimmer 7s infinite linear;
}
@keyframes cardShimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.card-mesh {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.07) 1.2px, transparent 1.2px);
  background-size: 8px 8px;
  pointer-events: none;
}

/* Card Top Row */
.card-top { display: flex; justify-content: space-between; align-items: center; position: relative; z-index: 2; }
.club-brand { display: flex; align-items: center; gap: 10px; }
.crest-icon { color: #facc15; filter: drop-shadow(0 2px 4px rgba(0,0,0,.7)); }
.club-title { font: 800 1.08rem 'Oswald', sans-serif; letter-spacing: .18em; display: block; line-height: 1.1; color: #fff; text-shadow: 0 2px 4px rgba(0,0,0,.8); }
.club-sub { font-size: .62rem; letter-spacing: .22em; color: rgba(255,255,255,.8); font-family: monospace; display: block; margin-top: 2px; }
.card-top-right { display: flex; align-items: center; gap: 12px; }
.tier-holo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 4px 14px;
  border-radius: 999px !important;
  backdrop-filter: blur(6px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  white-space: nowrap;
}
.thb-text { font: 800 .75rem 'Oswald', sans-serif; letter-spacing: .12em; color: #ffd54f; white-space: nowrap; }
.rfid-wave { opacity: .88; color: #fff; }

/* Card Mid Layout (Chip on Left, Large Avatar on Right) */
.card-mid-layout {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 2;
  margin: 10px 0;
  padding: 0 2px;
}
.card-mid-left { display: flex; flex-direction: column; gap: 4px; }
.emv-chip-wrap { display: flex; flex-direction: column; align-items: flex-start; }
.emv-chip {
  width: 56px;
  height: 42px;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #fef08a 0%, #eab308 45%, #ca8a04 100%);
  border: 1.2px solid #a16207;
  box-shadow: inset 0 1px 3px rgba(255,255,255,.9), 0 3px 8px rgba(0,0,0,.6);
  position: relative;
  overflow: hidden;
}
.chip-line { position: absolute; background: rgba(0,0,0,.28); }
.c1 { top: 50%; left: 0; right: 0; height: 1px; }
.c2 { top: 0; bottom: 0; left: 36%; width: 1px; }
.c3 { top: 0; bottom: 0; right: 36%; width: 1px; }
.chip-core {
  position: absolute;
  top: 25%;
  bottom: 25%;
  left: 36%;
  right: 36%;
  border: 1px solid rgba(0,0,0,.22);
  border-radius: 2px !important;
  background: rgba(254, 240, 138, 0.4);
}
.chip-meta { margin-top: 5px; }
.cm-label { font-size: .56rem; letter-spacing: .15em; color: rgba(255,255,255,.7); font-family: monospace; display: block; }

/* Enlarged Luxury Avatar Medallion */
.card-mid-right { display: flex; align-items: center; justify-content: center; }
.avatar-luxury-container { position: relative; display: flex; align-items: center; justify-content: center; }
.avatar-ring-outer {
  width: 92px;
  height: 92px;
  border-radius: 50% !important;
  padding: 3.5px;
  background: linear-gradient(135deg, #fef08a 0%, #ca8a04 50%, #fef08a 100%);
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.7), 0 10px 28px rgba(0, 0, 0, 0.85), inset 0 1px 2px rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-ring-inner {
  width: 100%;
  height: 100%;
  border-radius: 50% !important;
  overflow: hidden;
  background: #111;
  border: 2px solid rgba(0, 0, 0, 0.65);
  position: relative;
}
.avatar-photo {
  width: 100%;
  height: 100%;
  border-radius: 50% !important;
  object-fit: cover;
  display: block;
}
.avatar-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50% !important;
  background: linear-gradient(135deg, #374151, #111827);
  color: #facc15;
  display: grid;
  place-items: center;
  font: 800 2rem 'Oswald', sans-serif;
}
.avatar-vip-seal {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: linear-gradient(135deg, #fde047 0%, #ca8a04 100%);
  color: #111;
  font: 900 .65rem 'Oswald', sans-serif;
  letter-spacing: .08em;
  padding: 2.5px 8px;
  border-radius: 999px !important;
  border: 1.5px solid #fff;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.75);
}

/* Card Number Embossed */
.card-number-embossed {
  font-family: 'Share Tech Mono', 'Farrington 7B', 'OCR A Extended', 'Cinzel', monospace;
  font-size: clamp(1.02rem, 2.1vw, 1.28rem);
  font-weight: 700;
  letter-spacing: 0.18em;
  color: #f8fafc;
  text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.95), -1px -1px 0 rgba(255, 255, 255, 0.38), 0 0 14px rgba(255, 255, 255, 0.22);
  position: relative;
  z-index: 2;
  margin: 6px 0 8px;
  white-space: nowrap;
  overflow: visible;
  width: 100%;
}
.card-dragon-phoenix {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}
.dp-watermark {
  width: 100%;
  height: 100%;
  color: #f1f5f9;
}
.card-tier-Bạch_kim .dp-watermark, .card-tier-Kim_cương .dp-watermark {
  color: #e2e8f0;
  filter: drop-shadow(0 0 2px rgba(255,255,255,0.25));
}

/* Card Bottom Row */
.card-bottom { display: flex; justify-content: space-between; align-items: flex-end; position: relative; z-index: 2; padding: 0 2px; }
.cb-holder { flex: 1.2; }
.cb-label { font-size: .56rem; letter-spacing: .15em; color: rgba(255,255,255,.75); display: block; text-transform: uppercase; margin-bottom: 2px; }
.cb-name { font-size: .98rem; letter-spacing: .1em; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px; display: block; color: #fff; text-shadow: 0 1px 3px rgba(0,0,0,.8); }
.cb-since { text-align: center; margin: 0 14px; }
.cb-expiry { text-align: center; margin-right: 14px; }
.cb-val-text, .cb-val-exp { font-size: .94rem; letter-spacing: .1em; font-family: monospace; color: #fff; display: block; }
.cb-seal {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.28);
  padding: 3px 9px;
  border-radius: 6px !important;
  backdrop-filter: blur(4px);
}
.cb-seal-crest { font: 900 .72rem 'Oswald', sans-serif; letter-spacing: .15em; color: #fff; }
.cb-seal-sub { font-size: .52rem; letter-spacing: .18em; color: #ffd54f; font-family: monospace; }

</style>
