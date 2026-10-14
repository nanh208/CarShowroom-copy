import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'
import './theme2.css'
import './stores/theme'

import motion from './motion'

createApp(App).use(createPinia()).use(router).use(motion).mount('#app')

// Prefetch các trang hay dùng khi trình duyệt rảnh -> chuyển trang tức thì
const idle = window.requestIdleCallback || (cb => setTimeout(cb, 1500))
idle(() => {
  if (navigator.connection?.saveData) return
  ;[() => import('./views/Products.vue'), () => import('./views/ProductDetail.vue'), () => import('./views/Cart.vue'),
    () => import('./views/Checkout.vue'), () => import('./views/Account.vue')].forEach((f, i) => setTimeout(f, i * 150))
})
