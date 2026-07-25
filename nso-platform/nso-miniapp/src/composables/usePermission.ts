import { hasPermission } from '../utils/permission'
import { useUserStore } from '../stores/user'

export function usePermission() {
  const userStore = useUserStore()

  return {
    can: (code: string) => hasPermission(userStore.permissions, code)
  }
}
