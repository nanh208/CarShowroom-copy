import { test, expect } from '@playwright/test'
import { trackErrors, noHorizontalOverflow, firstProduct, API } from './helpers'

test('trang chủ tải, có SEO meta + JSON-LD, không lỗi JS, không tràn ngang', async ({ page }) => {
  const errs = trackErrors(page)
  await page.goto('/')
  await expect(page).toHaveTitle(/ARES Toys/)
  await expect(page.locator('#app header, #app .hdr, #app nav').first()).toBeVisible()
  await expect(page.locator('meta[name="description"]')).toHaveAttribute('content', /.{20,}/)
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href', /\/$/)
  await expect.poll(() => page.locator('script[type="application/ld+json"]').count()).toBeGreaterThan(0)
  await page.waitForLoadState('networkidle')
  await noHorizontalOverflow(page)
  expect(errs).toEqual([])
})

test('danh sách → chi tiết sản phẩm → thêm vào giỏ', async ({ page, request }) => {
  const p = await firstProduct(request)
  await page.goto('/products')
  await expect(page.locator(`a[href="/products/${p.id}"]`).first()).toBeVisible()
  await page.goto(`/products/${p.id}`)
  await expect(page.getByRole('heading', { level: 1 })).toContainText(p.name.slice(0, 12))
  await expect.poll(async () => (await page.locator('script[type="application/ld+json"]').allTextContents()).join(' ')).toContain('"Product"')
  await expect(page.locator('meta[property="og:title"]')).toHaveAttribute('content', new RegExp(p.name.slice(0, 10).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  await page.getByRole('button', { name: 'Thêm vào giỏ' }).first().click()
  await expect.poll(() => page.evaluate(() => JSON.stringify(localStorage))).toContain(String(p.id))
  await noHorizontalOverflow(page)
})

test('đổi ngôn ngữ vi ↔ en', async ({ page, isMobile }) => {
  test.skip(isMobile, 'nút ngôn ngữ nằm trong menu trên mobile')
  await page.goto('/')
  await page.getByRole('button', { name: 'EN', exact: true }).first().click()
  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
  await expect(page.getByRole('link', { name: 'News', exact: true }).first()).toBeVisible()
  await page.getByRole('button', { name: 'VI', exact: true }).first().click()
  await expect(page.getByRole('link', { name: 'Tin tức', exact: true }).first()).toBeVisible()
})

test('tin tức: danh sách và bài viết', async ({ page, request }) => {
  const d = await (await request.get(API + '/api/public/posts')).json()
  await page.goto('/blog')
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  const post = d.content[0]
  await page.locator(`a[href="/blog/${post.slug}"]`).first().click()
  await expect(page).toHaveURL(new RegExp(`/blog/${post.slug}$`))
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(post.title)
  await noHorizontalOverflow(page)
})

test('tìm bằng hình ảnh: tải ảnh lên → có kết quả', async ({ page }) => {
  await page.goto('/image-search')
  await page.locator('input[type="file"]').first().setInputFiles('public/images/toys/bburago-chiron-1.jpg')
  await expect(page.locator('a[href^="/products/"]').first()).toBeVisible({ timeout: 15_000 })
})

test('trang 404', async ({ page }) => {
  await page.goto('/khong-ton-tai-xyz')
  await expect(page.locator('body')).toContainText(/404|Không tìm thấy/i)
})
