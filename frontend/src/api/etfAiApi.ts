import axios from 'axios'

export type AnalysisType = 'BACKTEST_REVIEW' | 'TRADE_JOURNAL_REVIEW' | 'ETF_RISK_FILTER' | 'DAILY_BRIEFING'
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'UNKNOWN'

export interface EtfAiHealthDTO {
  service: string
  status: string
  providerName: string
  model: string
  providerEnabled: boolean
  checkedAt: string
}

export interface EtfAiAnalysisRequest {
  analysisType: AnalysisType
  symbolCode?: string
  symbolName?: string
  title: string
  content: string
  metrics?: Record<string, unknown>
  tags?: string[]
}

export interface EtfAiAnalysisResponse {
  requestId: string
  analysisType: AnalysisType
  symbolCode?: string
  symbolName?: string
  providerName: string
  model: string
  providerEnabled: boolean
  riskLevel: RiskLevel
  summary: string
  keyFindings: string[]
  suggestions: string[]
  promptPreview: string
  generatedAt: string
}

export interface AiProviderSettingsDTO {
  enabled: boolean
  provider: string
  baseUrl: string
  model: string
  temperature?: number
  maxTokens?: number
  apiKeyConfigured: boolean
  apiKeyMasked?: string
}

export interface NewsProviderSettingsDTO {
  provider: string
  displayName: string
  enabled: boolean
  baseUrl?: string
  apiKeyConfigured: boolean
  apiKeyMasked?: string
}

export interface AiRuntimeSettingsDTO {
  aiProvider: AiProviderSettingsDTO
  newsProviders: NewsProviderSettingsDTO[]
  updatedAt: string
}

export interface AiRuntimeSettingsRequest {
  aiProvider: {
    enabled: boolean
    provider: string
    baseUrl: string
    model: string
    apiKey?: string
    temperature?: number
    maxTokens?: number
  }
  newsProviders: Array<{
    provider: string
    displayName?: string
    enabled: boolean
    baseUrl?: string
    apiKey?: string
  }>
}

export interface AiChatRequest {
  conversationId?: string
  message: string
  systemPrompt?: string
  ragEnabled?: boolean
  history?: Array<{ role: string; content: string }>
}

export interface AiChatResponse {
  requestId: string
  conversationId: string
  provider: string
  model: string
  providerEnabled: boolean
  fallbackUsed: boolean
  ragEnabled: boolean
  content: string
  sources: string[]
  generatedAt: string
}

export interface EtfNewsItemDTO {
  title?: string
  summary?: string
  source?: string
  url?: string
  publishedAt?: string
  provider?: string
  query?: string
  relevanceScore?: number
}

export interface EtfCandidateDTO {
  symbolCode: string
  symbolName?: string
  market?: string
  trackingIndex?: string
  themes?: string[]
  tags?: string[]
  metrics?: Record<string, unknown>
  newsItems?: EtfNewsItemDTO[]
}

export interface EtfSelectionRequest {
  market?: string
  selectionGoal?: string
  riskPreference?: 'conservative' | 'balanced' | 'aggressive' | string
  fetchNews?: boolean
  maxResults?: number
  newsWindowDays?: number
  maxNewsPerCandidate?: number
  candidates: EtfCandidateDTO[]
}

export interface EtfSelectionResultItem {
  rank: number
  symbolCode: string
  symbolName?: string
  trackingIndex?: string
  score: number
  recommendation: 'PREFERRED' | 'WATCH' | 'AVOID' | string
  confidence: 'HIGH' | 'MEDIUM' | 'LOW' | string
  rationale: string
  strengths: string[]
  risks: string[]
  newsHighlights: EtfNewsItemDTO[]
  searchQueries: string[]
  metrics: Record<string, unknown>
}

export interface EtfSelectionResponse {
  requestId: string
  market: string
  selectionGoal: string
  riskPreference: string
  providerName: string
  model: string
  aiProviderEnabled: boolean
  aiUsed: boolean
  fallbackUsed: boolean
  newsFetched: boolean
  rankings: EtfSelectionResultItem[]
  warnings: string[]
  guardrails: string[]
  promptPreview: string
  aiRawOutput?: string
  generatedAt: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

const client = axios.create({
  baseURL: '/etf-ai-api',
  timeout: 120000
})

async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>) {
  const response = await promise
  if (response.data && response.data.success === false) {
    throw new Error(response.data.message || 'ETF AI 服务请求失败')
  }
  return response.data.data
}

export const etfAiApi = {
  health() {
    return unwrap<EtfAiHealthDTO>(client.get('/ai/health'))
  },
  analyze(payload: EtfAiAnalysisRequest) {
    return unwrap<EtfAiAnalysisResponse>(client.post('/ai/analyze', payload))
  },
  settings() {
    return unwrap<AiRuntimeSettingsDTO>(client.get('/ai/settings'))
  },
  saveSettings(payload: AiRuntimeSettingsRequest) {
    return unwrap<AiRuntimeSettingsDTO>(client.put('/ai/settings', payload))
  },
  chat(payload: AiChatRequest) {
    return unwrap<AiChatResponse>(client.post('/ai/chat', payload))
  },
  selectEtfs(payload: EtfSelectionRequest) {
    return unwrap<EtfSelectionResponse>(client.post('/ai/etf-selection/select', payload))
  }
}
