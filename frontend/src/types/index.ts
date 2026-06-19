export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthSessionDTO {
  token: string
  username: string
  displayName: string
}

export interface SymbolDTO {
  id: number
  symbolCode: string
  symbolName: string
  market: string
  assetType: string
  currency: string
  enabled: number
}

export interface DailyKlineDTO {
  symbolCode: string
  tradeDate: string
  open: number
  high: number
  low: number
  close: number
  volume?: number
  amount?: number
}

export interface KLineDataItem {
  timestamp: number
  open: number
  close: number
  high: number
  low: number
  volume?: number
  turnover?: number
}

export interface IndicatorSnapshotDTO {
  symbolCode: string
  tradeDate: string
  ma5?: number
  ma10?: number
  ma20?: number
  ma60?: number
  rsi14?: number
  atr14?: number
  volumeMa20?: number
  volumeRatio?: number
  fiveDayReturn?: number
  distanceToMa20?: number
}

export interface TechnicalSignalDTO {
  symbolCode: string
  tradeDate: string
  closePrice?: number
  ma5?: number
  ma10?: number
  ma20?: number
  ma60?: number
  rsi14?: number
  atr14?: number
  volumeRatio?: number
  trendState: string
  signalType: string
  signalLevel: string
  technicalScore: number
  entryReference?: number
  stopLossReference?: number
  takeProfitReference?: number
  reasons: string[]
  riskNotes: string[]
  aiSentimentScore?: number
  aiRiskScore?: number
  aiRiskLevel?: string
  finalAction?: string
  positionPolicy?: string
}

export interface DashboardDTO {
  totalSymbols: number
  totalWatchlist: number
  buyCandidateCount: number
  watchCount: number
  avoidCount: number
  latestSignals: TechnicalSignalDTO[]
}

export interface ImportKlineResultDTO {
  symbolCode: string
  totalRows: number
  successRows: number
  failedRows: number
  message: string
}

export interface WatchlistDTO {
  id: number
  symbolCode: string
  groupName: string
  priority: number
  note?: string
  enabled: number
}

export interface SymbolTechnicalDetailDTO {
  symbol: SymbolDTO
  klines: DailyKlineDTO[]
  indicators: IndicatorSnapshotDTO[]
  latestSignal?: TechnicalSignalDTO
  aiAnalysis?: AiAnalysisSnapshotDTO
  finalDecision?: FinalTradeDecisionDTO
}

export interface AiAnalysisSnapshotDTO {
  id?: number
  symbolCode: string
  analysisDate: string
  assetName?: string
  market?: string
  assetType?: string
  sentimentScore?: number
  riskScore?: number
  riskLevel?: string
  marketState?: string
  actionConstraint?: string
  summary?: string
  positiveFactors?: string
  negativeFactors?: string
  riskFactors?: string
  contrarianView?: string
  rawReport?: string
  sourceSystem?: string
  callStatus?: string
  errorMessage?: string
}

export interface FinalTradeDecisionDTO {
  id?: number
  symbolCode: string
  decisionDate: string
  technicalSignal?: string
  technicalScore?: number
  aiSentimentScore?: number
  aiRiskScore?: number
  aiRiskLevel?: string
  finalAction?: string
  positionPolicy?: string
  decisionReason?: string
  rejectReason?: string
}

export interface AiAnalyzeResultDTO {
  aiAnalysis?: AiAnalysisSnapshotDTO
  finalDecision?: FinalTradeDecisionDTO
  cached: boolean
  message: string
}

export interface AiBatchAnalyzeResultDTO {
  tradeDate: string
  successCount: number
  failedCount: number
  skippedCount: number
  results: AiAnalyzeResultDTO[]
}

export interface BacktestRunRequest {
  symbolCode: string
  start?: string
  end?: string
  initialCapital?: number
  positionRatio?: number
  feeRate?: number
  slippageRate?: number
  lotSize?: number
  strategyType?: string
  dcaFrequency?: string
  dcaBaseAmount?: number
  dcaLowMultiplier?: number
  dcaHighMultiplier?: number
}

export interface BacktestBatchRunRequest extends Omit<BacktestRunRequest, 'symbolCode'> {
  symbolCodes: string[]
}

export interface BacktestBatchItemDTO {
  symbolCode: string
  success: boolean
  message?: string
  recordId?: number
  strategyType?: string
  start?: string
  end?: string
  finalEquity?: number
  totalReturnRate?: number
  benchmarkReturnRate?: number
  excessReturnRate?: number
  maxDrawdownRate?: number
  benchmarkMaxDrawdownRate?: number
  winRate?: number
  tradeCount?: number
}

