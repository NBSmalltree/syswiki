import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ContentItem, ModuleType } from '@/types/content'
import { getModuleContent, saveModuleContent } from '@/api/content'

export const useContentStore = defineStore('content', () => {
  const contentCache = ref<Map<string, ContentItem>>(new Map())
  const loading = ref(false)
  const saving = ref(false)

  async function fetchContent(systemId: string, moduleType: ModuleType) {
    loading.value = true
    try {
      const res = await getModuleContent(systemId, moduleType)
      if (res.data) contentCache.value.set(moduleType, res.data)
      return res.data
    } finally { loading.value = false }
  }

  function getCachedContent(moduleType: ModuleType): string {
    return contentCache.value.get(moduleType)?.mdContent || ''
  }

  return { contentCache, loading, saving, fetchContent, getCachedContent }
})
