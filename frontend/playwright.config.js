// Bộ test giao diện tự động (Playwright). Yêu cầu backend Spring Boot chạy ở :8080 (API_URL) và bản build preview ở :4173.
//   npm run build && npm run test:e2e     (tự khởi động `vite preview` nếu chưa chạy)
import { defineConfig, devices } from '@playwright/test'
const BASE = process.env.E2E_BASE_URL || 'http://localhost:4173'
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 7_000 },
  fullyParallel: true,
  workers: process.env.CI ? 2 : 4,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: { baseURL: BASE, trace: 'retain-on-failure', screenshot: 'only-on-failure', locale: 'vi-VN', timezoneId: 'Asia/Ho_Chi_Minh' },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
    { name: 'mobile', use: { ...devices['Pixel 7'] } }
  ],
  webServer: process.env.E2E_BASE_URL ? undefined : { command: 'npx vite preview --port 4173 --strictPort', url: BASE, reuseExistingServer: true, timeout: 60_000 }
})
