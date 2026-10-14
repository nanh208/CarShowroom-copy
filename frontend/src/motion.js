// Hiệu ứng chuyển động chủ đề xe: reveal khi cuộn, tilt thẻ, bay vào giỏ. Tôn trọng prefers-reduced-motion.
export const reduced = () => typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

let io
const observer = () => io || (io = new IntersectionObserver(entries => {
  entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target) } })
}, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 }))

/** v-reveal="'left' | 'right' | 'up' | 'zoom'"  · v-reveal:120 (độ trễ ms) */
export const vReveal = {
  mounted(el, b) {
    if (reduced()) return
    el.classList.add('rv', 'rv-' + (b.value || 'up'))
    if (b.arg) el.style.setProperty('--rv-delay', b.arg + 'ms')
    observer().observe(el)
  },
  unmounted(el) { io?.unobserve(el) }
}

/** v-tilt: nghiêng nhẹ theo con trỏ (chỉ thiết bị có chuột). */
export const vTilt = {
  mounted(el) {
    if (reduced() || !window.matchMedia('(hover: hover)').matches) return
    let raf
    el._move = e => {
      cancelAnimationFrame(raf)
      raf = requestAnimationFrame(() => {
        const r = el.getBoundingClientRect()
        const x = (e.clientX - r.left) / r.width - 0.5, y = (e.clientY - r.top) / r.height - 0.5
        el.style.setProperty('--rx', (-y * 6).toFixed(2) + 'deg'); el.style.setProperty('--ry', (x * 8).toFixed(2) + 'deg')
        el.style.setProperty('--mx', ((x + 0.5) * 100).toFixed(1) + '%'); el.style.setProperty('--my', ((y + 0.5) * 100).toFixed(1) + '%')
      })
    }
    el._leave = () => { el.style.setProperty('--rx', '0deg'); el.style.setProperty('--ry', '0deg') }
    el.classList.add('tilt')
    el.addEventListener('mousemove', el._move); el.addEventListener('mouseleave', el._leave)
  },
  unmounted(el) { el.removeEventListener('mousemove', el._move); el.removeEventListener('mouseleave', el._leave) }
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
    { transform: 'translate(0,0) scale(1) rotate(0deg)', opacity: 1 },
    { transform: `translate(${dx * 0.45}px, ${dy * 0.45 - 90}px) scale(.8) rotate(-8deg)`, opacity: 1, offset: 0.5 },
    { transform: `translate(${dx}px, ${dy}px) scale(.18) rotate(0deg)`, opacity: .4 }
  ], { duration: 750, easing: 'cubic-bezier(.5,0,.3,1)' })
  anim.onfinish = () => img.remove()
}

export default { install(app) { app.directive('reveal', vReveal); app.directive('tilt', vTilt) } }
