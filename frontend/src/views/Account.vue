<template>
  <div class="container page">
    <h1>Tài khoản của tôi</h1>
    <p class="muted">{{ auth.user.email }} <span class="badge">{{ auth.user.provider === 'LOCAL' ? 'Email' : auth.user.provider }}</span></p>
    <div class="tabs">
      <button v-for="(v, k) in tabs" :key="k" :class="{ active: tab === k }" @click="setTab(k)">{{ v }}</button>
    </div>

    <!-- Hồ sơ -->
    <div v-if="tab === 'profile'" class="grid grid-2">
      <div class="card">
        <h3>Thông tin cá nhân</h3>
        <div v-if="msg.profile" class="alert alert-success">{{ msg.profile }}</div>
        <div class="field"><label>Họ và tên</label><input v-model="profile.fullName" class="input" /></div>
        <div class="field"><label>Số điện thoại</label><input v-model="profile.phone" class="input" /></div>
        <div class="field"><label>Ảnh đại diện (URL)</label><input v-model="profile.avatarUrl" class="input" /></div>
        <button class="btn" @click="saveProfile">Lưu thay đổi</button>
      </div>
      <div class="card">
        <h3>Đổi mật khẩu</h3>
        <div v-if="msg.pw" :class="['alert', msg.pwOk ? 'alert-success' : 'alert-error']">{{ msg.pw }}</div>
        <div v-if="auth.user.provider === 'LOCAL'" class="field"><label>Mật khẩu hiện tại</label><input v-model="pw.currentPassword" type="password" class="input" /></div>
        <div class="field"><label>Mật khẩu mới</label><input v-model="pw.newPassword" type="password" class="input" /></div>
        <button class="btn" @click="changePw">Đổi mật khẩu</button>
      </div>
    </div>

    <!-- Sổ địa chỉ -->
    <div v-if="tab === 'addresses'">
      <div class="row between" style="margin-bottom:14px"><span class="muted">Lưu địa chỉ giao nhận để thanh toán nhanh hơn</span>
        <button class="btn btn-sm" @click="editAddr()">+ Thêm địa chỉ</button></div>
      <div v-if="!addresses.length" class="empty card">Chưa có địa chỉ nào.</div>
      <div class="grid grid-4">
        <div v-for="a in addresses" :key="a.id" class="card addr">
          <div class="row between"><b>{{ a.label || 'Địa chỉ' }}</b><span v-if="a.isDefault" class="badge badge-new">Mặc định</span></div>
          <p>{{ a.receiverName }} · {{ a.phone }}</p>
          <p class="muted small">{{ [a.line, a.ward, a.district, a.city].filter(Boolean).join(', ') }}</p>
          <div class="row"><button class="btn btn-sm btn-outline" @click="editAddr(a)">Sửa</button><button class="btn btn-sm btn-outline" @click="delAddr(a)">Xoá</button><button v-if="!a.isDefault" class="btn btn-sm btn-outline" @click="makeDefault(a)">Đặt mặc định</button></div>
        </div>
      </div>
    </div>

    <!-- Lịch sử giao dịch -->
    <div v-if="tab === 'orders'" class="card table-wrap">
      <div v-if="!orders.length" class="empty">Bạn chưa có đơn hàng nào. <router-link to="/products">Mua sắm ngay</router-link></div>
      <table v-else class="table">
        <thead><tr><th>Mã đơn</th><th>Ngày đặt</th><th>Sản phẩm</th><th>Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="o in orders" :key="o.id">
          <td><b>{{ o.code }}</b></td><td>{{ dt(o.createdAt) }}</td>
          <td class="small">{{ o.items.map(i => i.productName + ' ×' + i.quantity).join(', ') }}</td>
          <td class="price">{{ vnd(o.total) }}</td>
          <td class="small">{{ PAY_METHOD[o.paymentMethod] }}<br /><span :class="['badge', 'badge-' + o.paymentStatus]">{{ PAY_STATUS[o.paymentStatus] }}</span></td>
          <td><span :class="['badge', 'badge-' + o.status]">{{ ORDER_STATUS[o.status] }}</span></td>
          <td><router-link :to="`/orders/${o.code}`" class="btn btn-sm btn-outline">Chi tiết</router-link></td>
        </tr></tbody>
      </table>
    </div>

    <!-- Điểm thưởng -->
    <div v-if="tab === 'points'" class="grid grid-2">
      <div class="card pcard">
        <Gauge :value="pts.points || 0" :max="nextTier.at" :label="`${pts.points || 0} điểm`" :sub="`Hạng ${pts.tier}`" :size="220" />
        <p class="muted small" style="text-align:center">1 điểm = {{ vnd(pts.pointValue) }} khi thanh toán · tích 1 điểm mỗi {{ vnd(pts.earnPer) }} đơn hoàn tất</p>
        <router-link to="/rewards" class="btn btn-sm" style="align-self:center;margin:6px 0">🎁 Điểm danh, vòng quay & huy hiệu →</router-link>
        <div class="tiers"><span v-for="t in TIERS" :key="t.name" :class="{ on: t.name === pts.tier }">{{ t.name }}<small>{{ t.label }}</small></span></div>
      </div>
      <div class="card table-wrap">
        <h3>Lịch sử điểm</h3>
        <div v-if="!pts.history?.length" class="empty">Chưa có giao dịch điểm.</div>
        <table v-else class="table"><tbody><tr v-for="h in pts.history" :key="h.id"><td>{{ dt(h.createdAt) }}</td><td>{{ h.reason }}</td><td :class="h.delta > 0 ? 'ok' : 'err'">{{ h.delta > 0 ? '+' : '' }}{{ h.delta }}</td></tr></tbody></table>
      </div>
    </div>

    <!-- Đổi trả -->
    <div v-if="tab === 'returns'" class="card table-wrap">
      <div v-if="!returnsList.length" class="empty">Chưa có yêu cầu đổi trả. Bạn có thể gửi yêu cầu trong trang chi tiết đơn đã giao (trong 7 ngày).</div>
      <table v-else class="table">
        <thead><tr><th>Đơn</th><th>Loại</th><th>Lý do</th><th>Trạng thái</th><th>Phản hồi</th><th>Ngày</th></tr></thead>
        <tbody><tr v-for="r in returnsList" :key="r.id"><td><router-link :to="`/orders/${r.orderCode}`">{{ r.orderCode }}</router-link></td><td>{{ r.kind === 'REFUND' ? 'Hoàn tiền' : 'Đổi trả' }}</td>
          <td class="small">{{ r.reason }}</td><td><span :class="['badge', 'badge-' + r.status]">{{ RETURN_STATUS[r.status] }}</span></td><td class="small">{{ r.adminNote }}</td><td>{{ d(r.createdAt) }}</td></tr></tbody>
      </table>
    </div>

    <!-- Phiếu hỗ trợ -->
    <div v-if="tab === 'tickets'" class="card table-wrap">
      <div class="row between" style="margin-bottom:10px"><span class="muted">Phiếu hỗ trợ của bạn</span><router-link to="/support#ticket" class="btn btn-sm">+ Tạo phiếu</router-link></div>
      <div v-if="!ticketsList.length" class="empty">Chưa có phiếu hỗ trợ.</div>
      <table v-else class="table">
        <thead><tr><th>Mã</th><th>Chủ đề</th><th>Loại</th><th>Trạng thái</th><th>Cập nhật</th></tr></thead>
        <tbody><tr v-for="t in ticketsList" :key="t.id"><td><router-link :to="`/support/tickets/${t.code}`">{{ t.code }}</router-link></td><td>{{ t.subject }}</td><td>{{ t.category }}</td>
          <td><span :class="['badge', 'badge-' + t.status]">{{ TICKET_STATUS[t.status] }}</span></td><td>{{ dt(t.updatedAt) }}</td></tr></tbody>
      </table>
    </div>

    <div v-if="tab === 'bids'" class="card table-wrap">
      <div v-if="!bids.length" class="empty">Bạn chưa tham gia phiên đấu giá nào. <router-link to="/auctions">Xem phiên đấu giá</router-link></div>
      <table v-else class="table">
        <thead><tr><th>Phiên</th><th>Giá bạn đặt</th><th>Giá hiện tại</th><th>Thời gian</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="b in bids" :key="b.id">
          <td><router-link :to="`/auctions/${b.auctionId}`">{{ b.auctionTitle }}</router-link></td>
          <td class="price">{{ vnd(b.amount) }}</td><td>{{ vnd(b.currentPrice) }}</td><td class="small">{{ dt(b.createdAt) }}</td>
          <td><span :class="['badge', b.won ? 'badge-COMPLETED' : 'badge-' + (b.status === 'LIVE' ? 'CONFIRMED' : 'PENDING')]">{{ b.won ? 'Thắng' : AUCTION_STATUS[b.status] }}</span></td>
          <td><router-link v-if="b.won && b.orderCode" :to="`/orders/${b.orderCode}`" class="btn btn-sm">Thanh toán</router-link></td>
        </tr></tbody>
      </table>
    </div>

    <div v-if="tab === 'wallet'" class="grid grid-2 wallet">
      <div class="card wcard">
        <small>Ví tín dụng ARES</small><b class="bal">{{ vnd(w.storeCredit || 0) }}</b>
        <p class="small muted">Dùng trừ thẳng vào đơn ở bước thanh toán. Nguồn: thẻ quà, hoàn tiền vào ví.</p>
        <form class="row" @submit.prevent="redeem"><input v-model.trim="gcCode" class="input grow" placeholder="Nhập mã thẻ quà GC…" aria-label="Mã thẻ quà" /><button class="btn btn-sm">Nạp vào ví</button></form>
        <router-link to="/gift-cards" class="small">🎁 Mua thẻ quà tặng bạn bè →</router-link>
      </div>
      <div class="card">
        <h3>Giới thiệu bạn bè</h3>
        <p class="small muted">Bạn bè giảm <b>{{ vnd(w.referral?.discount) }}</b> cho đơn đầu tiên; bạn nhận <b>{{ w.referral?.reward }} điểm</b> mỗi lượt.</p>
        <div class="refcode"><code>{{ w.referral?.code }}</code><button class="btn btn-sm" @click="copyRef">Sao chép liên kết</button></div>
        <p class="small muted">Đã có {{ w.referral?.uses || 0 }} người dùng mã của bạn.</p>
      </div>
      <div class="card">
        <h3>Thẻ quà của tôi</h3>
        <p v-if="!w.giftCards?.length" class="muted small">Chưa có thẻ quà.</p>
        <div v-for="g in w.giftCards" :key="g.id" class="row between gc"><code>{{ g.code }}</code><span class="small">{{ vnd(g.balance) }} / {{ vnd(g.initial) }}</span><span class="small muted">HSD {{ d(g.expiresAt) }}</span></div>
      </div>
      <div class="card">
        <h3>Hộp thư (email mô phỏng)</h3>
        <p v-if="!w.inbox?.length" class="muted small">Chưa có email.</p>
        <details v-for="m in w.inbox" :key="m.id" class="mail"><summary><b>{{ m.subject }}</b><small class="muted"> · {{ ago(m.createdAt) }}</small></summary><pre>{{ m.body }}</pre></details>
      </div>
    </div>

    <AccountSubscriptions v-if="tab === 'subscriptions'" />
    <AccountAffiliate v-if="tab === 'affiliate'" />
    <div v-if="tab === 'settings'" class="grid grid-2">
      <PushToggle />
      <div class="card">
        <h3>Xác thực 2 bước (TOTP)</h3>
        <p class="muted small">Bảo vệ tài khoản bằng mã 6 số từ Google Authenticator / Authy mỗi lần đăng nhập.</p>
        <template v-if="auth.user.totpEnabled">
          <p class="ok">✓ Đang bật</p>
          <form class="row" @submit.prevent="off2fa"><input v-model.trim="otp" class="input grow" inputmode="numeric" maxlength="6" placeholder="Mã 6 số để tắt" aria-label="Mã xác thực" /><button class="btn btn-sm btn-outline">Tắt 2FA</button></form>
        </template>
        <template v-else-if="tf">
          <div class="tf"><img v-if="tfQr" :src="tfQr" alt="Mã QR thiết lập 2FA" width="160" height="160" /><div><p class="small">1. Quét mã QR bằng ứng dụng xác thực<br />2. Hoặc nhập khoá: <code class="key">{{ tf.secret.match(/.{1,4}/g).join(' ') }}</code></p>
            <form class="row" @submit.prevent="on2fa"><input v-model.trim="otp" class="input grow" inputmode="numeric" maxlength="6" placeholder="Mã 6 số" aria-label="Mã xác thực" /><button class="btn btn-sm">Kích hoạt</button></form></div></div>
        </template>
        <button v-else class="btn btn-sm" @click="setup2fa">Bật xác thực 2 bước</button>
        <p v-if="msg.tf" :class="msg.tfOk ? 'ok' : 'err'">{{ msg.tf }}</p>
      </div>
      <div class="card">
        <h3>Thanh toán ưa thích</h3>
        <p class="muted small">Tự chọn sẵn ở bước thanh toán.</p>
        <div class="paypref">
          <label v-for="(v, k) in PAY_METHOD" :key="k" :class="['pp', { on: payPref === k }]"><input v-model="payPref" type="radio" :value="k" @change="savePay" />{{ v }}</label>
          <label :class="['pp', { on: !payPref }]"><input v-model="payPref" type="radio" value="" @change="savePay" />Không đặt</label>
        </div>
      </div>
      <div class="card">
        <h3>Thông báo giá & hàng</h3>
        <p v-if="!myAlerts.length" class="muted small">Chưa theo dõi sản phẩm nào. Bật trong <router-link to="/wishlist">Yêu thích</router-link>.</p>
        <div v-for="a in myAlerts" :key="a.id" class="row between alrow">
          <router-link :to="`/products/${a.productId}`">{{ a.name || 'Sản phẩm #' + a.productId }}</router-link>
          <span class="badge">{{ a.kind === 'PRICE' ? 'Giảm giá' : 'Có hàng' }}</span>
          <button class="btn btn-sm btn-outline" @click="delAlert(a)">Tắt</button>
        </div>
      </div>
    </div>

    <div v-if="addr" class="modal-bg" @click.self="addr = null">
      <div class="modal">
        <h3>{{ addr.id ? 'Sửa địa chỉ' : 'Thêm địa chỉ' }}</h3>
        <div v-if="msg.addr" class="alert alert-error">{{ msg.addr }}</div>
        <div class="form-grid" style="margin-top:12px">
          <div class="field"><label>Tên gợi nhớ</label><input v-model="addr.label" class="input" placeholder="Nhà riêng, Công ty..." /></div>
          <div class="field"><label>Người nhận *</label><input v-model="addr.receiverName" class="input" /></div>
          <div class="field"><label>Số điện thoại *</label><input v-model="addr.phone" class="input" /></div>
        </div>
        <AddressPicker v-model="addrPick" :cities="catalog.options.cities" />
        <label class="check field"><input v-model="addr.isDefault" type="checkbox" /> Đặt làm địa chỉ mặc định</label>
        <div class="row"><button class="btn" @click="saveAddr">Lưu</button><button class="btn btn-outline" @click="addr = null">Huỷ</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import Gauge from '../components/Gauge.vue'
