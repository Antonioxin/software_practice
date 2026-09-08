import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ProfilePage from './ProfilePage.vue'
import { api, ApiProblem } from '../services/http'
import { fixtures } from '../dev/fixtures'
import { useSessionStore } from '../stores/session'
import type { Actor } from '../types'

vi.mock('../services/http', async original => ({
  ...(await original<typeof import('../services/http')>()),
  api: vi.fn(),
}))
const mockedApi = vi.mocked(api)
let wrapper: VueWrapper | undefined
let session: ReturnType<typeof useSessionStore>

async function render(actor: Actor = structuredClone(fixtures.actors.user)) {
  const pinia = createPinia()
  setActivePinia(pinia)
  session = useSessionStore()
  session.replace(actor)
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  await router.push('/account/profile')
  wrapper = mount(ProfilePage, { attachTo: document.body, global: { plugins: [pinia, router] } })
  await flushPromises()
  return wrapper
}
async function edit() {
  await wrapper!.get('.account-section-heading button').trigger('click')
}

beforeEach(() => {
  mockedApi.mockReset()
  vi.stubGlobal('fetch', vi.fn(() => { throw new Error('个人资料测试不得访问真实后端') }))
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  document.body.innerHTML = ''
  expect(fetch).not.toHaveBeenCalled()
  vi.unstubAllGlobals()
})

describe('个人资料查看与编辑', () => {
  it('默认展示只读资料，进入编辑后聚焦昵称，取消恢复已保存内容', async () => {
    const page = await render()
    expect((page.get('#profile-nickname').element as HTMLInputElement).readOnly).toBe(true)
    expect((page.get('#profile-email').element as HTMLInputElement).disabled).toBe(true)
    await edit()
    expect(document.activeElement?.id).toBe('profile-nickname')
    await page.get('#profile-nickname').setValue('尚未保存的昵称')
    await page.get('#profile-phone').setValue('13800000000')
    await page.get('.account-cancel-button').trigger('click')
    expect((page.get('#profile-nickname').element as HTMLInputElement).value).toBe(fixtures.actors.user.nickname)
    expect((page.get('#profile-phone').element as HTMLInputElement).value).toBe('')
    expect(session.actor?.nickname).toBe(fixtures.actors.user.nickname)
    expect(mockedApi).not.toHaveBeenCalled()
    expect(document.activeElement?.textContent).toContain('编辑资料')
  })

  it('保存仅提交昵称和电话，用服务端返回值同步资料与个人卡片', async () => {
    const page = await render()
    const saved = { ...structuredClone(fixtures.actors.user), nickname: '新的称呼', phone: '+8613800000000', version: 2 }
    mockedApi.mockResolvedValue({ data: saved })
    await edit()
    await page.get('#profile-nickname').setValue('新的称呼')
    await page.get('#profile-phone').setValue('+86 138 0000 0000')
    await page.get('form[aria-label="个人资料"]').trigger('submit')
    await flushPromises()
    expect(mockedApi).toHaveBeenCalledExactlyOnceWith('/account/profile', {
      method: 'PATCH', body: JSON.stringify({ nickname: '新的称呼', phone: '+86 138 0000 0000' }),
    })
    expect(session.actor).toEqual(saved)
    expect(page.get('#account-name').text()).toBe('新的称呼')
    expect((page.get('#profile-phone').element as HTMLInputElement).value).toBe(saved.phone)
    expect((page.get('#profile-nickname').element as HTMLInputElement).readOnly).toBe(true)
    expect(page.get('[role="status"]').text()).toContain('个人资料已保存')
  })

  it('保存时阻止重复提交并禁用取消，失败后保留草稿并关联字段错误', async () => {
    const page = await render()
    let reject!: (error: ApiProblem) => void
    mockedApi.mockImplementation(() => new Promise((_, fail) => { reject = fail }))
    await edit()
    await page.get('#profile-phone').setValue('无效号码')
    const form = page.get('form[aria-label="个人资料"]')
    await form.trigger('submit')
    await form.trigger('submit')
    expect(mockedApi).toHaveBeenCalledOnce()
    expect(page.get('.account-cancel-button').attributes('disabled')).toBeDefined()
    expect((page.get('#profile-phone').element as HTMLInputElement).disabled).toBe(true)
    reject(new ApiProblem({ type: 'about:blank', title: 'Validation failed', status: 422, code: 'VALIDATION_FAILED', detail: '请检查填写的资料。', errors: [{ field: 'phone', code: 'INVALID_PHONE', message: '请输入有效联系电话。' }] }))
    await flushPromises()
    expect(page.get('[role="alert"]').text()).toContain('请检查填写的资料')
    expect(page.get('#profile-phone').attributes('aria-describedby')).toBe('profile-phone-error')
    expect(page.get('#profile-phone').attributes('aria-invalid')).toBe('true')
    expect((page.get('#profile-phone').element as HTMLInputElement).value).toBe('无效号码')
    expect((page.get('#profile-phone').element as HTMLInputElement).readOnly).toBe(false)
    expect(session.actor?.phone).toBeNull()
  })

  it('保留个人业务入口，普通账户可查看询价历史，经销商额外显示专属目录', async () => {
    const page = await render()
    for (const path of ['/account/orders', '/cart', '/account/tickets', '/contact', '/account/dealer-application', '/account/inquiries']) {
      expect(page.findAll('a').some(link => link.attributes('href') === path)).toBe(true)
    }
    expect(page.find('a[href="/dealer/catalog"]').exists()).toBe(false)
    session.replace({ ...structuredClone(fixtures.actors.user), derivedIdentity: 'DEALER' })
    await flushPromises()
    expect(page.find('a[href="/dealer/catalog"]').exists()).toBe(true)
    expect(page.text()).toContain('有效经销商')
  })

  it('仅有读取权限时不显示编辑入口，也不能通过提交事件写入资料', async () => {
    const page = await render({ ...structuredClone(fixtures.actors.user), capabilities: ['ACCOUNT_PROFILE_READ'] })
    expect(page.find('.account-section-heading button').exists()).toBe(false)
    await page.get('form[aria-label="个人资料"]').trigger('submit')
    expect(mockedApi).not.toHaveBeenCalled()
  })
})
