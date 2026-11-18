<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h2>Quản lý hình ảnh & Media</h2>
        <p class="muted">Tải lên, quản lý, xem trước và tái sử dụng hình ảnh cho sản phẩm, bài viết và banner.</p>
      </div>

      <div class="head-actions">
        <label class="btn btn-primary" :class="{ disabled: uploading }">
          <input
            type="file"
            accept="image/*"
            multiple
            style="display: none"
            :disabled="uploading"
            @change="handleFileUpload"
          />
          <span>{{ uploading ? 'Đang tải (' + uploadProgress + ')...' : '+ Tải ảnh lên' }}</span>
        </label>
        <button type="button" class="btn btn-outline" :disabled="loading" @click="fetchFiles">
          Làm mới
        </button>
      </div>
    </div>

    <!-- Hộp thống kê nhanh -->
    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-lbl">Tổng số ảnh</span>
        <b class="stat-val">{{ files.length }}</b>
      </div>
      <div class="stat-card">
        <span class="stat-lbl">Dung lượng ước tính</span>
        <b class="stat-val">{{ formatSize(totalBytes) }}</b>
      </div>
      <div class="stat-card">
        <span class="stat-lbl">Đang hiển thị</span>
        <b class="stat-val">{{ filteredFiles.length }}</b>
      </div>
    </div>

    <!-- Thanh tìm kiếm & lọc -->
    <div class="card filter-bar">
      <div class="search-input-wrap">
        <input
          v-model.trim="searchTerm"
          type="text"
          class="input"
          placeholder="Tìm theo tên file..."
        />
        <button v-if="searchTerm" class="btn-clear" @click="searchTerm = ''">×</button>
      </div>

      <div class="filter-actions">
        <button
          v-if="selectedNames.length > 0"
          type="button"
          class="btn btn-sm btn-danger"
          @click="deleteSelected"
        >
          Xoá {{ selectedNames.length }} ảnh đã chọn
        </button>
        <button
          type="button"
          class="btn btn-sm btn-outline"
          @click="toggleSelectAll"
        >
          {{ selectedNames.length === filteredFiles.length && filteredFiles.length > 0 ? 'Bỏ chọn tất cả' : 'Chọn tất cả' }}
        </button>
      </div>
    </div>

    <!-- Drag & Drop Zone -->
    <div
      class="upload-dropzone"
      :class="{ dragging: isDragging }"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <p>Kéo và thả nhiều tệp ảnh vào đây để tải lên hàng loạt (JPG, PNG, WEBP, GIF tối đa 10MB/tệp)</p>
    </div>

    <!-- Grid danh sách ảnh -->
    <div v-if="loading" class="loading-state">
      <div class="spinner" />
      <span>Đang tải danh sách tệp...</span>
    </div>

    <div v-else-if="filteredFiles.length === 0" class="empty-state card">
      <p v-if="searchTerm">Không tìm thấy ảnh nào khớp với từ khóa "{{ searchTerm }}".</p>
      <p v-else>Thư mục tải lên hiện chưa có tệp ảnh nào.</p>
    </div>

    <div v-else class="media-container card">
      <div class="media-gallery">
        <div
          v-for="item in filteredFiles"
          :key="item.name"
          class="gallery-item"
          :class="{ selected: selectedNames.includes(item.name) }"
        >
          <div class="select-checkbox" @click.stop="toggleItemSelect(item.name)">
            <input type="checkbox" :checked="selectedNames.includes(item.name)" />
          </div>

          <div class="preview-box" @click="previewUrl = item.url">
            <img :src="item.url" :alt="item.name" loading="lazy" />
            <div class="hover-actions" @click.stop>
              <button type="button" class="btn btn-xs btn-outline" @click="previewUrl = item.url">Xem</button>
              <button type="button" class="btn btn-xs btn-outline" @click="copyLink(item.url)">Copy URL</button>
              <button type="button" class="btn btn-xs btn-danger" @click="deleteItem(item)">Xoá</button>
            </div>
          </div>

          <div class="meta-box">
            <span class="name" :title="item.name">{{ item.name }}</span>
            <div class="details">
              <span>{{ formatSize(item.size) }}</span>
              <span>{{ formatDate(item.updatedAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Lightbox Xem ảnh phóng to -->
    <div v-if="previewUrl" class="lightbox-modal" @click.self="previewUrl = null">
      <div class="lightbox-body">
        <img :src="previewUrl" alt="Large preview" />
        <div class="lightbox-bar">
          <a :href="previewUrl" target="_blank" class="btn btn-sm btn-outline">Mở tab mới</a>
          <button type="button" class="btn btn-sm btn-primary" @click="copyLink(previewUrl)">Sao chép URL</button>
          <button type="button" class="btn btn-sm btn-outline" @click="previewUrl = null">Đóng</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'

const files = ref([])
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref('')
const searchTerm = ref('')
const isDragging = ref(false)
const selectedNames = ref([])
const previewUrl = ref(null)

const filteredFiles = computed(() => {
  if (!searchTerm.value) return files.value
  const kw = searchTerm.value.toLowerCase()
  return files.value.filter(f => f.name.toLowerCase().includes(kw))
})

const totalBytes = computed(() => {
  return files.value.reduce((acc, f) => acc + (f.size || 0), 0)
})

onMounted(() => {
  fetchFiles()
})

async function fetchFiles() {
  loading.value = true
  try {
    const { data } = await api.get('/uploads')
    files.value = Array.isArray(data) ? data : []
  } catch (e) {
    toast('Lỗi khi tải danh sách ảnh: ' + errMsg(e), 'error')
  } finally {
    loading.value = false
  }
}

function toggleItemSelect(name) {
  const idx = selectedNames.value.indexOf(name)
  if (idx >= 0) selectedNames.value.splice(idx, 1)
  else selectedNames.value.push(name)
}

function toggleSelectAll() {
  if (selectedNames.value.length === filteredFiles.value.length && filteredFiles.value.length > 0) {
    selectedNames.value = []
  } else {
    selectedNames.value = filteredFiles.value.map(f => f.name)
  }
}

function copyLink(url) {
  const full = url.startsWith('http') ? url : (window.location.origin + url)
  if (navigator.clipboard) {
    navigator.clipboard.writeText(full)
  }
  toast('Đã sao chép liên kết ảnh vào bộ nhớ tạm')
}

async function deleteItem(item) {
  if (!confirm(`Bạn có chắc muốn xoá vĩnh viễn tệp "${item.name}"?`)) return
  try {
    await api.delete(`/uploads/${encodeURIComponent(item.name)}`)
    files.value = files.value.filter(f => f.name !== item.name)
    selectedNames.value = selectedNames.value.filter(n => n !== item.name)
    toast('Đã xoá ảnh thành công')
  } catch (e) {
    toast('Không thể xoá: ' + errMsg(e), 'error')
  }
}

async function deleteSelected() {
  const count = selectedNames.value.length
  if (!confirm(`Bạn có chắc muốn xoá ${count} ảnh đã chọn khỏi máy chủ?`)) return
  let deleted = 0
  for (const name of [...selectedNames.value]) {
    try {
      await api.delete(`/uploads/${encodeURIComponent(name)}`)
      files.value = files.value.filter(f => f.name !== name)
      deleted++
    } catch (e) {
      console.error(e)
    }
  }
  selectedNames.value = []
  toast(`Đã xoá thành công ${deleted}/${count} ảnh`)
}

async function handleFileUpload(event) {
  const fileList = event.target?.files
  if (!fileList || fileList.length === 0) return
  await uploadBatch(Array.from(fileList))
  event.target.value = ''
}

async function handleDrop(event) {
  isDragging.value = false
  const dropped = event.dataTransfer?.files
  if (!dropped || dropped.length === 0) return
  await uploadBatch(Array.from(dropped))
}

async function uploadBatch(fileArray) {
  const validFiles = fileArray.filter(f => f.type.startsWith('image/'))
  if (validFiles.length === 0) {
    toast('Vui lòng chỉ chọn tệp hình ảnh (JPG, PNG, WEBP, GIF)', 'warn')
    return
  }

  uploading.value = true
  uploadProgress.value = `0/${validFiles.length}`

  try {
    if (validFiles.length === 1) {
      const fd = new FormData()
      fd.append('file', validFiles[0])
      await api.post('/uploads', fd)
      toast('Tải ảnh thành công')
    } else {
      const fd = new FormData()
      validFiles.forEach(f => fd.append('files', f))
      const { data } = await api.post('/uploads/batch', fd)
      const c = Array.isArray(data) ? data.length : 0
      toast(`Đã tải lên thành công ${c} ảnh`)
    }
    await fetchFiles()
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

function formatDate(isoStr) {
  if (!isoStr) return ''
  try {
    const d = new Date(isoStr)
    return `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`
  } catch {
    return ''
  }
}
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-head h2 {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 700;
}

.head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-lbl {
  font-size: 0.8rem;
  color: var(--muted);
}

.stat-val {
  font-size: 1.3rem;
  color: var(--text);
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  flex-wrap: wrap;
}

.search-input-wrap {
  position: relative;
  flex: 1;
  min-width: 240px;
}

.btn-clear {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--muted);
  font-size: 1.2rem;
  cursor: pointer;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.upload-dropzone {
  border: 2px dashed var(--border);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  background: rgba(255, 255, 255, 0.015);
  transition: all 0.2s;
  color: var(--muted);
  font-size: 0.88rem;
}

.upload-dropzone.dragging {
  border-color: var(--accent);
  background: rgba(212, 163, 115, 0.06);
  color: var(--text);
}

.media-container {
  padding: 16px;
}

.media-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
}

.gallery-item {
  border: 1.5px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface);
  position: relative;
  display: flex;
  flex-direction: column;
  transition: all 0.15s ease;
}

.gallery-item:hover {
  border-color: var(--accent);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.gallery-item.selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent);
}

.select-checkbox {
  position: absolute;
  top: 6px;
  left: 6px;
  z-index: 2;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 4px;
  padding: 2px 4px;
  cursor: pointer;
}

.select-checkbox input {
  cursor: pointer;
}

.preview-box {
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #111;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.preview-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.hover-actions {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.15s;
}

.preview-box:hover .hover-actions {
  opacity: 1;
}

.meta-box {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  background: var(--bg-2);
}

.meta-box .name {
  font-size: 0.78rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text);
}

.meta-box .details {
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  color: var(--muted);
}

.loading-state,
.empty-state {
  padding: 48px;
  text-align: center;
  color: var(--muted);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-danger {
  background: #dc2626;
  color: #fff;
  border: none;
}

.btn-danger:hover {
  background: #b91c1c;
}

.lightbox-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.lightbox-body {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.lightbox-body img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.6);
}

.lightbox-bar {
  display: flex;
  gap: 10px;
}
</style>