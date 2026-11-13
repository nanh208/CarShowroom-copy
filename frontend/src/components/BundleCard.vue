<template>
  <article class="bc">
    <header class="bhead" @click="showDetail = true" title="Bấm để xem chi tiết combo">
      <div class="tag-row">
        <span :class="['tag', b.bundle.kind]">{{ b.bundle.kind === 'FIXED' ? 'Combo giá gói' : `Mua ${b.bundle.buyQty} tặng ${b.bundle.getQty}` }}</span>
        <span class="view-detail-hint">Chi tiết ↗</span>
      </div>
      <h3 class="btitle">{{ b.bundle.title }}</h3>
      <p class="muted small">{{ b.bundle.description }}</p>
    </header>

    <div :class="['ps', { many: b.products.length > 3 }]" @click="showDetail = true" style="cursor:pointer" title="Bấm để xem chi tiết combo">
      <div v-for="(p, i) in b.products.slice(0, 4)" :key="p.id" class="p" :title="p.name">
        <img :src="p.image" :alt="p.name" loading="lazy" decoding="async" />
        <span v-if="i < Math.min(3, b.products.length) - 1 && b.bundle.kind === 'FIXED'" class="plus" aria-hidden="true">+</span>
      </div>
      <span v-if="b.products.length > 4" class="more">+{{ b.products.length - 4 }}</span>
    </div>

    <footer>
      <div v-if="b.bundle.kind === 'FIXED'" class="pr">
        <s class="muted">{{ vnd(b.sum) }}</s>
        <b class="price">{{ vnd(b.bundle.bundlePrice) }}</b>
        <em>Tiết kiệm {{ vnd(b.save) }}</em>
      </div>
      <div v-else class="pr">
        <b>Chọn {{ b.bundle.buyQty + b.bundle.getQty }} xe</b>
        <em>tặng {{ b.bundle.getQty }} xe rẻ nhất</em>
      </div>
      <div class="btn-group">
        <button class="btn btn-sm btn-outline detail-btn" title="Xem danh sách sản phẩm trong combo" @click="showDetail = true">Chi tiết</button>
        <button v-if="b.bundle.kind === 'FIXED'" class="btn btn-sm" :disabled="busy" @click="addAll">{{ busy ? '…' : 'Thêm combo' }}</button>
        <button v-else class="btn btn-sm btn-outline" :aria-expanded="open" @click="open = !open">{{ open ? 'Thu gọn' : 'Chọn xe' }}</button>
      </div>
    </footer>

    <ul v-if="open" class="pick">
      <li v-for="p in b.products" :key="p.id">
        <img :src="p.image" alt="" />
        <router-link :to="`/products/${p.id}`" class="nm">{{ p.name }}</router-link>
        <b>{{ vnd(p.price) }}</b>
        <button class="btn btn-sm" :disabled="!p.stock" :aria-label="`Thêm ${p.name}`" @click="one(p)">{{ p.stock ? '+' : 'Hết' }}</button>
      </li>
    </ul>

    <!-- MODAL XEM CHI TIẾT COMBO -->
    <Teleport to="body">
      <div v-if="showDetail" class="combo-modal-overlay" @click.self="showDetail = false">
        <div class="combo-modal-card card">
          <div class="cm-head">
            <div>
              <span :class="['tag', b.bundle.kind]">{{ b.bundle.kind === 'FIXED' ? 'Combo giá gói' : `Mua ${b.bundle.buyQty} tặng ${b.bundle.getQty}` }}</span>
              <h2>{{ b.bundle.title }}</h2>
              <p class="muted small">{{ b.bundle.description }}</p>
            </div>
            <button class="close-btn" aria-label="Đóng" @click="showDetail = false">✕</button>
          </div>

          <div class="cm-body">
            <h4 style="margin-bottom:8px">Sản phẩm bao gồm trong gói ({{ b.products.length }} món):</h4>
            <div class="cm-prods">
              <div v-for="p in b.products" :key="p.id" class="cm-prod-item">
                <img :src="p.image" :alt="p.name" />
                <div class="cm-pinfo">
                  <router-link :to="`/products/${p.id}`" class="cm-pname" @click="showDetail = false">{{ p.name }}</router-link>
                  <div class="cm-pmeta">
                    <span class="prod-price">{{ vnd(p.price) }}</span>
                    <span :class="['stock-pill', p.stock > 0 ? 'in' : 'out']">{{ p.stock > 0 ? 'Còn hàng' : 'Hết hàng' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="b.bundle.kind === 'FIXED'" class="cm-calc-box">
              <div class="cm-row">
                <span>Tổng giá mua lẻ từng món:</span>
                <s class="muted">{{ vnd(b.sum) }}</s>
              </div>
              <div class="cm-row ok">
                <span>Ưu đãi trọn gói (tiết kiệm):</span>
                <b>−{{ vnd(b.save) }}</b>
              </div>
              <div class="cm-row total-row">
                <span>Giá bán cả combo:</span>
                <b class="price" style="font-size:1.35rem;color:var(--accent)">{{ vnd(b.bundle.bundlePrice) }}</b>
              </div>
            </div>
            <div v-else class="cm-calc-box">
              <p class="small muted">Chương trình Mua {{ b.bundle.buyQty }} tặng {{ b.bundle.getQty }}: Chọn {{ b.bundle.buyQty + b.bundle.getQty }} xe bất kỳ trong nhóm để được miễn phí xe rẻ nhất!</p>
            </div>
          </div>

          <div class="cm-footer">
            <button class="btn btn-outline" @click="showDetail = false">Đóng</button>
            <button v-if="b.bundle.kind === 'FIXED'" class="btn" :disabled="busy" @click="addAllAndClose">
              {{ busy ? 'Đang thêm…' : `Mua trọn gói (${vnd(b.bundle.bundlePrice)})` }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </article>
</template>

<script setup>
import { ref } from 'vue'
import { vnd } from '../format'
import { addById } from '../cartAdd'
import { useCart } from '../stores/cart'
import { toast } from '../stores/prefs'

const cart = useCart()
const props = defineProps({ b: Object })
const busy = ref(false), open = ref(false), showDetail = ref(false)

const one = async p => {
  if (await addById(p.id, 1, null, { id: props.b.bundle.id, title: props.b.bundle.title })) {
    toast(`Đã thêm “${p.name}”`)
  }
}

async function addAll() {
  busy.value = true
  try {
    if (props.b.bundle.kind === 'FIXED') {
      // Đưa trọn gói combo vào giỏ hàng dưới dạng 1 gói thống nhất
      const pkg = {
        bundleId: props.b.bundle.id,
        title: props.b.bundle.title,
        bundlePrice: props.b.bundle.bundlePrice,
        sum: props.b.sum,
        save: props.b.save,
        products: props.b.products.map(p => ({
          productId: p.id,
          name: p.name,
          image: p.image,
          price: p.price,
          stock: p.stock
        }))
      }
      cart.addBundle(pkg)
      toast(`Đã thêm trọn gói “${props.b.bundle.title}” vào giỏ hàng!`, 'success')
    } else {
      let n = 0
      for (const p of props.b.products) {
        if (await addById(p.id, 1, null, { id: props.b.bundle.id, title: props.b.bundle.title })) n++
      }
      toast(n === props.b.products.length ? `Đã thêm combo “${props.b.bundle.title}”` : `Đã thêm ${n}/${props.b.products.length} món`, n ? 'success' : 'error')
    }
  } catch {
    toast('Không thêm được combo', 'error')
  } finally {
    busy.value = false
  }
}

async function addAllAndClose() {
  await addAll()
  showDetail.value = false
}
</script>

<style scoped>
.bc { container-type: inline-size; display: flex; flex-direction: column; gap: 14px; padding: 18px; background: var(--surface); border: 1px solid var(--border); min-width: 0; transition: border-color .2s; }
.bc:hover { border-color: rgba(224, 86, 36, .4); }
.bhead { cursor: pointer; }
.tag-row { display: flex; justify-content: space-between; align-items: center; gap: 6px; }
.tag { display: inline-block; font: 600 .7rem 'Oswald', sans-serif; letter-spacing: .14em; text-transform: uppercase; padding: 3px 8px; background: #111; color: #fff; }
.tag.FIXED { background: var(--accent); }
.view-detail-hint { font-size: .74rem; color: var(--accent); font-weight: 600; }
.btitle { margin: 8px 0 4px; font-size: 1.05rem; transition: color .15s; }
.bhead:hover .btitle { color: var(--accent); }
.ps { display: flex; align-items: center; gap: 6px; }
.p { position: relative; flex: 1 1 0; min-width: 0; }
.p img { width: 100%; aspect-ratio: 4/3; object-fit: cover; background: var(--bg-2); border-radius: 4px; }
.plus { position: absolute; right: -11px; top: 50%; transform: translateY(-50%); z-index: 1; width: 18px; height: 18px; border-radius: 50%; background: #111; color: #fff; display: grid; place-items: center; font-weight: 700; font-size: .8rem; }
.more { font-weight: 700; color: var(--muted); padding: 0 4px; }
footer { display: flex; align-items: flex-end; justify-content: space-between; gap: 10px; margin-top: auto; }
.pr { display: flex; flex-direction: column; min-width: 0; }
.pr s { font-size: .8rem; }
.pr b { font-size: 1.15rem; }
.pr em { font-style: normal; font-size: .76rem; color: var(--success); font-weight: 600; }
.btn-group { display: flex; gap: 6px; align-items: center; }
.detail-btn { font-size: .78rem; padding: 4px 8px; }
.pick { list-style: none; display: flex; flex-direction: column; gap: 6px; border-top: 1px solid var(--border); padding-top: 10px; }
.pick li { display: flex; align-items: center; gap: 8px; font-size: .84rem; }
.pick img { width: 40px; height: 30px; object-fit: cover; flex-shrink: 0; }
.pick .nm { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text); }
.pick .btn { padding: 4px 10px; }

/* MODAL CHI TIẾT COMBO */
.combo-modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.65); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 16px; backdrop-filter: blur(3px); }
.combo-modal-card { width: 100%; max-width: 540px; max-height: 90vh; overflow-y: auto; background: var(--surface); padding: 24px; border-radius: 14px; box-shadow: 0 20px 40px rgba(0,0,0,.3); display: flex; flex-direction: column; gap: 16px; }
.cm-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; border-bottom: 1px solid var(--border); padding-bottom: 14px; }
.cm-head h2 { font-size: 1.4rem; margin: 8px 0 4px; }
.close-btn { background: none; border: none; font-size: 1.3rem; cursor: pointer; color: var(--muted); padding: 4px 8px; border-radius: 6px; line-height: 1; }
.close-btn:hover { color: var(--text); background: var(--bg-2); }
.cm-prods { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.cm-prod-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; background: var(--bg-2); border-radius: 10px; }
.cm-prod-item img { width: 64px; height: 48px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.cm-pinfo { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.cm-pname { font-weight: 600; font-size: .92rem; color: var(--text); text-decoration: none; }
.cm-pname:hover { color: var(--accent); }
.cm-pmeta { display: flex; align-items: center; gap: 10px; font-size: .84rem; }
.prod-price { font-weight: 700; }
.stock-pill { font-size: .72rem; padding: 1px 7px; border-radius: 4px; }
.stock-pill.in { background: rgba(34, 197, 94, .15); color: var(--success); font-weight: 600; }
.stock-pill.out { background: rgba(239, 68, 68, .15); color: var(--danger); font-weight: 600; }
.cm-calc-box { background: var(--bg-2); border: 1px dashed var(--accent); border-radius: 10px; padding: 14px; display: flex; flex-direction: column; gap: 8px; }
.cm-row { display: flex; justify-content: space-between; font-size: .9rem; }
.cm-row.ok { color: var(--success); font-weight: 700; }
.cm-row.total-row { border-top: 1px solid var(--border); padding-top: 8px; font-weight: 700; font-size: 1.05rem; }
.cm-footer { display: flex; justify-content: flex-end; gap: 10px; border-top: 1px solid var(--border); padding-top: 14px; }

@container (max-width: 260px) { footer { flex-direction: column; align-items: stretch; } footer .btn-group { width: 100%; } footer .btn { flex: 1; } }
</style>
