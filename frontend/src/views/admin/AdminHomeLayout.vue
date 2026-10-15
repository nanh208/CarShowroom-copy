<template>
  <div>
    <div class="row between hd"><div><h1>Bố cục trang chủ</h1><p class="muted small">Sắp xếp thứ tự và bật/tắt từng khu vực. Áp dụng ngay cho cả giao diện 1.0 và 2.0.</p></div>
      <div class="row" style="gap:8px"><a href="/" target="_blank" class="btn btn-sm btn-outline">Xem trang chủ ↗</a><button class="btn" :disabled="!dirty || busy" @click="save">{{ busy ? 'Đang lưu…' : 'Lưu bố cục' }}</button></div></div>
    <div class="wrap2">
      <ol class="card list">
        <li v-for="(s, i) in list" :key="s.key" :class="{ off: !s.visible }">
          <span class="n">{{ i + 1 }}</span>
          <div class="grow"><b>{{ META[s.key]?.l || s.key }}</b><small class="muted">{{ META[s.key]?.d }}</small></div>
          <label class="sw"><input v-model="s.visible" type="checkbox" :aria-label="`Hiển thị ${META[s.key]?.l}`" @change="dirty = true" /><span>{{ s.visible ? 'Hiện' : 'Ẩn' }}</span></label>
          <button class="btn btn-sm btn-outline" :disabled="i === 0" :aria-label="`Đưa ${META[s.key]?.l} lên`" @click="mv(i, -1)">↑</button>
          <button class="btn btn-sm btn-outline" :disabled="i === list.length - 1" :aria-label="`Đưa ${META[s.key]?.l} xuống`" @click="mv(i, 1)">↓</button>
        </li>
      </ol>
      <div class="card prev" aria-label="Xem trước">
        <small class="muted">Xem trước</small>
        <div v-for="s in list.filter(x => x.visible)" :key="s.key" :class="['blk', s.key]">{{ META[s.key]?.l }}</div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'
const META = { hero: { l: 'Hero / banner chính', d: 'Ảnh lớn đầu trang' }, flash: { l: 'Flash sale', d: 'Chỉ hiện khi có phiên đang chạy' }, bundles: { l: 'Combo ưu đãi', d: 'Mua 2 tặng 1, giá gói' },
  featured: { l: 'Sản phẩm nổi bật', d: 'Bán chạy, RC, Model, Kit, phụ kiện' }, auctions: { l: 'Đấu giá hàng hiếm', d: 'Phiên đang/sắp diễn ra' }, recent: { l: 'Đã xem gần đây', d: 'Theo trình duyệt của khách' }, reviews: { l: 'Đánh giá khách hàng', d: '3 đánh giá mới nhất' } }
const list = ref([]), dirty = ref(false), busy = ref(false)
const mv = (i, d) => { const l = list.value; [l[i], l[i + d]] = [l[i + d], l[i]]; dirty.value = true }
async function save() { busy.value = true; try { list.value = (await api.put('/admin/home-layout', list.value)).data; dirty.value = false; toast('Đã lưu bố cục trang chủ') } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false } }
onMounted(async () => { list.value = (await api.get('/public/home-layout')).data.map(x => ({ ...x })) })
</script>
<style scoped>
.hd { gap: 12px; flex-wrap: wrap; align-items: flex-end; margin-bottom: 16px; }
.wrap2 { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.list { list-style: none; padding: 6px 14px; } .list li { display: flex; align-items: center; gap: 10px; padding: 12px 0; border-bottom: 1px solid var(--border); } .list li:last-child { border: none; }
.list li.off { opacity: .55; } .n { width: 26px; height: 26px; display: grid; place-items: center; background: #111; color: #fff; font-weight: 700; font-size: .8rem; flex-shrink: 0; }
.grow { min-width: 0; display: flex; flex-direction: column; } .sw { display: flex; gap: 6px; align-items: center; font-size: .82rem; margin: 0; }
.prev { display: flex; flex-direction: column; gap: 6px; } .blk { padding: 10px; background: var(--bg-2); font-size: .8rem; font-weight: 600; border-left: 3px solid var(--accent); }
.blk.hero { height: 70px; background: #111; color: #fff; } .blk.featured { height: 54px; }
@media (max-width: 899px) { .wrap2 { grid-template-columns: 1fr; } .list li { flex-wrap: wrap; } .list .grow { flex: 1 1 60%; } }
</style>
