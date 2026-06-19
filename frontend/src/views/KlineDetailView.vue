<template>
  <div class="page kline-page">
    <div class="kline-workbar">
      <div class="workbar-copy">
        <div class="section-title">行情工作台</div>
      </div>
      <div class="kline-toolbar">
        <label class="control-field symbol-field">
          <span>ETF</span>
          <SymbolSelector v-model="symbolCode" />
        </label>
        <label class="control-field date-field">
          <span>区间</span>
          <DateRangeControl v-model:value="dateRange" :presets="klineDatePresets" />
        </label>
        <div class="control-field period-field">
          <span>周期</span>
          <a-radio-group v-model:value="period" class="period-group" button-style="solid">
            <a-radio-button v-for="item in periods" :key="item" :value="item">{{ item }}</a-radio-button>
          </a-radio-group>
        </div>
      </div>
      <div class="action-strip">
        <a-tooltip :title="!symbolCode ? '请先选择 ETF' : ''">
          <span><a-button type="primary" :loading="loading" :disabled="!symbolCode" @click="load">
            <template #icon><CloudDownloadOutlined /></template>
            刷新行情
          </a-button></span>
        </a-tooltip>
        <a-tooltip :title="!symbolCode ? '请先选择 ETF' : ''">
          <span><a-button :disabled="!symbolCode" :loading="calcLoading" @click="calculate">
            <template #icon><CalculatorOutlined /></template>
            计算指标
          </a-button></span>
        </a-tooltip>
        <a-tooltip :title="!symbolCode ? '请先选择 ETF' : ''">
          <span><a-button :disabled="!symbolCode" :loading="signalLoading" @click="generateSignal">
            <template #icon><FundProjectionScreenOutlined /></template>
            生成信号
          </a-button></span>
        </a-tooltip>
        <a-tooltip :title="aiDisabledReason">
          <span><a-button class="warning-button" :disabled="!!aiDisabledReason" :loading="aiLoading" @click="analyzeAi">
            <template #icon><RobotOutlined /></template>
            AI 风险分析
          </a-button></span>
        </a-tooltip>
      </div>
    </div>

    <div class="kline-layout">
      <div class="glass-card chart-card">
        <a-spin :spinning="loading">
          <div class="chart-header">
            <div>
              <div class="symbol-title">{{ symbolCode || '未选择 ETF' }}</div>
              <div class="muted">{{ period }} · {{ dateRange?.[0] }} ~ {{ dateRange?.[1] }}</div>
            </div>
            <div class="chart-stats">
              <div>
                <span>最新收盘</span>
                <strong>{{ fmt(latestClose) }}</strong>
              </div>
              <div>
                <span>涨跌幅</span>
                <strong :class="changePct >= 0 ? 'value-up' : 'value-down'">{{ fmt(changePct) }}%</strong>
              </div>
              <div>
                <span>当前信号</span>
                <SignalBadge :signal-type="latestSignal?.signalType" />
              </div>
              <div>
                <span>技术评分</span>
                <strong class="value-blue">{{ latestSignal?.technicalScore ?? '-' }}</strong>
              </div>
            </div>
          </div>
          <KlineChart :data="chartData" :period="period" :symbol-code="symbolCode" :levels="chartLevels" :signals="signalHistory" />
        </a-spin>
      </div>

      <div class="side-decision-stack">
        <div class="glass-card decision-card">
          <div class="section-title">当前判断</div>
          <div class="decision-status">
            <SignalBadge :signal-type="latestSignal?.signalType" />
            <strong :class="riskScoreClass(aiAnalysis?.riskScore)">风险 {{ fmtInt(aiAnalysis?.riskScore) }}</strong>
          </div>
          <div class="decision-metrics">
            <div>
              <span>技术评分</span>
              <strong>{{ latestSignal?.technicalScore ?? '-' }}</strong>
            </div>
            <div>
              <span>风险等级</span>
              <strong>{{ aiAnalysis?.riskLevel || '-' }}</strong>
            </div>
            <div>
              <span>最终动作</span>
              <strong>{{ signalLabel(finalDecision?.finalAction) || '-' }}</strong>
            </div>
            <div>
              <span>仓位策略</span>
              <strong>{{ finalDecision?.positionPolicy || '-' }}</strong>
            </div>
          </div>
          <div class="trade-levels">
            <div><span>入场</span><strong>{{ fmt(latestSignal?.entryReference) }}</strong></div>
            <div><span>止损</span><strong class="value-down">{{ fmt(latestSignal?.stopLossReference) }}</strong></div>
            <div><span>止盈</span><strong class="value-up">{{ fmt(latestSignal?.takeProfitReference) }}</strong></div>
          </div>
        </div>

        <div class="glass-card indicator-card">
          <div class="section-title">最新指标</div>
          <IndicatorPanel :indicator="latestIndicator" :close-price="latestClose" />
        </div>
      </div>
    </div>

    <div class="glass-card analysis-tabs-card">
      <a-tabs v-model:activeKey="analysisTab" class="analysis-tabs">
        <a-tab-pane key="signal" tab="信号解释">
          <div class="signal-explain-grid">
            <div class="explain-card">
              <div class="section-title">技术优势</div>
              <a-empty v-if="!latestSignal?.reasons?.length" description="暂无技术优势" />
              <ul v-else class="explain-list">
                <li v-for="item in latestSignal.reasons" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div class="explain-card risk-card">
              <div class="section-title">风险提示</div>
              <a-empty v-if="!latestSignal?.riskNotes?.length" description="暂无风险提示" />
              <ul v-else class="explain-list">
                <li v-for="item in latestSignal.riskNotes" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="ai" tab="AI 风险">
          <div class="ai-card">
            <div class="ai-card-header">
              <div class="section-title">AI 风险分析</div>
              <div class="ai-actions">
                <SignalBadge v-if="finalDecision?.finalAction" :signal-type="finalDecision.finalAction" />
                <a-button
                  v-if="!aiAnalysis && latestSignal?.signalType === 'BUY_CANDIDATE'"
                  size="small"
                  class="warning-button"
                  :loading="aiLoading"
                  @click="analyzeAi"
                >
                  <template #icon><RobotOutlined /></template>
                  立即分析
                </a-button>
              </div>
            </div>

            <a-empty v-if="!aiAnalysis" description="暂无 AI 分析" />
            <template v-else>
              <div class="ai-score-grid">
                <div>
                  <span>情绪评分</span>
                  <strong>{{ fmtInt(aiAnalysis.sentimentScore) }}</strong>
                </div>
                <div>
                  <span>风险评分</span>
                  <strong :class="riskScoreClass(aiAnalysis.riskScore)">{{ fmtInt(aiAnalysis.riskScore) }}</strong>
                </div>
                <div>
                  <span>风险等级</span>
                  <strong>{{ aiAnalysis.riskLevel || 'UNKNOWN' }}</strong>
                </div>
                <div>
                  <span>市场状态</span>
                  <strong>{{ aiAnalysis.marketState || 'UNKNOWN' }}</strong>
                </div>
                <div>
                  <span>最终约束</span>
                  <strong>{{ aiAnalysis.actionConstraint || 'UNKNOWN' }}</strong>
                </div>
                <div>
                  <span>仓位策略</span>
                  <strong>{{ finalDecision?.positionPolicy || '-' }}</strong>
                </div>
              </div>

              <div class="ai-summary">{{ aiAnalysis.summary || aiAnalysis.errorMessage || '暂无摘要' }}</div>

              <div class="ai-factor-grid">
                <div>
                  <h3>利好因素</h3>
                  <ul>
                    <li v-for="item in factorLines(aiAnalysis.positiveFactors)" :key="`pos-${item}`">{{ item }}</li>
                  </ul>
                </div>
                <div>
                  <h3>利空因素</h3>
                  <ul>
                    <li v-for="item in factorLines(aiAnalysis.negativeFactors)" :key="`neg-${item}`">{{ item }}</li>
                  </ul>
                </div>
                <div>
                  <h3>风险因素</h3>
                  <ul>
                    <li v-for="item in factorLines(aiAnalysis.riskFactors)" :key="`risk-${item}`">{{ item }}</li>
                  </ul>
                </div>
                <div>
                  <h3>反方观点</h3>
                  <p>{{ aiAnalysis.contrarianView || '-' }}</p>
                </div>
              </div>

              <div class="ai-footer">
                <div>{{ finalDecision?.decisionReason || finalDecision?.rejectReason || '' }}</div>
                <a-button :disabled="!aiAnalysis.rawReport" @click="rawReportVisible = true">
                  <template #icon><FileSearchOutlined /></template>
                  查看完整报告
                </a-button>
              </div>
            </template>
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <a-modal v-model:open="rawReportVisible" title="完整 AI 报告" width="780px" :footer="null" class="raw-report-dialog">
      <pre class="raw-report">{{ aiAnalysis?.rawReport }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { CalculatorOutlined, CloudDownloadOutlined, FileSearchOutlined, FundProjectionScreenOutlined, RobotOutlined } from '@ant-design/icons-vue'
import KlineChart from '@/components/KlineChart.vue'
import IndicatorPanel from '@/components/IndicatorPanel.vue'
import SignalBadge from '@/components/SignalBadge.vue'
import SymbolSelector from '@/components/SymbolSelector.vue'
import DateRangeControl from '@/components/DateRangeControl.vue'
import { aiAnalysisApi } from '@/api/aiAnalysisApi'
import { dashboardApi } from '@/api/dashboardApi'
import { indicatorApi } from '@/api/indicatorApi'
import { klineApi } from '@/api/klineApi'
import { signalApi } from '@/api/signalApi'
import { feedback } from '@/utils/feedback'
import { formatInteger, formatNumber, riskScoreClass } from '@/utils/formatters'
import { signalLabel } from '@/utils/labels'
import type { AiAnalysisSnapshotDTO, DailyKlineDTO, FinalTradeDecisionDTO, IndicatorSnapshotDTO, KLineDataItem, TechnicalSignalDTO } from '@/types'

type KlinePageState = {
  symbolCode?: string
  period?: string
  dateRange?: [string, string]
}

const KLINE_PAGE_STATE_KEY = 'personal-kline:kline-detail-state'
const route = useRoute()
const periods = ['1m', '5m', '15m', '1h', '2h', '4h', 'D', 'W', 'M', 'Y']
const savedState = readSavedKlineState()
const initialPeriod = periods.includes(savedState.period || '') ? savedState.period as string : 'D'
const symbolCode = ref((route.params.symbolCode as string) || savedState.symbolCode || '')
const dateRange = ref<[string, string]>(savedState.dateRange || defaultRangeForPeriod(initialPeriod))
const klineDatePresets = [
  { key: '1m', label: '近 1 月', days: 30 },
  { key: '3m', label: '近 3 月', days: 90 },
  { key: '6m', label: '近 6 月', days: 180 },
  { key: '1y', label: '近 1 年', days: 365 },
  { key: '3y', label: '近 3 年', days: 1095 }
]
const period = ref(initialPeriod)
const klines = ref<DailyKlineDTO[]>([])
const chartData = ref<KLineDataItem[]>([])
const indicators = ref<IndicatorSnapshotDTO[]>([])
const latestSignal = ref<TechnicalSignalDTO>()
const signalHistory = ref<TechnicalSignalDTO[]>([])
const aiAnalysis = ref<AiAnalysisSnapshotDTO>()
const finalDecision = ref<FinalTradeDecisionDTO>()
const loading = ref(false)
const calcLoading = ref(false)
const signalLoading = ref(false)
const aiLoading = ref(false)
const rawReportVisible = ref(false)
const analysisTab = ref('signal')
let loadVersion = 0

const latestIndicator = computed(() => indicators.value.at(-1))
const chartLevels = computed(() => ({
  entry: latestSignal.value?.entryReference,
  stopLoss: latestSignal.value?.stopLossReference,
  takeProfit: latestSignal.value?.takeProfitReference
}))
const aiDisabledReason = computed(() => {
  if (!symbolCode.value) return '请先选择 ETF'
  if (latestSignal.value?.signalType !== 'BUY_CANDIDATE') return '仅买入候选信号可进行 AI 风险分析'
  return ''
})
const latestClose = computed(() => chartData.value.at(-1)?.close ?? latestSignal.value?.closePrice)
const changePct = computed(() => {
  const last = chartData.value.at(-1)
  const prev = chartData.value.at(-2)
  if (!last || !prev || !prev.close) return 0
  return ((last.close - prev.close) / prev.close) * 100
})

const clearMarketState = () => {
  klines.value = []
  indicators.value = []
  signalHistory.value = []
  latestSignal.value = undefined
  aiAnalysis.value = undefined
  finalDecision.value = undefined
  chartData.value = []
}

const load = async () => {
  const currentSymbol = symbolCode.value?.trim()
  if (!currentSymbol) {
    clearMarketState()
    return
  }
  const currentVersion = ++loadVersion
  const [start, end] = dateRange.value || []
  const currentPeriod = period.value
  loading.value = true
  saveKlinePageState()
  clearMarketState()
  try {
    const [detail, signals] = await Promise.all([
      dashboardApi.symbolDetail(currentSymbol, start, end),
      signalApi.history(currentSymbol).catch(() => [] as TechnicalSignalDTO[])
    ])
    if (currentVersion !== loadVersion || currentSymbol !== symbolCode.value) return
    const rows = await loadChartRows(currentSymbol, currentPeriod, start, end, detail.klines)
    if (currentVersion !== loadVersion || currentSymbol !== symbolCode.value) return
    klines.value = detail.klines
    indicators.value = detail.indicators
    latestSignal.value = detail.latestSignal
    signalHistory.value = filterSignalsByRange(signals, start, end, detail.latestSignal)
    aiAnalysis.value = detail.aiAnalysis
    finalDecision.value = detail.finalDecision
    chartData.value = rows
    saveKlinePageState()
  } catch {
    if (currentVersion === loadVersion) {
      clearMarketState()
    }
  } finally {
    if (currentVersion === loadVersion) {
      loading.value = false
    }
  }
}

const loadChartRows = async (targetSymbol: string, targetPeriod: string, start: string, end: string, dailyRows: DailyKlineDTO[]) => {
  try {
    return await klineApi.chart(targetSymbol, targetPeriod, start, end)
  } catch {
    if (targetPeriod === 'D') {
      feedback.warning('图表行情接口暂不可用，已使用本地日 K 数据显示')
      return dailyRows.map((item) => ({
        timestamp: new Date(`${item.tradeDate}T00:00:00+08:00`).getTime(),
        open: Number(item.open),
        close: Number(item.close),
        high: Number(item.high),
        low: Number(item.low),
        volume: item.volume == null ? undefined : Number(item.volume),
        turnover: item.amount == null ? undefined : Number(item.amount)
      }))
    }
    feedback.warning('当前后端未加载分时图接口，请重启后端后再切换周期')
    return []
  }
}

const calculate = async () => {
  if (!symbolCode.value) return
  calcLoading.value = true
  try {
    const result = await indicatorApi.calculate(symbolCode.value)
    feedback.success(`已计算 ${result.count} 条指标`)
    await load()
  } finally {
    calcLoading.value = false
  }
}

const generateSignal = async () => {
  if (!symbolCode.value) return
  signalLoading.value = true
  try {
    await signalApi.generate(symbolCode.value)
    feedback.success('信号已生成')
    await load()
  } finally {
    signalLoading.value = false
  }
}

const analyzeAi = async () => {
  if (!symbolCode.value || latestSignal.value?.signalType !== 'BUY_CANDIDATE' || !latestSignal.value.tradeDate) return
  aiLoading.value = true
  try {
    const result = await aiAnalysisApi.analyze(symbolCode.value, latestSignal.value.tradeDate)
    aiAnalysis.value = result.aiAnalysis
    finalDecision.value = result.finalDecision
    feedback.success(result.message || 'AI 风险分析已完成')
    await load()
  } finally {
    aiLoading.value = false
  }
}

const fmt = formatNumber
const fmtInt = formatInteger
const factorLines = (value?: string) => (value || '').split('\n').map((item) => item.trim()).filter(Boolean)
function filterSignalsByRange(signals: TechnicalSignalDTO[], start?: string, end?: string, latest?: TechnicalSignalDTO) {
  const rows = [...signals]
  if (latest && !rows.some((item) => item.tradeDate === latest.tradeDate && item.signalType === latest.signalType)) {
    rows.push(latest)
  }
  return rows.filter((item) => {
    if (!item.tradeDate) return false
    if (start && item.tradeDate < start) return false
    if (end && item.tradeDate > end) return false
    return true
  })
}
function today() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

watch(() => route.params.symbolCode, (value) => {
  if (value) {
    symbolCode.value = value as string
  } else if (!symbolCode.value && savedState.symbolCode) {
    symbolCode.value = savedState.symbolCode
  }
})

watch(symbolCode, (value, oldValue) => {
  if (value && value !== oldValue) {
    saveKlinePageState()
    load()
  } else if (!value) {
    ++loadVersion
    loading.value = false
    clearKlinePageState()
    clearMarketState()
  }
})

watch(period, () => {
  dateRange.value = defaultRangeForPeriod(period.value)
  saveKlinePageState()
  if (symbolCode.value) {
    load()
  }
})

watch(dateRange, () => {
  saveKlinePageState()
}, { deep: true })

onMounted(load)

function defaultRangeForPeriod(value: string): [string, string] {
  if (['1m', '5m', '15m'].includes(value)) return rangeFromDays(20)
  if (['1h', '2h', '4h'].includes(value)) return rangeFromDays(120)
  if (value === 'D') return rangeFromDays(365)
  return rangeFromDays(1095)
}

function rangeFromDays(days: number): [string, string] {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - days)
  return [formatDate(start), formatDate(end)]
}

