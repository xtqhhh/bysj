import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Portal from '@/views/Portal.vue'
import { loadDynamicRoutes, isDynamicRoutesLoaded } from '@/utils/routeHelper'

const routes = [
  // 门户首页（无需登录）
  { path: '/', name: 'Portal', component: Portal },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/403', name: 'Forbidden', component: () => import('@/views/Forbidden.vue') },

  // Layout 包裹的认证页面
  {
    path: '/app',
    component: () => import('@/views/layout/Layout.vue'),
    meta: { requiresAuth: true },
    redirect: '/app/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
      { path: 'admin/users', name: 'UserList', component: () => import('@/views/admin/UserList.vue') },
      { path: 'admin/roles', name: 'RoleList', component: () => import('@/views/admin/RoleList.vue') },
      { path: 'admin/audit-logs', name: 'AuditLog', component: () => import('@/views/admin/AuditLog.vue') },
      { path: 'admin/messages', name: 'MessageList', component: () => import('@/views/admin/MessageList.vue') },
      { path: 'admin/applications', name: 'ApplicationList', component: () => import('@/views/admin/ApplicationList.vue') },
      { path: 'profile/password', name: 'ChangePassword', component: () => import('@/views/profile/ChangePassword.vue') },
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/403' }
]

const router = createRouter({ history: createWebHistory(), routes })

const WHITE_LIST = ['/', '/login', '/register', '/403']

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('access_token')

  if (WHITE_LIST.includes(to.path)) {
    // 已登录用户访问登录/注册页，跳转到系统内
    if ((to.path === '/login' || to.path === '/register') && token) return next('/app/dashboard')
    return next()
  }

  if (!token) return next('/login')

  if (!isDynamicRoutesLoaded()) {
    await loadDynamicRoutes()
    return next({ ...to, replace: true })
  }

  if (to.matched.length > 0) return next()
  return next('/403')
})

export default router
