import request from './request'
import type { SqlItem } from '@/types/sqlLib'

export function getSqlList(systemId: string, category?: string) {
  return request.get<SqlItem[]>(`/spaces/${systemId}/sql-lib`, { params: { category } })
}

export function addSqlItem(systemId: string, data: any) {
  return request.post(`/spaces/${systemId}/sql-lib`, data)
}

export function deleteSqlItem(systemId: string, sqlId: string) {
  return request.delete(`/spaces/${systemId}/sql-lib/${sqlId}`)
}

export function renderSql(systemId: string, sqlId: string, params: Record<string, string>) {
  return request.post<{ renderedSql: string }>(`/spaces/${systemId}/sql-lib/${sqlId}/render`, { params })
}
