import request from './request'
import type { CacheRefreshResultDTO, MarketDataSourceOverviewDTO } from '@/types'

export const systemApi = {
  marketDataSources: () => request.get<unknown, MarketDataSourceOverviewDTO>('/system/market-data/sources'),
  refreshCache: () => request.post<unknown, CacheRefreshResultDTO>('/system/cache/refresh'),
  refreshKlineCache: (symbolCode?: string) =>
    request.post<unknown, CacheRefreshResultDTO>('/system/cache/kline/refresh', null, { params: { symbolCode } })
}
