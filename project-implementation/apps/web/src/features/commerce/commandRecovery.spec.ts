import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearCommands, useCommandRecovery } from './commandRecovery'
import { api, ApiProblem } from '../../services/http'
vi.mock('../../services/http', async (original) => ({
  ...(await original<typeof import('../../services/http')>()),
  api: vi.fn(),
}))
const mocked = vi.mocked(api)
describe('原命令恢复', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.resetAllMocks()
  })
  it('响应丢失后恢复完全相同的键和请求体', async () => {
    mocked.mockRejectedValueOnce(new TypeError('network')).mockResolvedValueOnce({ data: { id: 'order' } })
    const first = useCommandRecovery(() => 'alice', 'createOrder')
    await expect(first.send('/orders', { expectedVersion: 1 })).rejects.toThrow('结果暂未确认')
    const second = useCommandRecovery(() => 'alice', 'createOrder')
    expect(second.pending.value).not.toBeNull()
    await second.retry()
    expect(mocked.mock.calls[0]).toEqual(mocked.mock.calls[1])
    expect(second.pending.value).toBeNull()
  })
  it('不允许未知结果期间修改目标或版本', async () => {
    mocked.mockRejectedValue(new TypeError('network'))
    const command = useCommandRecovery(() => 'alice', 'pay')
    await expect(command.send('/orders/1/pay', { expectedVersion: 1 })).rejects.toThrow()
    await expect(command.send('/orders/2/pay', { expectedVersion: 2 })).rejects.toThrow('原请求')
    expect(mocked).toHaveBeenCalledTimes(1)
  })
  it('不同账户不读原请求，退出清除敏感地址', async () => {
    mocked.mockRejectedValue(new TypeError('network'))
    await expect(
      useCommandRecovery(() => 'alice', 'create').send('/orders', { address: 'private' }),
    ).rejects.toThrow()
    expect(useCommandRecovery(() => 'bob', 'create').pending.value).toBeNull()
    clearCommands()
    expect(sessionStorage.length).toBe(0)
  })
  it('明确业务冲突允许重新确认，进行中保留原键', async () => {
    const command = useCommandRecovery(() => 'alice', 'pay')
    mocked.mockRejectedValueOnce(
      new ApiProblem({
        type: 'about:blank',
        title: 'Conflict',
        status: 409,
        code: 'REQUEST_IN_PROGRESS',
        detail: 'wait',
      }),
    )
    await expect(command.send('/pay', {})).rejects.toThrow()
    expect(command.pending.value).not.toBeNull()
    mocked.mockRejectedValueOnce(
      new ApiProblem({
        type: 'about:blank',
        title: 'Conflict',
        status: 409,
        code: 'VERSION_CONFLICT',
        detail: 'changed',
      }),
    )
    await expect(command.retry()).rejects.toThrow('changed')
    expect(command.pending.value).toBeNull()
  })
  it('超过24小时不自动重发', async () => {
    mocked.mockRejectedValue(new TypeError('network'))
    const command = useCommandRecovery(() => 'alice', 'add')
    await expect(command.send('/cart/items', {})).rejects.toThrow()
    const key = Object.keys(sessionStorage)[0]!
    const saved = JSON.parse(sessionStorage.getItem(key)!)
    saved.startedAt = Date.now() - 86400001
    sessionStorage.setItem(key, JSON.stringify(saved))
    await expect(command.retry()).rejects.toThrow('24小时')
    expect(mocked).toHaveBeenCalledTimes(1)
  })
})
