<template>
  <div class="page ai-lab-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">Standalone AI Service</p>
        <h2>ETF AI 分析实验室</h2>
        <p>独立调用 etf-ai-analysis 服务，当前不读取回测系统数据。</p>
      </div>
      <div class="status-chip" :class="health?.status === 'UP' ? 'online' : 'offline'">
        <span></span>
        {{ health?.status === 'UP' ? '服务在线' : '未连接' }}
      </div>
    </section>

    <a-collapse v-model:active-key="activeAiPanels" class="ai-fold-panels" :bordered="false">
      <a-collapse-panel key="selection">
        <template #header>
          <div class="fold-header">
            <h3>AI 选 ETF</h3>
            <p>候选排序、新闻上下文和风险理由</p>
          </div>
        </template>
        <section class="selection-card glass-card">
          <div class="section-head">
            <div>
              <h3>AI 选 ETF</h3>
              <p>候选池默认从回测平台 HTTP 加载全市场 ETF，AI 只做研究优先级排序。</p>
            </div>
            <div class="selection-actions">
              <a-button :loading="candidatePoolLoading" @click="loadMarketCandidates">加载全市场 ETF</a-button>
              <a-button @click="fillSelectionDemo">候选示例</a-button>
              <a-button type="primary" :loading="selectingEtfs" @click="runEtfSelection">开始选择</a-button>
              <a-button :loading="loopRunning" @click="runClosedLoop">一键闭环测试</a-button>
            </div>
          </div>

          <div class="candidate-pool-summary">
            <span>候选池 {{ candidatePoolStats.total }} 只</span>
            <span>带技术信号 {{ candidatePoolStats.withSignals }} 只</span>
            <span>来源 {{ candidatePoolSource }}</span>
          </div>

          <div class="selection-layout">
            <div class="selection-inputs">
              <div class="selection-controls">
                <label class="field">
                  <span>市场</span>
                  <a-select v-model:value="selectionForm.market">
                    <a-select-option value="cn">A 股 ETF</a-select-option>
                    <a-select-option value="hk">港股 ETF</a-select-option>
                    <a-select-option value="us">美股 ETF</a-select-option>
                  </a-select>
                </label>
                <label class="field">
                  <span>风险偏好</span>
                  <a-select v-model:value="selectionForm.riskPreference">
                    <a-select-option value="conservative">保守</a-select-option>
                    <a-select-option value="balanced">均衡</a-select-option>
                    <a-select-option value="aggressive">进攻</a-select-option>
                  </a-select>
                </label>
                <label class="field compact-field">
                  <span>结果数</span>
                  <a-input-number v-model:value="selectionForm.maxResults" :min="1" :max="20" />
                </label>
                <label class="field switch-field">
                  <span>实时新闻</span>
                  <a-switch v-model:checked="selectionForm.fetchNews" />
                </label>
              </div>

              <label class="field full">
                <span>候选 ETF JSON</span>
                <a-textarea
                  v-model:value="selectionCandidatesText"
                  :rows="12"
                  placeholder="可手工粘贴候选 ETF 数组；点击“加载全市场 ETF”会自动填充。"
                />
              </label>
            </div>

            <div class="selection-results">
              <a-spin :spinning="selectingEtfs">
                <div v-if="selectionResult?.rankings?.length" class="ranking-list">
                  <article
                    v-for="item in selectionResult.rankings"
                    :key="item.symbolCode"
                    class="ranking-item"
                    :class="item.recommendation.toLowerCase()"
                  >
                    <div class="ranking-main">
                      <div class="rank-badge">#{{ item.rank }}</div>
                      <div>
                        <h4>{{ item.symbolName || item.symbolCode }}</h4>
                        <p>{{ item.symbolCode }}<span v-if="item.trackingIndex"> · {{ item.trackingIndex }}</span></p>
                      </div>
                    </div>
                    <div class="score-box">
                      <strong>{{ item.score }}</strong>
                      <span>{{ item.recommendation }}</span>
                    </div>
                    <p class="ranking-rationale">{{ item.rationale }}</p>
                    <div class="signal-columns">
                      <div>
                        <span>优势</span>
                        <ul>
                          <li v-for="strength in item.strengths" :key="strength">{{ strength }}</li>
                        </ul>
                      </div>
                      <div>
                        <span>风险</span>
                        <ul>
                          <li v-for="risk in item.risks" :key="risk">{{ risk }}</li>
                        </ul>
                      </div>
                    </div>
                    <details v-if="item.newsHighlights?.length" class="news-preview">
                      <summary>新闻依据 {{ item.newsHighlights.length }} 条</summary>
                      <ul>
                        <li v-for="news in item.newsHighlights" :key="news.url || news.title">
                          <strong>{{ news.title || '未命名新闻' }}</strong>
                          <span>{{ news.source || news.provider || 'unknown' }}</span>
                        </li>
                      </ul>
                    </details>
                  </article>
                </div>
                <div v-else class="empty-note result-empty">暂无 ETF 选择结果</div>
              </a-spin>

              <div v-if="selectionResult?.warnings?.length" class="warning-list">
                <span>提示</span>
              <p v-for="warning in selectionResult.warnings" :key="warning">{{ warning }}</p>
            </div>
          </div>
        </div>

        <div class="closed-loop-card">
          <div class="closed-loop-head">
            <div>
              <h3>最小闭环测试</h3>
              <p>AI 选第一名 ETF，调用回测平台 HTTP API，再交给 AI 解读回测结果。</p>
            </div>
            <span v-if="loopSelectedSymbol" class="loop-symbol">{{ loopSelectedSymbol }}</span>
          </div>

          <div class="closed-loop-controls">
            <label class="field">
              <span>策略</span>
              <a-select v-model:value="loopForm.strategyType">
                <a-select-option value="ETF_TREND">趋势策略</a-select-option>
                <a-select-option value="ETF_DCA">定投策略</a-select-option>
              </a-select>
            </label>
            <label class="field">
              <span>开始日期</span>
              <a-input v-model:value="loopForm.start" type="date" />
            </label>
            <label class="field">
              <span>结束日期</span>
              <a-input v-model:value="loopForm.end" type="date" />
            </label>
            <label class="field">
              <span>初始资金</span>
              <a-input-number v-model:value="loopForm.initialCapital" :min="1000" :step="10000" />
            </label>
            <label class="field">
              <span>仓位比例</span>
              <a-input-number v-model:value="loopForm.positionRatio" :min="0.1" :max="1" :step="0.05" />
            </label>
          </div>

          <a-spin :spinning="loopRunning">
            <div v-if="loopBacktestResult" class="loop-result-grid">
              <div class="loop-metric">
                <span>最终权益</span>
                <strong>{{ money(loopBacktestResult.summary.finalEquity) }}</strong>
              </div>
              <div class="loop-metric">
                <span>收益率</span>
                <strong :class="classBySign(loopBacktestResult.summary.totalReturnRate)">
                  {{ pct(loopBacktestResult.summary.totalReturnRate) }}
                </strong>
              </div>
              <div class="loop-metric">
                <span>最大回撤</span>
                <strong class="value-down">{{ pct(loopBacktestResult.summary.maxDrawdownRate) }}</strong>
              </div>
              <div class="loop-metric">
                <span>胜率</span>
                <strong>{{ pct(loopBacktestResult.summary.winRate) }}</strong>
              </div>
            </div>

            <div v-if="loopAnalysisResult" class="loop-analysis">
              <span class="risk-pill" :class="loopAnalysisResult.riskLevel.toLowerCase()">
                {{ loopAnalysisResult.riskLevel }}
              </span>
              <p>{{ loopAnalysisResult.summary }}</p>
              <ul>
                <li v-for="finding in loopAnalysisResult.keyFindings" :key="finding">{{ finding }}</li>
              </ul>
            </div>
            <div v-else class="empty-note loop-empty">尚未运行闭环测试</div>
          </a-spin>
        </div>
      </section>
      </a-collapse-panel>

      <a-collapse-panel key="chat">
        <template #header>
          <div class="fold-header">
            <h3>AI 对话</h3>
            <p>Ant Design X Vue 对话区</p>
          </div>
        </template>
        <section class="x-chat-card glass-card">
          <div class="x-conversation-pane">
            <div class="x-pane-head">
              <div>
                <h3>AI 对话</h3>
                <p>Ant Design X Vue</p>
              </div>
              <a-button size="small" @click="newConversation">新会话</a-button>
            </div>
            <Conversations
              :items="conversationItems"
              :active-key="activeConversationId"
              :on-active-change="switchConversation"
              :groupable="true"
              class="x-conversations"
            />
          </div>

          <div class="x-chat-pane">
            <div class="x-chat-head">
              <div>
                <h3>{{ activeConversation?.title || 'ETF AI 研究助手' }}</h3>
                <p>{{ health?.providerName || 'DeepSeek' }} · {{ health?.model || 'deepseek-chat' }}</p>
              </div>
              <a-switch v-model:checked="chatRagEnabled" size="small" />
            </div>

            <div class="x-bubble-list">
              <Bubble
                v-for="message in activeMessages"
                :key="message.id"
                :content="message.content"
                :placement="message.role === 'user' ? 'end' : 'start'"
                :loading="message.loading"
                :typing="message.role === 'assistant' && !message.loading ? { step: 3, interval: 20 } : false"
                :avatar="message.role === 'assistant' ? assistantAvatar : userAvatar"
                :variant="message.role === 'assistant' ? 'filled' : 'outlined'"
                shape="corner"
              />
            </div>

            <Sender
              v-model:value="chatInput"
              :loading="chatSending"
              placeholder="问问 ETF 新闻源、回测风险、候选过滤逻辑..."
              :auto-size="{ minRows: 2, maxRows: 5 }"
              @submit="sendChat"
              @cancel="chatSending = false"
            />
          </div>
        </section>
      </a-collapse-panel>

      <a-collapse-panel key="analysis">
        <template #header>
          <div class="fold-header">
            <h3>通用 AI 分析</h3>
            <p>回测解读、风险过滤和 Prompt 预览</p>
          </div>
        </template>
        <section class="ai-lab-grid">
          <form class="ai-form-card glass-card" @submit.prevent="runAnalysis">
        <div class="section-head">
          <div>
            <h3>分析输入</h3>
            <p>先手动输入内容，后续再通过 HTTP 和回测系统集成。</p>
          </div>
          <a-button :loading="healthLoading" @click="loadHealth">检查服务</a-button>
        </div>

        <div class="form-grid">
          <label class="field full">
            <span>分析类型</span>
            <a-select v-model:value="form.analysisType">
              <a-select-option value="BACKTEST_REVIEW">回测结果解读</a-select-option>
              <a-select-option value="TRADE_JOURNAL_REVIEW">交易日志复盘</a-select-option>
              <a-select-option value="ETF_RISK_FILTER">ETF 风险过滤</a-select-option>
              <a-select-option value="DAILY_BRIEFING">每日辅助报告</a-select-option>
            </a-select>
          </label>

          <label class="field">
            <span>ETF 代码</span>
            <a-input v-model:value="form.symbolCode" placeholder="例如 510300" />
          </label>

          <label class="field">
            <span>ETF 名称</span>
            <a-input v-model:value="form.symbolName" placeholder="例如 沪深300ETF" />
          </label>

          <label class="field full">
            <span>标题</span>
            <a-input v-model:value="form.title" placeholder="例如 沪深300ETF 一年回测复盘" />
          </label>

          <label class="field full">
            <span>分析内容</span>
            <a-textarea
              v-model:value="form.content"
              :rows="7"
              placeholder="粘贴回测摘要、交易日志、风险问题或每日观察内容。"
            />
          </label>

          <label class="field full">
            <span>结构化指标 JSON</span>
            <a-textarea
              v-model:value="metricsText"
              :rows="5"
              placeholder='例如 {"totalReturn":101.3,"maxDrawdown":-15.97,"winRate":56.25}'
            />
          </label>

          <label class="field full">
            <span>标签</span>
            <a-input v-model:value="tagsText" placeholder="用逗号分隔，例如 ETF, BACKTEST, TREND" />
          </label>
        </div>

        <div class="form-actions">
          <a-button @click="fillDemo">填入示例</a-button>
          <a-button type="primary" html-type="submit" :loading="analyzing">生成分析</a-button>
        </div>
          </form>

          <aside class="ai-result-stack">
            <div class="glass-card service-card">
              <div class="section-head compact">
                <div>
                  <h3>服务状态</h3>
                  <p>来自独立 Spring Boot 服务。</p>
                </div>
              </div>
              <div v-if="health" class="service-metrics">
                <div>
                  <span>Service</span>
                  <strong>{{ health.service }}</strong>
                </div>
                <div>
                  <span>Provider</span>
                  <strong>{{ health.providerName }}</strong>
                </div>
                <div>
                  <span>Model</span>
                  <strong>{{ health.model }}</strong>
                </div>
                <div>
                  <span>Enabled</span>
                  <strong>{{ health.providerEnabled ? 'Yes' : 'No' }}</strong>
                </div>
              </div>
              <div v-else class="empty-note">点击“检查服务”查看 etf-ai-analysis 是否启动。</div>
            </div>

            <div class="glass-card result-card">
              <div class="section-head compact">
                <div>
                  <h3>分析结果</h3>
                  <p>当前为独立服务返回结果，不会保存到回测系统。</p>
                </div>
                <span v-if="result" class="risk-pill" :class="result.riskLevel.toLowerCase()">{{ result.riskLevel }}</span>
              </div>

              <a-spin :spinning="analyzing">
                <div v-if="result" class="result-content">
                  <div class="summary-box">
                    <span>核心摘要</span>
                    <p>{{ result.summary }}</p>
                  </div>

                  <div class="result-section">
                    <h4>关键发现</h4>
                    <ul>
                      <li v-for="item in result.keyFindings" :key="item">{{ item }}</li>
                    </ul>
                  </div>

                  <div class="result-section">
                    <h4>后续建议</h4>
                    <ul>
                      <li v-for="item in result.suggestions" :key="item">{{ item }}</li>
                    </ul>
                  </div>

                  <details class="prompt-preview">
                    <summary>查看 Prompt 预览</summary>
                    <pre>{{ result.promptPreview }}</pre>
                  </details>
                </div>
                <div v-else class="empty-note result-empty">暂无分析结果</div>
              </a-spin>
            </div>
          </aside>
        </section>
      </a-collapse-panel>
    </a-collapse>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { Bubble, Conversations, Sender } from 'ant-design-x-vue'
