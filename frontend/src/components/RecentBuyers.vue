<template>
  <transition name="rb">
    <div v-if="cur" class="rb" role="status" aria-live="polite">
      <router-link :to="`/products/${cur.productId}`" class="in" @click="hide">
        <img :src="cur.image" alt="" /><div><small><b>{{ cur.who }}</b> ở {{ cur.city }}</small><span class="nm">vừa mua {{ cur.name }}</span><small class="muted">{{ ago(cur.at) }}</small></div>
      </router-link>
      <button class="x" aria-label="Ẩn thông báo mua hàng" @click="stop">×</button>
    </div>
  </transition>
</template>
<script setup>
// "Vừa có người mua" — xoay vòng 12 đơn gần nhất (ẩn danh), tối đa 5 lần/phiên, tắt được
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { ago } from '../format'
const route = useRoute(), cur = ref(null); let list = [], i = 0, shown = 0, t1, t2
const off = () => sessionStorage.getItem('rbOff') === '1' || /^\/(checkout|pay|login|register|admin)/.test(route.path)
const hide = () => { cur.value = null }
const stop = () => { sessionStorage.setItem('rbOff', '1'); hide() }
function tick() {
  if (off() || document.hidden || !list.length || shown >= 5) return
  cur.value = list[i++ % list.length]; shown++
  clearTimeout(t2); t2 = setTimeout(hide, 6000)
}
watch(() => route.path, () => { if (off()) hide() })
onMounted(async () => { try { list = (await api.get('/public/recent-purchases')).data } catch {} ; setTimeout(tick, 9000); t1 = setInterval(tick, 28000) })
onUnmounted(() => { clearInterval(t1); clearTimeout(t2) })
</script>
<style scoped>
.rb { position: fixed; left: 16px; bottom: 16px; z-index: 105; display: flex; align-items: flex-start; max-width: min(340px, calc(100vw - 92px)); background: var(--surface); border: 1px solid var(--border); border-left: 3px solid var(--accent); box-shadow: 0 14px 32px -10px rgba(0,0,0,.3); }
.in { display: flex; gap: 10px; padding: 10px 6px 10px 10px; color: var(--text); min-width: 0; }
.in img { width: 52px; height: 40px; object-fit: cover; flex-shrink: 0; }
.in div { display: flex; flex-direction: column; min-width: 0; font-size: .8rem; } .nm { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.x { background: none; border: none; color: var(--muted); cursor: pointer; font-size: 1.1rem; width: 28px; height: 28px; flex-shrink: 0; }
.rb-enter-active, .rb-leave-active { transition: transform .22s ease-out, opacity .22s ease-out; } .rb-enter-from, .rb-leave-to { transform: translateY(12px); opacity: 0; }
@media (max-width: 899px) { .rb { bottom: calc(var(--tabbar-h, 60px) + 10px); left: 10px; } }
@media (prefers-reduced-motion: reduce) { .rb-enter-active, .rb-leave-active { transition: opacity .1s; } .rb-enter-from, .rb-leave-to { transform: none; } }
</style>
