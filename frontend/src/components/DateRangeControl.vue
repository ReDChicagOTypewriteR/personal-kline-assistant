<template>
  <div class="date-range-control">
    <a-range-picker v-model:value="model" value-format="YYYY-MM-DD" class="date-range-picker" />
    <div class="date-quick-actions" aria-label="快捷日期区间">
      <a-button v-for="item in presetItems" :key="item.key" size="small" @click="applyPreset(item.days)">
        {{ item.label }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value?: [string, string]
  presets?: Array<{ key: string; label: string; days: number }>
}>()

const emit = defineEmits<{ 'update:value': [value: [string, string]] }>()

const presetItems = computed(() => props.presets || [
  { key: '1m', label: '近 1 月', days: 30 },
  { key: '3m', label: '近 3 月', days: 90 },
  { key: '6m', label: '近 6 月', days: 180 },
  { key: '1y', label: '近 1 年', days: 365 },
  { key: '3y', label: '近 3 年', days: 1095 }
])

const model = computed({
  get: () => props.value,
  set: (value) => {
    if (value?.[0] && value?.[1]) emit('update:value', value)
  }
})

const applyPreset = (days: number) => {
  emit('update:value', [formatDate(addDays(new Date(), -days)), formatDate(new Date())])
}

const addDays = (date: Date, days: number) => {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

const formatDate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
</script>

<style scoped>
.date-range-control {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.date-range-picker {
  width: 100%;
}

.date-quick-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.date-quick-actions :deep(.ant-btn) {
  min-height: 24px;
  padding: 0 7px;
  font-size: 12px;
}
</style>
