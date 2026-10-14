<template>
  <div class="cr">
    <button type="button" class="btn btn-sm btn-outline" :aria-expanded="open" @click="open = !open">Mẫu trả lời ▾</button>
    <div v-if="open" class="cr-pop" @keydown.esc="open = false">
      <input ref="inp" v-model="q" class="input" placeholder="Tìm mẫu…" aria-label="Tìm mẫu trả lời" />
      <ul>
        <li v-for="(c, i) in shown" :key="c.t + i"><button type="button" class="use" @click="use(c)"><b>{{ c.t }}</b><small>{{ fill(c.b) }}</small></button><button type="button" class="del" :aria-label="`Xoá mẫu ${c.t}`" @click="del(c)">×</button></li>
        <li v-if="!shown.length" class="muted small" style="padding:8px">Không có mẫu.</li>
      </ul>
      <div class="add"><button type="button" class="lnk" :disabled="!current?.trim()" @click="saveCurrent">+ Lưu nội dung đang soạn thành mẫu</button></div>
      <small class="muted">Biến: {ten} = tên khách, {ma_don} = mã đơn</small>
    </div>
  </div>
</template>
<script setup>
// Mẫu trả lời nhanh cho chat & phiếu hỗ trợ (lưu trên trình duyệt của nhân viên)
import { ref, computed, watch, nextTick } from 'vue'
const props = defineProps({ name: String, order: String, current: String })
const emit = defineEmits(['insert'])
const DEF = [
  { t: 'Chào hỏi', b: 'Chào {ten}, ARES Toys xin chào! Mình có thể hỗ trợ gì cho bạn ạ?' },
  { t: 'Đang kiểm tra đơn', b: 'Dạ {ten} chờ mình chút, mình đang kiểm tra đơn {ma_don} và phản hồi ngay ạ.' },
  { t: 'Thời gian giao', b: 'Đơn nội thành HCM/Hà Nội giao 1–2 ngày, các tỉnh 2–4 ngày. Bạn theo dõi hành trình trong mục Đơn hàng nhé.' },
  { t: 'Hướng dẫn đổi trả', b: 'Bạn vào Tài khoản › Đơn hàng › {ma_don} › "Đổi trả / Hoàn tiền", chọn sản phẩm, lý do và đính kèm ảnh. Shop duyệt trong 24h, chịu phí ship 2 chiều nếu lỗi NSX.' },
  { t: 'Pin LiPo an toàn', b: 'Pin LiPo nên sạc bằng sạc cân bằng, không sạc qua đêm, cất 3.8V/cell trong túi chống cháy và ngưng dùng khi pin phồng.' },
  { t: 'Kết thúc', b: 'Cảm ơn {ten} đã liên hệ ARES Toys. Chúc bạn một ngày vui vẻ!' }]
const load = () => { try { return JSON.parse(localStorage.getItem('cannedReplies')) || DEF } catch { return DEF } }
const list = ref(load()), open = ref(false), q = ref(''), inp = ref()
const save = () => localStorage.setItem('cannedReplies', JSON.stringify(list.value))
const fill = b => b.replace(/\{ten\}/g, props.name || 'bạn').replace(/\{ma_don\}/g, props.order || '(mã đơn)')
const shown = computed(() => list.value.filter(c => !q.value || (c.t + ' ' + c.b).toLowerCase().includes(q.value.toLowerCase())))
const use = c => { emit('insert', fill(c.b)); open.value = false; q.value = '' }
const del = c => { list.value = list.value.filter(x => x !== c); save() }
const saveCurrent = () => { const t = prompt('Tên mẫu?', props.current.slice(0, 24)); if (!t) return; list.value.push({ t, b: props.current.trim() }); save() }
watch(open, async v => { if (v) { await nextTick(); inp.value?.focus() } })
</script>
<style scoped>
.cr { position: relative; }
.cr-pop { position: absolute; bottom: calc(100% + 6px); left: 0; z-index: 40; width: min(380px, 86vw); background: var(--surface); border: 1px solid var(--border); box-shadow: 0 20px 40px -10px rgba(0,0,0,.3); padding: 10px; display: flex; flex-direction: column; gap: 8px; }
ul { list-style: none; max-height: 260px; overflow-y: auto; }
li { display: flex; align-items: stretch; border-bottom: 1px solid var(--border); }
.use { flex: 1; text-align: left; background: none; border: none; padding: 8px 6px; cursor: pointer; color: var(--text); font-family: inherit; min-width: 0; }
.use:hover { background: var(--bronze-soft); } .use b { display: block; font-size: .85rem; } .use small { display: block; color: var(--muted); font-size: .76rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.del { background: none; border: none; color: var(--muted); cursor: pointer; width: 28px; } .del:hover { color: var(--danger); }
.lnk { background: none; border: none; color: var(--accent); font: 600 .8rem inherit; font-family: inherit; cursor: pointer; padding: 0; } .lnk:disabled { color: var(--muted); cursor: not-allowed; }
</style>
