import request from './request'
import type { FinalTradeDecisionDTO } from '@/types'

export const finalDecisionApi = {
  latest: () => request.get<unknown, FinalTradeDecisionDTO[]>('/final-decisions/latest'),
  history: (symbolCode: string) => request.get<unknown, FinalTradeDecisionDTO[]>('/final-decisions/history', { params: { symbolCode } })
}
