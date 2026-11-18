<template>
  <div v-if="modelValue" class="media-modal-backdrop" @click.self="close">
    <div class="media-modal">
      <div class="media-modal-header">
        <div class="header-left">
          <h3>{{ title || 'Thư viện hình ảnh' }}</h3>
          <span class="file-count">({{ filteredFiles.length }} tệp)</span>
        </div>
        <button type="button" class="btn-close" @click="close" title="Đóng">×</button>
      </div>

      <div class="media-modal-toolbar">
        <div class="search-box">
          <input
            v-model.trim="searchTerm"
            type="text"
            class="input input-sm"
            placeholder="Tìm theo tên file (VD: lamborghini, r34, ferrari...)"
          />
          <button v-if="searchTerm" class="btn-clear-search" @click="searchTerm = ''">×</button>
        </div>

        <div class="toolbar-actions">
          <label class="btn btn-sm btn-primary upload-btn" :class="{ disabled: uploading }">
            <input
              type="file"
              accept="image/*"
              multiple
              style="display: none"
              :disabled="uploading"
              @change="handleFileUpload"
            />
            <span>{{ uploading ? 'Đang tải lên (' + uploadProgress + ')...' : '+ Tải ảnh từ máy tính' }}</span>
          </label>
          <button type="button" class="btn btn-sm btn-outline" :disabled="loading" @click="fetchFiles">
            Làm mới
          </button>
        </div>
      </div>

      <!-- Khu vực kéo thả file -->
      <div
        class="dropzone"
        :class="{ active: isDragging }"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
      >
        <div class="dropzone-hint">
          <span>Kéo và thả file ảnh vào đây, hoặc bấm nút "+ Tải ảnh từ máy tính" ở trên</span>
          <small class="muted">Hỗ trợ JPG, PNG, WEBP, GIF tối đa 10MB mỗi file</small>
        </div>
      </div>

      <!-- Danh sách ảnh -->
      <div class="media-modal-body">
        <div v-if="loading" class="media-loading">
          <div class="spinner" />
          <span>Đang tải danh sách ảnh từ máy chủ...</span>
        </div>

        <div v-else-if="filteredFiles.length === 0" class="media-empty">
          <p v-if="searchTerm">Không tìm thấy ảnh nào khớp với từ khóa "{{ searchTerm }}".</p>
          <p v-else>Thư viện hiện chưa có ảnh nào được tải lên. Hãy bấm nút "+ Tải ảnh từ máy tính" để bắt đầu.</p>
        </div>

        <div v-else class="media-grid">
          <div
            v-for="item in filteredFiles"
            :key="item.name"
            class="media-card"
            :class="{ selected: isSelected(item.url) }"
            @click="toggleSelect(item)"
          >
            <div class="thumb-wrap">
              <img :src="item.url" :alt="item.name" loading="lazy" />
              <div class="card-badges">
                <span v-if="isSelected(item.url)" class="check-badge">✓</span>
              </div>
              <div class="card-overlay" @click.stop>
                <button type="button" class="action-btn" title="Xem trước kích thước lớn" @click="previewUrl = item.url">
                  Xem
                </button>
                <button type="button" class="action-btn" title="Sao chép liên kết" @click="copyLink(item.url)">
                  Link
                </button>
                <button type="button" class="action-btn danger" title="Xoá ảnh này khỏi máy chủ" @click="deleteItem(item)">
                  Xoá
                </button>
              </div>
            </div>
            <div class="file-meta">
              <span class="file-name" :title="item.name">{{ item.name }}</span>
              <span class="file-size">{{ formatSize(item.size) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer Modal -->
      <div class="media-modal-footer">
        <div class="selected-summary">
          <span v-if="selectedUrls.length">
            Đã chọn: <b>{{ selectedUrls.length }}</b> ảnh
          </span>
          <span v-else class="muted">Chưa chọn ảnh nào. Bấm vào ảnh để chọn.</span>
        </div>
        <div class="footer-actions">
          <button type="button" class="btn btn-outline" @click="close">Hủy</button>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="selectedUrls.length === 0"
            @click="confirmSelection"
          >
            Xác nhận chọn {{ selectedUrls.length > 0 ? '(' + selectedUrls.length + ')' : '' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Xem trước ảnh lớn (Lightbox) -->
    <div v-if="previewUrl" class="lightbox-backdrop" @click.self="previewUrl = null">
      <div class="lightbox-content">
        <img :src="previewUrl" alt="Preview" />
        <button type="button" class="lightbox-close" @click="previewUrl = null">×</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  multiple: { type: Boolean, default: false },
  title: { type: String, default: 'Thư viện hình ảnh' }
})

const emit = defineEmits(['update:modelValue', 'select', 'close'])

const files = ref([])
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref('')
const searchTerm = ref('')
const isDragging = ref(false)
const selectedUrls = ref([])
const previewUrl = ref(null)

const filteredFiles = computed(() => {
  if (!searchTerm.value) return files.value
  const kw = searchTerm.value.toLowerCase()
  return files.value.filter(f => f.name.toLowerCase().includes(kw))
})

watch(() => props.modelValue, (val) => {
  if (val) {
    selectedUrls.value = []
    searchTerm.value = ''
    fetchFiles()
  }
})

async function fetchFiles() {
  loading.value = true
  try {
    const { data } = await api.get('/uploads')
    files.value = Array.isArray(data) ? data : []
  } catch (e) {
    toast('Không tải được danh sách ảnh: ' + errMsg(e), 'error')
  } finally {
    loading.value = false
  }
}

function isSelected(url) {
  return selectedUrls.value.includes(url)
}

function toggleSelect(item) {
  if (!props.multiple) {
    if (selectedUrls.value.includes(item.url)) {
      selectedUrls.value = []
    } else {
      selectedUrls.value = [item.url]
    }
  } else {
    const idx = selectedUrls.value.indexOf(item.url)
    if (idx >= 0) {
      selectedUrls.value.splice(idx, 1)
    } else {
      selectedUrls.value.push(item.url)
    }
  }
}

function confirmSelection() {
  if (selectedUrls.value.length === 0) return
  if (props.multiple) {
    emit('select', [...selectedUrls.value])
  } else {
    emit('select', selectedUrls.value[0])
  }
  close()
}

function close() {
  emit('update:modelValue', false)
  emit('close')
}

function copyLink(url) {
  const full = url.startsWith('http') ? url : (window.location.origin + url)
  if (navigator.clipboard) {
    navigator.clipboard.writeText(full)
  }
  toast('Đã sao chép liên kết ảnh vào bộ nhớ tạm')
}

async function deleteItem(item) {
  if (!confirm(`Bạn có chắc muốn xoá vĩnh viễn ảnh "${item.name}" khỏi máy chủ?`)) return
  try {
    await api.delete(`/uploads/${encodeURIComponent(item.name)}`)
    files.value = files.value.filter(f => f.name !== item.name)
    selectedUrls.value = selectedUrls.value.filter(u => u !== item.url)
    toast('Đã xoá ảnh thành công')
  } catch (e) {
    toast('Không thể xoá ảnh: ' + errMsg(e), 'error')
  }
}

async function handleFileUpload(event) {
  const fileList = event.target?.files
  if (!fileList || fileList.length === 0) return
  await uploadFiles(Array.from(fileList))
  event.target.value = ''
}

async function handleDrop(event) {
  isDragging.value = false
  const dropped = event.dataTransfer?.files
  if (!dropped || dropped.length === 0) return
  await uploadFiles(Array.from(dropped))
}

async function uploadFiles(fileArray) {
  const validFiles = fileArray.filter(f => f.type.startsWith('image/'))
  if (validFiles.length === 0) {
    toast('Vui lòng chỉ chọn các tệp hình ảnh (JPG, PNG, WEBP, GIF)', 'warn')
    return
  }

  uploading.value = true
  uploadProgress.value = `0/${validFiles.length}`

  try {
    if (validFiles.length === 1) {
      const fd = new FormData()
      fd.append('file', validFiles[0])
      const { data } = await api.post('/uploads', fd)
      toast('Tải ảnh thành công')
      await fetchFiles()
      if (data?.url) {
        if (!props.multiple) selectedUrls.value = [data.url]
        else selectedUrls.value.push(data.url)
      }
    } else {
      const fd = new FormData()
      validFiles.forEach(f => fd.append('files', f))
      const { data } = await api.post('/uploads/batch', fd)
      const count = Array.isArray(data) ? data.length : 0
      toast(`Đã tải lên thành công ${count} ảnh`)
      await fetchFiles()
      if (Array.isArray(data)) {
        const urls = data.map(d => d.url).filter(Boolean)
        if (!props.multiple && urls.length > 0) {
          selectedUrls.value = [urls[0]]
        } else if (props.multiple) {
          urls.forEach(u => {
            if (!selectedUrls.value.includes(u)) selectedUrls.value.push(u)
          })
        }
      }
    }
  } catch (e) {
    toast('Lỗi khi tải ảnh: ' + errMsg(e), 'error')
  } finally {
    uploading.value = false
    uploadProgress.value = ''
  }
}

function formatSize(bytes) {
  if (!bytes || bytes <= 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}
</script>

<style scoped>
.media-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.media-modal {
  background: var(--surface, #1e1e24);
  border: 1px solid var(--border, #333);
  border-radius: 12px;
  width: 100%;
  max-width: 980px;
  height: 85vh;
  max-height: 760px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.45);
  overflow: hidden;
}

.media-modal-header {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border, #333);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.header-left h3 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text, #fff);
}

.file-count {
  font-size: 0.85rem;
  color: var(--muted, #888);
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 1.6rem;
  line-height: 1;
  color: var(--muted, #888);
  cursor: pointer;
  padding: 0 4px;
  transition: color 0.15s;
}

.btn-close:hover {
  color: var(--text, #fff);
}

.media-modal-toolbar {
  padding: 12px 20px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border, #2a2a30);
  background: var(--bg-2, #18181c);
  flex-wrap: wrap;
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 240px;
}

.btn-clear-search {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  color: var(--muted, #888);
  cursor: pointer;
  font-size: 1.1rem;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.upload-btn {
  cursor: pointer;
}

.upload-btn.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dropzone {
  margin: 8px 20px;
  padding: 10px 16px;
  border: 1.5px dashed var(--border, #444);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
  text-align: center;
  transition: all 0.2s;
}

.dropzone.active {
  border-color: var(--accent, #d4a373);
  background: rgba(212, 163, 115, 0.08);
}

.dropzone-hint {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 0.82rem;
  color: var(--text, #ddd);
}

.media-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
}

.media-loading,
.media-empty {
  height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--muted, #888);
  text-align: center;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: var(--accent, #d4a373);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(136px, 1fr));
  gap: 12px;
}

.media-card {
  border: 1.5px solid var(--border, #333);
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface, #222);
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
  display: flex;
  flex-direction: column;
}

.media-card:hover {
  border-color: var(--accent, #d4a373);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.media-card.selected {
  border-color: var(--accent, #d4a373);
  box-shadow: 0 0 0 2px var(--accent, #d4a373);
}

.thumb-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #111;
  overflow: hidden;
}

.thumb-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-badges {
  position: absolute;
  top: 6px;
  left: 6px;
}

.check-badge {
  background: var(--accent, #d4a373);
  color: #111;
  font-weight: 800;
  font-size: 0.75rem;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}

.media-card:hover .card-overlay {
  opacity: 1;
}

.action-btn {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  padding: 3px 6px;
  font-size: 0.72rem;
  cursor: pointer;
  transition: background 0.15s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.35);
}

.action-btn.danger:hover {
  background: #dc2626;
  border-color: #dc2626;
}

.file-meta {
  padding: 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--surface, #1e1e24);
}

.file-name {
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text, #eee);
}

.file-size {
  font-size: 0.7rem;
  color: var(--muted, #888);
}

.media-modal-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--border, #333);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-2, #18181c);
}

.selected-summary {
  font-size: 0.85rem;
}

.footer-actions {
  display: flex;
  gap: 8px;
}

/* Lightbox preview */
.lightbox-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.lightbox-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.lightbox-content img {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.6);
}

.lightbox-close {
  position: absolute;
  top: -14px;
  right: -14px;
  background: #111;
  color: #fff;
  border: 2px solid #fff;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
</style>