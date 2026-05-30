import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Space } from '@/types/space'
import { getSpaceList, getSpaceDetail } from '@/api/space'

export const useSpaceStore = defineStore('space', () => {
  const spaceList = ref<Space[]>([])
  const currentSpace = ref<Space | null>(null)
  const loading = ref(false)

  const currentSystemId = computed(() => currentSpace.value?.systemId || '')
  const currentSystemName = computed(() => currentSpace.value?.systemName || '')

  async function fetchSpaceList() {
    loading.value = true
    try {
      const res = await getSpaceList()
      spaceList.value = res.data || []
    } finally { loading.value = false }
  }

  async function fetchSpaceDetail(systemId: string) {
    loading.value = true
    try {
      const res = await getSpaceDetail(systemId)
      currentSpace.value = res.data
    } finally { loading.value = false }
  }

  return { spaceList, currentSpace, loading, currentSystemId, currentSystemName, fetchSpaceList, fetchSpaceDetail }
})
