// Giao diện: 'v2' (racing poster 2.0, mặc định) hoặc 'v1' (wireframe đồng). Lưu localStorage, gắn vào <html data-theme>.
import { ref, watch } from 'vue'
const saved = typeof localStorage !== 'undefined' ? localStorage.getItem('theme') : null
export const theme = ref(saved === 'v1' || saved === 'v2' ? saved : 'v2')
const apply = v => { document.documentElement.dataset.theme = v }
apply(theme.value)
watch(theme, v => { localStorage.setItem('theme', v); apply(v) })
export const toggleTheme = () => { theme.value = theme.value === 'v2' ? 'v1' : 'v2' }

// ===== Hiển thị & trợ năng: sáng/tối, cỡ chữ, tương phản cao (lưu localStorage, gắn vào <html>) =====
const ls = (k, d) => { try { return localStorage.getItem(k) ?? d } catch { return d } }
export const mode = ref(ls('mode', 'light'))         // 'light' | 'dark'
export const fontScale = ref(+ls('fontScale', 1))                          // 0.9 | 1 | 1.125 | 1.25
export const contrast = ref(ls('contrast', '0') === '1')
const applyA11y = () => {
  const h = document.documentElement
  h.dataset.mode = mode.value; h.style.setProperty('--fs', fontScale.value); h.dataset.contrast = contrast.value ? 'high' : 'normal'
  h.style.colorScheme = mode.value
}
applyA11y()
watch([mode, fontScale, contrast], () => { localStorage.setItem('mode', mode.value); localStorage.setItem('fontScale', fontScale.value); localStorage.setItem('contrast', contrast.value ? '1' : '0'); applyA11y() })
export const FONT_STEPS = [0.9, 1, 1.125, 1.25]
