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

/** Ảnh sản phẩm "lái" theo đường cong vào icon giỏ hàng, để lại vệt bánh xe; ~520ms, tắt khi reduced-motion. */
const visible = el => { if (!el) return false; const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0 && getComputedStyle(el).visibility !== 'hidden' }
export function cartTarget() {
  const m = document.getElementById('cart-target-m'), d = document.getElementById('cart-target')
  return visible(m) ? m : visible(d) ? d : null
}
export function flyToCart(src, from) {
  const target = cartTarget()
  if (!target || reduced() || !src) return
  const a = from?.getBoundingClientRect?.() || { left: innerWidth / 2 - 40, top: innerHeight / 2 - 30, width: 80, height: 60 }
  const b = target.getBoundingClientRect()
  const x0 = a.left + a.width / 2, y0 = a.top + a.height / 2, x1 = b.left + b.width / 2, y1 = b.top + b.height / 2
  // điểm điều khiển: vồng lên trên (desktop) hoặc sang ngang (giỏ ở dưới, mobile)
  const cx = (x0 + x1) / 2 + (y1 > y0 ? (x1 > x0 ? -1 : 1) * 80 : 0), cy = Math.min(y0, y1) - (y1 > y0 ? 20 : 120)
  const pt = t => [(1 - t) ** 2 * x0 + 2 * (1 - t) * t * cx + t * t * x1, (1 - t) ** 2 * y0 + 2 * (1 - t) * t * cy + t * t * y1]
  const size = 72, D = 520, N = 12
  const img = document.createElement('img')
  img.src = src; img.className = 'fly-img'; img.alt = ''
  Object.assign(img.style, { left: x0 - size / 2 + 'px', top: y0 - size * 0.35 + 'px', width: size + 'px', height: size * 0.7 + 'px' })
  document.body.appendChild(img)
  const frames = []
  for (let k = 0; k <= N; k++) { const t = k / N, [x, y] = pt(t); frames.push({ transform: `translate3d(${x - x0}px, ${y - y0}px, 0) scale(${1 - 0.78 * t})`, opacity: 1 - 0.5 * t, offset: t }) }
  img.animate(frames, { duration: D, easing: 'cubic-bezier(.3,.6,.4,1)' }).onfinish = () => img.remove()
  // vệt bánh xe: các cặp gạch nhỏ dọc quỹ đạo, xuất hiện theo xe rồi mờ dần
  for (let k = 1; k < N; k++) {
    const t = k / N, [x, y] = pt(t), [x2, y2] = pt(Math.min(1, t + 0.02))
    const ang = Math.atan2(y2 - y, x2 - x) * 180 / Math.PI
    const tr = document.createElement('i'); tr.className = 'fly-trail'
    Object.assign(tr.style, { left: x + 'px', top: y + 'px', transform: `translate(-50%,-50%) rotate(${ang}deg)` })
    document.body.appendChild(tr)
    tr.animate([{ opacity: 0 }, { opacity: .55, offset: .25 }, { opacity: 0 }], { duration: 380, delay: D * t * 0.85, easing: 'ease-out', fill: 'both' }).onfinish = () => tr.remove()
  }
}

/** Pháo giấy cờ caro (thành công đặt hàng). */
export function checkeredConfetti(host, n = 46) {
  if (reduced() || !host) return
  const r = host.getBoundingClientRect()
  for (let k = 0; k < n; k++) {
    const c = document.createElement('i'); c.className = 'confetti' + (k % 3 === 0 ? ' or' : k % 3 === 1 ? ' ck' : '')
    const x = r.left + r.width / 2, y = r.top + r.height * 0.3
    Object.assign(c.style, { left: x + 'px', top: y + 'px' })
    document.body.appendChild(c)
    const ang = Math.random() * Math.PI * 2, sp = 120 + Math.random() * 220
    const dx = Math.cos(ang) * sp, dy = Math.sin(ang) * sp - 120
    c.animate([{ transform: 'translate(-50%,-50%) rotate(0)', opacity: 1 },
      { transform: `translate(calc(-50% + ${dx}px), calc(-50% + ${dy + 260}px)) rotate(${Math.random() * 720 - 360}deg)`, opacity: 0 }],
      { duration: 900 + Math.random() * 500, easing: 'cubic-bezier(.2,.7,.4,1)', fill: 'forwards' }).onfinish = () => c.remove()
  }
}

/** v-stagger: các phần tử con hiện lần lượt (40ms/phần tử, tối đa 10 bậc) khi khối vào màn hình; cập nhật khi danh sách đổi. */
const tagKids = el => { [...el.children].forEach((c, i) => { if (!c.style.getPropertyValue('--i')) c.style.setProperty('--i', Math.min(i, 10)) }) }
export const vStagger = {
  mounted(el) {
    if (reduced()) return
    el.classList.add('stg'); tagKids(el); observer().observe(el)
    el._stT = setTimeout(() => { el.classList.add('in'); io?.unobserve(el) }, 700)
  },
  updated(el) { if (!reduced()) tagKids(el) },
  unmounted(el) { clearTimeout(el._stT); io?.unobserve(el) }
}

/** Gợn sóng khi bấm (chỉ phản hồi thao tác bấm/chạm, không theo chuột). */
function ripple(e) {
  const b = e.target.closest?.('.btn, .chip, .ripple'); if (!b || reduced() || b.disabled) return
  const r = b.getBoundingClientRect(), s = Math.max(r.width, r.height) * 2
  const i = document.createElement('span'); i.className = 'rpl'
  Object.assign(i.style, { width: s + 'px', height: s + 'px', left: e.clientX - r.left - s / 2 + 'px', top: e.clientY - r.top - s / 2 + 'px' })
  if (getComputedStyle(b).position === 'static') b.style.position = 'relative'
  b.style.overflow = 'hidden'; b.appendChild(i); setTimeout(() => i.remove(), 500)
}

export default { install(app) { app.directive('reveal', vReveal); app.directive('stagger', vStagger); if (typeof document !== 'undefined') document.addEventListener('pointerdown', ripple, { passive: true }) } }
