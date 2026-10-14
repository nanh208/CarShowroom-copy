// Giao diện: 'v2' (racing poster 2.0, mặc định) hoặc 'v1' (wireframe đồng). Lưu localStorage, gắn vào <html data-theme>.
import { ref, watch } from 'vue'
const saved = typeof localStorage !== 'undefined' ? localStorage.getItem('theme') : null
export const theme = ref(saved === 'v1' || saved === 'v2' ? saved : 'v2')
const apply = v => { document.documentElement.dataset.theme = v }
apply(theme.value)
watch(theme, v => { localStorage.setItem('theme', v); apply(v) })
export const toggleTheme = () => { theme.value = theme.value === 'v2' ? 'v1' : 'v2' }
