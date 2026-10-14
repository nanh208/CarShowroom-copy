<template>
  <div class="rw" role="region" aria-label="Yêu cầu đổi trả">
    <ol class="rw-steps"><li v-for="(s, i) in STEPS" :key="s" :class="{ on: i <= step, cur: i === step }" :aria-current="i === step ? 'step' : null"><b>{{ i + 1 }}</b><span>{{ s }}</span></li></ol>

    <div v-if="step === 0" class="rw-body">
      <p class="hint">Chọn sản phẩm cần đổi/trả:</p>
      <label v-for="it in order.items" :key="it.id" :class="['ri', { on: sel[it.id] }]">
        <input v-model="sel[it.id]" type="checkbox" />
        <img :src="it.image" alt="" /><span class="grow"><b>{{ it.productName }}</b><small v-if="it.variantName">{{ it.variantName }}</small></span>
        <select v-if="it.quantity > 1 && sel[it.id]" v-model.number="qty[it.id]" class="input q" :aria-label="`Số lượng ${it.productName}`"><option v-for="n in it.quantity" :key="n" :value="n">{{ n }}</option></select>
      </label>
    </div>

    <div v-else-if="step === 1" class="rw-body">
      <p class="hint">Lý do đổi/trả:</p>
      <div class="reasons"><label v-for="r in REASONS" :key="r" :class="['rr', { on: reason === r }]"><input v-model="reason" type="radio" :value="r" />{{ r }}</label></div>
      <textarea v-model="detail" class="input" rows="3" maxlength="800" placeholder="Mô tả chi tiết (tối thiểu 10 ký tự): lỗi xuất hiện thế nào, khi nào…" />
    </div>

    <div v-else-if="step === 2" class="rw-body">
      <p class="hint">Bạn muốn shop xử lý thế nào?</p>
      <div class="kinds">
        <label :class="['kd', { on: kind === 'RETURN' }]"><input v-model="kind" type="radio" value="RETURN" /><b>Đổi sản phẩm mới</b><small>Shop gửi sản phẩm thay thế, miễn phí vận chuyển 2 chiều</small></label>
        <label :class="['kd', { on: kind === 'REFUND' }]"><input v-model="kind" type="radio" value="REFUND" /><b>Trả hàng, hoàn tiền</b><small>Hoàn về phương thức thanh toán ban đầu trong 3–5 ngày</small></label>
      </div>
      <p class="hint">Ảnh minh chứng <span class="muted">(khuyến nghị — giúp duyệt nhanh hơn)</span></p>
      <Attachments v-model="images" :max="4" />
    </div>

    <div v-else class="rw-body">
      <p class="hint">Kiểm tra lại yêu cầu:</p>
      <dl class="sum">
        <dt>Sản phẩm</dt><dd><div v-for="l in lines" :key="l">{{ l }}</div></dd>
        <dt>Lý do</dt><dd>{{ reason }} — {{ detail }}</dd>
        <dt>Phương án</dt><dd>{{ kind === 'REFUND' ? 'Trả hàng, hoàn tiền' : 'Đổi sản phẩm mới' }}</dd>
        <dt>Ảnh</dt><dd>{{ images.length ? images.length + ' ảnh' : 'Không có' }}</dd>
      </dl>
    </div>

    <p v-if="err" class="err" role="alert">{{ err }}</p>
    <div class="rw-nav">
      <button class="btn btn-sm btn-outline" type="button" @click="step ? step-- : $emit('cancel')">{{ step ? '← Quay lại' : 'Huỷ' }}</button>
      <button v-if="step < 3" class="btn btn-sm" type="button" @click="next">Tiếp tục →</button>
      <button v-else class="btn btn-sm" type="button" :disabled="busy" @click="submit">{{ busy ? 'Đang gửi…' : 'Gửi yêu cầu' }}</button>
    </div>
  </div>
