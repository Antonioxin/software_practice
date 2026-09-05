import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusBadge from './StatusBadge.vue'

describe('StatusBadge', () => {
  it('为启用账户提供文字和状态类，不只依赖颜色', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'ACTIVE' } })
    expect(wrapper.text()).toContain('已启用')
    expect(wrapper.classes()).toContain('active')
  })

  it('正确呈现停用账户', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'DISABLED' } })
    expect(wrapper.text()).toContain('已停用')
    expect(wrapper.classes()).toContain('disabled')
  })
})
