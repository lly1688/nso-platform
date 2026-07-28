import { defineStore } from 'pinia'
import { api } from '@/api'
import type { UserProfile } from '@/types'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('nso_session') || 'null') as any,
    token: localStorage.getItem('nso_access_token') || '',
    refreshToken: localStorage.getItem('nso_refresh_token') || '',
    profile: JSON.parse(localStorage.getItem('nso_profile') || 'null') as UserProfile | null,
    avatarObjectUrl: ''
  }),
  actions: {
    saveSession(session: any) {
      this.user = session
      this.token = session.accessToken || ''
      this.refreshToken = session.refreshToken || ''
      if (this.token) localStorage.setItem('nso_access_token', this.token)
      if (this.refreshToken) localStorage.setItem('nso_refresh_token', this.refreshToken)
      localStorage.setItem('nso_session', JSON.stringify(session))
    },
    clearSession() {
      this.user = null
      this.token = ''
      this.refreshToken = ''
      this.profile = null
      if (this.avatarObjectUrl) URL.revokeObjectURL(this.avatarObjectUrl)
      this.avatarObjectUrl = ''
      localStorage.removeItem('nso_access_token')
      localStorage.removeItem('nso_refresh_token')
      localStorage.removeItem('nso_session')
      localStorage.removeItem('nso_profile')
    },
    async login(username: string, password: string) {
      const session = await api.login({ username, password })
      this.saveSession(session)
      await this.loadProfile()
      return session
    },
    async refresh() {
      if (!this.refreshToken) throw new Error('登录已过期')
      const session = await api.refresh(this.refreshToken)
      this.saveSession(session)
      return session
    },
    async logout() {
      if (this.refreshToken) {
        try { await api.logout(this.refreshToken) } catch { /* local revocation still proceeds */ }
      }
      this.clearSession()
    },
    saveProfile(profile: UserProfile) {
      this.profile = profile
      localStorage.setItem('nso_profile', JSON.stringify(profile))
      if (this.user) {
        this.user = { ...this.user, nickname: profile.nickname, roles: profile.roles }
        localStorage.setItem('nso_session', JSON.stringify(this.user))
      }
    },
    async loadProfile() {
      const profile = await api.profile()
      this.saveProfile(profile)
      if (this.avatarObjectUrl) {
        URL.revokeObjectURL(this.avatarObjectUrl)
        this.avatarObjectUrl = ''
      }
      if (profile.avatarUrl) {
        try {
          this.avatarObjectUrl = await api.profileAvatar()
        } catch {
          // A stale or inaccessible historical avatar must not block the signed-in profile.
          this.avatarObjectUrl = ''
        }
      }
      return profile
    },
    can(permission: string) {
      return Boolean(this.user?.permissions?.includes(permission))
    }
  }
})
