<template>
  <div>
    <h1>Quản lý khách hàng</h1>
    <div class="row" style="margin-bottom:14px"><input v-model="q" class="input" style="max-width:320px" placeholder="Tìm theo tên, email, SĐT..." aria-label="Tìm khách" />
      <select v-model="tagF" class="input" style="width:auto" aria-label="Lọc theo nhãn"><option value="">Mọi nhãn</option><option v-for="t in allTagNames" :key="t">{{ t }}</option></select></div>
    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>ID</th><th>Họ tên</th><th>Email</th><th>SĐT</th><th>Đăng nhập</th><th>Vai trò</th><th>Hạng / Điểm</th><th>Số đơn</th><th>Tổng chi tiêu</th><th>Ngày tạo</th><th /></tr></thead>
        <tbody><tr v-for="c in pg.items.value" :key="c.user.id" :class="{ off: c.user.locked }">
          <td>{{ c.user.id }}</td><td><b>{{ c.user.fullName }}</b><div class="tags"><span v-for="t in tags[c.user.id] || []" :key="t" class="tg">{{ t }}</span><button class="lnk" @click="openCrm(c.user)">{{ (tags[c.user.id] || []).length ? 'Sửa' : '+ Nhãn / ghi chú' }}</button></div></td><td>{{ c.user.email }}</td><td>{{ c.user.phone }}</td>
          <td class="small">{{ c.user.provider }}</td>
          <td><select v-if="auth.isAdmin" class="input sel" :value="c.user.role" @change="setRole(c.user, $event.target.value)"><option>USER</option><option>STAFF</option><option>ADMIN</option></select><span v-else class="badge">{{ c.user.role }}</span></td>
          <td>
            <b>{{ c.user.tier }}</b>
            <div class="small">{{ c.user.points }} điểm</div>
            <button class="btn btn-xs btn-outline" style="margin-top:4px" @click="openVipModal(c.user)">Cấp thẻ &amp; Đổi hạng</button>
          </td>
          <td>{{ c.orderCount }}</td><td class="price">{{ vnd(c.totalSpent) }}</td><td class="small">{{ d(c.user.createdAt) }}</td>
          <td><button :class="['btn btn-sm', c.user.locked ? '' : 'btn-danger']" @click="patch(c.user, { locked: !c.user.locked })">{{ c.user.locked ? 'Mở khoá' : 'Khoá' }}</button></td>
        </tr></tbody>
      </table>
      <p v-if="!filtered.length" class="muted" style="padding:12px">Không có khách hàng phù hợp.</p>
      <Pager :p="pg" />
    </div>
    <div v-if="err" class="alert alert-error" style="margin-top:12px">{{ err }}</div>

    <div v-if="crm" class="modal-bg" @click.self="crm = null">
      <div class="modal">
        <h3>Ghi chú & nhãn — {{ crm.user.fullName }}</h3>
        <p class="small muted">Chỉ nhân viên nhìn thấy. {{ crm.user.email }} · {{ crm.user.phone }}</p>
        <div class="field" style="margin-top:12px"><label>Nhãn</label>
          <div class="tags edit"><span v-for="(t, i) in crm.tags" :key="t" class="tg">{{ t }}<button :aria-label="`Bỏ nhãn ${t}`" @click="crm.tags.splice(i, 1)">×</button></span>
            <input v-model="newTag" class="input tin" placeholder="Thêm nhãn, Enter" list="tag-sug" @keydown.enter.prevent="addTag" /><datalist id="tag-sug"><option v-for="t in SUGGEST" :key="t" :value="t" /></datalist></div>
          <div class="row" style="gap:6px;margin-top:6px"><button v-for="t in SUGGEST.filter(x => !crm.tags.includes(x))" :key="t" class="btn btn-sm btn-outline" @click="crm.tags.push(t)">+ {{ t }}</button></div></div>
        <div class="field"><label>Ghi chú nội bộ</label><textarea v-model="crm.note" class="input" rows="5" maxlength="2000" placeholder="VD: Sưu tầm Mini GT, thích hộp nguyên seal; gọi trước 17h…" /></div>
        <div class="row"><button class="btn" @click="saveCrm">Lưu</button><button class="btn btn-outline" @click="crm = null">Huỷ</button></div>
        <div class="tl"><b>Dòng thời gian khách hàng</b> <small class="muted">({{ crm.timeline?.length || 0 }} sự kiện)</small>
          <div v-if="!crm.timeline" class="sk" style="height:80px;margin-top:8px" />
          <ol v-else><li v-for="(e, i) in crm.timeline" :key="i" :class="'k-' + e.kind"><small class="muted">{{ dt(e.at) }}</small><b>{{ e.title }}</b><span v-if="e.detail" class="small">{{ e.detail }}</span>
            <router-link v-if="e.link" :to="e.link" class="small" @click="crm = null">Mở →</router-link></li><li v-if="!crm.timeline.length" class="muted">Chưa có hoạt động.</li></ol></div>
      </div>
    </div>
  </div>
    <!-- Modal Admin cấp thẻ, nâng hạng và tặng điểm/credit -->
    <div v-if="vipModal" class="modal-bg" @click.self="vipModal = null">
      <div class="modal vip-admin-modal" style="max-width:520px">
        <div class="row between" style="align-items:center;margin-bottom:14px;border-bottom:1px solid var(--border);padding-bottom:10px">
          <div>
            <h3 style="margin:0">CẤP THẺ &amp; NÂNG HẠNG VIP</h3>
            <p class="small muted" style="margin:2px 0 0">{{ vipModal.user.fullName }} &bull; {{ vipModal.user.email }}</p>
          </div>
          <button class="btn btn-sm btn-outline" @click="vipModal = null">&times;</button>
        </div>

        <div class="field">
          <label><b>Hạng thành viên VIP</b></label>
          <select v-model="vipModal.tier" class="input">
            <option value="Đồng">Hạng Đồng (Mặc định)</option>
            <option value="Bạc">Hạng Bạc (Chi từ 3 tr)</option>
            <option value="Vàng">Hạng Vàng (Chi từ 10 tr)</option>
            <option value="Titan">Hạng Titan VIP (Chi từ 20 tr - Thẻ đen Titan nguyên khối tối cao)</option>
            <option value="Bạch kim">Hạng Bạch Kim (Cũ - Tự động đồng bộ Titan)</option>
            <option value="Kim cương">Hạng Kim Cương</option>
          </select>
        </div>

        <div class="field">
          <label><b>Mã số thẻ VIP tùy biến</b></label>
          <input
            v-model="vipModal.vipCardNumber"
            class="input"
            placeholder="VD: 8899 • 2026 • 0001 • 9999"
          />
          <small class="muted">Để trống nếu dùng mã dập nổi tự động của hệ thống</small>
        </div>

        <div class="grid grid-2" style="gap:12px">
          <div class="field">
            <label><b>Biến động điểm thưởng (+/-)</b></label>
            <input
              v-model.number="vipModal.pointsDelta"
              type="number"
              class="input"
              placeholder="+500 hoặc -100"
            />
            <small class="muted">Hiện có: {{ vipModal.user.points || 0 }} pts</small>
          </div>

          <div class="field">
            <label><b>Tặng tiền vào Ví (VNĐ)</b></label>
            <input
              v-model.number="vipModal.creditDelta"
              type="number"
              class="input"
              placeholder="+200000"
            />
            <small class="muted">Cộng số dư mua hàng trực tiếp</small>
          </div>
        </div>

        <div class="field">
          <label><b>Lý do cấp thẻ / điều chỉnh</b></label>
          <input
            v-model="vipModal.reason"
            class="input"
            placeholder="VD: Tri ân khách hàng thân thiết / Nâng hạng danh dự"
          />
        </div>

        <div class="row" style="margin-top:18px;justify-content:flex-end;gap:8px">
          <button class="btn btn-outline" @click="vipModal = null">Huỷ</button>
          <button class="btn" @click="saveVip">Xác nhận &amp; Cấp thẻ</button>
        </div>
      </div>
    </div>

