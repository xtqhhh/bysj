import axios from 'axios'
import router from '@/router'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

// 创建 axios 实例
const request = axios.create({
  baseURL: BASE_URL,
  timeout: 10000
})

// 标记是否正在刷新 Token，防止并发请求重复刷新
let isRefreshing = false
// 等待刷新完成的请求队列
let pendingRequests = []

function onTokenRefreshed(newToken) {
  pendingRequests.forEach(cb => cb(newToken))
  pendingRequests = []
}

function addPendingRequest(cb) {
  pendingRequests.push(cb)
}

// 请求拦截器：自动附加 Authorization 头
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：处理 401 TOKEN_EXPIRED，自动刷新 Token
request.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config

    if (
      error.response?.status === 401 &&
      error.response?.data?.code === 'TOKEN_EXPIRED' &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true

      if (isRefreshing) {
        // 已在刷新中，将请求加入队列等待新 Token
        return new Promise(resolve => {
          addPendingRequest(newToken => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(request(originalRequest))
          })
        })
      }

      isRefreshing = true

      try {
        const refreshToken = localStorage.getItem('refresh_token')
        const { data } = await axios.post(`${BASE_URL}/api/auth/refresh`, { refreshToken })
        // 后端统一响应结构：{ code, message, data: { accessToken } }
        const newAccessToken = data.data?.accessToken

        localStorage.setItem('access_token', newAccessToken)
        request.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`

        onTokenRefreshed(newAccessToken)
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return request(originalRequest)
      } catch {
        // 刷新失败：清除本地 Token 并跳转登录页
        localStorage.removeItem('access_token')
        localStorage.removeItem('refresh_token')
        pendingRequests = []
        router.push('/login')
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default request
