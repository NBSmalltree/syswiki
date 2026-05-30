import request from './request'
import type { TopologyLink } from '@/types/topology'

export function getTopologyList(systemId: string) {
  return request.get<TopologyLink[]>(`/spaces/${systemId}/topologies`)
}

export function batchSaveTopology(systemId: string, links: any[]) {
  return request.post<TopologyLink[]>(`/spaces/${systemId}/topologies`, links)
}