import { RobotOutlined, UserOutlined } from '@ant-design/icons-vue'
import type { BacktestResultDTO, SymbolDTO, TechnicalSignalDTO } from '@/types'
import type {
  AnalysisType,
  EtfCandidateDTO,
  EtfAiAnalysisResponse,
  EtfAiHealthDTO,
  EtfSelectionResponse
} from '@/api/etfAiApi'
import { etfAiApi } from '@/api/etfAiApi'
import { backtestApi } from '@/api/backtestApi'
import { signalApi } from '@/api/signalApi'
import { symbolApi } from '@/api/symbolApi'
import { feedback } from '@/utils/feedback'
import { classBySign, formatMoney, formatPercent } from '@/utils/formatters'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
}

interface ChatConversation {
  id: string
  title: string
  group: string
  messages: ChatMessage[]
}

const isoDate = (date: Date) => date.toISOString().slice(0, 10)
const defaultStartDate = () => {
  const date = new Date()
  date.setFullYear(date.getFullYear() - 1)
  return isoDate(date)
}

const health = ref<EtfAiHealthDTO | null>(null)
const result = ref<EtfAiAnalysisResponse | null>(null)
const selectionResult = ref<EtfSelectionResponse | null>(null)
const loopBacktestResult = ref<BacktestResultDTO | null>(null)
const loopAnalysisResult = ref<EtfAiAnalysisResponse | null>(null)
const healthLoading = ref(false)
const analyzing = ref(false)
const selectingEtfs = ref(false)
const loopRunning = ref(false)
const candidatePoolLoading = ref(false)
const chatSending = ref(false)
const chatInput = ref('')
const chatRagEnabled = ref(false)
const activeAiPanels = ref<string[]>(['selection'])
const activeConversationId = ref('default')
const loopSelectedSymbol = ref('')
const candidatePoolSource = ref('示例')
const candidatePoolStats = reactive({
  total: 0,
  withSignals: 0
})
const money = formatMoney
const pct = formatPercent
const conversations = ref<ChatConversation[]>([
  {
    id: 'default',
    title: 'ETF 研究助手',
    group: 'Today',
    messages: [
      {
        id: 'welcome',
        role: 'assistant',
        content: '可以问我 ETF 新闻源选择、回测解读、风险复盘和 Prompt 版本管理。当前只使用 etf-ai-analysis 显式收到的信息。'
      }
    ]
  }
])
const metricsText = ref('{\n  "totalReturn": 101.3,\n  "maxDrawdown": -15.97,\n  "winRate": 56.25,\n  "tradeCount": 16\n}')
const tagsText = ref('ETF, BACKTEST, TREND')
const selectionCandidatesText = ref('')

