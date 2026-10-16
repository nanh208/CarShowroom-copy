import { test, expect } from '@playwright/test'
import { API, firstProduct } from './helpers'

test('ảnh tĩnh dùng WebP, ảnh ngoài màn hình lazy-load', async ({ page }) => {
  const types = []
  page.on('response', r => { const u = new URL(r.url()); if (u.pathname.startsWith('/images/toys/')) types.push(u.pathname) })
  await page.goto('/products')
  await page.waitForLoadState('networkidle')
  expect(types.length).toBeGreaterThan(0)
  expect(types.filter(t => /\.(jpe?g|png)$/.test(t)), 'jpg/png requests for images that have WebP').toEqual([])
  const imgs = await page.locator('img').evaluateAll(els => els.map(e => ({ lazy: e.loading, prio: e.getAttribute('fetchpriority'), decoding: e.decoding })))
  expect(imgs.filter(i => i.lazy !== 'lazy' && !i.prio).length).toBe(0)
})

test('code splitting: trang chủ không tải bundle trang quản trị / model-viewer', async ({ page }) => {
  const js = []
  page.on('request', r => { if (r.resourceType() === 'script') js.push(new URL(r.url()).pathname) })
  await page.goto('/'); await page.waitForLoadState('networkidle')
  expect(js.some(p => /vendor-vue/.test(p))).toBeTruthy()
  expect(js.filter(p => /Admin|model-viewer/.test(p))).toEqual([])
})

test('header cache: asset immutable, HTML no-cache', async ({ request, baseURL }) => {
  const html = await request.get(baseURL + '/')
  expect(html.headers()['cache-control']).toContain('no-cache')
  const asset = (await html.text()).match(/\/assets\/[^"]+\.js/)[0]
  const a = await request.get(baseURL + asset)
  expect(a.headers()['cache-control']).toContain('immutable')
})

test('API: ETag + 304, gzip, dữ liệu riêng tư no-store', async ({ request }) => {
  const r = await request.get(API + '/api/products?size=40', { headers: { 'Accept-Encoding': 'gzip' } })
  const etag = r.headers()['etag']; expect(etag).toBeTruthy()
  expect(r.headers()['content-encoding']).toBe('gzip')
  const b = await request.get(API + '/api/brands'); const be = b.headers()['etag']; expect(be).toBeTruthy()
  const again = await request.get(API + '/api/brands', { headers: { 'If-None-Match': be } })
  expect(again.status()).toBe(304)
  const p = await firstProduct(request)
  const priv = await request.get(API + '/api/admin/orders')
  expect(priv.headers()['cache-control']).toContain('no-store')
  expect(p).toBeTruthy()
})