function formatDate(date: Date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function readSavedKlineState(): KlinePageState {
  if (typeof window === 'undefined') return {}
  try {
    const raw = window.localStorage.getItem(KLINE_PAGE_STATE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as KlinePageState
    return {
      symbolCode: typeof parsed.symbolCode === 'string' ? parsed.symbolCode.trim().toUpperCase() : undefined,
      period: typeof parsed.period === 'string' ? parsed.period : undefined,
      dateRange: isValidDateRange(parsed.dateRange) ? parsed.dateRange : undefined
    }
  } catch {
    return {}
  }
}

function saveKlinePageState() {
  if (typeof window === 'undefined') return
  const code = symbolCode.value?.trim().toUpperCase()
  if (!code) return
  window.localStorage.setItem(KLINE_PAGE_STATE_KEY, JSON.stringify({
    symbolCode: code,
    period: period.value,
    dateRange: dateRange.value
  }))
}

function clearKlinePageState() {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(KLINE_PAGE_STATE_KEY)
}

function isValidDateRange(value: unknown): value is [string, string] {
  return Array.isArray(value)
    && value.length === 2
    && typeof value[0] === 'string'
    && typeof value[1] === 'string'
    && /^\d{4}-\d{2}-\d{2}$/.test(value[0])
    && /^\d{4}-\d{2}-\d{2}$/.test(value[1])
}
</script>

<style scoped>
.control-card,
.chart-card,
.indicator-card,
.explain-card,
.ai-card,
.decision-card,
.analysis-tabs-card {
  padding: 12px;
}

.kline-workbar {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(0, 1fr);
  align-items: end;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(246, 250, 253, 0.86));
}

.workbar-copy p {
  margin: -4px 0 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.control-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.control-field > span {
  color: #7a8fa0;
  font-size: 12px;
  font-weight: 800;
}

.chart-card {
  min-width: 0;
}

.indicator-card {
  position: sticky;
  top: 70px;
}

.side-decision-stack {
  display: grid;
  gap: 10px;
  align-self: start;
}

.decision-card {
  display: grid;
  gap: 10px;
}

.decision-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.decision-status > strong {
  font-size: 14px;
}

.decision-metrics,
.trade-levels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.decision-metrics > div,
.trade-levels > div {
  min-height: 52px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(246, 250, 253, 0.92);
  border: 1px solid rgba(129, 153, 174, 0.3);
}

.decision-metrics span,
.trade-levels span {
  display: block;
  color: #7a8fa0;
  font-size: 12px;
  margin-bottom: 4px;
}

.decision-metrics strong,
.trade-levels strong {
  display: block;
  color: #123044;
  font-size: 16px;
  overflow-wrap: anywhere;
}

.trade-levels {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.period-group {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-width: 100%;
}

.period-group :deep(.ant-radio-button-wrapper) {
  height: 28px;
  padding: 5px 8px;
  border-left: 1px solid rgba(129, 153, 174, 0.32);
  border-radius: 9px;
  color: #123044;
  background: rgba(255, 255, 255, 0.78);
  border-color: rgba(129, 153, 174, 0.32);
  line-height: 1.2;
}

.period-group :deep(.ant-radio-button-wrapper-checked) {
  color: #ffffff;
  background: linear-gradient(135deg, #4aa3ff, #2f80ed);
  border-color: transparent;
}

.period-group :deep(.ant-radio-button-wrapper::before) {
  display: none;
}

.kline-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 260px) minmax(270px, 320px) minmax(260px, 1fr);
  align-items: end;
  gap: 8px;
}

.kline-toolbar :deep(.symbol-selector-wrap),
.kline-toolbar :deep(.date-range-picker) {
  width: 100%;
}

.action-strip {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.action-strip :deep(.ant-btn) {
  margin-left: 0;
  min-width: 92px;
}

.action-strip > span {
  display: inline-flex;
}

.date-range-picker {
  width: 100%;
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

.kline-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 12px;
  align-items: start;
}

.chart-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.symbol-title {
  color: #123044;
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 0;
}

.chart-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, auto));
  gap: 8px;
}

