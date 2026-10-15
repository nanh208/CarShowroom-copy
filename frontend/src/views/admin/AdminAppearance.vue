<template>
  <div>
    <div class="row between hd"><div><h1>Giao diện</h1><p class="muted small">Áp dụng cho <b>mọi khách truy cập</b>. Thay đổi được xem trước ngay trong khung bên phải; bấm “Lưu” để áp dụng toàn site.</p></div>
      <div class="row" style="gap:8px"><span v-if="dirty" class="badge badge-PENDING">Chưa lưu</span><button class="btn btn-outline" :disabled="!dirty || busy" @click="revert">Hoàn tác</button><button class="btn" :disabled="!dirty || busy" @click="save">{{ busy ? 'Đang lưu…' : 'Lưu & áp dụng' }}</button></div></div>
    <div v-if="!f" class="sk" style="height:320px" />
    <div v-else class="g">
      <div class="col">
        <section class="card">
          <h3>Chủ đề & hiển thị</h3>
          <div class="fr"><span>Phiên bản giao diện</span><div class="seg"><button v-for="(l, k) in { v2: '2.0 Racing poster', v1: '1.0 Cổ điển' }" :key="k" :class="{ on: f.theme === k }" @click="f.theme = k">{{ l }}</button></div></div>
          <div class="fr"><span>Chế độ</span><div class="seg"><button :class="{ on: f.mode === 'light' }" @click="f.mode = 'light'">☀ Sáng</button><button :class="{ on: f.mode === 'dark' }" @click="f.mode = 'dark'">☾ Tối</button></div></div>
          <div class="fr"><span>Cỡ chữ</span><div class="seg"><button v-for="(s, i) in FONT_STEPS" :key="s" :class="{ on: f.fontScale === s }" :style="{ fontSize: (0.78 + i * 0.1) + 'rem' }" :aria-label="`Cỡ chữ ${Math.round(s * 100)}%`" @click="f.fontScale = s">A</button></div></div>
          <label class="fr"><span>Tương phản cao</span><input v-model="f.contrast" type="checkbox" class="tg" /></label>
          <label class="fr"><span>Hiển thị gọn (compact)</span><input type="checkbox" class="tg" :checked="f.density === 'compact'" @change="f.density = $event.target.checked ? 'compact' : 'comfy'" /></label>
        </section>
        <section class="card">
          <div class="row between"><h3>Ảnh nền site</h3><button v-if="f.bgImage" class="btn btn-sm btn-outline" @click="Object.assign(f, BG_DEFAULT)">Bỏ ảnh nền</button></div>
          <div class="fr col1"><span>Ảnh (tải lên hoặc dán URL https)</span>
            <div class="row" style="gap:8px;width:100%"><input v-model.trim="f.bgImage" class="input" style="flex:1;min-width:0" placeholder="https://… hoặc /uploads/…" aria-label="URL ảnh nền" />
              <label class="btn btn-sm btn-outline">{{ upBusy ? 'Đang tải…' : 'Tải ảnh lên' }}<input type="file" accept="image/*" hidden @change="up" /></label></div>
            <div class="sugg"><button v-for="s in SAMPLES" :key="s" type="button" :style="{ backgroundImage: `url(${s})` }" :aria-label="'Dùng ảnh mẫu ' + s" @click="f.bgImage = s" /></div>
            <small v-if="urlErr" class="err">{{ urlErr }}</small></div>
          <template v-if="f.bgImage">
            <div class="fr"><span>Lớp phủ (độ đọc chữ) <b>{{ Math.round(f.bgOverlay * 100) }}%</b></span><input v-model.number="f.bgOverlay" type="range" min="0.3" max="0.95" step="0.05" aria-label="Độ phủ" /></div>
            <div class="fr"><span>Làm mờ <b>{{ f.bgBlur }}px</b></span><input v-model.number="f.bgBlur" type="range" min="0" max="20" step="1" aria-label="Làm mờ" /></div>
            <div class="fr"><span>Cách hiển thị</span><div class="seg"><button v-for="(l, k) in { cover: 'Phủ kín', contain: 'Vừa khung', repeat: 'Lặp lại' }" :key="k" :class="{ on: f.bgFit === k }" @click="f.bgFit = k">{{ l }}</button></div></div>
            <div class="fr"><span>Khi cuộn trang</span><div class="seg"><button :class="{ on: f.bgAttach === 'fixed' }" @click="f.bgAttach = 'fixed'">Cố định</button><button :class="{ on: f.bgAttach === 'scroll' }" @click="f.bgAttach = 'scroll'">Cuộn theo</button></div></div>
            <div class="fr"><span>Áp dụng cho</span><div class="seg"><button :class="{ on: f.bgPages === 'all' }" @click="f.bgPages = 'all'">Mọi trang</button><button :class="{ on: f.bgPages === 'home' }" @click="f.bgPages = 'home'">Chỉ trang chủ</button></div></div>
            <p v-if="f.bgOverlay < 0.55" class="warn small">⚠ Lớp phủ dưới 55% có thể khiến chữ khó đọc trên ảnh nhiều chi tiết.</p>
          </template>
        </section>
        <section class="card danger">
          <h3>Khôi phục mặc định</h3><p class="small muted">Giao diện 2.0, chế độ sáng, cỡ chữ 100%, không ảnh nền.</p>
          <button class="btn btn-sm btn-danger" :disabled="busy" @click="resetAll">Khôi phục mặc định</button>
        </section>
      </div>
      <div class="col">
        <section class="card pvw">
          <div class="row between"><h3>Xem trước</h3><a href="/" target="_blank" class="small">Mở trang chủ ↗</a></div>
          <div :class="['frame', f.mode]" :style="{ '--pf': f.fontScale }">
            <div v-if="f.bgImage" class="pbg" :style="{ backgroundImage: `url(&quot;${f.bgImage}&quot;)`, backgroundSize: f.bgFit === 'repeat' ? 'auto' : f.bgFit, backgroundRepeat: f.bgFit === 'repeat' ? 'repeat' : 'no-repeat', filter: `blur(${f.bgBlur / 2}px)` }" />
            <div class="pov" :style="{ opacity: f.bgImage ? f.bgOverlay : 1 }" />
            <div class="pin">
              <div class="phd"><b :class="f.theme">ARES</b><span>Sản phẩm · Bộ sưu tập · Hỗ trợ</span></div>
              <h2 :class="f.theme">Nhỏ tỉ lệ. Lớn đam mê.</h2>
              <p :class="{ hc: f.contrast }">Thế giới xe mô hình và RC chính hãng — nội dung mẫu để kiểm tra độ đọc chữ trên ảnh nền.</p>
              <div class="pcards"><div v-for="i in 3" :key="i" class="pc"><i /><b>Mẫu xe {{ i }}</b><small>1.290.000đ</small></div></div>
              <span :class="['pbtn', f.theme]">Mua ngay</span>
            </div>
          </div>
          <p class="small muted" style="margin-top:8px">Trang quản trị không hiển thị ảnh nền. Khách thấy thay đổi ở lần tải trang kế tiếp.</p>
        </section>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import api, { errMsg, uploadImage } from '../../api'
