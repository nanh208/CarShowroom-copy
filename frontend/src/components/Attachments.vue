<template>
  <div class="att">
    <div v-if="modelValue.length" class="thumbs">
      <figure v-for="(u, i) in modelValue" :key="u"><img :src="u" alt="Ảnh đính kèm" /><button type="button" :aria-label="`Bỏ ảnh ${i + 1}`" @click="remove(i)">✕</button></figure>
    </div>
    <label v-if="modelValue.length < max" class="pick" :class="{ busy }">
      <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" multiple :disabled="busy" @change="pick" />
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m21.4 11.1-8.5 8.5a5.5 5.5 0 0 1-7.8-7.8l8.5-8.5a3.7 3.7 0 0 1 5.2 5.2l-8.5 8.5a1.8 1.8 0 0 1-2.6-2.6l7.8-7.8"/></svg>
      {{ busy ? 'Đang tải…' : `Đính kèm ảnh (${modelValue.length}/${max})` }}
    </label>
    <small v-if="err" class="err">{{ err }}</small>
  </div>
</template>
<script setup>
// Đính kèm ảnh (tối đa 5, ≤5MB, JPG/PNG/WEBP/GIF) — tải lên /api/uploads, v-model là danh sách URL
import { ref } from 'vue'
import { uploadImage, errMsg } from '../api'
const props = defineProps({ modelValue: { type: Array, default: () => [] }, max: { type: Number, default: 5 } })
const emit = defineEmits(['update:modelValue'])
const busy = ref(false), err = ref('')
async function pick(e) {
  err.value = ''; const files = [...e.target.files].slice(0, props.max - props.modelValue.length); e.target.value = ''
  busy.value = true
  try { const urls = []; for (const f of files) { if (f.size > 5 * 1024 * 1024) { err.value = `“${f.name}” lớn hơn 5MB`; continue } urls.push(await uploadImage(f)) } emit('update:modelValue', [...props.modelValue, ...urls]) }
  catch (x) { err.value = errMsg(x) } finally { busy.value = false }
}
const remove = i => emit('update:modelValue', props.modelValue.filter((_, k) => k !== i))
</script>
<style scoped>
.att { display: flex; flex-direction: column; gap: 8px; }
.thumbs { display: flex; flex-wrap: wrap; gap: 8px; }
figure { position: relative; width: 72px; height: 72px; border: 1px solid var(--border); }
figure img { width: 100%; height: 100%; object-fit: cover; }
figure button { position: absolute; top: -8px; right: -8px; width: 22px; height: 22px; border-radius: 50%; border: none; background: #111; color: #fff; font-size: .7rem; cursor: pointer; }
.pick { display: inline-flex; align-items: center; gap: 6px; align-self: flex-start; padding: 8px 12px; border: 1.5px dashed var(--border); cursor: pointer; font-size: .85rem; font-weight: 600; margin: 0; }
.pick:hover { border-color: var(--accent); } .pick input { position: absolute; width: 1px; height: 1px; opacity: 0; } .pick:focus-within { outline: 2px solid var(--accent); }
.busy { opacity: .6; cursor: progress; } .err { color: var(--danger); }
</style>
