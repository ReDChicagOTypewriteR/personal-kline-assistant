<template>
  <div class="page import-page">
    <div class="import-grid">
      <div class="glass-card import-card">
        <div class="section-title">ETF CSV 日 K 导入</div>
        <div class="toolbar">
          <label class="import-field">
            <span>ETF</span>
            <SymbolSelector v-model="symbolCode" />
          </label>
          <a-upload :before-upload="beforeUpload" :max-count="1" accept=".csv" @remove="onFileRemove">
            <a-button>
              <template #icon><UploadOutlined /></template>
              选择 CSV 文件
            </a-button>
          </a-upload>
          <a-tooltip :title="csvDisabledReason">
            <span><a-button type="primary" :loading="loading" :disabled="!!csvDisabledReason" @click="submit">
              <template #icon><ImportOutlined /></template>
              导入
            </a-button></span>
          </a-tooltip>
        </div>
      </div>

      <div class="glass-card import-card">
        <div class="section-title">ETF 日 K 拉取</div>
        <div class="toolbar">
          <label class="import-field">
            <span>ETF</span>
            <SymbolSelector v-model="symbolCode" />
          </label>
          <label class="import-field date-field">
            <span>拉取区间</span>
            <DateRangeControl v-model:value="dateRange" :presets="importDatePresets" />
          </label>
          <label class="import-field adjust-field">
            <span>复权方式</span>
            <a-select v-model:value="adjustType" class="adjust-select">
              <a-select-option :value="1">前复权</a-select-option>
              <a-select-option :value="0">不复权</a-select-option>
              <a-select-option :value="2">后复权</a-select-option>
            </a-select>
          </label>
          <a-tooltip :title="!symbolCode ? '请先选择 ETF' : ''">
            <span><a-button type="primary" :loading="fetchLoading" :disabled="!symbolCode" @click="fetchEastMoney">
              <template #icon><CloudDownloadOutlined /></template>
              拉取并导入
            </a-button></span>
          </a-tooltip>
        </div>
      </div>
    </div>

    <div v-if="result" class="glass-card result-card">
      <div class="section-title">导入结果</div>
      <div class="result-grid">
        <div><span>ETF</span><strong>{{ result.symbolCode }}</strong></div>
        <div><span>总行数</span><strong>{{ result.totalRows }}</strong></div>
        <div><span>成功</span><strong class="value-up">{{ result.successRows }}</strong></div>
        <div><span>失败</span><strong class="value-down">{{ result.failedRows }}</strong></div>
        <div><span>状态</span><strong>{{ result.message }}</strong></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { CloudDownloadOutlined, ImportOutlined, UploadOutlined } from '@ant-design/icons-vue'
import SymbolSelector from '@/components/SymbolSelector.vue'
import DateRangeControl from '@/components/DateRangeControl.vue'
import { klineApi } from '@/api/klineApi'
import { feedback } from '@/utils/feedback'
import type { ImportKlineResultDTO } from '@/types'

const symbolCode = ref('')
const dateRange = ref<[string, string]>(['2024-01-01', '2026-12-31'])
const importDatePresets = [
  { key: '1y', label: '近 1 年', days: 365 },
  { key: '3y', label: '近 3 年', days: 1095 },
  { key: '5y', label: '近 5 年', days: 1825 }
]
const adjustType = ref(1)
const file = ref<File>()
const result = ref<ImportKlineResultDTO>()
const loading = ref(false)
const fetchLoading = ref(false)
const csvDisabledReason = computed(() => {
  if (!symbolCode.value) return '请先选择 ETF'
  if (!file.value) return '请先选择 CSV 文件'
  return ''
})

const beforeUpload = (uploadFile: File) => {
  file.value = uploadFile
  return false
}

const onFileRemove = () => {
  file.value = undefined
}

const submit = async () => {
  if (!symbolCode.value || !file.value) return
  loading.value = true
  try {
    result.value = await klineApi.importDaily(symbolCode.value, file.value)
    feedback.success('导入完成')
  } finally {
    loading.value = false
  }
}

const fetchEastMoney = async () => {
  if (!symbolCode.value) return
  fetchLoading.value = true
  try {
    const [start, end] = dateRange.value || []
    result.value = await klineApi.fetchEastMoney(symbolCode.value, symbolCode.value, start, end, adjustType.value)
    feedback.success('ETF 日 K 导入完成')
  } finally {
    fetchLoading.value = false
  }
}
</script>

<style scoped>
.import-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.import-card,
.result-card {
  padding: 10px;
}

.import-card .toolbar {
  align-items: flex-end;
}

.import-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.import-field > span {
  color: #c8d1dc;
  font-size: 12px;
  font-weight: 800;
}

.import-card :deep(.symbol-selector-wrap) {
  flex: 1 1 230px;
}

.import-card :deep(.ant-upload-wrapper) {
  flex: 0 0 auto;
}

.import-card :deep(.ant-btn),
.import-card :deep(.ant-input),
.import-card :deep(.ant-picker),
.import-card :deep(.ant-select-selector) {
  min-height: 32px;
  border-radius: 10px;
}

.import-card :deep(.ant-input),
.import-card :deep(.ant-picker),
.import-card :deep(.ant-select-selector) {
  color: #123044;
  background: rgba(255, 255, 255, 0.94) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}

.import-card :deep(.ant-input::placeholder),
.import-card :deep(.ant-picker-input input::placeholder) {
  color: #aeb8c5;
}

.import-card :deep(.ant-picker-input input),
.import-card :deep(.ant-select-selection-item),
.import-card :deep(.ant-picker-suffix),
.import-card :deep(.ant-select-arrow) {
  color: #123044;
}

.date-field {
  flex: 1 1 300px;
}

.adjust-field {
  width: 130px;
  flex: 0 1 140px;
}

.adjust-select {
  width: 100%;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.result-grid > div {
  min-height: 68px;
  padding: 10px;
  border-radius: 12px;
  background: rgba(246, 250, 253, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.result-grid span {
  display: block;
  color: #c8d1dc;
  font-size: 12px;
}

.result-grid strong {
  display: block;
  color: #123044;
  font-size: 17px;
  margin-top: 6px;
}

@media (max-width: 1280px) {
  .import-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 780px) {
  .import-card .toolbar > *,
  .import-card :deep(.symbol-selector-wrap),
  .date-field,
  .adjust-field {
    width: 100%;
    flex-basis: 100%;
  }
}
</style>
