<template>
  <div class="modal-bg" role="dialog" aria-modal="true" aria-labelledby="sg-t" @click.self="$emit('close')" @keydown.esc="$emit('close')">
    <div class="modal sg">
      <div class="row between"><h3 id="sg-t">Hướng dẫn tỷ lệ & kích thước</h3><button class="x" aria-label="Đóng" @click="$emit('close')">×</button></div>
      <p class="muted small">Tỷ lệ 1:N nghĩa là mô hình nhỏ hơn xe thật N lần. Kích thước tính cho một chiếc sedan/siêu xe dài ~4,5 m.</p>
      <div class="bars" aria-hidden="true">
        <div v-for="s in SCALES" :key="s.k" :class="['b', { on: s.k === current }]"><span class="car" :style="{ width: Math.min(100, s.cm / 56 * 100) + '%' }" /><b>{{ s.k }}</b><small>{{ s.cm }} cm</small></div>
      </div>
      <div class="tw">
        <table class="table">
          <thead><tr><th>Tỷ lệ</th><th>Dài (≈)</th><th>Phổ biến cho</th><th>Gợi ý</th></tr></thead>
          <tbody><tr v-for="s in SCALES" :key="s.k" :class="{ cur: s.k === current }"><td><b>{{ s.k }}</b></td><td>{{ s.cm }} cm</td><td>{{ s.use }}</td><td>{{ s.tip }}</td></tr></tbody>
        </table>
      </div>
      <p class="small muted">Xe RC: 1:10 phổ biến nhất (dài 40–55 cm), cần sân rộng; 1:16–1:28 chạy được trong nhà. Mẹo: so sánh với hộp mica 1:64 (20 ô) dài 35 cm.</p>
    </div>
  </div>
</template>
<script setup>
defineProps({ current: String }); defineEmits(['close'])
const SCALES = [
  { k: '1:64', cm: 7, use: 'Hot Wheels, Mini GT, Tomica', tip: 'Sưu tầm số lượng lớn, hộp mica 20 ô' },
  { k: '1:43', cm: 10.5, use: 'Minichamps, Spark (F1, rally)', tip: 'Chi tiết vừa phải, tủ kính nhỏ' },
  { k: '1:32', cm: 14, use: 'Tamiya Mini 4WD, slot car', tip: 'Đua mini 4WD trên đường đua' },
  { k: '1:24', cm: 19, use: 'Maisto, Bburago, Tamiya kit', tip: 'Mở cửa/capo, đẹp để bàn' },
  { k: '1:18', cm: 25, use: 'Bburago, Kyosho, Norev', tip: 'Cao cấp, nội thất chi tiết' },
  { k: '1:10', cm: 45, use: 'Traxxas Slash, Tamiya RC', tip: 'RC sân rộng, nâng cấp nhiều' },
  { k: '1:8', cm: 56, use: 'Kyosho Inferno, LEGO Technic', tip: 'Buggy nitro, LEGO cỡ lớn' }]
</script>
<style scoped>
.sg { max-width: 760px; display: flex; flex-direction: column; gap: 14px; background: var(--surface); color: var(--text); }
.x { background: none; border: none; font-size: 1.6rem; cursor: pointer; color: var(--text); width: 36px; height: 36px; }
.bars { display: flex; flex-direction: column; gap: 6px; }
.b { display: grid; grid-template-columns: 1fr 52px 60px; align-items: center; gap: 8px; font-size: .82rem; }
.car { display: block; height: 12px; background: #d9d4c9; clip-path: polygon(0 100%, 6% 30%, 30% 0, 70% 0, 88% 35%, 100% 55%, 100% 100%); }
.b.on .car { background: var(--accent); } .b small { color: var(--muted); }
.tw { overflow-x: auto; } tr.cur td { background: var(--bronze-soft); }
</style>
