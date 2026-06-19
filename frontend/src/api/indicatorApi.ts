import request from './request'
import type { IndicatorSnapshotDTO } from '@/types'

export const indicatorApi = {
  calculate: (symbolCode: string) => request.post<unknown, { count: number }>('/indicators/calculate', null, { params: { symbolCode } }),
  calculateAll: () => request.post<unknown, { count: number }>('/indicators/calculate-all'),
  list: (symbolCode: string, start?: string, end?: string) =>
    request.get<unknown, IndicatorSnapshotDTO[]>('/indicators', { params: { symbolCode, start, end } }),
  latest: (symbolCode: string) => request.get<unknown, IndicatorSnapshotDTO>('/indicators/latest', { params: { symbolCode } })
}
