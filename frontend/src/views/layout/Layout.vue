<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed }">
      <div class="logo">
        <div class="logo-emblem">国</div>
        <transition name="fade">
          <div v-if="!collapsed" class="logo-info">
            <span class="logo-title">政务管理系统</span>
            <span class="logo-sub">Gov Security Platform</span>
          </div>
        </transition>
      </div>

      <nav class="menu">
        <div v-for="group in menuGroups" :key="group.label">
          <div v-if="!collapsed" class="menu-group-label">{{ group.label }}</div>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="menu-item"
            active-class="active"
          >
            <span class="menu-icon" :title="collapsed ? item.name : ''">{{ item.icon }}</span>
            <transition name="fade">
              <span v-if="!collapsed" class="menu-label">{{ item.name }}</span>
            </transition>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-footer">
        <transition name="fade">
          <span v-if="!collapsed" class="version">v1.0.0</span>
        </transition>
        <button class="collapse-btn" @click="collapsed = !collapsed" :title="collapsed ? '展开菜单' : '收起菜单'">
          <span>{{ collapsed ? '▶' : '◀' }}</span>
        </button>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-wrapper">
      <!-- 顶部导航 -->
      <header class="topbar">
        <div class="topbar-left">
          <span class="breadcrumb">{{ currentPageName }}</span>
        </div>
        <div class="topbar-right">
          <router-link to="/" class="home-btn" title="返回门户首页">🏛 返回首页</router-link>
          <div class="divider"></div>
          <div class="user-info">
            <div class="avatar">{{ avatarChar }}</div>
            <div class="user-detail">
              <span class="username">{{ userInfo.realName || userInfo.username }}</span>
              <span class="role-tag" :class="roleClass">{{ roleLabel }}</span>
            </div>
          </div>
          <div class="divider"></div>
          <button class="logout-btn" @click="handleLogout">
            <span>⏻</span> 退出
          </button>
        </div>
      </header>

      <!-- 面包屑 + 内容区 -->
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout } from '@/api/auth'
import { resetDynamicRoutes } from '@/utils/routeHelper'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)

const userInfo = computed(() => {
  try { return JSON.parse(localStorage.getItem('user_info') || '{}') } catch { return {} }
})

const avatarChar = computed(() => {
  const name = userInfo.value.realName || userInfo.value.username || '?'
  return name.charAt(0).toUpperCase()
})

const roleLabel = computed(() => {
  const roles = userInfo.value.roles || []
  if (roles.includes('ROLE_SUPER_ADMIN')) return '超级管理员'
  if (roles.includes('ROLE_OFFICER')) return '工作人员'
  return '群众用户'
})

const roleClass = computed(() => {
  const roles = userInfo.value.roles || []
  if (roles.includes('ROLE_SUPER_ADMIN')) return 'role-admin'
  if (roles.includes('ROLE_OFFICER')) return 'role-officer'
  return 'role-citizen'
})

const menuGroups = computed(() => {
  const roles = userInfo.value.roles || []
  const groups = [
    {
      label: '工作台',
      items: [{ path: '/app/dashboard', name: '系统首页', icon: '⊞' }]
    }
  ]
  if (roles.includes('ROLE_SUPER_ADMIN')) {
    groups.push({
      label: '系统管理',
      items: [
        { path: '/app/admin/users', name: '用户管理', icon: '👤' },
        { path: '/app/admin/roles', name: '角色管理', icon: '🛡' },
        { path: '/app/admin/audit-logs', name: '审计日志', icon: '📋' },
        { path: '/app/admin/messages', name: '留言管理', icon: '💬' },
        { path: '/app/admin/applications', name: '申请管理', icon: '📋' },
      ]
    })
  } else if (roles.includes('ROLE_OFFICER')) {
    groups.push({
      label: '系统管理',
      items: [
        { path: '/app/admin/users', name: '用户管理', icon: '👤' },
        { path: '/app/admin/applications', name: '申请管理', icon: '📋' },
      ]
    })
  }
  groups.push({
    label: '个人中心',
    items: [{ path: '/app/profile/password', name: '修改密码', icon: '🔒' }]
  })
  return groups
})

const currentPageName = computed(() => {
  const allItems = menuGroups.value.flatMap(g => g.items)
  return allItems.find(i => route.path.startsWith(i.path))?.name || '政务管理系统'
})

