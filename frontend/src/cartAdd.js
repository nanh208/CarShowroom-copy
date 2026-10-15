// Thêm sản phẩm vào giỏ theo id (tự chọn biến thể còn hàng rẻ nhất) — dùng cho combo, mua lại, giỏ đã lưu
import api from './api'
import { useCart } from './stores/cart'
export async function addById(id, qty = 1, variantId = null) {
  const cart = useCart()
  const { data } = await api.get(`/products/${id}`)
  const p = data.product
  if (!p?.active || p.stock <= 0) return false
  if (p.hasVariants) {
    const vs = (data.variants || []).filter(x => x.active !== false && x.stock > 0)
    const v = vs.find(x => x.id === variantId) || vs.sort((a, b) => a.price - b.price)[0]
    if (!v) return false
    cart.add({ productId: p.id, variantId: v.id, variantName: v.name, name: p.name, image: v.image || p.images?.[0], unitPrice: v.price, oldPrice: v.oldPrice, quantity: Math.min(qty, v.stock), stock: v.stock })
  } else {
    const flash = data.flash?.salePrice
    cart.add({ productId: p.id, name: p.name, image: p.images?.[0], unitPrice: flash || p.price, oldPrice: p.oldPrice, quantity: Math.min(qty, p.stock), stock: p.stock, flash: !!flash })
  }
  return true
}
