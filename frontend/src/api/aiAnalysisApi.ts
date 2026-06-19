import request from './request'
import type { AiAnalysisSnapshotDTO, AiAnalyzeResultDTO, AiBatchAnalyzeResultDTO } from '@/types'

export const aiAnalysisApi = {
  analyze: (symbolCode: string, tradeDate: string) =>
    request.post<unknown, AiAnalyzeResultDTO>('/ai-analysis/analyze', null, { params: { symbolCode, tradeDate } }),
  analyzeBuyCandidates: (tradeDate: string) =>
    request.post<unknown, AiBatchAnalyzeResultDTO>('/ai-analysis/analyze-buy-candidates', null, { params: { tradeDate } }),
  snapshot: (symbolCode: string, analysisDate: string) =>
    request.get<unknown, AiAnalysisSnapshotDTO | null>('/ai-analysis/snapshot', { params: { symbolCode, analysisDate } })
}
