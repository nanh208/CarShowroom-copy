/* ARES Toys service worker — PWA: app shell offline, cache ảnh, catalog API network-first. */
const V = 'ares-v13'
const SHELL = ['/', '/offline.html', '/icons/icon-192.png', '/icons/icon-512.png', '/images/logo.png']
const API_CACHE = /^\/api\/(products(\/\d+)?$|products\?|brands|categories|options|banners|public\/(home-layout|collections|appearance|i18n))/
self.addEventListener('install', e => { e.waitUntil(caches.open(V).then(c => c.addAll(SHELL)).then(() => self.skipWaiting())) })
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => !k.startsWith(V)).map(k => caches.delete(k)))).then(() => self.clients.claim())) })
const trim = async (name, max) => { const c = await caches.open(name), ks = await c.keys(); for (let i = 0; i < ks.length - max; i++) await c.delete(ks[i]) }
async function networkFirst(req, name) {
  try { const r = await fetch(req); if (r.ok) { const c = await caches.open(name); c.put(req, r.clone()); trim(name, 120) } return r }
  catch { const hit = await caches.match(req); if (hit) return hit; throw new Error('offline') }
}
async function staleWhileRevalidate(req, name) {
  const c = await caches.open(name), hit = await c.match(req)
  const net = fetch(req).then(r => { if (r.ok) { c.put(req, r.clone()); trim(name, 300) } return r }).catch(() => hit)
  return hit || net
}
self.addEventListener('fetch', e => {
  const req = e.request; if (req.method !== 'GET') return
  const u = new URL(req.url); if (u.origin !== location.origin) return
  if (u.pathname.startsWith('/ws')) return
  if (req.mode === 'navigate') { e.respondWith(fetch(req).then(r => { caches.open(V).then(c => c.put('/', r.clone())); return r }).catch(async () => (await caches.match('/')) || caches.match('/offline.html'))); return }
  if (u.pathname.startsWith('/assets/')) { e.respondWith(caches.match(req).then(h => h || fetch(req).then(r => { const cl = r.clone(); caches.open(V + '-assets').then(c => c.put(req, cl)); return r }))); return }
  if (u.pathname.startsWith('/images/') || u.pathname.startsWith('/uploads/') || u.pathname.startsWith('/icons/')) { e.respondWith(staleWhileRevalidate(req, V + '-img')); return }
  if (API_CACHE.test(u.pathname + u.search)) { e.respondWith(networkFirst(req, V + '-api')); return }
})
