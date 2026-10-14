import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from './stores/auth'

const routes = [
  { path: '/', component: () => import('./views/Home.vue') },
  { path: '/products', component: () => import('./views/Products.vue') },
  { path: '/products/:id', component: () => import('./views/ProductDetail.vue') },
  { path: '/promotions', component: () => import('./views/Promotions.vue') },
  { path: '/services', component: () => import('./views/Services.vue') },
  { path: '/cart', component: () => import('./views/Cart.vue') },
  { path: '/checkout', component: () => import('./views/Checkout.vue'), meta: { auth: true } },
  { path: '/orders/:code', component: () => import('./views/OrderDetail.vue'), meta: { auth: true } },
  { path: '/login', component: () => import('./views/Login.vue'), meta: { bare: true } },
  { path: '/register', component: () => import('./views/Register.vue'), meta: { bare: true } },
  { path: '/forgot-password', component: () => import('./views/ForgotPassword.vue'), meta: { bare: true } },
  { path: '/oauth-callback', component: () => import('./views/OAuthCallback.vue'), meta: { bare: true } },
  { path: '/account', component: () => import('./views/Account.vue'), meta: { auth: true } },
  {
    path: '/admin', component: () => import('./views/admin/AdminLayout.vue'), meta: { admin: true },
    children: [
      { path: '', component: () => import('./views/admin/Dashboard.vue') },
      { path: 'products', component: () => import('./views/admin/AdminProducts.vue') },
      { path: 'orders', component: () => import('./views/admin/AdminOrders.vue') },
      { path: 'customers', component: () => import('./views/admin/AdminCustomers.vue') },
      { path: 'promotions', component: () => import('./views/admin/AdminPromotions.vue') },
      { path: 'bookings', component: () => import('./views/admin/AdminBookings.vue') },
      { path: 'chat', component: () => import('./views/admin/AdminChat.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', component: () => import('./views/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(), routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async to => {
  const auth = useAuth()
  if (!auth.ready) await auth.init()
  if ((to.meta.auth || to.meta.admin) && !auth.loggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.admin && !auth.isAdmin) return '/'
})

export default router
