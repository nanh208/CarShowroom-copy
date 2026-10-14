<template>
  <button :class="['btn bn', state]" :disabled="disabled || state !== 'idle'" :aria-busy="state === 'spin'" @click="go">
    <span class="lbl"><slot>Mua ngay</slot></span>
    <svg v-if="state === 'spin'" class="wheel" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10" /><circle cx="12" cy="12" r="3.2" /><path d="M12 2v6.8M12 15.2V22M2 12h6.8M15.2 12H22M4.9 4.9l4.8 4.8M14.3 14.3l4.8 4.8M19.1 4.9l-4.8 4.8M9.7 14.3l-4.8 4.8" /></svg>
    <svg v-if="state === 'done'" class="ok" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12.5 4.5 4.5L19 7.5" /></svg>
    <span class="sr" aria-live="polite">{{ state === 'spin' ? 'Đang xử lý' : state === 'done' ? 'Xong' : '' }}</span>
  </button>
</template>
<script setup>
// "Mua ngay": nút co lại thành bánh xe quay → dấu tick → chuyển trang. Gọi before() (trả false để huỷ) rồi after().
import { ref } from 'vue'
import { reduced } from '../motion'
const props = defineProps({ disabled: Boolean, before: { type: Function, default: () => true }, after: { type: Function, required: true } })
const state = ref('idle')
const wait = ms => new Promise(r => setTimeout(r, ms))
async function go() {
  if (props.before() === false) return
  if (reduced()) return props.after()
  state.value = 'spin'; await wait(420)
  state.value = 'done'; await wait(260)
  await props.after(); state.value = 'idle'
}
</script>
<style scoped>
.bn { position: relative; overflow: hidden; transition: width .2s ease-out, padding .2s ease-out, border-radius .2s ease-out, background-color .15s ease-out; }
.bn.spin, .bn.done { color: transparent !important; }
.bn.spin:disabled, .bn.done:disabled { opacity: 1; cursor: progress; }
.bn .lbl { transition: opacity .12s ease-out; } .bn.spin .lbl, .bn.done .lbl { opacity: 0; }
.wheel, .ok { position: absolute; inset: 0; margin: auto; width: 26px; height: 26px; fill: none; stroke: #fff; stroke-width: 2; }
.wheel { animation: bn-spin .42s linear infinite; } .wheel circle:first-child { stroke-width: 3; stroke: #111; fill: #222; } .wheel circle:nth-child(2) { fill: #ddd; stroke: #ddd; } .wheel path { stroke: #bbb; stroke-width: 1.4; }
.ok { stroke-width: 3; stroke-dasharray: 24; stroke-dashoffset: 24; animation: bn-draw .24s ease-out forwards; }
.bn.done { background: var(--success, #2e7d32) !important; }
.sr { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
@keyframes bn-spin { to { transform: rotate(360deg); } }
@keyframes bn-draw { to { stroke-dashoffset: 0; } }
</style>
