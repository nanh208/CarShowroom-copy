<template>
  <div>
    <div class="row between"><h1>Tin tức, blog & video review</h1><button class="btn" @click="edit()">+ Bài mới</button></div>
    <div class="row" style="gap:8px;margin:8px 0"><input v-model="q" class="input" style="max-width:260px" placeholder="Lọc theo tiêu đề / tag" /><select v-model="fk" class="input" style="max-width:160px"><option value="">Mọi loại</option><option>NEWS</option><option>BLOG</option><option>VIDEO</option></select></div>
    <div class="table-wrap card"><table class="table">
      <thead><tr><th>Tiêu đề</th><th>Loại</th><th>Trạng thái</th><th>Xuất bản</th><th>Lượt xem</th><th></th></tr></thead>
      <tbody>
        <tr v-for="p in shown" :key="p.id"><td><b>{{ p.title }}</b><div class="small muted">/blog/{{ p.slug }} · {{ (p.tags || []).map(t => '#' + t).join(' ') }}</div></td><td>{{ p.kind }}</td>
          <td><span :class="['badge', p.status === 'PUBLISHED' ? (new Date(p.publishedAt) > new Date() ? 'badge-warn' : 'badge-ok') : '']">{{ p.status === 'PUBLISHED' ? (new Date(p.publishedAt) > new Date() ? 'Hẹn giờ' : 'Đã đăng') : 'Nháp' }}</span></td>
          <td class="small">{{ dt(p.publishedAt) }}</td><td>{{ p.views }}</td>
          <td class="nowrap"><button class="btn btn-sm btn-outline" @click="edit(p)">Sửa</button> <a v-if="p.status === 'PUBLISHED'" class="btn btn-sm btn-outline" :href="`/blog/${p.slug}`" target="_blank" rel="noopener">Xem</a> <button class="btn btn-sm btn-danger" @click="remove(p)">Xoá</button></td></tr>
        <tr v-if="!shown.length"><td colspan="6" class="muted">Chưa có bài.</td></tr>
      </tbody></table></div>

    <div v-if="f" class="modal-bg" @click.self="f = null">
      <div class="modal wide" role="dialog" aria-label="Soạn bài">
        <h3>{{ f.id ? 'Sửa bài' : 'Bài mới' }}</h3>
        <div v-if="err" class="alert alert-error">{{ err }}</div>
        <div class="form-grid">
          <div class="field"><label>Tiêu đề (VI) *</label><input v-model="f.title" class="input" /></div>
          <div class="field"><label>Title (EN)</label><input v-model="f.titleEn" class="input" /></div>
          <div class="field"><label>Slug (để trống = tự tạo)</label><input v-model="f.slug" class="input" /></div>
          <div class="field"><label>Loại</label><select v-model="f.kind" class="input"><option value="NEWS">Tin tức</option><option value="BLOG">Blog</option><option value="VIDEO">Video review</option></select></div>
          <div class="field"><label>Trạng thái</label><select v-model="f.status" class="input"><option value="DRAFT">Nháp</option><option value="PUBLISHED">Xuất bản</option></select></div>
          <div class="field"><label>Thời điểm đăng (hẹn giờ)</label><input v-model="f.publishedAt" type="datetime-local" class="input" /></div>
        </div>
        <div class="field"><label>Tóm tắt (VI)</label><textarea v-model="f.excerpt" class="input" rows="2" /></div>
        <div class="field"><label>Excerpt (EN)</label><textarea v-model="f.excerptEn" class="input" rows="2" /></div>
        <div v-if="f.kind === 'VIDEO'" class="field"><label>Link video (YouTube / Vimeo / .mp4)</label><input v-model="f.videoUrl" class="input" placeholder="https://www.youtube.com/watch?v=…" /></div>
        <div class="field"><label>Ảnh bìa</label><div class="row" style="gap:8px"><input v-model="f.coverImage" class="input" style="flex:1" /><label class="btn btn-sm btn-outline">Tải lên<input type="file" accept="image/*" hidden @change="upCover" /></label></div></div>
        <div class="tabs"><button :class="{ on: lang === 'vi' }" @click="lang = 'vi'">Nội dung VI</button><button :class="{ on: lang === 'en' }" @click="lang = 'en'">Content EN</button><button :class="{ on: raw }" @click="toggleRaw">&lt;/&gt; HTML</button></div>
        <div v-if="!raw" class="tb" role="toolbar" aria-label="Định dạng">
          <button v-for="b in TB" :key="b[0] + (b[1] || '')" type="button" :title="b[2]" @mousedown.prevent @click="cmd(b[0], b[1])">{{ b[3] }}</button>
          <button type="button" title="Liên kết" @mousedown.prevent @click="link">🔗</button>
          <label class="tbimg" title="Chèn ảnh">🖼<input type="file" accept="image/*" hidden @change="insImg" /></label>
        </div>
        <textarea v-if="raw" v-model="f[key]" class="input mono" rows="14" />
        <div v-else :key="lang" ref="ed" class="editor rich" contenteditable="true" @input="sync" v-html="f[key]" />
        <div class="form-grid">
          <div class="field"><label>Tags (phân tách bởi dấu phẩy)</label><input v-model="tagStr" class="input" placeholder="rc, review, drift" /></div>
          <div class="field"><label>Sản phẩm liên quan</label><select v-model="f.productIds" multiple class="input" size="4"><option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }}</option></select></div>
          <div class="field"><label>SEO title</label><input v-model="f.seoTitle" class="input" maxlength="70" /></div>
          <div class="field"><label>SEO description</label><input v-model="f.seoDescription" class="input" maxlength="170" /></div>
        </div>
        <div class="row" style="justify-content:flex-end;margin-top:16px;gap:8px"><button class="btn btn-outline" @click="f = null">Huỷ</button><button class="btn" :disabled="busy" @click="save">Lưu</button></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import api, { errMsg, uploadImage } from '../../api'
