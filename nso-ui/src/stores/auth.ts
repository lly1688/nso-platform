import { defineStore } from 'pinia'
import { api } from '@/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as any,
    token: localStorage.getItem('nso_access_token') || ''
  }),
  actions: {
    async login(username: string, password: string) {
      const session = await api.login({ username, password })
      this.user = session
      this.token = session.accessToken
      localStorage.setItem('nso_access_token', session.accessToken)
      return session
    },
    logout() {
      this.user = null
      this.token = ''
      localStorage.removeItem('nso_access_token')
    }
  }
})
