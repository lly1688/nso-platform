import { request } from '../utils/request'

export function login(data) {
  return request({
    url: '/mp/auth/login',
    method: 'POST',
    data
  })
}

export function getProfile() {
  return request({
    url: '/mp/auth/profile'
  })
}
