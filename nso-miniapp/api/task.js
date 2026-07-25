import { request } from '@/utils/request.js'

export function getTaskList(params) {
  return request({
    url: '/api/task/list',
    method: 'GET',
    data: params
  })
}

export function getTaskDetail(taskId) {
  return request({
    url: `/api/task/detail/${taskId}`,
    method: 'GET'
  })
}

export function acceptTask(taskId) {
  return request({
    url: `/api/task/accept/${taskId}`,
    method: 'POST'
  })
}

export function completeTask(data) {
  return request({
    url: '/api/task/complete',
    method: 'POST',
    data
  })
}
