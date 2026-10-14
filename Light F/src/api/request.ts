import axios, { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'

const ACCESS_KEY = 'light_access_token'
const REFRESH_KEY = 'light_refresh_token'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ===== refresh token 单飞控制 =====
// access token 有效期只有 2 小时，过期后业务请求会收到 401。
// 此时用 refresh token 静默换新 access，再把原请求重放一遍，用户无感知。
// 多个请求同时 401 时只允许一个刷新请求，其余排队等待新 token。
let isRefreshing = false
let waitQueue: Array<(token: string | null) => void> = []

const flushQueue = (token: string | null) => {
  waitQueue.forEach((cb) => cb(token))
  waitQueue = []
}

const forceLogout = () => {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  if (location.pathname !== '/login') location.href = '/login'
}

// 用裸 axios 调刷新接口，绕过本文件的拦截器，避免 401 处理递归
async function exchangeRefreshToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem(REFRESH_KEY)
  if (!refreshToken) return null
  try {
    const res = await axios.post('/api/identity/refresh', { refreshToken })
    const data = res.data?.data
    if (data?.accessToken) {
      localStorage.setItem(ACCESS_KEY, data.accessToken)
      if (data.refreshToken) localStorage.setItem(REFRESH_KEY, data.refreshToken)
      return data.accessToken as string
    }
    return null
  } catch {
    return null
  }
}

// 登录/注册/刷新接口自身的 401 属于“凭证确实无效”，不能再尝试刷新
const isAuthEndpoint = (url?: string) => typeof url === 'string' && /\/identity\/(login|register|refresh)$/.test(url)

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

request.interceptors.response.use(
  (response: AxiosResponse) => {
    // 文件下载（blob）没有统一的 { code, msg, data } 包装，直接放行完整 response，
    // 调用方通过 res.data 拿到 Blob
    if (response.config.responseType === 'blob') return response

    const res = response.data as ApiResult<unknown>
    if (res.code !== 0) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    return res as unknown as AxiosResponse
  },
  async (error) => {
    const config = error.config as RetriableConfig | undefined
    const status = error.response?.status

    if (status === 401 && config && !config._retried && !isAuthEndpoint(config.url)) {
      // 已有刷新在途：挂入队列，等刷新结果出来后用新 token 重放
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          waitQueue.push((token) => {
            if (!token) {
              reject(error)
              return
            }
            config.headers.Authorization = `Bearer ${token}`
            resolve(request(config))
          })
        })
      }

      config._retried = true
      isRefreshing = true
      const newToken = await exchangeRefreshToken()
      isRefreshing = false

      if (newToken) {
        config.headers.Authorization = `Bearer ${newToken}`
        flushQueue(newToken)
        return request(config) // 重放原请求
      }
      flushQueue(null)
      forceLogout()
      return Promise.reject(error)
    }

    if (status === 401) forceLogout()
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
