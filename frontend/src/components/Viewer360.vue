<template>
  <div class="modal-bg" role="dialog" aria-modal="true" aria-label="Xem 360° và video" @click.self="$emit('close')" @keydown.esc="$emit('close')" @keydown.left="step(-1)" @keydown.right="step(1)">
    <div class="modal v3">
      <div class="row between"><div class="tabs" role="tablist"><button role="tab" :aria-selected="tab === '360'" :class="{ on: tab === '360' }" @click="tab = '360'">360°</button><button role="tab" :aria-selected="tab === 'video'" :class="{ on: tab === 'video' }" @click="tab = 'video'">Video</button></div>
        <button ref="xb" class="x" aria-label="Đóng" @click="$emit('close')">×</button></div>
      <template v-if="tab === '360'">
        <div class="stage"><img :src="frames[i]" :alt="`${name} – góc ${i + 1}/${frames.length}`" /><span class="deg">{{ Math.round(i * 360 / frames.length) }}°</span></div>
        <div class="ctl"><button class="btn btn-sm btn-outline" aria-label="Xoay trái" @click="step(-1)">⟲</button>
          <input v-model.number="i" type="range" min="0" :max="frames.length - 1" step="1" aria-label="Góc xoay" />
          <button class="btn btn-sm btn-outline" aria-label="Xoay phải" @click="step(1)">⟳</button>
          <button class="btn btn-sm" :aria-pressed="auto" @click="auto = !auto">{{ auto ? 'Dừng' : 'Tự xoay' }}</button></div>
        <p class="small muted">Bản xem trước 360° ghép từ ảnh thật của sản phẩm — dùng thanh trượt hoặc phím ← → để xoay.</p>
      </template>
      <template v-else>
        <video v-if="video" :src="video" controls preload="metadata" class="vid" />
        <div v-else class="ph"><img :src="frames[0]" alt="" /><div><b>▶ Video sản phẩm đang được cập nhật</b><span>Xem video mở hộp & chạy thử trên kênh ARES Toys — hoặc nhắn shop để nhận video thực tế.</span></div></div>
      </template>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
const props = defineProps({ images: Array, name: String, video: String }); defineEmits(['close'])
const tab = ref('360'), i = ref(0), auto = ref(false), xb = ref()
// đủ khung hình cho cảm giác xoay: lặp ảnh thật theo vòng
const frames = computed(() => { const l = props.images?.length ? props.images : []; return l.length >= 8 ? l : Array.from({ length: Math.max(8, l.length) }, (_, k) => l[k % l.length]) })
const step = d => { i.value = (i.value + d + frames.value.length) % frames.value.length }
let t; const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches
watch(auto, v => { clearInterval(t); if (v) t = setInterval(() => step(1), reduce ? 1200 : 450) })
onMounted(() => xb.value?.focus()); onUnmounted(() => clearInterval(t))
</script>
<style scoped>
.v3 { max-width: 760px; display: flex; flex-direction: column; gap: 12px; background: var(--surface); color: var(--text); }
.tabs { display: flex; gap: 4px; } .tabs button { border: 1px solid var(--border); background: none; padding: 6px 14px; font: 600 .85rem inherit; font-family: inherit; cursor: pointer; color: var(--text); }
.tabs button.on { background: #111; color: #fff; border-color: #111; }
.x { background: none; border: none; font-size: 1.6rem; cursor: pointer; color: var(--text); width: 36px; height: 36px; }
.stage { position: relative; background: var(--bg-2); aspect-ratio: 16/10; display: grid; place-items: center; overflow: hidden; }
.stage img { width: 100%; height: 100%; object-fit: contain; }
.deg { position: absolute; right: 10px; top: 10px; background: #111; color: #fff; font: 600 .75rem 'Oswald', sans-serif; padding: 3px 8px; }
.ctl { display: flex; gap: 8px; align-items: center; } .ctl input { flex: 1; min-width: 0; accent-color: var(--accent); }
.vid { width: 100%; background: #000; aspect-ratio: 16/9; }
.ph { position: relative; aspect-ratio: 16/9; background: #111; overflow: hidden; } .ph img { width: 100%; height: 100%; object-fit: cover; opacity: .35; }
.ph div { position: absolute; inset: 0; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; gap: 8px; color: #fff; padding: 20px; }
.ph b { font: 600 1.2rem 'Oswald', sans-serif; } .ph span { font-size: .85rem; opacity: .8; max-width: 420px; }
@media (max-width: 520px) { .ctl { flex-wrap: wrap; } .ctl input { flex-basis: 100%; order: -1; } }
</style>
