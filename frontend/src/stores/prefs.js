// Yêu thích, so sánh (tối đa 3), xem gần đây. Khách vãng lai: localStorage; đã đăng nhập: đồng bộ với backend (/api/account/prefs, /api/account/viewed).
import { defineStore } from 'pinia'
const authed = () => { try { return !!localStorage.getItem('token') } catch { return false } }
const load = (k, d = []) => { try { return JSON.parse(localStorage.getItem(k)) || d } catch { return d } }
const slim = p => ({ id: p.id, name: p.name, price: p.price, oldPrice: p.oldPrice, images: p.images, type: p.type, grp: p.grp, scale: p.scale, ageMin: p.ageMin, rating: p.rating, reviewCount: p.reviewCount, hasVariants: p.hasVariants, warrantyMonths: p.warrantyMonths, material: p.material,
  stock: p.stock, sold: p.sold, isNew: p.isNew, baseColor: p.baseColor, brand: p.brand && { id: p.brand.id, name: p.brand.name, logoUrl: p.brand.logoUrl }, specs: p.specs })

export const usePrefs = defineStore('prefs', {
  state: () => ({ wish: load('ares_wish'), compare: load('ares_compare'), recent: load('ares_recent') }),
  actions: {
    save(which) {
      localStorage.setItem('ares_wish', JSON.stringify(this.wish)); localStorage.setItem('ares_compare', JSON.stringify(this.compare)); localStorage.setItem('ares_recent', JSON.stringify(this.recent))
      if (!authed() || !which) return
      const ids = (which === 'wishlist' ? this.wish : this.compare).map(x => x.id)
      api.put('/account/prefs/' + which, ids).catch(() => {})
    },
    /** Sau đăng nhập: gộp dữ liệu máy này với server rồi dùng server làm nguồn chuẩn. */
    async syncFromServer() {
      if (!authed()) return
      try {
        const { data } = await api.get('/account/prefs')
        const merge = async (key, local, max) => {
          const ids = [...new Set([...(data[key] || []), ...local.map(x => x.id)])].slice(0, max)
          if (!ids.length) return []
          const list = (await api.get('/public/wishlist', { params: { ids: ids.join(',') } })).data
          return ids.map(id => list.find(p => p.id === id)).filter(Boolean).map(slim)
        }
        this.wish = await merge('wishlist', this.wish, 50)
        const cmp = await merge('compare', this.compare, 3); this.compare = cmp.filter(p => p.grp === cmp[0]?.grp)
        const local = this.recent.map(x => x.id)
        for (const id of local.slice(0, 8).reverse()) await api.post('/account/viewed/' + id).catch(() => {})
        this.recent = (await api.get('/account/viewed')).data.slice(0, 8).map(slim)
        this.save('wishlist'); this.save('compare')
      } catch { /* giữ dữ liệu máy này */ }
    },
    isWished(id) { return this.wish.some(x => x.id === id) },
    toggleWish(p) {
      const on = this.isWished(p.id)
      this.wish = on ? this.wish.filter(x => x.id !== p.id) : [slim(p), ...this.wish]; this.save('wishlist')
      toast(on ? 'Đã bỏ khỏi danh sách yêu thích' : 'Đã thêm vào danh sách yêu thích')
    },
    inCompare(id) { return this.compare.some(x => x.id === id) },
    toggleCompare(p) {
      if (this.inCompare(p.id)) { this.compare = this.compare.filter(x => x.id !== p.id); this.save('compare'); return }
      if (this.compare.length && this.compare[0].grp !== p.grp) return toast('Chỉ so sánh các sản phẩm cùng nhóm (xe hoặc phụ kiện)', 'error')
      if (this.compare.length >= 3) return toast('Chỉ so sánh tối đa 3 sản phẩm', 'error')
      this.compare = [...this.compare, slim(p)]; this.save('compare'); toast(`Đã thêm vào so sánh (${this.compare.length}/3)`)
    },
    clearCompare() { this.compare = []; this.save('compare') },
    viewed(p) { this.recent = [slim(p), ...this.recent.filter(x => x.id !== p.id)].slice(0, 8); this.save(); if (authed()) api.post('/account/viewed/' + p.id).catch(() => {}) }
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
