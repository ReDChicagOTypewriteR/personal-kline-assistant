<template>
  <div class="page dashboard-page">
    <div class="dashboard-hero">
      <div class="dashboard-copy">
        <div class="section-title">ETF 技术总览</div>
      </div>
      <div class="dashboard-command-bar">
        <a-button type="primary" :loading="calcLoading" @click="calculateAll">
          <template #icon><CalculatorOutlined /></template>
          计算全部指标
        </a-button>
        <a-button :loading="signalLoading" @click="generateAll">
          <template #icon><FundProjectionScreenOutlined /></template>
          生成全部信号
        </a-button>
        <a-button class="warning-button" :loading="batchAiLoading" :disabled="!latestTradeDate" @click="analyzeBuyCandidates">
          <template #icon><RobotOutlined /></template>
          批量 AI 风险分析
        </a-button>
        <a-button @click="load">
          <template #icon><ReloadOutlined /></template>
          刷新 Dashboard
        </a-button>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric" :class="item.className">
        <div class="metric-icon">
          <component :is="item.icon" />
        </div>
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
        <div class="metric-note">{{ item.note }}</div>
      </div>
    </div>

    <DraggableWorkspace
      storage-key="dashboard-workspace-layout-v1"
      :items="dashboardWorkspaceItems"
      @layout-change="renderDistribution"
    >
      <template #actions>
        <div class="decision-panel primary-panel">
          <div class="panel-head">
            <div>
              <div class="section-title">今日行动</div>
            </div>
            <strong>{{ actionableSignals.length }}</strong>
          </div>
          <div v-if="actionableSignals.length" class="signal-stack">
            <button v-for="item in actionableSignals" :key="`action-${item.symbolCode}-${item.tradeDate}`" class="signal-card" @click="goDetail(item.symbolCode)">
              <span class="signal-symbol">{{ item.symbolCode }}</span>
              <SignalBadge :signal-type="item.finalAction || item.signalType" />
              <span class="signal-meta">
                <span>评分 {{ item.technicalScore ?? '-' }}</span>
                <span>风险 {{ fmtInt(item.aiRiskScore) }}</span>
                <span>{{ fmt(item.closePrice) }}</span>
              </span>
            </button>
          </div>
          <a-empty v-else description="暂无可行动候选" />
        </div>
      </template>

      <template #risk>
        <div class="decision-panel">
          <div class="panel-head">
            <div>
              <div class="section-title">风险队列</div>
            </div>
            <strong class="value-down">{{ riskSignals.length }}</strong>
          </div>
          <div v-if="riskSignals.length" class="signal-stack compact">
            <button v-for="item in riskSignals" :key="`risk-${item.symbolCode}-${item.tradeDate}`" class="signal-card risk" @click="goDetail(item.symbolCode)">
              <span class="signal-symbol">{{ item.symbolCode }}</span>
              <SignalBadge :signal-type="item.signalType" />
              <span class="signal-meta">
                <span>{{ item.aiRiskLevel || '风险待评估' }}</span>
                <span>{{ item.riskNotes[0] || item.trendState }}</span>
              </span>
            </button>
          </div>
          <a-empty v-else description="暂无高风险 ETF" />
        </div>
      </template>

      <template #ranking>
        <div class="decision-panel">
          <div class="panel-head">
            <div>
              <div class="section-title">强势排行</div>
            </div>
          </div>
          <div v-if="topScores.length" class="rank-list">
            <button v-for="(item, index) in topScores" :key="`score-${item.symbolCode}`" class="rank-row" @click="goDetail(item.symbolCode)">
              <span>{{ index + 1 }}</span>
              <strong>{{ item.symbolCode }}</strong>
              <em>{{ item.technicalScore }}</em>
            </button>
          </div>
          <a-empty v-else description="暂无评分" />
        </div>
      </template>

      <template #signals>
        <div class="table-card signal-table-card">
          <div class="panel-head table-panel-head">
            <div>
              <div class="section-title">最新技术信号</div>
            </div>
            <div class="table-summary"><span class="table-count-pill">{{ etfLatestSignals.length }} 条</span></div>
          </div>
          <a-table
            class="app-table dashboard-table"
            :data-source="etfLatestSignals"
            :loading="loading"
            :pagination="false"
            :row-key="latestSignalRowKey"
            size="small"
            :expand-row-by-click="false"
            :scroll="{ y: 420, x: 1180 }"
          >
            <template #expandedRowRender="{ record }">
              <div class="signal-expanded">
                <div>
                  <h4>技术原因</h4>
                  <p>{{ record.reasons?.join('；') || '暂无技术原因' }}</p>
                </div>
                <div>
                  <h4>风险提示</h4>
                  <p>{{ record.riskNotes?.join('；') || '暂无风险提示' }}</p>
                </div>
                <div>
                  <h4>最终动作</h4>
                  <p>{{ signalLabel(record.finalAction) || '暂无最终动作' }} · {{ record.positionPolicy || '暂无仓位策略' }}</p>
                </div>
              </div>
            </template>
            <a-table-column title="标的" :width="120" fixed="left">
              <template #default="{ record }">
                <button class="symbol-link" @click.stop="goDetail(record.symbolCode)">{{ record.symbolCode }}</button>
              </template>
            </a-table-column>
            <a-table-column data-index="tradeDate" title="日期" :width="120" />
            <a-table-column title="评分" :width="90">
              <template #default="{ record }">
                <strong class="score-text">{{ record.technicalScore }}</strong>
              </template>
            </a-table-column>
            <a-table-column title="信号" :width="150">
              <template #default="{ record }"><SignalBadge :signal-type="record.signalType" /></template>
            </a-table-column>
            <a-table-column title="AI情绪" :width="90">
              <template #default="{ record }">{{ fmtInt(record.aiSentimentScore) }}</template>
            </a-table-column>
            <a-table-column title="AI风险" :width="90">
              <template #default="{ record }">
                <span :class="riskScoreClass(record.aiRiskScore)">{{ fmtInt(record.aiRiskScore) }}</span>
              </template>
            </a-table-column>
            <a-table-column title="趋势 / RSI" :width="160">
              <template #default="{ record }">
                <div class="cell-stack">
                  <strong>{{ record.trendState || '-' }}</strong>
                  <span :class="rsiClass(record.rsi14)">RSI {{ fmt(record.rsi14) }}</span>
                </div>
              </template>
            </a-table-column>
            <a-table-column title="交易参考" :width="230">
              <template #default="{ record }">
                <div class="price-triplet">
                  <span>入 {{ fmt(record.entryReference) }}</span>
                  <span>损 {{ fmt(record.stopLossReference) }}</span>
                  <span>盈 {{ fmt(record.takeProfitReference) }}</span>
                </div>
              </template>
            </a-table-column>
            <a-table-column title="摘要" :width="300">
              <template #default="{ record }">
                <div class="reason-summary">
                  {{ firstLine(record.reasons) || firstLine(record.riskNotes) || '-' }}
                </div>
              </template>
            </a-table-column>
            <a-table-column title="操作" :width="250" fixed="right">
              <template #default="{ record }">
                <div class="row-actions">
                  <a-button
                    v-if="record.signalType === 'BUY_CANDIDATE'"
                    size="small"
                    class="warning-button"
                    :loading="aiLoadingKey === `${record.symbolCode}-${record.tradeDate}`"
                    @click.stop="analyzeRow(record)"
                  >
                    <template #icon><RobotOutlined /></template>
                    AI 分析
                  </a-button>
                  <span v-else class="muted action-note">无需分析</span>
                  <a-button size="small" @click.stop="goDetail(record.symbolCode)">
                    <template #icon><LineChartOutlined /></template>
                    K 线
                  </a-button>
                  <a-button size="small" danger @click.stop="deleteSignal(record)">
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
                </div>
              </template>
            </a-table-column>
          </a-table>
        </div>
      </template>

      <template #distribution>
        <div class="distribution-card">
          <div class="section-title">信号分布</div>
          <div v-if="totalSignalCount" ref="distributionRef" class="distribution-chart"></div>
          <a-empty v-else description="暂无信号分布" />
        </div>
      </template>

      <template #watch>
        <div class="watch-card">
          <div class="section-title">今日观察</div>
          <div class="watch-block">
            <h3>风险提示 Top 5</h3>
            <div v-for="item in riskTop" :key="`risk-${item.symbolCode}`" class="risk-row">
              <span>{{ item.symbolCode }}</span>
              <small>{{ item.riskNotes[0] || item.signalType }}</small>
            </div>
            <a-empty v-if="!riskTop.length" description="暂无风险提示" />
          </div>
          <div class="updated-time">最近更新时间：{{ updatedAt }}</div>
        </div>
      </template>

      <template #finalDecision>
        <div class="final-table-card">
          <div class="panel-head table-panel-head">
            <div>
              <div class="section-title">最终交易辅助决策</div>
            </div>
            <div class="table-summary"><span class="table-count-pill">{{ etfFinalDecisions.length }} 条</span></div>
          </div>
          <a-table
            class="app-table dashboard-table"
            :data-source="etfFinalDecisions"
            :pagination="false"
            :row-key="finalDecisionRowKey"
            size="small"
            :scroll="{ y: 300, x: 1360 }"
          >
            <a-table-column data-index="symbolCode" title="标的" :width="110" />
            <a-table-column data-index="decisionDate" title="日期" :width="120" />
            <a-table-column title="K线信号" :width="150">
              <template #default="{ record }">
                <SignalBadge :signal-type="record.technicalSignal" />
              </template>
            </a-table-column>
            <a-table-column data-index="technicalScore" title="技术评分" :width="100" />
            <a-table-column data-index="aiSentimentScore" title="AI情绪分" :width="100" />
            <a-table-column data-index="aiRiskScore" title="AI风险分" :width="100" />
            <a-table-column data-index="aiRiskLevel" title="AI风险等级" :width="120" />
            <a-table-column title="最终动作" :width="160">
              <template #default="{ record }">
                <SignalBadge v-if="record.finalAction" :signal-type="record.finalAction" />
                <span v-else>-</span>
              </template>
            </a-table-column>
            <a-table-column data-index="positionPolicy" title="仓位策略" :width="110" />
            <a-table-column data-index="decisionReason" title="决策原因" :width="320" />
            <a-table-column data-index="rejectReason" title="拒绝原因" :width="280" />
          </a-table>
        </div>
      </template>
    </DraggableWorkspace>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { PieChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use, type EChartsType } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {
  AlertOutlined,
  BarChartOutlined,
  CalculatorOutlined,
  DashboardOutlined,
  DeleteOutlined,
  FundProjectionScreenOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  ReloadOutlined,
  RobotOutlined,
  StarOutlined
} from '@ant-design/icons-vue'
import { aiAnalysisApi } from '@/api/aiAnalysisApi'
import { dashboardApi } from '@/api/dashboardApi'
import { finalDecisionApi } from '@/api/finalDecisionApi'
import { indicatorApi } from '@/api/indicatorApi'
import { signalApi } from '@/api/signalApi'
import SignalBadge from '@/components/SignalBadge.vue'
import DraggableWorkspace from '@/components/workspace/DraggableWorkspace.vue'
import { confirmAction, feedback } from '@/utils/feedback'
import { formatInteger, formatNumber, riskScoreClass, rsiClass } from '@/utils/formatters'
import { signalLabel } from '@/utils/labels'
import type { DashboardDTO, FinalTradeDecisionDTO, TechnicalSignalDTO } from '@/types'

