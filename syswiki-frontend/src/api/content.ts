import request from './request'
import type { ContentItem, ContentSaveDTO, ContentVersion, ModuleType } from '@/types/content'

export function listContents(systemId: string) {
  return request.get<ContentItem[]>(`/spaces/${systemId}/contents`)
}

export function getModuleContent(systemId: string, moduleType: string) {
  return request.get<ContentItem>(`/spaces/${systemId}/contents/${moduleType}`)
}

export function saveModuleContent(systemId: string, moduleType: string, data: ContentSaveDTO) {
  return request.put<ContentItem>(`/spaces/${systemId}/contents/${moduleType}`, data)
}

export function importMarkdown(systemId: string, file: File, operator: string) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('operator', operator)
  return request.post(`/spaces/${systemId}/contents/import`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getVersionHistory(systemId: string, moduleType: string) {
  return request.get(`/spaces/${systemId}/contents/${moduleType}/versions`)
}