.chart-stats > div {
  min-height: 48px;
  padding: 8px 10px;
  border-radius: 11px;
  background: rgba(246, 250, 253, 0.92);
  border: 1px solid rgba(129, 153, 174, 0.3);
}

.chart-stats span {
  display: block;
  color: #7a8fa0;
  font-size: 12px;
  margin-bottom: 3px;
}

.chart-stats strong {
  color: #123044;
  font-size: 16px;
}

.signal-explain-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.analysis-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 10px;
}

.analysis-tabs :deep(.ant-tabs-tab) {
  color: #7a8fa0;
  font-weight: 800;
}

.analysis-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #123044;
}

.analysis-tabs :deep(.ant-tabs-ink-bar) {
  background: #2f80ed;
}

.analysis-tabs :deep(.ant-tabs-nav::before) {
  border-color: rgba(129, 153, 174, 0.3);
}

.explain-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.explain-list li {
  position: relative;
  padding: 9px 12px 9px 30px;
  color: #123044;
  border-radius: 14px;
  background: rgba(246, 250, 253, 0.92);
  border: 1px solid rgba(52, 211, 153, 0.18);
}

.explain-list li::before {
  content: "";
  position: absolute;
  left: 12px;
  top: 14px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #34d399;
  box-shadow: 0 0 14px rgba(52, 211, 153, 0.5);
}