</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import Pager from '../../components/Pager.vue'
import { usePaged } from '../../composables/usePaged'
import api, { errMsg } from '../../api'
import { vnd, d, dt } from '../../format'
import { useAuth } from '../../stores/auth'
const auth = useAuth()
import { useRoute } from 'vue-router'
import { toast } from '../../stores/prefs'
const list = ref([]), q = ref(useRoute().query.q || ''), err = ref(''), tags = ref({}), tagF = ref(''), crm = ref(null), newTag = ref('')
const SUGGEST = ['VIP', 'Sưu tầm 1:64', 'Chơi RC', 'Mua sỉ', 'Hay đổi trả', 'Cần gọi trước']
const allTagNames = computed(() => [...new Set(Object.values(tags.value).flat())])
const openCrm = async u => { const { data } = await api.get(`/admin/customers/${u.id}/crm`); crm.value = { user: u, note: data.note, tags: [...data.tags], timeline: null }; try { crm.value.timeline = (await api.get(`/admin/customers/${u.id}/timeline`)).data } catch { crm.value.timeline = [] } }
const addTag = () => { const t = newTag.value.trim(); if (t && !crm.value.tags.includes(t)) crm.value.tags.push(t); newTag.value = '' }
async function saveCrm() { try { const { data } = await api.put(`/admin/customers/${crm.value.user.id}/crm`, { note: crm.value.note, tags: crm.value.tags }); tags.value = { ...tags.value, [crm.value.user.id]: data.tags }; crm.value = null; toast('Đã lưu ghi chú khách') } catch (e) { toast(errMsg(e), 'error') } }
const filtered = computed(() => list.value.filter(c => !tagF.value || (tags.value[c.user.id] || []).includes(tagF.value)).filter(c => !q.value || [c.user.fullName, c.user.email, c.user.phone].join(' ').toLowerCase().includes(q.value.toLowerCase())))
const pg = usePaged(filtered, { size: 25 })
async function load() { list.value = (await api.get('/admin/customers')).data; tags.value = (await api.get('/admin/customer-tags')).data }
async function patch(u, body) { err.value = ''; try { await api.patch(`/admin/customers/${u.id}`, body) } catch (e) { err.value = errMsg(e) } load() }
async function setRole(u, role) { err.value = ''; try { await api.patch(`/admin/staff/${u.id}/role`, { role }) } catch (e) { err.value = errMsg(e) } load() }
function points(u) {
  const v = prompt(`Cộng/trừ điểm cho ${u.fullName} (vd 50 hoặc -20):`); if (!v || isNaN(+v)) return
  const reason = prompt('Lý do:', +v > 0 ? 'Tặng điểm tri ân' : 'Điều chỉnh điểm') || undefined
  patch(u, { pointsDelta: +v, reason })
}
const vipModal = ref(null)
function openVipModal(u) {
  vipModal.value = {
    user: u,
    tier: u.tier || 'Đồng',
    pointsDelta: 0,
    reason: '',
    creditDelta: 0,
    vipCardNumber: u.vipCardNumber || ''
  }
}
async function saveVip() {
  if (!vipModal.value) return
  err.value = ''
  try {
    const vm = vipModal.value
    const body = {
      tier: vm.tier,
      pointsDelta: vm.pointsDelta ? +vm.pointsDelta : undefined,
      reason: vm.reason || (vm.pointsDelta > 0 ? 'Admin cấp điểm thưởng VIP' : 'Admin điều chỉnh điểm'),
      creditDelta: vm.creditDelta ? +vm.creditDelta : undefined,
      vipCardNumber: vm.vipCardNumber?.trim() || undefined
    }
    await api.patch(`/admin/customers/${vm.user.id}`, body)
    toast(`Đã cập nhật đặc quyền & hạng VIP cho ${vm.user.fullName}`)
    vipModal.value = null
    await load()
  } catch (e) {
    err.value = errMsg(e)
    toast(errMsg(e), 'error')
  }
}

