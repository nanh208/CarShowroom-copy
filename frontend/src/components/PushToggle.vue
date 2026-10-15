<template>
  <div class="card">
    <h3>Thông báo đẩy</h3>
    <p class="muted small">Nhận thông báo đơn hàng, giảm giá và đơn định kỳ ngay trên điện thoại/máy tính — kể cả khi đã đóng trang.</p>
    <p v-if="!pushSupported" class="small">Trình duyệt này không hỗ trợ thông báo đẩy. Trên iPhone, hãy “Thêm vào MH chính” rồi mở từ biểu tượng ứng dụng.</p>
    <template v-else>
      <p v-if="pushPerm === 'denied'" class="small err">Bạn đã chặn quyền thông báo — bật lại trong cài đặt trình duyệt.</p>
      <div class="row">
        <button v-if="!pushOn" class="btn btn-sm" :disabled="busy || pushPerm === 'denied'" @click="on">Bật thông báo trên thiết bị này</button>
        <template v-else><span class="ok">✓ Đang bật</span><button class="btn btn-sm btn-outline" :disabled="busy" @click="test">Gửi thử</button><button class="btn btn-sm btn-outline" :disabled="busy" @click="off">Tắt</button></template>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../api'
import { pushSupported, pushOn, pushPerm, refreshPush, enablePush, disablePush } from '../push'
import { toast } from '../stores/prefs'
const busy = ref(false)
const run = async f => { busy.value = true; try { await f() } catch (e) { toast(e?.response ? errMsg(e) : e.message, 'error') } finally { busy.value = false } }
const on = () => run(async () => { await enablePush(); toast('Đã bật thông báo đẩy') })
const off = () => run(async () => { await disablePush(); toast('Đã tắt thông báo') })
const test = () => run(async () => { const { data } = await api.post('/push/test'); toast(data.delivered ? 'Đã gửi thông báo thử' : 'Chưa gửi được — thử bật lại', data.delivered ? 'ok' : 'error') })
onMounted(refreshPush)
</script>
<style scoped>.err { color: var(--danger); } .ok { color: #17803d; font-weight: 600; }</style>
