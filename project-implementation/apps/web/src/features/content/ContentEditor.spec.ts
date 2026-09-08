import { mount, type VueWrapper } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ContentEditor from './ContentEditor.vue'
import { articleIllustrations } from './articleBlocks'
import type { Media } from './types'

function button(wrapper: VueWrapper, text: string) { return wrapper.findAll('button').find(item => item.text() === text)! }
function currentValue(wrapper: VueWrapper) { return wrapper.emitted<string[]>('update:modelValue')!.at(-1)![0]! }

describe('文章可视化编辑器', () => {
  it('添加、选择、排序和移除图文块，并即时预览', async () => {
    const original = '<h2>周末故事</h2><p>在<strong>户外</strong>一起玩。</p>'
    const wrapper = mount(ContentEditor, { props: { id: 'body', label: '正文', article: true, modelValue: original } })
    expect(wrapper.find('textarea').exists()).toBe(false)
    expect(wrapper.get('[contenteditable]').html()).toContain('<strong>户外</strong>')
    await button(wrapper, '添加图文块').trigger('click')
    await wrapper.get('[aria-label="选择套圈 · 一起瞄准"]').trigger('click')
    await button(wrapper, '图右文左').trigger('click')
    const values = wrapper.findAll('.article-image-fields input')
    await values[0]!.setValue('孩子拿起套圈')
    await values[1]!.setValue('站稳，再瞄准')
    expect(currentValue(wrapper)).toContain(original)
    expect(currentValue(wrapper)).toContain('data-layout="image-right"')
    expect(currentValue(wrapper)).toContain('alt="孩子拿起套圈"')
    expect(wrapper.get('.content-body figure img').attributes('src')).toBe(articleIllustrations[2]!.src)
    expect(wrapper.get('.content-body figcaption').text()).toBe('站稳，再瞄准')
    await wrapper.get('[aria-label="上移第 2 块"]').trigger('click')
    expect(currentValue(wrapper).startsWith('<section')).toBe(true)
    await wrapper.get('[aria-label="移除第 1 块"]').trigger('click')
    expect(currentValue(wrapper)).toBe(original)
    wrapper.unmount()
  })
  it('输入富文本时保存其格式，选择上传媒体时使用受控地址', async () => {
    const media: Media = { id: 'e1000000-0000-4000-8000-000000000010', url: 'https://unused.test/image.png', altText: '我们的玩法照片', filename: 'photo.png', mimeType: 'image/png', width: 300, height: 300, sizeBytes: 100, version: 1, referenceCount: 0, publicReferenceCount: 0, createdAt: '' }
    const wrapper = mount(ContentEditor, { props: { id: 'body', label: '正文', article: true, media: [media], modelValue: '' } })
    await button(wrapper, '添加文字块').trigger('click')
    const editable = wrapper.get('[contenteditable]')
    editable.element.innerHTML = '<p>保留<strong>重点</strong>。</p><ul><li>一起练习</li></ul>'
    await editable.trigger('input')
    expect(currentValue(wrapper)).toContain('<strong>重点</strong>')
    await button(wrapper, '添加图文块').trigger('click')
    await wrapper.get('.article-image-picker select').setValue(media.id)
    expect(currentValue(wrapper)).toContain(`/api/v1/media/${media.id}/content`)
    expect(currentValue(wrapper)).toContain('alt="我们的玩法照片"')
    expect(currentValue(wrapper)).not.toContain('unused.test')
    wrapper.unmount()
  })
  it('源码修改可返回区块编辑，FAQ 保留原文字工具', async () => {
    const wrapper = mount(ContentEditor, { props: { id: 'body', label: '正文', article: true, modelValue: '<p>原内容</p>' } })
    await button(wrapper, '编辑 HTML 源码').trigger('click')
    const source = `<figure><img src="${articleIllustrations[3]!.src}" alt="团队协作"></figure><p>新的段落</p>`
    await wrapper.get('textarea').setValue(source)
    await button(wrapper, '返回图文编辑').trigger('click')
    expect(wrapper.findAll('[data-block-type="image"]')).toHaveLength(1)
    expect(wrapper.get('[contenteditable]').text()).toBe('')
    expect(wrapper.findAll('[contenteditable]')[1]!.text()).toBe('新的段落')
    wrapper.unmount()
    const faq = mount(ContentEditor, { props: { id: 'answer', label: '答案', modelValue: '<p>FAQ</p>', required: true } })
    expect(faq.get('textarea').attributes('required')).toBeDefined()
    expect(button(faq, '预览排版')).toBeDefined()
    expect(faq.find('.article-add-blocks').exists()).toBe(false)
    faq.unmount()
  })
})
