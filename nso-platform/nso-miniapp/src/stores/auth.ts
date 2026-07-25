import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: uni.getStorageSync('accessToken') || '',
    refreshToken: uni.getStorageSync('refreshToken') || ''
  }),
  actions: {
    setToken(accessToken: string, refreshToken: string) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      uni.setStorageSync('accessToken', accessToken)
      uni.setStorageSync('refreshToken', refreshToken)
    },
    clearToken() {
      this.accessToken = ''
      this.refreshToken = ''
      uni.removeStorageSync('accessToken')
      uni.removeStorageSync('refreshToken')
    }
  }
})