</template>
<script setup>
// Trình hướng dẫn đổi trả 4 bước: sản phẩm → lý do → phương án & ảnh → xác nhận
import { ref, reactive, computed } from 'vue'
import api, { errMsg } from '../api'
import Attachments from './Attachments.vue'
const props = defineProps({ order: { type: Object, required: true } })
const emit = defineEmits(['done', 'cancel'])
const STEPS = ['Sản phẩm', 'Lý do', 'Phương án', 'Xác nhận']
const REASONS = ['Sản phẩm lỗi / hỏng', 'Giao sai mẫu / màu', 'Thiếu phụ kiện', 'Không đúng mô tả', 'Đổi ý (còn nguyên seal)']
const step = ref(0), reason = ref(''), detail = ref(''), kind = ref('RETURN'), images = ref([]), err = ref(''), busy = ref(false)
const sel = reactive({}), qty = reactive({})
props.order.items.forEach(i => { qty[i.id] = i.quantity; if (props.order.items.length === 1) sel[i.id] = true })
const lines = computed(() => props.order.items.filter(i => sel[i.id]).map(i => `${i.productName}${i.variantName ? ' (' + i.variantName + ')' : ''} × ${qty[i.id]}`))
function next() {
  err.value = ''
  if (step.value === 0 && !lines.value.length) return (err.value = 'Chọn ít nhất một sản phẩm')
  if (step.value === 1 && (!reason.value || detail.value.trim().length < 10)) return (err.value = 'Chọn lý do và mô tả tối thiểu 10 ký tự')
  step.value++
}
async function submit() {
  busy.value = true; err.value = ''
  try {
    await api.post(`/orders/${props.order.code}/returns`, { kind: kind.value, images: images.value,
      reason: `[${reason.value}] ${detail.value.trim()}\nSản phẩm: ${lines.value.join('; ')}` })
    emit('done')
  } catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
</script>
<style scoped>
.rw { margin-top: 12px; padding: 16px; border: 1.5px solid var(--border); background: var(--surface); }
.rw-steps { list-style: none; display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; margin-bottom: 16px; counter-reset: s; }
.rw-steps li { display: flex; align-items: center; gap: 6px; padding-bottom: 8px; border-bottom: 3px solid var(--border); color: var(--muted); font-size: .8rem; font-weight: 600; min-width: 0; }
.rw-steps li b { flex-shrink: 0; width: 22px; height: 22px; display: grid; place-items: center; background: var(--tile); font-size: .72rem; }
.rw-steps li span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rw-steps li.on { border-color: var(--accent); color: var(--text); } .rw-steps li.on b { background: var(--accent); color: #fff; }
.hint { font-weight: 600; margin: 4px 0 8px; }
.rw-body { display: flex; flex-direction: column; gap: 8px; }
.ri, .rr, .kd { display: flex; align-items: center; gap: 10px; padding: 10px; border: 1.5px solid var(--border); cursor: pointer; margin: 0; font-weight: 500; }
.ri.on, .rr.on, .kd.on { border-color: var(--accent); background: var(--bronze-soft); }
.ri img { width: 56px; height: 40px; object-fit: cover; } .ri small { display: block; color: var(--muted); } .ri .q { width: 70px; padding: 4px 6px; }
.reasons { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.kinds { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; } .kd { flex-direction: column; align-items: flex-start; gap: 2px; } .kd input { display: none; } .kd small { color: var(--muted); font-weight: 400; }
.sum { display: grid; grid-template-columns: 100px 1fr; gap: 6px 12px; font-size: .9rem; } .sum dt { color: var(--muted); }
.err { color: var(--danger); margin-top: 8px; }
.rw-nav { display: flex; justify-content: space-between; margin-top: 14px; }
@media (max-width: 600px) { .reasons, .kinds { grid-template-columns: 1fr; } .rw-steps span { display: none; } .rw-steps li { justify-content: center; } }
</style>
