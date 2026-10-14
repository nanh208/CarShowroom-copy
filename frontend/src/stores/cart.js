import { defineStore } from 'pinia'

const KEY = 'cart'

// Mỗi dòng giỏ hàng: { key, productId, name, scale, image, baseColor, unitPrice, quantity, stock, options:{paint,wheel,interior}, optionLabel }
export const useCart = defineStore('cart', {
  state: () => ({ items: JSON.parse(localStorage.getItem(KEY) || '[]') }),
  getters: {
    count: s => s.items.reduce((a, i) => a + i.quantity, 0),
    subtotal: s => s.items.reduce((a, i) => a + i.unitPrice * i.quantity, 0)
  },
  actions: {
    save() { localStorage.setItem(KEY, JSON.stringify(this.items)) },
    add(line) {
      const key = line.productId + '|' + JSON.stringify(line.options || {})
      const ex = this.items.find(i => i.key === key)
      if (ex) ex.quantity = Math.min(ex.quantity + line.quantity, line.stock)
      else this.items.push({ ...line, key })
      this.save()
    },
    setQty(key, q) {
      const i = this.items.find(x => x.key === key)
      if (i) { i.quantity = Math.max(1, Math.min(q, i.stock)); this.save() }
    },
    remove(key) { this.items = this.items.filter(i => i.key !== key); this.save() },
    clear() { this.items = []; this.save() }
  }
})
