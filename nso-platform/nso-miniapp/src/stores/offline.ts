import { defineStore } from 'pinia'

export interface OfflineTask {
  id: string
  type: string
  payload: unknown
}

export const useOfflineStore = defineStore('offline', {
  state: () => ({
    queue: [] as OfflineTask[]
  }),
  actions: {
    enqueue(task: OfflineTask) {
      this.queue.push(task)
    },
    clear() {
      this.queue = []
    }
  }
})
