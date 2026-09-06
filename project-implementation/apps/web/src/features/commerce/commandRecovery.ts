import { ref } from 'vue'
import { api, ApiProblem } from '../../services/http'

export interface PendingCommand {
  actorId: string
  operation: string
  path: string
  key: string
  body: string
  startedAt: number
}
const prefix = 'wemove:command:'
export function clearCommands() {
  for (const key of Object.keys(sessionStorage)) if (key.startsWith(prefix)) sessionStorage.removeItem(key)
}
export function useCommandRecovery(actor: () => string | undefined, operation: string) {
  const busy = ref(false)
  const pending = ref<PendingCommand | null>(null)
  function storageKey() {
    return prefix + actor() + ':' + operation
  }
  function restore() {
    const value = sessionStorage.getItem(storageKey())
    try {
      pending.value = value ? (JSON.parse(value) as PendingCommand) : null
    } catch {
      pending.value = null
    }
    if (pending.value?.actorId !== actor()) pending.value = null
    return pending.value
  }
  restore()
  async function send<T>(path: string, body: unknown): Promise<T> {
    if (!actor()) throw new Error('请先登录。')
    if (busy.value) throw new Error('请求正在发送。')
    restore()
    const serialized = JSON.stringify(body)
    if (pending.value && (pending.value.path !== path || pending.value.body !== serialized))
      throw new Error('有结果尚未确认的原请求，请先重试或查询原结果。')
    if (!pending.value) {
      pending.value = {
        actorId: actor()!,
        operation,
        path,
        body: serialized,
        key: crypto.randomUUID(),
        startedAt: Date.now(),
      }
      // Save before sending; if storage is unavailable, no request is sent.
      sessionStorage.setItem(storageKey(), JSON.stringify(pending.value))
    }
    return retry<T>()
  }
  async function retry<T>(): Promise<T> {
    restore()
    const command = pending.value
    if (!command || command.actorId !== actor()) throw new Error('没有可恢复请求。')
    if (Date.now() - command.startedAt >= 86400000)
      throw new Error('请求已超过24小时，请通过购物车或订单列表核对，不自动重发。')
    busy.value = true
    try {
      const result = await api<T>(command.path, {
        method: 'POST',
        headers: { 'Idempotency-Key': command.key },
        body: command.body,
      })
      sessionStorage.removeItem(storageKey())
      pending.value = null
      return result.data
    } catch (error) {
      if (
        error instanceof ApiProblem &&
        error.problem.status < 500 &&
        error.problem.code !== 'REQUEST_IN_PROGRESS'
      ) {
        sessionStorage.removeItem(storageKey())
        pending.value = null
        throw error
      }
      throw new Error('结果暂未确认。请保留原请求重试，或查询购物车/订单；不要重复创建新操作。')
    } finally {
      busy.value = false
    }
  }
  return { busy, pending, send, retry, restore }
}
