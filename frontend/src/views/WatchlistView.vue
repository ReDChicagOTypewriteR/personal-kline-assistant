<template>
  <div class="page watchlist-page">
    <div class="glass-card form-card compact-card">
      <div>
        <div class="section-title">添加 ETF</div>
      </div>
      <div class="toolbar compact-toolbar">
        <SymbolSelector v-model="symbolCode" />
        <a-tooltip :title="!symbolCode ? '请先输入或选择 ETF' : ''">
          <span><a-button type="primary" :disabled="!symbolCode" @click="addWatch(symbolCode)">
            <template #icon><StarOutlined /></template>
            加入 ETF 自选池
          </a-button></span>
        </a-tooltip>
      </div>
    </div>

    <div class="watchlist-grid">
      <div class="glass-card table-card list-card">
        <div class="table-head">
          <div>
            <div class="section-title">ETF 标的池</div>
          </div>
          <div class="table-summary"><span class="table-count-pill">{{ symbols.length }} 条</span></div>
        </div>
        <a-table
          class="app-table"
          :data-source="symbols"
          :loading="loading"
          :pagination="false"
          row-key="symbolCode"
          size="middle"
          :scroll="{ y: 560, x: 980 }"
        >
          <a-table-column data-index="symbolCode" title="代码" :width="120" fixed="left" />
          <a-table-column data-index="symbolName" title="名称" :width="190" />
          <a-table-column data-index="market" title="市场" :width="90" />
          <a-table-column data-index="assetType" title="资产类型" :width="100" />
          <a-table-column data-index="currency" title="货币" :width="90" />
          <a-table-column title="状态" :width="90">
            <template #default="{ record }">
              <span :class="record.enabled === 1 ? 'value-up' : 'value-down'">{{ record.enabled === 1 ? '启用' : '禁用' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="330" fixed="right">
            <template #default="{ record }">
              <div class="table-actions">
                <a-button size="small" @click="addWatch(record.symbolCode)">
                  <template #icon><StarOutlined /></template>
                  加入 ETF
                </a-button>
                <a-button size="small" v-if="record.enabled === 0" @click="setEnabled(record.symbolCode, true)">
                  <template #icon><SelectOutlined /></template>
                  启用
                </a-button>
                <a-button size="small" v-else class="warning-button" @click="setEnabled(record.symbolCode, false)">
                  <template #icon><ClearOutlined /></template>
                  禁用
                </a-button>
                <a-button size="small" danger @click="deleteSymbol(record)">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </div>
            </template>
          </a-table-column>
        </a-table>
      </div>

      <div class="glass-card table-card list-card">
        <div class="table-head">
          <div>
            <div class="section-title">ETF 自选池</div>
          </div>
          <div class="table-summary">
            <span class="table-count-pill">{{ watchlist.length }} 条</span>
            <a-button size="small" danger :disabled="!selectedWatchIds.length" @click="removeSelectedWatch">
              <template #icon><DeleteOutlined /></template>
              批量移除
            </a-button>
          </div>
        </div>
        <a-table
          class="app-table"
          :data-source="watchlist"
          :pagination="false"
          row-key="id"
          size="middle"
          :row-selection="watchRowSelection"
          :scroll="{ y: 560, x: 620 }"
        >
          <a-table-column data-index="symbolCode" title="代码" :width="130" />
          <a-table-column data-index="groupName" title="分组" :width="160" />
          <a-table-column data-index="priority" title="优先级" :width="100" />
          <a-table-column title="操作" :width="120">
            <template #default="{ record }">
              <a-button size="small" danger @click="removeWatch(record.id)">
                <template #icon><DeleteOutlined /></template>
                移除
              </a-button>
            </template>
          </a-table-column>
        </a-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ClearOutlined, DeleteOutlined, SelectOutlined, StarOutlined } from '@ant-design/icons-vue'
import SymbolSelector from '@/components/SymbolSelector.vue'
import { symbolApi } from '@/api/symbolApi'
import { watchlistApi } from '@/api/watchlistApi'
import { useNumberTableSelection } from '@/composables/useTableSelection'
import { confirmAction, feedback } from '@/utils/feedback'
import type { SymbolDTO, WatchlistDTO } from '@/types'

const symbols = ref<SymbolDTO[]>([])
const watchlist = ref<WatchlistDTO[]>([])
const loading = ref(false)
const symbolCode = ref('')
const { selectedIds: selectedWatchIds, rowSelection: watchRowSelection, clearSelection: clearWatchSelection } = useNumberTableSelection()

const etfCodePattern = /^(510|511|512|513|515|516|517|518|519|520|521|522|560|561|562|563|588|589|159)\d{3}$/
const isEtfSymbol = (symbol: SymbolDTO) => String(symbol.assetType || '').toUpperCase() === 'ETF' || etfCodePattern.test(symbol.symbolCode)

const load = async () => {
  loading.value = true
  try {
    const [allSymbols, allWatchlist] = await Promise.all([symbolApi.list(), watchlistApi.list()])
    symbols.value = allSymbols.filter(isEtfSymbol)
    const etfCodes = new Set(symbols.value.map((item) => item.symbolCode))
    watchlist.value = allWatchlist.filter((item) => etfCodes.has(item.symbolCode))
  } finally {
    loading.value = false
  }
}

const addWatch = async (symbolCode: string) => {
  await watchlistApi.add(symbolCode)
  feedback.success('已加入 ETF 自选池')
  await load()
}

const removeWatch = async (id: number) => {
  await watchlistApi.removeById(id)
  feedback.success('已移除')
  await load()
}

const removeSelectedWatch = async () => {
  if (!selectedWatchIds.value.length) return
  await confirmAction({
    title: '批量移除',
    content: `确认移除选中的 ${selectedWatchIds.value.length} 条自选记录吗？`,
    okText: '移除',
    danger: true
  })
  const result = await watchlistApi.removeBatch(selectedWatchIds.value)
  feedback.success(`已移除 ${result.count} 条`)
  clearWatchSelection()
  await load()
}

const setEnabled = async (symbolCode: string, enabled: boolean) => {
  enabled ? await symbolApi.enable(symbolCode) : await symbolApi.disable(symbolCode)
  await load()
}

const deleteSymbol = async (row: SymbolDTO) => {
  await confirmAction({
    title: '删除 ETF',
    content: `确认删除 ${row.symbolCode} ${row.symbolName || ''} 吗？该操作会清理它的自选、K线、指标、信号、AI分析和交易日志。`,
    okText: '删除',
    danger: true
  })
  await symbolApi.remove(row.symbolCode)
  feedback.success('ETF 已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.form-card,
.list-card {
  padding: 10px;
}

.compact-card {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}

.compact-card p {
  margin: -4px 0 0;
}

.compact-toolbar {
  align-items: flex-end;
}

.compact-toolbar :deep(.symbol-selector-wrap) {
  flex: 1 1 240px;
}

.compact-toolbar :deep(.ant-btn),
.list-card :deep(.ant-btn) {
  margin-left: 0;
}

.compact-toolbar > span {
  flex: 0 0 auto;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.warning-button {
  color: #090909;
  background: linear-gradient(135deg, #facc15, #fde68a);
  border-color: transparent;
}

.warning-button:hover,
.warning-button:focus {
  color: #090909;
  border-color: transparent;
  filter: brightness(1.03);
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

.watchlist-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(420px, 0.75fr);
  gap: 10px;
}

@media (max-width: 1280px) {
  .watchlist-grid {
    grid-template-columns: 1fr;
  }

  .compact-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .compact-toolbar,
  .compact-toolbar :deep(.symbol-selector-wrap),
  .compact-toolbar :deep(.ant-btn) {
    width: 100%;
  }
}
</style>
