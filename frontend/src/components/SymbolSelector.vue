<template>
  <div class="symbol-selector-wrap">
    <a-select
      v-model:value="model"
      show-search
      allow-clear
      mode="combobox"
      placeholder="输入 ETF 代码或名称"
      class="symbol-selector"
      option-label-prop="label"
      :filter-option="filterOption"
      @search="handleSearch"
      @change="handleChange"
      @select="handleSelect"
      @input-key-down="handleInputKeyDown"
      @dropdown-visible-change="loadIfNeeded"
    >
      <a-select-option v-for="symbol in symbols" :key="symbol.symbolCode" :label="label(symbol)" :value="symbol.symbolCode">
        <div class="symbol-option">
          <strong>{{ symbol.symbolCode }}</strong>
          <span>{{ symbol.symbolName || symbol.symbolCode }}</span>
        </div>
      </a-select-option>
    </a-select>
    <div v-if="currentSymbol" class="symbol-resolved-name">
      <span class="resolved-name">{{ currentSymbol.symbolName || currentSymbol.symbolCode }}</span>
      <span class="resolved-type">ETF</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { symbolApi } from '@/api/symbolApi'
import { feedback } from '@/utils/feedback'
import type { SymbolDTO } from '@/types'

const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value?: string] }>()

const symbols = ref<SymbolDTO[]>([])
const resolvedSymbol = ref<SymbolDTO>()
const loaded = ref(false)
const searchText = ref('')
let resolveTimer: number | undefined

const model = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const etfCodePattern = /^(510|511|512|513|515|516|517|518|519|520|521|522|560|561|562|563|588|589|159)\d{3}$/
const isEtfSymbol = (symbol: SymbolDTO) => {
  const assetType = String(symbol.assetType || '').toUpperCase()
  return assetType === 'ETF' || etfCodePattern.test(symbol.symbolCode)
}
const label = (symbol: SymbolDTO) => `${symbol.symbolCode}${symbol.symbolName ? ` · ${symbol.symbolName}` : ''}`
const currentSymbol = computed(() => {
  const code = props.modelValue?.trim()
  if (!code) return undefined
  if (resolvedSymbol.value?.symbolCode === code) return resolvedSymbol.value
  return symbols.value.find((item) => item.symbolCode === code)
})
const filterOption = (input: string, option?: { label?: string; value?: string }) => {
  const keyword = input.toLowerCase()
  return String(option?.label || option?.value || '').toLowerCase().includes(keyword)
}

const upsertSymbol = (symbol: SymbolDTO) => {
  if (!isEtfSymbol(symbol)) return
  resolvedSymbol.value = symbol
  const index = symbols.value.findIndex((item) => item.symbolCode === symbol.symbolCode)
  if (index >= 0) {
    symbols.value[index] = symbol
  } else {
    symbols.value.unshift(symbol)
  }
}

const load = async () => {
  const list = await symbolApi.listEnabled()
  symbols.value = list.filter(isEtfSymbol)
  loaded.value = true
}

const loadIfNeeded = async () => {
  if (!loaded.value) await load()
}

const submitCode = async (value?: string) => {
  const code = value?.trim()
  if (!code) {
    emit('update:modelValue', undefined)
    resolvedSymbol.value = undefined
    return
  }
  emit('update:modelValue', code)
  await resolveCurrent(code)
}

const handleSearch = (value: string) => {
  searchText.value = value
}

const handleChange = async (value?: string) => {
  searchText.value = value || ''
  await submitCode(value)
}

const handleSelect = async (value: string) => {
  searchText.value = value
  await submitCode(value)
}

const handleInputKeyDown = async (event: KeyboardEvent) => {
  if (event.key !== 'Enter') return
  const value = searchText.value || props.modelValue || ''
  await submitCode(value)
}

const resolveCurrent = async (rawCode = props.modelValue) => {
  const code = rawCode?.trim()
  if (!code || !/^\d{6}$/.test(code)) return
  try {
    const symbol = await symbolApi.resolve(code)
    if (!isEtfSymbol(symbol)) {
      feedback.warning('当前阶段仅支持 ETF，请输入 ETF 代码')
      emit('update:modelValue', undefined)
      return
    }
    upsertSymbol(symbol)
    emit('update:modelValue', symbol.symbolCode)
  } catch {
    // Resolution is best-effort; existing input remains usable.
  }
}

watch(() => props.modelValue, () => {
  window.clearTimeout(resolveTimer)
  searchText.value = props.modelValue || ''
  resolveTimer = window.setTimeout(() => resolveCurrent(), 300)
})

onMounted(async () => {
  await load()
  await resolveCurrent()
})
</script>

<style scoped>
.symbol-selector {
  width: 100%;
}

.symbol-selector :deep(.ant-select-selector) {
  min-height: 36px;
  border-radius: 11px;
  background: var(--bg-field) !important;
  border-color: rgba(129, 153, 174, 0.42) !important;
  box-shadow: none !important;
}

.symbol-selector:hover :deep(.ant-select-selector) {
  border-color: rgba(47, 128, 237, 0.45) !important;
}

.symbol-selector.ant-select-focused :deep(.ant-select-selector) {
  border-color: rgba(47, 128, 237, 0.72) !important;
  box-shadow: 0 0 0 3px rgba(47, 128, 237, 0.12) !important;
}

.symbol-selector :deep(.ant-select-selection-search-input),
.symbol-selector :deep(.ant-select-selection-item),
.symbol-selector :deep(.ant-select-selection-placeholder) {
  color: #123044;
}

.symbol-selector :deep(.ant-select-selection-placeholder) {
  color: #8aa0af;
}

.symbol-selector :deep(.ant-select-arrow),
.symbol-selector :deep(.ant-select-clear) {
  color: #7a8fa0;
  background: transparent;
}

.symbol-selector-wrap {
  display: grid;
  gap: 4px;
  width: 240px;
  max-width: 100%;
  min-width: 0;
}

.symbol-resolved-name {
  min-height: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #4d6475;
  font-size: 12px;
  line-height: 1.2;
  padding-left: 2px;
  min-width: 0;
}

.resolved-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resolved-type {
  flex: 0 0 auto;
  color: #1c5fb9;
  font-weight: 800;
}

.symbol-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.symbol-option strong {
  color: #123044;
}

.symbol-option span {
  color: #7a8fa0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
