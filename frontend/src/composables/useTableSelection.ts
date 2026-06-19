import { computed, ref } from 'vue'

export const useNumberTableSelection = () => {
  const selectedIds = ref<number[]>([])

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedIds.value,
    onChange: (keys: Array<string | number>) => {
      selectedIds.value = keys.map((item) => Number(item))
    }
  }))

  const clearSelection = () => {
    selectedIds.value = []
  }

  const removeSelectedId = (id: number) => {
    selectedIds.value = selectedIds.value.filter((item) => item !== id)
  }

  const keepExistingIds = (ids: Iterable<number>) => {
    const existingIds = new Set(ids)
    selectedIds.value = selectedIds.value.filter((id) => existingIds.has(id))
  }

  return {
    selectedIds,
    rowSelection,
    clearSelection,
    removeSelectedId,
    keepExistingIds
  }
}
