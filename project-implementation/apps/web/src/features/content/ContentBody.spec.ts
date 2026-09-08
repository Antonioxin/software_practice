import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ContentBody from './ContentBody'
import { inlineImages, safeImage } from './bodyMarkup'
import { articleCover } from './presentation'

describe('受控正文渲染', () => {
  it('保留段落与安全链接，去除脚本、嵌入和事件属性', () => {
    const view = mount(ContentBody, { props: { body: '<h2 onclick="alert(1)">保养指南</h2><p>用<strong>软布</strong>擦拭。<script>alert(1)</script><iframe src="https://bad.test"></iframe><img src="x" onerror="alert(1)"></p><a href="javascript:alert(1)">危险链接</a><a href="//bad.test">协议相对链接</a><a href="/products">查看商品</a>' } })
    expect(view.find('h2').text()).toBe('保养指南')
    expect(view.find('strong').text()).toBe('软布')
    expect(view.find('h2').attributes('onclick')).toBeUndefined()
    expect(view.findAll('script, iframe, img')).toHaveLength(0)
    expect(view.text()).not.toContain('alert(1)')
    expect(view.findAll('a').map(link => link.attributes('href'))).toEqual([undefined, undefined, '/products'])
  })
  it('普通文本中的尖括号和换行按文本展示', () => {
    const view = mount(ContentBody, { props: { body: '温度 < 40°C\n轻轻擦拭，不要浸泡。' } })
    expect(view.text()).toContain('温度 < 40°C\n轻轻擦拭')
    expect(view.classes()).toContain('content-body-plain')
  })
  it('保留左右图文与图注，只输出受控的图片属性', () => {
    const src = '/assets/products/guides/balance-stones-guide.png'
    const view = mount(ContentBody, { props: { body: `<section data-layout="image-right" class="attack" style="position:fixed"><figure><img src="${src}" alt="孩子走过平衡石" onerror="alert(1)" srcset="https://other.test/x.png 2x" /><figcaption>慢慢走，也很好玩。</figcaption></figure><div><h3>一起找平衡</h3><p>把<strong>小路</strong>摆好。</p></div></section>` } })
    expect(view.get('section').attributes()).toEqual({ 'data-layout': 'image-right' })
    expect(view.get('img').attributes()).toEqual({ src, alt: '孩子走过平衡石', loading: 'lazy', decoding: 'async' })
    expect(view.get('figcaption').text()).toBe('慢慢走，也很好玩。')
    expect(view.get('section > div strong').text()).toBe('小路')
  })
  it('拒绝外站、协议相对、内联数据和伪装成本地资源的图片', () => {
    for (const src of ['https://example.test/x.png', '//example.test/x.png', 'data:image/svg+xml,x', '/assets/products/guides/../private.png', '/assets/products/guides/balance-stones-guide.png?x=1', '/api/v1/media/not-an-id/content', '/api/v1/files/a/content', '/\\example.test/x']) {
      expect(safeImage(src), src).toBeUndefined()
      expect(mount(ContentBody, { props: { body: `<figure><img src="${src}" /><figcaption>图片说明仍可读</figcaption></figure>` } }).find('img').exists()).toBe(false)
    }
    const view = mount(ContentBody, { props: { body: '<section data-layout="arbitrary"><p>文本</p></section>' } })
    expect(view.get('section').attributes('data-layout')).toBeUndefined()
  })
  it('注册媒体与插图库都能提取封面，过滤不可显示的图片', () => {
    const src = '/api/v1/media/e1000000-0000-4000-8000-000000000001/content'
    const body = `<img src="https://bad.test/image"><figure><img src="${src}" alt="走过小路"></figure>`
    expect(inlineImages(body)).toEqual([{ src, alt: '走过小路' }])
    expect(articleCover({ body, mediaIds: [] })).toBe(src)
    const preset = '/assets/products/guides/rainbow-arch-guide.png'
    expect(articleCover({ body: `<figure><img src="${preset}"></figure>${body}`, mediaIds: ['e1000000-0000-4000-8000-000000000001'] })).toBe(preset)
    expect(inlineImages(`<form><img src="${src}"></form>`)).toEqual([])
    expect(articleCover({ body: '<p>只有文字</p>', mediaIds: [] })).toBeUndefined()
  })
  it('已知主题使用专用封面，独立上传封面优先，其他文章沿用正文首图', () => {
    const src = '/assets/products/guides/balance-stones-guide.png'
    const body = `<figure><img src="${src}" alt="走过小路"></figure>`
    for (const id of ['e1000000-0000-4000-8000-000000000001', 'e2000000-0000-4000-8000-000000000001']) {
      expect(articleCover({ id, body, mediaIds: [] })).toBe('/assets/articles/living-room-adventure-cover-v2.png')
      expect(articleCover({ id, body, mediaIds: ['e1000000-0000-4000-8000-000000000002'] })).toBe('/api/v1/media/e1000000-0000-4000-8000-000000000002/content')
    }
    expect(articleCover({ id: 'e1000000-0000-4000-8000-000000000003', body, mediaIds: [] })).toBe(src)
  })
})
