import request from './request'
import type { TechnicalSignalDTO } from '@/types'

export const signalApi = {
  generate: (symbolCode: string) => request.post<unknown, TechnicalSignalDTO[]>('/signals/generate', null, { params: { symbolCode } }),
  generateAll: () => request.post<unknown, { count: number }>('/signals/generate-all'),
  latest: () => request.get<unknown, TechnicalSignalDTO[]>('/signals/latest'),
  history: (symbolCode: string) => request.get<unknown, TechnicalSignalDTO[]>('/signals/history', { params: { symbolCode } }),
  delete: (symbolCode: string, tradeDate: string) =>
    request.delete<unknown, { count: number }>('/signals', { params: { symbolCode, tradeDate } })
}