export interface BacktestBatchResultDTO {
  totalCount: number
  successCount: number
  failedCount: number
  results: BacktestBatchItemDTO[]
}

export interface BacktestTradeDTO {
  tradeDate: string
  action: string
  price: number
  shares: number
  amount: number
  fee: number
  pnl?: number
  pnlRate?: number
  reason: string
}

export interface BacktestPositionDTO {
  symbolCode: string
  shares: number
  avgCost?: number
  marketPrice: number
  marketValue: number
  unrealizedPnl?: number
  unrealizedPnlRate?: number
  status: string
}

export interface BacktestBenchmarkDTO {
  finalEquity?: number
  totalReturnRate?: number
  annualizedReturnRate?: number
  maxDrawdownRate?: number
  shares?: number
  cash?: number
  startPrice?: number
  endPrice?: number
}

export interface EquityCurvePointDTO {
  tradeDate: string
  cash: number
  marketValue: number
  totalEquity: number
  dailyReturn: number
  drawdown: number
  positionShares: number
  closePrice: number
}

export interface BacktestSummaryDTO {
  initialCapital: number
  finalEquity: number
  totalReturnRate: number
  annualizedReturnRate: number
  maxDrawdownRate: number
  realizedPnl: number
  tradeCount: number
  winCount: number
  lossCount: number
  winRate: number
  profitLossRatio?: number
}

export interface BacktestDataQualityDTO {
  totalRows?: number
  invalidPriceRows?: number
  missingVolumeRows?: number
  missingAmountRows?: number
  longCalendarGapCount?: number
  suspiciousChangeRows?: number
  firstTradeDate?: string
  lastTradeDate?: string
  passed?: boolean
  warnings?: string[]
}

export interface BacktestResultDTO {
  recordId?: number
  symbolCode: string
  strategyType?: string
  executionMode?: string
  start: string
  end: string
  dataQuality?: BacktestDataQualityDTO
  summary: BacktestSummaryDTO
  benchmark?: BacktestBenchmarkDTO
  position: BacktestPositionDTO
  trades: BacktestTradeDTO[]
  equityCurve: EquityCurvePointDTO[]
}

export interface BacktestRecordSummaryDTO {
  id: number
  symbolCode: string
  strategyType?: string
  startDate?: string
  endDate?: string
  initialCapital?: number
  positionRatio?: number
  feeRate?: number
  slippageRate?: number
  lotSize?: number
  finalEquity?: number
  totalReturnRate?: number
  annualizedReturnRate?: number
  maxDrawdownRate?: number
  benchmarkReturnRate?: number
  benchmarkMaxDrawdownRate?: number
  excessReturnRate?: number
  winRate?: number
  tradeCount?: number
  createdAt?: string
}

export interface BacktestRecordDetailDTO {
  summary: BacktestRecordSummaryDTO
  request: BacktestRunRequest
  result: BacktestResultDTO
}

export interface TradeJournalDTO {
  id: number
  symbolCode: string
  tradeDate: string
  signalType: string
  signalLevel?: string
  trendState?: string
  closePrice?: number
  ma5?: number
  ma10?: number
  ma20?: number
  ma60?: number
  rsi14?: number
  atr14?: number
  volumeRatio?: number
  technicalScore?: number
  signalReason?: string
  riskNote?: string
  aiRiskScore?: number
  aiRiskLevel?: string
  finalAction?: string
  positionPolicy?: string
  decisionReason?: string
  rejectReason?: string
  executed: boolean
  executionDate?: string
  executionPrice?: number
  executionNote?: string
  followUpStatus?: string
  followUpDate?: string
  followUpPrice?: number
  followUpReturnRate?: number
  followUpNote?: string
}

export interface UpdateTradeJournalRequest {
  executed: boolean
  executionDate?: string
  executionPrice?: number
  executionNote?: string
  followUpStatus?: string
  followUpDate?: string
  followUpPrice?: number
  followUpNote?: string
}

export interface MarketDataSourceStatusDTO {
  sourceCode: string
  displayName: string
  providerType: string
  role: string
  priority: number
  enabled: boolean
  status: string
  healthMessage?: string
  lastCheckedAt?: string
  lastSuccessAt?: string
  lastErrorAt?: string
  lastError?: string
  successCount: number
  failureCount: number
}

export interface MarketDataSourceOverviewDTO {
  totalCount: number
  upCount: number
  downCount: number
  unknownCount: number
  refreshedAt: string
  sources: MarketDataSourceStatusDTO[]
}

export interface CacheRefreshResultDTO {
  status: string
  message: string
  refreshedAt: string
  actions: string[]
}