use([PieChart, LegendComponent, TooltipComponent, CanvasRenderer])

interface DashboardWorkspaceItem {
  id: string
  title: string
  x: number
  y: number
  w: number
  h: number
  minW?: number
  minH?: number
}

const dashboard = reactive<DashboardDTO>({
  totalSymbols: 0,
  totalWatchlist: 0,
  buyCandidateCount: 0,
  watchCount: 0,
  avoidCount: 0,
  latestSignals: []
})
const router = useRouter()
const loading = ref(false)
const calcLoading = ref(false)
const signalLoading = ref(false)
const batchAiLoading = ref(false)
const aiLoadingKey = ref('')
const updatedAt = ref('-')
const finalDecisions = ref<FinalTradeDecisionDTO[]>([])
const distributionRef = ref<HTMLDivElement>()
let chart: EChartsType | null = null

const dashboardWorkspaceItems: DashboardWorkspaceItem[] = [
  { id: 'actions', title: '今日行动', x: 0, y: 0, w: 5, h: 4, minW: 4, minH: 3 },
  { id: 'risk', title: '风险队列', x: 5, y: 0, w: 4, h: 4, minW: 3, minH: 3 },
  { id: 'ranking', title: '强势排行', x: 9, y: 0, w: 3, h: 4, minW: 3, minH: 3 },
  { id: 'signals', title: '最新技术信号', x: 0, y: 4, w: 8, h: 7, minW: 6, minH: 5 },
  { id: 'distribution', title: '信号分布', x: 8, y: 4, w: 4, h: 4, minW: 3, minH: 3 },
  { id: 'watch', title: '今日观察', x: 8, y: 8, w: 4, h: 3, minW: 3, minH: 3 },
  { id: 'finalDecision', title: '最终交易辅助决策', x: 0, y: 11, w: 12, h: 5, minW: 8, minH: 4 }
]

