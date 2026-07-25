import { request } from '../utils/request'

export function getChangeList(params) {
  return request({
    url: '/mp/changes',
    data: params
  })
}
