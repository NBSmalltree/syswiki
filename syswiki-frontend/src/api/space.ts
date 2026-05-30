import request from './request'
import type { Space, CreateSpaceForm } from '@/types/space'

export function getSpaceList() {
  return request.get<Space[]>('/spaces')
}

export function getSpaceDetail(systemId: string) {
  return request.get<Space>(`/spaces/${systemId}`)
}

export function createSpace(data: CreateSpaceForm) {
  return request.post<Space>('/spaces', data)
}
