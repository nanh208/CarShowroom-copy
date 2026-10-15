<template>
  <div v-if="show" class="sitebg" aria-hidden="true" :style="{ position: bg.bgAttach === 'fixed' ? 'fixed' : 'absolute', height: bg.bgAttach === 'fixed' ? '100vh' : docH + 'px' }">
    <div class="img" :style="imgStyle" />
    <div class="ov" :style="{ opacity: bg.bgOverlay }" />
  </div>
</template>
<script setup>
// Ảnh nền toàn site: lớp ảnh + lớp phủ (màu nền theo chế độ sáng/tối) để chữ luôn đọc được.
import { computed, watch, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { bg } from '../stores/theme'
const route = useRoute(), docH = ref(0)
const show = computed(() => !!bg.bgImage && !route.path.startsWith('/admin') && !route.meta.bare && (bg.bgPages === 'all' || route.path === '/'))
const imgStyle = computed(() => ({
  backgroundImage: `url("${bg.bgImage.replace(/"/g, '%22')}")`,
  backgroundSize: bg.bgFit === 'repeat' ? 'auto' : bg.bgFit, backgroundRepeat: bg.bgFit === 'repeat' ? 'repeat' : 'no-repeat', backgroundPosition: 'center',
  filter: bg.bgBlur ? `blur(${bg.bgBlur}px)` : 'none', inset: bg.bgBlur ? `-${bg.bgBlur * 2}px` : '0'
}))
let ro
const measure = () => { docH.value = document.documentElement.scrollHeight }
onMounted(() => { measure(); ro = new ResizeObserver(measure); ro.observe(document.body) })
onUnmounted(() => ro?.disconnect())
watch(show, v => document.documentElement.classList.toggle('has-sitebg', v), { immediate: true })
</script>
<style scoped>
.sitebg { left: 0; top: 0; width: 100%; z-index: -1; overflow: hidden; pointer-events: none; }
.img { position: absolute; }
.ov { position: absolute; inset: 0; background: var(--bg, #f4efe6); }
</style>
