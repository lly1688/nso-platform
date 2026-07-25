import { defineStore } from 'pinia'

export const useMessageStore = defineStore('message', {
  state: () => ({
    unreadCount: 0
  }),
  actions: {
    setUnreadCount(count: number) {
      this.unreadCount = count
    }
  }
})
