import { request } from '../utils/request'

export function getSampleList(params) {
  return request({
    url: '/mp/samples',
    data: params
  })
}
