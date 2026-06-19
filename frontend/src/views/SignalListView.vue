<template>
  <div class="page">
    <div class="glass-card signal-filter">
      <div>
        <div class="section-title">信号雷达</div>
      </div>
      <div class="toolbar">
        <a-input v-model:value="keyword" allow-clear placeholder="搜索 ETF 代码" class="filter-input" />
        <a-select v-model:value="signalType" allow-clear placeholder="按信号筛选" class="filter-input">
          <a-select-option value="BUY_CANDIDATE">买入候选</a-select-option>
          <a-select-option value="WATCH">观察</a-select-option>
          <a-select-option value="NEUTRAL">中性</a-select-option>
          <a-select-option value="AVOID">规避</a-select-option>
          <a-select-option value="SELL_WARNING">卖出预警</a-select-option>
        </a-select>
        <a-button type="primary" @click="load">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </div>
    </div>

    <div class="glass-card table-card signal-list-card">
      <div class="table-head">
        <div>
          <div class="section-title">最新技术信号</div>
        </div>
        <div class="table-summary">
          <span class="table-count-pill">{{ filtered.length }} / {{ signals.length }} 条</span>
        </div>
      </div>
      <a-table
        class="app-table"
        :data-source="filtered"
        :loading="loading"
        :pagination="false"
        :custom-row="signalRowProps"
        row-key="symbolCode"
        size="middle"
        :scroll="{ y: 680, x: 1560 }"
      >
        <a-table-column data-index="symbolCode" title="标的" :width="120" fixed="left" />
        <a-table-column data-index="tradeDate" title="日期" :width="120" />
        <a-table-column title="信号" :width="150">
          <template #default="{ record }"><SignalBadge :signal-type="record.signalType" /></template>
        </a-table-column>
        <a-table-column data-index="signalLevel" title="级别" :width="100" />
        <a-table-column data-index="trendState" title="趋势" :width="110" />
        <a-table-column title="评分" :width="90">
          <template #default="{ record }"><strong class="value-blue">{{ record.technicalScore }}</strong></template>
        </a-table-column>
        <a-table-column title="RSI" :width="90">
          <template #default="{ record }">{{ fmt(record.rsi14) }}</template>
        </a-table-column>
        <a-table-column title="MA20" :width="100">
          <template #default="{ record }">{{ fmt(record.ma20) }}</template>
        </a-table-column>
        <a-table-column title="量比" :width="90">
          <template #default="{ record }">{{ fmt(record.volumeRatio) }}</template>
        </a-table-column>
        <a-table-column title="原因" :width="320">
          <template #default="{ record }">{{ record.reasons.join('；') || '-' }}</template>
        </a-table-column>
        <a-table-column title="风险提示" :width="300">
          <template #default="{ record }">{{ record.riskNotes.join('；') || '-' }}</template>
        </a-table-column>
        <a-table-column title="操作" :width="100" fixed="right">
          <template #default="{ record }">
            <a-button size="small" danger @click.stop="deleteSignal(record)">删除</a-button>
          </template>
        </a-table-column>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { signalApi } from '@/api/signalApi'
import SignalBadge from '@/components/SignalBadge.vue'
import { confirmAction, feedback } from '@/utils/feedback'
import { formatNumber } from '@/utils/formatters'
import type { TechnicalSignalDTO } from '@/types'

const router = useRouter()
const signals = ref<TechnicalSignalDTO[]>([])
const keyword = ref('')
const signalType = ref('')
const loading = ref(false)
const etfCodePattern = /^(510|511|512|513|515|516|517|518|519|520|521|522|560|561|562|563|588|589|159)\d{3}$/

const filtered = computed(() => signals.value.filter((item) => {
  const matchedSymbol = !keyword.value || item.symbolCode.toLowerCase().includes(keyword.value.toLowerCase())
  const matchedSignal = !signalType.value || item.signalType === signalType.value
  return matchedSymbol && matchedSignal
}))

const load = async () => {
  loading.value = true
  try {
    const list = await signalApi.latest()
    signals.value = list.filter((item) => etfCodePattern.test(item.symbolCode))
  } finally {
    loading.value = false
  }
}

const goDetail = (row: TechnicalSignalDTO) => {
  router.push(`/kline-detail/${row.symbolCode}`)
}

const deleteSignal = async (row: TechnicalSignalDTO) => {
  try {
    await confirmAction({
      title: '删除技术信号',
      content: `确认删除 ${row.symbolCode} 在 ${row.tradeDate} 的技术信号吗？相关最终决策也会同步清理。`,
      okText: '删除',
      danger: true
    })
  } catch {
    return
  }
  const result = await signalApi.delete(row.symbolCode, row.tradeDate)
  feedback.success(`已删除 ${result.count} 条技术信号`)
  await load()
}

const signalRowProps = (row: TechnicalSignalDTO) => ({
  class: 'clickable-signal-row',
  onClick: () => goDetail(row)
})

const fmt = formatNumber

onMounted(load)
</script>

<style scoped>
.signal-filter,
.signal-list-card {
  padding: 10px;
}

.signal-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.signal-filter p {
  margin: -4px 0 0;
}

.filter-input {
  width: 220px;
  flex: 1 1 220px;
}

.signal-list-card :deep(.clickable-signal-row) {
  cursor: pointer;
}

.table-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.table-head p {
  margin: -4px 0 0;
  line-height: 1.35;
}

@media (max-width: 900px) {
  .signal-filter {
    align-items: flex-start;
    flex-direction: column;
  }

  .signal-filter .toolbar,
  .filter-input,
  .signal-filter :deep(.ant-btn) {
    width: 100%;
  }

  .table-head {
    flex-direction: column;
  }
}
</style>
