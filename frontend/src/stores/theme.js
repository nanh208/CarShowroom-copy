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

// Mật độ hiển thị: 'comfy' (mặc định) | 'compact' (giảm khoảng cách, thẻ nhỏ hơn)
export const density = ref(ls('density', 'comfy'))
const applyDensity = () => { document.documentElement.dataset.density = density.value }
applyDensity()
watch(density, v => { localStorage.setItem('density', v); applyDensity() })

// ===== Giao diện toàn site do admin cấu hình (backend /api/public/appearance) =====
// Giá trị localStorage chỉ là bộ nhớ đệm để tránh nháy khi tải; nguồn chuẩn là backend.
import { reactive } from 'vue'
export const BG_DEFAULT = { bgImage: '', bgOverlay: 0.78, bgBlur: 0, bgFit: 'cover', bgAttach: 'fixed', bgPages: 'all' }
export const bg = reactive({ ...BG_DEFAULT, ...(() => { try { return JSON.parse(ls('bgCache', '{}')) } catch { return {} } })() })
watch(bg, v => localStorage.setItem('bgCache', JSON.stringify(v)))
export function applyAppearance(a) {
  if (!a) return
  if (a.theme) theme.value = a.theme
  if (a.mode) mode.value = a.mode
  if (a.fontScale) fontScale.value = +a.fontScale
  contrast.value = !!a.contrast
  if (a.density) density.value = a.density
  Object.keys(BG_DEFAULT).forEach(k => { if (a[k] !== undefined) bg[k] = a[k] })
}
export const snapshot = () => ({ theme: theme.value, mode: mode.value, fontScale: fontScale.value, contrast: contrast.value, density: density.value, ...bg })
export async function loadAppearance() {
  try { const r = await fetch('/api/public/appearance'); if (r.ok) applyAppearance(await r.json()) } catch { /* giữ bộ nhớ đệm */ }
}
