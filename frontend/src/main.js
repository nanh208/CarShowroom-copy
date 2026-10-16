import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
import './theme2.css'
import './stores/theme'

import motion from './motion'
import i18n from './i18n'
import { img } from './img'

const app = createApp(App); app.config.globalProperties.$img = img
app.use(createPinia()).use(router).use(motion).use(i18n).mount('#app')

// Prefetch các trang hay dùng khi trình duyệt rảnh -> chuyển trang tức thì
const idle = window.requestIdleCallback || (cb => setTimeout(cb, 1500))
idle(() => {
  if (navigator.connection?.saveData) return
  ;[() => import('./views/Products.vue'), () => import('./views/ProductDetail.vue'), () => import('./views/Cart.vue'),
    () => import('./views/Checkout.vue'), () => import('./views/Account.vue')].forEach((f, i) => setTimeout(f, i * 150))
})

// PWA: service worker (chỉ bản build) — app shell offline, cache ảnh & catalog
if ('serviceWorker' in navigator && import.meta.env.PROD) addEventListener('load', () => navigator.serviceWorker.register('/sw.js').catch(() => {}))
