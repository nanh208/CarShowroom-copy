<template>
  <div class="container page narrow">
    <Breadcrumb :items="[{ label: 'Sản phẩm', to: '/products' }, { label: 'Tìm mẫu phù hợp' }]" />
    <h1>Tìm mẫu phù hợp</h1>
    <p class="muted">3 câu hỏi nhanh — ARES gợi ý tỉ lệ, loại xe và mức giá phù hợp.</p>
    <div class="steps"><i v-for="(q, i) in Q" :key="i" :class="{ on: i <= step }" /></div>
    <transition name="qz" mode="out-in">
      <section v-if="step < Q.length" :key="step" class="card q">
        <small class="muted">Câu {{ step + 1 }}/{{ Q.length }}</small><h2>{{ Q[step].t }}</h2>
        <div class="opts"><button v-for="o in Q[step].o" :key="o.v" :class="['opt', { on: ans[Q[step].k] === o.v }]" @click="pick(o.v)"><span>{{ o.i }}</span><b>{{ o.l }}</b><small>{{ o.d }}</small></button></div>
        <button v-if="step" class="btn btn-sm btn-outline" @click="step--">← Quay lại</button>
      </section>
      <section v-else key="r" class="res">
        <div class="card sum"><h2>Gợi ý cho bạn</h2><p>{{ summary }}</p>
          <div class="row" style="gap:8px;flex-wrap:wrap"><router-link :to="{ path: '/products', query }" class="btn">Xem tất cả {{ total }} mẫu →</router-link><button class="btn btn-outline" @click="restart">Làm lại</button></div></div>
        <div v-if="loading"><Skeleton :count="4" /></div>
        <p v-else-if="!items.length" class="muted">Chưa có mẫu khớp hoàn toàn — thử nới ngân sách nhé.</p>
        <div v-else class="grid grid-4" v-stagger><ProductCard v-for="p in items" :key="p.id" :p="p" /></div>
      </section>
    </transition>
  </div>
</template>
<script setup>
import { ref, reactive, computed } from 'vue'
import api from '../api'
import Breadcrumb from '../components/Breadcrumb.vue'
import ProductCard from '../components/ProductCard.vue'
import Skeleton from '../components/Skeleton.vue'
const Q = [
  { k: 'who', t: 'Mua cho ai?', o: [{ v: 'kid', i: '🧒', l: 'Bé dưới 8 tuổi', d: 'Bền, an toàn, dễ chơi' }, { v: 'teen', i: '🧑', l: 'Thiếu niên 8–14', d: 'Lắp ráp, RC cơ bản' }, { v: 'adult', i: '🧔', l: 'Người lớn / sưu tầm', d: 'Chi tiết cao, trưng bày' }] },
  { k: 'use', t: 'Dùng để làm gì?', o: [{ v: 'display', i: '🏛️', l: 'Trưng bày', d: 'Mô hình tĩnh, hộp mica' }, { v: 'play', i: '🏁', l: 'Chạy / điều khiển', d: 'Xe RC, drift, crawler' }, { v: 'build', i: '🔧', l: 'Lắp ráp', d: 'LEGO Technic, kit Tamiya' }] },
  { k: 'budget', t: 'Ngân sách?', o: [{ v: 500000, i: '💵', l: 'Dưới 500k', d: '1:64, 1:43, mini' }, { v: 3000000, i: '💳', l: '500k – 3 triệu', d: '1:24, 1:18, RC phổ thông' }, { v: 50000000, i: '💎', l: 'Trên 3 triệu', d: 'RC hobby-grade, LEGO lớn' }] }]
const step = ref(0), ans = reactive({}), items = ref([]), total = ref(0), loading = ref(false)
const query = computed(() => { const q = { price: '0-' + ans.budget }; if (ans.who === 'kid') q.age = 6; else if (ans.who === 'teen') q.age = 12
  q.type = ans.use === 'display' ? 'DISPLAY' : ans.use === 'play' ? 'RC' : ans.use === 'build' ? 'KIT' : undefined; if (ans.use === 'display' && ans.budget <= 500000) q.type = 'MODEL'; return q })
const SC = { 500000: '1:64 (dài ~7cm) hoặc 1:43 (~10cm)', 3000000: '1:24 (~18cm) hoặc 1:18 (~25cm); RC 1:16–1:12', 50000000: '1:18 cao cấp hoặc RC 1:10 (~45cm)' }
const summary = computed(() => `Tỉ lệ nên chọn: ${SC[ans.budget]}. ${ans.use === 'play' ? 'Ưu tiên xe RC có pin sạc & phụ tùng sẵn.' : ans.use === 'build' ? 'Bộ lắp ráp có hướng dẫn từng bước, 2–10 giờ hoàn thành.' : 'Kèm hộp mica chống bụi để trưng bày lâu bền.'}`)
async function pick(v) {
  ans[Q[step.value].k] = v; step.value++
  if (step.value === Q.length) { loading.value = true
    try { let { data } = await api.get('/products', { params: { maxPrice: ans.budget, age: query.value.age, type: query.value.type, size: 8, sort: 'bestseller' } })
      if (!data.content.length) data = (await api.get('/products', { params: { maxPrice: ans.budget, age: query.value.age, size: 8 } })).data
      items.value = data.content; total.value = data.totalElements } finally { loading.value = false } }
}
const restart = () => { step.value = 0; Object.keys(ans).forEach(k => delete ans[k]) }
</script>
<style scoped>
.narrow { max-width: 1000px; }
.steps { display: flex; gap: 6px; margin: 18px 0; } .steps i { flex: 1; height: 4px; background: var(--border); transition: background .3s ease-out; } .steps i.on { background: var(--primary); }
.q h2 { margin: 6px 0 16px; }
.opts { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; margin-bottom: 14px; }
.opt { display: flex; flex-direction: column; gap: 4px; text-align: left; padding: 16px; border: 1.5px solid var(--border); background: var(--surface); color: var(--text); cursor: pointer; font-family: inherit; transition: transform .12s ease-out, border-color .15s; }
.opt:active { transform: scale(.98); } .opt.on { border-color: var(--primary); } .opt span { font-size: 1.6rem; } .opt small { color: var(--muted); }
.sum { margin-bottom: 16px; border-left: 4px solid var(--primary); } .sum p { margin: 6px 0 12px; }
.qz-enter-active, .qz-leave-active { transition: opacity .18s ease-out, transform .2s ease-out; } .qz-enter-from { opacity: 0; transform: translateX(16px); } .qz-leave-to { opacity: 0; transform: translateX(-16px); }
@media (prefers-reduced-motion: reduce) { .qz-enter-active, .qz-leave-active { transition: none; } }
</style>
