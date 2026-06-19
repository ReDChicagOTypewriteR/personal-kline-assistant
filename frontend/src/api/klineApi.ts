import request from './request'
import type { DailyKlineDTO, ImportKlineResultDTO, KLineDataItem } from '@/types'

export const klineApi = {
  importDaily: (symbolCode: string, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<unknown, ImportKlineResultDTO>('/klines/daily/import', formData, {
      params: { symbolCode },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  fetchEastMoney: (symbolCode: string, stockCode?: string, start?: string, end?: string, adjustType = 1) =>
    request.post<unknown, ImportKlineResultDTO>('/klines/daily/fetch/eastmoney', null, {
      params: { symbolCode, stockCode, start, end, adjustType }
    }),
  chart: (symbolCode: string, period = 'D', start?: string, end?: string, stockCode?: string, adjustType = 1) =>
    request.get<unknown, KLineDataItem[]>('/klines/chart', {
      params: { symbolCode, period, start, end, stockCode, adjustType }
    }),
  listDaily: (symbolCode: string, start?: string, end?: string) =>
    request.get<unknown, DailyKlineDTO[]>('/klines/daily', { params: { symbolCode, start, end } }),
  latest: (symbolCode: string) => request.get<unknown, DailyKlineDTO>('/klines/daily/latest', { params: { symbolCode } })
}
