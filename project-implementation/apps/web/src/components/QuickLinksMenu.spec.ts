import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import { nextTick } from 'vue'
import QuickLinksMenu from './QuickLinksMenu.vue'
import PublicShell from './PublicShell.vue'
import { useSessionStore } from '../stores/session'
import { fixtures } from '../dev/fixtures'

let wrapper: VueWrapper | undefined
let router: Router
const fetchSpy = vi.fn(() => { throw new Error('快捷入口不应发送业务请求') })

async function render(role: 'guest' | 'user' | 'admin' = 'guest', shell = false) {
  vi.stubGlobal('fetch', fetchSpy)
  const pinia = createPinia()
  setActivePinia(pinia)
  const session = useSessionStore()
  if (role !== 'guest') session.replace(fixtures.actors[role])
  router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  await router.push('/')
  wrapper = mount(shell ? PublicShell : QuickLinksMenu, { attachTo: document.body, global: { plugins: [pinia, router] } })
  await router.isReady()
  return wrapper
}

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  document.body.innerHTML = ''
  expect(fetchSpy).not.toHaveBeenCalled()
  fetchSpy.mockClear()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('快捷入口下拉导航', () => {
  it('默认收起，游客与普通用户看到原有四个入口', async () => {
    const menu = await render('user')
    const button = menu.get('button')
    expect(button.text()).toBe('快捷入口')
    expect(button.attributes('aria-expanded')).toBe('false')
    expect(menu.find('nav').exists()).toBe(false)
    await button.trigger('click')
    expect(button.attributes('aria-expanded')).toBe('true')
    expect(menu.get('nav').attributes('id')).toBe(button.attributes('aria-controls'))
    expect(menu.findAll('nav a').map(link => link.attributes('href'))).toEqual(['/products', '/cart', '/account/orders', '/account/profile'])
    expect(menu.get('nav').attributes('role')).toBeUndefined()
    expect(menu.findAll('nav a').every(link => (link.element as HTMLAnchorElement).tabIndex === 0)).toBe(true)
  })

  it('管理员的购物车、订单和账户入口映射到对应管理页面', async () => {
    const menu = await render('admin')
    await menu.get('button').trigger('click')
    expect(menu.findAll('nav a').map(link => link.attributes('href'))).toEqual(['/products', '/admin/products', '/admin/orders', '/admin/users'])
    expect(menu.text()).toContain('整理好物')
    expect(menu.text()).not.toContain('装一点快乐')
  })

  it('键盘 Escape 关闭并将焦点返回触发按钮', async () => {
    const menu = await render()
    const button = menu.get('button')
    await button.trigger('click')
    const firstLink = menu.get('nav a')
    ;(firstLink.element as HTMLAnchorElement).focus()
    expect(document.activeElement).toBe(firstLink.element)
    await firstLink.trigger('keydown', { key: 'Escape' })
    expect(menu.find('nav').exists()).toBe(false)
    expect(document.activeElement).toBe(button.element)
  })

  it('选择链接和程序化路由改变都会关闭下拉', async () => {
    const menu = await render()
    await menu.get('button').trigger('click')
    await menu.get('nav a[href="/products"]').trigger('click')
    await flushPromises()
    expect(menu.find('nav').exists()).toBe(false)
    expect(router.currentRoute.value.path).toBe('/products')
    await menu.get('button').trigger('click')
    await router.push('/cart')
    await nextTick()
    expect(menu.find('nav').exists()).toBe(false)
  })

  it('点击外部或将焦点移出时关闭，组件卸载后清理监听', async () => {
    const removeListener = vi.spyOn(document, 'removeEventListener')
    const menu = await render()
    await menu.get('button').trigger('click')
    document.body.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true }))
    await nextTick()
    expect(menu.find('nav').exists()).toBe(false)
    await menu.get('button').trigger('click')
    ;(menu.get('nav a').element as HTMLAnchorElement).focus()
    const outside = document.createElement('button')
    document.body.append(outside)
    outside.focus()
    await nextTick()
    expect(menu.find('nav').exists()).toBe(false)
    menu.unmount()
    wrapper = undefined
    expect(removeListener).toHaveBeenCalledWith('pointerdown', expect.any(Function))
  })

  it('入口位于公共头部操作区，独立于移动端会隐藏的主导航', async () => {
    const page = await render('guest', true)
    expect(page.find('.wm-header-actions .wm-quick-links').exists()).toBe(true)
    expect(page.find('.wm-navigation .wm-quick-links').exists()).toBe(false)
    expect(page.find('.wm-header-actions a[aria-label="购物车"]').exists()).toBe(false)
    expect(page.find('.wm-header-actions a[aria-label="登录或注册"]').exists()).toBe(true)
    expect(page.find('button[aria-controls="public-mobile-menu"]').exists()).toBe(true)
  })
})
