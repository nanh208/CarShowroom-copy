import { defineStore } from 'pinia'

const load = (k) => { try { return JSON.parse(localStorage.getItem(k) || '[]') } catch { return [] } }

// Dòng giỏ: { key, productId, variantId, variantName, name, image, unitPrice, oldPrice, quantity, stock, flash }
export const useCart = defineStore('cart', {
  state: () => ({ items: load('cart'), saved: load('cart_saved'), fly: 0 }),
  getters: {
    count: s => s.items.reduce((a, i) => a + i.quantity, 0),
    subtotal: s => s.items.reduce((a, i) => a + i.unitPrice * i.quantity, 0)
  },
  actions: {
    save() { localStorage.setItem('cart', JSON.stringify(this.items)); localStorage.setItem('cart_saved', JSON.stringify(this.saved)) },
    add(line) {
      const key = line.productId + '|' + (line.variantId || '')
      const ex = this.items.find(i => i.key === key)
      if (ex) ex.quantity = Math.min(ex.quantity + line.quantity, line.stock || 99)
      else this.items.push({ ...line, key })
      this.fly++
      this.save()
    },
    setQty(key, q) {
      const i = this.items.find(x => x.key === key)
      if (i) { i.quantity = Math.max(1, Math.min(q, i.stock || 99)); this.save() }
    },
    remove(key) { this.items = this.items.filter(i => i.key !== key); this.save() },
    saveForLater(key) {
      const i = this.items.find(x => x.key === key); if (!i) return
      this.items = this.items.filter(x => x.key !== key)
      this.saved = [i, ...this.saved.filter(x => x.key !== key)]; this.save()
    },
    moveToCart(key) {
      const i = this.saved.find(x => x.key === key); if (!i) return
      this.saved = this.saved.filter(x => x.key !== key); this.add({ ...i, quantity: 1 })
    },
    removeSaved(key) { this.saved = this.saved.filter(x => x.key !== key); this.save() },
    clear() { this.items = []; this.save() }
  }
})