.risk-card .explain-list li {
  border-color: rgba(251, 113, 133, 0.2);
}

.risk-card .explain-list li::before {
  background: #fb7185;
  box-shadow: 0 0 14px rgba(251, 113, 133, 0.5);
}

.ai-card-header,
.ai-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ai-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-score-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(110px, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.ai-score-grid > div {
  min-height: 56px;
  padding: 9px;
  border-radius: 11px;
  background: rgba(246, 250, 253, 0.92);
  border: 1px solid rgba(129, 153, 174, 0.32);
}

.ai-score-grid span {
  display: block;
  color: #7a8fa0;
  font-size: 12px;
  margin-bottom: 4px;
}

.ai-score-grid strong {
  color: #123044;
  font-size: 17px;
}

.ai-summary {
  margin-top: 10px;
  padding: 10px;
  color: #123044;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(129, 153, 174, 0.32);
  line-height: 1.55;
}

.ai-factor-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.ai-factor-grid h3 {
  margin: 0 0 6px;
  color: #123044;
  font-size: 14px;
}

.ai-factor-grid > div {
  min-height: 120px;
  padding: 10px;
  border-radius: 12px;
  background: rgba(246, 250, 253, 0.9);
  border: 1px solid rgba(129, 153, 174, 0.3);
}

.ai-factor-grid > div:nth-child(1) {
  border-color: rgba(52, 211, 153, 0.22);
}

.ai-factor-grid > div:nth-child(2) {
  border-color: rgba(251, 113, 133, 0.22);
}

.ai-factor-grid > div:nth-child(3),
.ai-factor-grid > div:nth-child(4) {
  border-color: rgba(251, 191, 36, 0.24);
}

.ai-factor-grid > div:nth-child(4) {
  background: rgba(251, 191, 36, 0.08);
}

.ai-factor-grid ul {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
  color: #123044;
}

.ai-factor-grid li {
  position: relative;
  padding-left: 16px;
  line-height: 1.45;
}

.ai-factor-grid li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0.72em;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: currentColor;
}

