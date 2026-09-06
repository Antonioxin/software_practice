import { describe, expect, it } from 'vitest'
import { buildCatalogQuery, formatAgeRange, formatCny } from './presentation'

describe('catalog presentation helpers', () => {
  it('formats money and open/closed age ranges consistently', () => {
    expect(formatCny(12900)).toBe('¥129.00')
    expect(formatCny(null)).toBe('—')
    expect(formatAgeRange(4, 9)).toBe('4—9 岁')
    expect(formatAgeRange(6, null)).toBe('6 岁以上')
  })

  it('keeps meaningful filters in a shareable URL and drops blanks', () => {
    const query = buildCatalogQuery({
      keyword: '  平衡  ', categoryId: '', age: '6', scene: 'INDOOR', sort: 'recommended',
    }, 2)

    expect(query.get('keyword')).toBe('平衡')
    expect(query.has('categoryId')).toBe(false)
    expect(query.get('age')).toBe('6')
    expect(query.get('scene')).toBe('INDOOR')
    expect(query.get('page')).toBe('2')
    expect(query.get('pageSize')).toBe('12')
  })
})
