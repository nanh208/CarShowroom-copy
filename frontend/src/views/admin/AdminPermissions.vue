<template>
  <div>
    <h1>Phân quyền nhân viên</h1>
    <p class="muted" style="margin:-16px 0 18px">Chọn các mục tài khoản <b>STAFF</b> được truy cập. Quản trị viên (ADMIN) luôn có toàn quyền; Báo cáo, Nhật ký và Phân quyền chỉ dành cho ADMIN.</p>
    <div class="card table-wrap">
      <table class="table matrix">
        <thead><tr><th>Mục</th><th>ADMIN</th><th>STAFF</th></tr></thead>
        <tbody>
          <tr v-for="s in sections" :key="s"><td>{{ LABEL[s] || s }}</td><td><input type="checkbox" checked disabled :aria-label="`ADMIN: ${LABEL[s]}`" /></td>
            <td><input v-model="staff" type="checkbox" :value="s" :aria-label="`STAFF: ${LABEL[s]}`" /></td></tr>
          <tr v-for="s in ADMIN_ONLY" :key="s" class="muted"><td>{{ s }}</td><td><input type="checkbox" checked disabled /></td><td><input type="checkbox" disabled /></td></tr>
        </tbody>
      </table>
    </div>
    <div class="row" style="margin-top:14px"><button class="btn" :disabled="busy" @click="save">Lưu phân quyền</button><button class="btn btn-outline" @click="staff = [...sections]">Chọn tất cả</button><button class="btn btn-outline" @click="staff = []">Bỏ chọn</button></div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'
const LABEL = { products: 'Sản phẩm', orders: 'Đơn hàng', customers: 'Khách hàng', inventory: 'Kho hàng', returns: 'Đổi trả / hoàn tiền', promotions: 'Voucher', 'flash-sales': 'Flash sale', banners: 'Banner', catalog: 'Danh mục & hãng', auctions: 'Đấu giá', questions: 'Hỏi đáp', reviews: 'Đánh giá', tickets: 'Phiếu hỗ trợ', chat: 'Live chat' }
const ADMIN_ONLY = ['Báo cáo CSV', 'Nhật ký thao tác', 'Phân quyền nhân viên']
const sections = ref([]), staff = ref([]), busy = ref(false)
onMounted(async () => { const { data } = await api.get('/admin/permissions'); sections.value = data.sections; staff.value = data.staff })
async function save() { busy.value = true; try { const { data } = await api.put('/admin/permissions', { staff: staff.value }); staff.value = data.staff; toast('Đã lưu phân quyền') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
</script>
<style scoped>.matrix td:not(:first-child), .matrix th:not(:first-child) { text-align: center; width: 110px; } .matrix input { width: 18px; height: 18px; accent-color: var(--accent); }</style>
