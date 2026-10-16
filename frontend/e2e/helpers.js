import { expect } from '@playwright/test'
export const API = process.env.API_URL || 'http://localhost:8080'
export const ADMIN = { email: process.env.E2E_ADMIN_EMAIL || 'admin@ares.vn', password: process.env.E2E_ADMIN_PASSWORD || 'admin123' }
export const CUSTOMER = { email: process.env.E2E_USER_EMAIL || 'khach@ares.vn', password: process.env.E2E_USER_PASSWORD || '123456' }

/** Đăng nhập qua API rồi đặt token vào localStorage (nhanh, không phụ thuộc form). */
export async function loginAs(page, request, who = ADMIN) {
  const r = await request.post(API + '/api/auth/login', { data: who }); expect(r.ok()).toBeTruthy()
  const d = await r.json()
  await page.goto('/')
  await page.evaluate(d => { localStorage.setItem('token', d.token); localStorage.setItem('user', JSON.stringify(d.user)) }, d)
  return d
}
/** Theo dõi lỗi JS trên trang. */
export function trackErrors(page) {
  const errs = []
  page.on('pageerror', e => errs.push(e.message))
  return errs
}
export async function noHorizontalOverflow(page) {
  const ov = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(ov, 'horizontal overflow (px)').toBeLessThanOrEqual(1)
}
export async function firstProduct(request) {
  const r = await request.get(API + '/api/products?size=40'); const d = await r.json()
  return d.content.find(p => p.stock > 0 && !p.hasVariants && !p.preorder) || d.content[0]
}
