import { request } from '../utils/request'

export function getTaskList(params) {
  return request({
    url: '/mp/tasks',
    data: params
  })
}