const form = reactive({
  analysisType: 'BACKTEST_REVIEW' as AnalysisType,
  symbolCode: '510300',
  symbolName: '沪深300ETF',
  title: '沪深300ETF 一年回测复盘',
  content: '最终权益 20130.24，总收益率 101.30%，年化收益率 101.30%，最大回撤 -15.97%，胜率 56.25%。'
})

const selectionForm = reactive({
  market: 'cn',
  riskPreference: 'balanced',
  maxResults: 3,
  fetchNews: true
})

const loopForm = reactive({
  strategyType: 'ETF_TREND',
  start: defaultStartDate(),
  end: isoDate(new Date()),
  initialCapital: 100000,
  positionRatio: 0.95
})

const assistantAvatar = {
  icon: h(RobotOutlined),
  style: { backgroundColor: '#eef6fb', color: '#1c5fb9' }
}

const userAvatar = {
  icon: h(UserOutlined),
  style: { backgroundColor: '#e8f7ef', color: '#15803d' }
}

const activeConversation = computed(() =>
  conversations.value.find((item) => item.id === activeConversationId.value) || conversations.value[0]
)

const activeMessages = computed(() => activeConversation.value?.messages || [])

const conversationItems = computed(() =>
  conversations.value.map((item) => ({
    key: item.id,
    label: item.title,
    group: item.group
  }))
)

