import { useAuthStore } from '../stores/auth'

export function useAuth() {
  const authStore = useAuthStore()

  return {
    authStore,
    isLoggedIn: () => Boolean(authStore.accessToken)
  }
}
