export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface RequestOptions {
  url: string
  method?: UniApp.RequestOptions['method']
  data?: unknown
  header?: Record<string, string>
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export function request<T = unknown>(options: RequestOptions): Promise<ApiResponse<T>> {
  const token = uni.getStorageSync('accessToken')
  const header: Record<string, string> = {
    ...(options.header || {})
  }

  if (token) {
    header.Authorization = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data as UniApp.RequestOptions['data'],
      header,
      success: (response) => {
        resolve(response.data as ApiResponse<T>)
      },
      fail: reject
    })
  })
}