const loadHealth = async () => {
  healthLoading.value = true
  try {
    health.value = await etfAiApi.health()
  } catch (error) {
    health.value = null
    feedback.error(error instanceof Error ? error.message : 'ETF AI 服务不可用，请确认 8090 服务已启动')
  } finally {
    healthLoading.value = false
  }
}

const parseMetrics = () => {
  const text = metricsText.value.trim()
  if (!text) {
    return undefined
  }
  try {
    return JSON.parse(text) as Record<string, unknown>
  } catch {
    throw new Error('结构化指标 JSON 格式不正确')
  }
}

const parseTags = () =>
  tagsText.value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

const parseSelectionCandidates = () => {
  const text = selectionCandidatesText.value.trim()
  if (!text) {
    throw new Error('请填写候选 ETF JSON')
  }
  const parsed = JSON.parse(text) as unknown
  if (!Array.isArray(parsed)) {
    throw new Error('候选 ETF JSON 必须是数组')
  }
  return parsed.map((item) => {
    const candidate = item as Record<string, unknown>
    if (!String(candidate.symbolCode || '').trim()) {
      throw new Error('每个候选 ETF 都必须包含 symbolCode')
    }
    return candidate as unknown as EtfCandidateDTO
  })
}

const loadMarketCandidates = async () => {
  candidatePoolLoading.value = true
  try {
    const [symbols, latestSignals] = await Promise.all([
      symbolApi.listEnabled(),
      signalApi.latest().catch(() => [] as TechnicalSignalDTO[])
    ])
    const signalMap = new Map(latestSignals.map((item) => [item.symbolCode, item]))
    const etfSymbols = symbols.filter(isEtfSymbol)
    if (!etfSymbols.length) {
      feedback.warning('回测平台暂无启用 ETF 标的，请先在平台中维护 ETF 列表')
      return
    }
    const candidates = etfSymbols.map((symbol) => toMarketCandidate(symbol, signalMap.get(symbol.symbolCode)))
    selectionCandidatesText.value = JSON.stringify(candidates, null, 2)
    candidatePoolSource.value = '回测平台'
    candidatePoolStats.total = candidates.length
    candidatePoolStats.withSignals = candidates.filter((item) => item.metrics && Object.keys(item.metrics).length > 0).length
    selectionForm.fetchNews = false
    feedback.success(`已加载 ${candidates.length} 只 ETF 候选`)
  } catch (error) {
    feedback.error(error instanceof Error ? error.message : '加载全市场 ETF 候选失败')
  } finally {
    candidatePoolLoading.value = false
  }
}

const isEtfSymbol = (symbol: SymbolDTO) => {
  const assetType = String(symbol.assetType || '').toUpperCase()
  const code = String(symbol.symbolCode || '').trim()
  const name = String(symbol.symbolName || '').toUpperCase()
  if (assetType.includes('ETF') || name.includes('ETF')) {
    return true
  }
  return /^(51|52|56|58|15|16|18)\d{4}$/.test(code)
}

