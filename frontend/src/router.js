import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from './stores/auth'

const routes = [
  { path: '/', component: () => import('./views/Home.vue'), meta: { darkHero: true } },
  { path: '/about', component: () => import('./views/About.vue') },
  { path: '/products', component: () => import('./views/Products.vue') },
  { path: '/products/:id', component: () => import('./views/ProductDetail.vue') },
  { path: '/auctions', component: () => import('./views/Auctions.vue') },
  { path: '/auctions/:id', component: () => import('./views/AuctionDetail.vue') },
  { path: '/promotions', component: () => import('./views/Promotions.vue') },
  { path: '/support', component: () => import('./views/Support.vue') },
  { path: '/services', redirect: '/support' },
  { path: '/support/tickets/:code', component: () => import('./views/TicketDetail.vue'), meta: { auth: true } },
  { path: '/pay/:code', component: () => import('./views/PayMock.vue'), meta: { auth: true, bare: true } },
  { path: '/orders/:code/invoice', component: () => import('./views/Invoice.vue'), meta: { auth: true, bare: true } },
  { path: '/wishlist', component: () => import('./views/Wishlist.vue') },
  { path: '/compare', component: () => import('./views/Compare.vue') },
  { path: '/faq', component: () => import('./views/Faq.vue') },
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
      { path: 'auctions', component: () => import('./views/admin/AdminAuctions.vue') },
      { path: 'inventory', component: () => import('./views/admin/AdminInventory.vue') },
      { path: 'returns', component: () => import('./views/admin/AdminReturns.vue') },
      { path: 'flash-sales', component: () => import('./views/admin/AdminFlash.vue') },
      { path: 'banners', component: () => import('./views/admin/AdminBanners.vue') },
      { path: 'catalog', component: () => import('./views/admin/AdminCatalog.vue') },
      { path: 'questions', component: () => import('./views/admin/AdminQuestions.vue') },
      { path: 'tickets', component: () => import('./views/admin/AdminTickets.vue') },
      { path: 'reports', component: () => import('./views/admin/AdminReports.vue'), meta: { adminOnly: true } },
      { path: 'reviews', component: () => import('./views/admin/AdminReviews.vue') },
      { path: 'chat', component: () => import('./views/admin/AdminChat.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', component: () => import('./views/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(), routes,
  scrollBehavior: (to, from, saved) => saved || (to.hash ? { el: to.hash, top: 90 } : to.path === from.path ? false : { top: 0 })
})

router.beforeEach(async to => {
  const auth = useAuth()
  if (!auth.ready) await auth.init()
  if ((to.meta.auth || to.meta.admin) && !auth.loggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.admin && !auth.isStaff) return '/'
  if (to.meta.adminOnly && !auth.isAdmin) return '/admin'
})

export default router