import AccountSubscriptions from '../components/AccountSubscriptions.vue'
import AccountAffiliate from '../components/AccountAffiliate.vue'
import PushToggle from '../components/PushToggle.vue'
import AddressPicker from '../components/AddressPicker.vue'
import { toast } from '../stores/prefs'
const TIERS = [{ name: 'Đồng', label: 'mặc định', at: 0 }, { name: 'Bạc', label: 'chi 3 tr', at: 300 }, { name: 'Vàng', label: 'chi 10 tr', at: 1000 }, { name: 'Kim cương', label: 'chi 20 tr', at: 2000 }]
import { useAuth } from '../stores/auth'
import { ago, catalog, loadCatalog, vnd, dt, d, ORDER_STATUS, PAY_METHOD, PAY_STATUS, AUCTION_STATUS, RETURN_STATUS, TICKET_STATUS } from '../format'

const auth = useAuth(), route = useRoute(), router = useRouter()
const tabs = { profile: 'Hồ sơ', addresses: 'Sổ địa chỉ', orders: 'Đơn hàng', points: 'Điểm thưởng', wallet: 'Ví & quà tặng', returns: 'Đổi trả', tickets: 'Hỗ trợ', bids: 'Đấu giá của tôi', subscriptions: 'Mua định kỳ', affiliate: 'Tiếp thị liên kết', settings: 'Cài đặt' }
const nextTier = computed(() => TIERS.find(t => t.at > (pts.value.points || 0)) || { at: 2000 })
const tab = ref(tabs[route.query.tab] ? route.query.tab : 'profile')
const profile = reactive({ fullName: auth.user.fullName, phone: auth.user.phone, avatarUrl: auth.user.avatarUrl })
const pw = reactive({ currentPassword: '', newPassword: '' })
const bids = ref([])
const msg = reactive({}), addresses = ref([]), orders = ref([]), pts = ref({}), returnsList = ref([]), ticketsList = ref([]), addr = ref(null)