const toMarketCandidate = (symbol: SymbolDTO, signal?: TechnicalSignalDTO): EtfCandidateDTO => {
  const themes = inferThemes(symbol.symbolName)
  return {
    symbolCode: symbol.symbolCode,
    symbolName: symbol.symbolName,
    market: normalizeMarket(symbol.market),
    trackingIndex: inferTrackingIndex(symbol.symbolName),
    themes,
    tags: [
      symbol.assetType || 'ETF',
      ...themes,
      signal?.signalType,
      signal?.signalLevel,
      signal?.trendState
    ].filter(Boolean) as string[],
    metrics: signal
      ? {
          technicalScore: signal.technicalScore,
          closePrice: signal.closePrice,
          ma5: signal.ma5,
          ma10: signal.ma10,
          ma20: signal.ma20,
          ma60: signal.ma60,
          rsi14: signal.rsi14,
          volumeRatio: signal.volumeRatio,
          signalType: signal.signalType,
          signalLevel: signal.signalLevel,
          trendState: signal.trendState,
          aiSentimentScore: signal.aiSentimentScore,
          aiRiskScore: signal.aiRiskScore,
          aiRiskLevel: signal.aiRiskLevel
        }
      : {}
  }
}

const normalizeMarket = (market?: string) => {
  const value = String(market || '').toLowerCase()
  if (value.includes('hk')) return 'hk'
  if (value.includes('us')) return 'us'
  return 'cn'
}

const inferThemes = (name?: string) => {
  const text = String(name || '')
  const themes: string[] = []
  if (/沪深|中证|上证|创业板|科创|A50|红利|宽基/.test(text)) themes.push('宽基')
  if (/证券|银行|保险|金融|地产|煤炭|钢铁|有色|电力|能源|军工|医药|医疗|消费|酒|食品|农业|物流|传媒/.test(text)) themes.push('行业')
  if (/芯片|半导体|人工智能|AI|机器人|新能源|光伏|电池|汽车|云计算|数据|数字|创新|低碳/.test(text)) themes.push('主题')
  if (/纳指|标普|恒生|港股|日经|德国|法国|海外|QDII|全球/.test(text)) themes.push('跨境')
  if (/债|货币|现金|国开|政金|利率/.test(text)) themes.push('低波动')
  return themes.length ? themes : ['ETF']
}

const inferTrackingIndex = (name?: string) => {
  const text = String(name || '')
  const cleaned = text.replace(/ETF|联接|基金|指数|增强|发起式|交易型开放式/g, '').trim()
  return cleaned || text
}

const runEtfSelection = async () => {
  selectingEtfs.value = true
  try {
    selectionResult.value = await etfAiApi.selectEtfs({
      market: selectionForm.market,
      riskPreference: selectionForm.riskPreference,
      maxResults: selectionForm.maxResults,
      fetchNews: selectionForm.fetchNews,
      selectionGoal: '寻找适合后续回测验证的 ETF 候选',
      candidates: parseSelectionCandidates()
    })
    feedback.success(selectionResult.value.aiUsed ? 'AI ETF 选择完成' : '已使用本地评分完成 ETF 初筛')
  } catch (error) {
    feedback.error(error instanceof Error ? error.message : 'AI ETF 选择失败')
  } finally {
    selectingEtfs.value = false
  }
}

const runClosedLoop = async () => {
  if (loopRunning.value) {
    return
  }
  loopRunning.value = true
  loopBacktestResult.value = null
  loopAnalysisResult.value = null

  try {
    if (!selectionResult.value?.rankings?.length) {
      await runEtfSelection()
    }
    const selected = selectionResult.value?.rankings?.[0]
    if (!selected) {
      feedback.warning('请先提供候选 ETF，或点击“候选示例”后再运行闭环')
      return
    }

    loopSelectedSymbol.value = selected.symbolCode
    const backtestResult = await backtestApi.run({
      symbolCode: selected.symbolCode,
      start: loopForm.start,
      end: loopForm.end,
      strategyType: loopForm.strategyType,
      initialCapital: loopForm.initialCapital,
      positionRatio: loopForm.positionRatio
    })
    loopBacktestResult.value = backtestResult

    loopAnalysisResult.value = await etfAiApi.analyze({
      analysisType: 'BACKTEST_REVIEW',
      symbolCode: selected.symbolCode,
      symbolName: selected.symbolName,
      title: `${selected.symbolName || selected.symbolCode} AI 选取后回测解读`,
      content: buildBacktestAnalysisContent(selected, backtestResult),
      metrics: {
        finalEquity: backtestResult.summary.finalEquity,
        totalReturn: backtestResult.summary.totalReturnRate,
        annualizedReturn: backtestResult.summary.annualizedReturnRate,
        maxDrawdown: backtestResult.summary.maxDrawdownRate,
        winRate: backtestResult.summary.winRate,
        tradeCount: backtestResult.summary.tradeCount,
        benchmarkReturn: backtestResult.benchmark?.totalReturnRate,
        benchmarkMaxDrawdown: backtestResult.benchmark?.maxDrawdownRate
      },
      tags: ['ETF_SELECTION_LOOP', loopForm.strategyType, selected.recommendation]
    })
    feedback.success('AI 选取、回测、AI 解读闭环完成')
  } catch (error) {
    feedback.error(error instanceof Error ? error.message : '闭环测试失败')
  } finally {
    loopRunning.value = false
  }
}

const buildBacktestAnalysisContent = (
  selected: NonNullable<EtfSelectionResponse['rankings']>[number],
  backtestResult: BacktestResultDTO
) => {
  const summary = backtestResult.summary
  return [
    `AI 选取标的：${selected.symbolCode} ${selected.symbolName || ''}`,
    `AI 选取理由：${selected.rationale}`,
    `AI 分层：${selected.recommendation}，AI/本地融合评分：${selected.score}`,
    `回测策略：${backtestResult.strategyType || loopForm.strategyType}`,
    `回测区间：${backtestResult.start} 至 ${backtestResult.end}`,
    `最终权益：${summary.finalEquity}`,
    `总收益率：${summary.totalReturnRate}%`,
    `年化收益率：${summary.annualizedReturnRate}%`,
    `最大回撤：${summary.maxDrawdownRate}%`,
    `胜率：${summary.winRate}%`,
    `交易次数：${summary.tradeCount}`,
    `当前持仓状态：${backtestResult.position.status}`,
    `数据质量：${backtestResult.dataQuality?.passed ? '通过' : '存在提醒'}`,
    `数据质量提醒：${backtestResult.dataQuality?.warnings?.join('；') || '无'}`
  ].join('\n')
}

