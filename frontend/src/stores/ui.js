// Trạng thái giao diện dùng chung: menu off-canvas trên mobile, bảng lọc dạng sheet
import { ref } from 'vue'
export const navOpen = ref(false)
export const isMobile = ref(typeof matchMedia !== 'undefined' && matchMedia('(max-width: 899px)').matches)
if (typeof matchMedia !== 'undefined') matchMedia('(max-width: 899px)').addEventListener('change', e => { isMobile.value = e.matches; if (!e.matches) navOpen.value = false })
export const cartOpen = ref(false)
