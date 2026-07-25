import { request } from '../utils/request'

export function getProjectList(params) {
  return request({
    url: '/mp/projects',
    data: params
  })
}

export function getProjectDetail(id) {
  return request({
    url: `/mp/projects/${id}`
  })
}