const runAnalysis = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    feedback.warning('请填写标题和分析内容')
    return
  }

  analyzing.value = true
  try {
    result.value = await etfAiApi.analyze({
      analysisType: form.analysisType,
      symbolCode: form.symbolCode.trim() || undefined,
      symbolName: form.symbolName.trim() || undefined,
      title: form.title.trim(),
      content: form.content.trim(),
      metrics: parseMetrics(),
      tags: parseTags()
    })
    feedback.success('分析完成')
  } catch (error) {
    feedback.error(error instanceof Error ? error.message : 'ETF AI 分析失败')
  } finally {
    analyzing.value = false
  }
}

const switchConversation = (id: string) => {
  activeConversationId.value = id
}

const newConversation = () => {
  const id = `conv-${Date.now()}`
  conversations.value.unshift({
    id,
    title: '新的 ETF 对话',
    group: 'Today',
    messages: [
      {
        id: `${id}-welcome`,
        role: 'assistant',
        content: '新的对话已创建。可以直接输入 ETF 代码、名称、回测摘要或新闻源问题。'
      }
    ]
  })
  activeConversationId.value = id
}

const sendChat = async (message?: string) => {
  const content = (message || chatInput.value).trim()
  if (!content || chatSending.value) {
    return
  }
  const current = activeConversation.value
  if (!current) {
    return
  }
  chatInput.value = ''
  const userMessage: ChatMessage = {
    id: `user-${Date.now()}`,
    role: 'user',
    content
  }
  const assistantMessage: ChatMessage = {
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    loading: true
  }
  current.messages.push(userMessage, assistantMessage)
  if (current.title === '新的 ETF 对话') {
    current.title = content.slice(0, 18)
  }
  chatSending.value = true
  try {
    const response = await etfAiApi.chat({
      conversationId: current.id,
      message: content,
      ragEnabled: chatRagEnabled.value,
      history: current.messages
        .filter((item) => item.id !== assistantMessage.id && !item.loading)
        .slice(-8)
        .map((item) => ({ role: item.role, content: item.content }))
    })
    assistantMessage.content = response.content || '模型没有返回内容。'
    assistantMessage.loading = false
    if (response.fallbackUsed) {
      feedback.warning('AI Provider 未启用或未配置 Key，已返回本地占位回复')
    }
  } catch (error) {
    assistantMessage.content = error instanceof Error ? error.message : 'AI 对话失败'
    assistantMessage.loading = false
  } finally {
    chatSending.value = false
  }
}

const fillDemo = () => {
  form.analysisType = 'BACKTEST_REVIEW'
  form.symbolCode = '510300'
  form.symbolName = '沪深300ETF'
  form.title = '沪深300ETF 一年回测复盘'
  form.content = '最终权益 20130.24，总收益率 101.30%，年化收益率 101.30%，最大回撤 -15.97%，胜率 56.25%。'
  metricsText.value = '{\n  "totalReturn": 101.3,\n  "maxDrawdown": -15.97,\n  "winRate": 56.25,\n  "tradeCount": 16\n}'
  tagsText.value = 'ETF, BACKTEST, TREND'
}

const fillSelectionDemo = () => {
  const demoCandidates = [
      {
        symbolCode: '510300',
        symbolName: '沪深300ETF',
        market: 'cn',
        trackingIndex: '沪深300',
        themes: ['宽基', '核心资产'],
        tags: ['宽基', '低费率'],
        metrics: {
          totalReturn: 28.5,
          maxDrawdown: -5.2,
          sharpe: 1.35,
          winRate: 61,
          technicalScore: 78,
          liquidityScore: 85,
          trackingError: 1.1
        }
      },
      {
        symbolCode: '159915',
        symbolName: '创业板ETF',
        market: 'cn',
        trackingIndex: '创业板指',
        themes: ['成长', '科技'],
        tags: ['高弹性'],
        metrics: {
          totalReturn: 18.2,
          maxDrawdown: -18.6,
          sharpe: 0.72,
          winRate: 53,
          technicalScore: 66,
          liquidityScore: 72,
          trackingError: 1.8
        }
      },
      {
        symbolCode: '512760',
        symbolName: '芯片ETF',
        market: 'cn',
        trackingIndex: '中华半导体芯片指数',
        themes: ['半导体', '国产替代'],
        tags: ['主题', '高波动'],
        metrics: {
          totalReturn: -6.4,
          maxDrawdown: -28.5,
          sharpe: 0.18,
          winRate: 45,
          technicalScore: 38,
          liquidityScore: 58,
          trackingError: 3.2
        }
      }
    ]
  selectionCandidatesText.value = JSON.stringify(demoCandidates, null, 2)
  candidatePoolSource.value = '示例'
  candidatePoolStats.total = demoCandidates.length
  candidatePoolStats.withSignals = demoCandidates.length
}

onMounted(() => {
  loadHealth()
  loadMarketCandidates()
})
</script>

<style scoped>
.ai-lab-page {
  display: grid;
  gap: 14px;
}

