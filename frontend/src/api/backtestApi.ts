import request from './request'
import type {
  BacktestBatchResultDTO,
  BacktestBatchRunRequest,
  BacktestRecordDetailDTO,
  BacktestRecordSummaryDTO,
  BacktestResultDTO,
  BacktestRunRequest
} from '@/types'

export const backtestApi = {
  run: (data: BacktestRunRequest) => request.post<unknown, BacktestResultDTO>('/backtest/run', data),
  batchRun: (data: BacktestBatchRunRequest) => request.post<unknown, BacktestBatchResultDTO>('/backtest/batch-run', data),
  records: (params?: { symbolCode?: string; limit?: number }) =>
    request.get<unknown, BacktestRecordSummaryDTO[]>('/backtest/records', { params }),
  recordDetail: (id: number) => request.get<unknown, BacktestRecordDetailDTO>(`/backtest/records/${id}`),
  deleteRecord: (id: number) => request.delete<unknown, void>(`/backtest/records/${id}`)
}
