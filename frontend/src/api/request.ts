import axios from 'axios'
import type { ApiResponse } from '@/types'
import { feedback } from '@/utils/feedback'
import { clearAuthSession, getAuthToken } from '@/utils/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && typeof body.success === 'boolean') {
      if (!body.success) {
        feedback.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return response.data
  },
  (error) => {
    if (error?.response?.status === 401) {
      clearAuthSession()
      feedback.error(error?.response?.data?.message || '登录状态已失效，请重新登录')
      if (window.location.pathname !== '/login') {
        const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`
        window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
      }
    } else {
      feedback.error(error?.message || '网络请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
