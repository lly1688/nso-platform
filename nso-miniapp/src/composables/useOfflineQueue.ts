import type { OfflineTask } from '../stores/offline'
import { useOfflineStore } from '../stores/offline'

export function useOfflineQueue() {
  const offlineStore = useOfflineStore()

  return {
    enqueue: (task: OfflineTask) => offlineStore.enqueue(task),
    clear: () => offlineStore.clear()
  }
}
