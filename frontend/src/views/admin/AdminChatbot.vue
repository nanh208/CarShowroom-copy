<template>
  <div>
    <div class="row between hd">
      <div>
        <h1>Trợ lý ảo & Tư vấn AI</h1>
        <p class="muted small">Cấu hình chuyên gia AI tư vấn, ý định từ khoá và xem lại toàn bộ lịch sử hội thoại khách hàng.</p>
      </div>
      <div class="seg" role="tablist">
        <button v-for="(l, k) in TABS" :key="k" role="tab" :aria-selected="tab === k" :class="{ on: tab === k }" @click="tab = k">{{ l }}</button>
      </div>
    </div>

    <div v-if="stats" class="kp" v-stagger>
      <div class="card"><small class="muted">Câu hỏi (1000 gần nhất)</small><b><CountUp :value="stats.total" /></b></div>
      <div class="card"><small class="muted">Tỉ lệ hiểu được</small><b>{{ stats.matchedRate }}%</b></div>
      <div class="card"><small class="muted">Phiên trò chuyện</small><b><CountUp :value="stats.sessions" /></b></div>
      <div class="card"><small class="muted">Ý định đang bật</small><b>{{ intents.filter(i => i.active).length }}/{{ intents.length }}</b></div>
    </div>

    <!-- TAB 1: Ý ĐỊNH & PHẢN HỒI -->
    <template v-if="tab === 'intents'">
      <div class="g2">
        <div class="card">
          <div class="row between" style="margin-bottom:8px">
            <h3>Ý định quy tắc ({{ intents.length }})</h3>
            <div class="row" style="gap:6px">
              <input v-model.trim="q" class="input" placeholder="Lọc…" aria-label="Lọc ý định" style="width:140px" />
              <button class="btn btn-sm" @click="edit()">+ Thêm</button>
            </div>
          </div>
          <div v-if="loading" class="sk" style="height:200px" />
          <div v-for="i in shownIntents" :key="i.id" :class="['it', { off: !i.active }]">
            <div style="min-width:0"><b>{{ i.name }}</b> <small class="muted">· {{ i.hits || 0 }} lượt</small><div class="small muted ell">{{ i.keywords }}</div></div>
            <div class="row" style="gap:6px;flex-wrap:nowrap"><label class="sw" :title="i.active ? 'Đang bật' : 'Đang tắt'"><input type="checkbox" :checked="i.active" @change="toggle(i)" :aria-label="`Bật ${i.name}`" /></label><button class="btn btn-sm btn-outline" @click="edit(i)">Sửa</button><button class="btn btn-sm btn-outline" @click="del(i)">Xoá</button></div>
          </div>
          <p v-if="!loading && !shownIntents.length" class="muted">Không có ý định.</p>
        </div>
        <div class="card">
          <h3>Thử quy tắc trợ lý</h3>
          <form class="row" style="gap:6px;margin:8px 0" @submit.prevent="test"><input v-model="tq" class="input" style="flex:1" placeholder="VD: phí ship ra Hà Nội?" aria-label="Câu hỏi thử" /><button class="btn btn-sm" :disabled="!tq">Hỏi</button></form>
          <div v-if="tr" class="tr"><small class="muted">Ý định: <b>{{ tr.intent || 'không khớp' }}</b></small><p>{{ tr.text }}</p>
            <ul v-if="tr.products"><li v-for="p in tr.products" :key="p.id">{{ p.name }} — {{ vnd(p.price) }}</li></ul><ul v-if="tr.coupons"><li v-for="c in tr.coupons" :key="c.code">{{ c.code }} · {{ c.value }}</li></ul>
            <div class="row" style="gap:4px;flex-wrap:wrap"><span v-for="c in tr.quick" :key="c" class="qc">{{ c }}</span></div></div>
          <h3 style="margin-top:16px">Câu hỏi chưa hiểu</h3>
          <p class="small muted">Thêm từ khoá vào ý định phù hợp để trợ lý trả lời được.</p>
          <ul class="um"><li v-for="u in stats?.unmatched || []" :key="u.q"><span>{{ u.q }}</span><b>{{ u.count }}</b><button class="btn btn-sm btn-outline" @click="edit(null, u.q)">+ Ý định</button></li><li v-if="!stats?.unmatched?.length" class="muted small">Không có — tuyệt!</li></ul>
        </div>
      </div>
    </template>

    <!-- TAB 2: CẤU HÌNH AI TƯ VẤN (NEW) -->
    <template v-else-if="tab === 'ai'">
      <div class="g2">
        <div class="card">
          <h3>Cấu hình Trí tuệ Nhân tạo (Google Gemini AI)</h3>
          <p class="small muted" style="margin-bottom:14px">Tích hợp Google Gemini API để tư vấn chuyên sâu về xe mô hình, tỷ lệ, chọn quà và kỹ thuật RC. Nếu không nhập key, hệ thống tự động kích hoạt Ares Knowledge Engine nội bộ để luôn phản hồi hoàn hảo.</p>

          <form @submit.prevent="saveAi">
            <div class="field" style="margin-bottom:12px">
              <label class="row" style="gap:8px;font-weight:700;cursor:pointer">
                <input type="checkbox" v-model="aiCfg.enabled" style="width:18px;height:18px;accent-color:var(--primary)" />
                Bật tính năng AI tư vấn khách hàng
              </label>
            </div>

            <div class="field" style="margin-bottom:12px">
              <label>Mô hình AI (Model)</label>
              <select v-model="aiCfg.model" class="input" style="width:100%">
                <option value="gemini-1.5-flash">Google Gemini 1.5 Flash (Khuyên dùng - Nhanh, thông minh, tiết kiệm)</option>
                <option value="gemini-2.0-flash">Google Gemini 2.0 Flash (Thế hệ mới nhất)</option>
                <option value="gemini-1.5-pro">Google Gemini 1.5 Pro (Suy luận chuyên sâu cao cấp)</option>
              </select>
            </div>

            <div class="field" style="margin-bottom:12px">
              <label class="row between">
                <span>Google Gemini API Key</span>
                <small v-if="aiCfg.hasApiKey" class="small" style="color:var(--success)">Đã cấu hình key</small>
              </label>
              <input v-model="aiCfg.apiKey" type="password" class="input" :placeholder="aiCfg.maskedApiKey || 'Dán Google Gemini API Key tại đây (bỏ trống nếu dùng engine nội bộ)'" />
              <small class="muted" style="font-size:.76rem;display:block;margin-top:4px">Lấy API Key miễn phí tại <a href="https://aistudio.google.com" target="_blank" rel="noopener noreferrer">aistudio.google.com</a>.</small>
            </div>

            <div class="field" style="margin-bottom:14px">
              <label>Lời nhắc định hướng chuyên gia (System Prompt)</label>
              <textarea v-model="aiCfg.systemPrompt" class="input" rows="4" style="font-size:.85rem;line-height:1.5" />
            </div>

            <div class="row" style="gap:8px">
              <button class="btn" :disabled="aiSaving">{{ aiSaving ? 'Đang lưu…' : 'Lưu cấu hình AI' }}</button>
            </div>
          </form>
        </div>

        <div class="card">
          <h3>Thử nghiệm phản hồi AI trực tiếp</h3>
          <p class="small muted">Kiểm tra ngay câu trả lời của chuyên gia AI đối với các câu hỏi về tỷ lệ, dòng xe, hoặc gợi ý quà tặng.</p>
          <form class="row" style="gap:6px;margin:10px 0" @submit.prevent="testAi">
            <input v-model="aiTestQ" class="input" style="flex:1" placeholder="VD: Bé 10 tuổi nên mua xe RC nào?" aria-label="Câu hỏi thử AI" />
            <button class="btn btn-sm" :disabled="!aiTestQ || aiTesting">{{ aiTesting ? 'Đang hỏi…' : 'Hỏi AI' }}</button>
          </form>

          <div v-if="aiTestRes" class="tr" style="border-left:3px solid var(--primary)">
            <small class="muted">Mô hình xử lý: <b>{{ aiTestRes.modelUsed || 'Ares Knowledge Engine' }}</b></small>
            <p style="margin:8px 0;line-height:1.5;white-space:pre-wrap">{{ aiTestRes.text }}</p>
            <div v-if="aiTestRes.products?.length" style="margin-top:8px">
              <small class="muted">Sản phẩm đính kèm đề xuất:</small>
              <ul style="margin:4px 0 0 16px;font-size:.86rem">
                <li v-for="p in aiTestRes.products" :key="p.id"><b>{{ p.name }}</b> — {{ vnd(p.price) }} ({{ p.stock ? 'Còn hàng' : 'Hết hàng' }})</li>
              </ul>
            </div>
            <div v-if="aiTestRes.quick?.length" class="row" style="gap:4px;flex-wrap:wrap;margin-top:8px">
              <span v-for="c in aiTestRes.quick" :key="c" class="qc">{{ c }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- TAB 3: LỊCH SỬ HỘI THOẠI -->
    <div v-else class="split">
      <div class="card sl">
        <input v-model.trim="tq2" class="input" placeholder="Tìm trong hội thoại…" aria-label="Tìm hội thoại" style="margin-bottom:8px" />
        <label class="small row" style="gap:6px;margin-bottom:6px"><input v-model="onlyUnmatched" type="checkbox" /> Chỉ phiên có câu chưa hiểu</label>
        <button v-for="t in shownTx" :key="t.session" :class="['ss', { on: cur?.session === t.session }]" @click="cur = t">
          <div class="row between"><b>{{ t.userId ? 'Khách #' + t.userId : 'Khách vãng lai' }}</b><small class="muted">{{ ago(t.last) }}</small></div>
          <small class="muted ell">{{ t.messages[t.messages.length - 1].question }}</small>
          <small>{{ t.count }} câu<span v-if="t.unmatched" class="um2"> · {{ t.unmatched }} chưa hiểu</span></small>
        </button>
        <p v-if="!shownTx.length" class="muted small">Không có hội thoại.</p>
      </div>
      <div class="card cv">
        <p v-if="!cur" class="muted">Chọn một phiên để xem.</p>
        <template v-else>
          <div class="small muted" style="margin-bottom:8px">Phiên {{ cur.session }}</div>
          <div v-for="m in cur.messages" :key="m.id" class="pair">
            <div class="q">{{ m.question }}<small>{{ dt(m.createdAt) }}</small></div>
            <div :class="['a', { miss: !m.matched }]"><small>{{ m.intent || 'không khớp' }}</small>{{ m.answer }}</div>
          </div>
        </template>
      </div>
    </div>

    <!-- MODAL SỬA Ý ĐỊNH -->
    <div v-if="f" class="modal-bg" @click.self="f = null">
      <form class="modal" @submit.prevent="save">
        <h3>{{ f.id ? 'Sửa ý định' : 'Ý định mới' }}</h3>
        <div class="field"><label>Tên *</label><input v-model.trim="f.name" class="input" required maxlength="80" /></div>
        <div class="field"><label>Từ khoá * <small class="muted">(phân tách dấu phẩy, gõ có/không dấu đều được)</small></label><textarea v-model="f.keywords" class="input" rows="2" required /></div>
        <div class="field"><label>Câu trả lời *</label><textarea v-model="f.answer" class="input" rows="5" required maxlength="3000" /></div>
        <div class="field"><label>Gợi ý nhanh <small class="muted">(phân tách dấu phẩy)</small></label><input v-model="f.quickReplies" class="input" placeholder="Tra cứu đơn hàng,Gặp nhân viên" /></div>
        <div class="row" style="gap:12px"><label class="small row" style="gap:6px"><input v-model="f.active" type="checkbox" /> Đang bật</label><label class="small">Thứ tự <input v-model.number="f.sortOrder" type="number" class="input" style="width:80px" /></label></div>
        <p v-if="err" class="alert alert-error">{{ err }}</p>
        <div class="row" style="justify-content:flex-end;gap:8px;margin-top:10px"><button type="button" class="btn btn-outline" @click="f = null">Huỷ</button><button class="btn">Lưu</button></div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { vnd, dt, ago } from '../../format'
import { toast } from '../../stores/prefs'
import CountUp from '../../components/CountUp.vue'

const TABS = { intents: 'Ý định & Phản hồi', transcripts: 'Lịch sử hội thoại', ai: 'Cấu hình AI tư vấn' }
const tab = ref('intents'), intents = ref([]), stats = ref(null), tx = ref([]), cur = ref(null), f = ref(null), err = ref(''), q = ref(''), tq = ref(''), tr = ref(null), tq2 = ref(''), onlyUnmatched = ref(false), loading = ref(true)

const aiCfg = ref({ enabled: true, model: 'gemini-1.5-flash', apiKey: '', maskedApiKey: '', systemPrompt: '', hasApiKey: false })
const aiSaving = ref(false), aiTestQ = ref(''), aiTestRes = ref(null), aiTesting = ref(false)

const shownIntents = computed(() => intents.value.filter(i => !q.value || (i.name + ' ' + i.keywords).toLowerCase().includes(q.value.toLowerCase())))
const shownTx = computed(() => tx.value.filter(t => (!onlyUnmatched.value || t.unmatched) && (!tq2.value || t.messages.some(m => (m.question + ' ' + m.answer).toLowerCase().includes(tq2.value.toLowerCase())))))

async function load() {
  try {
    const [a, b, c, d] = await Promise.all([
      api.get('/admin/bot/intents'),
      api.get('/admin/bot/stats'),
      api.get('/admin/bot/transcripts'),
      api.get('/admin/bot/ai-config').catch(() => ({ data: {} }))
    ])
    intents.value = a.data
    stats.value = b.data
    tx.value = c.data
    if (d.data) {
      aiCfg.value = { ...aiCfg.value, ...d.data }
    }
  } catch (e) {
    console.error('Lỗi tải dữ liệu chatbot:', e)
  } finally {
    loading.value = false
  }
}

const edit = (i, kw) => { err.value = ''; f.value = i ? { ...i } : { name: '', keywords: kw || '', answer: '', quickReplies: '', active: true, sortOrder: intents.value.length } }

async function save() {
  try {
    f.value.id ? await api.put(`/admin/bot/intents/${f.value.id}`, f.value) : await api.post('/admin/bot/intents', f.value)
    f.value = null
    toast('Đã lưu ý định')
    load()
  } catch (e) {
    err.value = errMsg(e)
  }
}

async function toggle(i) {
  try {
    await api.put(`/admin/bot/intents/${i.id}`, { ...i, active: !i.active })
    load()
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

async function del(i) {
  if (!confirm('Xoá ý định ' + i.name + '?')) return
  await api.delete(`/admin/bot/intents/${i.id}`)
  load()
}

async function test() {
  try {
    tr.value = (await api.post('/admin/bot/test', { text: tq.value })).data
    load()
  } catch (e) {
    toast(errMsg(e), 'error')
  }
}

async function saveAi() {
  aiSaving.value = true
  try {
    const res = await api.post('/admin/bot/ai-config', aiCfg.value)
    aiCfg.value = { ...aiCfg.value, ...res.data, apiKey: '' }
    toast('Đã lưu cấu hình AI thành công')
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    aiSaving.value = false
  }
}

async function testAi() {
  if (!aiTestQ.value) return
  aiTesting.value = true
  try {
    const res = await api.post('/admin/bot/ai-test', { text: aiTestQ.value })
    aiTestRes.value = res.data
  } catch (e) {
    toast(errMsg(e), 'error')
  } finally {
    aiTesting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.hd { gap: 12px; flex-wrap: wrap; align-items: flex-end; margin-bottom: 14px; }
.seg { display: flex; border: 1px solid var(--border); }
.seg button { border: none; background: none; padding: 8px 14px; font: 600 .82rem inherit; font-family: inherit; cursor: pointer; color: var(--muted); }
.seg button.on { background: #111; color: #fff; }
.kp { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; margin-bottom: 16px; }
.kp b { display: block; font: 700 1.5rem 'Oswald', sans-serif; margin-top: 4px; }
.g2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 380px), 1fr)); gap: 16px; }
.g2 > * { min-width: 0; }
.it { display: flex; justify-content: space-between; gap: 10px; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border); }
.it.off { opacity: .55; }
.ell { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.sw input { width: 18px; height: 18px; accent-color: var(--primary); }
.tr { background: var(--bg-2); padding: 10px 12px; }
.tr p { white-space: pre-wrap; margin: 4px 0 8px; }
.qc { border: 1px solid var(--primary); padding: 2px 8px; font-size: .74rem; }
.um { list-style: none; padding: 0; margin: 8px 0 0; }
.um li { display: grid; grid-template-columns: 1fr auto auto; gap: 8px; align-items: center; padding: 6px 0; border-bottom: 1px dashed var(--border); font-size: .86rem; }
.um span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.split { display: grid; grid-template-columns: minmax(240px, 340px) 1fr; gap: 16px; align-items: start; }
.split > * { min-width: 0; }
.sl { max-height: 70vh; overflow-y: auto; }
.ss { display: flex; flex-direction: column; gap: 2px; width: 100%; text-align: left; background: none; border: none; border-bottom: 1px solid var(--border); padding: 10px 6px; cursor: pointer; font-family: inherit; color: var(--text); }
.ss.on { background: var(--bronze-soft); }
.um2 { color: var(--danger); }
.cv { max-height: 70vh; overflow-y: auto; }
.pair { display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px; }
.q { align-self: flex-end; background: var(--primary); color: #fff; padding: 8px 12px; max-width: 80%; }
.q small { display: block; opacity: .7; font-size: .68rem; }
.a { align-self: flex-start; background: var(--bg-2); padding: 8px 12px; max-width: 85%; white-space: pre-wrap; font-size: .88rem; }
.a small { display: block; color: var(--muted); font-size: .68rem; text-transform: uppercase; letter-spacing: .06em; }
.a.miss { border-left: 3px solid var(--danger); }
@media (max-width: 899px) { .split { grid-template-columns: 1fr; } .sl { max-height: 40vh; } }
</style>