const etfCodePattern = /^(510|511|512|513|515|516|517|518|519|520|521|522|560|561|562|563|588|589|159)\d{3}$/
const isEtfCode = (symbolCode?: string) => !!symbolCode && etfCodePattern.test(symbolCode)
const etfLatestSignals = computed(() => dashboard.latestSignals.filter((item) => isEtfCode(item.symbolCode)))
const etfFinalDecisions = computed(() => finalDecisions.value.filter((item) => isEtfCode(item.symbolCode)))

const metrics = computed(() => [
  { label: 'ETF 信号数', value: etfLatestSignals.value.length, note: '最新 ETF 技术信号', icon: DashboardOutlined, className: 'metric-blue' },
  { label: '自选池数量', value: dashboard.totalWatchlist, note: '重点观察 ETF', icon: StarOutlined, className: 'metric-cyan' },
  { label: '买入候选', value: signalCounts.value.BUY_CANDIDATE, note: '需要进一步确认', icon: BarChartOutlined, className: 'metric-green' },
  { label: '观察中', value: signalCounts.value.WATCH, note: '等待条件成熟', icon: RadarChartOutlined, className: 'metric-blue' },
  { label: '规避风险', value: signalCounts.value.AVOID, note: '暂不参与', icon: AlertOutlined, className: 'metric-red' }
])

