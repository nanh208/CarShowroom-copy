// Hiệu ứng chuyển động chủ đề xe: reveal khi cuộn (1 lần), bay vào giỏ. Tôn trọng prefers-reduced-motion.
export const reduced = () => typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

let io
const observer = () => io || (io = new IntersectionObserver(entries => {
  entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target) } })
}, { rootMargin: '0px 0px 120px 0px', threshold: 0 }))

/** v-reveal="'left' | 'right' | 'up' | 'zoom'"  · v-reveal:120 (độ trễ ms) */
export const vReveal = {
  mounted(el, b) {
    if (reduced()) return
    // chỉ hiện 1 lần, trễ tối đa 120ms để không chờ lâu
    el.classList.add('reveal')
    if (b.arg) el.style.setProperty('--rv-delay', Math.min(+b.arg, 120) + 'ms')
    observer().observe(el)
    // An toàn: nội dung luôn hiện dù observer không kích hoạt (chụp full-page, in, tab ẩn…)
    el._rvT = setTimeout(() => { el.classList.add('in'); io?.unobserve(el) }, 600)
  },
  unmounted(el) { clearTimeout(el._rvT); io?.unobserve(el) }
}

/** Ảnh sản phẩm "lái" theo đường cong vào icon giỏ hàng. */
export function flyToCart(src, from) {
  const target = document.getElementById('cart-target')
  if (!target || reduced() || !src) return
  const a = from?.getBoundingClientRect?.() || { left: innerWidth / 2 - 40, top: innerHeight / 2 - 30, width: 80, height: 60 }
  const b = target.getBoundingClientRect()
  const img = document.createElement('img')
  img.src = src; img.className = 'fly-img'
  const size = 72
  Object.assign(img.style, { left: a.left + a.width / 2 - size / 2 + 'px', top: a.top + a.height / 2 - size / 2 + 'px', width: size + 'px', height: size * 0.7 + 'px' })
  document.body.appendChild(img)
  const dx = b.left + b.width / 2 - (a.left + a.width / 2), dy = b.top + b.height / 2 - (a.top + a.height / 2)
  const anim = img.animate([
    { transform: 'translate3d(0,0,0) scale(1)', opacity: 1 },
    { transform: `translate3d(${dx}px, ${dy}px, 0) scale(.2)`, opacity: .3 }
  ], { duration: 420, easing: 'cubic-bezier(.2,.7,.3,1)' })
  anim.onfinish = () => img.remove()
}

export default { install(app) { app.directive('reveal', vReveal) } }
