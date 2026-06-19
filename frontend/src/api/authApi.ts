import request from '@/api/request'
import type { AuthSessionDTO, LoginRequest } from '@/types'

export const authApi = {
  login(payload: LoginRequest) {
    return request.post<unknown, AuthSessionDTO>('/auth/login', payload)
  },
  me() {
    return request.get<unknown, AuthSessionDTO>('/auth/me')
  }
}