const topScores = computed(() =>
  [...etfLatestSignals.value]
    .sort((a, b) => Number(b.technicalScore || 0) - Number(a.technicalScore || 0))
    .slice(0, 5)
)

const actionableSignals = computed(() =>
  etfLatestSignals.value
    .filter((item) => item.signalType === 'BUY_CANDIDATE' || item.finalAction === 'ALLOW_SIMULATION')
    .slice(0, 6)
)

const riskSignals = computed(() =>
  etfLatestSignals.value
    .filter((item) => item.signalType === 'AVOID' || item.signalType === 'SELL_WARNING' || Number(item.aiRiskScore || 0) >= 70)
    .slice(0, 6)
)

const riskTop = computed(() =>
  etfLatestSignals.value
    .filter((item) => item.signalType === 'AVOID' || item.signalType === 'SELL_WARNING' || item.riskNotes.length)
    .slice(0, 5)
)

const latestTradeDate = computed(() => etfLatestSignals.value[0]?.tradeDate || '')
const totalSignalCount = computed(() => Object.values(signalCounts.value).reduce((sum, value) => sum + value, 0))

const signalCounts = computed(() => {
  const counts: Record<string, number> = {
    BUY_CANDIDATE: 0,
    WATCH: 0,
    NEUTRAL: 0,
    AVOID: 0,
    SELL_WARNING: 0
  }
  etfLatestSignals.value.forEach((item) => {
    counts[item.signalType] = (counts[item.signalType] || 0) + 1
  })
  return counts
})

