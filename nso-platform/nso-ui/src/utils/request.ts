import axios from 'axios'
import type { ApiResponse } from '@/types'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('nso_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use((response) => {
  const body = response.data as ApiResponse<unknown>
  if (body && typeof body.code === 'number' && body.code !== 200) {
    return Promise.reject(new Error(body.message || '接口请求失败'))
  }
  return response
})

export async function getData<T>(url: string, params?: Record<string, unknown>) {
  const response = await http.get<ApiResponse<T>>(url, { params })
  return response.data.data
}

export async function postData<T>(url: string, data?: unknown) {
  const response = await http.post<ApiResponse<T>>(url, data)
  return response.data.data
}
