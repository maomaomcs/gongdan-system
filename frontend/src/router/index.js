import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'report', component: () => import('../views/Report.vue') },
  { path: '/query', name: 'query', component: () => import('../views/Query.vue') },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/admin/tickets' },
      { path: 'tickets', name: 'tickets', component: () => import('../views/Admin.vue') },
      { path: 'stats', name: 'stats', component: () => import('../views/Stats.vue') },
      { path: 'users', name: 'users', component: () => import('../views/Users.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !localStorage.getItem('ticket_token')) {
    return { path: '/login' }
  }
})

export default router
