import { describe, expect, it } from 'vitest'
import { articleIllustrations, editableHtml, parseArticleBlocks, serializeArticleBlocks, type ImageBlock } from './articleBlocks'

describe('文章图文区块往返', () => {
  it('插入和排序图片时保留既有标题、强调、链接与嵌套列表', () => {
    const original = '<h2>一起出发</h2><p>保留<strong>重点</strong>与<a href="/products">商品链接</a>。</p><ol><li>第一步<ul><li>小提示</li></ul></li></ol>'
    const blocks = parseArticleBlocks(original)
    const image = parseArticleBlocks(`<figure><img src="${articleIllustrations[0]!.src}" alt="平衡石"><figcaption>一步一步</figcaption></figure>`)[0]!
    expect(serializeArticleBlocks([image, ...blocks])).toContain(original)
    expect(serializeArticleBlocks(blocks)).toBe(original)
  })
  it('左右图文可重新打开编辑，保留配文中的富文本', () => {
    const original = `<section data-layout="image-right"><figure><img src="${articleIllustrations[1]!.src}" alt="彩虹桥"><figcaption>小小桥梁</figcaption></figure><div><h3>搭一座桥</h3><p>从<strong>想象</strong>开始。</p><ul><li>也可以当隧道</li></ul></div></section>`
    const blocks = parseArticleBlocks(original)
    expect(blocks[0]).toMatchObject({ type: 'image', layout: 'image-right', title: '搭一座桥', caption: '小小桥梁', html: '<p>从<strong>想象</strong>开始。</p><ul><li>也可以当隧道</li></ul>' })
    expect(serializeArticleBlocks(blocks)).toBe(original)
  })
  it('切换到通栏时配文仍保存在图片下方，并正确转义文字字段', () => {
    const block: ImageBlock = { id: 'image', type: 'image', layout: 'full', src: articleIllustrations[0]!.src, alt: '孩子与 "石头"', caption: '<一起玩>', title: '继续故事', html: '<p>已有配文不能丢。</p>' }
    const html = serializeArticleBlocks([block])
    expect(html).toContain('alt="孩子与 &quot;石头&quot;"')
    expect(html).toContain('<figcaption>&lt;一起玩&gt;</figcaption>')
    expect(html).toContain('</figure><h3>继续故事</h3><p>已有配文不能丢。</p>')
  })
  it('可编辑区清理脚本、事件、外部图片和危险链接', () => {
    const html = editableHtml(`<p onclick="alert(1)">可见<strong>内容</strong></p><script>alert(1)</script><img src="https://evil.test/a.png" onerror="alert(1)"><a href="javascript:alert(1)">链接</a><img src="${articleIllustrations[0]!.src}" alt="安全配图">`)
    expect(html).toContain('<p>可见<strong>内容</strong></p>')
    expect(html).toContain('<a>链接</a>')
    expect(html).toContain('alt="安全配图"')
    expect(html).not.toMatch(/onclick|onerror|script|evil\.test/)
  })
})