.page-heading,
.glass-card {
  border: 1px solid rgba(148, 173, 198, 0.34);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 14px 36px rgba(31, 76, 112, 0.08);
  backdrop-filter: blur(14px);
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.eyebrow,
.page-heading p,
.section-head p,
.field span,
.service-metrics span,
.empty-note {
  margin: 0;
  color: #668197;
  font-size: 12px;
  font-weight: 700;
}

.page-heading h2 {
  margin: 2px 0 6px;
  color: #123044;
  font-size: 22px;
  font-weight: 900;
}

.eyebrow {
  color: #2f80ed;
  text-transform: uppercase;
}

.status-chip,
.risk-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;
  white-space: nowrap;
}

.status-chip {
  color: #be123c;
  background: rgba(244, 63, 94, 0.08);
  border: 1px solid rgba(244, 63, 94, 0.18);
}

.status-chip.online {
  color: #15803d;
  background: rgba(22, 163, 74, 0.08);
  border-color: rgba(22, 163, 74, 0.18);
}

.status-chip span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
}

.ai-fold-panels {
  display: grid;
  gap: 12px;
  background: transparent;
}

.ai-fold-panels :deep(.ant-collapse-item) {
  border: 0;
  background: transparent;
}

.ai-fold-panels :deep(.ant-collapse-header) {
  align-items: center !important;
  min-height: 58px;
  padding: 12px 16px !important;
  border: 1px solid rgba(148, 173, 198, 0.34);
  border-radius: 16px !important;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 12px 30px rgba(31, 76, 112, 0.07);
}

.ai-fold-panels :deep(.ant-collapse-content) {
  border-top: 0;
  background: transparent;
}

.ai-fold-panels :deep(.ant-collapse-content-box) {
  padding: 10px 0 0 !important;
}

.fold-header {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.fold-header h3 {
  margin: 0;
  color: #123044;
  font-size: 15px;
  font-weight: 900;
}

.fold-header p {
  margin: 0;
  color: #668197;
  font-size: 12px;
  font-weight: 700;
}

.ai-lab-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: 14px;
  align-items: start;
}

.x-chat-card {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 0;
  overflow: hidden;
}

.x-conversation-pane {
  padding: 14px;
  border-right: 1px solid rgba(148, 173, 198, 0.22);
  background: rgba(238, 246, 251, 0.42);
}

.x-pane-head,
.x-chat-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.x-pane-head h3,
.x-chat-head h3 {
  margin: 0 0 3px;
  color: #123044;
  font-size: 15px;
  font-weight: 900;
}

.x-pane-head p,
.x-chat-head p {
  margin: 0;
  color: #668197;
  font-size: 12px;
  font-weight: 750;
}

.x-conversations {
  background: transparent;
}

.x-chat-pane {
  display: grid;
  grid-template-rows: auto minmax(320px, 52vh) auto;
  gap: 12px;
  padding: 14px;
  min-width: 0;
}

.x-bubble-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  padding: 2px 4px 8px;
}

.x-bubble-list :deep(.ant-bubble-content) {
  line-height: 1.65;
}

.x-chat-pane :deep(.ant-sender) {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
}

.ai-form-card,
.service-card,
.result-card {
  padding: 18px;
}

