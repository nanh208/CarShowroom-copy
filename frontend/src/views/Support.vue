<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Dịch vụ & Hỗ trợ' }]" />
    <h1>Trung tâm hỗ trợ</h1>
    <p class="muted">Chúng tôi luôn sẵn sàng giúp bạn chọn đồ chơi, xử lý đơn hàng, bảo hành và kỹ thuật RC.</p>

    <div class="channels">
      <a v-reveal :href="`tel:${HOTLINE.replace(/\s/g, '')}`" class="ch"><span>📞</span><b>Hotline {{ HOTLINE }}</b><small>8:00 – 21:00 mỗi ngày, miễn phí</small></a>
      <button v-reveal class="ch" @click="openChat"><span>💬</span><b>Chat trực tuyến</b><small>Nhân viên trả lời trong vài phút</small></button>
      <a v-reveal href="#ticket" class="ch"><span>🎫</span><b>Gửi phiếu hỗ trợ</b><small>Phản hồi trong 24 giờ</small></a>
      <router-link v-reveal to="/faq" class="ch"><span>❓</span><b>Câu hỏi thường gặp</b><small>Vận chuyển, đổi trả, pin LiPo…</small></router-link>
    </div>

    <div class="grid grid-2 two">
      <section id="warranty" v-reveal class="card">
        <h3>🛡 Tra cứu bảo hành</h3>
        <p class="muted small">Nhập mã đơn hàng và số điện thoại đặt hàng.</p>
        <div class="form-grid"><input v-model="w.code" class="input" placeholder="Mã đơn (VD: TS2610...)" /><input v-model="w.phone" class="input" placeholder="Số điện thoại" /></div>
        <button class="btn btn-sm" style="margin-top:10px" :disabled="!w.code || !w.phone" @click="lookup">Tra cứu</button>
        <div v-if="wErr" class="alert alert-error" style="margin-top:10px">{{ wErr }}</div>
        <div v-if="wr" class="wres">
          <p>Đơn <b>{{ wr.code }}</b> · {{ wr.deliveredAt ? 'giao ngày ' + d(wr.deliveredAt) : 'chưa giao hàng' }}</p>
          <div v-for="i in wr.items" :key="i.name" class="witem">
            <img loading="lazy" decoding="async" v-if="i.image" :src="i.image" alt="" /><div class="grow"><b>{{ i.name }}</b><small v-if="!i.months">Không áp dụng bảo hành — đổi mới 7 ngày nếu lỗi</small><small v-else>Bảo hành {{ i.months }} tháng</small></div>
            <span v-if="i.valid === true" class="badge badge-COMPLETED">Còn hạn đến {{ d(i.until) }}</span>
            <span v-else-if="i.valid === false" class="badge badge-CANCELLED">Hết hạn {{ d(i.until) }}</span>
            <span v-else class="badge">—</span>
          </div>
        </div>
      </section>

      <section id="ticket" v-reveal class="card">
        <h3>🎫 Gửi phiếu hỗ trợ</h3>
        <template v-if="auth.loggedIn">
          <div class="form-grid">
            <select v-model="t.category" class="input"><option v-for="c in cats" :key="c">{{ c }}</option></select>
            <input v-model="t.orderCode" class="input" placeholder="Mã đơn liên quan (nếu có)" />
          </div>
          <input v-model="t.subject" class="input" placeholder="Tiêu đề" style="margin-top:8px" />
          <textarea v-model="t.content" class="input" rows="4" placeholder="Mô tả chi tiết vấn đề của bạn…" style="margin-top:8px" />
          <Attachments v-model="t.attachments" style="margin-top:8px" />
          <div v-if="tErr" class="alert alert-error">{{ tErr }}</div>
          <button class="btn btn-sm" style="margin-top:10px" @click="createTicket">Gửi phiếu</button>
          <div v-if="mine.length" class="mine"><h4>Phiếu của bạn</h4>
            <router-link v-for="x in mine" :key="x.id" :to="`/support/tickets/${x.code}`" class="trow"><b>{{ x.code }}</b><span class="grow">{{ x.subject }}</span><span :class="['badge', 'badge-' + x.status]">{{ TICKET_STATUS[x.status] }}</span></router-link>
          </div>
        </template>
        <p v-else class="muted"><router-link :to="{ path: '/login', query: { redirect: '/support#ticket' } }">Đăng nhập</router-link> để gửi và theo dõi phiếu hỗ trợ. Hoặc chat trực tuyến không cần đăng nhập.</p>
      </section>
    </div>

    <section v-reveal class="card policy">
      <div><b>🚚 Vận chuyển</b><p>Nội thành HCM/HN 20.000đ, lân cận 30.000đ, tỉnh khác 40.000đ. Freeship cho đơn từ {{ vnd(catalog.options.freeShipFrom) }}.</p></div>
      <div><b>↩ Đổi trả 7 ngày</b><p>Gửi yêu cầu trong trang đơn hàng kèm ảnh. Lỗi nhà sản xuất: ARES chịu phí ship 2 chiều.</p></div>
      <div><b>🔋 Pin LiPo</b><p>Luôn sạc bằng bộ sạc cân bằng, không để pin phồng, cất giữ ở mức 3.8V/cell.</p></div>
    </section>
  </div>
