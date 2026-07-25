import { request } from '../utils/request'

export function getMessageList(params) {
  return request({
    url: '/mp/messages',
    data: params
  })
}
