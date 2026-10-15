<template>
  <div class="container page gc">
    <Breadcrumb :items="[{ label: 'Thẻ quà tặng' }]" />
    <section class="hero">
      <div><span class="kick">ARES GIFT CARD</span><h1>Tặng đam mê, không lo chọn sai mẫu</h1>
        <p class="muted">Thẻ quà điện tử gửi qua email, dùng cho mọi sản phẩm, hạn 12 tháng. Người nhận nhập mã khi thanh toán hoặc nạp vào ví tín dụng.</p></div>
      <div class="card-art" aria-hidden="true"><b>ARES</b><span>{{ vnd(amount) }}</span><i class="chk" /></div>
    </section>
    <div class="grid2">
      <form class="card" @submit.prevent="buy">
        <h3>Mua thẻ quà</h3>
        <div class="amts" role="radiogroup" aria-label="Mệnh giá"><button v-for="a in AMOUNTS" :key="a" type="button" :class="['amt', { on: amount === a }]" :aria-pressed="amount === a" @click="amount = a">{{ k(a) }}</button></div>
        <div class="field"><label for="gc-to">Email người nhận</label><input id="gc-to" v-model.trim="to" type="email" class="input" :placeholder="auth.user?.email || 'ban@vidu.vn'" /></div>
        <div class="field"><label for="gc-msg">Lời nhắn</label><textarea id="gc-msg" v-model="msg" class="input" rows="2" maxlength="200" placeholder="Chúc mừng sinh nhật! 🏎️" /></div>
        <p v-if="!auth.loggedIn" class="small muted"><router-link :to="{ path: '/login', query: { redirect: '/gift-cards' } }">Đăng nhập</router-link> để mua thẻ quà.</p>
        <button class="btn btn-block" :disabled="busy || !auth.loggedIn">{{ busy ? 'Đang xử lý…' : `Thanh toán ${vnd(amount)} (giả lập)` }}</button>
        <div v-if="done" class="alert alert-success" style="margin-top:10px">Đã gửi thẻ <b>{{ done.code }}</b> tới {{ done.sentTo }}.</div>
      </form>
      <form class="card" @submit.prevent="check">
        <h3>Kiểm tra số dư</h3>
        <div class="row" style="gap:8px;flex-wrap:nowrap"><input v-model.trim="code" class="input grow" placeholder="GC…" aria-label="Mã thẻ" style="min-width:0" /><button class="btn btn-outline">Kiểm tra</button></div>
        <p v-if="bal" class="bal">Số dư: <b class="price">{{ vnd(bal.balance) }}</b><br /><small class="muted">Hạn dùng {{ bal.expiresAt ? d(bal.expiresAt) : '—' }}</small></p>
        <p v-if="err" class="small err">{{ err }}</p>
        <p class="small muted">Đã có thẻ? Nạp vào ví tại <router-link to="/account?tab=wallet">Tài khoản › Ví & quà tặng</router-link>.</p>
      </form>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import api, { errMsg } from '../api'
import { vnd, k, d } from '../format'
import { useAuth } from '../stores/auth'
import { toast } from '../stores/prefs'
import Breadcrumb from '../components/Breadcrumb.vue'
const auth = useAuth(), AMOUNTS = [100000, 200000, 500000, 1000000, 2000000]
const amount = ref(500000), to = ref(''), msg = ref(''), busy = ref(false), done = ref(null), code = ref(''), bal = ref(null), err = ref('')
async function buy() {
  busy.value = true
  try { done.value = (await api.post('/giftcards/buy', { amount: amount.value, recipientEmail: to.value, message: msg.value })).data; toast('Đã phát hành thẻ quà 🎁') }
  catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
async function check() { bal.value = null; err.value = ''; try { bal.value = (await api.get('/public/giftcards/' + encodeURIComponent(code.value))).data } catch (e) { err.value = errMsg(e) } }
</script>
<style scoped>
.hero { display: grid; grid-template-columns: 1.3fr 1fr; gap: clamp(20px, 4vw, 48px); align-items: center; margin: 12px 0 28px; }
.kick { font: 600 .72rem 'Oswald', sans-serif; letter-spacing: .3em; color: var(--accent); } .hero h1 { font-size: clamp(1.6rem, 1.1rem + 2.2vw, 2.6rem); margin: 6px 0 10px; }
.card-art { aspect-ratio: 1.6; background: linear-gradient(135deg, #111 0 60%, var(--accent) 60%); color: #fff; padding: 22px; display: flex; flex-direction: column; justify-content: space-between; position: relative; overflow: hidden; box-shadow: 0 24px 48px -20px rgba(0,0,0,.5); }
.card-art b { font: 700 1.6rem 'Oswald', sans-serif; letter-spacing: .1em; } .card-art span { font: 700 clamp(1.4rem, 1rem + 2vw, 2.2rem) 'Oswald', sans-serif; }
.chk { position: absolute; right: 18px; top: 18px; width: 54px; height: 20px; background: conic-gradient(#fff 25%, transparent 0 50%, #fff 0 75%, transparent 0) 0 0/10px 10px; transform: skewX(-20deg); }
.grid2 { display: grid; grid-template-columns: 1.3fr 1fr; gap: 20px; align-items: start; } .grid2 > * { min-width: 0; } .card h3 { margin-bottom: 12px; }
.amts { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.amt { border: 1.5px solid var(--border); background: var(--surface); color: var(--text); padding: 8px 14px; font: 700 .9rem inherit; font-family: inherit; cursor: pointer; }
.amt.on { border-color: var(--accent); background: var(--accent); color: #fff; }
.bal { margin-top: 12px; } .err { color: var(--danger); }
@media (max-width: 860px) { .hero, .grid2 { grid-template-columns: 1fr; } .card-art { max-width: 380px; } }
</style>
