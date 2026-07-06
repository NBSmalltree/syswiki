import request from './request'
import type { Space, CreateSpaceForm } from '@/types/space'

export function getSpaceList(scope: string = 'all') {
  return request.get<Space[]>('/spaces', { params: { scope } })
}

export function getSpaceDetail(systemId: string) {
  return request.get<Space>(`/spaces/${systemId}`)
}

export function createSpace(data: CreateSpaceForm) {
  return request.post<Space>('/spaces', data)
}

export function updateSpace(systemId: string, data: { systemName?: string; systemCode?: string; description?: string }) {
  return request.put<Space>(`/spaces/${systemId}`, data)
}

export function deleteSpace(systemId: string) {
  return request.delete(`/spaces/${systemId}`)
}

export function getSpacePermission(systemId: string) {
  return request.get<{ canEdit: boolean; isAdmin: boolean }>(`/spaces/${systemId}/permission`)
}
