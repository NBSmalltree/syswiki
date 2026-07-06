import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSpacePermission } from '@/api/space'

export interface SpacePermission {
  canEdit: boolean
  isAdmin: boolean
}

interface CacheEntry {
  data: SpacePermission
  timestamp: number
}

export const usePermissionStore = defineStore('permission', () => {
  // systemId -> CacheEntry
  const cache = ref<Map<string, CacheEntry>>(new Map())

  // Cache TTL: 5 minutes (permissions don't change frequently)
  const CACHE_TTL = 5 * 60 * 1000

  function getCached(systemId: string): SpacePermission | null {
    const entry = cache.value.get(systemId)
    if (!entry) return null
    // Check if cache has expired
    if (Date.now() - entry.timestamp > CACHE_TTL) {
      cache.value.delete(systemId)
      return null
    }
    return entry.data
  }

  async function fetchPermission(systemId: string): Promise<SpacePermission> {
    // Check cache first
    const cached = getCached(systemId)
    if (cached) return cached

    try {
      const res = await getSpacePermission(systemId)
      const data: SpacePermission = res.data || { canEdit: false, isAdmin: false }
      // Store in cache
      cache.value.set(systemId, { data, timestamp: Date.now() })
      return data
    } catch {
      // Cache the default value too to avoid repeated failed requests
      const defaultPerm: SpacePermission = { canEdit: false, isAdmin: false }
      cache.value.set(systemId, { data: defaultPerm, timestamp: Date.now() })
      return defaultPerm
    }
  }

  async function fetchPermissions(systemIds: string[]): Promise<Map<string, SpacePermission>> {
    const results = new Map<string, SpacePermission>()
    // Only fetch for IDs not in cache
    const toFetch = systemIds.filter((id) => !getCached(id))

    if (toFetch.length > 0) {
      const promises = toFetch.map(async (id) => {
        const perm = await fetchPermission(id)
        results.set(id, perm)
      })
      await Promise.all(promises)
    }

    // Fill in already-cached results
    for (const id of systemIds) {
      const cached = getCached(id)
      if (cached) results.set(id, cached)
    }

    return results
  }

  function getPermission(systemId: string): SpacePermission | null {
    return getCached(systemId)
  }

  function invalidate(systemId: string) {
    cache.value.delete(systemId)
  }

  function clearCache() {
    cache.value.clear()
  }

  return {
    cache,
    fetchPermission,
    fetchPermissions,
    getPermission,
    invalidate,
    clearCache,
  }
})
