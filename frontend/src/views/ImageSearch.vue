<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: $t('Tìm bằng hình ảnh') }]" />
    <h1>{{ $t('Tìm bằng hình ảnh') }}</h1>
    <p class="muted">{{ $t('Chụp hoặc tải ảnh một chiếc xe — hệ thống so khớp hình dạng và màu sắc để gợi ý mẫu tương tự.') }}</p>
    <label class="drop card" :class="{ busy }" @dragover.prevent @drop.prevent="onDrop">
      <input type="file" accept="image/*" capture="environment" class="sr-only" @change="onPick" />
      <img v-if="preview" :src="preview" alt="" class="pv" />
      <span v-else class="hint">📷 {{ $t('Chọn ảnh, chụp ảnh hoặc kéo thả vào đây') }}</span>
      <span class="btn btn-sm">{{ preview ? $t('Đổi ảnh') : $t('Chọn ảnh') }}</span>
    </label>
    <p v-if="err" class="err">{{ err }}</p>
    <div v-if="busy" class="grid grid-4" style="margin-top:18px"><div v-for="i in 4" :key="i" class="sk" style="height:260px" /></div>
    <template v-else-if="res">
      <p class="small muted" style="margin-top:18px">{{ $t('{0} mẫu tương tự', res.results.length) }}<span v-if="res.keywords"> · AI: {{ res.keywords }}</span></p>
      <div class="grid grid-4"><div v-for="p in res.results" :key="p.id" class="rc"><ProductCard :p="p" /><span class="score">{{ $t('Độ giống') }} {{ Math.round(p.score * 100) }}%</span></div></div>
    </template>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import Breadcrumb from '../components/Breadcrumb.vue'
import ProductCard from '../components/ProductCard.vue'
import api, { errMsg } from '../api'
import { t } from '../i18n'
const preview = ref(''), res = ref(null), busy = ref(false), err = ref('')
const onPick = e => run(e.target.files?.[0])
const onDrop = e => run(e.dataTransfer.files?.[0])
async function run(file) {
  if (!file) return; err.value = ''
  if (!file.type.startsWith('image/')) { err.value = t('Vui lòng chọn tệp ảnh'); return }
  if (file.size > 6 * 1024 * 1024) { err.value = t('Ảnh tối đa 6MB'); return }
  preview.value = URL.createObjectURL(file); busy.value = true; res.value = null
  try { const fd = new FormData(); fd.append('file', await shrink(file)); res.value = (await api.post('/public/image-search', fd)).data } catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
// Thu nhỏ ảnh trên máy trước khi gửi (nhanh hơn trên 4G); HEIC/khác → gửi nguyên bản
async function shrink(file) {
  try {
    const bmp = await createImageBitmap(file); const k = Math.min(1, 640 / Math.max(bmp.width, bmp.height))
    const c = document.createElement('canvas'); c.width = Math.round(bmp.width * k); c.height = Math.round(bmp.height * k); c.getContext('2d').drawImage(bmp, 0, 0, c.width, c.height)
    const b = await new Promise(r => c.toBlob(r, 'image/jpeg', 0.85)); return b ? new File([b], 'q.jpg', { type: 'image/jpeg' }) : file
  } catch { return file }
}
</script>
<style scoped>
.drop { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; min-height: 200px; margin-top: 14px; border: 2px dashed var(--line, #ddd); cursor: pointer; text-align: center }
.drop.busy { opacity: .7 }
.pv { max-height: 220px; max-width: 100%; object-fit: contain; border-radius: 8px }
.hint { color: var(--muted, #666) }
.sr-only { position: absolute; width: 1px; height: 1px; opacity: 0; overflow: hidden }
.rc { display: flex; flex-direction: column; gap: 4px }
.score { font-size: 12px; color: var(--muted, #666) }
.err { color: #c62828; margin-top: 10px }
</style>
