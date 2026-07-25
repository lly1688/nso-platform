import { request } from '@/utils/request.js'

export function getSampleList(projectId) {
  return request({
    url: `/api/sample/list/${projectId}`,
    method: 'GET'
  })
}

export function confirmSample(data) {
  return request({
    url: '/api/sample/confirm',
    method: 'POST',
    data
  })
}

export function getSampleDetail(sampleId) {
  return request({
    url: `/api/sample/detail/${sampleId}`,
    method: 'GET'
  })
}

export function submitSampleFeedback(data) {
  return request({
    url: '/api/sample/feedback',
    method: 'POST',
    data
  })
}
