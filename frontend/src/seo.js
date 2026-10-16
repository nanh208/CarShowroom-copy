// Thẻ meta/OG/canonical/JSON-LD phía client. Trang động (/products/:id, /blog/:slug, /) lấy meta từ server (/api/public/seo/meta) —
// cùng nguồn với bản render cho bot (/api/public/seo/render), nên tiêu đề & structured data luôn khớp.
import api from './api'
const DEF = 'ARES Toys: xe mô hình die-cast, xe RC, xe trưng bày và phụ kiện RC chính hãng.'
function tag(sel, make) { let el = document.head.querySelector(sel); if (!el) { el = make(); document.head.appendChild(el) } return el }
function meta(attr, key, val) { const el = tag(`meta[${attr}="${key}"]`, () => { const m = document.createElement('meta'); m.setAttribute(attr, key); return m }); el.setAttribute('content', val || '') }
export function setSeo({ title, description, image, canonical, type = 'website', jsonLd = [], robots }) {
  if (title) document.title = title
  const d = description || DEF, url = canonical || location.origin + location.pathname, img = image || location.origin + '/icons/icon-512.png'
  meta('name', 'description', d); meta('name', 'robots', robots || 'index,follow')
  meta('property', 'og:title', title || document.title); meta('property', 'og:description', d); meta('property', 'og:image', img); meta('property', 'og:url', url); meta('property', 'og:type', type); meta('property', 'og:site_name', 'ARES Toys')
  meta('name', 'twitter:card', 'summary_large_image')
  tag('link[rel="canonical"]', () => { const l = document.createElement('link'); l.rel = 'canonical'; return l }).href = url
  document.head.querySelectorAll('script[data-seo]').forEach(s => s.remove())
  for (const o of jsonLd) { const s = document.createElement('script'); s.type = 'application/ld+json'; s.dataset.seo = '1'; s.textContent = JSON.stringify(o); document.head.appendChild(s) }
}
const DYN = [/^\/$/, /^\/products\/\d+$/, /^\/blog\/[a-z0-9-]+$/]
let seq = 0
export async function routeSeo(to, fallbackTitle) {
  const my = ++seq, priv = /^\/(admin|account|checkout|cart|orders|pay|oauth-callback|login|register)/.test(to.path)
  setSeo({ title: fallbackTitle, robots: priv ? 'noindex,nofollow' : undefined })
  if (priv || !DYN.some(r => r.test(to.path))) return
  try { const { data } = await api.get('/public/seo/meta', { params: { path: to.path } }); if (my === seq) setSeo({ ...data, jsonLd: data.jsonLd }) } catch { /* giữ mặc định */ }
}
