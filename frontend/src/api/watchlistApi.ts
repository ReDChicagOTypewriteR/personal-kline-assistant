import request from './request'
import type { WatchlistDTO } from '@/types'

export const watchlistApi = {
  list: () => request.get<unknown, WatchlistDTO[]>('/watchlist'),
  add: (symbolCode: string, groupName = 'DEFAULT') =>
    request.post<unknown, WatchlistDTO>('/watchlist', null, { params: { symbolCode, groupName } }),
  remove: (symbolCode: string, groupName = 'DEFAULT') =>
    request.delete<unknown, void>('/watchlist', { params: { symbolCode, groupName } }),
  removeById: (id: number) => request.delete<unknown, void>(`/watchlist/${id}`),
  removeBatch: (ids: number[]) => request.delete<unknown, { count: number }>('/watchlist/batch', { data: ids })
}
