<template>
  <div class="page journal-page">
    <div class="glass-card journal-actions">
      <div>
        <div class="section-title">交易日志</div>
        <p>系统生成信号后会自动写入日志，你只需要在“复盘”里记录是否执行、执行价格和后续结果。</p>
      </div>
      <div class="toolbar">
        <SymbolSelector v-model="filterSymbol" />
        <a-button type="primary" :loading="loading" @click="load">
          <template #icon><ReloadOutlined /></template>
          刷新日志
        </a-button>
        <a-button danger :disabled="!selectedIds.length" @click="deleteSelected">
          <template #icon><DeleteOutlined /></template>
          批量删除
        </a-button>
        <a-button @click="clearFilter">
          <template #icon><EyeOutlined /></template>
          查看全部
        </a-button>
      </div>
    </div>

    <div class="glass-card journal-guide">
      <div class="guide-step">
        <strong>1. 自动记录</strong>
        <span>生成技术信号或 AI 最终决策后，系统保存当天 K 线状态、评分、AI 风险和决策。</span>
      </div>
      <div class="guide-step">
        <strong>2. 手动复盘</strong>
        <span>点击“复盘”，填写你是否执行、执行日期、执行价格和原因。</span>
      </div>
      <div class="guide-step">
        <strong>3. 追踪结果</strong>
        <span>后续补充复盘价格，系统计算后续收益，方便判断信号质量。</span>
      </div>
    </div>

    <div class="glass-card table-card journal-table-card">
      <div class="table-head">
        <div>
          <div class="section-title">复盘记录</div>
        </div>
        <div class="table-summary">
          <span class="table-count-pill">{{ rows.length }} 条</span>
          <span v-if="selectedIds.length" class="table-count-pill">已选 {{ selectedIds.length }}</span>
        </div>
      </div>
      <a-table
        class="app-table journal-table"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
        :row-selection="journalRowSelection"
        :scroll="{ y: 680, x: 2520 }"
      >
        <a-table-column data-index="symbolCode" title="标的" :width="110" fixed="left" />
        <a-table-column data-index="tradeDate" title="日期" :width="120" />
        <a-table-column title="信号" :width="150">
          <template #default="{ record }"><SignalBadge :signal-type="record.signalType" /></template>
        </a-table-column>
        <a-table-column data-index="trendState" title="K线状态" :width="110" />
        <a-table-column title="收盘价" :width="100"><template #default="{ record }">{{ fmt(record.closePrice) }}</template></a-table-column>
        <a-table-column title="MA20" :width="100"><template #default="{ record }">{{ fmt(record.ma20) }}</template></a-table-column>
        <a-table-column title="RSI" :width="90"><template #default="{ record }">{{ fmt(record.rsi14) }}</template></a-table-column>
        <a-table-column title="技术评分" :width="100"><template #default="{ record }"><strong class="value-blue">{{ record.technicalScore ?? '-' }}</strong></template></a-table-column>
        <a-table-column title="AI风险" :width="100"><template #default="{ record }">{{ record.aiRiskScore ?? '-' }}</template></a-table-column>
        <a-table-column title="最终决策" :width="160">
          <template #default="{ record }">
            <SignalBadge v-if="record.finalAction" :signal-type="record.finalAction" />
            <span v-else>-</span>
          </template>
        </a-table-column>
        <a-table-column title="是否执行" :width="100">
          <template #default="{ record }">
            <a-tag :class="record.executed ? 'executed-tag' : 'pending-tag'">{{ record.executed ? '已执行' : '未执行' }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="执行价格" :width="110"><template #default="{ record }">{{ fmt(record.executionPrice) }}</template></a-table-column>
        <a-table-column title="后续结果" :width="120">
          <template #default="{ record }">
            <a-tag :class="followUpTagClass(record.followUpStatus)">{{ followUpLabel(record.followUpStatus) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="后续收益" :width="110">
          <template #default="{ record }">
            <span :class="Number(record.followUpReturnRate || 0) >= 0 ? 'value-up' : 'value-down'">{{ pct(record.followUpReturnRate) }}</span>
          </template>
        </a-table-column>
        <a-table-column data-index="signalReason" title="信号原因" :width="300" />
        <a-table-column data-index="decisionReason" title="决策原因" :width="300" />
        <a-table-column title="操作" :width="170" fixed="right">
          <template #default="{ record }">
            <div class="row-actions">
              <a-button size="small" @click="openEdit(record)">
                <template #icon><EditOutlined /></template>
                复盘
              </a-button>
              <a-button size="small" danger @click="deleteRow(record)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </div>
          </template>
        </a-table-column>
      </a-table>
    </div>

    <a-modal v-model:open="dialogVisible" title="填写交易复盘" width="560px" class="journal-modal">
      <div class="journal-form">
        <label class="journal-field">
          <span>是否执行</span>
          <a-switch v-model:checked="form.executed" checked-children="已执行" un-checked-children="未执行" />
        </label>
        <div class="form-grid">
          <label class="journal-field">
            <span>执行日期</span>
            <a-date-picker v-model:value="form.executionDate" value-format="YYYY-MM-DD" placeholder="选择执行日期" />
          </label>
          <label class="journal-field">
            <span>执行价格</span>
            <a-input-number v-model:value="form.executionPrice" :min="0" :precision="4" :step="0.01" />
          </label>
          <label class="journal-field">
            <span>后续日期</span>
            <a-date-picker v-model:value="form.followUpDate" value-format="YYYY-MM-DD" placeholder="选择复盘日期" />
          </label>
          <label class="journal-field">
            <span>后续价格</span>
            <a-input-number v-model:value="form.followUpPrice" :min="0" :precision="4" :step="0.01" />
          </label>
        </div>
        <label class="journal-field">
          <span>后续结果</span>
          <a-select v-model:value="form.followUpStatus">
            <a-select-option value="PENDING">待观察</a-select-option>
            <a-select-option value="WIN">符合预期</a-select-option>
            <a-select-option value="LOSS">不符合预期</a-select-option>
            <a-select-option value="FLAT">基本持平</a-select-option>
            <a-select-option value="INVALID">信号失效</a-select-option>
          </a-select>
        </label>
        <label class="journal-field">
          <span>执行备注</span>
          <a-textarea v-model:value="form.executionNote" :rows="3" placeholder="记录为什么执行或不执行" />
        </label>
        <label class="journal-field">
          <span>后续复盘</span>
          <a-textarea v-model:value="form.followUpNote" :rows="3" placeholder="记录后续走势、是否符合预期、改进点" />
        </label>
      </div>
      <template #footer>
        <a-button @click="dialogVisible = false">
          <template #icon><ClearOutlined /></template>
          取消
        </a-button>
        <a-button type="primary" :loading="saving" @click="save">
          <template #icon><SaveOutlined /></template>
          保存复盘
        </a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ClearOutlined, DeleteOutlined, EditOutlined, EyeOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons-vue'
import SignalBadge from '@/components/SignalBadge.vue'
import SymbolSelector from '@/components/SymbolSelector.vue'
import { tradeJournalApi } from '@/api/tradeJournalApi'
import { useNumberTableSelection } from '@/composables/useTableSelection'
import { confirmAction, feedback } from '@/utils/feedback'
import { formatNumber, formatPercent } from '@/utils/formatters'
import { followUpLabel, followUpTagClass } from '@/utils/labels'
import type { TradeJournalDTO, UpdateTradeJournalRequest } from '@/types'

const rows = ref<TradeJournalDTO[]>([])
const filterSymbol = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const { selectedIds, rowSelection: journalRowSelection, clearSelection } = useNumberTableSelection()
const form = reactive<UpdateTradeJournalRequest>({
  executed: false,
  followUpStatus: 'PENDING'
})

const load = async () => {
  loading.value = true
  try {
    rows.value = filterSymbol.value
      ? await tradeJournalApi.history(filterSymbol.value)
      : await tradeJournalApi.latest(200)
  } finally {
    loading.value = false
  }
}

const clearFilter = async () => {
  filterSymbol.value = ''
  await load()
}

const openEdit = (row: TradeJournalDTO) => {
  editingId.value = row.id
  form.executed = row.executed
  form.executionDate = row.executionDate
  form.executionPrice = row.executionPrice
  form.executionNote = row.executionNote
  form.followUpStatus = row.followUpStatus || 'PENDING'
  form.followUpDate = row.followUpDate
  form.followUpPrice = row.followUpPrice
  form.followUpNote = row.followUpNote
  dialogVisible.value = true
}

const save = async () => {
  if (!editingId.value) return
  saving.value = true
  try {
    await tradeJournalApi.update(editingId.value, { ...form })
    feedback.success('交易日志已更新')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const deleteRow = async (row: TradeJournalDTO) => {
  await confirmAction({
    title: '删除交易日志',
    content: `确认删除 ${row.symbolCode} 在 ${row.tradeDate} 的交易日志吗？`,
    okText: '删除',
    danger: true
  })
  await tradeJournalApi.remove(row.id)
  feedback.success('交易日志已删除')
  await load()
}

const deleteSelected = async () => {
  if (!selectedIds.value.length) return
  await confirmAction({
    title: '批量删除',
    content: `确认删除选中的 ${selectedIds.value.length} 条交易日志吗？`,
    okText: '删除',
    danger: true
  })
  const result = await tradeJournalApi.removeBatch(selectedIds.value)
  feedback.success(`已删除 ${result.count} 条交易日志`)
  clearSelection()
  await load()
}

const fmt = formatNumber
const pct = formatPercent

onMounted(load)
</script>

<style scoped>
.journal-actions,
.journal-guide,
.journal-table-card {
  padding: 10px;
}

.journal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.journal-actions p {
  margin: -4px 0 0;
}

.journal-guide {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.guide-step {
  min-width: 0;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid rgba(129, 153, 174, 0.28);
  background: rgba(246, 250, 253, 0.86);
}

.guide-step strong {
  display: block;
  color: #123044;
  margin-bottom: 4px;
}

.guide-step span {
  color: #5c7182;
  line-height: 1.45;
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

.journal-actions .toolbar {
  justify-content: flex-end;
}

.journal-actions :deep(.symbol-selector-wrap) {
  flex: 1 1 240px;
}

.row-actions {
  display: flex;
  gap: 6px;
}

.row-actions :deep(.ant-btn) {
  margin-left: 0;
}

.journal-form {
  display: grid;
  gap: 10px;
}

.journal-field {
  display: grid;
  gap: 4px;
}

.journal-field span {
  color: #7a8fa0;
  font-size: 12px;
  font-weight: 700;
}

.journal-form :deep(.ant-input-number),
.journal-form :deep(.ant-picker),
.journal-form :deep(.ant-select),
.journal-form :deep(.ant-textarea),
.journal-form :deep(.ant-input) {
  width: 100%;
}

.journal-form :deep(.ant-input-number),
.journal-form :deep(.ant-picker),
.journal-form :deep(.ant-select-selector),
.journal-form :deep(.ant-input) {
  min-height: 32px;
  border-radius: 10px;
  color: #123044;
  background: rgba(255, 255, 255, 0.94) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}

.journal-form :deep(.ant-input-number-input),
.journal-form :deep(.ant-picker-input input),
.journal-form :deep(.ant-select-selection-item),
.journal-form :deep(.ant-input) {
  color: #123044;
}

.journal-form :deep(.ant-input::placeholder),
.journal-form :deep(.ant-picker-input input::placeholder) {
  color: #aeb8c5;
}

.executed-tag {
  color: #4ade80;
  background: rgba(74, 222, 128, 0.1);
  border-color: rgba(74, 222, 128, 0.22);
}

.pending-tag {
  color: #7a8fa0;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.14);
}

.loss-tag {
  color: #fb7185;
  background: rgba(251, 113, 133, 0.1);
  border-color: rgba(251, 113, 133, 0.22);
}

.flat-tag {
  color: #facc15;
  background: rgba(250, 204, 21, 0.1);
  border-color: rgba(250, 204, 21, 0.22);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 900px) {
  .journal-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .journal-guide {
    grid-template-columns: 1fr;
  }

  .journal-actions .toolbar,
  .journal-actions :deep(.symbol-selector-wrap),
  .journal-actions :deep(.ant-btn) {
    width: 100%;
  }

  .table-head {
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
