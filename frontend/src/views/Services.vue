<template>
  <div class="container page">
    <h1>Dịch vụ & Hậu mãi</h1>
    <p class="muted" style="margin-bottom:22px">Tư vấn mua xe, đăng ký lái thử, sửa chữa – bảo dưỡng và chăm sóc xe trọn đời.</p>
    <div class="grid svc">
      <div class="card"><div class="ico">💬</div><h3>Tư vấn trực tuyến</h3><p class="muted small">Chat với nhân viên ở góc phải màn hình, phản hồi trong vài phút (8:00–21:00).</p></div>
      <div class="card"><div class="ico">📞</div><h3>Hotline {{ HOTLINE }}</h3><p class="muted small">Gọi miễn phí để được tư vấn chọn mẫu, kiểm tra đơn hàng và bảo hành.</p>
        <a :href="`tel:${HOTLINE.replace(/ /g, '')}`" class="btn btn-sm" style="margin-top:8px">Gọi ngay</a></div>
      <div class="card"><div class="ico">📍</div><h3>Showroom</h3><p class="muted small">123 Nguyễn Văn Linh, Q.7, TP.HCM — trưng bày hơn 50 mẫu xe, sẵn xe lái thử mỗi ngày.</p></div>
    </div>

    <div class="card form-card">
      <div class="tabs">
        <button :class="{ active: tab === 'viewing' }" @click="tab = 'viewing'">🚗 Đăng ký lái thử</button>
        <button :class="{ active: tab === 'maintenance' }" @click="tab = 'maintenance'">🔧 Đặt lịch sửa chữa & bảo dưỡng</button>
      </div>
      <p class="muted small" style="margin-bottom:16px">{{ tab === 'viewing'
        ? 'Chọn mẫu xe, ngày và khung giờ — chuyên viên sẽ chuẩn bị xe, lộ trình lái thử 15–30 phút cùng tư vấn trả góp. Mang theo bằng lái B2 trở lên.'
        : 'Bảo dưỡng định kỳ, sửa chữa máy – gầm, đồng sơn, thay lốp – ắc quy, cứu hộ 24/7. Có dịch vụ nhận & giao xe tận nhà.' }}</p>
      <div v-if="ok" class="alert alert-success">✔ Đã gửi yêu cầu! Mã lịch hẹn #{{ ok.id }} — chúng tôi sẽ gọi xác nhận trong 30 phút.
        <router-link v-if="auth.loggedIn" to="/account?tab=bookings">Xem lịch hẹn</router-link></div>
      <div v-if="err" class="alert alert-error">{{ err }}</div>
      <div class="form-grid">
        <div class="field"><label>Họ và tên *</label><input v-model="f.name" class="input" /></div>
        <div class="field"><label>Số điện thoại *</label><input v-model="f.phone" class="input" /></div>
        <div class="field"><label>Email</label><input v-model="f.email" class="input" /></div>
        <div v-if="tab === 'viewing'" class="field"><label>Mẫu xe muốn lái thử</label>
          <select v-model="f.productId" class="input"><option :value="null">— Chưa chọn —</option><option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }}</option></select></div>
        <div v-else class="field"><label>Loại dịch vụ *</label>
          <select v-model="f.serviceType" class="input"><option v-for="s in services" :key="s">{{ s }}</option></select></div>
        <div class="field"><label>Ngày hẹn *</label><input v-model="f.date" type="date" :min="today" class="input" /></div>
        <div class="field"><label>Khung giờ *</label>
          <select v-model="f.timeSlot" class="input"><option v-for="t in slots" :key="t">{{ t }}</option></select></div>
      </div>
      <div class="field"><label>{{ tab === 'viewing' ? 'Ghi chú' : 'Mô tả tình trạng xe (biển số, số km...)' }}</label><textarea v-model="f.note" rows="3" class="input" /></div>
      <button class="btn" :disabled="busy" @click="submit">{{ busy ? 'Đang gửi...' : 'Gửi đăng ký' }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api, { errMsg } from '../api'
import { HOTLINE } from '../format'
import { useAuth } from '../stores/auth'
const route = useRoute(), auth = useAuth()
const tab = ref(route.query.tab === 'maintenance' ? 'maintenance' : 'viewing')
const products = ref([]), ok = ref(null), err = ref(''), busy = ref(false)
const today = new Date().toISOString().slice(0, 10)
const slots = ['09:00 – 10:00', '10:00 – 11:00', '14:00 – 15:00', '15:00 – 16:00', '16:00 – 17:00', '19:00 – 20:00']
const services = ['Bảo dưỡng định kỳ', 'Sửa chữa máy – gầm', 'Đồng sơn', 'Thay lốp / ắc quy', 'Kiểm tra trước chuyến đi xa', 'Bảo hành', 'Khác']
const f = reactive({ name: auth.user?.fullName || '', phone: auth.user?.phone || '', email: auth.user?.email || '',
  productId: route.query.productId ? Number(route.query.productId) : null, serviceType: services[0], date: today, timeSlot: slots[0], note: '' })
watch(tab, () => { ok.value = null; err.value = '' })
async function submit() {
  err.value = ''; ok.value = null; busy.value = true
  try {
    const body = { ...f, type: tab.value === 'viewing' ? 'TEST_DRIVE' : 'MAINTENANCE' }
    if (tab.value === 'viewing') body.serviceType = 'Lái thử'; else body.productId = null
    ok.value = (await api.post('/bookings', body)).data
  } catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
onMounted(async () => { products.value = (await api.get('/products', { params: { size: 60, sort: 'name' } })).data.content.filter(p => p.type !== 'ACCESSORY') })
</script>
<style scoped>
.svc { grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); margin-bottom: 26px; }
.ico { font-size: 2rem; margin-bottom: 6px; }
.form-card { max-width: 900px; }
</style>
