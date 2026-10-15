<template>
  <article class="bc">
    <header><span :class="['tag', b.bundle.kind]">{{ b.bundle.kind === 'FIXED' ? 'Combo giá gói' : `Mua ${b.bundle.buyQty} tặng ${b.bundle.getQty}` }}</span>
      <h3>{{ b.bundle.title }}</h3><p class="muted small">{{ b.bundle.description }}</p></header>
    <div :class="['ps', { many: b.products.length > 3 }]">
      <router-link v-for="(p, i) in b.products.slice(0, 4)" :key="p.id" :to="`/products/${p.id}`" class="p" :title="p.name">
        <img :src="p.image" alt="" loading="lazy" decoding="async" /><span v-if="i < Math.min(3, b.products.length) - 1 && b.bundle.kind === 'FIXED'" class="plus" aria-hidden="true">+</span>
      </router-link>
      <span v-if="b.products.length > 4" class="more">+{{ b.products.length - 4 }}</span>
    </div>
    <footer>
      <div v-if="b.bundle.kind === 'FIXED'" class="pr"><s class="muted">{{ vnd(b.sum) }}</s><b class="price">{{ vnd(b.bundle.bundlePrice) }}</b><em>Tiết kiệm {{ vnd(b.save) }}</em></div>
      <div v-else class="pr"><b>Chọn {{ b.bundle.buyQty + b.bundle.getQty }} xe</b><em>tặng {{ b.bundle.getQty }} xe rẻ nhất</em></div>
      <button v-if="b.bundle.kind === 'FIXED'" class="btn btn-sm" :disabled="busy" @click="addAll">{{ busy ? '…' : 'Thêm combo' }}</button>
      <button v-else class="btn btn-sm btn-outline" :aria-expanded="open" @click="open = !open">{{ open ? 'Thu gọn' : 'Chọn xe' }}</button>
    </footer>
    <ul v-if="open" class="pick">
      <li v-for="p in b.products" :key="p.id"><img :src="p.image" alt="" /><router-link :to="`/products/${p.id}`" class="nm">{{ p.name }}</router-link><b>{{ vnd(p.price) }}</b>
        <button class="btn btn-sm" :disabled="!p.stock" :aria-label="`Thêm ${p.name}`" @click="one(p)">{{ p.stock ? '+' : 'Hết' }}</button></li>
    </ul>
  </article>
</template>
<script setup>
import { ref } from 'vue'
import { vnd } from '../format'
import { addById } from '../cartAdd'
import { toast } from '../stores/prefs'
const props = defineProps({ b: Object }), busy = ref(false), open = ref(false)
const one = async p => { if (await addById(p.id)) toast(`Đã thêm “${p.name}”`) }
async function addAll() {
  busy.value = true
  try {
    let n = 0; for (const p of props.b.products) if (await addById(p.id)) n++
    toast(n === props.b.products.length ? `Đã thêm combo “${props.b.bundle.title}” — giảm giá tự áp ở giỏ` : `Đã thêm ${n}/${props.b.products.length} món (một số món tạm hết)`, n ? 'success' : 'error')
  } catch { toast('Không thêm được combo', 'error') } finally { busy.value = false }
}
</script>
<style scoped>
.bc { container-type: inline-size; display: flex; flex-direction: column; gap: 14px; padding: 18px; background: var(--surface); border: 1px solid var(--border); min-width: 0; }
.tag { display: inline-block; font: 600 .7rem 'Oswald', sans-serif; letter-spacing: .14em; text-transform: uppercase; padding: 3px 8px; background: #111; color: #fff; }
.tag.FIXED { background: var(--accent); }
h3 { margin: 8px 0 4px; font-size: 1.05rem; }
.ps { display: flex; align-items: center; gap: 6px; } .p { position: relative; flex: 1 1 0; min-width: 0; }
.p img { width: 100%; aspect-ratio: 4/3; object-fit: cover; background: var(--bg-2); }
.plus { position: absolute; right: -11px; top: 50%; transform: translateY(-50%); z-index: 1; width: 18px; height: 18px; border-radius: 50%; background: #111; color: #fff; display: grid; place-items: center; font-weight: 700; font-size: .8rem; }
.more { font-weight: 700; color: var(--muted); padding: 0 4px; }
footer { display: flex; align-items: flex-end; justify-content: space-between; gap: 10px; margin-top: auto; }
.pr { display: flex; flex-direction: column; min-width: 0; } .pr s { font-size: .8rem; } .pr b { font-size: 1.15rem; } .pr em { font-style: normal; font-size: .76rem; color: var(--success); font-weight: 600; }
.pick { list-style: none; display: flex; flex-direction: column; gap: 6px; border-top: 1px solid var(--border); padding-top: 10px; }
.pick li { display: flex; align-items: center; gap: 8px; font-size: .84rem; } .pick img { width: 40px; height: 30px; object-fit: cover; flex-shrink: 0; }
.pick .nm { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text); } .pick .btn { padding: 4px 10px; }
@container (max-width: 260px) { footer { flex-direction: column; align-items: stretch; } footer .btn { width: 100%; } }
</style>
