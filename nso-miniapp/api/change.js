import { request } from '@/utils/request.js'

export function applyChange(data) {
  return request({
    url: '/api/change/apply',
    method: 'POST',
    data
  })
}

export function getChangeDetail(changeId) {
  return request({
    url: `/api/change/detail/${changeId}`,
    method: 'GET'
  })
}

export function getChangeList(params) {
  return request({
    url: '/api/change/list',
    method: 'GET',
    data: params
  })
}

export function approveChange(data) {
  return request({
    url: '/api/change/approve',
    method: 'POST',
    data
  })
}
