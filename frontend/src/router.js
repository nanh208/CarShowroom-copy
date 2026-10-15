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
  { path: '/checkout', component: () => import('./views/Checkout.vue') },
  { path: '/track/:code?', component: () => import('./views/Track.vue') },
  { path: '/gift-cards', component: () => import('./views/GiftCards.vue') },
  { path: '/wishlist/shared', component: () => import('./views/SharedWishlist.vue') },
  { path: '/bundles', component: () => import('./views/Bundles.vue') },
  { path: '/collections', component: () => import('./views/Collections.vue') },
  { path: '/collections/:slug', component: () => import('./views/CollectionDetail.vue') },
  { path: '/upcoming', component: () => import('./views/Upcoming.vue') },
  { path: '/finder', component: () => import('./views/ScaleFinder.vue') },
  { path: '/rewards', component: () => import('./views/Rewards.vue'), meta: { auth: true } },
  { path: '/notifications', component: () => import('./views/Notifications.vue'), meta: { auth: true } },
  { path: '/orders/:code', component: () => import('./views/OrderDetail.vue'), meta: { auth: true } },
  { path: '/admin/packing', component: () => import('./views/admin/PackingSlips.vue'), meta: { admin: true, bare: true } },
  { path: '/admin/labels', component: () => import('./views/admin/ShippingLabels.vue'), meta: { admin: true, bare: true } },
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
      { path: 'chat', component: () => import('./views/admin/AdminChat.vue') },
      { path: 'audit', component: () => import('./views/admin/AdminAudit.vue'), meta: { adminOnly: true } },
      { path: 'purchasing', component: () => import('./views/admin/AdminPurchasing.vue') },
      { path: 'segments', component: () => import('./views/admin/AdminSegments.vue') },
      { path: 'carts', component: () => import('./views/admin/AdminCarts.vue') },
      { path: 'bundles', component: () => import('./views/admin/AdminBundles.vue') },
      { path: 'home-layout', component: () => import('./views/admin/AdminHomeLayout.vue') },
      { path: 'analytics', component: () => import('./views/admin/AdminAnalytics.vue') },
      { path: 'emails', component: () => import('./views/admin/AdminEmails.vue') },
      { path: 'chatbot', component: () => import('./views/admin/AdminChatbot.vue') },
      { path: 'preorders', component: () => import('./views/admin/AdminPreorders.vue') },
      { path: 'collections', component: () => import('./views/admin/AdminCollections.vue') },
      { path: 'warehouses', component: () => import('./views/admin/AdminWarehouses.vue') },
      { path: 'broadcast', component: () => import('./views/admin/AdminBroadcast.vue') },
      { path: 'price-alerts', component: () => import('./views/admin/AdminPriceAlerts.vue') },
      { path: 'jobs', component: () => import('./views/admin/AdminJobs.vue'), meta: { adminOnly: true } },
      { path: 'appearance', component: () => import('./views/admin/AdminAppearance.vue'), meta: { adminOnly: true } },
      { path: 'permissions', component: () => import('./views/admin/AdminPermissions.vue'), meta: { adminOnly: true } }
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

// Tiêu đề trang tiếng Việt cho từng màn hình
const TITLES = [[/^\/collections/, 'Bộ sưu tập'], [/^\/upcoming/, 'Sắp ra mắt'], [/^\/finder/, 'Tìm mẫu phù hợp'], [/^\/rewards/, 'Phần thưởng'], [/^\/notifications/, 'Thông báo'], [/^\/$/, 'Trang chủ'], [/^\/products\/\d+/, 'Chi tiết sản phẩm'], [/^\/products/, 'Sản phẩm'], [/^\/cart/, 'Giỏ hàng'], [/^\/checkout/, 'Thanh toán'],
  [/^\/track/, 'Tra cứu đơn hàng'], [/^\/gift-cards/, 'Thẻ quà tặng'], [/^\/bundles/, 'Combo ưu đãi'], [/^\/wishlist/, 'Yêu thích'], [/^\/compare/, 'So sánh'],
  [/^\/account/, 'Tài khoản'], [/^\/orders/, 'Đơn hàng'], [/^\/support/, 'Hỗ trợ'], [/^\/faq/, 'Câu hỏi thường gặp'], [/^\/auctions/, 'Đấu giá'], [/^\/promotions/, 'Khuyến mãi'],
  [/^\/login/, 'Đăng nhập'], [/^\/register/, 'Đăng ký'], [/^\/admin/, 'Quản trị'], [/^\/about/, 'Về chúng tôi']]
router.afterEach(to => { const t = TITLES.find(([re]) => re.test(to.path)); document.title = (t ? t[1] + ' · ' : '') + 'ARES Toys — Xe mô hình & RC' })

export default router
