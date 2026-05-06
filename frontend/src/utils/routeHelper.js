import router from '@/router'
import { getMenus } from '@/api/route'

// 标记动态路由是否已加载，防止重复注册
let dynamicRoutesLoaded = false

/**
 * 将菜单列表递归转换为 Vue Router 路由配置
 * @param {Array} menus
 * @returns {Array}
 */
function buildRoutes(menus) {
  const routes = []
  for (const menu of menus) {
    if (menu.component) {
      const route = {
        path: menu.path,
        name: menu.name,
        component: () => import(`../views/${menu.component}.vue`),
        meta: {
          title: menu.name,
          icon: menu.icon,
          requiresAuth: true
        }
      }
      if (menu.children && menu.children.length > 0) {
        route.children = buildRoutes(menu.children)
      }
      routes.push(route)
    } else if (menu.children && menu.children.length > 0) {
      // 目录节点：只注册子路由
      routes.push(...buildRoutes(menu.children))
    }
  }
  return routes
}

/**
 * 加载并注册动态路由
 * 登录成功后调用，将后端返回的菜单列表注册为 Vue Router 路由
 * @returns {Promise<Array>} 返回权限列表
 */
export async function loadDynamicRoutes() {
  if (dynamicRoutesLoaded) return

  try {
    const res = await getMenus()
    const { menus = [], permissions = [] } = res.data.data || {}

    // 将权限列表存入 localStorage，供 v-permission 指令使用
    localStorage.setItem('permissions', JSON.stringify(permissions))

    // 构建并注册动态路由（挂载到根路由下）
    const dynamicRoutes = buildRoutes(menus)
    dynamicRoutes.forEach(route => {
      router.addRoute(route)
    })

    dynamicRoutesLoaded = true
  } catch (e) {
    // 加载失败也标记为已加载，避免导航守卫死循环
    dynamicRoutesLoaded = true
    console.error('动态路由加载失败', e)
  }
}

/**
 * 重置动态路由状态（登出时调用）
 */
export function resetDynamicRoutes() {
  dynamicRoutesLoaded = false
  localStorage.removeItem('permissions')
}

/**
 * 判断动态路由是否已加载
 */
export function isDynamicRoutesLoaded() {
  return dynamicRoutesLoaded
}
