/**
 * v-permission 自定义指令
 * 根据用户权限列表控制元素的显示与隐藏
 *
 * 用法：<button v-permission="'user:add'">添加用户</button>
 *
 * 权限列表从 localStorage 的 'permissions' 字段读取（JSON 数组）
 * 权限刷新机制：用户权限变更后，下次登录时重新调用 GET /api/route/menus，
 * routeHelper.js 中的 loadDynamicRoutes 会更新 localStorage 中的权限列表
 */
export const permissionDirective = {
  mounted(el, binding) {
    const requiredPermission = binding.value
    if (!requiredPermission) return

    let userPermissions = []
    try {
      const stored = localStorage.getItem('permissions')
      userPermissions = stored ? JSON.parse(stored) : []
    } catch {
      userPermissions = []
    }

    if (!userPermissions.includes(requiredPermission)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default permissionDirective
