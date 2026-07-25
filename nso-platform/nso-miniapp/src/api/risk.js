import { request } from '../utils/request'

export function getRiskList(params) {
  return request({
    url: '/mp/risks',
    data: params
  })
}
