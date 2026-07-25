import { defineStore } from 'pinia'

export const useDictionaryStore = defineStore('dictionary', {
  state: () => ({
    items: {} as Record<string, Array<{ label: string; value: string }>>
  }),
  actions: {
    setDictionary(type: string, values: Array<{ label: string; value: string }>) {
      this.items[type] = values
    }
  }
})
