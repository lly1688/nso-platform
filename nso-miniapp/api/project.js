import { request } from '@/utils/request.js'

export function getProjectList(params) {
  return request({
    url: '/api/project/list',
    method: 'GET',
    data: params
  })
}

export function getProjectDetail(id) {
  return request({
    url: `/api/project/detail/${id}`,
    method: 'GET'
  })
}

export function createProject(data) {
  return request({
    url: '/api/project/create',
    method: 'POST',
    data
  })
}

export function updateProject(data) {
  return request({
    url: '/api/project/update',
    method: 'PUT',
    data
  })
}
