const TOKEN_KEY = 'nso_token'
const USER_KEY = 'nso_user'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY)
}

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

export function getUserInfo() {
  return uni.getStorageSync(USER_KEY)
}

export function setUserInfo(user) {
  uni.setStorageSync(USER_KEY, user)
}

export function removeUserInfo() {
  uni.removeStorageSync(USER_KEY)
}

export function isLogin() {
  return !!getToken()
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
}