</template>
<script setup>
import Attachments from '../components/Attachments.vue'
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Breadcrumb from '../components/Breadcrumb.vue'
import api, { errMsg } from '../api'
import { useAuth } from '../stores/auth'
import { vnd, d, HOTLINE, TICKET_STATUS, catalog, loadCatalog } from '../format'
import { toast } from '../stores/prefs'
const auth = useAuth(), route = useRoute(), router = useRouter()
const cats = ['Đơn hàng', 'Thanh toán', 'Vận chuyển', 'Đổi trả', 'Bảo hành', 'Kỹ thuật', 'Khác']
const w = reactive({ code: route.query.order || '', phone: auth.user?.phone || '' }), wr = ref(null), wErr = ref('')
const t = reactive({ category: route.query.order ? 'Đơn hàng' : 'Khác', orderCode: route.query.order || '', subject: '', content: '', attachments: [] }), tErr = ref(''), mine = ref([])
const openChat = () => dispatchEvent(new Event('open-chat'))
async function lookup() { wErr.value = ''; wr.value = null; try { wr.value = (await api.get('/warranty', { params: w })).data } catch (e) { wErr.value = errMsg(e) } }
async function createTicket() {
  tErr.value = ''
  try { const { data } = await api.post('/tickets', t); toast('Đã tạo phiếu ' + data.code); router.push(`/support/tickets/${data.code}`) } catch (e) { tErr.value = errMsg(e) }
}
onMounted(async () => { loadCatalog(); if (auth.loggedIn) mine.value = (await api.get('/tickets')).data.slice(0, 5) })
</script>
<style scoped>
.channels { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 18px 0; }
.ch { position: relative; display: flex; flex-direction: column; gap: 4px; padding: 18px; border-radius: 12px; background: var(--bronze-soft); color: var(--text); border: none; text-align: left; font: inherit; cursor: pointer; }
.ch span { font-size: 1.6rem; } .ch small { color: var(--muted); }
.two { gap: 16px; align-items: start; } .card h3 { margin-bottom: 6px; }
.wres { margin-top: 14px; } .witem { display: flex; gap: 10px; align-items: center; padding: 8px 0; border-top: 1px solid var(--border); } .witem img { width: 54px; height: 40px; object-fit: cover; border-radius: 6px; } .witem small { display: block; color: var(--muted); }
.mine { margin-top: 16px; } .mine h4 { margin-bottom: 6px; } .trow { display: flex; gap: 10px; align-items: center; padding: 6px 0; border-top: 1px solid var(--border); color: var(--text); font-size: .88rem; }
.policy { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-top: 16px; } .policy p { color: var(--muted); font-size: .88rem; margin-top: 4px; }
@media (max-width: 860px) { .channels, .policy { grid-template-columns: 1fr 1fr; } }
</style>
