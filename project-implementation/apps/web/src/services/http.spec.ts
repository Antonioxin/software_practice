import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiDownload, ApiProblem } from './http'

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks() })
const path = '/files/file-id/versions/current-id/content'
describe('共享受控下载', () => {
  it('以同源会话获取PDF，禁止响应缓存', async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response('%PDF-1.7\n', { headers: { 'Content-Type': 'application/pdf' } }))
    vi.stubGlobal('fetch', fetcher)
    const blob = await apiDownload(path)
    expect(blob.type).toBe('application/pdf')
    expect(fetcher).toHaveBeenCalledWith(`/api/v1${path}`, expect.objectContaining({ credentials: 'include', cache: 'no-store' }))
  })
  it('会话过期通知统一身份存储，并保留结构化错误', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ status: 401, code: 'SESSION_EXPIRED', detail: '会话已过期。' }), { status: 401 })))
    const event = vi.spyOn(window, 'dispatchEvent')
    await expect(apiDownload(path)).rejects.toBeInstanceOf(ApiProblem)
    expect(event).toHaveBeenCalledWith(expect.objectContaining({ type: 'wemove:auth-invalid' }))
  })
  it('不能向外部地址发送Cookie，也不能把错误HTML保存成PDF', async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response('<html>failure</html>', { headers: { 'Content-Type': 'text/html' } }))
    vi.stubGlobal('fetch', fetcher)
    await expect(apiDownload('https://example.invalid/files/file-id/versions/current-id/content')).rejects.toThrow('无效的下载地址')
    expect(fetcher).not.toHaveBeenCalled()
    await expect(apiDownload(path)).rejects.toThrow('不是 PDF')
  })
})
