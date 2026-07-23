import { createRouter, createWebHistory } from 'vue-router'
import { trackVisit } from '../api'

const routes = [
  // ---- 用户(老师)端 ----
  { path: '/login', name: 'userLogin', component: () => import('../views/user/Login.vue') },
  { path: '/register', name: 'userRegister', component: () => import('../views/user/Register.vue') },
  {
    path: '/',
    component: () => import('../layouts/UserLayout.vue'),
    meta: { requiresUser: true },
    children: [
      { path: '', redirect: '/report' },
      { path: 'report', name: 'report', component: () => import('../views/user/Report.vue') },
      { path: 'my', name: 'my', component: () => import('../views/user/MyTickets.vue') },
    ],
  },

  // ---- 管理端 ----
  { path: '/admin/login', name: 'adminLogin', component: () => import('../views/Login.vue') },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/tickets' },
      { path: 'tickets', name: 'tickets', component: () => import('../views/Admin.vue') },
      { path: 'assets', name: 'assets', component: () => import('../views/Assets.vue') },
      { path: 'stats', name: 'stats', component: () => import('../views/Stats.vue') },
      { path: 'users', name: 'users', component: () => import('../views/Users.vue') },
      { path: 'settings', name: 'settings', component: () => import('../views/Settings.vue') },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.requiresAdmin && !localStorage.getItem('admin_token')) return { path: '/admin/login' }
  if (to.meta.requiresUser && !localStorage.getItem('user_token')) return { path: '/login' }
})

router.afterEach((to, from) => {
  // 前台(老师端/登录/注册)访问打点,不含 /admin
  if (!to.path.startsWith('/admin')) {
    const referrer = from && from.name ? location.origin + from.fullPath : (document.referrer || '')
    trackVisit({ path: to.fullPath, referrer }).catch(() => {})
  }
})

export default router
