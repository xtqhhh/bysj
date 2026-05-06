import request from '@/utils/request'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

/**
 * 获取图形验证码
 * @returns {Promise<{uuid: string, image: string}>}
 */
export function getCaptcha() {
  return request.get(`${BASE_URL}/api/auth/captcha`)
}

/**
 * 用户登录
 * @param {Object} data - { username, password, captchaUuid, captchaCode }
 * @returns {Promise<{accessToken: string, refreshToken: string, user: Object}>}
 */
export function login(data) {
  return request.post(`${BASE_URL}/api/auth/login`, data)
}

/**
 * 刷新 Access Token
 * @param {string} refreshToken
 * @returns {Promise<{accessToken: string}>}
 */
export function refreshToken(refreshToken) {
  return request.post(`${BASE_URL}/api/auth/refresh`, { refreshToken })
}

/**
 * 用户登出
 * @returns {Promise}
 */
export function logout() {
  return request.post(`${BASE_URL}/api/auth/logout`)
}

/**
 * 用户注册
 * @param {Object} data - { username, password, phone, realName }
 * @returns {Promise<{message: string}>}
 */
export function register(data) {
  return request.post(`${BASE_URL}/api/auth/register`, data)
}

// 留言板（公开查询 + 登录提交）
export function getMessages(params) {
  return request.get('/api/messages', { params })
}
export function submitMessage(content) {
  return request.post('/api/messages', { content })
}
export function getMyMessages(params) {
  return request.get('/api/messages/my', { params })
}

// 政务申请
export function submitApplication(data) {
  return request.post('/api/applications', data)
}
export function getMyApplications(params) {
  return request.get('/api/applications/my', { params })
}
export function getApplicationDetail(id) {
  return request.get(`/api/applications/${id}`)
}
export function queryApplicationByNo(applyNo) {
  return request.get('/api/applications/query', { params: { applyNo } })
}
export function cancelApplication(id) {
  return request.delete(`/api/applications/${id}`)
}
