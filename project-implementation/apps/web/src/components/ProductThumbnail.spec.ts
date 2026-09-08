import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ProductThumbnail from './ProductThumbnail.vue'

describe('ProductThumbnail', () => {
  it('媒体库图片优先于静态示意，失效后占位，更换图片可以恢复', async () => {
    const id = 'e1000000-0000-4000-8000-000000000001'
    const nextId = 'e1000000-0000-4000-8000-000000000002'
    const wrapper = mount(ProductThumbnail, { props: { sku: 'WM-BALANCE-STONES', name: '平衡石', mainImageId: id } })
    expect(wrapper.get('img').attributes('src')).toBe(`/api/v1/media/${id}/content`)
    expect(wrapper.get('img').attributes('alt')).toBe('平衡石商品图片')
    await wrapper.get('img').trigger('error')
    expect(wrapper.find('.product-thumbnail__image').exists()).toBe(false)
    await wrapper.setProps({ mainImageId: nextId })
    expect(wrapper.get('img').attributes('src')).toBe(`/api/v1/media/${nextId}/content`)
  })
  it('按明确的 SKU 映射显示图片并标明示意图性质', () => {
    const wrapper = mount(ProductThumbnail, { props: { sku: 'WM-BALANCE-STONES', name: '平衡石' } })

    expect(wrapper.get('.product-thumbnail__image').attributes('src')).toBe('/assets/products/balance-stones.png')
    expect(wrapper.get('.product-thumbnail__image').attributes('alt')).toBe('平衡石，AI 商品示意图，非产品实拍')
    expect(wrapper.find('[role="img"]').exists()).toBe(false)
  })

  it('未知 SKU 显示可访问占位，不按名称猜测图片', () => {
    const wrapper = mount(ProductThumbnail, { props: { sku: 'UNLISTED-PRODUCT', name: '平衡石' } })

    expect(wrapper.find('.product-thumbnail__image').exists()).toBe(false)
    expect(wrapper.get('[role="img"]').attributes('aria-label')).toBe('平衡石，暂无商品图片')
  })

  it('图片加载失败后显示占位', async () => {
    const wrapper = mount(ProductThumbnail, { props: { sku: 'WM-BALANCE-STONES', name: '平衡石' } })
    await wrapper.get('.product-thumbnail__image').trigger('error')

    expect(wrapper.find('.product-thumbnail__image').exists()).toBe(false)
    expect(wrapper.get('[role="img"]').attributes('aria-label')).toBe('平衡石，暂无商品图片')
  })

  it('切换商品后清除之前的加载失败状态', async () => {
    const wrapper = mount(ProductThumbnail, { props: { sku: 'WM-BALANCE-STONES', name: '平衡石' } })
    await wrapper.get('.product-thumbnail__image').trigger('error')
    await wrapper.setProps({ sku: 'WM-RAINBOW-ARCH', name: '彩虹拱门' })

    expect(wrapper.get('.product-thumbnail__image').attributes('src')).toBe('/assets/products/rainbow-arch.png')
    expect(wrapper.get('.product-thumbnail__image').attributes('alt')).toBe('彩虹拱门，AI 商品示意图，非产品实拍')
    expect(wrapper.find('[role="img"]').exists()).toBe(false)
  })
})
