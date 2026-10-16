import { test, expect } from '@playwright/test'
import { loginAs, noHorizontalOverflow, ADMIN, CUSTOMER } from './helpers'

test('đăng nhập bằng form email', async ({ page }) => {
  await page.goto('/login')
  await page.locator('input[type="email"]').fill(ADMIN.email)
  await page.locator('input[type="password"]').fill(ADMIN.password)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await expect.poll(() => page.evaluate(() => !!localStorage.getItem('token'))).toBe(true)
})

test('khách thường không vào được trang quản trị', async ({ page, request }) => {
  await loginAs(page, request, CUSTOMER)
  await page.goto('/admin/fraud')
  await expect(page).not.toHaveURL(/\/admin/)
})

for (const [path, text] of [['/admin/forecast', 'Dự báo tồn kho'], ['/admin/fraud', 'Rủi ro gian lận'], ['/admin/posts', 'Tin tức, blog'], ['/admin/orders', /đơn hàng/i]]) {
  test(`quản trị: ${path}`, async ({ page, request }) => {
    await loginAs(page, request, ADMIN)
    await page.goto(path)
    await expect(page.getByRole('heading', { level: 1 })).toContainText(text)
    await page.waitForLoadState('networkidle')
    await noHorizontalOverflow(page)
  })
}

test('dự báo tồn kho có bảng + đổi mức phục vụ', async ({ page, request }) => {
  await loginAs(page, request, ADMIN)
  await page.goto('/admin/forecast')
  await expect(page.locator('table tbody tr').first()).toBeVisible()
  const resp = page.waitForResponse(r => r.url().includes('/api/admin/forecast') && r.url().includes('serviceLevel=0.99'))
  await page.getByLabel('Mức phục vụ').selectOption('0.99')
  expect((await resp).ok()).toBeTruthy()
})