import { toast } from '../../stores/prefs'
import { FONT_STEPS, BG_DEFAULT, applyAppearance } from '../../stores/theme'
const SAMPLES = ['/images/bg-pagani.jpg', '/images/toys/traxxas-trx4-0.jpg', '/images/toys/bburago-sf90-card.jpg']
const f = ref(null), saved = ref(null), busy = ref(false), upBusy = ref(false)
const dirty = computed(() => f.value && JSON.stringify(f.value) !== JSON.stringify(saved.value))
const urlErr = computed(() => { const u = f.value?.bgImage; return u && !(u.startsWith('/uploads/') || u.startsWith('/images/') || /^https:\/\/[^\s"'()<>]+$/.test(u)) ? 'Chỉ nhận ảnh đã tải lên hoặc URL bắt đầu bằng https://' : '' })
const load = async () => { const { data } = await api.get('/public/appearance'); saved.value = { ...data }; f.value = { ...data } }
async function save() {
  if (urlErr.value) return toast(urlErr.value, 'error')
  busy.value = true; try { const { data } = await api.put('/admin/appearance', f.value); saved.value = { ...data }; f.value = { ...data }; applyAppearance(data); toast('Đã áp dụng giao diện cho toàn site') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
const revert = () => { f.value = { ...saved.value } }
async function resetAll() { if (!confirm('Khôi phục giao diện mặc định cho toàn site?')) return; busy.value = true; try { const { data } = await api.delete('/admin/appearance'); saved.value = { ...data }; f.value = { ...data }; applyAppearance(data); toast('Đã khôi phục mặc định') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
async function up(e) { const file = e.target.files[0]; if (!file) return; if (file.size > 5e6) return toast('Ảnh tối đa 5MB', 'error'); upBusy.value = true; try { f.value.bgImage = await uploadImage(file) } catch (x) { toast(errMsg(x), 'error') } finally { upBusy.value = false; e.target.value = '' } }
const leave = e => { if (dirty.value) { e.preventDefault(); e.returnValue = '' } }
onMounted(() => { load(); addEventListener('beforeunload', leave) })
onBeforeUnmount(() => removeEventListener('beforeunload', leave))
</script>
<style scoped>
.hd { gap: 12px; flex-wrap: wrap; align-items: flex-end; margin-bottom: 16px; }
.g { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16px; align-items: start; } .col { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.fr { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--border); flex-wrap: wrap; font-size: .9rem; } .fr.col1 { flex-direction: column; align-items: flex-start; }
.fr input[type=range] { flex: 1 1 160px; max-width: 240px; accent-color: var(--primary); } .tg { width: 20px; height: 20px; accent-color: var(--primary); }
.seg { display: inline-flex; border: 1px solid var(--border); flex-wrap: wrap; } .seg button { border: none; background: var(--surface); color: var(--text); padding: 7px 12px; font: 600 .8rem inherit; font-family: inherit; cursor: pointer; } .seg button.on { background: #111; color: #fff; }
.sugg { display: flex; gap: 6px; } .sugg button { width: 64px; height: 40px; border: 1px solid var(--border); background-size: cover; background-position: center; cursor: pointer; }
.err { color: var(--danger); } .warn { color: #b26a00; margin-top: 8px; } .danger { border-color: rgba(200,0,0,.25); }
.pvw { position: sticky; top: 80px; }
.frame { position: relative; overflow: hidden; height: 380px; border: 1px solid var(--border); margin-top: 8px; --pb: #f3efe6; --pt: #111; font-size: calc(14px * var(--pf)); }
.frame.dark { --pb: #121212; --pt: #eee; }
.pbg { position: absolute; inset: -10px; background-position: center; } .pov { position: absolute; inset: 0; background: var(--pb); }
.pin { position: relative; padding: 16px; color: var(--pt); display: flex; flex-direction: column; gap: 10px; }
.phd { display: flex; justify-content: space-between; gap: 8px; font-size: .78em; border-bottom: 2px solid var(--pt); padding-bottom: 8px; } .phd b.v2 { color: #ff6a00; font-style: italic; }
.pin h2 { margin: 0; color: var(--pt); font-size: 1.7em; } .pin h2.v2 { font-family: 'Anton', sans-serif; font-style: italic; text-transform: uppercase; } .pin h2.v1 { font-family: 'Oswald', sans-serif; }
.pin p { margin: 0; font-size: .95em; } .pin p.hc { font-weight: 600; text-decoration: underline; }
.pcards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; } .pc { background: var(--surface); color: var(--text); padding: 6px; display: flex; flex-direction: column; font-size: .8em; } .frame.dark .pc { background: #1e1e1e; color: #eee; } .pc i { height: 50px; background: var(--tile); margin-bottom: 4px; }
.pbtn { align-self: flex-start; background: #ff6a00; color: #fff; padding: 8px 16px; font-weight: 700; font-size: .85em; } .pbtn.v1 { background: #a87a3c; border-radius: 8px; }
@media (max-width: 1000px) { .g { grid-template-columns: 1fr; } .pvw { position: static; } }
</style>
