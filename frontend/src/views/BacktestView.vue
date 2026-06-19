<template>
  <div class="page backtest-page">
    <div class="glass-card control-card">
      <div>
        <div class="section-title">模拟交易回测</div>
      </div>
      <div class="toolbar backtest-form">
        <label class="backtest-field symbol-field">
          <span>ETF</span>
          <SymbolSelector v-model="form.symbolCode" />
        </label>
        <label class="backtest-field strategy-field">
          <span>ETF 策略模式</span>
          <a-radio-group v-model:value="form.strategyType" button-style="solid" class="strategy-toggle">
            <a-radio-button value="ETF_TREND">趋势策略</a-radio-button>
            <a-radio-button value="ETF_DCA">定投策略</a-radio-button>
          </a-radio-group>
        </label>
        <label class="backtest-field date-range-field">
          <span>回测区间</span>
          <DateRangeControl v-model:value="dateRange" :presets="backtestDatePresets" />
        </label>
        <label class="backtest-field">
          <span>初始资金</span>
          <a-input-number v-model:value="form.initialCapital" :min="1000" :step="10000" placeholder="初始资金" />
        </label>
        <label class="backtest-field">
          <span>仓位比例</span>
          <a-input-number v-model:value="form.positionRatio" :min="0.1" :max="1" :step="0.05" placeholder="仓位比例" />
        </label>
        <a-tooltip :title="!form.symbolCode ? '请先选择 ETF' : ''">
          <span><a-button type="primary" :loading="loading" :disabled="!form.symbolCode" @click="run">
            <template #icon><PlayCircleOutlined /></template>
            运行回测
          </a-button></span>
        </a-tooltip>
      </div>

      <a-collapse v-model:activeKey="advancedPanels" ghost class="advanced-collapse">
        <a-collapse-panel key="advanced" header="高级参数">
          <div class="toolbar backtest-form advanced-form">
            <label class="backtest-field">
              <span>手续费</span>
              <a-input-number v-model:value="form.feeRate" :min="0" :max="0.01" :step="0.0001" :precision="4" placeholder="默认 0.0003" />
            </label>
            <label class="backtest-field">
              <span>滑点</span>
              <a-input-number v-model:value="form.slippageRate" :min="0" :max="0.02" :step="0.0001" :precision="4" placeholder="默认 0.0005" />
            </label>
            <label class="backtest-field">
              <span>最小交易单位</span>
              <a-input-number v-model:value="form.lotSize" :min="1" :step="100" :precision="0" placeholder="默认 100" />
            </label>
            <template v-if="form.strategyType === 'ETF_DCA'">
              <label class="backtest-field">
                <span>定投频率</span>
                <a-select v-model:value="form.dcaFrequency" class="dca-select">
                  <a-select-option value="MONTHLY">每月</a-select-option>
                  <a-select-option value="WEEKLY">每周</a-select-option>
                </a-select>
              </label>
              <label class="backtest-field">
                <span>基础金额</span>
                <a-input-number v-model:value="form.dcaBaseAmount" :min="100" :step="500" placeholder="默认 1000" />
              </label>
              <label class="backtest-field">
                <span>低于 MA20 倍数</span>
                <a-input-number v-model:value="form.dcaLowMultiplier" :min="0.1" :max="5" :step="0.1" :precision="1" placeholder="默认 2" />
              </label>
              <label class="backtest-field">
                <span>高于 MA20 倍数</span>
                <a-input-number v-model:value="form.dcaHighMultiplier" :min="0.1" :max="5" :step="0.1" :precision="1" placeholder="默认 0.5" />
              </label>
            </template>
          </div>
        </a-collapse-panel>
      </a-collapse>
    </div>

    <div class="glass-card batch-card">
      <div class="batch-head">
        <div>
          <div class="section-title">多 ETF 批量回测</div>
        </div>
        <div class="batch-actions">
          <a-button size="small" @click="selectAllEtfs">
            <template #icon><SelectOutlined /></template>
            选择全部 ETF
          </a-button>
          <a-button size="small" @click="batchSymbols = []">
            <template #icon><ClearOutlined /></template>
            清空
          </a-button>
          <a-button type="primary" :loading="batchLoading" :disabled="!batchSymbols.length" @click="runBatch">
            <template #icon><PlayCircleOutlined /></template>
            批量回测
          </a-button>
        </div>
      </div>
      <div class="batch-selector-row">
        <a-select
          v-model:value="batchSymbols"
          mode="multiple"
          allow-clear
          show-search
          class="batch-symbol-select"
          placeholder="选择多个 ETF"
          :options="batchSymbolOptions"
          :filter-option="filterBatchSymbol"
        />
      </div>
      <div v-if="batchResult" class="batch-summary">
        <span>总数 {{ batchResult.totalCount }}</span>
        <span class="value-up">成功 {{ batchResult.successCount }}</span>
        <span class="value-down">失败 {{ batchResult.failedCount }}</span>
      </div>
      <a-table
        v-if="batchResult"
        class="app-table batch-table"
        :data-source="batchResult.results"
        :pagination="false"
        :row-key="batchRowKey"
        size="small"
        :scroll="{ x: 1180 }"
      >
        <a-table-column data-index="symbolCode" title="标的" :width="110" fixed="left" />
        <a-table-column title="状态" :width="90">
          <template #default="{ record }">
            <span :class="record.success ? 'value-up' : 'value-down'">{{ record.success ? '成功' : '失败' }}</span>
          </template>
        </a-table-column>
        <a-table-column title="策略收益" :width="110">
          <template #default="{ record }"><span :class="classBySign(record.totalReturnRate)">{{ pct(record.totalReturnRate) }}</span></template>
        </a-table-column>
        <a-table-column title="基准收益" :width="110">
          <template #default="{ record }"><span :class="classBySign(record.benchmarkReturnRate)">{{ pct(record.benchmarkReturnRate) }}</span></template>
        </a-table-column>
        <a-table-column title="超额收益" :width="110">
          <template #default="{ record }"><span :class="classBySign(record.excessReturnRate)">{{ signedPct(record.excessReturnRate) }}</span></template>
        </a-table-column>
        <a-table-column title="最大回撤" :width="110">
          <template #default="{ record }"><span class="value-down">{{ pct(record.maxDrawdownRate) }}</span></template>
        </a-table-column>
        <a-table-column title="胜率" :width="90">
          <template #default="{ record }">{{ pct(record.winRate) }}</template>
        </a-table-column>
        <a-table-column data-index="tradeCount" title="交易数" :width="90" />
        <a-table-column title="说明" :width="250">
          <template #default="{ record }">{{ record.message || '-' }}</template>
        </a-table-column>
        <a-table-column title="操作" :width="90" fixed="right">
          <template #default="{ record }">
            <a-button v-if="record.recordId" size="small" type="primary" @click="loadRecord(record.recordId)">
              <template #icon><ImportOutlined /></template>
              载入
            </a-button>
          </template>
        </a-table-column>
      </a-table>
    </div>

    <div class="glass-card history-card">
      <div class="table-head">
        <div>
          <div class="section-title">历史回测记录</div>
        </div>
        <div class="table-summary">
          <span class="table-count-pill">{{ records.length }} 条</span>
          <span v-if="selectedRecordIds.length" class="table-count-pill">已选 {{ selectedRecordIds.length }}</span>
          <a-button size="small" :disabled="!selectedRecordIds.length" @click="copyComparisonReport">
            <template #icon><CopyOutlined /></template>
            复制对比
          </a-button>
          <a-button size="small" :disabled="!selectedRecordIds.length" @click="clearRecordSelection">
            <template #icon><ClearOutlined /></template>
            清空选择
          </a-button>
          <a-button size="small" :loading="recordsLoading" @click="loadRecords">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
      <a-table
        class="app-table history-table"
        :data-source="records"
        :loading="recordsLoading"
        :pagination="false"
        :row-key="recordRowKey"
        :row-selection="historyRowSelection"
        size="small"
        :scroll="{ x: 1280 }"
      >
        <a-table-column data-index="symbolCode" title="标的" :width="110" fixed="left" />
        <a-table-column title="策略" :width="130">
          <template #default="{ record }">{{ strategyName(record.strategyType) }}</template>
        </a-table-column>
        <a-table-column title="区间" :width="190">
          <template #default="{ record }">{{ record.startDate || '-' }} 至 {{ record.endDate || '-' }}</template>
        </a-table-column>
        <a-table-column title="最终权益" :width="120">
          <template #default="{ record }">{{ money(record.finalEquity) }}</template>
        </a-table-column>
        <a-table-column title="收益率" :width="100">
          <template #default="{ record }">
            <span :class="(record.totalReturnRate || 0) >= 0 ? 'value-up' : 'value-down'">{{ pct(record.totalReturnRate) }}</span>
          </template>
        </a-table-column>
        <a-table-column title="基准收益" :width="110">
          <template #default="{ record }">
            <span :class="classBySign(record.benchmarkReturnRate)">{{ pct(record.benchmarkReturnRate) }}</span>
          </template>
        </a-table-column>
        <a-table-column title="超额收益" :width="110">
          <template #default="{ record }">
            <span :class="classBySign(record.excessReturnRate)">{{ signedPct(record.excessReturnRate) }}</span>
          </template>
        </a-table-column>
        <a-table-column title="最大回撤" :width="110">
          <template #default="{ record }"><span class="value-down">{{ pct(record.maxDrawdownRate) }}</span></template>
        </a-table-column>
        <a-table-column title="胜率" :width="90">
          <template #default="{ record }">{{ pct(record.winRate) }}</template>
        </a-table-column>
        <a-table-column data-index="tradeCount" title="交易数" :width="90" />
        <a-table-column title="保存时间" :width="170">
          <template #default="{ record }">{{ record.createdAt || '-' }}</template>
        </a-table-column>
        <a-table-column title="操作" :width="150" fixed="right">
          <template #default="{ record }">
            <div class="table-actions compact-actions">
              <a-button size="small" type="primary" @click="loadRecord(record.id)">
                <template #icon><ImportOutlined /></template>
                载入
              </a-button>
              <a-button size="small" danger @click="deleteRecord(record.id)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </div>
          </template>
        </a-table-column>
      </a-table>
    </div>

    <div v-if="selectedComparisonRecords.length" class="glass-card compare-card">
      <div class="compare-head">
        <div>
          <div class="section-title">回测记录对比</div>
        </div>
        <span class="benchmark-badge" :class="classBySign(compareStats.bestExcess?.excessReturnRate)">
          最佳超额 {{ signedPct(compareStats.bestExcess?.excessReturnRate) }}
        </span>
      </div>
      <div class="compare-metric-grid">
        <div class="compare-metric">
          <span>对比记录</span>
          <strong>{{ selectedComparisonRecords.length }}</strong>
          <small>Selected</small>
        </div>
        <div class="compare-metric">
          <span>跑赢基准</span>
          <strong class="value-up">{{ compareStats.outperformCount }}</strong>
          <small>Excess &gt; 0</small>
        </div>
        <div class="compare-metric">
          <span>最高策略收益</span>
          <strong :class="classBySign(compareStats.bestReturn?.totalReturnRate)">{{ pct(compareStats.bestReturn?.totalReturnRate) }}</strong>
          <small>{{ compareStats.bestReturn?.symbolCode || '-' }}</small>
        </div>
        <div class="compare-metric">
          <span>最低回撤</span>
          <strong class="value-down">{{ pct(compareStats.lowestDrawdown?.maxDrawdownRate) }}</strong>
          <small>{{ compareStats.lowestDrawdown?.symbolCode || '-' }}</small>
        </div>
      </div>
      <a-table
        class="app-table compare-table"
        :data-source="selectedComparisonRecords"
        :pagination="false"
        :row-key="recordRowKey"
        size="small"
        :scroll="{ x: 1380 }"
      >
        <a-table-column data-index="symbolCode" title="标的" :width="110" fixed="left" />
        <a-table-column title="策略" :width="130">
          <template #default="{ record }">{{ strategyName(record.strategyType) }}</template>
        </a-table-column>
        <a-table-column title="区间" :width="190">
          <template #default="{ record }">{{ record.startDate || '-' }} 至 {{ record.endDate || '-' }}</template>
        </a-table-column>
        <a-table-column title="初始资金" :width="110">
          <template #default="{ record }">{{ money(record.initialCapital) }}</template>
        </a-table-column>
        <a-table-column title="策略收益" :width="110">
          <template #default="{ record }"><span :class="classBySign(record.totalReturnRate)">{{ pct(record.totalReturnRate) }}</span></template>
        </a-table-column>
        <a-table-column title="基准收益" :width="110">
          <template #default="{ record }"><span :class="classBySign(record.benchmarkReturnRate)">{{ pct(record.benchmarkReturnRate) }}</span></template>
        </a-table-column>
        <a-table-column title="超额收益" :width="110">
          <template #default="{ record }"><span :class="classBySign(record.excessReturnRate)">{{ signedPct(record.excessReturnRate) }}</span></template>
        </a-table-column>
        <a-table-column title="最大回撤" :width="110">
          <template #default="{ record }"><span class="value-down">{{ pct(record.maxDrawdownRate) }}</span></template>
        </a-table-column>
        <a-table-column title="基准回撤" :width="110">
          <template #default="{ record }"><span class="value-down">{{ pct(record.benchmarkMaxDrawdownRate) }}</span></template>
        </a-table-column>
        <a-table-column title="胜率" :width="90">
          <template #default="{ record }">{{ pct(record.winRate) }}</template>
        </a-table-column>
        <a-table-column data-index="tradeCount" title="交易数" :width="90" />
        <a-table-column title="保存时间" :width="170">
          <template #default="{ record }">{{ record.createdAt || '-' }}</template>
        </a-table-column>
        <a-table-column title="操作" :width="90" fixed="right">
          <template #default="{ record }">
            <a-button size="small" type="primary" @click="loadRecord(record.id)">
              <template #icon><ImportOutlined /></template>
              载入
            </a-button>
          </template>
        </a-table-column>
      </a-table>
    </div>

    <template v-if="result">
      <div class="metric-grid">
        <div v-for="item in metrics" :key="item.label" class="metric">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value" :class="item.className">{{ item.value }}</div>
          <div class="metric-note">{{ item.note }}</div>
        </div>
      </div>

      <div class="glass-card quality-card">
        <div class="quality-head">
          <div>
            <div class="section-title">可信回测检查</div>
          </div>
          <span class="quality-badge" :class="result.dataQuality?.passed === false ? 'danger' : 'ok'">
            {{ result.dataQuality?.passed === false ? '数据需修复' : '检查通过' }}
          </span>
        </div>
        <div class="quality-grid">
          <div><span>成交模型</span><strong>{{ executionModeName(result.executionMode) }}</strong></div>
          <div><span>K 线数量</span><strong>{{ result.dataQuality?.totalRows ?? '-' }}</strong></div>
          <div><span>价格异常</span><strong :class="(result.dataQuality?.invalidPriceRows || 0) > 0 ? 'value-down' : 'value-up'">{{ result.dataQuality?.invalidPriceRows ?? 0 }}</strong></div>
          <div><span>日期断档</span><strong>{{ result.dataQuality?.longCalendarGapCount ?? 0 }}</strong></div>
          <div><span>缺少成交量</span><strong>{{ result.dataQuality?.missingVolumeRows ?? 0 }}</strong></div>
          <div><span>缺少成交额</span><strong>{{ result.dataQuality?.missingAmountRows ?? 0 }}</strong></div>
        </div>
        <div v-if="result.dataQuality?.warnings?.length" class="quality-warnings">
          <span v-for="item in result.dataQuality.warnings.slice(0, 4)" :key="item">{{ item }}</span>
        </div>
      </div>

      <div v-if="result.benchmark" class="glass-card benchmark-card">
        <div class="benchmark-head">
          <div>
            <div class="section-title">买入持有基准对比</div>
          </div>
          <span class="benchmark-badge" :class="classBySign(excessReturnRate)">
            超额 {{ signedPct(excessReturnRate) }}
          </span>
        </div>
        <div class="benchmark-grid">
          <div v-for="item in benchmarkMetrics" :key="item.label" class="benchmark-item">
            <span>{{ item.label }}</span>
            <strong :class="item.className">{{ item.value }}</strong>
            <small>{{ item.note }}</small>
          </div>
        </div>
      </div>

      <div class="glass-card ai-export-card">
        <div class="ai-export-main">
          <div>
            <div class="section-title">AI 复盘导出</div>
          </div>
          <div class="ai-export-actions">
            <a-button @click="previewExpanded = !previewExpanded">
              <template #icon><EyeOutlined /></template>
              {{ previewExpanded ? '收起预览' : '查看文本' }}
            </a-button>
            <a-button type="primary" :loading="copying" @click="copyAiReport">
              <template #icon><CopyOutlined /></template>
              复制给 AI
            </a-button>
          </div>
        </div>
        <div class="ai-export-meta">
          <span>{{ result.symbolCode }}</span>
          <span>{{ strategyName(result.strategyType) }}</span>
          <span>{{ result.start }} 至 {{ result.end }}</span>
          <span>{{ result.trades.length }} 条成交记录</span>
          <span v-if="result.recordId">记录 #{{ result.recordId }}</span>
        </div>
        <pre v-if="previewExpanded" class="ai-report-preview">{{ aiReportText }}</pre>
      </div>

      <div class="backtest-grid">
        <div class="glass-card curve-card">
          <div class="section-title">资金曲线 <a-tag>{{ strategyName(result.strategyType) }}</a-tag></div>
          <div ref="curveRef" class="curve-chart"></div>
        </div>

        <div class="glass-card position-card">
          <div class="section-title">当前模拟持仓</div>
          <div class="position-status" :class="result.position.status === 'HOLDING' ? 'holding' : 'empty'">
            {{ result.position.status === 'HOLDING' ? '持仓中' : '空仓' }}
          </div>
          <div class="position-list">
            <div><span>持仓数量</span><strong>{{ result.position.shares }}</strong></div>
            <div><span>均价</span><strong>{{ fmt(result.position.avgCost) }}</strong></div>
            <div><span>最新价</span><strong>{{ fmt(result.position.marketPrice) }}</strong></div>
            <div><span>市值</span><strong>{{ money(result.position.marketValue) }}</strong></div>
            <div><span>浮动盈亏</span><strong :class="(result.position.unrealizedPnl || 0) >= 0 ? 'value-up' : 'value-down'">{{ money(result.position.unrealizedPnl) }}</strong></div>
            <div><span>浮动收益率</span><strong :class="(result.position.unrealizedPnlRate || 0) >= 0 ? 'value-up' : 'value-down'">{{ pct(result.position.unrealizedPnlRate) }}</strong></div>
          </div>
        </div>
      </div>

      <div class="glass-card table-card trade-card">
        <div class="table-head">
          <div>
            <div class="section-title">模拟买卖记录</div>
          </div>
          <div class="table-summary"><span class="table-count-pill">{{ result.trades.length }} 条</span></div>
        </div>
        <a-table
          class="app-table"
          :data-source="result.trades"
          :pagination="false"
          :row-key="tradeRowKey"
          size="middle"
          :scroll="{ y: 420, x: 1120 }"
        >
          <a-table-column data-index="tradeDate" title="日期" :width="120" />
          <a-table-column title="动作" :width="90">
            <template #default="{ record }">
              <span :class="record.action === 'BUY' ? 'value-up' : 'value-down'">{{ record.action === 'BUY' ? '买入' : '卖出' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="价格" :width="110"><template #default="{ record }">{{ fmt(record.price) }}</template></a-table-column>
          <a-table-column data-index="shares" title="数量" :width="100" />
          <a-table-column title="成交额" :width="130"><template #default="{ record }">{{ money(record.amount) }}</template></a-table-column>
          <a-table-column title="费用" :width="100"><template #default="{ record }">{{ money(record.fee) }}</template></a-table-column>
          <a-table-column title="盈亏" :width="120">
            <template #default="{ record }">
              <span :class="(record.pnl || 0) >= 0 ? 'value-up' : 'value-down'">{{ record.pnl == null ? '-' : money(record.pnl) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="收益率" :width="110">
            <template #default="{ record }">
              <span :class="(record.pnlRate || 0) >= 0 ? 'value-up' : 'value-down'">{{ record.pnlRate == null ? '-' : pct(record.pnlRate) }}</span>
            </template>
          </a-table-column>
          <a-table-column data-index="reason" title="原因" :width="340" />
        </a-table>
      </div>
    </template>

    <div v-else class="glass-card empty-card">
      <a-empty description="暂无回测结果" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import { LineChart } from 'echarts/charts'
import { DataZoomComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use, type EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { ClearOutlined, CopyOutlined, DeleteOutlined, EyeOutlined, ImportOutlined, PlayCircleOutlined, ReloadOutlined, SelectOutlined } from '@ant-design/icons-vue'
import SymbolSelector from '@/components/SymbolSelector.vue'
import DateRangeControl from '@/components/DateRangeControl.vue'
import { backtestApi } from '@/api/backtestApi'
import { symbolApi } from '@/api/symbolApi'
import { useNumberTableSelection } from '@/composables/useTableSelection'
import { buildBacktestAiReport, buildBacktestComparisonReport } from '@/features/backtest/backtestReports'
import { writeClipboard } from '@/utils/clipboard'
import { confirmAction, feedback } from '@/utils/feedback'
import {
  classBySign,
  diffRate,
  formatMoney,
  formatNumber,
  formatPercent,
  formatSignedPercent
} from '@/utils/formatters'
import { strategyName } from '@/utils/labels'
import type {
  BacktestBatchItemDTO,
  BacktestBatchResultDTO,
  BacktestRecordSummaryDTO,
  BacktestResultDTO,
  BacktestRunRequest,
  BacktestTradeDTO,
  SymbolDTO
} from '@/types'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, CanvasRenderer])

const dateRange = ref<[string, string]>(['2024-01-01', today()])
const advancedPanels = ref<string[]>([])
const backtestDatePresets = [
  { key: '1y', label: '近 1 年', days: 365 },
  { key: '3y', label: '近 3 年', days: 1095 },
  { key: '5y', label: '近 5 年', days: 1825 },
  { key: '8y', label: '近 8 年', days: 2920 }
]
const form = ref<BacktestRunRequest>({
  symbolCode: '',
  strategyType: 'ETF_TREND',
  initialCapital: 100000,
  positionRatio: 0.95,
  feeRate: 0.0003,
  slippageRate: 0.0005,
  lotSize: 100,
  dcaFrequency: 'MONTHLY',
  dcaBaseAmount: 1000,
  dcaLowMultiplier: 2,
  dcaHighMultiplier: 0.5
})
const result = ref<BacktestResultDTO>()
const loading = ref(false)
const copying = ref(false)
const previewExpanded = ref(false)
const batchLoading = ref(false)
const batchSymbols = ref<string[]>([])
const enabledSymbols = ref<SymbolDTO[]>([])
const batchResult = ref<BacktestBatchResultDTO>()
const recordsLoading = ref(false)
const records = ref<BacktestRecordSummaryDTO[]>([])
const {
  selectedIds: selectedRecordIds,
  rowSelection: historyRowSelection,
  clearSelection: clearRecordSelection,
  removeSelectedId: removeSelectedRecordId,
  keepExistingIds: keepExistingRecordIds
} = useNumberTableSelection()
const curveRef = ref<HTMLDivElement>()
let chart: EChartsType | null = null

const metrics = computed(() => {
  const summary = result.value?.summary
  if (!summary) return []
  return [
    { label: '最终权益', value: money(summary.finalEquity), note: 'Cash + Market Value', className: 'value-blue' },
    { label: '总收益率', value: pct(summary.totalReturnRate), note: 'Total Return', className: summary.totalReturnRate >= 0 ? 'value-up' : 'value-down' },
    { label: '年化收益率', value: pct(summary.annualizedReturnRate), note: 'Annualized', className: summary.annualizedReturnRate >= 0 ? 'value-up' : 'value-down' },
    { label: '最大回撤', value: pct(summary.maxDrawdownRate), note: 'Max Drawdown', className: 'value-down' },
    { label: '胜率', value: pct(summary.winRate), note: `${summary.winCount} 赢 / ${summary.lossCount} 输`, className: 'value-blue' }
  ]
})

const excessReturnRate = computed(() =>
  diffRate(result.value?.summary.totalReturnRate, result.value?.benchmark?.totalReturnRate)
)

const benchmarkMetrics = computed(() => {
  const benchmark = result.value?.benchmark
  const summary = result.value?.summary
  if (!benchmark || !summary) return []
  return [
    {
      label: '策略收益',
      value: pct(summary.totalReturnRate),
      note: 'Strategy',
      className: classBySign(summary.totalReturnRate)
    },
    {
      label: '买入持有',
      value: pct(benchmark.totalReturnRate),
      note: 'Buy & Hold',
      className: classBySign(benchmark.totalReturnRate)
    },
    {
      label: '超额收益',
      value: signedPct(excessReturnRate.value),
      note: 'Strategy - Benchmark',
      className: classBySign(excessReturnRate.value)
    },
    {
      label: '基准回撤',
      value: pct(benchmark.maxDrawdownRate),
      note: `${benchmark.shares || 0} 份，首日 ${fmt(benchmark.startPrice)} -> 末日 ${fmt(benchmark.endPrice)}`,
      className: 'value-down'
    }
  ]
})

const aiReportText = computed(() =>
  result.value ? buildBacktestAiReport({ data: result.value, form: form.value, selectedRange: dateRange.value }) : ''
)

const etfCodePattern = /^(510|511|512|513|515|516|517|518|519|520|521|522|560|561|562|563|588|589|159)\d{3}$/
const isEtfSymbol = (item: { symbolCode: string; assetType?: string }) =>
  String(item.assetType || '').toUpperCase() === 'ETF' || etfCodePattern.test(item.symbolCode)

const batchSymbolOptions = computed(() =>
  enabledSymbols.value.map((item) => ({
    value: item.symbolCode,
    label: `${item.symbolCode} ${item.symbolName || ''}`.trim(),
    assetType: item.assetType
  }))
)

const selectedComparisonRecords = computed(() => {
  const ids = new Set(selectedRecordIds.value)
  return sortCompareRecords(records.value.filter((item) => ids.has(item.id)))
})

const compareStats = computed(() => {
  const rows = selectedComparisonRecords.value
  return {
    outperformCount: rows.filter((item) => (item.excessReturnRate ?? 0) > 0).length,
    bestExcess: bestBy(rows, (item) => item.excessReturnRate),
    bestReturn: bestBy(rows, (item) => item.totalReturnRate),
    lowestDrawdown: bestBy(rows, (item) => item.maxDrawdownRate)
  }
})

const run = async () => {
  if (!form.value.symbolCode) return
  loading.value = true
  try {
    const [start, end] = dateRange.value || []
    result.value = await backtestApi.run({ ...form.value, start, end })
    feedback.success('回测完成，已保存记录')
    await loadRecords()
    await renderCurve()
  } finally {
    loading.value = false
  }
}

const loadRecords = async () => {
  recordsLoading.value = true
  try {
    records.value = await backtestApi.records({ limit: 50 })
    keepExistingRecordIds(records.value.map((item) => item.id))
  } finally {
    recordsLoading.value = false
  }
}

const loadEnabledSymbols = async () => {
  const list = await symbolApi.listEnabled()
  enabledSymbols.value = list.filter(isEtfSymbol)
}

const selectAllEtfs = () => {
  batchSymbols.value = enabledSymbols.value.map((item) => item.symbolCode)
}

const filterBatchSymbol = (input: string, option?: { label?: string; value?: string }) => {
  const keyword = input.trim().toLowerCase()
  return String(option?.label || option?.value || '').toLowerCase().includes(keyword)
}

const runBatch = async () => {
  if (!batchSymbols.value.length) return
  batchLoading.value = true
  try {
    const [start, end] = dateRange.value || []
    const response = await backtestApi.batchRun({
      ...form.value,
      symbolCodes: batchSymbols.value,
      start,
      end
    })
    response.results = sortBatchResults(response.results)
    batchResult.value = response
    feedback.success(`批量回测完成：成功 ${response.successCount}，失败 ${response.failedCount}`)
    await loadRecords()
  } finally {
    batchLoading.value = false
  }
}

const loadRecord = async (id: number) => {
  const detail = await backtestApi.recordDetail(id)
  form.value = {
    ...form.value,
    ...detail.request,
    symbolCode: detail.request.symbolCode || detail.result.symbolCode,
    strategyType: detail.request.strategyType || detail.result.strategyType || 'ETF_TREND'
  }
  dateRange.value = [detail.result.start, detail.result.end]
  result.value = detail.result
  previewExpanded.value = false
  feedback.success('历史回测已载入')
  await renderCurve()
}

const deleteRecord = async (id: number) => {
  try {
    await confirmAction({
      title: '删除回测记录',
      content: `确认删除回测记录 #${id} 吗？该操作不会影响 K 线数据和技术信号。`,
      okText: '删除',
      danger: true
    })
  } catch {
    return
  }
  await backtestApi.deleteRecord(id)
  if (result.value?.recordId === id) {
    result.value = undefined
  }
  removeSelectedRecordId(id)
  feedback.success('回测记录已删除')
  await loadRecords()
}

const copyAiReport = async () => {
  if (!aiReportText.value) return
  copying.value = true
  try {
    await writeClipboard(aiReportText.value)
    feedback.success('AI 复盘文本已复制')
  } finally {
    copying.value = false
  }
}

const copyComparisonReport = async () => {
  if (!selectedComparisonRecords.value.length) return
  await writeClipboard(buildBacktestComparisonReport(selectedComparisonRecords.value, compareStats.value))
  feedback.success('回测对比摘要已复制')
}

const renderCurve = async () => {
  await nextTick()
  if (!curveRef.value || !result.value) return
  if (!chart) chart = init(curveRef.value, 'dark')
  const instance = chart
  instance.setOption(curveOption(), true)
  instance.resize()
}

const curveOption = (): EChartsOption => {
  const rows = result.value?.equityCurve || []
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(148, 173, 198, 0.38)',
      textStyle: { color: '#102033' }
    },
    legend: {
      top: 0,
      textStyle: { color: '#52677d', fontWeight: 700 },
      data: ['总权益', '回撤']
    },
    grid: { left: 60, right: 24, top: 48, bottom: 40 },
    xAxis: {
      type: 'category',
      data: rows.map((item) => item.tradeDate),
      axisLabel: { color: '#64748b' },
      axisLine: { lineStyle: { color: 'rgba(148, 173, 198, 0.42)' } }
    },
    yAxis: [
      {
        type: 'value',
        axisLabel: { color: '#64748b' },
        splitLine: { lineStyle: { color: 'rgba(148, 173, 198, 0.24)' } }
      },
      {
        type: 'value',
        axisLabel: { color: '#e11d48', formatter: '{value}%' },
        splitLine: { show: false }
      }
    ],
    dataZoom: [{ type: 'inside' }, { type: 'slider', height: 22, bottom: 6 }],
    series: [
      {
        name: '总权益',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: rows.map((item) => item.totalEquity),
        lineStyle: { color: '#2f80ed', width: 2 },
        areaStyle: { color: 'rgba(47, 128, 237, 0.1)' }
      },
      {
        name: '回撤',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'none',
        data: rows.map((item) => item.drawdown),
        lineStyle: { color: '#e11d48', width: 1.5 },
        areaStyle: { color: 'rgba(225, 29, 72, 0.08)' }
      }
    ]
  }
}

const money = formatMoney
const fmt = formatNumber
const pct = formatPercent
const executionModeName = (value?: string) => value === 'NEXT_OPEN' ? '次日开盘成交' : '收盘价成交'
const tradeRowKey = (row: BacktestTradeDTO, index?: number) => `${row.tradeDate}-${row.action}-${index ?? 0}`
const recordRowKey = (row: BacktestRecordSummaryDTO) => row.id
const batchRowKey = (row: BacktestBatchItemDTO) => `${row.symbolCode}-${row.recordId || row.message || 'failed'}`

const sortBatchResults = (rows: BacktestBatchItemDTO[]) =>
  [...rows].sort((a, b) => {
    if (a.success !== b.success) return a.success ? -1 : 1
    return (b.excessReturnRate ?? Number.NEGATIVE_INFINITY) - (a.excessReturnRate ?? Number.NEGATIVE_INFINITY)
  })

const sortCompareRecords = (rows: BacktestRecordSummaryDTO[]) =>
  [...rows].sort((a, b) => (b.excessReturnRate ?? Number.NEGATIVE_INFINITY) - (a.excessReturnRate ?? Number.NEGATIVE_INFINITY))

const bestBy = (rows: BacktestRecordSummaryDTO[], getter: (item: BacktestRecordSummaryDTO) => number | undefined) =>
  rows.reduce<BacktestRecordSummaryDTO | undefined>((best, item) => {
    const value = getter(item)
    if (value == null) return best
    if (!best || value > (getter(best) ?? Number.NEGATIVE_INFINITY)) return item
    return best
  }, undefined)

const signedPct = formatSignedPercent

function today() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

watch(result, renderCurve)
window.addEventListener('resize', renderCurve)
onMounted(() => {
  loadRecords()
  loadEnabledSymbols()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', renderCurve)
  chart?.dispose()
})
</script>

<style scoped>
.control-card,
.batch-card,
.history-card,
.compare-card,
.benchmark-card,
.ai-export-card,
.curve-card,
.position-card,
.trade-card,
.empty-card {
  padding: 10px;
}

.control-card {
  display: grid;
  gap: 10px;
  background: rgba(246, 250, 253, 0.9);
}

.control-card p {
  margin: -4px 0 0;
}

.backtest-form :deep(.ant-input-number),
.backtest-form :deep(.ant-picker),
.backtest-form :deep(.ant-select),
.backtest-form :deep(.ant-btn) {
  width: 100%;
}

.backtest-form :deep(.ant-input-number),
.backtest-form :deep(.ant-picker),
.backtest-form :deep(.ant-select-selector) {
  min-height: 32px;
  border-radius: 10px;
  color: #123044;
  background: rgba(255, 255, 255, 0.94) !important;
  border-color: rgba(129, 153, 174, 0.42) !important;
}

.backtest-form :deep(.ant-input-number-input),
.backtest-form :deep(.ant-picker-input input),
.backtest-form :deep(.ant-select-selection-item) {
  color: #123044;
}

.backtest-form :deep(.ant-picker-suffix),
.backtest-form :deep(.ant-picker-clear),
.backtest-form :deep(.ant-select-arrow) {
  color: #123044;
}

.backtest-form :deep(.ant-radio-button-wrapper) {
  min-height: 28px;
  height: auto;
  padding: 6px 10px;
  line-height: 1.1;
  white-space: nowrap;
  color: #123044;
  background: rgba(255, 255, 255, 0.88);
  border-color: rgba(129, 153, 174, 0.36);
}

.backtest-form :deep(.ant-radio-button-wrapper-checked) {
  color: #ffffff;
  background: linear-gradient(135deg, #4aa3ff, #2f80ed);
  border-color: transparent;
}

.backtest-form :deep(.ant-radio-button-wrapper::before) {
  display: none;
}

.dca-select {
  width: 100%;
}

.backtest-form {
  align-items: flex-end;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.backtest-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.backtest-field span {
  color: #7a8fa0;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  min-height: 16px;
  white-space: normal;
}

.strategy-field {
  min-width: 220px;
}

.date-range-field {
  min-width: 0;
}

.backtest-form :deep(.symbol-selector-wrap),
.backtest-form :deep(.date-range-picker) {
  width: 100%;
}

.backtest-form > .ant-btn {
  align-self: end;
  width: 100%;
}

.backtest-form > span {
  align-self: end;
}

.backtest-form > span :deep(.ant-btn) {
  width: 100%;
}

.advanced-collapse {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.advanced-collapse :deep(.ant-collapse-header) {
  color: #123044 !important;
  padding: 6px 0 0 !important;
  font-weight: 850;
}

.advanced-collapse :deep(.ant-collapse-content-box) {
  padding: 8px 0 0 !important;
}

.advanced-form {
  align-items: start;
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

.history-card {
  padding: 10px;
}

.history-table {
  margin-top: 4px;
}

.compact-actions {
  gap: 6px;
  flex-wrap: nowrap;
}

.compact-actions :deep(.ant-btn) {
  margin-left: 0;
}

.batch-card {
  display: grid;
  gap: 8px;
  background: rgba(246, 250, 253, 0.86);
}

.batch-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.batch-head p {
  margin: -4px 0 0;
  line-height: 1.35;
}

.batch-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.batch-actions :deep(.ant-btn) {
  margin-left: 0;
}

.batch-selector-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
}

.batch-symbol-select {
  width: 100%;
}

.batch-symbol-select :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.94) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}

.batch-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.batch-summary span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.batch-table {
  margin-top: 2px;
}

.compare-card {
  display: grid;
  gap: 8px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(238, 246, 251, 0.9));
}

.compare-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.compare-head p {
  margin: -4px 0 0;
  line-height: 1.35;
}

.compare-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.compare-metric {
  min-width: 0;
  padding: 9px;
  border-radius: 11px;
  background: rgba(246, 250, 253, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.compare-metric span,
.compare-metric small {
  display: block;
  color: #7a8fa0;
  font-size: 12px;
  line-height: 1.45;
}

.compare-metric strong {
  display: block;
  margin: 4px 0 2px;
  color: #123044;
  font-size: 19px;
  line-height: 1.1;
  font-weight: 950;
  font-variant-numeric: tabular-nums;
}

.compare-table {
  margin-top: 2px;
}

.benchmark-card {
  display: grid;
  gap: 8px;
  background: rgba(246, 250, 253, 0.86);
}

.quality-card {
  display: grid;
  gap: 8px;
  background: rgba(246, 250, 253, 0.86);
}

.quality-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.quality-head p {
  margin: -4px 0 0;
  line-height: 1.35;
}

.quality-badge {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.quality-badge.ok {
  color: #bbf7d0;
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.28);
}

.quality-badge.danger {
  color: #fecdd3;
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(239, 68, 68, 0.32);
}

.quality-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.quality-grid > div {
  min-width: 0;
  padding: 9px;
  border-radius: 11px;
  background: rgba(246, 250, 253, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.quality-grid span {
  display: block;
  color: #7a8fa0;
  font-size: 12px;
}

.quality-grid strong {
  display: block;
  margin-top: 4px;
  color: #123044;
  font-size: 16px;
  font-weight: 950;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.quality-warnings {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quality-warnings span {
  padding: 5px 8px;
  border-radius: 999px;
  color: #fde68a;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.24);
  font-size: 12px;
  line-height: 1.35;
}

.benchmark-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.benchmark-head p {
  margin: -4px 0 0;
  line-height: 1.35;
}

.benchmark-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.benchmark-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.benchmark-item {
  min-width: 0;
  padding: 9px;
  border-radius: 11px;
  background: rgba(246, 250, 253, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.benchmark-item span,
.benchmark-item small {
  display: block;
  color: #7a8fa0;
  font-size: 12px;
  line-height: 1.45;
}

.benchmark-item strong {
  display: block;
  margin: 4px 0 2px;
  font-size: 19px;
  line-height: 1.1;
  font-weight: 950;
  font-variant-numeric: tabular-nums;
}

.ai-export-card {
  display: grid;
  gap: 8px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(238, 246, 251, 0.9));
}

.ai-export-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.ai-export-main p {
  margin: -4px 0 0;
  line-height: 1.35;
}

.ai-export-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex: 0 0 auto;
}

.ai-export-actions :deep(.ant-btn) {
  min-width: 112px;
}

.ai-export-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-export-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  color: #123044;
  font-size: 12px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.ai-report-preview {
  max-height: 340px;
  margin: 0;
  padding: 10px;
  overflow: auto;
  white-space: pre-wrap;
  color: #e8e8e4;
  font-size: 12px;
  line-height: 1.55;
  border-radius: 11px;
  background: rgba(5, 5, 5, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.backtest-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 12px;
}

.curve-chart {
  height: 390px;
  border-radius: 12px;
  background: rgba(246, 250, 253, 0.86);
}

.position-status {
  height: 32px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  font-weight: 900;
  margin-bottom: 8px;
}

.position-status.holding {
  color: #34d399;
  background: rgba(52, 211, 153, 0.14);
  border: 1px solid rgba(52, 211, 153, 0.36);
}

.position-status.empty {
  color: #cbd5e1;
  background: rgba(148, 163, 184, 0.12);
  border: 1px solid rgba(148, 163, 184, 0.28);
}

.position-list {
  display: grid;
  gap: 8px;
}

.position-list > div {
  min-height: 48px;
  padding: 8px 10px;
  border-radius: 11px;
  background: rgba(246, 250, 253, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.position-list span {
  display: block;
  color: #7a8fa0;
  font-size: 12px;
}

.position-list strong {
  display: block;
  margin-top: 4px;
  color: #123044;
  font-size: 15px;
}

@media (max-width: 1280px) {
  .backtest-grid {
    grid-template-columns: 1fr;
  }

  .benchmark-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quality-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .compare-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .backtest-form {
    grid-template-columns: repeat(2, minmax(190px, 1fr));
  }

  .strategy-field,
  .date-range-field {
    grid-column: auto;
  }
}

@media (max-width: 780px) {
  .backtest-form {
    grid-template-columns: 1fr;
  }

  .strategy-field {
    grid-column: auto;
  }

  .batch-head {
    flex-direction: column;
  }

  .batch-actions,
  .batch-actions :deep(.ant-btn) {
    width: 100%;
  }

  .compare-head {
    flex-direction: column;
  }

  .compare-metric-grid {
    grid-template-columns: 1fr;
  }

  .benchmark-head {
    flex-direction: column;
  }

  .benchmark-grid {
    grid-template-columns: 1fr;
  }

  .quality-head {
    flex-direction: column;
  }

  .quality-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .benchmark-badge {
    width: 100%;
  }

  .ai-export-main {
    flex-direction: column;
  }

  .ai-export-actions,
  .ai-export-actions :deep(.ant-btn) {
    width: 100%;
  }
}
</style>
