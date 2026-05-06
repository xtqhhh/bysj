import request from '@/utils/request'

/**
 * 获取当前用户的菜单列表与按钮权限
 * @returns {Promise<{menus: Array, permissions: Array}>}
 */
export function getMenus() {
  return request.get('/api/route/menus')
}
