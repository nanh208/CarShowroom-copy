// Yêu thích, so sánh (tối đa 3), xem gần đây — lưu localStorage
import { defineStore } from 'pinia'
const load = (k, d = []) => { try { return JSON.parse(localStorage.getItem(k)) || d } catch { return d } }
const slim = p => ({ id: p.id, name: p.name, price: p.price, oldPrice: p.oldPrice, images: p.images, type: p.type, fuel: p.fuel, year: p.year,
  stock: p.stock, sold: p.sold, isNew: p.isNew, baseColor: p.baseColor, brand: p.brand && { id: p.brand.id, name: p.brand.name, logoUrl: p.brand.logoUrl }, specs: p.specs })

export const usePrefs = defineStore('prefs', {
  state: () => ({ wish: load('ares_wish'), compare: load('ares_compare'), recent: load('ares_recent') }),
  actions: {
    save() { localStorage.setItem('ares_wish', JSON.stringify(this.wish)); localStorage.setItem('ares_compare', JSON.stringify(this.compare)); localStorage.setItem('ares_recent', JSON.stringify(this.recent)) },
    isWished(id) { return this.wish.some(x => x.id === id) },
    toggleWish(p) {
      const on = this.isWished(p.id)
      this.wish = on ? this.wish.filter(x => x.id !== p.id) : [slim(p), ...this.wish]; this.save()
      toast(on ? 'Đã bỏ khỏi danh sách yêu thích' : 'Đã thêm vào danh sách yêu thích')
    },
    inCompare(id) { return this.compare.some(x => x.id === id) },
    toggleCompare(p) {
      if (this.inCompare(p.id)) { this.compare = this.compare.filter(x => x.id !== p.id); this.save(); return }
      if (p.type === 'ACCESSORY') return toast('Chỉ so sánh được xe, không gồm phụ kiện', 'error')
      if (this.compare.length >= 3) return toast('Chỉ so sánh tối đa 3 xe', 'error')
      this.compare = [...this.compare, slim(p)]; this.save(); toast(`Đã thêm vào so sánh (${this.compare.length}/3)`)
    },
    clearCompare() { this.compare = []; this.save() },
    viewed(p) { this.recent = [slim(p), ...this.recent.filter(x => x.id !== p.id)].slice(0, 8); this.save() }
  }
})

// ===== Toast =====
import { reactive } from 'vue'
export const toasts = reactive([])
let seq = 0
export function toast(text, kind = 'success', ms = 2600) {
  const id = ++seq; toasts.push({ id, text, kind })
  setTimeout(() => { const i = toasts.findIndex(t => t.id === id); if (i >= 0) toasts.splice(i, 1) }, ms)
}

// ===== Điểm đánh giá trung bình (tải 1 lần) =====
import api from '../api'
export const ratings = reactive({})
let ratingsLoaded = false
export function loadRatings(force = false) {
  if (ratingsLoaded && !force) return; ratingsLoaded = true
  api.get('/products/ratings').then(r => Object.assign(ratings, r.data)).catch(() => { ratingsLoaded = false })
}
