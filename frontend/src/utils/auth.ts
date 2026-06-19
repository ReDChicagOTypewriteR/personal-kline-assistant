import type { AuthSessionDTO } from '@/types'

const AUTH_STORAGE_KEY = 'personal-kline-auth-session'

export function getAuthSession(): AuthSessionDTO | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw) as Partial<AuthSessionDTO>
    if (!parsed.token || !parsed.username) {
      return null
    }
    return {
      token: parsed.token,
      username: parsed.username,
      displayName: parsed.displayName || parsed.username
    }
  } catch {
    return null
  }
}

export function getAuthToken(): string {
  return getAuthSession()?.token || ''
}

export function saveAuthSession(session: AuthSessionDTO) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
}

export function clearAuthSession() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
}

export function isAuthenticated() {
  return Boolean(getAuthToken())
}