async function handleLogout() {
  try { await logout() } catch { /* ignore */ }
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  localStorage.removeItem('user_info')
  localStorage.removeItem('permissions')
  resetDynamicRoutes()
  router.push('/')
}
</script>

<style scoped>
.layout { display: flex; height: 100vh; overflow: hidden; font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; }

/* ── 侧边栏 ── */
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #0f2a52 0%, #1a3a6b 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(.4,0,.2,1);
  flex-shrink: 0;
  box-shadow: 2px 0 8px rgba(0,0,0,0.15);
}
.sidebar.collapsed { width: 64px; }

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  overflow: hidden;
}
.logo-emblem {
  width: 36px; height: 36px; flex-shrink: 0;
  background: linear-gradient(135deg, #c8a84b, #e8c96a);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 900; color: #0f2a52;
  letter-spacing: -1px;
}
.logo-info { display: flex; flex-direction: column; overflow: hidden; }
.logo-title { font-size: 14px; font-weight: 700; white-space: nowrap; }
.logo-sub { font-size: 10px; opacity: 0.5; white-space: nowrap; margin-top: 1px; }

.menu { flex: 1; padding: 8px 0; overflow-y: auto; overflow-x: hidden; }
.menu::-webkit-scrollbar { width: 4px; }
.menu::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15); border-radius: 2px; }

.menu-group-label {
  font-size: 11px; color: rgba(255,255,255,0.35);
  padding: 12px 16px 4px;
  letter-spacing: 1px;
  text-transform: uppercase;
  white-space: nowrap;
}

.menu-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px;
  color: rgba(255,255,255,0.65);
  text-decoration: none; font-size: 13.5px;
  transition: all 0.15s; white-space: nowrap; overflow: hidden;
  border-left: 3px solid transparent;
  margin: 1px 0;
}
.menu-item:hover { background: rgba(255,255,255,0.08); color: #fff; }
.menu-item.active {
  background: rgba(200,168,75,0.15);
  color: #e8c96a;
  border-left-color: #c8a84b;
  font-weight: 600;
}
.menu-icon { font-size: 15px; flex-shrink: 0; width: 20px; text-align: center; }

.sidebar-footer {
  height: 48px; border-top: 1px solid rgba(255,255,255,0.08);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 12px;
}
.version { font-size: 11px; color: rgba(255,255,255,0.25); }
.collapse-btn {
  background: rgba(255,255,255,0.08); border: none;
  width: 28px; height: 28px; border-radius: 6px;
  cursor: pointer; color: rgba(255,255,255,0.5);
  font-size: 10px; display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.collapse-btn:hover { background: rgba(255,255,255,0.15); color: #fff; }

/* ── 主区域 ── */
.main-wrapper { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #f0f2f5; }

.topbar {
  height: 56px; background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.topbar-left .breadcrumb { font-size: 15px; font-weight: 600; color: #1a3a6b; }

.topbar-right { display: flex; align-items: center; gap: 16px; }
.user-info { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, #1a3a6b, #2d6a9f);
  color: #fff; font-size: 14px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.user-detail { display: flex; flex-direction: column; }
.username { font-size: 13px; font-weight: 600; color: #333; line-height: 1.3; }
.role-tag { font-size: 11px; padding: 1px 6px; border-radius: 8px; line-height: 1.6; }
.role-admin { background: #fff3cd; color: #856404; }
.role-officer { background: #d1ecf1; color: #0c5460; }
.role-citizen { background: #d4edda; color: #155724; }

.home-btn {
  display: flex; align-items: center; gap: 5px;
  font-size: 13px; color: #555; text-decoration: none;
  padding: 5px 12px; border: 1px solid #e0e0e0;
  border-radius: 6px; transition: all 0.15s;
}
.home-btn:hover { background: #f0f4ff; border-color: #c5d5f0; color: #1a3a6b; }
.divider { width: 1px; height: 24px; background: #e8e8e8; }
.logout-btn {
  display: flex; align-items: center; gap: 5px;
  background: none; border: 1px solid #e0e0e0;
  border-radius: 6px; padding: 5px 12px;
  font-size: 13px; cursor: pointer; color: #666;
  transition: all 0.15s;
}
.logout-btn:hover { background: #fff2f0; border-color: #ffccc7; color: #cf1322; }

.content { flex: 1; overflow-y: auto; padding: 20px 24px; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