import { dt } from '../../format'
import { toast } from '../../stores/prefs'
const TB = [['bold', null, 'Đậm', 'B'], ['italic', null, 'Nghiêng', 'I'], ['formatBlock', 'h2', 'Tiêu đề 2', 'H2'], ['formatBlock', 'h3', 'Tiêu đề 3', 'H3'], ['formatBlock', 'p', 'Đoạn văn', '¶'],
  ['insertUnorderedList', null, 'Danh sách', '•'], ['insertOrderedList', null, 'Danh sách số', '1.'], ['formatBlock', 'blockquote', 'Trích dẫn', '❝'], ['removeFormat', null, 'Xoá định dạng', '⌫']]
const list = ref([]), products = ref([]), f = ref(null), err = ref(''), busy = ref(false), q = ref(''), fk = ref(''), lang = ref('vi'), raw = ref(false), ed = ref(null), tagStr = ref('')
const key = computed(() => lang.value === 'en' ? 'contentHtmlEn' : 'contentHtml')
const shown = computed(() => list.value.filter(p => (!fk.value || p.kind === fk.value) && (!q.value || (p.title + ' ' + (p.tags || []).join(' ')).toLowerCase().includes(q.value.toLowerCase()))))
const load = async () => (list.value = (await api.get('/admin/posts')).data)
const local = s => s ? s.slice(0, 16) : ''
async function edit(p) {
  err.value = ''; lang.value = 'vi'; raw.value = false
  const x = p ? (await api.get('/admin/posts/' + p.id)).data : { kind: 'BLOG', status: 'DRAFT', contentHtml: '<p></p>', tags: [], productIds: [] }
  x.publishedAt = local(x.publishedAt); x.productIds = x.productIds || []; tagStr.value = (x.tags || []).join(', '); f.value = x
}
const sync = () => { if (ed.value) f.value[key.value] = ed.value.innerHTML }
function cmd(c, v) { document.execCommand(c, false, v); sync() }
function link() { const u = prompt('URL (https://… hoặc /…)'); if (u && /^(https?:\/\/|\/)/.test(u)) cmd('createLink', u) }
async function insImg(e) { const file = e.target.files[0]; e.target.value = ''; if (!file) return; try { const url = await uploadImage(file); ed.value?.focus(); cmd('insertImage', url) } catch (x) { err.value = errMsg(x) } }
async function upCover(e) { const file = e.target.files[0]; e.target.value = ''; if (file) try { f.value.coverImage = await uploadImage(file) } catch (x) { err.value = errMsg(x) } }
async function toggleRaw() { sync(); raw.value = !raw.value; await nextTick() }
async function save() {
  sync(); busy.value = true; err.value = ''
  const body = { ...f.value, tags: tagStr.value.split(',').map(s => s.trim()).filter(Boolean), publishedAt: f.value.publishedAt || null }
  try { f.value.id ? await api.put('/admin/posts/' + f.value.id, body) : await api.post('/admin/posts', body); f.value = null; toast('Đã lưu'); load() } catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
async function remove(p) { if (!confirm('Xoá bài "' + p.title + '"?')) return; await api.delete('/admin/posts/' + p.id); toast('Đã xoá'); load() }
onMounted(async () => { load(); products.value = (await api.get('/admin/products', { params: { size: 500 } })).data.content })
</script>
<style scoped>
.wide { max-width: 920px; width: 100% }
.tabs { display: flex; gap: 4px; margin-top: 8px }
.tabs button { border: 1px solid var(--border); background: none; padding: 5px 12px; border-radius: 6px 6px 0 0; font-size: 13px }
.tabs button.on { background: var(--surface-2, #eee); font-weight: 600 }
.tb { display: flex; flex-wrap: wrap; gap: 2px; border: 1px solid var(--border); border-bottom: 0; padding: 4px; background: var(--surface-2, #f5f5f5) }
.tb button, .tbimg { min-width: 34px; height: 30px; border: 0; background: none; font-weight: 700; cursor: pointer; display: grid; place-items: center; border-radius: 4px }
.editor { min-height: 260px; max-height: 50vh; overflow: auto; border: 1px solid var(--border); padding: 12px; line-height: 1.7; outline: none }
.editor:focus { box-shadow: inset 0 0 0 2px var(--accent, #f15a24) }
.editor :deep(img) { max-width: 100% }
.mono { font-family: ui-monospace, monospace; font-size: 13px }
.nowrap { white-space: nowrap }
</style>
