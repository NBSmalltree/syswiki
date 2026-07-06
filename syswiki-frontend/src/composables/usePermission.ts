import { computed, toValue, type Ref } from 'vue'
import { usePermissionStore } from '@/stores/permission'

/**
 * Composable for accessing per-space permissions with automatic caching.
 *
 * Permissions are cached for 5 minutes per systemId to avoid redundant API calls.
 *
 * Usage:
 * ```ts
 * const { fetchPermission, canEdit, clearCache } = usePermission()
 * await fetchPermission('abc-123')
 * canEdit('abc-123') // reactive computed
 * ```
 */
export function usePermission() {
  const permissionStore = usePermissionStore()

  /**
   * Fetch permission for a single systemId.
   * Returns cached value if available and not expired.
   */
  async function fetchPermission(systemId: string) {
    return await permissionStore.fetchPermission(systemId)
  }

  /**
   * Fetch permissions for multiple systemIds in parallel.
   * Only fetches from API for IDs not already cached.
   */
  async function fetchPermissions(systemIds: string[]) {
    return await permissionStore.fetchPermissions(systemIds)
  }

  /**
   * Reactive check: returns a computed that reflects whether the user can edit
   * the given system. Must call fetchPermission first to populate cache.
   * Accepts a plain string or a Ref<string> for reactive systemId.
   */
  function canEdit(systemId: string | Ref<string>) {
    return computed(() => {
      const id = toValue(systemId)
      const perm = permissionStore.getPermission(id)
      return perm?.canEdit || false
    })
  }

  /**
   * Reactive check: returns a computed for whether the user is admin of a system.
   * Accepts a plain string or a Ref<string> for reactive systemId.
   */
  function isAdmin(systemId: string | Ref<string>) {
    return computed(() => {
      const id = toValue(systemId)
      const perm = permissionStore.getPermission(id)
      return perm?.isAdmin || false
    })
  }

  /**
   * Get permission synchronously from cache (non-reactive).
   * Returns null if not cached or expired.
   */
  function getCachedPermission(systemId: string) {
    return permissionStore.getPermission(systemId)
  }

  /**
   * Invalidate the cache for a specific systemId.
   * Useful after changing roles or memberships.
   */
  function invalidate(systemId: string) {
    permissionStore.invalidate(systemId)
  }

  /**
   * Clear all permission cache entries.
   */
  function clearCache() {
    permissionStore.clearCache()
  }

  return {
    fetchPermission,
    fetchPermissions,
    canEdit,
    isAdmin,
    getCachedPermission,
    invalidate,
    clearCache,
  }
}