const payPref = ref(auth.user.preferredPayment || ''), myAlerts = ref([])
const savePay = async () => { try { await api.put('/account/payment-preference', { method: payPref.value }); auth.user.preferredPayment = payPref.value || null; toast('Đã lưu phương thức ưa thích') } catch (e) { toast(errMsg(e), 'error') } }
const delAlert = async a => { await api.delete(`/alerts/${a.id}`); myAlerts.value = myAlerts.value.filter(x => x !== a) }
const loadAlerts = async () => { const list = (await api.get('/alerts')).data; await Promise.all(list.map(async a => { try { a.name = (await api.get(`/products/${a.productId}`)).data.product.name } catch {} })); myAlerts.value = list }
const setTab = k => router.replace({ query: { tab: k } })
const w = ref({}), gcCode = ref(''), tf = ref(null), tfQr = ref(''), otp = ref('')
async function redeem() { try { const { data } = await api.post('/account/giftcards/redeem', { code: gcCode.value }); gcCode.value = ''; toast(`Đã nạp ${vnd(data.added)} vào ví`); load() } catch (e) { toast(errMsg(e), 'error') } }
const copyRef = () => { const u = `${location.origin}/register?ref=${w.value.referral.code}`; navigator.clipboard?.writeText(u); toast('Đã sao chép liên kết giới thiệu') }
async function setup2fa() {
  msg.tf = ''; tf.value = (await api.post('/account/2fa/setup')).data
  try { const QR = await import('qrcode'); tfQr.value = await QR.toDataURL(tf.value.otpauth, { margin: 1, width: 320 }) } catch { tfQr.value = '' }
}
async function on2fa() { try { await api.post('/account/2fa/enable', { code: otp.value }); auth.user.totpEnabled = true; localStorage.setItem('user', JSON.stringify(auth.user)); tf.value = null; otp.value = ''; msg.tf = 'Đã bật xác thực 2 bước'; msg.tfOk = true } catch (e) { msg.tf = errMsg(e); msg.tfOk = false } }
async function off2fa() { try { await api.post('/account/2fa/disable', { code: otp.value }); auth.user.totpEnabled = false; localStorage.setItem('user', JSON.stringify(auth.user)); otp.value = ''; msg.tf = 'Đã tắt xác thực 2 bước'; msg.tfOk = true } catch (e) { msg.tf = errMsg(e); msg.tfOk = false } }
async function makeDefault(a) { await api.put(`/account/addresses/${a.id}`, { ...a, isDefault: true }); toast('Đã đặt làm địa chỉ mặc định'); load() }
watch(() => route.query.tab, t => { tab.value = tabs[t] ? t : 'profile'; load() })

