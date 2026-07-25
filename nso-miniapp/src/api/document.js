import { request } from '../utils/request'

export function getDocumentList(params) {
  return request({
    url: '/mp/documents',
    data: params
  })
}
