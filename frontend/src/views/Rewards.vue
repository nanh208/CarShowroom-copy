<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Tài khoản', to: '/account' }, { label: 'Phần thưởng' }]" />
    <div v-if="!r" class="rsk"><div class="sk" style="height:180px" /><div class="sk" style="height:260px" /></div>
    <template v-else>
      <section class="hero" v-reveal>
        <div class="hl"><small>Hạng thành viên</small><h1>{{ r.tier || 'Đồng' }}</h1>
          <p class="pts"><CountUp :value="r.points" /> <span>điểm</span></p>
          <div v-if="r.nextTier" class="prog"><div class="bar"><i :style="{ width: pct + '%' }" /></div><small>Chi tiêu thêm <b class="money">{{ vnd(r.nextTier.need) }}</b> để lên hạng <b>{{ r.nextTier.name }}</b></small></div>
          <small v-else>Bạn đang ở hạng cao nhất 🎉</small></div>
        <div class="tiers" v-stagger><div v-for="t in r.tiers" :key="t.name" :class="['tier', { on: t.name === (r.tier || 'Đồng') }]"><b>{{ t.name }}</b><small>{{ t.min ? 'từ ' + short(t.min) : 'mặc định' }}</small><p>{{ t.perk }}</p></div></div>
      </section>

      <div class="g2">
        <section class="card" v-reveal>
          <div class="row between"><h3>Điểm danh mỗi ngày</h3><span class="muted small">Chuỗi {{ r.streak }} ngày</span></div>
          <ol class="days"><li v-for="(p, i) in r.checkinRewards" :key="i" :class="{ done: i < doneDays, today: i === doneDays && !r.checkedToday }"><small>Ngày {{ i + 1 }}</small><b>+{{ p }}</b></li></ol>
          <button class="btn" :disabled="r.checkedToday || busy" @click="checkin">{{ r.checkedToday ? 'Đã điểm danh hôm nay ✓' : 'Điểm danh nhận điểm' }}</button>
        </section>

        <section class="card spin" v-reveal:60>
          <h3>Vòng quay may mắn</h3><p class="muted small">1 lượt miễn phí mỗi ngày</p>
          <div class="wheelwrap"><span class="ptr" aria-hidden="true">▼</span>
            <svg class="wheel" viewBox="-100 -100 200 200" :style="{ transform: `rotate(${rot}deg)`, transitionDuration: spinning ? '3.2s' : '0s' }" role="img" aria-label="Vòng quay">
              <g v-for="(lab, i) in r.spinPrizes" :key="i"><path :d="slice(i)" :fill="i % 2 ? '#111' : 'var(--primary)'" stroke="#fff" stroke-width="1" />
                <text :transform="`rotate(${(i + .5) * seg - 90}) translate(58 0)`" text-anchor="middle" dominant-baseline="middle" fill="#fff" font-size="9" font-weight="700">{{ lab.length > 12 ? lab.slice(0, 11) + '…' : lab }}</text></g>
              <circle r="16" fill="#fff" /><text text-anchor="middle" dominant-baseline="middle" font-size="9" font-weight="800">ARES</text></svg></div>
          <p v-if="prize" class="prize" aria-live="polite">🎉 {{ prize.label }}<span v-if="prize.code"> — mã <b class="money">{{ prize.code }}</b> (7 ngày)</span></p>
          <button class="btn" :disabled="r.spunToday || spinning" @click="spin">{{ spinning ? 'Đang quay…' : r.spunToday ? 'Hẹn bạn ngày mai' : 'Quay ngay' }}</button>
        </section>
      </div>

      <div class="g2">
        <section class="card" v-reveal>
          <h3>Quà sinh nhật 🎂</h3>
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
          <div class="badges" v-stagger><div v-for="b in r.badges" :key="b.key" :class="['bd', { on: b.earned }]"><span>{{ ICON[b.key] }}</span><b>{{ b.name }}</b><small>{{ b.desc }}</small></div></div>
        </section>
      </div>
      <p class="muted small">Lịch sử điểm chi tiết xem tại <router-link to="/account?tab=points">Tài khoản › Điểm thưởng</router-link>. 100 điểm = 10.000đ khi thanh toán.</p>
    </template>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../api'
import { vnd, d } from '../format'
import { toast } from '../stores/prefs'
import { reduced } from '../motion'
import Breadcrumb from '../components/Breadcrumb.vue'
import CountUp from '../components/CountUp.vue'
const ICON = { first: '🏁', collector: '🏆', critic: '✍️', streak7: '🔥', bigspender: '💎', rc: '🏎️', birthday: '🎂' }
const r = ref(null), busy = ref(false), rot = ref(0), spinning = ref(false), prize = ref(null), bd = ref(''), bdCode = ref('')
const today = new Date().toISOString().slice(0, 10)
const seg = computed(() => 360 / (r.value?.spinPrizes.length || 1))
const doneDays = computed(() => r.value ? (r.value.checkedToday ? ((r.value.streak - 1) % 7) + 1 : r.value.streak % 7) : 0)
const pct = computed(() => { const x = r.value; if (!x?.nextTier) return 100; const cur = x.tiers.filter(t => t.min <= x.spent).pop()?.min || 0; const nx = x.spent + x.nextTier.need; return Math.round((x.spent - cur) * 100 / (nx - cur)) })
const short = n => n >= 1e6 ? n / 1e6 + ' triệu' : vnd(n)
const slice = i => { const a0 = (i * seg.value - 90) * Math.PI / 180, a1 = ((i + 1) * seg.value - 90) * Math.PI / 180; return `M0 0 L${95 * Math.cos(a0)} ${95 * Math.sin(a0)} A95 95 0 0 1 ${95 * Math.cos(a1)} ${95 * Math.sin(a1)} Z` }
const load = async () => { r.value = (await api.get('/account/rewards')).data }
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
</style>