async function load() {
  if (tab.value === 'settings') loadAlerts()
  if (tab.value === 'wallet') w.value = (await api.get('/account/wallet')).data
  if (tab.value === 'addresses') { loadCatalog() }
  if (tab.value === 'addresses') addresses.value = (await api.get('/account/addresses')).data
  if (tab.value === 'orders') orders.value = (await api.get('/account/orders')).data
  if (tab.value === 'bids') bids.value = (await api.get('/account/bids')).data
  if (tab.value === 'points') pts.value = (await api.get('/account/points')).data
  if (tab.value === 'returns') returnsList.value = (await api.get('/account/returns')).data
  if (tab.value === 'tickets') ticketsList.value = (await api.get('/tickets')).data
}
async function saveProfile() { auth.user = (await api.put('/account/profile', profile)).data; msg.profile = 'Đã lưu thông tin' }
async function changePw() {
  try { await api.put('/account/password', pw); msg.pw = 'Đã đổi mật khẩu'; msg.pwOk = true; pw.currentPassword = pw.newPassword = '' }
  catch (e) { msg.pw = errMsg(e); msg.pwOk = false }
}
const addrPick = ref({})
const editAddr = a => { msg.addr = ''; addr.value = a ? { ...a } : { receiverName: auth.user.fullName, phone: auth.user.phone, isDefault: false }; addrPick.value = { province: addr.value.city || '', district: addr.value.district || '', ward: addr.value.ward || '', line: addr.value.line || '' } }
watch(addrPick, v => { if (addr.value) Object.assign(addr.value, { city: v.province, district: v.district, ward: v.ward, line: v.line }) })
async function saveAddr() {
  const a = addr.value
  if (!a.receiverName || !a.phone || !a.line || !a.city) { msg.addr = 'Vui lòng nhập đủ các trường bắt buộc'; return }
  a.id ? await api.put(`/account/addresses/${a.id}`, a) : await api.post('/account/addresses', a)
  addr.value = null; load()
}
async function delAddr(a) { if (confirm('Xoá địa chỉ này?')) { await api.delete(`/account/addresses/${a.id}`); load() } }
onMounted(load)
</script>
<style scoped>
.wcard { display: flex; flex-direction: column; gap: 8px; background: linear-gradient(135deg, #111 0 62%, var(--accent) 62%); color: #fff; }
.wcard small { letter-spacing: .2em; text-transform: uppercase; font-size: .7rem; opacity: .8; } .wcard .bal { font: 700 2rem 'Oswald', sans-serif; } .wcard .muted { color: #ddd; } .wcard a { color: #fff; }
.wallet form { gap: 8px; flex-wrap: nowrap; } .wallet .input { min-width: 0; }
.refcode { display: flex; gap: 10px; align-items: center; margin: 10px 0; flex-wrap: wrap; } .refcode code { font: 700 1.3rem 'Oswald', sans-serif; letter-spacing: .12em; border: 2px dashed var(--accent); padding: 4px 12px; }
.gc { padding: 6px 0; border-bottom: 1px solid var(--border); gap: 8px; flex-wrap: wrap; }
.mail { border-bottom: 1px solid var(--border); padding: 6px 0; } .mail summary { cursor: pointer; font-size: .86rem; } .mail pre { white-space: pre-wrap; font: .82rem/1.5 inherit; font-family: inherit; background: var(--bg-2); padding: 10px; margin-top: 6px; }
.tf { display: flex; gap: 14px; align-items: flex-start; flex-wrap: wrap; } .tf img { background: #fff; padding: 6px; border: 1px solid var(--border); } .tf > div { flex: 1 1 200px; min-width: 0; }
.key { font-size: .85rem; word-break: break-all; background: var(--bg-2); padding: 2px 6px; } .tf form { gap: 8px; flex-wrap: nowrap; margin-top: 10px; } .tf .input { min-width: 0; }

.addr p { margin: 8px 0; }
.pcard { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.tiers { display: flex; gap: 6px; } .tiers span { padding: 6px 10px; border-radius: 8px; background: var(--bg-2); font-size: .82rem; text-align: center; } .tiers span.on { background: var(--bronze); color: #fff; } .tiers small { display: block; font-size: .68rem; opacity: .8; }
.ok { color: var(--success); font-weight: 700; } .err { color: var(--danger); font-weight: 700; }
.paypref { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-top: 10px; }
.pp { display: flex; align-items: center; gap: 8px; padding: 10px; border: 1.5px solid var(--border); margin: 0; cursor: pointer; } .pp.on { border-color: var(--accent); background: var(--bronze-soft); }
.alrow { padding: 8px 0; border-bottom: 1px solid var(--border); gap: 8px; } .alrow a { flex: 1; min-width: 0; }
.tabs { overflow-x: auto; flex-wrap: nowrap; scrollbar-width: none; } .tabs button { flex-shrink: 0; }
</style>
