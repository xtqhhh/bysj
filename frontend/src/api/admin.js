import request from '@/utils/request'

// 用户管理
export function getUsers(params) {
  return request.get('/api/admin/users', { params })
}
export function createUser(data) {
  return request.post('/api/admin/users', data)
}
export function updateUserStatus(id, status) {
  return request.put(`/api/admin/users/${id}/status`, { status })
}
export function resetUserPassword(id, newPassword) {
  return request.put(`/api/admin/users/${id}/password`, { newPassword })
}

// 角色管理
export function getRoles() {
  return request.get('/api/admin/roles')
}
export function createRole(data) {
  return request.post('/api/admin/roles', data)
}
export function updateRole(id, data) {
  return request.put(`/api/admin/roles/${id}`, data)
}
export function deleteRole(id) {
  return request.delete(`/api/admin/roles/${id}`)
}
export function assignPermissions(roleId, permissionIds) {
  return request.put(`/api/admin/roles/${roleId}/permissions`, { permissionIds })
}

// 审计日志
export function getAuditLogs(params) {
  return request.get('/api/admin/audit-logs', { params })
}

// 修改密码
export function changePassword(data) {
  return request.put('/api/auth/password', data)
}

// 留言管理（管理员）
export function getMessagesAdmin(params) {
  return request.get('/api/messages/admin', { params })
}
export function approveMessage(id) {
  return request.put(`/api/messages/${id}/approve`)
}
export function rejectMessage(id) {
  return request.put(`/api/messages/${id}/reject`)
}
export function replyMessage(id, reply) {
  return request.put(`/api/messages/${id}/reply`, { reply })
}
export function deleteMessage(id) {
  return request.delete(`/api/messages/${id}`)
}

// 政务申请管理
export function getApplicationsAdmin(params) {
  return request.get('/api/applications/admin', { params })
}
export function reviewApplication(id, data) {
  return request.put(`/api/applications/${id}/review`, data)
}
export function updateUserRoles(id, roleIds) {
  return request.put(`/api/admin/users/${id}/roles`, { roleIds })
}
