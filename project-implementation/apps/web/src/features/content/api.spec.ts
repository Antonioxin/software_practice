import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, apiDownload, ApiProblem, newIdempotencyKey } from '../../services/http'
import { downloadFile, readProducts, saveRecord, setPublication, uploadFile } from './api'
import type { ContentFile } from './types'
vi.mock('../../services/http', async original => ({ ...(await original<typeof import('../../services/http')>()), api: vi.fn(), apiDownload: vi.fn(), newIdempotencyKey: vi.fn() }))
const mockedApi = vi.mocked(api), mockedDownload = vi.mocked(apiDownload)
let keyCount = 0
beforeEach(() => { mockedApi.mockReset(); mockedDownload.mockReset(); vi.mocked(newIdempotencyKey).mockImplementation(() => `00000000-0000-4000-8000-${++keyCount}`) })

describe('内容 API 契约', () => {
  it('商品选项逐页读取，单页不超过服务端上限 50', async () => {
    mockedApi.mockImplementation(async path => { const query = new URL(path, 'https://test.invalid').searchParams; const page = Number(query.get('page')); expect(query.get('pageSize')).toBe('50'); return { data: Array.from({ length: page === 1 ? 50 : 10 }, (_, i) => ({ id: `${page}-${i}`, name: '商品' })), meta: { page, pageSize: 50, totalItems: 60, totalPages: 2 } } as never })
    expect(await readProducts(true)).toHaveLength(60)
    expect(mockedApi).toHaveBeenCalledTimes(2)
  })
  it('网络未知的创建重试复用幂等键，成功后新创建使用新键', async () => {
    mockedApi.mockRejectedValueOnce(new TypeError('Failed to fetch')).mockResolvedValue({ data: { id: 'saved' } })
    const body = { title: '网络未知恢复测试' }
    await expect(saveRecord('/admin/articles', body)).rejects.toThrow('Failed to fetch')
    await saveRecord('/admin/articles', body)
    await saveRecord('/admin/articles', body)
    const keys = mockedApi.mock.calls.map(([, init]) => new Headers(init?.headers).get('Idempotency-Key'))
    expect(keys[0]).toBe(keys[1]); expect(keys[2]).not.toBe(keys[1])
  })
  it('发布请求携带版本，并在明确校验失败后重新取得幂等键', async () => {
    mockedApi.mockRejectedValueOnce(new ApiProblem({ type: 'about:blank', title: 'Validation', status: 422, detail: '请设置图片', code: 'VALIDATION_ERROR' })).mockResolvedValue({ data: {} })
    await expect(setPublication('/admin/banners', { id: 'banner', version: 7 }, true)).rejects.toThrow('请设置图片')
    await setPublication('/admin/banners', { id: 'banner', version: 7 }, true)
    expect(JSON.parse(mockedApi.mock.calls[0]![1]!.body as string)).toEqual({ expectedVersion: 7 })
    expect(new Headers(mockedApi.mock.calls[0]![1]?.headers).get('Idempotency-Key')).not.toBe(new Headers(mockedApi.mock.calls[1]![1]?.headers).get('Idempotency-Key'))
  })
  it('PDF 上传使用二进制及 JSON metadata 分段', async () => {
    mockedApi.mockResolvedValue({ data: {} })
    const file = new File(['%PDF-1.7'], 'guide.pdf', { type: 'application/pdf' })
    await uploadFile(file, { title: '商品说明', type: '产品说明', productIds: [], visibility: 'PUBLIC', versionNote: 'v1' })
    const body = mockedApi.mock.calls[0]![1]!.body as FormData
    expect(body.get('file')).toBe(file)
    expect((body.get('metadata') as Blob).type).toBe('application/json')
    expect(mockedApi.mock.calls[0]![0]).toBe('/admin/files')
  })
  it('下载权限错误直接传播，不创建虚假的下载成功或外部请求', async () => {
    const file = { downloadUrl: '/api/v1/files/f/versions/v/content', filename: 'guide.pdf' } as ContentFile
    mockedDownload.mockRejectedValue(new ApiProblem({ type: 'about:blank', title: 'Forbidden', status: 403, detail: '需要经销商身份', code: 'FORBIDDEN' }))
    await expect(downloadFile(file)).rejects.toThrow('需要经销商身份')
    expect(mockedDownload).toHaveBeenCalledWith('/files/f/versions/v/content')
    await expect(downloadFile({ ...file, downloadUrl: 'https://other.invalid/content' })).rejects.toThrow('文件下载地址无效')
    expect(mockedDownload).toHaveBeenCalledTimes(1)
  })
})