onMounted(load)
</script>
<style scoped>
.tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; align-items: center; } .tg { display: inline-flex; align-items: center; gap: 4px; padding: 1px 7px; background: #111; color: #fff; font-size: .7rem; font-weight: 600; } .tg button { background: none; border: none; color: #fff; cursor: pointer; }
.lnk { background: none; border: none; color: var(--accent); font: 600 .74rem inherit; font-family: inherit; cursor: pointer; padding: 0; } .tags.edit { padding: 6px; border: 1px solid var(--border); } .tin { flex: 1; min-width: 140px; border: none !important; padding: 4px !important; min-height: 0 !important; }.sel { padding: 5px 8px; width: 100px; } .off { opacity: .55; }.tl { border-top: 1px solid var(--border); margin-top: 14px; padding-top: 10px; } .tl ol { list-style: none; padding: 0 0 0 14px; margin: 8px 0 0; border-left: 2px solid var(--border); max-height: 320px; overflow-y: auto; }
.tl li { position: relative; display: flex; flex-direction: column; padding: 6px 0 6px 10px; font-size: .86rem; } .tl li::before { content: ''; position: absolute; left: -20px; top: 11px; width: 10px; height: 10px; border-radius: 50%; background: var(--muted); }
.tl li.k-ORDER::before { background: var(--primary); } .tl li.k-RETURN::before, .tl li.k-TICKET::before { background: var(--danger); } .tl li.k-POINTS::before { background: #c9a227; }
</style>
