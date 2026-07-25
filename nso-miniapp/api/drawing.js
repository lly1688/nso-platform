import { request } from '@/utils/request.js'

export function getDrawingVersions(projectId) {
  return request({
    url: `/api/drawing/versions/${projectId}`,
    method: 'GET'
  })
}

export function getDrawingDetail(versionId) {
  return request({
    url: `/api/drawing/detail/${versionId}`,
    method: 'GET'
  })
}

export function uploadDrawing(data) {
  return request({
    url: '/api/drawing/upload',
    method: 'POST',
    data
  })
}

export function approveDrawing(data) {
  return request({
    url: '/api/drawing/approve',
    method: 'POST',
    data
  })
}