const load = async () => {
  loading.value = true
  try {
    const [dashboardResult, decisions] = await Promise.all([
      dashboardApi.get(),
      finalDecisionApi.latest()
    ])
    Object.assign(dashboard, dashboardResult)
    finalDecisions.value = decisions
    updatedAt.value = new Intl.DateTimeFormat('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).format(new Date())
    renderDistribution()
  } finally {
    loading.value = false
  }
}

const calculateAll = async () => {
  calcLoading.value = true
  try {
    const result = await indicatorApi.calculateAll()
    feedback.success(`已计算 ${result.count} 条指标`)
    await load()
  } finally {
    calcLoading.value = false
  }
}

const generateAll = async () => {
  signalLoading.value = true
  try {
    const result = await signalApi.generateAll()
    feedback.success(`已生成 ${result.count} 条信号`)
    await load()
  } finally {
    signalLoading.value = false
  }
}

const analyzeRow = async (row: TechnicalSignalDTO) => {
  if (row.signalType !== 'BUY_CANDIDATE') return
  aiLoadingKey.value = `${row.symbolCode}-${row.tradeDate}`
  try {
    const result = await aiAnalysisApi.analyze(row.symbolCode, row.tradeDate)
    feedback.success(result.message || 'AI 风险分析已完成')
    await load()
  } finally {
    aiLoadingKey.value = ''
  }
}

const analyzeBuyCandidates = async () => {
  if (!latestTradeDate.value) return
  batchAiLoading.value = true
  try {
    const result = await aiAnalysisApi.analyzeBuyCandidates(latestTradeDate.value)
    feedback.success(`AI 分析完成：成功 ${result.successCount}，失败 ${result.failedCount}，缓存跳过 ${result.skippedCount}`)
    await load()
  } finally {
    batchAiLoading.value = false
  }
}

const deleteSignal = async (row: TechnicalSignalDTO) => {
  try {
    await confirmAction({
      title: '删除技术信号',
      content: `确认删除 ${row.symbolCode} 在 ${row.tradeDate} 的技术信号吗？`,
      okText: '删除',
      danger: true
    })
    const result = await signalApi.delete(row.symbolCode, row.tradeDate)
    feedback.success(`已删除 ${result.count} 条技术信号`)
    await load()
  } catch {
    // user cancelled
  }
}

const renderDistribution = async () => {
  await nextTick()
  if (!distributionRef.value || !totalSignalCount.value) return
  if (!chart) chart = init(distributionRef.value, 'dark')
  const instance = chart
  instance.setOption(distributionOption(), true)
  instance.resize()
}

const distributionOption = (): EChartsOption => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(255, 255, 255, 0.98)',
    borderColor: 'rgba(148, 173, 198, 0.38)',
    textStyle: { color: '#102033' }
  },
  legend: {
    bottom: 0,
    textStyle: { color: '#52677d' },
    itemWidth: 10,
    itemHeight: 10
  },
  series: [
    {
      type: 'pie',
      radius: ['48%', '70%'],
      center: ['50%', '43%'],
      avoidLabelOverlap: true,
      label: { color: '#334155', fontWeight: 700 },
      itemStyle: { borderColor: '#ffffff', borderWidth: 3 },
      data: [
        { name: 'BUY', value: signalCounts.value.BUY_CANDIDATE, itemStyle: { color: '#16a34a' } },
        { name: 'WATCH', value: signalCounts.value.WATCH, itemStyle: { color: '#38bdf8' } },
        { name: 'NEUTRAL', value: signalCounts.value.NEUTRAL, itemStyle: { color: '#94a3b8' } },
        { name: 'AVOID', value: signalCounts.value.AVOID, itemStyle: { color: '#e11d48' } },
        { name: 'SELL', value: signalCounts.value.SELL_WARNING, itemStyle: { color: '#d97706' } }
      ]
    }
  ]
})

const fmt = formatNumber
const fmtInt = formatInteger
const firstLine = (value?: string[]) => value?.find((item) => item && item.trim()) || ''
const goDetail = (symbolCode: string) => {
  router.push(`/kline-detail/${symbolCode}`)
}
const latestSignalRowKey = (row: TechnicalSignalDTO) => `${row.symbolCode}-${row.tradeDate}-${row.signalType}`
const finalDecisionRowKey = (row: FinalTradeDecisionDTO) => `${row.symbolCode}-${row.decisionDate}`

watch(signalCounts, renderDistribution, { deep: true })

onMounted(() => {
  load()
  window.addEventListener('resize', renderDistribution)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderDistribution)
  chart?.dispose()
})
</script>

<style scoped>
.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(238, 246, 251, 0.88));
}

