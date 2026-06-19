import request from './request'
import type { TradeJournalDTO, UpdateTradeJournalRequest } from '@/types'

export const tradeJournalApi = {
  latest: (limit = 100) => request.get<unknown, TradeJournalDTO[]>('/trade-journal/latest', { params: { limit } }),
  history: (symbolCode: string) => request.get<unknown, TradeJournalDTO[]>('/trade-journal/history', { params: { symbolCode } }),
  update: (id: number, data: UpdateTradeJournalRequest) => request.put<unknown, TradeJournalDTO>(`/trade-journal/${id}`, data),
  remove: (id: number) => request.delete<unknown, void>(`/trade-journal/${id}`),
  removeBatch: (ids: number[]) => request.delete<unknown, { count: number }>('/trade-journal/batch', { data: ids })
}
