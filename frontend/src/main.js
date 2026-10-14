import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './style.css'

import motion from './motion'

createApp(App).use(createPinia()).use(router).use(motion).mount('#app')