.dashboard-copy p {
  margin: -4px 0 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.dashboard-command-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.metric-icon {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  color: currentColor;
  background: rgba(255, 255, 255, 0.06);
  margin-bottom: 8px;
}

.metric-blue {
  color: #1c5fb9;
}

.metric-cyan {
  color: #7a8fa0;
}

.metric-green {
  color: #34d399;
}

.metric-red {
  color: #fb7185;
}

.decision-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  height: 100%;
  padding: 0;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.panel-head p {
  margin: -4px 0 0;
  line-height: 1.35;
}

.panel-head > strong {
  color: #123044;
  font-size: 24px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.table-panel-head {
  margin-bottom: 6px;
}

.signal-stack {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.signal-stack.compact {
  grid-template-columns: 1fr;
}

.signal-card,
.rank-row,
.mobile-signal-card {
  width: 100%;
  border: 1px solid rgba(129, 153, 174, 0.3);
  background: rgba(246, 250, 253, 0.92);
  color: #123044;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease, transform 0.16s ease;
}

.signal-card:hover,
.rank-row:hover,
.mobile-signal-card:hover {
  border-color: rgba(47, 128, 237, 0.26);
  background: rgba(255, 255, 255, 0.96);
  transform: translateY(-1px);
}

.signal-card {
  display: grid;
  gap: 7px;
  min-height: 82px;
  padding: 10px;
  border-radius: 10px;
  text-align: left;
}

.signal-card.risk {
  border-color: rgba(251, 113, 133, 0.2);
}

.signal-symbol {
  font-size: 16px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.signal-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #7a8fa0;
  font-size: 12px;
}

.signal-meta span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-list {
  display: grid;
  gap: 6px;
}

.rank-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 10px;
  border-radius: 10px;
  text-align: left;
}

.rank-row span {
  color: #7a8fa0;
}

.rank-row strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-row em {
  color: #4ade80;
  font-style: normal;
  font-weight: 900;
}

.signal-table-card,
.distribution-card,
.watch-card,
.final-table-card {
  height: 100%;
  min-width: 0;
  padding: 0;
}

.dashboard-table {
  width: 100%;
}

.dashboard-table :deep(.ant-table-cell) {
  padding: 7px 9px;
  line-height: 1.28;
  white-space: nowrap;
}

.dashboard-table :deep(.ant-tag) {
  max-width: 100%;
}

.dashboard-table :deep(.clickable-signal-row) {
  cursor: pointer;
}

.symbol-link {
  padding: 0;
  border: 0;
  color: #123044;
  background: transparent;
  cursor: pointer;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.symbol-link:hover {
  color: #4ade80;
}

.signal-expanded {
  display: grid;
  grid-template-columns: 1fr 1fr 0.8fr;
  gap: 10px;
  padding: 2px 8px 6px;
}

.signal-expanded h4 {
  margin: 0 0 6px;
  color: #123044;
  font-size: 13px;
}

.signal-expanded p {
  margin: 0;
  color: #7a8fa0;
  line-height: 1.6;
}

.cell-stack {
  display: grid;
  gap: 3px;
}

.cell-stack span {
  color: #7a8fa0;
  font-size: 12px;
}

.price-triplet {
  display: flex;
  gap: 6px;
  color: #123044;
  font-size: 12px;
}

.reason-summary {
  max-width: 280px;
  color: #123044;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  min-width: 0;
  white-space: nowrap;
}

.row-actions :deep(.ant-btn) {
  margin-left: 0;
}

.action-note {
  width: 68px;
  flex: 0 0 68px;
  font-size: 12px;
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

.risk-tag-low {
  color: #4ade80;
  background: rgba(74, 222, 128, 0.1);
  border-color: rgba(74, 222, 128, 0.22);
}

.risk-tag-medium {
  color: #facc15;
  background: rgba(250, 204, 21, 0.1);
  border-color: rgba(250, 204, 21, 0.22);
}

.risk-tag-high {
  color: #fb7185;
  background: rgba(251, 113, 133, 0.1);
  border-color: rgba(251, 113, 133, 0.22);
}

.risk-tag-unknown {
  color: #7a8fa0;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.14);
}

.distribution-chart {
  height: 100%;
  min-height: 180px;
}

.distribution-card :deep(.ant-empty) {
  min-height: 180px;
  display: grid;
  place-items: center;
}

.distribution-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 8px;
}

.watch-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 10px;
}

.watch-block h3 {
  margin: 0 0 6px;
  color: #123044;
  font-size: 13px;
}

.watch-row,
.risk-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 30px;
  padding: 0 8px;
  border-radius: 10px;
  background: rgba(246, 250, 253, 0.9);
  color: #123044;
  margin-top: 6px;
}

.watch-row strong {
  color: #123044;
  text-shadow: none;
}

.risk-row small {
  max-width: 210px;
  color: #fb7185;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.updated-time {
  color: #7a8fa0;
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 8px;
}

.score-text {
  color: #123044;
}

@media (max-width: 900px) {
  .dashboard-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .dashboard-command-bar,
  .dashboard-command-bar :deep(.ant-btn) {
    width: 100%;
  }

  .signal-stack {
    grid-template-columns: 1fr;
  }

  .dashboard-table :deep(.ant-table-body) {
    max-height: 360px !important;
  }

  .signal-expanded {
    grid-template-columns: 1fr;
  }
}
</style>
