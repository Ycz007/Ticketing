export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) { super(message); this.status = status }
}

type Csrf = { headerName: string; token: string }

async function send<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`/api${path}`, { ...init, credentials: 'same-origin' })
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: '请求失败' }))
    throw new ApiError(response.status, error.message ?? '请求失败')
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  get: <T>(path: string, signal?: AbortSignal) => send<T>(path, { signal }),
  async post<T>(path: string, body?: BodyInit): Promise<T> {
    // Fetch again after login/logout: Spring rotates the CSRF token.
    const csrf = await send<Csrf>('/auth/csrf')
    return send<T>(path, { method: 'POST', body, headers: { [csrf.headerName]: csrf.token } })
  },
  async json<T>(path: string, body: unknown): Promise<T> {
    const csrf = await send<Csrf>('/auth/csrf')
    return send<T>(path, { method: 'POST', body: JSON.stringify(body),
      headers: { [csrf.headerName]: csrf.token, 'Content-Type': 'application/json' } })
  },
}
