<template>
  <div>
    <h1>Gửi thông báo</h1>
    <p class="muted small">Gửi thông báo trong ứng dụng (chuông & trang Thông báo) tới nhóm khách hàng.</p>
    <div class="g2">
      <form class="card" @submit.prevent="send">
        <div class="field"><label>Đối tượng</label><select v-model="f.audience" class="input"><option value="ALL">Tất cả khách hàng</option><option value="GOLD">Hạng Vàng & Kim cương</option><option value="SILVER">Hạng Bạc</option><option value="BRONZE">Hạng Đồng</option></select></div>
        <div class="field"><label>Tiêu đề * <small class="muted">{{ f.title.length }}/80</small></label><input v-model="f.title" class="input" maxlength="80" required /></div>
        <div class="field"><label>Nội dung * <small class="muted">{{ f.body.length }}/500</small></label><textarea v-model="f.body" class="input" rows="4" maxlength="500" required /></div>
        <div class="field"><label>Liên kết (tuỳ chọn)</label><input v-model="f.link" class="input" placeholder="/promotions" pattern="/.*" title="Đường dẫn nội bộ, bắt đầu bằng /" /></div>
        <div class="row" style="gap:6px;flex-wrap:wrap;margin-bottom:10px"><button v-for="t in TPL" :key="t.title" type="button" class="btn btn-sm btn-outline" @click="Object.assign(f, t)">Mẫu: {{ t.title.slice(0, 18) }}…</button></div>
        <button class="btn" :disabled="busy">{{ busy ? 'Đang gửi…' : 'Gửi thông báo' }}</button>
        <p v-if="sent != null" class="ok">✓ Đã gửi tới {{ sent }} khách hàng.</p>
      </form>
      <div class="card"><h3>Xem trước</h3>
        <div class="pv"><span>📣</span><div><b>{{ f.title || 'Tiêu đề thông báo' }}</b><p>{{ f.body || 'Nội dung sẽ hiện ở đây.' }}</p><small class="muted">vừa xong</small></div></div>
        <p class="small muted" style="margin-top:12px">Tối đa 30 thông báo gần nhất hiển thị ở chuông. Không gửi email/SMS.</p></div>
    </div>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'
const TPL = [{ title: 'Flash sale cuối tuần — giảm đến 30%', body: 'Từ 20h tối nay, số lượng có hạn. Đặt nhắc nhở ngay!', link: '/products' }, { title: 'Hàng mới về: bộ sưu tập JDM Legends', body: 'Skyline, Supra, NSX — đủ tỉ lệ 1:64 đến 1:24.', link: '/collections/jdm-legends' }]
const f = reactive({ audience: 'ALL', title: '', body: '', link: '' }), busy = ref(false), sent = ref(null)
async function send() {
  if (!confirm('Gửi thông báo này?')) return
  busy.value = true; try { sent.value = (await api.post('/admin/notifications/broadcast', f)).data.sent; toast('Đã gửi') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
</script>
<style scoped>
.g2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 340px), 1fr)); gap: 16px; margin-top: 14px; } .g2 > * { min-width: 0; }
.pv { display: grid; grid-template-columns: 36px 1fr; gap: 10px; border: 1px solid var(--border); border-left: 4px solid var(--primary); padding: 12px; margin-top: 8px; } .pv span { font-size: 1.4rem; } .pv p { margin: 2px 0; font-size: .88rem; white-space: pre-wrap; overflow-wrap: anywhere; }
.ok { color: var(--success, #2e7d32); font-weight: 600; margin-top: 8px; }
</style>
