import type { ApiEnvelope, ProblemDetails } from '../types'

interface CsrfPayload { token: string; headerName: string }

let csrf: CsrfPayload | null = null

export class ApiProblem extends Error {
  constructor(public readonly problem: ProblemDetails) {
    super(problem.detail)
  }

  field(name: string): string | undefined {
    return this.problem.errors?.find((item) => item.field === name)?.message
  }
}

async function getCsrf() {
  if (csrf) return csrf
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'include' })
  if (!response.ok) throw await toProblem(response)
  const body = (await response.json()) as ApiEnvelope<CsrfPayload>
  csrf = body.data
  return csrf
}

async function toProblem(response: Response): Promise<ApiProblem> {
  let problem: ProblemDetails
  try {
    problem = (await response.json()) as ProblemDetails
  } catch {
    problem = {
      type: 'about:blank',
      title: 'Request failed',
      status: response.status,
      detail: '服务暂时无法响应，请稍后重试。',
      code: 'UNEXPECTED_RESPONSE',
    }
  }
  if (response.status === 401) window.dispatchEvent(new CustomEvent('wemove:auth-invalid'))
  return new ApiProblem(problem)
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<ApiEnvelope<T>> {
  return (await apiWithMeta<T>(path, init)).envelope
}

export async function apiWithMeta<T>(path: string, init: RequestInit = {}): Promise<{
  envelope: ApiEnvelope<T>; status: number; replayed: boolean; requestId: string | null
}> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json, application/problem+json')
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = await getCsrf()
    headers.set(token.headerName, token.token)
  }
  const response = await fetch(`/api/v1${path}`, { ...init, method, headers, credentials: 'include' })
  if (!response.ok) throw await toProblem(response)
  const envelope = response.status === 204 ? { data: undefined as T } : (await response.json()) as ApiEnvelope<T>
  return { envelope, status: response.status, replayed: response.headers.get('Idempotency-Replayed') === 'true', requestId: response.headers.get('X-Request-Id') }
}

export function resetCsrf() { csrf = null }
export function newIdempotencyKey() { return crypto.randomUUID() }
