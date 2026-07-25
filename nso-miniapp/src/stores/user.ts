import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userId: '',
    nickname: '',
    roles: [] as string[],
    permissions: [] as string[]
  }),
  actions: {
    setProfile(profile: Partial<{ userId: string; nickname: string; roles: string[]; permissions: string[] }>) {
      Object.assign(this, profile)
    }
  }
})
