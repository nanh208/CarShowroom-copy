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
          <div class="row"><button class="btn btn-sm btn-outline" @click="editAddr(a)">Sửa</button><button class="btn btn-sm btn-outline" @click="delAddr(a)">Xoá</button></div>
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

    <!-- Lịch hẹn -->
    <div v-if="tab === 'bookings'" class="card table-wrap">
      <div v-if="!bookings.length" class="empty">Chưa có lịch hẹn. <router-link to="/services">Đặt lịch</router-link></div>
      <table v-else class="table">
        <thead><tr><th>#</th><th>Loại</th><th>Nội dung</th><th>Ngày / giờ</th><th>Trạng thái</th><th /></tr></thead>
        <tbody><tr v-for="b in bookings" :key="b.id">
          <td>{{ b.id }}</td><td>{{ BOOKING_TYPE[b.type] }}</td><td class="small">{{ b.productName || b.serviceType }}<br /><span class="muted">{{ b.note }}</span></td>
          <td>{{ d(b.date) }}<br /><span class="small muted">{{ b.timeSlot }}</span></td>
          <td><span :class="['badge', 'badge-' + b.status]">{{ BOOKING_STATUS[b.status] }}</span></td>
          <td><button v-if="b.status === 'PENDING'" class="btn btn-sm btn-outline" @click="cancelBooking(b)">Huỷ</button></td>
        </tr></tbody>
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
          <td><router-link v-if="b.won && b.orderCode" :to="`/orders/${b.orderCode}`" class="btn btn-sm">Đặt cọc</router-link></td>
        </tr></tbody>
      </table>
    </div>

    <div v-if="addr" class="modal-bg" @click.self="addr = null">
      <div class="modal">
        <h3>{{ addr.id ? 'Sửa địa chỉ' : 'Thêm địa chỉ' }}</h3>
        <div v-if="msg.addr" class="alert alert-error">{{ msg.addr }}</div>
        <div class="form-grid" style="margin-top:12px">
          <div class="field"><label>Tên gợi nhớ</label><input v-model="addr.label" class="input" placeholder="Nhà riêng, Công ty..." /></div>
          <div class="field"><label>Người nhận *</label><input v-model="addr.receiverName" class="input" /></div>
          <div class="field"><label>Số điện thoại *</label><input v-model="addr.phone" class="input" /></div>
          <div class="field"><label>Tỉnh / Thành phố *</label><input v-model="addr.city" class="input" /></div>
          <div class="field"><label>Quận / Huyện</label><input v-model="addr.district" class="input" /></div>
          <div class="field"><label>Phường / Xã</label><input v-model="addr.ward" class="input" /></div>
        </div>
        <div class="field"><label>Số nhà, tên đường *</label><input v-model="addr.line" class="input" /></div>
        <label class="check field"><input v-model="addr.isDefault" type="checkbox" /> Đặt làm địa chỉ mặc định</label>
        <div class="row"><button class="btn" @click="saveAddr">Lưu</button><button class="btn btn-outline" @click="addr = null">Huỷ</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api, { errMsg } from '../api'
import { useAuth } from '../stores/auth'
import { vnd, dt, d, ORDER_STATUS, PAY_METHOD, PAY_STATUS, BOOKING_TYPE, BOOKING_STATUS, AUCTION_STATUS } from '../format'

const auth = useAuth(), route = useRoute(), router = useRouter()
const tabs = { profile: 'Hồ sơ', addresses: 'Sổ địa chỉ', orders: 'Lịch sử giao dịch', bookings: 'Lịch sử đặt lịch', bids: 'Đấu giá của tôi' }
const tab = ref(tabs[route.query.tab] ? route.query.tab : 'profile')
const profile = reactive({ fullName: auth.user.fullName, phone: auth.user.phone, avatarUrl: auth.user.avatarUrl })
const pw = reactive({ currentPassword: '', newPassword: '' })
const bids = ref([])
const msg = reactive({}), addresses = ref([]), orders = ref([]), bookings = ref([]), addr = ref(null)

const setTab = k => router.replace({ query: { tab: k } })
watch(() => route.query.tab, t => { tab.value = tabs[t] ? t : 'profile'; load() })

async function load() {
  if (tab.value === 'addresses') addresses.value = (await api.get('/account/addresses')).data
  if (tab.value === 'orders') orders.value = (await api.get('/account/orders')).data
  if (tab.value === 'bids') bids.value = (await api.get('/account/bids')).data
  if (tab.value === 'bookings') bookings.value = (await api.get('/account/bookings')).data
}
async function saveProfile() { auth.user = (await api.put('/account/profile', profile)).data; msg.profile = 'Đã lưu thông tin' }
async function changePw() {
  try { await api.put('/account/password', pw); msg.pw = 'Đã đổi mật khẩu'; msg.pwOk = true; pw.currentPassword = pw.newPassword = '' }
  catch (e) { msg.pw = errMsg(e); msg.pwOk = false }
}
const editAddr = a => { msg.addr = ''; addr.value = a ? { ...a } : { receiverName: auth.user.fullName, phone: auth.user.phone, isDefault: false } }
async function saveAddr() {
  const a = addr.value
  if (!a.receiverName || !a.phone || !a.line || !a.city) { msg.addr = 'Vui lòng nhập đủ các trường bắt buộc'; return }
  a.id ? await api.put(`/account/addresses/${a.id}`, a) : await api.post('/account/addresses', a)
  addr.value = null; load()
}
async function delAddr(a) { if (confirm('Xoá địa chỉ này?')) { await api.delete(`/account/addresses/${a.id}`); load() } }
async function cancelBooking(b) { if (confirm('Huỷ lịch hẹn này?')) { await api.post(`/bookings/${b.id}/cancel`); load() } }
onMounted(load)
</script>
<style scoped>
.addr p { margin: 8px 0; }
</style>
