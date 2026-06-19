import request from './request'
import type { SymbolDTO } from '@/types'

export interface CreateSymbolRequest {
  symbolCode: string
  symbolName?: string
  market: string
  assetType?: string
  currency?: string
}

export const symbolApi = {
  list: () => request.get<unknown, SymbolDTO[]>('/symbols'),
  listEnabled: () => request.get<unknown, SymbolDTO[]>('/symbols/enabled'),
  resolve: (symbolCode: string) => request.get<unknown, SymbolDTO>('/symbols/resolve', { params: { symbolCode } }),
  create: (data: CreateSymbolRequest) => request.post<unknown, SymbolDTO>('/symbols', data),
  enable: (symbolCode: string) => request.put<unknown, void>(`/symbols/${symbolCode}/enable`),
  disable: (symbolCode: string) => request.put<unknown, void>(`/symbols/${symbolCode}/disable`),
  remove: (symbolCode: string) => request.delete<unknown, void>(`/symbols/${symbolCode}`)
}