.ai-result-stack {
  display: grid;
  gap: 14px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.section-head.compact {
  margin-bottom: 12px;
}

.section-head h3 {
  margin: 0 0 4px;
  color: #123044;
  font-size: 16px;
  font-weight: 900;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.field.full {
  grid-column: 1 / -1;
}

.field :deep(.ant-input),
.field :deep(.ant-input-affix-wrapper),
.field :deep(.ant-select-selector),
.field :deep(.ant-input-textarea) {
  border-radius: 11px !important;
  background: rgba(255, 255, 255, 0.86) !important;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.service-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.service-metrics div {
  min-width: 0;
  padding: 12px;
  border-radius: 12px;
  background: rgba(238, 246, 251, 0.72);
  border: 1px solid rgba(148, 173, 198, 0.22);
}

.service-metrics strong {
  display: block;
  margin-top: 6px;
  color: #123044;
  font-size: 13px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.risk-pill.low {
  color: #15803d;
  background: rgba(22, 163, 74, 0.08);
  border: 1px solid rgba(22, 163, 74, 0.2);
}

.risk-pill.medium {
  color: #b45309;
  background: rgba(217, 119, 6, 0.08);
  border: 1px solid rgba(217, 119, 6, 0.2);
}

.risk-pill.high {
  color: #be123c;
  background: rgba(244, 63, 94, 0.08);
  border: 1px solid rgba(244, 63, 94, 0.2);
}

.risk-pill.unknown {
  color: #64748b;
  background: rgba(100, 116, 139, 0.08);
  border: 1px solid rgba(100, 116, 139, 0.18);
}

.result-content {
  display: grid;
  gap: 14px;
}

.summary-box {
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(47, 128, 237, 0.08), rgba(20, 184, 166, 0.06));
  border: 1px solid rgba(47, 128, 237, 0.16);
}

.summary-box span,
.result-section h4 {
  color: #1c5fb9;
  font-size: 12px;
  font-weight: 900;
}

.summary-box p {
  margin: 8px 0 0;
  color: #123044;
  line-height: 1.7;
}

.result-section ul {
  display: grid;
  gap: 8px;
  margin: 8px 0 0;
  padding-left: 18px;
  color: #29495e;
  line-height: 1.6;
}

.prompt-preview {
  border-top: 1px solid rgba(148, 173, 198, 0.24);
  padding-top: 12px;
}

.prompt-preview summary {
  cursor: pointer;
  color: #1c5fb9;
  font-weight: 850;
}

.prompt-preview pre {
  max-height: 280px;
  overflow: auto;
  margin: 12px 0 0;
  padding: 12px;
  color: #123044;
  background: rgba(238, 246, 251, 0.82);
  border: 1px solid rgba(148, 173, 198, 0.24);
  border-radius: 12px;
  white-space: pre-wrap;
}

.result-empty {
  min-height: 180px;
  display: grid;
  place-items: center;
}

.selection-card {
  padding: 18px;
}

.selection-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.selection-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(380px, 1.05fr);
  gap: 14px;
  align-items: start;
}

.selection-inputs {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.selection-controls {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) minmax(120px, 1fr) 110px 92px;
  gap: 10px;
  align-items: end;
}

.compact-field :deep(.ant-input-number) {
  width: 100%;
  border-radius: 11px;
}

.switch-field {
  align-content: end;
}

.selection-results {
  min-width: 0;
}

.ranking-list {
  display: grid;
  gap: 12px;
}

.ranking-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px 12px;
  padding: 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(148, 173, 198, 0.24);
}

.ranking-item.preferred {
  border-color: rgba(22, 163, 74, 0.24);
}

.ranking-item.avoid {
  border-color: rgba(244, 63, 94, 0.24);
}

.ranking-main {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.rank-badge {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #1c5fb9;
  font-size: 12px;
  font-weight: 900;
  background: rgba(47, 128, 237, 0.1);
}

.ranking-main h4 {
  margin: 0 0 4px;
  color: #123044;
  font-size: 15px;
  font-weight: 900;
}

.ranking-main p,
.ranking-rationale,
.news-preview li span,
.warning-list p {
  margin: 0;
  color: #668197;
  font-size: 12px;
  line-height: 1.55;
}

.score-box {
  min-width: 70px;
  text-align: right;
}

.score-box strong {
  display: block;
  color: #123044;
  font-size: 22px;
  font-weight: 950;
  line-height: 1;
}

.score-box span,
.signal-columns span,
.warning-list span {
  color: #1c5fb9;
  font-size: 11px;
  font-weight: 900;
}

.ranking-rationale,
.signal-columns,
.news-preview {
  grid-column: 1 / -1;
}

.signal-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.signal-columns ul,
.news-preview ul {
  display: grid;
  gap: 6px;
  margin: 6px 0 0;
  padding-left: 16px;
  color: #29495e;
  font-size: 12px;
  line-height: 1.55;
}

.news-preview {
  padding-top: 8px;
  border-top: 1px solid rgba(148, 173, 198, 0.2);
}

.news-preview summary {
  cursor: pointer;
  color: #1c5fb9;
  font-size: 12px;
  font-weight: 850;
}

.news-preview li {
  display: grid;
  gap: 2px;
}

.warning-list {
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(217, 119, 6, 0.07);
  border: 1px solid rgba(217, 119, 6, 0.16);
}

.warning-list p {
  margin-top: 6px;
}

.closed-loop-card {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(148, 173, 198, 0.24);
}

.closed-loop-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.closed-loop-head h3 {
  margin: 0 0 4px;
  color: #123044;
  font-size: 15px;
  font-weight: 900;
}

.closed-loop-head p {
  margin: 0;
  color: #668197;
  font-size: 12px;
  font-weight: 700;
}

.loop-symbol {
  padding: 5px 9px;
  border-radius: 999px;
  color: #1c5fb9;
  background: rgba(47, 128, 237, 0.08);
  border: 1px solid rgba(47, 128, 237, 0.16);
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.closed-loop-controls {
  display: grid;
  grid-template-columns: minmax(130px, 1fr) repeat(2, minmax(130px, 1fr)) minmax(120px, 0.8fr) minmax(120px, 0.8fr);
  gap: 10px;
  align-items: end;
  margin-bottom: 12px;
}

.closed-loop-controls :deep(.ant-input-number) {
  width: 100%;
  border-radius: 11px;
}

.loop-result-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.loop-metric {
  padding: 12px;
  border-radius: 12px;
  background: rgba(238, 246, 251, 0.72);
  border: 1px solid rgba(148, 173, 198, 0.22);
}

.loop-metric span {
  display: block;
  color: #668197;
  font-size: 12px;
  font-weight: 800;
}

.loop-metric strong {
  display: block;
  margin-top: 6px;
  color: #123044;
  font-size: 16px;
  font-weight: 950;
}

.loop-analysis {
  display: grid;
  gap: 10px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(148, 173, 198, 0.22);
}

.loop-analysis p {
  margin: 0;
  color: #123044;
  line-height: 1.65;
}

.loop-analysis ul {
  display: grid;
  gap: 6px;
  margin: 0;
  padding-left: 18px;
  color: #29495e;
  line-height: 1.55;
}

.loop-empty {
  min-height: 72px;
  display: grid;
  place-items: center;
}

@media (max-width: 1180px) {
  .ai-lab-grid,
  .selection-layout {
    grid-template-columns: 1fr;
  }

  .x-chat-card {
    grid-template-columns: 1fr;
  }

  .x-conversation-pane {
    border-right: 0;
    border-bottom: 1px solid rgba(148, 173, 198, 0.22);
  }
}

@media (max-width: 720px) {
  .page-heading,
  .section-head,
  .form-actions,
  .selection-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .form-grid,
  .service-metrics,
  .selection-controls,
  .closed-loop-controls,
  .loop-result-grid,
  .signal-columns {
    grid-template-columns: 1fr;
  }

  .ranking-item {
    grid-template-columns: 1fr;
  }

  .score-box {
    text-align: left;
  }
}
</style>