.ai-factor-grid > div:nth-child(1) li::before {
  color: #34d399;
}

.ai-factor-grid > div:nth-child(2) li::before {
  color: #fb7185;
}

.ai-factor-grid > div:nth-child(3) li::before {
  color: #fbbf24;
}

.ai-factor-grid p {
  margin: 0;
  color: #123044;
  line-height: 1.55;
}

.ai-footer {
  margin-top: 10px;
  color: #7a8fa0;
}

.raw-report {
  max-height: 620px;
  overflow: auto;
  white-space: pre-wrap;
  color: #123044;
  background: #f6fafd;
  border-radius: 12px;
  padding: 10px;
}

@media (max-width: 1320px) {
  .kline-layout {
    grid-template-columns: 1fr;
  }

  .indicator-card {
    position: static;
  }

  .kline-workbar {
    grid-template-columns: 1fr;
  }

  .kline-toolbar {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .action-strip :deep(.ant-btn) {
    width: 100%;
  }
}

@media (max-width: 1100px) {
  .chart-header,
  .signal-explain-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .chart-stats {
    width: 100%;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-score-grid,
  .ai-factor-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 780px) {
  .kline-toolbar {
    grid-template-columns: 1fr;
  }

  .action-strip {
    display: grid;
    grid-template-columns: 1fr;
  }

  .decision-metrics,
  .trade-levels {
    grid-template-columns: 1fr;
  }
}
</style>
