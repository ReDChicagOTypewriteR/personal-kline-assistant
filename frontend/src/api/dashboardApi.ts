import request from './request'
import type { DashboardDTO, SymbolTechnicalDetailDTO } from '@/types'

export const dashboardApi = {
  get: () => request.get<unknown, DashboardDTO>('/dashboard'),
  symbolDetail: (symbolCode: string, start?: string, end?: string) =>
    request.get<unknown, SymbolTechnicalDetailDTO>(`/dashboard/symbol/${symbolCode}`, { params: { start, end } })
}
