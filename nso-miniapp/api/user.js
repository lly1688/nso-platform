import { request } from '@/utils/request.js'

export function login(data) {
  return request({
    url: '/api/user/login',
    method: 'POST',
    data
  })
}

export function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'GET'
  })
}

export function updateUserInfo(data) {
  return request({
    url: '/api/user/info',
    method: 'PUT',
    data
  })
}

export function logout() {
  return request({
    url: '/api/user/logout',
    method: 'POST'
  })
}
